package com.symbase.hud;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * 单行文本 HUD 元素
 */
public abstract class AbstractTextHudElement extends AbstractHudElement {
    // 内边距
    private static final int PADDING_X = 3;
    private static final int PADDING_Y = 1;

    @Getter
    @Setter
    private int textColor;
    @Getter
    @Setter
    private int bgColor;

    protected AbstractTextHudElement(String id, String displayName, String description,
                                     HudAnchor defaultAnchor, int defaultOffsetX, int defaultOffsetY,
                                     int textColor, int bgColor) {
        super(id, displayName, description, defaultAnchor, defaultOffsetX, defaultOffsetY);
        this.textColor = textColor;
        this.bgColor = bgColor;
    }

    protected abstract String text(Minecraft client, boolean preview);

    @Override
    public int width(Font font) {
        return width(font, true);
    }

    @Override
    public int width(Font font, boolean showLabel) {
        String value = text(Minecraft.getInstance(), false);
        if (!showLabel) {
            value = stripLabel(value);
        }
        return font.width(value) + PADDING_X * 2;
    }

    @Override
    public int height(Font font) {
        return font.lineHeight + PADDING_Y * 2;
    }

    @Override
    public void render(HudRenderContext context) {
        String text = text(Minecraft.getInstance(), context.preview());
        if (!context.showLabel()) {
            text = stripLabel(text);
        }
        int width = Math.max(context.width(), context.font().width(text) + PADDING_X * 2);
        if ((bgColor >>> 24) != 0) {
            context.g().fill(0, 0, width, context.height(), bgColor);
        }
        int color = context.textColor() != null ? context.textColor() : textColor;
        context.g().text(context.font(), text, PADDING_X, PADDING_Y, color, true);
    }

    /**
     * 只保留值
     */
    private static String stripLabel(String text) {
        int ascii = text.indexOf(':');
        int fullWidth = text.indexOf('：');
        int index;
        if (ascii < 0) {
            index = fullWidth;
        } else if (fullWidth < 0) {
            index = ascii;
        } else {
            index = Math.min(ascii, fullWidth);
        }
        if (index < 0) {
            return text;
        }
        return text.substring(index + 1).stripLeading();
    }
}




