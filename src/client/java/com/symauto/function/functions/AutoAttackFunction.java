package com.symauto.function.functions;

import com.symauto.function.FeatureConfig;
import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;


public class AutoAttackFunction extends SymAbstractFunction {
    public AutoAttackFunction() {
        super("自动攻击", "自动攻击，自适应当前攻速，同时添加浮动系数，防检测");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }

        if (client.player.isUsingItem() || isAutoEatActive()) {
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

    private boolean isAutoEatActive() {
        SymAbstractFunction autoEat = FeatureConfig.AUTO_EAT_FUNCTION;
        return autoEat.isEnable() && autoEat instanceof AutoEatFunction f && f.isEating();
    }
}
