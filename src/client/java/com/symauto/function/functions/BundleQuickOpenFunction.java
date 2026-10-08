package com.symauto.function.functions;

import com.betterbundle.util.BundleContentsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.config.SymAutoKeys;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.gui.BundleQuickViewScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BundleQuickOpenFunction extends SymAbstractFunction {
    public static final BundleQuickOpenFunction INSTANCE = new BundleQuickOpenFunction();
    private final KeyMapping key;

    private BundleQuickOpenFunction() {
        super("bundle_quick_open", "中键快速查看收纳袋",
                "手持收纳袋时按中键弹出纯客户端的袋内物品界面（自适应条数）");
        this.key = new KeyMapping(
                "key.symauto.bundle_quick_open",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                SymAutoKeys.CATEGORY_MAIN);
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }


    @Override public KeyMapping getKeyMapping() { return key; }
    @Override public ScreenContext requireScreenContext() {
        return super.requireScreenContext();
    }

    @Override
    public void onKeyAction(Minecraft client) {
        if (client.player == null) {
            return;
        }
        if (BundleContentsHelper.isNonEmptyBundle(client.player.getMainHandItem())) {
            int bundleSlot = 36 + client.player.getInventory().getSelectedSlot();
            client.gui.setScreen(new BundleQuickViewScreen(bundleSlot));
        }
    }
}