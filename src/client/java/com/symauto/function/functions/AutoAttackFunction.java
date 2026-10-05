package com.symauto.function.functions;

import com.symauto.function.FeatureConfig;
import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;


public class AutoAttackFunction extends SymAbstractFunction {
    public AutoAttackFunction() {
        super("auto_attack", "自动攻击§c(危险)", "§c⚠ 警告：此版本直接调用 gameMode.attack()，可后台挂机，但缺少挥手/音效封包，会被反作弊检测！\n§c⚠ 请改用「自动攻击(安全版)」");
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
