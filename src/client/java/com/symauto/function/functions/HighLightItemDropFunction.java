package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.abstracts.HighLightFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class HighLightItemDropFunction extends HighLightFunction {
    public static final HighLightItemDropFunction INSTANCE = new HighLightItemDropFunction();
    private static final int SCAN_RADIUS = 30;
    // 仅高亮价值物（白名单）时置 true；false 则高亮范围内所有掉落物
    private static final boolean ONLY_WHITELIST = false;
    @Getter
    private final BWList<Item> WHITE_LIST;

    private HighLightItemDropFunction() {
        super("high_light_item_drop", "高亮周围掉落物", true, SCAN_RADIUS,
                Tooltip.create()
                        .line("高亮范围内（边长" + SCAN_RADIUS * 2 + "格）掉落物")
                        .keyValueLine(ChatFormatting.YELLOW, "仅高亮白名单物品", ChatFormatting.GRAY, ONLY_WHITELIST ? "是" : "否")
                        .toString());
        WHITE_LIST = new BWList<>(getId());
        WHITE_LIST.withDefaultsApplier(
                () -> {
                });
    }

    @Override
    protected String describe() {
        return Tooltip.create()
                .line("高亮扫描范围（边长" + getScanRadius() * 2 + "格）内的物品掉落物")
                .keyValueLine(ChatFormatting.YELLOW, "仅高亮白名单物品", ChatFormatting.GRAY, ONLY_WHITELIST ? "是" : "否")
                .toString();
    }

    @Override
    protected @Nullable Set<UUID> collectGlowIds(Minecraft client) {
        return scan(client, ItemEntity.class, this::shouldHighlight);
    }

    private boolean shouldHighlight(ItemEntity item) {
        if (item.isRemoved()) {
            return false;
        }
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        return !ONLY_WHITELIST; // TODO: 接白名单 Set<Item> 过滤
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.GOLD;
    }
}