package com.betterbundle.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

/**
 * 重量计算，单位制：一个袋子 = 64。
 *
 * <p>与 26.2 {@code BundleContents.getWeight} 保持一致：
 * <ul>
 *   <li>普通物品：{@code 1 / maxStackSize}，再乘 count。</li>
 *   <li>内嵌收纳袋：{@code 内部内容重量 + 1/16}（即 +4/64）。</li>
 * </ul>
 */
public final class ItemWeights {

    private ItemWeights() {}

    public static boolean isBundle(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BundleItem;
    }

    public static boolean isEmptyBundle(ItemStack stack) {
        if (!isBundle(stack)) return false;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null || contents.isEmpty();
    }

    /** 单个 stack（含 count）的重量，向上取整到整数单位。 */
    public static int weightOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            int inner = unitsFromFraction(contents.weight().result().orElse(Fraction.ZERO));
            return inner + SortConfig.NESTED_BUNDLE_WEIGHT;
        }
        int max = Math.max(1, stack.getMaxStackSize());
        return (int) Math.ceil((double) SortConfig.W_FULL * stack.getCount() / max);
    }

    /** 一个袋子的总内容重量（0..64）。 */
    public static int contentsWeight(BundleContents contents) {
        if (contents == null) return 0;
        return unitsFromFraction(contents.weight().result().orElse(Fraction.ZERO));
    }

    private static int unitsFromFraction(Fraction fraction) {
        return (int) Math.ceil(fraction.doubleValue() * SortConfig.W_FULL);
    }
}
