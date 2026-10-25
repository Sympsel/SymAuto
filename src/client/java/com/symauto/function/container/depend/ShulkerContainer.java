package com.symauto.function.container.depend;

import com.symauto.function.base.SymContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShulkerContainer implements SymContainer {
    /**
     * 这里的潜影盒物品必须传入原引用而不是副本
     */
    private final ItemStack host;
    private static final int SIZE = 27;

    public ShulkerContainer(ItemStack host) {
        this.host = host;
    }

    private ItemContainerContents contents() {
        return host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private List<ItemStack> slotList() {
        List<ItemStack> slots = new ArrayList<>(SIZE);
        contents().allItemsCopyStream().forEach(slots::add);
        while (slots.size() < SIZE) {
            slots.add(ItemStack.EMPTY);
        }
        return slots;
    }

    @Override
    public Type type() {
        return Type.SHULKER_BOX;
    }

    @Override
    public ItemStack host() {
        return host;
    }

    @Override
    public boolean supportsDirectWrite() {
        // 能修改 host 的数据组件；但是还要经过 commit() 鉴权
        return true;
    }

    @Override
    public int entryCount() {
        return (int) contents().nonEmptyItemCopyStream().count();
    }

    @Override
    public double fillRatio() {
        return (double) entryCount() / SIZE;
    }

    @Override
    public int maxAcceptable(ItemStack toInsert) {
        if (toInsert == null || toInsert.isEmpty()) {
            return 0;
        }
        int max = toInsert.getMaxStackSize();
        int room = 0;
        for (ItemStack slot : slotList()) {
            if (slot.isEmpty()) {
                room += max;
            } else if (ItemStack.isSameItemSameComponents(slot, toInsert)) {
                room += max - slot.count();
            }
        }
        return room;
    }

    @Override
    public boolean canFit(ItemStack toInsert) {
        return maxAcceptable(toInsert) > 0;
    }

    @Override
    public boolean isEmpty() {
        return entryCount() == 0;
    }

    @Override
    public boolean isFull() {
        return entryCount() >= SIZE;
    }

    @Override
    public List<ItemStack> entries() {
        return contents().nonEmptyItemCopyStream().toList();
    }


    @Override
    public Optional<ItemStack> entryAt(int index) {
        List<ItemStack> slots = slotList();
        if (index < 0 || index >= slots.size()) {
            return Optional.empty();
        }
        ItemStack s = slots.get(index);
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    @Override
    public int indexOf(ItemStack probe) {
        if (probe == null || probe.isEmpty()) {
            return -1;
        }
        List<ItemStack> slots = slotList();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, probe)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int countOf(ItemStack probe) {
        int total = 0;
        for (ItemStack s : contents().nonEmptyItemCopyStream().toList()) {
            if (ItemStack.isSameItemSameComponents(s, probe)) {
                total += s.count();
            }
        }
        return total;
    }

    @Override
    public boolean contains(ItemStack probe) {
        return indexOf(probe) >= 0;
    }

    @Override
    public int add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        List<ItemStack> slots = slotList();
        int max = stack.getMaxStackSize();
        int toAdd = stack.count();

        for (int i = 0; i < SIZE && toAdd > 0; i++) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, stack)) {
                continue;
            }
            int move = Math.min(max - slot.count(), toAdd);
            if (move > 0) {
                ItemStack merged = slot.copy();
                merged.grow(move);
                slots.set(i, merged);
                toAdd -= move;
            }
        }

        for (int i = 0; i < SIZE && toAdd > 0; i++) {
            if (!slots.get(i).isEmpty()) {
                continue;
            }
            int move = Math.min(max, toAdd);
            ItemStack placed = stack.copy();
            placed.setCount(move);
            slots.set(i, placed);
            toAdd -= move;
        }

        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return stack.count() - toAdd;
    }

    @Override
    public int remove(ItemStack probe, int count) {
        if (probe == null || probe.isEmpty() || count <= 0) {
            return 0;
        }
        List<ItemStack> slots = slotList();
        int removed = 0;
        for (int i = SIZE - 1; i >= 0 && removed < count; i--) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, probe)) {
                continue;
            }
            int move = Math.min(slot.count(), count - removed);
            if (move >= slot.count()) {
                slots.set(i, ItemStack.EMPTY);
            } else {
                ItemStack left = slot.copy();
                left.shrink(move);
                slots.set(i, left);
            }
            removed += move;
        }
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return removed;
    }

    @Override
    public boolean setEntry(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE) {
            return false;
        }
        List<ItemStack> slots = slotList();
        slots.set(index, stack == null ? ItemStack.EMPTY : stack);
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return true;
    }

    @Override
    public void clear() {
        host.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    @Override
    public boolean commit() {
        if (!canWriteToWorld()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;
        // 从容器中找
        for (Slot slot : menu.slots) {
            if (slot.getItem() == host) {
                slot.set(host);
                slot.container.setChanged();
                return true;
            }
        }

        // 从背包中找
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == host) {
                inv.setItem(i, host);
                inv.setChanged();
                return true;
            }
        }
        return false;
    }

    private boolean canWriteToWorld() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }
}
