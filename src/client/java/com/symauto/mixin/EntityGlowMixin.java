package com.symauto.mixin;

import com.symauto.function.utils.EntityGlowRegistry;
import com.symauto.function.utils.GlowTeam;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing()Z", at = @At("HEAD"), cancellable = true)
    private void symauto$forceGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (EntityGlowRegistry.isGlowing(self.getUUID())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeam()Lnet/minecraft/world/scores/PlayerTeam;", at = @At("HEAD"), cancellable = true)
    private void symauto$glowTeam(CallbackInfoReturnable<PlayerTeam> cir) {
        Entity self = (Entity) (Object) this;
        if (EntityGlowRegistry.isGlowing(self.getUUID())) {
            TeamColor color = EntityGlowRegistry.colorOf(self.getUUID());
            if (color != null) {
                PlayerTeam team = GlowTeam.get(color);
                if (team != null) {
                    cir.setReturnValue(team);
                }
            }
        }
    }
}