package com.symauto.mixin;

import com.symbase.SymBaseClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 容器界面内的快捷键分发
 *
 * <p>原版 {@code KeyboardHandler} 只在没有界面打开时才会把按键派发给 {@code KeyMapping}，
 * 界面打开期间 {@code KeyMapping#consumeClick()} 永远为空，因此只依赖 KeyMapping 的功能
 * （例如“一键平铺物品”的 Ctrl + A）在容器界面里完全无法触发。
 * 这里在按键进入界面处理之前补一次分发，命中后取消原版处理，避免按键字符落进搜索框等控件。</p>
 */
@Mixin(KeyboardHandler.class)
public class ContainerScreenKeyMixin {

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("HEAD"), cancellable = true)
    private void symauto$dispatchContainerKey(long window, int action, KeyEvent event,
                                              CallbackInfo ci) {
        // 只处理按下，忽略长按重复，避免功能被连续触发
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (SymBaseClient.handleContainerKeyPress(client, screen, event)) {
            ci.cancel();
        }
    }
}
