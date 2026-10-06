package com.betterbundle.sort.model;

import com.betterbundle.sort.ItemWeights;
import net.minecraft.world.item.ItemStack;

/**
 * 袋内一个条目（一个 stack）。{@code nestedBundle} 为 true 表示这是内嵌收纳袋，
 * 属于静态障碍：不参与移动、合并，也不作为目标，但仍占用重量。
 */
public final class BagEntry {

    public final ItemKey key;
    public final ItemStack stack;
    public int count;
    public final int maxStack;
    public int weight;
    public final boolean nestedBundle;

    public BagEntry(ItemStack stack) {
        this.stack = stack;
        this.key = ItemKey.of(stack);
        this.count = stack.getCount();
        this.maxStack = Math.max(1, stack.getMaxStackSize());
        this.weight = ItemWeights.weightOf(stack);
        this.nestedBundle = ItemWeights.isBundle(stack);
    }

    public boolean movable() {
        return !nestedBundle;
    }

    public boolean sameKey(ItemKey other) {
        return key.equals(other);
    }
}
