package com.symauto.mixin;

import com.symauto.function.FeatureConfig;
import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft self = (Minecraft)(Object)this;
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            if (f.isEnable()) {
                f.tick();
            }
        }
    }
}
