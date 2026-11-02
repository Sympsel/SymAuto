package com.symbase.hud;

import net.minecraft.client.gui.Font;

import java.util.Objects;

/**
 * 单个 HUD 元素的布局数据：锚点 + 相对偏移 + 缩放 + 是否显示
 */
public final class HudPlacement {
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 3.0F;

    private HudAnchor anchor;
    private int offsetX;
    private int offsetY;
    private float scale;
    private boolean visible;
    private boolean showLabel = true;
    private Integer textColor;

    public HudPlacement(HudAnchor anchor, int offsetX, int offsetY, float scale, boolean visible) {
        this(anchor, offsetX, offsetY, scale, visible, null, true);
    }

    public HudPlacement(HudAnchor anchor, int offsetX, int offsetY, float scale, boolean visible, Integer textColor) {
        this(anchor, offsetX, offsetY, scale, visible, textColor, true);
    }

    public HudPlacement(HudAnchor anchor, int offsetX, int offsetY, float scale, boolean visible,
                        Integer textColor, boolean showLabel) {
        this.anchor = anchor == null ? HudAnchor.TOP_LEFT : anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = clampScale(scale);
        this.visible = visible;
        this.textColor = textColor;
        this.showLabel = showLabel;
    }

    public static float clampScale(float value) {
        if (!Float.isFinite(value)) {
            return 1.0F;
        }
        return Math.clamp(value, MIN_SCALE, MAX_SCALE);
    }

    public HudAnchor anchor() {
        return anchor;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public float scale() {
        return scale;
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean showLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }

    public void setScale(float scale) {
        this.scale = clampScale(scale);
    }

    public Integer textColor() {
        return textColor;
    }

    public void setTextColor(Integer textColor) {
        this.textColor = textColor;
    }

    /**
     * 计算元素在屏幕上的实际区域
     */
    public HudBounds resolve(Font font, HudElement element, int screenWidth, int screenHeight) {
        int width = scaledWidth(font, element);
        int height = scaledHeight(font, element);
        int x = anchor.baseX(screenWidth, width) + offsetX;
        int y = anchor.baseY(screenHeight, height) + offsetY;
        return new HudBounds(x, y, width, height);
    }

    public int scaledWidth(Font font, HudElement element) {
        return Math.max(1, Math.round(element.width(font, showLabel) * scale));
    }

    public int scaledHeight(Font font, HudElement element) {
        return Math.max(1, Math.round(element.height(font, showLabel) * scale));
    }

    /**
     * 把元素移动到屏幕上的指定位置，并根据落点自动重新选择锚点
     */
    public void moveTo(int x, int y, int elementWidth, int elementHeight, int screenWidth, int screenHeight) {
        HudAnchor next = HudAnchor.forBounds(x, y, elementWidth, elementHeight, screenWidth, screenHeight);
        this.anchor = next;
        this.offsetX = x - next.baseX(screenWidth, elementWidth);
        this.offsetY = y - next.baseY(screenHeight, elementHeight);
    }

    public HudPlacement copy() {
        return new HudPlacement(anchor, offsetX, offsetY, scale, visible, textColor, showLabel);
    }

    public boolean sameState(HudPlacement other) {
        return other != null
                && anchor == other.anchor
                && offsetX == other.offsetX
                && offsetY == other.offsetY
                && Float.compare(scale, other.scale) == 0
                && visible == other.visible
                && showLabel == other.showLabel
                && Objects.equals(textColor, other.textColor);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HudPlacement other && sameState(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anchor, offsetX, offsetY, scale, visible, showLabel, textColor);
    }
}



