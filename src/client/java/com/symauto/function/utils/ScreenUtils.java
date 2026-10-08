package com.symauto.function.utils;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public class ScreenUtils {
    private ScreenUtils() {
    }

    public static boolean isContainerScreen(Minecraft client) {
        return client.gui.screen() instanceof AbstractContainerScreen<?>;
    }

    public static boolean isNoScreen(Minecraft client) {
        return client.gui.screen() == null;
    }

    public static boolean screenContextMatches(Minecraft client, SymAbstractFunction.ScreenContext ctx) {
        return switch (ctx) {
            case NO_SCREEN -> isNoScreen(client);
            case ANY_SCREEN -> !isNoScreen(client);
            case CONTAINER_SCREEN -> isContainerScreen(client);
        };
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
