package com.symhud.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 准心目标射线检测
 */
public final class TargetRayCast {
    public static final double MAX_DISTANCE = 64.0;
    private static final float ENTITY_MARGIN = 0.3F;

    private TargetRayCast() {
    }

    public static HitResult pick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        Vec3 from = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0F);
        Vec3 to = from.add(view.scale(MAX_DISTANCE));

        BlockHitResult blockHit = client.level.clip(new ClipContext(
                from, to,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                client.player));

        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        AABB searchBox = client.player.getBoundingBox()
                .expandTowards(view.scale(MAX_DISTANCE))
                .inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                client.level,
                client.player,
                from,
                end,
                searchBox,
                entity -> entity != client.player
                        && !entity.isSpectator()
                        && (entity.isPickable() || entity instanceof ItemEntity),
                ENTITY_MARGIN);

        return entityHit != null ? entityHit : blockHit;
    }
}

