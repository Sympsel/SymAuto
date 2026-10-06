package com.symauto.function.functions;

import com.betterbundle.gui.BundleCategory;
import com.betterbundle.gui.BundlePanelRenderer;
import com.betterbundle.sort.exec.SortStateMachine;
import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.Minecraft;

public class BetterBundleFunction extends SymAbstractFunction {
    public static final BetterBundleFunction INSTANCE = new BetterBundleFunction();

    private BetterBundleFunction() {
        super("better_bundle", "收纳袋面板", "在容器界面显示收纳袋整理面板与一键整理");
        BundleCategory.registerCategoryItems();
    }

    @Override
    protected void onTrigger(Minecraft client) {

    }

    @Override
    protected void onDisable() {
        SortStateMachine.get().abortByUser();
        BundlePanelRenderer.visible = false;
        BundlePanelRenderer.searchFocused = false;
    }
}
