package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.mixin.MinecraftInvoker;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.Set;

public class AutoAttackSafetyFunction extends SymAbstractFunction {
    public static final AutoAttackSafetyFunction INSTANCE = new AutoAttackSafetyFunction();
    @Getter
    private final BWList<EntityType<?>> BW_LIST = new BWList<>("auto_attack_safety_blacklist");
    private static String TOOLTIP_BASE;

    private AutoAttackSafetyFunction() {
        TOOLTIP_BASE = Tooltip.create()
                .line(ChatFormatting.GREEN, "自适应攻速，防检测")
                .line(ChatFormatting.GRAY, "推荐使用此版本").toString();
        super("auto_attack_safety", "自动攻击(安全版)",
                TOOLTIP_BASE);
        BW_LIST.withDefaultsApplier(() -> {
            // 玩家
            BW_LIST.addToBlacklist(EntityTypes.PLAYER);
            // 展示框类
            BW_LIST.addToBlacklist(EntityTypes.ITEM_FRAME);
            BW_LIST.addToBlacklist(EntityTypes.GLOW_ITEM_FRAME);
            BW_LIST.addToBlacklist(EntityTypes.PAINTING);
            // 村民
            BW_LIST.addToBlacklist(EntityTypes.VILLAGER);
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
        if (BW_LIST.isBlacklisted(target.getType())) {
            return;
        }
        ((MinecraftInvoker) client).invokeStartAttack();
    }

    @Override
    public String describe() {
        return TOOLTIP_BASE + BW_LIST.displayBlacklist(
                entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString()
        );
    }

    private boolean isAutoEatActive() {
        SymAbstractFunction autoEat = FeatureConfig.AUTO_EAT_FUNCTION;
        return autoEat.isEnable() && autoEat instanceof AutoEatFunction f && f.isEating();
    }

    @Override
    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Set.of(
                AutoAttackFunction.INSTANCE.getClass()
        );
    }
}
