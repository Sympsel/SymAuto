package com.symauto.mixin;

import com.symauto.gui.container.ContainerPanel;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 鼠标手势兼容
 */
@Mixin(targets = "yalter.mousetweaks.handlers.GuiContainerHandler")
public class MouseTweaksCompatMixin {
    @Inject(method = "getSlotUnderMouse", at = @At("HEAD"), cancellable = true)
    private void symauto$hideSlotFromMouseTweaks(double mouseX, double mouseY,
                                                 CallbackInfoReturnable<Slot> cir) {
        if (ContainerPanel.isShiftDragActive()) {
            cir.setReturnValue(null);
        }
    }
}
