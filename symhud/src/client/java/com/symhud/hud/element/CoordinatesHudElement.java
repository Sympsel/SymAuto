package com.symhud.hud.element;

import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * 玩家坐标
 */
public final class CoordinatesHudElement extends AbstractTextHudElement {
    public CoordinatesHudElement() {
        super("coordinates", "坐标", "显示玩家当前方块坐标",
                HudAnchor.TOP_LEFT, 4, 56, 0xFF9CD2FF, 0x66000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        if (client.player == null) {
            return "XYZ - - -";
        }
        BlockPos pos = client.player.blockPosition();
        return "XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}


