package com.symauto.function.highlight;

import com.symauto.config.option.BooleanOption;
import com.symauto.config.option.IntOption;
import com.symauto.model.common.SymList;
import com.symauto.function.base.HighLightFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class HighLightMasterFunction extends HighLightFunction {
    public static final HighLightMasterFunction INSTANCE = new HighLightMasterFunction();
    // 配置项：是否仅在主手持有武器时才高亮
    @Getter
    @Setter
    private boolean onlyWhenHoldingWeapon;

    private static final Set<TagKey<Item>> WEAPON_TAGS = Set.of(
            ItemTags.SWORDS,
            ItemTags.AXES,
            ItemTags.SPEARS, // 矛
            ItemTags.MACE_ENCHANTABLE // 重锤
    );

    private static final Set<Item> WEAPON_ITEMS = Set.of(
            Items.BOW,
            Items.CROSSBOW, // 弩
            Items.TRIDENT // 三叉戟
    );
    // 黑白名单
    @Getter
    private final SymList<EntityType<?>> BLACK_LIST;
    @Getter
    private final SymList<EntityType<?>> WHITE_LIST;

    private HighLightMasterFunction() {
        super("high_light_master", "高亮周围敌对生物", SCAN_RADIUS);
        BLACK_LIST = new SymList<>(getId(), SymList.Type.BLACKLIST);
        WHITE_LIST = new SymList<>(getId(), SymList.Type.WHITELIST);
        BLACK_LIST.withDefaultItems(
                EntityTypes.ZOMBIFIED_PIGLIN, // 僵尸猪灵
                EntityTypes.PIGLIN // 猪灵
        );
        WHITE_LIST.withDefaultItems(
                EntityTypes.WARDEN // 监守者（不属于怪物分类，强制高亮）
        );
        addOption(new IntOption("scan_radius", "扫描半径",
                this::getScanRadius, this::setScanRadius, 1, 100, 1));
        addOption(new BooleanOption("only_when_holding_weapon", "仅在主手持有武器时高亮",
                this::isOnlyWhenHoldingWeapon, this::setOnlyWhenHoldingWeapon, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("高亮矩形范围内分类为敌对的怪物")
                .keyValueLine(ChatFormatting.YELLOW, "扫描半径", ChatFormatting.GRAY, String.valueOf(getScanRadius()))
                .keyValueLine(ChatFormatting.YELLOW, "仅在主手持有武器时高亮", ChatFormatting.GRAY, onlyWhenHoldingWeapon ? "是" : "否")
                .display(BLACK_LIST, type -> Component.translatable(type.getDescriptionId()).getString())
                .display(WHITE_LIST, type -> Component.translatable(type.getDescriptionId()).getString())
                .toString();
    }

    @Override
    protected @Nullable Set<UUID> collectGlowIds(Minecraft client) {
        if (onlyWhenHoldingWeapon && !isHoldingWeapon(client.player.getMainHandItem())) {
            return Set.of();
        }
        return scan(client, Mob.class, this::shouldHighlight);
    }

    private static boolean isHoldingWeapon(ItemStack stack) {
        return WEAPON_TAGS.stream().anyMatch(stack::is)
                || WEAPON_ITEMS.contains(stack.getItem());
    }

    private boolean shouldHighlight(Mob mob) {
        EntityType<?> type = mob.getType();
        if (BLACK_LIST.contains(type)) {
            return false;
        }
        if (WHITE_LIST.contains(type)) {
            return true;
        }
        return type.getCategory() == MobCategory.MONSTER;
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.RED;
    }
}
