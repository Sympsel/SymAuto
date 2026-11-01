package com.symbase.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * 单行文本 HUD 元素
 */
public abstract class AbstractTextHudElement extends AbstractHudElement {
    // 内边距
    private static final int PADDING_X = 3;
    private static final int PADDING_Y = 1;

    private final int textColor;
    private final int bgColor;

    protected AbstractTextHudElement(String id, String displayName, String description,
                                     HudAnchor defaultAnchor, int defaultOffsetX, int defaultOffsetY,
                                     int textColor, int bgColor) {
        super(id, displayName, description, defaultAnchor, defaultOffsetX, defaultOffsetY, false);
        this.textColor = textColor;
        this.bgColor = bgColor;
    }

    protected abstract String text(Minecraft client, boolean preview);

    @Override
    public int width(Font font) {
        return font.width(text(Minecraft.getInstance(), false)) + PADDING_X * 2;
    }

    @Override
    public int height(Font font) {
        return font.lineHeight + PADDING_Y * 2;
    }

    @Override
    public void render(HudRenderContext context) {
        String text = text(Minecraft.getInstance(), context.preview());
        int width = Math.max(context.width(), context.font().width(text) + PADDING_X * 2);
        if ((bgColor >>> 24) != 0) {
            context.g().fill(0, 0, width, context.height(), bgColor);
        }
        context.g().text(context.font(), text, PADDING_X, PADDING_Y, textColor, true);
    }
}


