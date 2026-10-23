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
    private final SymList<Item> WHITE_LIST;
    @Getter
    private final SymList<Item> BLACK_LIST;

    private HighLightItemDropFunction() {
        super("high_light_item_drop", "高亮周围掉落物", SCAN_RADIUS);
        WHITE_LIST = new SymList<>(getId(), SymList.Type.WHITELIST);
        BLACK_LIST = new SymList<>(getId(), SymList.Type.BLACKLIST);
        addOption(new IntOption("scan_radius", "扫描半径",
                this::getScanRadius, this::setScanRadius, 1, 100, 1));
        addOption(new BooleanOption("only_whitelist", "仅高亮白名单物品",
                this::isOnlyWhitelist, this::setOnlyWhitelist, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("高亮扫描矩形范围内的物品掉落物")
                .keyValueLine(ChatFormatting.YELLOW, "扫描半径", ChatFormatting.GRAY, String.valueOf(getScanRadius()))
                .keyValueLine(ChatFormatting.YELLOW, "仅高亮白名单物品", ChatFormatting.GRAY, onlyWhitelist ? "是" : "否")
                .display(WHITE_LIST, item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .display(BLACK_LIST, item -> BuiltInRegistries.ITEM.getKey(item).toString())
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
        if (BLACK_LIST.contains(stack.getItem())) {
            return false;
        }
        return !onlyWhitelist || WHITE_LIST.contains(stack.getItem());
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.GOLD;
    }
}