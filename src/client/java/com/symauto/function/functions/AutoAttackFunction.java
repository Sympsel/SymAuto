package com.symauto.function.functions;

import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Set;


public class AutoAttackFunction extends SymAbstractFunction {
    public static final AutoAttackFunction INSTANCE = new AutoAttackFunction();

    private AutoAttackFunction() {
        Tooltip tooltip = Tooltip.create().line(ChatFormatting.RED, "⚠ 警告")
                .line(ChatFormatting.WHITE, "此版本直接为了可以后台挂机，没有封包，会被反作弊检测！");
        tooltip.line(ChatFormatting.YELLOW, "请改用「自动攻击(安全版)」或者仅在单人游戏下使用");
        super("auto_attack", "自动攻击§c(危险)", false,
                tooltip.toString());
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

    @Override
    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Set.of(
                AutoAttackSafetyFunction.INSTANCE.getClass()
        );
    }
}
