package com.betterbundle.sort.plan;

import com.betterbundle.sort.SortConfig;
import com.betterbundle.sort.dag.DependencyGraph;
import com.betterbundle.sort.model.BagEntry;
import com.betterbundle.sort.model.BagModel;
import com.betterbundle.sort.model.InventoryModel;
import com.betterbundle.sort.model.ItemKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划层：Constrained TBFD（聚合 + 腾空 + 依赖排序的撤离）。
 *
 * <p>范围（D2）：只重排袋内可移动条目。三部分提升利用率：
 * <ol>
 *   <li><b>同类合并</b>：把分散在多袋的同种物品合并到承袋者；承袋者放不下时，先撤离其中
 *       最轻的可移动条目到其它袋，腾出重量后再合并。</li>
 *   <li><b>腾空袋子</b>：从最空的袋子开始，若其所有条目都能安置到其它袋，则整袋搬空。</li>
 *   <li><b>依赖排序</b>：撤离动作必须早于依赖它的放入动作；拓扑排序后逐条执行。</li>
 * </ol>
 * 无 Swap：撤离同样经光标整叠取出→放入，只是顺序更严格。
 */
public final class ConstrainedTbfdPlanner {

    private static final class Owner {
        final BagModel bag;
        final BagEntry entry;

        Owner(BagModel bag, BagEntry entry) {
            this.bag = bag;
            this.entry = entry;
        }
    }

    private int nextId;
    private List<PlannedMove> moves;
    private DependencyGraph dag;
    /** 已确定最终归宿的条目：一旦落入这里就不再作为撤离对象，避免反复搬运。 */
    private final java.util.Set<BagEntry> settled =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public PlanResult plan(InventoryModel live) {
        nextId = 0;
        moves = new ArrayList<>();
        dag = new DependencyGraph();
        settled.clear();

        InventoryModel vm = clone(live);

        mergeSameItems(vm);

        // 生成顺序本身已满足"撤离先于依赖它的放入"。取最长有效前缀，避免误报。
        List<PlannedMove> ordered = validPrefix(live, moves);
        if (ordered.isEmpty()) return PlanResult.fail("没有可安全执行的整理步骤");

        List<BagModel> locked = new ArrayList<>();
        for (BagModel b : vm.bags) {
            if (b.locked) locked.add(b);
        }
        return PlanResult.ok(new SortPlan(ordered, locked));
    }

    // ---------- 第一趟：同类合并（可撤离） ----------

    private void mergeSameItems(InventoryModel vm) {
        Map<ItemKey, List<Owner>> byKey = new LinkedHashMap<>();
        for (BagModel b : vm.bags) {
            for (BagEntry e : b.entries) {
                if (!e.movable()) continue;
                byKey.computeIfAbsent(e.key, k -> new ArrayList<>()).add(new Owner(b, e));
            }
        }
        for (List<Owner> owners : byKey.values()) {
            if (owners.size() < 2) continue;
            if (moves.size() > SortConfig.MAX_TOTAL_MOVES) return;

            BagModel sink = null;
            for (Owner o : owners) {
                if (sink == null || o.bag.freeWeight() > sink.freeWeight()) sink = o.bag;
            }
            for (Owner o : owners) {
                if (o.bag == sink) {
                    settled.add(o.entry);
                    continue;
                }
                if (o.entry.count <= 0) continue;
                List<Integer> deps = new ArrayList<>();
                if (!ensureSpace(sink, o.entry.weight, vm, 0, deps, o.entry.key)) continue;
                emitMove(o.bag, o.entry.key, o.entry.count, sink, vm, deps);
                settled.add(o.entry);
            }
        }
    }

    /** 保证 bag 有至少 needed 的空余重量；不足则撤离其最轻的可移动条目（可递归）。 */
    private boolean ensureSpace(BagModel bag, int needed, InventoryModel vm, int depth,
                                List<Integer> deps, ItemKey reserved) {
        if (bag.freeWeight() >= needed) return true;
        if (depth >= SortConfig.MAX_EVAC_DEPTH) return false;

        int guard = 0;
        while (bag.freeWeight() < needed) {
            if (++guard > 64 || moves.size() > SortConfig.MAX_TOTAL_MOVES) return false;
            BagEntry victim = null;
            for (BagEntry e : bag.entries) {
                if (!e.movable() || e.count <= 0) continue;
                if (reserved != null && e.sameKey(reserved)) continue;
                if (settled.contains(e)) continue;   // 已定归宿，不再搬第二次
                if (victim == null || e.weight < victim.weight) victim = e;
            }
            if (victim == null) return false;

            BagModel dest = findRecipient(vm, bag, victim);
            if (dest == null) dest = pickCandidate(vm, bag);
            if (dest == null) return false;

            List<Integer> sub = new ArrayList<>();
            if (dest.freeWeight() < victim.weight
                    && !ensureSpace(dest, victim.weight, vm, depth + 1, sub, null)) {
                return false;
            }

            int id = nextId++;
            moves.add(new PlannedMove(id,
                    new MoveAction(bag.invSlot, victim.key, victim.count, dest.invSlot)));
            dag.addNode(id);
            for (int d : sub) dag.addEdge(d, id);
            deps.add(id);
            applyExtract(bag, victim.key, victim.count, dest);
            settled.add(victim);   // 撤离后即固定，避免后续再次搬它
        }
        return true;
    }

