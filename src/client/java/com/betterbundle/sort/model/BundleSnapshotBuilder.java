package com.betterbundle.sort.model;

import com.betterbundle.sort.ItemWeights;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 从玩家背包构建整理模型。 */
public final class BundleSnapshotBuilder {

    private BundleSnapshotBuilder() {}

    public static InventoryModel build(Player player) {
        InventoryModel model = new InventoryModel();
        Inventory inv = player.getInventory();
        // 主背包 9..35 优先，再快捷栏 0..8（与现有 findEmptyPlayerSlot 顺序一致）
        for (int i = 9; i < 36; i++) addIfBundle(player, inv, model, i);
        for (int i = 0; i < 9; i++) addIfBundle(player, inv, model, i);
        return model;
    }

    private static void addIfBundle(Player player, Inventory inv, InventoryModel model, int index) {
        ItemStack stack = inv.getItem(index);
        if (!ItemWeights.isBundle(stack)) return;
        int containerSlot = containerSlotOf(player, inv, index);
        model.bags.add(new BagModel(containerSlot, stack));
    }

    private static int containerSlotOf(Player player, Inventory inv, int inventoryIndex) {
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == inv && slot.getContainerSlot() == inventoryIndex) {
                return slot.index;
            }
        }
        return inventoryIndex;
    }
}
