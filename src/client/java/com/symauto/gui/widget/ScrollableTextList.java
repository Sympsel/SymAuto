package com.symauto.gui.widget;

import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ScrollableTextList {
    public record Line(Component content, int color) {}

    private static final int DEFAULT_ROW_H = 12;

    private final Supplier<List<Line>> lines;
    private int rowH = DEFAULT_ROW_H;

    private int left = 8, top = 30, right = 100, bottom = 100;
    private String emptyText = "";
    private String hintText = "";

    @Setter
    private int scrollOffset;
    private int maxScroll;

    // 滚动条（可选）
    private boolean scrollbarEnabled;
    private int totalLines;
    private int visibleRowsNow;
    private final SimpleScrollbar scrollbar = new SimpleScrollbar(new SimpleScrollbar.ScrollTarget() {
        @Override public int totalRows() { return totalLines; }
        @Override public int visibleRows() { return visibleRowsNow; }
        @Override public int scrollRow() { return scrollOffset; }
        @Override public void scrollRow(int v) { scrollOffset = v; }
    });

    public ScrollableTextList(Supplier<List<Line>> lines) {
        this.lines = lines;
    }

    public ScrollableTextList setBounds(int left, int top, int right, int bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        return this;
    }

    public ScrollableTextList setRowHeight(int rowH) {
        this.rowH = rowH;
        return this;
    }

    public ScrollableTextList setEmptyText(String emptyText) {
        this.emptyText = emptyText;
        return this;
    }

    public ScrollableTextList setHintText(String hintText) {
        this.hintText = hintText;
        return this;
    }
    /** 开启右侧滚动条；内容宽度会相应收窄 */
    public ScrollableTextList setScrollbarEnabled(boolean enabled) {
        this.scrollbarEnabled = enabled;
        return this;
    }

    public void render(GuiGraphicsExtractor graphics, Font font) {
        // 有滚动条时内容区让出轨道宽度
        int contentRight = (scrollbarEnabled && maxScroll > 0)
                ? right - SimpleScrollbar.WIDTH - 2
                : right;
        int maxWidth = contentRight - left;

        List<FormattedCharSequence> all = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Line line : lines.get()) {
            var split = font.split(line.content(), maxWidth);
            if (split.isEmpty()) {
                all.add(Component.literal(" ").getVisualOrderText());
                colors.add(line.color());
            } else {
                for (var seq : split) {
                    all.add(seq);
                    colors.add(line.color());
                }
            }
        }
        totalLines = all.size();

        int visible = Math.max(1, (bottom - top) / rowH);
        visibleRowsNow = visible;
        maxScroll = Math.max(0, totalLines - visible);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        graphics.fill(left - 2, top - 4, right + 2, bottom + 4, 0x60000000);

        if (totalLines == 0 && !emptyText.isEmpty()) {
            graphics.text(font, emptyText, (left + right - font.width(emptyText)) / 2, top, 0xFFAAAAAA, false);
        }

        int y = top;
        for (int i = scrollOffset; i < totalLines; i++) {
            if (y + rowH > bottom) break;
            graphics.text(font, all.get(i), left, y, colors.get(i), false);
            y += rowH;
        }

        if (!hintText.isEmpty()) {
            graphics.text(font, hintText, right - font.width(hintText), bottom + 8, 0xFFAAAAAA, false);
        }

        if (scrollbarEnabled) {
            scrollbar.setBounds(right - SimpleScrollbar.WIDTH, top, bottom);
            scrollbar.render(graphics);
        }
    }

    public boolean mouseScrolled(double scrollY) {
        if (scrollY == 0) return false;
        scrollOffset = Math.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
        return true;
    }

    // —— 滚动条事件转发 ——
    public boolean mouseClicked(double mouseX, double mouseY) {
        return scrollbarEnabled && scrollbar.mouseClicked(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseY) {
        return scrollbar.mouseDragged(mouseY);
    }

    public boolean mouseReleased() {
        return scrollbar.mouseReleased();
    }

    public int scrollOffset() { return scrollOffset; }

    public void reset() { scrollOffset = 0; }

    public void scrollToEnd() { scrollOffset = Integer.MAX_VALUE; }
}