package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.abstracts.HighLightFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final int SCAN_RADIUS = 30;
    // 配置项：是否仅在主手持有武器时才高亮
    private static final boolean ONLY_WHEN_HOLDING_WEAPON = true;

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
    private final BWList<EntityType<?>> BW_LIST;

    private HighLightMasterFunction() {
        super("high_light_master",
                "高亮周围敌对生物",
                true,
                SCAN_RADIUS,
                Tooltip.create()
                        .line("高亮范围内（边长" + SCAN_RADIUS * 2 + "格）的敌对怪物")
                        .toString());
        BW_LIST = new BWList<>(getId());
        BW_LIST.withDefaultsApplier(() -> {
            BW_LIST.addAllToBlacklist(Set.of(
                    EntityTypes.ZOMBIFIED_PIGLIN, // 僵尸猪灵
                    EntityTypes.PIGLIN // 猪灵
            ));
            BW_LIST.addAllToWhitelist(Set.of(
                    EntityTypes.WARDEN // 监守者（不属于怪物分类，强制高亮）
            ));
        });
    }

    @Override
    protected String describe() {
        int diameter = getScanRadius() * 2;
        String tooltip = Tooltip.create()
                .line("高亮扫描范围（边长" + diameter + "格）内、分类为敌对（MONSTER）的怪物")
                .keyValueLine(ChatFormatting.YELLOW, "仅在主手持有武器时高亮", ChatFormatting.GRAY, ONLY_WHEN_HOLDING_WEAPON ? "是" : "否")
                .toString();
        tooltip += BW_LIST.displayBlacklist(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        tooltip += BW_LIST.displayWhitelist(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        return tooltip;
    }

    @Override
    protected @Nullable Set<UUID> collectGlowIds(Minecraft client) {
        if (client.player == null) {
            return Set.of();
        }
        if (ONLY_WHEN_HOLDING_WEAPON && !isHoldingWeapon(client.player.getMainHandItem())) {
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
        if (BW_LIST.isBlacklisted(type)) {
            return false;
        }
        if (BW_LIST.isWhitelisted(type)) {
            return true;
        }
        return type.getCategory() == MobCategory.MONSTER;
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.RED;
    }
}
