package com.symauto.mixin;

import com.symauto.function.FeatureConfig;
import com.symauto.function.functions.FixYPlaceOrDestroyFunction;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiPlayerGameMode.class, priority = 900)
public class FixYPlaceMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand,
                             BlockHitResult blockHit,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!FeatureConfig.FIX_Y_PLACE_OR_DESTROY_FUNCTION.isEnable()) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        int placeY = blockHit.getBlockPos().getY() + blockHit.getDirection().getStepY();

        // 如果还没进入 holding 状态，说明这是本次长按的第一次放置
        if (!FixYPlaceOrDestroyFunction.isHolding()) {
            FixYPlaceOrDestroyFunction.startHold(placeY);
            // 放行第一次
            return;
        }

        // 已处于 holding，检查是否匹配
        if (placeY != FixYPlaceOrDestroyFunction.getLockedY()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroy(BlockPos pos, Direction direction,
                                CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.FIX_Y_PLACE_OR_DESTROY_FUNCTION.isEnable()) return;

        int targetY = pos.getY();

        if (!FixYPlaceOrDestroyFunction.isMining()) {
            // 本次左键的第一次，记录 Y
            FixYPlaceOrDestroyFunction.startMining(targetY);
            return; // 放行
        }

        // 已经在挖掘中，检查是否匹配
        if (targetY != FixYPlaceOrDestroyFunction.getMineLockedY()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroy(BlockPos pos, Direction direction,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.FIX_Y_PLACE_OR_DESTROY_FUNCTION.isEnable()) return;
        if (!FixYPlaceOrDestroyFunction.isMining()) return;

        if (pos.getY() != FixYPlaceOrDestroyFunction.getMineLockedY()) {
            cir.setReturnValue(false);
        }
    }
}