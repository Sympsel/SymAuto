package com.symauto.function.functions;

import com.symauto.entity.SymFunctionTags;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.mixin.FishingHookAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class AutoFishingFunction extends SymAbstractFunction {
    public static final AutoFishingFunction INSTANCE = new AutoFishingFunction();
    private static final int RECAST_DELAY_TICKS = 15;
    private static final int CAST_TIMEOUT_TICKS = 900;

    private enum State { IDLE, CAST, WAITING, REELING, RECASTING }

    private State state = State.IDLE;
    private int waitTicks = 0;
    private int recastCountdown = 0;
    // 抛竿后等待 2s 判定是否落水 + 是否开放水域
    private static final int SETTLE_CHECK_TICKS = 40;
    // 不再水中十秒后重抛
    private static final int NOT_IN_WATER_RECAST_TICKS = 200;
    private boolean notInWater = false;

    private AutoFishingFunction() {
        super("auto_fishing","自动钓鱼", "手持钓鱼杆时自动甩杆收杆\n等待期间：\n2秒后检测是否在水中和开放水域\n如果不在水中10秒后重新抛竿\n兜底45秒没有鱼会超时重新抛竿");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }

        boolean mainRod = client.player.getMainHandItem().is(Items.FISHING_ROD);
        boolean offRod = client.player.getOffhandItem().is(Items.FISHING_ROD);
        if (!mainRod && !offRod) {
            if (state != State.IDLE) {
                reset();
            }
            return;
        }

        switch (state) {
            case IDLE -> {
                castRod(client);
                state = State.CAST;
            }
            case CAST -> {
                if (client.player.fishing != null) {
                    waitTicks = 0;
                    notInWater = false;
                    state = State.WAITING;
                }
            }
            case WAITING -> {
                FishingHook hook = client.player.fishing;
                if (hook == null) {
                    state = State.IDLE;
                    return;
                }

                waitTicks++;
                if (waitTicks == SETTLE_CHECK_TICKS) {
                    notInWater = !isHookInWater(client.level, hook);
                    if (notInWater) {
                        client.player.sendOverlayMessage(
                                Component.literal("⚠ 鱼漂未落入水中，" + NOT_IN_WATER_RECAST_TICKS / 20 + " 秒后自动重抛")
                                        .withStyle(ChatFormatting.RED)
                        );
                    } else {
                        judgeIsInOpenWater(client);
                    }
                }

                if (notInWater && waitTicks >= NOT_IN_WATER_RECAST_TICKS) {
                    reelIn(client);
                    state = State.REELING;
                    notInWater = false;
                    return;
                }

                if (waitTicks > CAST_TIMEOUT_TICKS) {
                    reelIn(client);
                    state = State.REELING;
                    return;
                }

                if (((FishingHookAccessor) hook).isBiting()) {
                    reelIn(client);
                    state = State.REELING;
                }
            }
            case REELING -> {
                if (client.player.fishing == null) {
                    recastCountdown = RECAST_DELAY_TICKS;
                    state = State.RECASTING;
                }
            }
            case RECASTING -> {
                recastCountdown--;
                if (recastCountdown <= 0) {
                    state = State.IDLE;
                }
            }
        }
    }

    /**
     * 手动判定开阔水域
     * 规则：以浮漂为中心 5×5 区域，
     *  - 水面层只能是水或空气/睡莲；
     *  - 水面下方一格必须是水（保证水深，排除一格水坑）；
     *  - 水面上方两格必须是空气/睡莲（保证天空暴露，排除室内/加盖）；
     *  - 至少存在一层水。
     */
    private boolean isManuallyInOpenWater(Level level, FishingHook hook) {
        BlockPos center = hook.blockPosition();

        int waterY;
        if (isWater(level, center)) {
            waterY = center.getY();
        } else if (isWater(level, center.below())) {
            waterY = center.getY() - 1;
        } else {
            return false;
        }

        int cx = center.getX();
        int cz = center.getZ();
        boolean anyWater = false;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx;
                int z = cz + dz;

                BlockPos surface = new BlockPos(x, waterY, z);
                if (isWater(level, surface)) {
                    anyWater = true;
                } else if (!isAirOrLily(level, surface)) {
                    return false;
                }

                if (!isWater(level, new BlockPos(x, waterY - 1, z))) {
                    return false;
                }
                if (!isAirOrLily(level, new BlockPos(x, waterY + 1, z))
                        || !isAirOrLily(level, new BlockPos(x, waterY + 2, z))) {
                    return false;
                }
            }
        }
        return anyWater;
    }

    private boolean isHookInWater(Level level, FishingHook hook) {
        BlockPos pos = hook.blockPosition();
        return isWater(level, pos) || isWater(level, pos.below());
    }

    private boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private boolean isAirOrLily(Level level, BlockPos pos) {
        return level.isEmptyBlock(pos) || level.getBlockState(pos).is(Blocks.LILY_PAD);
    }

    private void judgeIsInOpenWater(Minecraft client) {
        if (client.player == null) {
            return;
        }
        FishingHook hook = client.player.fishing;
        if (hook == null) {
            return;
        }
        if (!isManuallyInOpenWater(client.level, hook)) {
            client.player.sendOverlayMessage(
                    Component.literal("⚠ 非开放水域，无法钓到宝藏").withStyle(ChatFormatting.GOLD)
            );
        }
    }

    private void castRod(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        InteractionHand hand = client.player.getMainHandItem().is(Items.FISHING_ROD)
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        client.gameMode.useItem(client.player, hand);
    }

    private void reelIn(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        InteractionHand hand = client.player.getMainHandItem().is(Items.FISHING_ROD)
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        client.gameMode.useItem(client.player, hand);
    }

    private void reset() {
        state = State.IDLE;
        recastCountdown = 0;
        waitTicks = 0;
        notInWater = false;
    }
}