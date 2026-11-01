package com.symbase.function.utils;

import com.symbase.function.base.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

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
}


