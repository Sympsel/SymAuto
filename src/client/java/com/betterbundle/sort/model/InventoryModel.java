package com.betterbundle.sort.model;

import java.util.ArrayList;
import java.util.List;

/** 整理用的背包模型：只关心收纳袋，不采集散落物品（已对齐 D2）。 */
public final class InventoryModel {

    public final List<BagModel> bags = new ArrayList<>();

    public BagModel bySlot(int invSlot) {
        for (BagModel b : bags) {
            if (b.invSlot == invSlot) return b;
        }
        return null;
    }
}
