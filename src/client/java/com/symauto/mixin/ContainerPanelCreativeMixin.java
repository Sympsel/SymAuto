package com.symauto.mixin;

import com.symauto.core.FunctionConfig;
import com.symauto.gui.container.ContainerPanel;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class ContainerPanelCreativeMixin {

    /**
     * 自定义搜索框聚焦时，阻止创造界面把字符键劫持到"搜索物品"分类
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void symauto$guardCreativeKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                || !ContainerPanel.isVisible() || !ContainerPanel.isSearchFocused()) {
            return;
        }
        if (event.key() == GLFW.GLFW_KEY_T) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void symauto$guardCreativeChars(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                || !ContainerPanel.isVisible() || !ContainerPanel.isSearchFocused()) {
            return;
        }
        ContainerPanel.handleCharTyped(event.codepoint());
        cir.setReturnValue(true);
    }
}