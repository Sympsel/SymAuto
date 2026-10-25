package com.symauto.mixin;

import com.symauto.core.FunctionConfig;
import com.symauto.gui.container.ContainerPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerPanelScreenMixin {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    @Final
    protected int imageWidth;
    @Shadow
    @Final
    protected int imageHeight;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void symauto$renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                     float partialTick, CallbackInfo ci) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        ContainerPanel.render(graphics, leftPos, topPos, imageHeight, mouseX, mouseY);
        ContainerPanel.renderToggleButton(graphics,
                ContainerPanel.toggleX(leftPos, imageWidth),
                ContainerPanel.toggleY(topPos), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void symauto$onClick(MouseButtonEvent event, boolean doubleClick,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        double mx = event.x();
        double my = event.y();

        int bx = ContainerPanel.toggleX(leftPos, imageWidth);
        int by = ContainerPanel.toggleY(topPos);
        if (mx >= bx && mx < bx + ContainerPanel.TOGGLE_SIZE
                && my >= by && my < by + ContainerPanel.TOGGLE_SIZE) {
            ContainerPanel.toggle();
            cir.setReturnValue(true);
            return;
        }

        if (!ContainerPanel.isVisible()) {
            return;
        }
        if (ContainerPanel.handleClick(mx, my, leftPos, topPos, imageHeight, event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void symauto$onScroll(double mouseX, double mouseY, double scrollX, double scrollY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable() || !ContainerPanel.isVisible()) {
            return;
        }
        if (ContainerPanel.isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) {
            ContainerPanel.handleScroll(mouseX, mouseY, scrollY, leftPos, topPos, imageHeight);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void symauto$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                || !ContainerPanel.isVisible() || !ContainerPanel.isSearchFocused()) {
            return;
        }
        ContainerPanel.handleSearchKey(event.key());
        cir.setReturnValue(true);
    }
}