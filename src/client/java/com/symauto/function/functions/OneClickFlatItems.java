package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Constants;
import com.symauto.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class OneClickFlatItems extends SymAbstractFunction {
    public static final OneClickFlatItems INSTANCE = new OneClickFlatItems();

    private OneClickFlatItems() {
        String tooltip = "鼠标悬浮在要平铺的物品上，按下 Ctrl + A 进行平铺\n如果指针在容器界面则铺满容器\n如果在背包界面则平涂背包，然后提起剩余物品";
        super("one_click_flat_items", "一键平铺物品", tooltip);
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    public void trigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> cs)) {
            return;
        }
        AbstractContainerMenu menu = cs.getMenu();
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        Slot hovered = findSlotUnderMouse(client, cs);
        if (hovered == null || !hovered.hasItem()) {
            return;
        }

        int regionStart, regionEnd;

        boolean inventoryMenu = (menu == client.player.inventoryMenu);
        if (inventoryMenu) {
            regionStart = Constants.MAIN_INVENTORY_START;
            regionEnd = Constants.HOTBAR_END_IN_INVENTORY;
        } else {
            regionStart = 0;
            regionEnd = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = regionStart; i < regionEnd; i++) {
            if (i == hovered.index) {
                // PICKUP 后一定为空槽
                slots.add(i);
                continue;
            }
            if (menu.getSlot(i).getItem().isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return;
        }

        int cid = menu.containerId;

        client.gameMode.handleContainerInput(cid, hovered.index, 0, ContainerInput.PICKUP, client.player);

        for (int idx : slots) {
            if (menu.getCarried().isEmpty()) break;
            Slot s = menu.getSlot(idx);
            if (s.getItem().isEmpty()) {
                client.gameMode.handleContainerInput(cid, idx, 1, ContainerInput.PICKUP, client.player);
            }
        }
    }

    private static AbstractContainerScreenAccessor acc(AbstractContainerScreen<?> cs) {
        return (AbstractContainerScreenAccessor) cs;
    }

    private static Slot findSlotUnderMouse(Minecraft client, AbstractContainerScreen<?> cs) {
        double mx = client.mouseHandler.xpos() * (double) cs.width / (double) client.getWindow().getWidth();
        double my = client.mouseHandler.ypos() * (double) cs.height / (double) client.getWindow().getHeight();
        int lx = acc(cs).getLeftPos();
        int ty = acc(cs).getTopPos();
        for (Slot slot : cs.getMenu().slots) {
            int sx = lx + slot.x;
            int sy = ty + slot.y;
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                return slot;
            }
        }
        return null;
    }
}