    // ---------- 目标选择 ----------

    /** 有空余且能容纳 entry 的袋子：优先已有同种物品的，其次最紧（填缝）的。 */
    private BagModel findRecipient(InventoryModel vm, BagModel exclude, BagEntry entry) {
        BagModel bestSame = null;
        BagModel best = null;
        for (BagModel b : vm.bags) {
            if (b == exclude || b.locked) continue;
            if (b.freeWeight() < entry.weight) continue;
            if (b.findEntry(entry.key) != null) {
                if (bestSame == null || b.freeWeight() < bestSame.freeWeight()) bestSame = b;
            } else if (best == null || b.freeWeight() < best.freeWeight()) {
                best = b;
            }
        }
        return bestSame != null ? bestSame : best;
    }

    /** 撤离时的候选：选择空余重量最大的袋子，腾出空间最可能成功。 */
    private BagModel pickCandidate(InventoryModel vm, BagModel exclude) {
        BagModel best = null;
        for (BagModel b : vm.bags) {
            if (b == exclude || b.locked) continue;
            if (best == null || b.freeWeight() > best.freeWeight()) best = b;
        }
        return best;
    }

    // ---------- 工具 ----------

    private void emitMove(BagModel src, ItemKey key, int count, BagModel dst,
                          InventoryModel vm, List<Integer> deps) {
        if (src == dst || count <= 0) return;
        int id = nextId++;
        moves.add(new PlannedMove(id, new MoveAction(src.invSlot, key, count, dst.invSlot)));
        dag.addNode(id);
        for (int d : deps) dag.addEdge(d, id);
        applyExtract(src, key, count, dst);
    }

    private void applyExtract(BagModel src, ItemKey key, int count, BagModel dst) {
        BagEntry se = src.findEntry(key);
        if (se == null) return;
        int moved = Math.min(count, se.count);
        if (moved <= 0) return;
        int w = (int) Math.ceil((double) se.weight * moved / se.count);
        if (w <= 0) w = 1;   // 保证每次移动都减少源袋容量，避免死循环

        se.count -= moved;
        se.weight = Math.max(0, se.weight - w);
        if (se.count <= 0) src.entries.remove(se);
        src.usedWeight = Math.max(0, src.usedWeight - w);

        BagEntry de = dst.findEntry(key);
        if (de != null) {
            de.count += moved;
            de.weight += w;
        } else {
            BagEntry ne = new BagEntry(se.stack.copy());
            ne.count = moved;
            ne.weight = w;
            dst.entries.add(ne);
        }
        dst.usedWeight += w;
    }

    /** 在模型上逐条重放，返回能安全执行的最长前缀。 */
    private List<PlannedMove> validPrefix(InventoryModel live, List<PlannedMove> planned) {
        InventoryModel vm = clone(live);
        List<PlannedMove> valid = new ArrayList<>();
        for (PlannedMove m : planned) {
            MoveAction a = m.action();
            BagModel src = vm.bySlot(a.srcBagSlot());
            BagModel dst = vm.bySlot(a.dstBagSlot());
            if (src == null || dst == null || dst.locked) break;
            BagEntry se = src.findEntry(a.key());
            if (se == null || se.count < a.count()) break;
            int weight = (int) Math.ceil((double) se.weight * a.count() / se.count);
            if (dst.freeWeight() < weight) break;
            applyExtract(src, a.key(), a.count(), dst);
            valid.add(m);
        }
        return valid;
    }

    private InventoryModel clone(InventoryModel live) {
        InventoryModel model = new InventoryModel();
        for (BagModel b : live.bags) {
            model.bags.add(new BagModel(b.invSlot, b.bundleStack));
        }
        return model;
    }
}
