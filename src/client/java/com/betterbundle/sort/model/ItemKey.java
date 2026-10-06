package com.betterbundle.sort.model;

import net.minecraft.world.item.ItemStack;

/**
 * 物品合并身份：同物品且同数据组件。内部持有一个 count=1 的代表 stack。
 */
public record ItemKey(ItemStack representative) {

    public static ItemKey of(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ItemKey(copy);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemKey other
                && ItemStack.isSameItemSameComponents(representative, other.representative);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(representative);
    }
}
