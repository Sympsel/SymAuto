package com.symauto.function.util;

import com.symauto.mixin.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public final class ContainerScreenUtils {
    private ContainerScreenUtils() {
    }

    public static Slot findSlotUnderMouse(Minecraft client, AbstractContainerScreen<?> cs) {
        double mx = client.mouseHandler.xpos() * (double) cs.width / (double) client.getWindow().getWidth();
        double my = client.mouseHandler.ypos() * (double) cs.height / (double) client.getWindow().getHeight();
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) cs;
        int lx = acc.getLeftPos();
        int ty = acc.getTopPos();
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
