package com.symhud.hud.element;

import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Locale;

public class AnotherCoordinateHudElement  extends AbstractTextHudElement  {
    public AnotherCoordinateHudElement() {
        super("another_coordinates", "另一维度坐标", "当玩家在主世界时显示下界坐标，反之显示主世界坐标",
                HudAnchor.TOP_LEFT, 0, 29, 0xFFD8D2FF, 0x00000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        if (client.player == null || client.level == null) {
            return "";
        }

        ResourceKey<Level> dimension = client.level.dimension();
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String target;

        if (dimension.equals(Level.OVERWORLD)) {
            x /= 8.0;
            z /= 8.0;
            target = "下界";
        } else if (dimension.equals(Level.NETHER)) {
            x *= 8.0;
            z *= 8.0;
            target = "主世界";
        } else {
            return "";
        }

        return String.format(Locale.ROOT, "%s: [%.2f %.2f %.2f]", target, x, y, z);
    }
}
