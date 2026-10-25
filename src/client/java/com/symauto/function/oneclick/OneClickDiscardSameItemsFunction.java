package com.symauto.function.oneclick;

import com.symauto.config.option.BooleanOption;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.core.Constants;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import com.symauto.gui.container.ContainerPanel;
import com.symauto.mixin.accessor.AbstractContainerScreenAccessor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OneClickDiscardSameItemsFunction extends SymAbstractFunction {
    public static final OneClickDiscardSameItemsFunction INSTANCE = new OneClickDiscardSameItemsFunction();

    @Getter
    @Setter
    private boolean compareWithDataComponent;

    private OneClickDiscardSameItemsFunction() {
        super("one_click_discard_same_items", "一键丢出相同物品");
        addOption(new BooleanOption("compareWithDataComponent", "比较物品数据组件",
                this::isCompareWithDataComponent, this::setCompareWithDataComponent, false));
    }

    @Override
    protected String tooltip() {
       return Tooltip.create().line("一键丢弃容器和背包的相同物品，不丢副手（只需要一件的物品可以放副手当黑名单）")
               .line(ChatFormatting.AQUA, "触发方式：选中其中一组拖拽式丢弃")
               .keyValueLine(ChatFormatting.YELLOW, "比较物品数据组件",
                       ChatFormatting.GRAY, compareWithDataComponent ? "是" : "否")
               .line("在比较数据组件模式关闭时，对于潜影盒，当盒内只有一种物品且两盒物品种类相同时视作同一物品")
               .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    @Override
    public boolean allowContainerMouseClick(Minecraft client,
                                            AbstractContainerScreen<?> cs,
                                            double mouseX, double mouseY, int button) {
        AbstractContainerMenu menu = cs.getMenu();
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return true;

        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) cs;
        int xo = acc.getLeftPos(), yo = acc.getTopPos();
        int imgW = acc.getImageWidth(), imgH = acc.getImageHeight();
        boolean outside = mouseX < xo || mouseY < yo || mouseX >= xo + imgW || mouseY >= yo + imgH;
        if (!outside) return true;

        if (FunctionConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                && ContainerPanel.isVisible()
                && ContainerPanel.isInsidePanel(mouseX, mouseY, xo, yo, imgH)) {
            return true;
        }

        if (cs instanceof CreativeModeInventoryScreen) {
            return true;
        }

        ItemStack target = carried.copy();
        client.gameMode.handleContainerInput(menu.containerId, -999, 0, ContainerInput.PICKUP, client.player);

        if (menu == client.player.inventoryMenu) {
            // 背包界面：跳过副手，丢弃主背包 + 快捷栏里相同物品
            for (int i = Constants.MAIN_INVENTORY_START; i < menu.slots.size(); i++) {
                if (i == Constants.OFFHAND) continue;
                Slot slot = menu.getSlot(i);
                if (slot.hasItem() && ItemUtils.isSameItemWithoutDataComponent(target, slot.getItem())) {
                    client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                }
            }
        } else {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.getSlot(i);
                if (slot.hasItem() && ItemUtils.isSameItemWithoutDataComponent(target, slot.getItem())) {
                    client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                }
            }
        }
        return false;
    }
}
