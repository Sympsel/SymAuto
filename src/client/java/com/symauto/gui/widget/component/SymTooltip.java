package com.symauto.gui.widget.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SymTooltip {
    private static final int BG = 0xE6101418;
    private static final int BORDER = 0x66FFFFFF;
    private static final int TEXT = 0xF0FFFFFF;
    private static final int PAD_X = 4;
    private static final int PAD_Y = 3;

    private record Request(List<FormattedCharSequence> lines, int x, int y) {
    }

    private static @Nullable Request request;

    public static void offer(List<FormattedCharSequence> lines, int x, int y) {
        if (request == null) {
            request = new Request(lines, x, y);
        }
    }

    public static void renderAndClear(GuiGraphicsExtractor g, Font font, int screenWidth, int screenHeight) {
        Request r = request;
        request = null;
        if (r == null || r.lines().isEmpty()) {
            return;
        }
        int w = PAD_X * 2;
        for (FormattedCharSequence line : r.lines()) {
            w = Math.max(w, font.width(line) + PAD_X * 2);
        }
        int h = r.lines().size() * font.lineHeight + PAD_Y * 2;

        int x = r.x() + 12;
        if (x + w > screenWidth - 2) {
            x = r.x() - w - 8;
        }
        x = Mth.clamp(x, 2, Math.max(2, screenWidth - w - 2));
        int y = Mth.clamp(r.y() + 4, 2, Math.max(2, screenHeight - h - 2));

        g.fill(x, y, x + w, y + h, BG);
        g.outline(x, y, w, h, BORDER);
        int ty = y + PAD_Y;
        for (FormattedCharSequence line : r.lines()) {
            g.text(font, line, x + PAD_X, ty, TEXT, false);
            ty += font.lineHeight;
        }
    }
}
