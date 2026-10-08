package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.Minecraft;

public class BundleQuickOpenFunction extends SymAbstractFunction {
    public static final BundleQuickOpenFunction INSTANCE = new BundleQuickOpenFunction();

    private BundleQuickOpenFunction() {
        super("bundle_quick_open", "中键快速查看收纳袋",
                "手持收纳袋时按中键弹出纯客户端的袋内物品界面（自适应条数）");
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }
}