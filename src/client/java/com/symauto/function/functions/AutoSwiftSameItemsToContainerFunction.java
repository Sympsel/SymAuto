package com.symauto.function.functions;

import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class AutoSwiftSameItemsToContainerFunction extends SymAbstractFunction {
    private static final int PLAYER_SLOT_COUNT = 36;
    private boolean executed = false;

    public AutoSwiftSameItemsToContainerFunction() {
        super("自动转移相同物品到容器", "打开容器会将背包内的而且存在于容器的物品尽可能填满容器\n对于潜影盒，当盒内只有一种物品且两盒物品相同时视作同一物品");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            executed = false;
            return;
        }
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            executed = false;
            return;
        }
        if (executed) {
            return;
        }
        executed = true;
        int containerId = menu.containerId;
        int containerSlotCount = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (containerSlotCount <= 0) {
            return;
        }

        for (int i = containerSlotCount; i < menu.slots.size(); ++i) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            if (containsInContainer(slot, menu)) {
                client.gameMode.handleContainerInput(
                        containerId,
                        i,
                        0,
                        ContainerInput.QUICK_MOVE,
                        client.player
                );
            }
        }
    }

    private boolean isSameItem(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItem(a, b)) {
            return false;
        }
        // 同种物品且不是潜影盒，直接视为相同
        if (!isShulkerBox(a)) {
            return true;
        }
        // 都是潜影盒
        return isSameShulkerContent(a, b);
    }

    /**
     * 判断物品是否是潜影盒
     */
    private boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * 两个潜影盒内容是否“同一种物品 -- 两边各自内部只出现一种物品，且两种物品相同。
     */
    private boolean isSameShulkerContent(ItemStack a, ItemStack b) {
        ItemStack onlyA = getSingleContentType(a);
        ItemStack onlyB = getSingleContentType(b);
        if (onlyA == null || onlyB == null) {
            // 任意盒子为空或者内部物品种类超过一种，不视作相同物品
            return false;
        }
        return ItemStack.isSameItemSameComponents(onlyA, onlyB);
    }

    /**
     * 判断潜影盒内是否只有一种物品，如果是，返回一个代表栈，否则返回 null
     */
    private ItemStack getSingleContentType(ItemStack shulker) {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) {
            return null;
        }
        ItemStack found = null;
        for (ItemStack inner : contents.nonEmptyItemCopyStream().toList()) {
            if (found == null) {
                found = inner;
            } else if (!ItemStack.isSameItemSameComponents(found, inner)) {
                // 出现第二种物品
                return null;
            }
        }
        return found;
    }

    private boolean containsInContainer(Slot slot0, AbstractContainerMenu menu) {
        int containerSlotCount = menu.slots.size() - PLAYER_SLOT_COUNT;
        for (int i = 0; i < containerSlotCount; ++i) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && isSameItem(slot.getItem(), slot0.getItem())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDisable() {
        executed = false;
    }
}
