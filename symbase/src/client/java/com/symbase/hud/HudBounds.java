package com.symbase.hud;

/**
 * HUD 元素在 GUI 缩放坐标系中的矩形区域
 */
public record HudBounds(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public boolean contains(double px, double py, int padding) {
        return px >= x - padding && px < right() + padding
                && py >= y - padding && py < bottom() + padding;
    }
}


