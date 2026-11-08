package com.symauto.function.place;

import com.symauto.core.AutoFunctions;
import com.symbase.config.option.BooleanOption;
import com.symbase.config.option.IntOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symauto.function.auto.AutoEatFunction;
import com.symbase.function.util.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpeedBridgeFunction extends SymAbstractFunction {
    public static final SpeedBridgeFunction INSTANCE = new SpeedBridgeFunction();

    @Getter
    @Setter
    private boolean enableFakeSneakPlace;

    @Getter
    @Setter
    private boolean enableFakeSneak;

    @Getter
    @Setter
    private boolean destroyKelp;

    @Getter
    @Setter
    private boolean frostWalkerMode;

    /** 冰霜行者模式的扩散半径（以脚下平面为中心，正方形区域） */
    @Getter
    @Setter
    private int frostWalkerRadius = 2;

    /** 冰霜行者模式每 tick 最多放置的方块数 */
    @Getter
    @Setter
    private int frostWalkerMaxPerTick = 16;

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

    /** 自动化破坏期间置位，避免被“锁定 Y 轴”的 mixin 当成玩家手动挖掘拦掉 */
    @Getter
    private static boolean automatedBreak;

    /** 冰霜行者模式下锁定的搭路平面（站立时刷新，跳跃/下落时保持不变） */
    private int frostWalkerY = Integer.MIN_VALUE;

    private SpeedBridgeFunction() {
        super("speed_bridge", "水平跑搭");
        addOption(new BooleanOption("fake_sneak_place", "伪潜行放置",
                "放置时临时潜行，避免打开容器界面",
                this::isEnableFakeSneakPlace, this::setEnableFakeSneakPlace, true));
        addOption(new BooleanOption("fake_sneak", "伪潜行",
                "始终保持潜行状态",
                this::isEnableFakeSneak, this::setEnableFakeSneak, false));
        addOption(new BooleanOption("destroy_kelp", "破坏海带",
                "破坏挡路的海带后再放置",
                this::isDestroyKelp, this::setDestroyKelp, false));
        addOption(new BooleanOption("frost_walker_mode", "冰霜行者模式",
                "在脚下平面按半径向四周扩散放置",
                this::isFrostWalkerMode, this::setFrostWalkerMode, false));
        addOption(new IntOption("frost_walker_radius", "冰霜行者扩散半径",
                "扩散半径，正方形区域",
                this::getFrostWalkerRadius, this::setFrostWalkerRadius, 1, 6, 1));
        addOption(new IntOption("frost_walker_max_per_tick", "冰霜行者每tick最大放置数",
                "每 tick 最多放置的方块数",
                this::getFrostWalkerMaxPerTick, this::setFrostWalkerMaxPerTick, 1, 64, 1));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("移动时在脚下补方块，支持完整方块、上半砖和下半砖")
                .keyValueLine("伪潜行放置", enableFakeSneakPlace ? "是" : "否")
                .keyValueLine("伪潜行", enableFakeSneak ? "是" : "否")
                .keyValueLine("破坏海带", destroyKelp ? "是" : "否")
                .keyValueLine("冰霜行者模式", frostWalkerMode ? "是" : "否")
                .keyValueLine("冰霜行者扩散半径", String.valueOf(frostWalkerRadius))
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

        if (!client.player.onGround()) {
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
        // 冰霜行者模式：站立时刷新搭路平面
        if (frostWalkerMode) {
            if (client.player.onGround()) {
                frostWalkerY = BlockPos.containing(
                        client.player.getX(),
                        client.player.getY() - 1.0E-4,
                        client.player.getZ()).getY();
            }
        } else {
            frostWalkerY = Integer.MIN_VALUE;
        }
        if (blockItem.getBlock() instanceof SlabBlock) {
            placeSlab(client);
        } else {
            placeFullBlock(client);
        }
        // 冰霜行者模式：在原路径之外，向脚下平面四周扩散铺开
        if (frostWalkerMode) {
            spreadFrostWalker(client, blockItem);
        }
    }

    /**
     * 冰霜行者式扩散放置：以脚下平面为中心，按半径把所有“可替换方块”
     */
    private void spreadFrostWalker(Minecraft client, BlockItem blockItem) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        BlockPos center = bridgeTarget(client);
        int radius = Math.max(1, frostWalkerRadius);
        double reach = client.player.blockInteractionRange();
        SlabType slab = blockItem.getBlock() instanceof SlabBlock ? desiredSlab(client) : null;

        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            if (!client.level.getBlockState(pos).canBeReplaced()) {
                continue;
            }
            Vec3 hitPos = Vec3.atCenterOf(pos);
            if (!withinReach(client, hitPos, reach)) {
                continue;
            }
            targets.add(pos.immutable());
        }
        if (targets.isEmpty()) {
            return;
        }
        // 近处优先，保证脚下先铺
        targets.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));

        int budget = Math.min(frostWalkerMaxPerTick, targets.size());
        for (int i = 0; i < budget; i++) {
            if (!(client.player.getMainHandItem().getItem() instanceof BlockItem)) {
                // todo 下一 tick 补给逻辑
                break;
            }
            BlockPos pos = targets.get(i);
            BlockHitResult hit = buildSpreadHit(client, pos, reach, slab);
            if (hit == null) {
                continue;
            }
            // 扩散不伪潜行，避免每格多发两个输入包
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        }
    }

    /**
     * 扩散时单格的命中：优先点相邻实心方块，没有支撑再点目标格自身
     */
    private static BlockHitResult buildSpreadHit(Minecraft client, BlockPos pos, double reach, SlabType slab) {
        BlockHitResult hit = slab == null
                ? buildFullBlockHit(client, pos, reach)
                : buildSlabHit(client, pos, reach, slab);
        if (hit != null) {
            return hit;
        }
        return buildFrostWalkerHit(client, pos, reach, slab);
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
        SlabType slab = desiredSlab(client);
        // 优先点相邻实心方块的面
        BlockHitResult hit = buildSlabHit(client, target, reach, slab);
        // 冰霜行者模式：没有支撑面时，直接点可替换方块（水/空气）自身
        if (hit == null && frostWalkerMode) {
            hit = buildFrostWalkerHit(client, target, reach, slab);
        }
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
        // 原路径：优先点相邻实心方块的面
        BlockHitResult hit = buildFullBlockHit(client, target, reach);
        // 冰霜行者模式：没有支撑面时，直接点可替换方块（水/空气）自身
        if (hit == null && frostWalkerMode) {
            hit = buildFrostWalkerHit(client, target, reach, null);
        }
        if (hit == null) {
            return;
        }
        doPlace(client, hit);
    }

    private static BlockHitResult buildFrostWalkerHit(Minecraft client, BlockPos target, double reach,
                                                      SlabType slab) {
        if (client.level == null) {
            return null;
        }
        BlockState state = client.level.getBlockState(target);
        if (!state.canBeReplaced()) {
            return null;
        }
        Vec3 hitPos = Vec3.atCenterOf(target);
        if (!withinReach(client, hitPos, reach)) {
            return null;
        }
        Direction face = slab == null
                ? Direction.UP
                : slab == SlabType.TOP ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(hitPos, face, target, false);
    }

    /**
     * 玩家脚下要放置的方块位置
     */
    private BlockPos bridgeTarget(Minecraft client) {
        BlockPos feet = BlockPos.containing(
                client.player.getX(),
                client.player.getY() - 1.0E-4,
                client.player.getZ()
        );
        if (frostWalkerMode && frostWalkerY != Integer.MIN_VALUE) {
            return new BlockPos(feet.getX(), frostWalkerY, feet.getZ());
        }
        return feet;
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
        if (destroyKelp && isKelp(state)) {
            breakKelp(client, pos);
            state = client.level.getBlockState(pos);
            if (state.isAir()) {
                return false;
            }
        }
        return !state.isAir() && !state.canBeReplaced();
    }

    private static boolean isKelp(BlockState state) {
        return state.getBlock() instanceof KelpBlock
                || state.getBlock() instanceof KelpPlantBlock;
    }

    private void breakKelp(Minecraft client, BlockPos pos) {
        if (client.gameMode == null || client.player == null) {
            return;
        }
        automatedBreak = true;
        try {
            client.gameMode.startDestroyBlock(pos, Direction.UP);
        } finally {
            automatedBreak = false;
        }
        client.player.swing(InteractionHand.MAIN_HAND);
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





