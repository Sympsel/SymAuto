package com.symauto.function.utils;

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
     * 上升沿检测：
     * - KEYSYM：读 GLFW 物理状态（不受 Screen 消费影响）
     * - MOUSE：走 clickCount（一次点击天然是一次事件）
     * 内部按 id 记录上一次状态，同一 id 从 false 变 true 只返回一次 true。
     */
    public static boolean risingEdge(Minecraft client, String id, KeyMapping km) {
        InputConstants.Key bind = km.getDefaultKey();
        int code = bind.getValue();
        if (code < 0) return false;

        boolean nowDown;
        if (bind.getType() == InputConstants.Type.KEYSYM) {
            nowDown = InputConstants.isKeyDown(client.getWindow(), code);
        } else if (bind.getType() == InputConstants.Type.MOUSE) {
            nowDown = km.consumeClick();
        } else {
            nowDown = km.isDown();
        }

        boolean prev = edgeState.getOrDefault(id, false);
        edgeState.put(id, nowDown);
        return nowDown && !prev;
    }

    public static void resetEdge(String id) {
        edgeState.remove(id);
    }
}
