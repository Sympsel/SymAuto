package com.betterbundle.sort.net;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.Map;
import java.util.TreeMap;

/**
 * 袋子内容的规范化签名：key(物品+组件) -> 总数量。
 * 用于与规划预期做精确比对，确认服务端已处理完成（防幽灵物品）。
 */
public final class BundleSignature {

    private BundleSignature() {}

    public static Map<String, Integer> of(ItemStack bagStack) {
        Map<String, Integer> map = new TreeMap<>();
        if (bagStack == null || bagStack.isEmpty()) return map;
        BundleContents contents = bagStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return map;
        contents.itemCopyStream().forEach(s -> map.merge(keyOf(s), s.getCount(), Integer::sum));
        return map;
    }

    public static String keyOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "@"
                + ItemStack.hashItemAndComponents(stack);
    }
}
