package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.mixin.MinecraftInvoker;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public class AutoAttackSafetyFunction extends SymAbstractFunction {
    public static final AutoAttackSafetyFunction INSTANCE = new AutoAttackSafetyFunction();
    @Getter
    private final BWList<EntityType<?>> BLACK_LIST = new BWList<>("auto_attack_safety_blacklist");
    private static final String TOOLTIP_BASE = "§a自适应攻速，防检测\n§e推荐使用此版本";

    private AutoAttackSafetyFunction() {
        super("auto_attack_safety", "自动攻击(安全版)",
                TOOLTIP_BASE);
        BLACK_LIST.withDefaultsApplier(() -> {
            // 玩家
            BLACK_LIST.addToBlacklist(EntityTypes.PLAYER);
            // 展示框类
            BLACK_LIST.addToBlacklist(EntityTypes.ITEM_FRAME);
            BLACK_LIST.addToBlacklist(EntityTypes.GLOW_ITEM_FRAME);
            BLACK_LIST.addToBlacklist(EntityTypes.PAINTING);
            // 村民
            BLACK_LIST.addToBlacklist(EntityTypes.VILLAGER);
        });
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
        if (BLACK_LIST.isBlacklisted(target.getType())) {
            return;
        }
        ((MinecraftInvoker) client).invokeStartAttack();
    }

    @Override
    public String getTooltip() {
        return TOOLTIP_BASE + "\n黑名单：" + BLACK_LIST.displayBlacklist(
                "\n §7",
                type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                "\n §7（空）"
        );
    }

    private boolean isAutoEatActive() {
        SymAbstractFunction autoEat = FeatureConfig.AUTO_EAT_FUNCTION;
        return autoEat.isEnable() && autoEat instanceof AutoEatFunction f && f.isEating();
    }
}
