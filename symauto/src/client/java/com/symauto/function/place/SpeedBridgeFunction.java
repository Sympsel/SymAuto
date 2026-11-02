package com.symauto.function.place;

import com.symauto.core.AutoFunctions;
import com.symbase.config.option.BooleanOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symauto.function.auto.AutoEatFunction;
import com.symbase.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpeedBridgeFunction extends SymAbstractFunction {
    public static final SpeedBridgeFunction INSTANCE = new SpeedBridgeFunction();

    @Getter
    @Setter
    private boolean enableFakeSneakPlace;

    @Getter
    @Setter
    private boolean enableFakeSneak;

    private static final Direction[] ALL_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.DOWN, Direction.UP
    };
    // 仅水平，为了适配半砖
    private static final Direction[] HORIZONTAL_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private int tickCounter = 0;
    private final int placeInterval = 1;

    private SpeedBridgeFunction() {
        super("speed_bridge", "水平跑搭");
        addOption(new BooleanOption("fake_sneak_place", "伪潜行放置",
                this::isEnableFakeSneakPlace, this::setEnableFakeSneakPlace, true));
        addOption(new BooleanOption("fake_sneak", "伪潜行",
                this::isEnableFakeSneak, this::setEnableFakeSneak, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("移动时在脚下补方块，支持完整方块、上半砖和下半砖")
                .keyValueLine("伪潜行放置", enableFakeSneakPlace ? "是" : "否")
                .keyValueLine("伪潜行", enableFakeSneak ? "是" : "否")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null) {
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }

        if (client.player.isCreative()) {
            return;
        }
        if (isAutoEatActive(client)) {
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

    private boolean isAutoEatActive(Minecraft client) {
        SymAbstractFunction autoEat = AutoFunctions.AUTO_EAT_FUNCTION;
        return (autoEat instanceof AutoEatFunction f && f.isEating())
                || (client.player != null && client.player.isUsingItem());
    }

    private static Input withShift(Input input, boolean shift) {
        return new Input(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.jump(),
                shift,
                input.sprint()
        );
    }


    private static BlockHitResult buildFullBlockHit(Minecraft client, BlockPos target, double reach) {
        if (client.level == null) {
            return null;
        }
        for (Direction dir : ALL_SIDES) {
            BlockPos neighbor = target.relative(dir);
            BlockState ns = client.level.getBlockState(neighbor);
            if (ns.isAir() || ns.canBeReplaced()) {
                continue;
            }
            VoxelShape shape = ns.getShape(client.level, neighbor);
            if (shape.isEmpty()) {
                continue;
            }
            double hitY = switch (dir) {
                case UP -> neighbor.getY() + shape.min(Direction.Axis.Y);
                case DOWN -> neighbor.getY() + shape.max(Direction.Axis.Y);
                default -> neighbor.getY() + (shape.min(Direction.Axis.Y) + shape.max(Direction.Axis.Y)) * 0.5;
            };
            Vec3 hitPos = new Vec3(
                    neighbor.getX() + 0.5 - dir.getStepX() * 0.5,
                    hitY,
                    neighbor.getZ() + 0.5 - dir.getStepZ() * 0.5
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
        BlockPos target = bridgeTarget(client);
        if (isOccupied(target, client)) {
            return;
        }
        double reach = client.player.blockInteractionRange();
        if (!withinReach(client, Vec3.atCenterOf(target), reach)) {
            return;
        }
        BlockHitResult hit = buildSlabHit(client, target, reach, desiredSlab(client));
        if (hit == null) {
            return;
        }
        doPlace(client, hit);
    }

    private static BlockHitResult buildSlabHit(Minecraft client, BlockPos target, double reach,
                                               SlabType desiredSlab) {
        if (client.level == null) {
            return null;
        }
        for (Direction dir : HORIZONTAL_SIDES) {
            BlockPos neighbor = target.relative(dir);
            BlockState ns = client.level.getBlockState(neighbor);
            if (ns.isAir() || ns.canBeReplaced()) {
                continue;
            }
            VoxelShape shape = ns.getShape(client.level, neighbor);
            if (shape.isEmpty()) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(neighbor);
            double rel = desiredSlab == SlabType.TOP
                    ? Math.max(shape.min(Direction.Axis.Y), shape.max(Direction.Axis.Y) - 0.05)
                    : Math.min(shape.max(Direction.Axis.Y), shape.min(Direction.Axis.Y) + 0.05);
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
        BlockPos target = bridgeTarget(client);
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

    /**
     * 玩家脚下要放置的方块位置
     */
    private static BlockPos bridgeTarget(Minecraft client) {
        return BlockPos.containing(
                client.player.getX(),
                client.player.getY() - 1.0E-4,
                client.player.getZ()
        );
    }

    /**
     * 根据当前站立高度推断应放上半砖还是下半砖
     */
    private static SlabType desiredSlab(Minecraft client) {
        double feetY = client.player.getY();
        double fraction = feetY - Math.floor(feetY);
        return fraction < 0.25 || fraction >= 0.75 ? SlabType.TOP : SlabType.BOTTOM;
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

        // 伪潜行
        Input originalInput = client.player.input.keyPresses;
        boolean fakeSneak = (enableFakeSneakPlace || enableFakeSneak)
                && !client.player.isShiftKeyDown();
        if (fakeSneak) {
            Input sneakInput = withShift(originalInput, true);
            client.player.input.keyPresses = sneakInput;
            client.player.setShiftKeyDown(true);
            sendInput(client, sneakInput);
        }

        try {
            client.gameMode.useItemOn(client.player, hand, hit);
        } finally {
            if (fakeSneak) {
                client.player.input.keyPresses = originalInput;
                client.player.setShiftKeyDown(false);
                sendInput(client, originalInput);
            }
        }
        client.player.swing(hand);
    }

    private static void sendInput(Minecraft client, Input input) {
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundPlayerInputPacket(input));
        }
    }
}





