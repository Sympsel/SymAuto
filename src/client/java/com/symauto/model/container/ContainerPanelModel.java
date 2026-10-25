package com.symauto.model.container;

import com.symauto.function.base.SymContainer;
import com.symauto.function.container.Containers;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用容器面板的视图模型：从玩家背包收集所有 {@link SymContainer}，汇总为 {@link ItemPool}，
 * 再按 {@link ContainerCategory} 与搜索词产出可渲染的扁平条目列表。
 *
 */
public final class ContainerPanelModel {

    /** 面板上展示的一个聚合条目：某物品 + 跨容器累计数量 + 其所有来源。 */
    public record Entry(ItemStack stack, int totalCount, List<SourceRef> sources) {}

    /**
     * 聚合物品的一个来源。
     * @param container 来源容器
     * @param hostInvIndex  该容器宿主物品在背包中的槽位（0-35），无法定位时为 -1
     * @param count     该来源上的数量
     */
    public record SourceRef(SymContainer container, int hostInvIndex, int entryIndex, int count) {}

    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> visible = new ArrayList<>();
    private final Map<SymContainer, Integer> hostIndexToInv = new IdentityHashMap<>();

    @Setter
    private SymContainer.Type type;
    private ContainerCategory category = ContainerCategory.ALL;
    private String query = "";
    private int containerCount = 0;

    public SymContainer.Type type() {
        return type;
    }

    /** 重新扫描背包并重建全部数据（保留当前分类/搜索状态）。 */
    public void rebuild() {
        all.clear();
        hostIndexToInv.clear();

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            containerCount = 0;
            applyFilter();
            return;
        }
        Inventory inv = player.getInventory();

        List<SymContainer> containers = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            final int slot = i;
            Containers.of(inv.getItem(slot)).ifPresent(c -> {
                if (type != null && c.type() != type) {
                    return;
                }
                containers.add(c);
                hostIndexToInv.put(c, slot);
            });
        }
        containerCount = containers.size();

        ItemPool pool = ItemPool.of(containers);
        for (ItemPool.ItemContext ctx : pool.contexts()) {
            List<SourceRef> srcs = new ArrayList<>();
            for (ItemPool.Source s : ctx.getSources()) {
                srcs.add(new SourceRef(
                        s.container(),
                        hostIndexToInv.getOrDefault(s.container(), -1),
                        s.index(),
                        s.count()));
            }
            ItemStack display = ctx.getKey().representative().copy();
            display.setCount(ctx.getCount());
            all.add(new Entry(display, ctx.getCount(), srcs));
        }

        all.sort(Comparator.comparing(e ->
                BuiltInRegistries.ITEM.getKey(e.stack().getItem()).toString()));
        applyFilter();
    }

    public void setCategory(ContainerCategory category) {
        this.category = category;
        applyFilter();
    }

    public ContainerCategory category() {
        return category;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        applyFilter();
    }

    public String query() {
        return query;
    }

    public List<Entry> visible() {
        return visible;
    }

    public int size() {
        return visible.size();
    }

    public boolean isEmpty() {
        return visible.isEmpty();
    }

    public int containerCount() {
        return containerCount;
    }

    public int totalItems() {
        int n = 0;
        for (Entry e : all) {
            n += e.totalCount();
        }
        return n;
    }

    private void applyFilter() {
        visible.clear();
        String q = query.toLowerCase(Locale.ROOT);
        for (Entry e : all) {
            if (!category.matches(e.stack())) {
                continue;
            }
            if (!q.isEmpty() && !matchesSearch(e.stack(), q)) {
                continue;
            }
            visible.add(e);
        }
    }

    private static boolean matchesSearch(ItemStack stack, String q) {
        if (stack.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString().toLowerCase(Locale.ROOT).contains(q)
                || key.getPath().toLowerCase(Locale.ROOT).contains(q);
    }
}