package com.symauto.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 通用竖向滚动条：轨道 + 滑块 + 拖拽。行滚动单位由 ScrollTarget 换算。 */
public class SimpleScrollbar {
    public static final int WIDTH = 6;

    public interface ScrollTarget {
        int totalRows();
        int visibleRows();
        int scrollRow();
        void scrollRow(int value);
    }

    private final ScrollTarget target;
    private int trackLeft, trackTop, trackBottom;
    private boolean dragging;
    private int dragOffset;

    public SimpleScrollbar(ScrollTarget target) {
        this.target = target;
    }

    public void setBounds(int left, int top, int bottom) {
        this.trackLeft = left;
        this.trackTop = top;
        this.trackBottom = bottom;
    }

    public int maxScroll() {
        return Math.max(0, target.totalRows() - target.visibleRows());
    }

    private int thumbHeight() {
        int trackH = trackBottom - trackTop;
        return Math.max(12, trackH * target.visibleRows() / Math.max(1, target.totalRows()));
    }

    private int thumbTop(int thumbH) {
        int max = maxScroll();
        if (max <= 0) return trackTop;
        return trackTop + (int) ((long) (trackBottom - trackTop - thumbH) * target.scrollRow() / max);
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (maxScroll() <= 0) return;
        graphics.fill(trackLeft, trackTop, trackLeft + WIDTH, trackBottom, 0x40FFFFFF);
        int thumbH = thumbHeight();
        int thumbY = thumbTop(thumbH);
        graphics.fill(trackLeft, thumbY, trackLeft + WIDTH, thumbY + thumbH,
                dragging ? 0xFFFFFFFF : 0xC0FFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (maxScroll() <= 0) return false;
        boolean over = mouseX >= trackLeft - 3 && mouseX <= trackLeft + WIDTH + 3
                && mouseY >= trackTop && mouseY <= trackBottom;
        if (!over) return false;
        int thumbH = thumbHeight();
        int thumbY = thumbTop(thumbH);
        if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
            dragOffset = (int) mouseY - thumbY;
        } else {
            dragOffset = thumbH / 2;
            updateFromMouse(mouseY);
        }
        dragging = true;
        return true;
    }

    public boolean mouseDragged(double mouseY) {
        if (!dragging) return false;
        updateFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) return false;
        dragging = false;
        return true;
    }

    private void updateFromMouse(double mouseY) {
        int usable = trackBottom - trackTop - thumbHeight();
        if (usable <= 0) {
            target.scrollRow(0);
            return;
        }
        double ratio = (mouseY - trackTop - dragOffset) / usable;
        target.scrollRow(Math.clamp((int) Math.round(ratio * maxScroll()), 0, maxScroll()));
    }
}