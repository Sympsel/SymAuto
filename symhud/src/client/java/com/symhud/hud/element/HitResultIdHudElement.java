package com.symhud.hud.element;

import com.symbase.function.utils.ClientGuard;
import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import com.symhud.hud.TargetRayCast;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 显示准心指向的方块或实体 ID
 */
public class HitResultIdHudElement extends AbstractTextHudElement {
    public HitResultIdHudElement() {
        super("hit_result_id", "目标 ID", "准心命中的方块或实体 ID",
                HudAnchor.TOP_LEFT, 0, 51, 0xFFFFFFFF, 0x00000000);
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

        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof ItemEntity itemEntity) {
                return "ID: " + BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
            }
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return "ID: " + id;
        }

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            if (!state.isAir()) {
                return "ID: " + BuiltInRegistries.BLOCK.getKey(state.getBlock());
            }
        }

        return "";
    }
}



