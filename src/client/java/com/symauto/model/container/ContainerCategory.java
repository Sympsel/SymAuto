package com.symauto.model.container;

import com.symauto.function.container.BundleCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * SymAuto 通用容器面板使用的物品分类。
 *
 * <p>分类引擎当前委托给 betterbundle 的 {@link BundleCategory}（其集合镜像原版创造模式的物品分类），
 * 上层面板只依赖本枚举，从而与 betterbundle 的 GUI 实现解耦；后续可平滑替换为运行时查询原版创造标签。
 */
public enum ContainerCategory {
    ALL(BundleCategory.ALL),
    BLOCKS(BundleCategory.BLOCKS),
    NON_FULL_BLOCKS(BundleCategory.NON_FULL_BLOCKS),
    PLANTS(BundleCategory.PLANTS),
    FOOD(BundleCategory.FOOD),
    TOOLS_AND_EQUIPMENT(BundleCategory.TOOLS_AND_EQUIPMENT),
    MISC(BundleCategory.MISC),
    MINERALS(BundleCategory.MINERALS);

    private final BundleCategory delegate;

    ContainerCategory(BundleCategory delegate) {
        this.delegate = delegate;
    }

    public String displayName() {
        return delegate.getDisplayName();
    }

    public ItemStack icon() {
        return delegate.getIcon();
    }

    public boolean isAll() {
        return this == ALL;
    }

    /** 判断物品是否归属本分类（ALL 恒为真）。 */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return delegate.matches(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static ContainerCategory next(ContainerCategory current) {
        ContainerCategory[] values = values();
        return values[(current.ordinal() + 1) % values.length];
    }
}