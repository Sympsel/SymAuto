package com.symauto.function.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.Optional;

public final class GlowTeam {
    private static final TeamColor COLOR = TeamColor.AQUA;


    private static final String NAME = "symauto_highlight";

    private static PlayerTeam cached;
    private static Scoreboard owner;

    private GlowTeam() {
    }

    public static PlayerTeam get() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        // 换世界/重连后 scoreboard 会变，缓存需要重建
        if (cached == null || owner != scoreboard) {
            PlayerTeam existing = scoreboard.getPlayerTeam(NAME);
            cached = existing != null ? existing : scoreboard.addPlayerTeam(NAME);
            cached.setColor(Optional.of(COLOR));
            owner = scoreboard;
        }
        return cached;
    }
}