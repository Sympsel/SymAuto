package com.symauto.mixin;

import com.symauto.core.FunctionConfig;
import com.symauto.function.container.FasterContainerPanelFunction;
import com.symauto.gui.container.ContainerPanel;
import com.symauto.gui.container.ContainerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
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
    @Shadow
    protected @Nullable Slot hoveredSlot;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void symauto$renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                     float partialTick, CallbackInfo ci) {
        if (!FunctionConfig.FASTER_CONTAINER_PANEL_FUNCTION.isEnable()) {
            return;
        }
        ContainerPanel.render(graphics, leftPos, topPos, imageHeight, mouseX, mouseY);
        ContainerPanel.renderToggleButton(graphics,
                ContainerPanel.toggleX(leftPos, imageWidth),
                ContainerPanel.toggleY(topPos), mouseX, mouseY);
        ContainerPanel.renderHostHighlights(graphics, leftPos, topPos);
        ContainerPanel.handleShiftDragInsert(hoveredSlot);
        if (FasterContainerPanelFunction.INSTANCE.isHighlightContainerContainsThisItem()) {
            ContainerPanel.renderHoveredItemHighlights(graphics, hoveredSlot, leftPos, topPos);
        }
        if (FasterContainerPanelFunction.INSTANCE.isEnableContainerPreview()) {
            ContainerPreviewRenderer.render(graphics, hoveredSlot, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void symauto$onClick(MouseButtonEvent event, boolean doubleClick,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.FASTER_CONTAINER_PANEL_FUNCTION.isEnable()) {
            return;
        }
        double mx = event.x();
        double my = event.y();

        int bx = ContainerPanel.toggleX(leftPos, imageWidth);
        int by = ContainerPanel.toggleY(topPos);
        if (mx >= bx && mx < bx + ContainerPanel.TOGGLE_SIZE
                && my >= by && my < by + ContainerPanel.TOGGLE_HEIGHT) {
            ContainerPanel.toggle();
            cir.setReturnValue(true);
            return;
        }

        if (!ContainerPanel.isVisible()) {
            return;
        }
        boolean shift = (event.modifiers() & 0x1) != 0;
        if (ContainerPanel.handleClick(mx, my, leftPos, topPos, imageHeight, event.button(), shift)) {
            cir.setReturnValue(true);
            return;
        }
        if (event.button() == 0 && (event.modifiers() & 0x1) != 0
                && ContainerPanel.tryShiftInsert(hoveredSlot)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void symauto$onScroll(double mouseX, double mouseY, double scrollX, double scrollY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.FASTER_CONTAINER_PANEL_FUNCTION.isEnable() || !ContainerPanel.isVisible()) {
            return;
        }
        if (ContainerPanel.isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) {
            ContainerPanel.handleScroll(mouseX, mouseY, scrollY, leftPos, topPos, imageHeight);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void symauto$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FunctionConfig.FASTER_CONTAINER_PANEL_FUNCTION.isEnable()
                || !ContainerPanel.isVisible() || !ContainerPanel.isSearchFocused()) {
            return;
        }
        ContainerPanel.handleSearchKey(event.key());
        cir.setReturnValue(true);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void symauto$cancelPreviewTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                              CallbackInfo ci) {
        if (!FunctionConfig.FASTER_CONTAINER_PANEL_FUNCTION.isEnable()) {
            return;
        }
        if (FasterContainerPanelFunction.INSTANCE.isEnableContainerPreview()
                && ContainerPreviewRenderer.isPreviewActive(hoveredSlot)) {
            ci.cancel();
        }
    }
}