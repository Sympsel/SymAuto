package com.symbase.gui;

/**
 * 供不同功能 MOD 共享的屏幕覆盖区域判断。
 *
 * <p>例如 HUD 模块的快捷容器面板由 HUD 渲染，但 SymAuto 的点击逻辑需要知道
 * 鼠标是否落在该面板上，避免跨模块直接依赖具体面板类。</p>
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
