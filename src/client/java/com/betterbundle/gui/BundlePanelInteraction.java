package com.betterbundle.gui;

import com.betterbundle.util.BundleContentsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BundlePanelInteraction {

    private static final int GLFW_MOD_SHIFT = 0x1;

    private BundlePanelInteraction() {}

    private static int gridX(int leftPos) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        return panelX + BundlePanelRenderer.PADDING
                + BundlePanelRenderer.CAT_BAR_WIDTH + 2
                + BundlePanelRenderer.SCROLL_BAR_WIDTH + 2;
    }

    private static int gridY(int topPos) {
        return topPos + BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3 + BundlePanelRenderer.PADDING;
    }

    private static BundlePanelRenderer.FlatItem getClickedItem(double mouseX, double mouseY,
                                                                int leftPos, int topPos) {
        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getBundles();
        if (bundles.isEmpty()) return null;

        List<BundlePanelRenderer.FlatItem> allItems = BundlePanelRenderer.buildFlatItemList(bundles);
        if (allItems.isEmpty()) return null;

        // Use filtered items to match rendered panel
        List<BundlePanelRenderer.FlatItem> items = BundlePanelRenderer.filterItems(allItems, BundlePanelRenderer.searchQuery);
        if (items.isEmpty()) return null;

        int gx = gridX(leftPos);
        int gy = gridY(topPos);

        int relX = (int) mouseX - gx;
        int relY = (int) mouseY - gy;

        int col = relX / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);
        int row = relY / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);

        if (col < 0 || col >= BundlePanelRenderer.COLUMNS) return null;
        if (row < 0 || row >= BundlePanelRenderer.VISIBLE_ROWS) return null;

        int flatIndex = (BundlePanelRenderer.getScrollOffset() + row) * BundlePanelRenderer.COLUMNS + col;
        if (flatIndex >= items.size()) return null;
        return items.get(flatIndex);
    }

    public static boolean handlePanelClick(double mouseX, double mouseY, int button, int modifiers,
                                            int leftPos, int topPos,
                                            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        BundlePanelRenderer.FlatItem clicked = getClickedItem(mouseX, mouseY, leftPos, topPos);
        if (clicked == null) return false;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        List<BundlePanelRenderer.SourceEntry> sources = clicked.sources();
        if (sources.isEmpty()) return false;

        int containerId = player.containerMenu.containerId;
        int maxStack = Math.max(1, clicked.stack().getMaxStackSize());

        extractFromSources(connection, containerId, player, sources, maxStack);
        return true;
    }

    public static boolean handleSpaceClick(Slot hoveredSlot) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        long window = client.getWindow().handle();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) != GLFW.GLFW_PRESS) return false;

        Player player = client.player;
        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty() || BundleContentsHelper.isNonEmptyBundle(stack)) {
            return  false;
        }

        // 不可堆叠物品禁止放入
        if (stack.getMaxStackSize() <= 1) {
            sendQuickMove(player.containerMenu.containerId, hoveredSlot.index);
            return true;
        }

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        List<Integer> targets = buildInsertTargets(bundles, stack, hoveredSlot.index);
        if (targets.isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        int containerId = player.containerMenu.containerId;
        int itemSlot = hoveredSlot.index;

        // 拿起整叠 → 依次放入多个袋子（每个尽量填充，余量留在光标）→ 余量放回原槽。
        sendClick(containerId, itemSlot, 0);
        for (int target : targets) {
            sendClick(containerId, target, 0);
        }
        sendClick(containerId, itemSlot, 0);

        return true;
    }

    private static void sendQuickMove(int containerId, int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            client.gameMode.handleContainerInput(
                    containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
        }
    }

    /**
     * 计算把 stack 放入哪些袋子（类似 XFS 聚堆预分配）：
     * <ol>
     *   <li>优先已有同种物品的袋子（同种越多越靠前）；</li>
     *   <li>没有同种时，选当前能容纳最多的袋子（“找最大的一个”），尽量把整叠聚到一处；</li>
     *   <li>依次累计容量，凑够整叠就停，装不下的部分才继续找下一个袋子。</li>
     * </ol>
     * 返回目标袋子槽位（按放入顺序）。
     */
    private static List<Integer> buildInsertTargets(
            List<BundlePanelRenderer.BundleSlotEntry> bundles, ItemStack stack, int excludeSlot) {
        List<BundlePanelRenderer.BundleSlotEntry> cands = new ArrayList<>();
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (entry.bundleSlot() == excludeSlot) continue;
            if (BundleContentsHelper.maxAcceptable(entry.bundleStack(), stack) > 0) {
                cands.add(entry);
            }
        }
        cands.sort(Comparator
                .comparingInt((BundlePanelRenderer.BundleSlotEntry e) ->
                        BundleContentsHelper.sameItemCount(e.bundleStack(), stack) > 0 ? 0 : 1)
                .thenComparingInt(e -> -BundleContentsHelper.sameItemCount(e.bundleStack(), stack))
                .thenComparingInt(e -> -BundleContentsHelper.maxAcceptable(e.bundleStack(), stack)));

        List<Integer> targets = new ArrayList<>();
        int remaining = stack.getCount();
        for (BundlePanelRenderer.BundleSlotEntry entry : cands) {
            int cap = BundleContentsHelper.maxAcceptable(entry.bundleStack(), stack);
            if (cap <= 0) continue;
            targets.add(entry.bundleSlot());
            remaining -= Math.min(cap, remaining);
            if (remaining <= 0) break;
        }
        return targets;
    }

    /** 发送容器点击操作（通过 gameMode.handleContainerInput 自动追踪 stateID） */
    private static void sendClick(int containerId, int slot, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            client.gameMode.handleContainerInput(
                    containerId, slot, button, ContainerInput.PICKUP, client.player);
        }
    }

    private static long bulkInsertStart = 0;
    private static final long BULK_INSERT_DELAY = 50; // 0.05s

    /** Start the bulk-insert timer (called on space+left-click inside panel with empty cursor). */
    public static void startBulkInsert() {
        bulkInsertStart = System.currentTimeMillis();
    }

    /** Whether the bulk-insert state is active (left button held > 0.05s). */
    public static boolean isBulkInsertActive() {
        return bulkInsertStart > 0 && (System.currentTimeMillis() - bulkInsertStart) >= BULK_INSERT_DELAY;
    }

    /** Exit bulk-insert state. */
    public static void stopBulkInsert() {
        bulkInsertStart = 0;
    }

    /** Put cursor item into available bundles, distributing across several if needed.
     *  单袋放不下时自动拆到多个袋子（优先聚堆）。 */
    public static boolean handlePanelInsert(int button) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;

        ItemStack cursor = player.containerMenu.getCarried();
        if (cursor.isEmpty()) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        List<Integer> targets = buildInsertTargets(bundles, cursor, -1);
        if (targets.isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;
        int containerId = player.containerMenu.containerId;

        // 光标已有整叠，直接依次放入多个袋子；每个尽量填充，余量留在光标上。
        for (int target : targets) {
            sendClick(containerId, target, 0);
        }
        return true;
    }

    /**
     * 当面板可见时，拦截对背包中收纳袋格子的点击：
     * 不拿起收纳袋，而是从面板分组列表中提取该收纳袋里的物品。
     */
    public static boolean handleBundleSlotClick(Slot hoveredSlot,
                                                 net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;
        if (!BundlePanelRenderer.visible) return false;
        if (!BundleContentsHelper.isNonEmptyBundle(hoveredSlot.getItem())) return false;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;
        if (!player.containerMenu.getCarried().isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        int slotIndex = hoveredSlot.index;

        // 在面板分组列表中查找包含该收纳袋的分组物品
        List<BundlePanelRenderer.FlatItem> allItems = BundlePanelRenderer.buildFlatItemList(
                BundlePanelRenderer.getBundles());
        List<BundlePanelRenderer.FlatItem> items = BundlePanelRenderer.filterItems(allItems, BundlePanelRenderer.searchQuery);

        BundlePanelRenderer.FlatItem target = null;
        for (BundlePanelRenderer.FlatItem fi : items) {
            for (BundlePanelRenderer.SourceEntry src : fi.sources()) {
                if (src.bundleSlot() == slotIndex) {
                    target = fi;
                    break;
                }
            }
            if (target != null) break;
        }
        if (target == null) return false;

        int containerId = player.containerMenu.containerId;
        int maxStack = Math.max(1, target.stack().getMaxStackSize());

        extractFromSources(connection, containerId, player, target.sources(), maxStack);
        return true;
    }

    /**
     * 核心提取逻辑：跨收纳袋自动凑满一组（maxStack）。
     * <p>
     * 1. 升序排列 sources（小袋子先取），中间条目 deposit 到空槽
     * 2. 最后一个条目留在光标
     * 3. 从 deposit 槽位取回物品合并到光标（Minecraft 自动处理：
     *    光标+槽位 → 最多 maxStack 在光标，余量留在槽位）
     * 4. 余量塞回有容量的收纳袋
     */
    private static void extractFromSources(ClientPacketListener connection, int containerId,
                                            Player player,
                                            List<BundlePanelRenderer.SourceEntry> sources, int maxStack) {
        // 升序排列：小袋子先 deposit，大袋子留光标
        List<BundlePanelRenderer.SourceEntry> sortedSources = new ArrayList<>(sources);
        sortedSources.sort(Comparator.comparingInt(BundlePanelRenderer.SourceEntry::itemCount));

        // 在提取前缓存物品类型（bundle 提取后内容会变空）
        ItemStack sampleItem = getItemFromSource(player, sortedSources.getFirst());

        List<Integer> emptySlots = findAllEmptyPlayerSlots(player);
        int emptyIdx = 0;
        List<Integer> usedSlotIndices = new ArrayList<>();
        List<Integer> usedSlotCounts = new ArrayList<>();

        int totalExtracted = 0;
        int cursorCount = 0;

        // Phase 1: 提取整叠 + deposit（bundle 提取是全有或全无）
        for (int i = 0; i < sortedSources.size(); i++) {
            BundlePanelRenderer.SourceEntry src = sortedSources.get(i);
            boolean isLast = (i == sortedSources.size() - 1);

            connection.send(new ServerboundSelectBundleItemPacket(src.bundleSlot(), -1));
            connection.send(new ServerboundSelectBundleItemPacket(src.bundleSlot(), src.itemIndex()));
            sendClick(containerId, src.bundleSlot(), 1);

            totalExtracted += src.itemCount();

            if (!isLast) {
                if (emptyIdx < emptySlots.size()) {
                    int slot = emptySlots.get(emptyIdx++);
                    sendClick(containerId, slot, 0);
                    usedSlotIndices.add(slot);
                    usedSlotCounts.add(src.itemCount());
                } else {
                    cursorCount = src.itemCount();
                    break;
                }
            } else {
                cursorCount = src.itemCount();
            }
        }

        // Phase 2: 从 deposit 槽位取回合并到光标
        for (int j = 0; j < usedSlotIndices.size(); j++) {
            if (cursorCount >= maxStack) break;
            int slot = usedSlotIndices.get(j);
            int inSlot = usedSlotCounts.get(j);
            sendClick(containerId, slot, 0);
            int merged = Math.min(inSlot, maxStack - cursorCount);
            int remaining = inSlot - merged;
            usedSlotCounts.set(j, remaining);
            cursorCount += merged;
        }

        // Phase 3: 余量塞回收纳袋（优先刚提取过的 bundle，用数学推算容量）
        if (!sampleItem.isEmpty()) {
            int targetBundle = findExtractedBundleWithCapacity(sortedSources, sampleItem);
            if (targetBundle < 0) {
                targetBundle = findBundleWithCapacity(player, sampleItem);
            }
            if (targetBundle >= 0) {
                for (int j = 0; j < usedSlotIndices.size(); j++) {
                    if (usedSlotCounts.get(j) <= 0) continue;
                    int slot = usedSlotIndices.get(j);
                    if (cursorCount >= maxStack) {
                        if (emptyIdx < emptySlots.size()) {
                            int tempSlot = emptySlots.get(emptyIdx++);
                            sendClick(containerId, tempSlot, 0);
                            sendClick(containerId, slot, 0);
                            sendClick(containerId, targetBundle, 0);
                            sendClick(containerId, tempSlot, 0);
                            usedSlotCounts.set(j, 0);
                        }
                    } else {
                        sendClick(containerId, slot, 0);
                        sendClick(containerId, targetBundle, 0);
                        usedSlotCounts.set(j, 0);
                    }
                    break;
                }
            }
        }
    }

    /** 从 source 获取物品类型样本 */
    private static ItemStack getItemFromSource(Player player, BundlePanelRenderer.SourceEntry src) {
        BundleContents contents = BundleContentsHelper.getContents(
                player.containerMenu.getSlot(src.bundleSlot()).getItem());
        if (contents == null) return ItemStack.EMPTY;
        List<ItemStack> items = contents.itemCopyStream().toList();
        if (src.itemIndex() >= items.size()) return ItemStack.EMPTY;
        return items.get(src.itemIndex());
    }

    /** 查找有剩余容量接受该物品的收纳袋槽位（基于客户端数据，可能过时） */
    private static int findBundleWithCapacity(Player player, ItemStack item) {
        if (item.isEmpty()) return -1;
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != player.getInventory()) continue;
            if (!slot.hasItem()) continue;
            ItemStack bundleStack = slot.getItem();
            if (!BundleContentsHelper.isBundle(bundleStack)) continue;
            if (BundleContentsHelper.maxAcceptable(bundleStack, item) > 0) {
                return slot.index;
            }
        }
        return -1;
    }

    /**
     * 从刚提取过的 bundle 中查找有剩余容量的槽位。
     * 通过数学计算判断：原始重量 - 提取的物品重量 = 当前重量，剩余空间 = 1 - 当前重量。
     * 不依赖过时的客户端 bundle 数据。
     */
    private static int findExtractedBundleWithCapacity(
            List<BundlePanelRenderer.SourceEntry> sortedSources, ItemStack item) {
        int itemMaxStack = Math.max(1, item.getMaxStackSize());
        for (BundlePanelRenderer.SourceEntry src : sortedSources) {
            double totalWeight = 0;
            for (Slot slot : Minecraft.getInstance().player.containerMenu.slots) {
                if (slot.index == src.bundleSlot() && slot.hasItem()) {
                    BundleContents contents = BundleContentsHelper.getContents(slot.getItem());
                    if (contents != null) {
                        totalWeight = contents.weight().result()
                                .orElse(org.apache.commons.lang3.math.Fraction.ZERO).doubleValue();
                    }
                    break;
                }
            }
            double extractedWeight = (double) src.itemCount() / itemMaxStack;
            double remainingWeight = totalWeight - extractedWeight;
            double freeWeight = 1.0 - remainingWeight;
            if (freeWeight >= 1.0 / itemMaxStack - 0.001) {
                return src.bundleSlot();
            }
        }
        return -1;
    }

    /** 找出所有空的玩家背包槽位，先主背包(9-35)后快捷栏(0-8) */
    private static List<Integer> findAllEmptyPlayerSlots(Player player) {
        List<Integer> result = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int min = (pass == 0) ? 9 : 0;
            int max = (pass == 0) ? 36 : 9;
            for (Slot slot : player.containerMenu.slots) {
                if (slot.container == player.getInventory() && !slot.hasItem()) {
                    int idx = slot.getContainerSlot();
                    if (idx >= min && idx < max) result.add(slot.index);
                }
            }
        }
        return result;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double scrollDelta,
                                        int leftPos, int topPos, int imageHeight) {
        if (!BundlePanelRenderer.visible) return false;
        if (!isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) return false;
        BundlePanelRenderer.scrollBy(scrollDelta > 0 ? -1 : 1);
        return true;
    }

    public static boolean isInsidePanel(double mouseX, double mouseY,
                                         int leftPos, int topPos, int imageHeight) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        if (mouseX < panelX || mouseX > panelX + pw) return false;
        int pTop = topPos;
        int searchH = BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3;
        int gridH = BundlePanelRenderer.PADDING * 2
                + BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        int panelH = Math.min(imageHeight, searchH + gridH) + 16;
        if (mouseY < pTop || mouseY > pTop + panelH) return false;
        return true;
    }
}
