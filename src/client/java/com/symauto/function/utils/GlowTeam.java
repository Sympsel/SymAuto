package com.symauto.function.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GlowTeam {
    private static final String PREFIX = "symauto_hl_";

    private static final Map<TeamColor, PlayerTeam> CACHE = new EnumMap<>(TeamColor.class);
    private static Scoreboard owner;

    private GlowTeam() {
    }

    // 取得指定颜色的描边队伍
    public static PlayerTeam get(TeamColor color) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        // 换世界/重连后 scoreboard 会变，缓存需整体失效重建
        if (owner != scoreboard) {
            CACHE.clear();
            owner = scoreboard;
        }
        return CACHE.computeIfAbsent(color, c -> {
            String name = PREFIX + c.name().toLowerCase(Locale.ROOT);
            PlayerTeam team = scoreboard.getPlayerTeam(name);
            if (team == null) {
                team = scoreboard.addPlayerTeam(name);
            }
            team.setColor(Optional.of(c));
            return team;
        });
    }
}