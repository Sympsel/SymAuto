package com.symauto.mixin;

import com.symauto.function.place.SpeedBridgeFunction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tweakeroo 式伪潜行
 */
@Mixin(Player.class)
public abstract class FakeSneakMixin {
    @Shadow
    protected abstract boolean isStayingOnGroundSurface();

    @Redirect(
            method = "maybeBackOffFromEdge",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isStayingOnGroundSurface()Z"
            )
    )
    private boolean symauto$fakeSneak(Player player) {
        if (player instanceof LocalPlayer
                && SpeedBridgeFunction.INSTANCE.isEnable()
                && SpeedBridgeFunction.INSTANCE.isEnableFakeSneak()) {
            return true;
        }
        return this.isStayingOnGroundSurface();
    }
}

