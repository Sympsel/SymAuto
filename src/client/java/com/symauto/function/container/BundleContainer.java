package com.symauto.function.container;

import com.symauto.function.base.SymContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BundleContainer implements SymContainer {
    private final ItemStack host;

    public BundleContainer(ItemStack host) {
        this.host = host;
    }

    private BundleContents contents() {
        return host.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    private List<ItemStack> itemList() {
        return new ArrayList<>(contents().itemCopyStream().toList());
    }

    @Override
    public Type type() {
        return Type.BUNDLE;
    }

    @Override
    public ItemStack host() {
        return host;
    }

    @Override
    public boolean supportsDirectWrite() {
        return true;
    }

    @Override
    public int entryCount() {
        return contents().size();
    }


    @Override
    public double fillRatio() {
        return contents().weight().result().orElse(Fraction.ONE).doubleValue();
    }

    /**
     * 最大可容纳的 toInsert 数量
     */
    @Override
    public int maxAcceptable(ItemStack toInsert) {
        if (toInsert == null || toInsert.isEmpty()) {
            return 0;
        }
        Fraction curr = contents().weight().result().orElse(Fraction.ZERO);
        Fraction free = Fraction.ONE.subtract(curr);
        if (free.compareTo(Fraction.ZERO) <= 0) {
            return 0;
        }
        Fraction one = perItemWeight(toInsert);
        if (one.compareTo(Fraction.ZERO) <= 0) {
            return 0;
        }
        int maxCount = free.divideBy(one).intValue();
        return Math.max(0, maxCount);
    }

    /**
     * 每个物品的重量分数
     */
    public static Fraction perItemWeight(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            // 是收纳袋，占 1/16
            return contents.weight().result().orElse(Fraction.ZERO)
                    .add(Fraction.getFraction(1, 16));
        }
        // 一个物品的最大堆叠数
        int one = Math.max(1, stack.getMaxStackSize());
        return Fraction.getFraction(1, one);
    }

    @Override
    public boolean canFit(ItemStack toInsert) {
        return maxAcceptable(toInsert) > 0;
    }

    @Override
    public boolean isEmpty() {
        return contents().isEmpty();
    }


    @Override
    public boolean isFull() {
        return contents().weight().result()
                .map(f -> f.compareTo(Fraction.ONE) >= 0)
                .orElse(true);
    }


    @Override
    public List<ItemStack> entries() {
        return contents().itemCopyStream().toList();
    }

    @Override
    public Optional<ItemStack> entryAt(int index) {
        List<ItemStack> items = itemList();
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        return Optional.of(items.get(index));
    }

    @Override
    public int indexOf(ItemStack probe) {
        if (probe == null || probe.isEmpty()) {
            return -1;
        }
        List<ItemStack> items = itemList();
        for (int i = 0; i < items.size(); ++i) {
            if (ItemStack.isSameItemSameComponents(items.get(i), probe)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int countOf(ItemStack probe) {
        int total = 0;
        for (ItemStack s : contents().itemCopyStream().toList()) {
            if (ItemStack.isSameItemSameComponents(s, probe)) {
                total += s.count();
            }
        }
        return total;
    }

    @Override
    public boolean contains(ItemStack probe) {
        return indexOf(probe) >= 0;
    }

    @Override
    public int add(ItemStack stack) {
        return 0;
    }

    @Override
    public int remove(ItemStack probe, int count) {
        return 0;
    }

    @Override
    public boolean setEntry(int index, ItemStack stack) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean commit() {
        return false;
    }
}
