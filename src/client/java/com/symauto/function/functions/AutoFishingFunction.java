package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.mixin.FishingHookAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public class AutoFishingFunction extends SymAbstractFunction {
    private static final int RECAST_DELAY_TICKS = 15;
    private static final int CAST_TIMEOUT_TICKS = 900;

    private enum State { IDLE, CAST, WAITING, REELING, RECASTING }

    private State state = State.IDLE;
    private int waitTicks = 0;
    private int recastCountdown = 0;

    public AutoFishingFunction() {
        super("auto_fishing","自动钓鱼", "手持钓鱼杆时自动甩杆收杆，45秒没有鱼会超时重新抛竿");
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
    }
}