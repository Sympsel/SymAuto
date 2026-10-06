package com.betterbundle.mixin;

import com.betterbundle.gui.BundlePanelRenderer;
import com.betterbundle.gui.SortButton;
import com.symauto.function.FeatureConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Final
    @Shadow
    protected int imageHeight;
    @Final
    @Shadow
    protected int imageWidth;

    @Unique
    private static final WidgetSprites BUNDLE_BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("symauto", "bundle_button"),
            Identifier.fromNamespaceAndPath("symauto", "bundle_button_highlighted")
    );
    @Unique
    private static final int BUTTON_SIZE = 20;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void onExtractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        BundlePanelRenderer.render(graphics,
                leftPos,
                topPos,
                imageHeight,
                mouseX, mouseY);

        int bundleSlot = BundlePanelRenderer.getHoveredBundleSlot();
        if (bundleSlot >= 0) {
            Slot slot = self.getMenu().getSlot(bundleSlot);
            if (slot != null && slot.hasItem()) {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(sx + 8, sy + 8);
                float scale = 19f / 16f;
                pose.scale(scale, scale);
                pose.translate(-8, -8);
                graphics.item(slot.getItem(), 0, 0);
                pose.popMatrix();
            }
        }

        renderToggleButton(graphics,
                BundlePanelRenderer.toggleX(leftPos, imageWidth),
                BundlePanelRenderer.toggleY(topPos),
                mouseX, mouseY);

        // 一键整理按钮暂时隐藏（功能有 bug）
        // if (BundlePanelRenderer.isEffectivelyVisible()) {
        //     SortButton.render(graphics, Minecraft.getInstance().font,
        //             leftPos, topPos, imageHeight, mouseX, mouseY);
        // }
    }

    @Unique
    private static void renderToggleButton(GuiGraphicsExtractor graphics, int x, int y,
                                           int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + BUTTON_SIZE
                && mouseY >= y && mouseY < y + BUTTON_SIZE;

        // 根据原版 RecipeBookTabButton 逻辑：用 WidgetSprites.get(true, hovered) 取精灵
        Identifier sprite = BUNDLE_BUTTON_SPRITES.get(true, hovered);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, BUTTON_SIZE, BUTTON_SIZE);
    }
}