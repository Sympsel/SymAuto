package com.betterbundle.sort.model;

import com.betterbundle.sort.ItemWeights;
import com.betterbundle.sort.SortConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟袋子。既包含非空袋（整理源），也包含空袋（可用目标）。
 */
public final class BagModel {

    /** 该 bundle 物品所在的容器菜单 slot index，用于发包。 */
    public final int invSlot;
    public final ItemStack bundleStack;
    public final List<BagEntry> entries = new ArrayList<>();
    public int usedWeight;
    public boolean locked;
    public String lockReason = "";

    public BagModel(int invSlot, ItemStack bundleStack) {
        this.invSlot = invSlot;
        this.bundleStack = bundleStack;
        BundleContents contents = bundleStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            contents.itemCopyStream().forEach(s -> entries.add(new BagEntry(s)));
            this.usedWeight = ItemWeights.contentsWeight(contents);
        }
        if (!entries.isEmpty() && entries.stream().noneMatch(BagEntry::movable)) {
            this.locked = true;
            this.lockReason = "含不可移动内嵌袋";
        }
    }

    public boolean isEmptyBag() {
        return entries.isEmpty();
    }

    public int freeWeight() {
        return Math.max(0, SortConfig.W_FULL - usedWeight);
    }

    public BagEntry findEntry(ItemKey key) {
        for (BagEntry e : entries) {
            if (e.movable() && e.sameKey(key)) return e;
        }
        return null;
    }

    public int movableCount() {
        int n = 0;
        for (BagEntry e : entries) if (e.movable()) n++;
        return n;
    }
}
