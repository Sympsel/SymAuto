package com.symbase.gui;

/**
 * 供不同功能 MOD 共享的屏幕覆盖区域判断
 */
public final class ScreenOverlayGuard {
    public interface Guard {
        boolean isOver(double mouseX, double mouseY);
    }

    private static Guard guard = (mouseX, mouseY) -> false;

    private ScreenOverlayGuard() {
    }

    public static void set(Guard newGuard) {
        guard = newGuard == null ? (mouseX, mouseY) -> false : newGuard;
    }

    public static boolean isOver(double mouseX, double mouseY) {
        return guard.isOver(mouseX, mouseY);
    }
}
