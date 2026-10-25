package com.symauto.model.container;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 面板物品分类：固定的"全部"标签页 + 运行时读取的原版创造物品栏标签页
 */
public final class ContainerCategory {

    private static final ContainerCategory ALL =
            new ContainerCategory("全部", new ItemStack(Items.COMPASS), stack -> true);

    private static List<ContainerCategory> cache = List.of();
    private static List<CreativeModeTab> cacheSource = List.of();

    private final String displayName;
    private final ItemStack icon;
    private final Predicate<ItemStack> matcher;

    private ContainerCategory(String displayName, ItemStack icon, Predicate<ItemStack> matcher) {
        this.displayName = displayName;
        this.icon = icon;
        this.matcher = matcher;
    }

    public static ContainerCategory all() {
        return ALL;
    }

    public boolean isAll() {
        return this == ALL;
    }

    public String displayName() {
        return displayName;
    }

    public ItemStack icon() {
        return icon.copy();
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && matcher.test(stack);
    }

    /** "全部" 在最前，其后按注册顺序列出所有可见创造标签页 */
    public static List<ContainerCategory> list() {
        List<CreativeModeTab> tabs = visibleTabs();
        if (!tabs.equals(cacheSource)) {
            cacheSource = tabs;
            List<ContainerCategory> out = new ArrayList<>(tabs.size() + 1);
            out.add(ALL);
            for (CreativeModeTab tab : tabs) {
                out.add(new ContainerCategory(
                        tab.getDisplayName().getString(),
                        tab.getIconItem(),
                        itemSetMatcher(tab)));
            }
            cache = List.copyOf(out);
        }
        return cache;
    }

    private static Predicate<ItemStack> itemSetMatcher(CreativeModeTab tab) {
        ensureBuilt(tab);
        Set<Item> items = new HashSet<>();
        for (ItemStack s : tab.getSearchTabDisplayItems()) {
            if (!s.isEmpty()) {
                items.add(s.getItem());
            }
        }
        if (items.isEmpty()) {
            for (ItemStack s : tab.getDisplayItems()) {
                if (!s.isEmpty()) {
                    items.add(s.getItem());
                }
            }
        }
        return stack -> items.contains(stack.getItem());
    }

    private static void ensureBuilt(CreativeModeTab tab) {
        if (tab.hasAnyItems()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Level level = client.level;
        if (level == null) {
            return;
        }
        FeatureFlagSet flags = level.enabledFeatures();
        boolean hasPermissions = client.player != null && client.player.isCreative();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                flags, hasPermissions, level.registryAccess()));
    }

    private static List<CreativeModeTab> visibleTabs() {
        Minecraft client = Minecraft.getInstance();
        Level level = client.level;
        if (level == null) {
            return List.of();
        }
        List<CreativeModeTab> out = new ArrayList<>();
        for (Holder<CreativeModeTab> holder : level.registryAccess()
                .lookupOrThrow(Registries.CREATIVE_MODE_TAB).listElements().toList()) {
            CreativeModeTab tab = holder.value();
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            out.add(tab);
        }
        return out;
    }
}