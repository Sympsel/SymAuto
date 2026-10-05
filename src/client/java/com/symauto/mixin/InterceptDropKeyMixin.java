package com.symauto.mixin;

import com.symauto.function.functions.OneClickDiscardItems;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class InterceptDropKeyMixin {

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("HEAD"), cancellable = true)
    private void symauto$interceptDropKey(long window, int action, KeyEvent event,
                                          CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) {
            return;
        }
        // 从 KeyEvent 里取 key 和 modifiers
        if (OneClickDiscardItems.shouldInterceptVanillaKey(event.key(), event.modifiers())) {
            ci.cancel();
        }
    }
}