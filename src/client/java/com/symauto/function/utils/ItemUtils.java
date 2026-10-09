package com.symauto.function.utils;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ItemUtils {
    private ItemUtils() {
    }

    /**
     * 判断是否是同种物品，不包含数据组件或NBT
     */
    public static boolean isSameItem(ItemStack a, ItemStack b) {
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
    public static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * 两个潜影盒内容是否“同一种物品 -- 两边各自内部只出现一种物品，且两种物品相同。
     */
    public static boolean isSameShulkerContent(ItemStack a, ItemStack b) {
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
    public static ItemStack getSingleContentType(ItemStack shulker) {
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

    /**
     * 判断容器中是否存在与 slot0 相同的物品
     */
    public static boolean containsInContainer(Slot slot0, AbstractContainerMenu menu) {
        int containerSlotCount = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
        for (int i = 0; i < containerSlotCount; ++i) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && isSameItem(slot.getItem(), slot0.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断玩家背包（最后 36 个槽位）中是否存在与 slot0 相同的物品
     */
    public static boolean containsInInventory(Slot slot0, AbstractContainerMenu menu) {
        int containerSlotCount = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
        for (int i = containerSlotCount; i < menu.slots.size(); ++i) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && isSameItem(slot.getItem(), slot0.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断附魔是否可以应用在物品上
     */
    public static boolean isEnchantmentCanBeAppliedToItem(Enchantment enchantment, Item item) {
        if (item == null) {
            return false;
        }
        Enchantment.EnchantmentDefinition def = enchantment.definition();
        HolderSet<Item> supportedItem = def.supportedItems();
        return supportedItem.contains(BuiltInRegistries.ITEM.wrapAsHolder(item));
    }
}
