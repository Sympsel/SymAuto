package com.betterbundle.mixin;

import com.betterbundle.gui.BundleCategory;
import com.betterbundle.gui.BundlePanelInteraction;
import com.betterbundle.gui.BundlePanelRenderer;
import com.betterbundle.gui.SortButton;
import com.betterbundle.sort.exec.SortStateMachine;
import com.symauto.function.FeatureConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Final
    @Shadow
    protected int imageWidth;
    @Final
    @Shadow
    protected int imageHeight;
    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        double mx = event.x();
        double my = event.y();

        // 一键整理按钮（面板可见时优先；运行中点击即取消）
        // 使用 visible 而非 isEffectivelyVisible，确保配方书关闭的过渡帧也能响应
        if (BundlePanelRenderer.visible
                && SortButton.handleClick(leftPos, topPos, imageHeight, mx, my)) {
            cir.setReturnValue(true);
            return;
        }

        // 玩家任意输入 → 立即中止整理（不回滚，D4）
        SortStateMachine.get().abortByUser();

        // Bulk-insert: space+left anywhere starts the timer (0.05s to activate)
        if (event.button() == 0 && isSpaceDown()) {
            BundlePanelInteraction.startBulkInsert();
        }

        // Space+Click works on ALL container screens
        Slot hovered = hoveredSlot;
        if (hovered != null && hovered.hasItem()) {
            boolean handled = BundlePanelInteraction.handleSpaceClick(hovered);
            if (handled) {
                cir.setReturnValue(true);
                return;
            }
        }

        // 切换按钮对所有容器界面生效（含配方书/背包界面）
        int bx = BundlePanelRenderer.toggleX(leftPos, imageWidth);
        int by = BundlePanelRenderer.toggleY(topPos);
        if (mx >= bx && mx < bx + 20 && my >= by && my < by + 20) {
            BundlePanelRenderer.togglePanel();
            cir.setReturnValue(true);
            return;
        }

        // 分类标签和搜索栏对所有容器界面生效
        if (BundlePanelRenderer.visible) {
            BundleCategory cat = BundlePanelRenderer.getCategoryAt(mx, my, leftPos, topPos, imageHeight);
            if (cat != null) {
                BundlePanelRenderer.currentCategory = cat;
                BundlePanelRenderer.searchQuery = "";
                BundlePanelRenderer.scrollToTop();
                cir.setReturnValue(true);
                return;
            }

            if (BundlePanelRenderer.isInsideSearchBar(mx, my, leftPos, topPos, imageHeight)) {
                BundlePanelRenderer.searchFocused = true;
                cir.setReturnValue(true);
                return;
            }
        }
        BundlePanelRenderer.searchFocused = false;

        if (!BundlePanelRenderer.visible) {
            return;
        }

        // Cursor has items + click anywhere in panel (except category buttons) → insert
        ItemStack cursor = self.getMenu().getCarried();
        if (!cursor.isEmpty() && isInsidePanelBounds(mx, my, leftPos, topPos, imageHeight)) {
            BundleCategory cat = BundlePanelRenderer.getCategoryAt(mx, my, leftPos, topPos, imageHeight);
            if (cat == null) {
                boolean handled = BundlePanelInteraction.handlePanelInsert(event.button());
                if (handled) {
                    cir.setReturnValue(true);
                }
            }
        }

        if (BundlePanelInteraction.isInsidePanel(mx, my, leftPos, topPos, imageHeight)) {
            if (cursor.isEmpty()) {
                boolean handled = BundlePanelInteraction.handlePanelClick(
                        mx, my, event.button(), event.modifiers(), leftPos, topPos, self);
                if (handled) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    private static boolean isInsidePanelBounds(double mx, double my, int leftPos, int topPos, int imageHeight) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        int panelY = topPos;
        int searchH = BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3;
        int gridH = BundlePanelRenderer.PADDING * 2
                + BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        int panelH = Math.min(imageHeight, searchH + gridH) + 16;
        return mx >= panelX && mx <= panelX + pw && my >= panelY && my <= panelY + panelH;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        BundlePanelInteraction.stopBulkInsert();
        lastBulkSlot = -1;
        if (!BundlePanelRenderer.visible) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (BundlePanelInteraction.isInsidePanel(event.x(), event.y(),
                leftPos, topPos, imageHeight)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        SortStateMachine.get().abortByUser();
        if (BundlePanelRenderer.searchFocused) {
            BundlePanelRenderer.onSearchKeyPress(event.key());
            cir.setReturnValue(true);
        }
    }

    private int lastBulkSlot = -1;

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(MouseButtonEvent event, double dx, double dy,
                                CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        SortStateMachine.get().abortByUser();
        if (!BundlePanelInteraction.isBulkInsertActive()) return;
        if (!isSpaceDown()) {
            BundlePanelInteraction.stopBulkInsert();
            return;
        }

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Slot hovered = hoveredSlot;
        if (hovered != null && hovered.hasItem() && hovered.index != lastBulkSlot) {
            lastBulkSlot = hovered.index;
            BundlePanelInteraction.handleSpaceClick(hovered);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()) {
            return;
        }
        SortStateMachine.get().abortByUser();
        if (!BundlePanelRenderer.visible) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY,
                leftPos, topPos, imageHeight)) {
            boolean handled = BundlePanelInteraction.handleScroll(mouseX, mouseY, scrollY,
                    leftPos, topPos, imageHeight);
            if (handled) cir.setReturnValue(true);
        }
    }

    private static boolean isSpaceDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
    }
}
