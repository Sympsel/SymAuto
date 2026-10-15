package com.symauto.function.highlight;

import com.symauto.config.option.BooleanOption;
import com.symauto.config.option.IntOption;
import com.symauto.function.base.HighLightFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HighLightPlayerFunction extends HighLightFunction {
    public static final HighLightPlayerFunction INSTANCE = new HighLightPlayerFunction();

    @Getter
    private final TeamColor highlightColor = TeamColor.DARK_GREEN;

    @Getter
    @Setter
    private boolean isHighlightSelf = false;

    @Getter
    private final Map<UUID, String> revealedIds = new HashMap<>();

    private HighLightPlayerFunction() {
        super("high_light_player",
                "高亮周围玩家",
                SCAN_RADIUS,
                Tooltip.create()
                        .line("高亮范围内除自己以外的其他玩家")
                        .line(ChatFormatting.GRAY, "可以高亮隐身玩家，并为其单独渲染了HUD")
                        .line(ChatFormatting.RED, "本质透视，有反作弊服务器慎用")
                        .toString());
        addOption(new IntOption("scan_radius", "扫描半径",
                this::getScanRadius, this::setScanRadius, 1, 50, 1));
        addOption(new BooleanOption("is_highlight_self", "高亮自己",
                this::isHighlightSelf, this::setHighlightSelf, false));
    }

    @Override
    protected String describe() {
        int diameter = getScanRadius() * 2;
        return Tooltip.create()
                .line("高亮范围（边长" + diameter + "格）内，除自己以外的其他玩家")
                .line(ChatFormatting.GRAY, "可以高亮隐身玩家，并为其单独渲染了HUD")
                .keyValueLine(ChatFormatting.YELLOW, "高亮自己", ChatFormatting.GRAY, isHighlightSelf ? "是" : "否")
                .line(ChatFormatting.RED, "本质透视，有反作弊服务器慎用，启用此功能即同意以下：禁止用于 PVP，后果自负，出事别找本 Mod 作者")
                .toString();
    }

    @Override
    protected @Nullable Set<UUID> collectGlowIds(Minecraft client) {
        if (client.player == null || client.level == null) {
            revealedIds.clear();
            return Set.of();
        }
        UUID self = client.player.getUUID();
        revealedIds.clear();
        Set<UUID> out = new HashSet<>();
        for (Player p : client.level.getEntitiesOfClass(Player.class,
                client.player.getBoundingBox().inflate(getScanRadius()))) {
            if (!p.isAlive()) continue;
            if (!isHighlightSelf && p.getUUID().equals(self)) continue;
            // 所有玩家都进入高亮集合
            out.add(p.getUUID());
            // 仅隐身玩家额外登记，用于绘制蓝色浮空名字
            if (p.isInvisible() && p instanceof AbstractClientPlayer acp) {
                revealedIds.put(p.getUUID(), acp.getGameProfile().name());
            }
        }
        return out;
    }

    @Override
    protected TeamColor glowColor() {
        return highlightColor;
    }

    @Override
    protected boolean hideNameTag() {
        return true;
    }
}