package com.symauto.function;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;


public class AutoLeftClickFunction extends SymAbstractFunction {
    protected AutoLeftClickFunction() {
        super("自动左键", "定时左键");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }

        if (client.player.getAttackStrengthScale(0f) < 1.0f) {
            return;
        }
        Entity target = client.crosshairPickEntity;
        if (target == null) {
            return;
        }
        if (!target.isAlive()) {
            return;
        }
        client.gameMode.attack(client.player, target);
    }
}
