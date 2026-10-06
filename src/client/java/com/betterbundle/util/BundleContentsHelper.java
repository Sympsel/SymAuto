package com.betterbundle.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

public final class BundleContentsHelper {

    private BundleContentsHelper() {}

    public static BundleContents getContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.get(DataComponents.BUNDLE_CONTENTS);
    }

    public static boolean isNonEmptyBundle(ItemStack stack) {
        BundleContents contents = getContents(stack);
        return contents != null && !contents.isEmpty();
    }

    /** True if this stack is a Bundle item (even if empty/unused). */
    public static boolean isBundle(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BundleItem;
    }

    /** Check if the given item can still fit into this bundle. Works for empty bundles too. */
    public static boolean canFitItem(ItemStack bundleStack, ItemStack toInsert) {
        if (!isBundle(bundleStack) || toInsert.isEmpty()) return false;

        BundleContents contents = getContents(bundleStack);
        Fraction currentWeight;
        if (contents != null) {
            currentWeight = contents.weight().result().orElse(Fraction.ZERO);
        } else {
            currentWeight = Fraction.ZERO; // empty/unused bundle
        }

        Fraction itemWeight = Fraction.getFraction(toInsert.getCount(), toInsert.getMaxStackSize());
        return currentWeight.add(itemWeight).compareTo(Fraction.ONE) <= 0;
    }

    /** 单个物品占袋子的重量分数（内嵌袋按其内容重量 + 1/16）。 */
    public static Fraction perItemWeight(ItemStack stack) {
        BundleContents contents = getContents(stack);
        if (contents != null) {
            return contents.weight().result().orElse(Fraction.ZERO)
                    .add(Fraction.getFraction(1, 16));
        }
        int max = Math.max(1, stack.getMaxStackSize());
        return Fraction.getFraction(1, max);
    }

    /**
     * 这个袋子还能容纳多少个 {@code toInsert}（按重量，floor）。
     * 用于把单袋放不下的一叠拆到多个袋子。
     */
    public static int maxAcceptable(ItemStack bundleStack, ItemStack toInsert) {
        if (!isBundle(bundleStack) || toInsert.isEmpty()) return 0;
        try {
            BundleContents contents = getContents(bundleStack);
            Fraction current = contents != null
                    ? contents.weight().result().orElse(Fraction.ZERO)
                    : Fraction.ZERO;
            Fraction free = Fraction.ONE.subtract(current);
            if (free.compareTo(Fraction.ZERO) <= 0) return 0;
            Fraction per = perItemWeight(toInsert);
            if (per.compareTo(Fraction.ZERO) <= 0) return 0;
            int maxAdd = free.divideBy(per).intValue();
            return Math.max(0, maxAdd);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 袋子中与 {@code item} 同种（物品+组件）的数量，用于“尽量聚堆”。 */
    public static int sameItemCount(ItemStack bundleStack, ItemStack item) {
        BundleContents contents = getContents(bundleStack);
        if (contents == null || item.isEmpty()) return 0;
        int n = 0;
        for (ItemStack s : contents.itemCopyStream().toList()) {
            if (ItemStack.isSameItemSameComponents(s, item)) n += s.getCount();
        }
        return n;
    }
}
