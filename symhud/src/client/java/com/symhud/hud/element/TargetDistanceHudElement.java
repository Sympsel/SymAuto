package com.symhud.hud.element;

import com.symbase.function.util.ClientGuard;
import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import com.symhud.hud.TargetRayCast;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

/**
 * 显示准心到方块/实体目标的距离
 */
public class TargetDistanceHudElement extends AbstractTextHudElement {
    public TargetDistanceHudElement() {
        super("target_distance", "目标距离", "准心到方块或实体目标的距离（最远 64 格）",
                HudAnchor.TOP_LEFT, 0, 62, 0xFFA8D157, 0x00000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        if (!ClientGuard.ready(client)) {
            return "";
        }

        HitResult hit = TargetRayCast.pick(client);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return "";
        }

        double distance = client.player.getEyePosition().distanceTo(hit.getLocation());
        return String.format(Locale.ROOT, "目标距离: %.2f", distance);
    }
}
