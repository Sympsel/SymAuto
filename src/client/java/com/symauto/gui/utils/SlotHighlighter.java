package com.symauto.gui.utils;

import com.symauto.function.utils.ClientGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class SlotHighlighter {
    private static final int SLOT_SIZE = 18; // 槽位尺寸


    public static void highlightInventorySlots(GuiGraphicsExtractor g, int leftPos, int topPos,
                                               Set<Integer> invIndices, int color) {
        if (invIndices == null || invIndices.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        Player player = client.player;
        for (Slot slot : player.containerMenu.slots) {
            if (!(slot.container instanceof Inventory)) {
                continue;
            }
            int idx = slot.getContainerSlot();
            if (idx < 0 || idx >= 36 || !invIndices.contains(idx)) {
                continue;
            }
            if (slot.getItem().isEmpty()) {
                continue;
            }
            frameSlot(g, slot.x + leftPos, slot.y + topPos, color);
        }
    }

    /**
     * 框住指定槽位
     */
    public static void highlightSlot(GuiGraphicsExtractor g, int leftPos, int topPos, Slot slot, int color) {
        if (slot == null) {
            return;
        }
        frameSlot(g, slot.x + leftPos, slot.y + topPos, color);
    }

    /**
     * 批量指定
     */
    public static void highlightSlots(GuiGraphicsExtractor g, int leftPos, int topPos, Collection<Slot> slots, int color) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (Slot slot : slots) {
            highlightSlot(g, leftPos, topPos, slot, color);
        }
    }

    /**
     * 框住指定菜单的指定槽位
     */
    public static void highlightMenuSlots(GuiGraphicsExtractor g, int leftPos, int topPos, Set<Integer> menuIndices, int color) {
        if (menuIndices == null || menuIndices.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        AbstractContainerMenu menu = client.player.containerMenu;
        List<Slot> slots = menu.slots;
        for (Integer boxed : menuIndices) {
            if (boxed == null) {
                continue;
            }
            int idx = boxed;
            if (idx < 0 || idx >= slots.size()) {
                continue;
            }
            highlightSlot(g, leftPos, topPos, slots.get(idx), color);
        }
    }

    public static void highlightSlot(GuiGraphicsExtractor g, int slotX, int slotY, int bgColor, int frameColor) {
        int hx0 = slotX - 1;
        int hy0 = slotY - 1;
        int hx1 = slotX + SLOT_SIZE - 1;
        int hy1 = slotY + SLOT_SIZE - 1;
        g.fill(hx0, hy0, hx1, hy1, bgColor);
        g.fill(hx0, hy0, hx1, hy0 + 1, frameColor);
        g.fill(hx0, hy1 - 1, hx1, hy1, frameColor);
        g.fill(hx0, hy0, hx0 + 1, hy1, frameColor);
        g.fill(hx1 - 1, hy0, hx1, hy1, frameColor);
    }

    public static void fillSlot(GuiGraphicsExtractor g, int slotX, int slotY, int color) {
        int hx0 = slotX - 1;
        int hy0 = slotY - 1;
        int hx1 = slotX + SLOT_SIZE - 1;
        int hy1 = slotY + SLOT_SIZE - 1;
        g.fill(hx0, hy0, hx1, hy1, color);
    }

    public static void frameSlot(GuiGraphicsExtractor g, int slotX, int slotY, int color) {
        int hx0 = slotX - 1;
        int hy0 = slotY - 1;
        int hx1 = slotX + SLOT_SIZE - 1;
        int hy1 = slotY + SLOT_SIZE - 1;
        g.fill(hx0, hy0, hx1, hy0 + 1, color);
        g.fill(hx0, hy1 - 1, hx1, hy1, color);
        g.fill(hx0, hy0, hx0 + 1, hy1, color);
        g.fill(hx1 - 1, hy0, hx1, hy1, color);
    }
}
