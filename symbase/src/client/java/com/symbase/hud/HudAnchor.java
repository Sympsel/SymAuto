package com.symbase.hud;

/**
 * HUD 元素的锚点
 */
public enum HudAnchor {
    TOP_LEFT("左上", 0.0F, 0.0F),
    TOP_CENTER("中上", 0.5F, 0.0F),
    TOP_RIGHT("右上", 1.0F, 0.0F),
    MIDDLE_LEFT("左中", 0.0F, 0.5F),
    CENTER("居中", 0.5F, 0.5F),
    MIDDLE_RIGHT("右中", 1.0F, 0.5F),
    BOTTOM_LEFT("左下", 0.0F, 1.0F),
    BOTTOM_CENTER("中下", 0.5F, 1.0F),
    BOTTOM_RIGHT("右下", 1.0F, 1.0F);

    private static final HudAnchor[][] GRID = {
            {TOP_LEFT, TOP_CENTER, TOP_RIGHT},
            {MIDDLE_LEFT, CENTER, MIDDLE_RIGHT},
            {BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT}
    };

    private final String label;
    private final float horizontal;
    private final float vertical;

    HudAnchor(String label, float horizontal, float vertical) {
        this.label = label;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public String label() {
        return label;
    }

    public int baseX(int screenWidth, int elementWidth) {
        return Math.round(horizontal * Math.max(0, screenWidth - elementWidth));
    }

    public int baseY(int screenHeight, int elementHeight) {
        return Math.round(vertical * Math.max(0, screenHeight - elementHeight));
    }

    /**
     * 根据元素中心所在区域选择最接近的锚点，用于拖拽时自动吸附
     */
    public static HudAnchor forBounds(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        float centerX = (x + width / 2.0F) / Math.max(1, screenWidth);
        float centerY = (y + height / 2.0F) / Math.max(1, screenHeight);
        int column = centerX < 1.0F / 3.0F ? 0 : centerX > 2.0F / 3.0F ? 2 : 1;
        int row = centerY < 1.0F / 3.0F ? 0 : centerY > 2.0F / 3.0F ? 2 : 1;
        return GRID[row][column];
    }

    public static HudAnchor byName(String name) {
        if (name != null) {
            for (HudAnchor anchor : values()) {
                if (anchor.name().equalsIgnoreCase(name)) {
                    return anchor;
                }
            }
        }
        return TOP_LEFT;
    }
}


