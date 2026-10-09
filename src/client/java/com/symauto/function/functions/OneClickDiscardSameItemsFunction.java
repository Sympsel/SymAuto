package com.symauto.function.functions;

import com.betterbundle.gui.BundlePanelInteraction;
import com.betterbundle.gui.BundlePanelRenderer;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Constants;
import com.symauto.function.utils.ItemUtils;
import com.symauto.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OneClickDiscardSameItemsFunction extends SymAbstractFunction {
    public static final OneClickDiscardSameItemsFunction INSTANCE = new OneClickDiscardSameItemsFunction();

    private OneClickDiscardSameItemsFunction() {
        super("one_click_discard_same_items", "一键丢出相同物品",
                "一键丢弃容器和背包的相同物品，触发方式：选中其中一组拖拽式丢弃，不丢副手（只需要一件的物品可以放副手当黑名单）\n对于潜影盒，当盒内只有一种物品且两盒物品相同时视作同一物品");
    }

    @Override
    protected void onTrigger(Minecraft client) {}

    @Override
    public boolean allowContainerMouseClick(Minecraft client,
                                            AbstractContainerScreen<?> cs,
                                            double mouseX, double mouseY, int button) {
        if (client.player == null || client.gameMode == null) return true;

        AbstractContainerMenu menu = cs.getMenu();
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return true;

        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) cs;
        int xo = acc.getLeftPos(), yo = acc.getTopPos();
        int imgW = acc.getImageWidth(), imgH = acc.getImageHeight();
        boolean outside = mouseX < xo || mouseY < yo || mouseX >= xo + imgW || mouseY >= yo + imgH;
        if (!outside) return true;

        if (FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                && BundlePanelRenderer.visible
                && BundlePanelInteraction.isInsidePanel(mouseX, mouseY, xo, yo, imgH)) {
            return true;
        }

        ItemStack target = carried.copy();
        client.gameMode.handleContainerInput(menu.containerId, -999, 0, ContainerInput.PICKUP, client.player);

        if (menu == client.player.inventoryMenu) {
            // 背包界面：跳过副手，丢弃主背包 + 快捷栏里相同物品
            for (int i = Constants.MAIN_INVENTORY_START; i < menu.slots.size(); i++) {
                if (i == Constants.OFFHAND) continue;
                Slot slot = menu.getSlot(i);
                if (slot.hasItem() && ItemUtils.isSameItem(target, slot.getItem())) {
                    client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                }
            }
        } else {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.getSlot(i);
                if (slot.hasItem() && ItemUtils.isSameItem(target, slot.getItem())) {
                    client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                }
            }
        }
        return false;
    }
}
