package com.symhud.hud.element;

import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * 玩家坐标
 */
public final class CoordinatesHudElement extends AbstractTextHudElement {
    public CoordinatesHudElement() {
        super("coordinates", "坐标", "显示玩家坐标",
                HudAnchor.TOP_LEFT, 0, 18, 0xFF9CD2FF, 0x00000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        if (client.player == null) {
            return "";
        }
        return String.format(Locale.ROOT, "主世界: [%.2f %.2f %.2f]",
                client.player.getX(),
                client.player.getY(),
                client.player.getZ());
    }
}
