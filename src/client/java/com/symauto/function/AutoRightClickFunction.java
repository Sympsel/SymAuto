package com.symauto.function;

import com.symauto.mixin.MinecraftInvoker;
import net.minecraft.client.Minecraft;

public class AutoRightClickFunction extends SymAbstractFunction {

    protected AutoRightClickFunction() {
        super("自动右键", "定时右键");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }
        if (client.player.getAttackStrengthScale(0f) < 1.0f) {
            return;
        }
        ((MinecraftInvoker) client).invokeStartUseItem();
    }
}
