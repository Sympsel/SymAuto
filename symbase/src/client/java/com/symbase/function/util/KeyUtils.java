package com.symbase.function.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyUtils {
    private KeyUtils() {
    }

    private static final Map<String, Boolean> edgeState = new HashMap<>();

    public static boolean isCtrlDown(Minecraft client) {
        return isAnyDown(client, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isShiftDown(Minecraft client) {
        return isAnyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isAltDown(Minecraft client) {
        return isAnyDown(client, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isAnyDown(Minecraft client, int... keys) {
        var window = client.getWindow();
        for (int k : keys) {
            if (InputConstants.isKeyDown(window, k)) return true;
        }
        return false;
    }

    /**
     * 创建一个没有默认按键的键位
     */
    public static KeyMapping createUnbound(String translationKey, KeyMapping.Category category) {
        return new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category);
    }

    /**
     * 上升沿检测
     */
    public static boolean risingEdge(Minecraft client, String id, KeyMapping km) {
        if (km.isUnbound()) {
            edgeState.put(id, false);
            return false;
        }
        return km.consumeClick();
    }

    public static void resetEdge(String id) {
        edgeState.remove(id);
    }
}


