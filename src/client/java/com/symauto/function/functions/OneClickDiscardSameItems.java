package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.Minecraft;

public class OneClickDiscardSameItems extends SymAbstractFunction {
    public static final OneClickDiscardSameItems INSTANCE = new OneClickDiscardSameItems();

    private OneClickDiscardSameItems() {
        super("one_click_discard_same_items", "一键丢出相同物品",
                "一键丢弃容器和背包的相同物品，触发方式：选中其中一组拖拽式丢弃，不丢副手（只需要一件的物品可以放副手当黑名单）\n对于潜影盒，当盒内只有一种物品且两盒物品相同时视作同一物品");
    }

    @Override
    protected void onTrigger(Minecraft client) {}
}
