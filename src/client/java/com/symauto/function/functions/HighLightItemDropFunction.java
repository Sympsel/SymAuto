package com.symauto.function.functions;

import com.symauto.config.option.BooleanOption;
import com.symauto.config.option.IntOption;
import com.symauto.entity.BWList;
import com.symauto.function.abstracts.HighLightFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class HighLightItemDropFunction extends HighLightFunction {
    public static final HighLightItemDropFunction INSTANCE = new HighLightItemDropFunction();
    @Getter
    @Setter
    private boolean onlyWhitelist;
    @Getter
    private final BWList<Item> BW_LIST;

    private HighLightItemDropFunction() {
        super("high_light_item_drop", "高亮周围掉落物", SCAN_RADIUS,
                Tooltip.create()
                        .line("高亮范围内（边长" + SCAN_RADIUS * 2 + "格）掉落物")
                        .toString());
        BW_LIST = new BWList<>(getId());
        BW_LIST.withDefaultsApplier(
                () -> {
                });
        addOption(new IntOption("scan_radius", "扫描半径",
                this::getScanRadius, this::setScanRadius, 1, 50, 1));
        addOption(new BooleanOption("only_whitelist", "仅高亮白名单物品",
                this::isOnlyWhitelist, this::setOnlyWhitelist, false));
    }

    @Override
    protected String describe() {
        return Tooltip.create()
                .line("高亮扫描范围（边长" + getScanRadius() * 2 + "格）内的物品掉落物")
                .keyValueLine(ChatFormatting.YELLOW, "仅高亮白名单物品", ChatFormatting.GRAY, onlyWhitelist ? "是" : "否")
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
        // 黑名单一律不高亮
        if (BW_LIST.isBlacklisted(stack.getItem())) {
            return false;
        }
        return !onlyWhitelist || BW_LIST.isWhitelisted(stack.getItem());
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.GOLD;
    }
}