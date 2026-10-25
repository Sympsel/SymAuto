package com.symauto.function.container.depend;

import com.symauto.function.utils.BundleContentsHelper;
import com.symauto.function.utils.ClientGuard;
import com.symauto.function.utils.ContainerActions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BundleAccess {
    private BundleAccess() {
    }

    /**
     * 原版取出：选中袋内 {@code itemIndex} 项物品后右键袋槽位，取出整项物品到光标
     *
     * @param bundleSlot 收纳袋槽位
     * @param itemIndex  收纳袋内物品索引
     */
    public static void extract(int bundleSlot, int itemIndex) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (!ClientGuard.ready(client)) {
            return;
        }
        client.getConnection().send(new ServerboundSelectBundleItemPacket(bundleSlot, -1));
        client.getConnection().send(new ServerboundSelectBundleItemPacket(bundleSlot, itemIndex));
        ContainerActions.rightClickSlot(bundleSlot);
    }

    /**
     * 原版存入：拿起 {@code itemSlot} 整叠 → 点袋槽由服务端自动吸收 → 余量放回原槽
     */
    public static void insert(int itemSlot, int bundleSlot) {
        ContainerActions.leftClickSlot(itemSlot);
        ContainerActions.leftClickSlot(bundleSlot);
        ContainerActions.leftClickSlot(itemSlot);
    }

    public static int findBestBundle(Player player, ItemStack stack, int excludeSlot) {
        int fallback = -1;
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != player.getInventory() || slot.index == excludeSlot || !slot.hasItem()) {
                continue;
            }
            ItemStack bundle = slot.getItem();
            if (!BundleContentsHelper.isBundle(bundle)) {
                continue;
            }
            if (BundleContentsHelper.maxAcceptable(bundle, stack) <= 0) {
                continue;
            }
            if (BundleContentsHelper.sameItemCount(bundle, stack) > 0) {
                return slot.index;
            }
            if (fallback < 0) {
                fallback = slot.index;
            }
        }
        return fallback;
    }

    /**
     * 光标已有物品时
     */
    public static void insertCarried(int bundleSlot) {
        ContainerActions.leftClickSlot(bundleSlot);
    }

    /**
     * Shift + 左键 直接取出到背包
     */
    public static void extractToInventory(int bundleSlot, int itemIndex, ItemStack probe) {
        extract(bundleSlot, itemIndex);
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        int dest = findDestination(client.player, probe);
        if (dest >= 0) {
            ContainerActions.leftClickSlot(dest);
        }
    }

    private static int findDestination(Player player, ItemStack probe) {
        Inventory inv = player.getInventory();
        int empty = -1;
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != inv || !slot.mayPlace(probe)) {
                continue;
            }
            ItemStack s = slot.getItem();
            if (s.isEmpty()) {
                if (empty < 0) {
                    empty = slot.index;
                }
            } else if (ItemStack.isSameItemSameComponents(s, probe)
                    && s.getCount() < s.getMaxStackSize()) {
                return slot.index;
            }
        }
        return empty;
    }
}
