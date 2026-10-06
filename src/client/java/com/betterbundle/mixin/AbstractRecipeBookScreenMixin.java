package com.betterbundle.mixin;

import com.betterbundle.gui.BundleCategory;
import com.betterbundle.gui.BundlePanelRenderer;
import com.betterbundle.gui.SortButton;
import com.betterbundle.sort.exec.SortStateMachine;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    private AbstractContainerScreenAccessor acc() {
        return (AbstractContainerScreenAccessor) this;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        int leftPos = acc().getLeftPos();
        int topPos = acc().getTopPos();
        int imageWidth = acc().getImageWidth();
        int imageHeight = acc().getImageHeight();
        double mouseX = event.x();
        double mouseY = event.y();

        // 一键整理按钮(面板可见时优先;运行中点击即取消)
        // 使用 visible 而非 isEffectivelyVisible,避免配方书关闭过渡帧导致交互失效
        if (BundlePanelRenderer.visible
                && SortButton.handleClick(leftPos, topPos, imageHeight, mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        SortStateMachine.get().abortByUser();

        // 配方书界面:切换按钮
        int bx = BundlePanelRenderer.toggleX(leftPos, imageWidth);
        int by = BundlePanelRenderer.toggleY(topPos);
        if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) {
            BundlePanelRenderer.togglePanel();
            cir.setReturnValue(true);
            return;
        }

        // 非 InventoryScreen 的配方书界面(如工作台):处理分类标签和搜索栏
        // InventoryScreen 的分类检测已由 AbstractContainerScreenMixin 处理，避免重复
        if (!(((Object) this) instanceof InventoryScreen)) {
            if (BundlePanelRenderer.visible) {
                BundleCategory cat = BundlePanelRenderer.getCategoryAt(mouseX, mouseY, leftPos, topPos, imageHeight);
                if (cat != null) {
                    BundlePanelRenderer.currentCategory = cat;
                    BundlePanelRenderer.searchQuery = "";
                    BundlePanelRenderer.scrollToTop();
                    cir.setReturnValue(true);
                    return;
                }

                if (BundlePanelRenderer.isInsideSearchBar(mouseX, mouseY, leftPos, topPos, imageHeight)) {
                    BundlePanelRenderer.searchFocused = true;
                    cir.setReturnValue(true);
                    return;
                }
            }

            BundlePanelRenderer.searchFocused = false;
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        SortStateMachine.get().abortByUser();
    }
}