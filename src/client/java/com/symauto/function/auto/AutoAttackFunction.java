package com.symauto.function.auto;

import com.symauto.config.option.BooleanOption;
import com.symauto.model.common.SymList;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.mixin.accessor.MinecraftAccessor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public class AutoAttackFunction extends SymAbstractFunction {
    public static final AutoAttackFunction INSTANCE = new AutoAttackFunction();

    @Getter
    private final SymList<EntityType<?>> BLACK_LIST = new SymList<>("auto_attack_safety_blacklist", SymList.Type.BLACKLIST);

    /**
     * true=安全模式(防检测，有GUI不攻击)；false=危险模式(直发包，可后台挂机)
     */
    @Getter
    @Setter
    private boolean safeMode = true;


    private AutoAttackFunction() {
        super("auto_attack", "自动攻击");
        BLACK_LIST.withDefaultItems(
                // 玩家
                EntityTypes.PLAYER,
                // 展示类
                EntityTypes.ITEM_FRAME,
                EntityTypes.GLOW_ITEM_FRAME,
                EntityTypes.PAINTING,
                // 村民
                EntityTypes.VILLAGER
        );
        addOption(new BooleanOption("auto_attack_safe_mode", "安全模式（防检测）",
                this::isSafeMode, this::setSafeMode, true));
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }
        // 安全模式：有界面打开时不攻击（模拟真人在线操作，避免误伤/异常）
        if (safeMode && client.gui.screen() != null) {
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
        if (BLACK_LIST.contains(target.getType())) {
            return;
        }
        if (safeMode) {
            ((MinecraftAccessor) client).invokeStartAttack();
        } else {
            client.gameMode.attack(client.player, target);
        }
    }

    @Override
    public String tooltip() {
        Tooltip tooltip = Tooltip.create();
        tooltip.line("自适应攻速的攻击")
                .line(ChatFormatting.GREEN, "安全模式：推荐")
                .line(ChatFormatting.RED, "危险模式：直发攻击包，可后台挂机，会被反作弊检测，建议单人用");
        if (safeMode) {
            tooltip.keyValueLine(ChatFormatting.WHITE, "当前模式", ChatFormatting.GREEN, "安全");
        } else {
            tooltip.keyValueLine(ChatFormatting.WHITE, "当前模式", ChatFormatting.RED, "危险");
        }
        tooltip.display(BLACK_LIST, entityTypes -> BuiltInRegistries.ENTITY_TYPE.getKey(entityTypes).toString());
        return tooltip.toString();
    }

    private boolean isAutoEatActive() {
        SymAbstractFunction autoEat = FunctionConfig.AUTO_EAT_FUNCTION;
        return autoEat.isEnable() && autoEat instanceof AutoEatFunction f && f.isEating();
    }
}