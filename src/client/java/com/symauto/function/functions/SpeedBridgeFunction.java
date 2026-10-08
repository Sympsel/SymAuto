package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SpeedBridgeFunction extends SymAbstractFunction {
    public static final SpeedBridgeFunction INSTANCE = new SpeedBridgeFunction();
    private static final Direction[] ALL_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.DOWN, Direction.UP
    };
    // 仅水平，为了适配半砖
    private static final Direction[] HORIZONTAL_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final SlabType DESIRED_SLAB = SlabType.TOP;
    private int tickCounter = 0;
    private final int placeInterval = 1;
    private int lockedSupportY = Integer.MIN_VALUE;

    private SpeedBridgeFunction() {
        super("speed_bridge", "水平跑搭",
                "§e移动时在脚下补方块；裸插件服可用；目前仅支持整砖和上半砖\n§c装了 Grim/Vulcan 等反作弊仍可能因'无视线放置'被标记。");
    }


    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }
        if (!(client.player.getMainHandItem().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (++tickCounter < placeInterval) {
            return;
        }
        tickCounter = 0;
        if (blockItem.getBlock() instanceof SlabBlock) {
            placeSlab(client);
        } else {
            placeFullBlock(client);
        }
    }

    private static BlockHitResult buildFullBlockHit(Minecraft client, BlockPos target, double reach) {
        if (client.level == null) {
            return null;
        }
        for (Direction dir : ALL_SIDES) {
            BlockPos neighbor = target.relative(dir);
            BlockState ns = client.level.getBlockState(neighbor);
            // 整块需要结实支点面
            if (!ns.isFaceSturdy(client.level, neighbor, dir.getOpposite())) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(neighbor);
            // 命中面中心
            Vec3 hitPos = center.add(
                    -dir.getStepX() * 0.5,
                    -dir.getStepY() * 0.5,
                    -dir.getStepZ() * 0.5
            );
            if (!withinReach(client, hitPos, reach)) {
                continue;
            }
            return new BlockHitResult(hitPos, dir.getOpposite(), neighbor, false);
        }
        return null;
    }

    private void placeSlab(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        BlockPos target = BlockPos.containing(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        ).below();
        if (isOccupied(target, client)) {
            return;
        }
        double reach = client.player.blockInteractionRange();
        if (!withinReach(client, Vec3.atCenterOf(target), reach)) {
            return;
        }
        BlockHitResult hit = buildSlabHit(client, target, reach);
        if (hit == null) {
            return;
        }
        doPlace(client, hit);
    }

    private static BlockHitResult buildSlabHit(Minecraft client, BlockPos target, double reach) {
        if (client.level == null) {
            return null;
        }
        for (Direction dir : HORIZONTAL_SIDES) {
            BlockPos neighbor = target.relative(dir);
            BlockState ns = client.level.getBlockState(neighbor);
            // 半砖侧面不 sturdy，但半砖无需支撑，只要有实体、不可替换的方块即可附着
            if (ns.isAir() || ns.canBeReplaced()) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(neighbor);
            double rel = (DESIRED_SLAB == SlabType.TOP) ? 0.9 : 0.1;
            double hy = neighbor.getY() + rel;
            Vec3 hitPos = new Vec3(
                    center.x - dir.getStepX() * 0.5,
                    hy,
                    center.z - dir.getStepZ() * 0.5
            );
            if (!withinReach(client, hitPos, reach)) {
                continue;
            }
            return new BlockHitResult(hitPos, dir.getOpposite(), neighbor, false);
        }
        return null;
    }

    private void placeFullBlock(Minecraft client) {
        if (client.player == null) {
            return;
        }
        BlockPos target = BlockPos.containing(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        ).below();
        if (isOccupied(target, client)) {
            return;
        }
        double reach = client.player.blockInteractionRange();
        if (!withinReach(client, Vec3.atCenterOf(target), reach)) {
            return;
        }
        BlockHitResult hit = buildFullBlockHit(client, target, reach);
        if (hit == null) {
            return;
        }
        doPlace(client, hit);
    }

    private boolean isOccupied(BlockPos pos, Minecraft client) {
        if (client.level == null) {
            return true;
        }
        BlockState state = client.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private static boolean withinReach(Minecraft client, Vec3 point, double reach) {
        if (client.player == null) {
            return false;
        }
        return client.player.position().distanceToSqr(point) <= reach * reach;
    }

    private void doPlace(Minecraft client, BlockHitResult hit) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        InteractionHand hand = InteractionHand.MAIN_HAND;
        client.gameMode.useItemOn(client.player, hand, hit);
        client.player.swing(hand);
    }

    @Override
    protected void onDisable() {
        lockedSupportY = Integer.MIN_VALUE;
    }
}
