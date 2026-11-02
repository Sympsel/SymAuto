package com.symbase.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 绘制 HUD 元素时使用的上下文
 */
public record HudRenderContext(
        GuiGraphicsExtractor g,
        Font font,
        int width,
        int height,
        boolean preview,
        Integer textColor
) {
}



