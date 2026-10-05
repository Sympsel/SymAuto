package com.symauto.function.functions;

import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.mixin.MinecraftInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class AutoAttackSafetyFunction extends SymAbstractFunction {
    public AutoAttackSafetyFunction() {
        super("auto_attack_safety", "自动攻击(安全版)", "§a走自适应攻速，防检测\n§e推荐使用此版本");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.gui.screen() != null) {
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
        if (target == null || !target.isAlive()) {
            return;
        }
        ((MinecraftInvoker) client).invokeStartAttack();
    }

    private boolean isAutoEatActive() {
        SymAbstractFunction autoEat = FeatureConfig.AUTO_EAT_FUNCTION;
        return autoEat.isEnable() && autoEat instanceof AutoEatFunction f && f.isEating();
    }
}
