package com.betterbundle.sort.plan;

import com.betterbundle.sort.model.ItemKey;

/** 一个逻辑移动：从 srcBagSlot 取出 key 的 count 个物品，放入 dstBagSlot（经光标中转）。 */
public record MoveAction(int srcBagSlot, ItemKey key, int count, int dstBagSlot) {

    @Override
    public String toString() {
        return "Move[" + srcBagSlot + " -> " + dstBagSlot + " x" + count + "]";
    }
}
