package com.symauto.gui.widget.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 实心箭头按钮
 */
public class ArrowButton extends SymButton {
    public enum Direction { LEFT, RIGHT, UP, DOWN }

    private static final int ARROW_COLOR = 0xF0FFFFFF;
    private static final int ARROW_COLOR_HOVER = 0xFFFFFFFF;
    private static final int ARROW_COLOR_DISABLED = 0x60FFFFFF;
    private static final int HALF = 4;

    private final Direction direction;

    public ArrowButton(int x, int y, int width, int height, Direction direction, int bg) {
        super(Component.empty(), x, y, width, height, bg);
        this.direction = direction;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractContents(g, mouseX, mouseY, delta);
        int color = !isActive() ? ARROW_COLOR_DISABLED
                : isHovered() ? ARROW_COLOR_HOVER
                : ARROW_COLOR;
        int cx = this.getX() + this.getWidth() / 2;
        int cy = this.getY() + this.getHeight() / 2;
        int span = HALF * 2;
        for (int i = 0; i <= span; i++) {
            int t = switch (direction) {
                case LEFT, UP -> i;
                case RIGHT, DOWN -> span - i;
            };
            int hh = t / 2;
            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                int x = cx - HALF + i;
                g.fill(x, cy - hh, x + 1, cy + hh + 1, color);
            } else {
                int y = cy - HALF + i;
                g.fill(cx - hh, y, cx + hh + 1, y + 1, color);
            }
        }
    }
}