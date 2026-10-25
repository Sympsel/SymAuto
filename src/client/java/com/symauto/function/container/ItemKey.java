package com.symauto.function.container;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * 拷贝一份代表物品作为键
 * @param representative 代表物品
 */
public record ItemKey(ItemStack representative) {
    public static ItemKey of(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ItemKey(copy);
    }

    public String id() {
        return BuiltInRegistries.ITEM.getKey(representative.getItem()).toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemKey(ItemStack representative1)
                && ItemStack.isSameItemSameComponents(representative, representative1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(representative);
    }
}
