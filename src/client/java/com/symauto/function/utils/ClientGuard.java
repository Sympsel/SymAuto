package com.symauto.function.utils;

import net.minecraft.client.Minecraft;

public class ClientGuard {
    private ClientGuard() {}

    public static boolean ready(Minecraft client) {
        return client.player != null && client.level != null && client.gameMode != null;
    }
}
