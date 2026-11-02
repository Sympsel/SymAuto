package com.symbase.hud;

import net.minecraft.client.gui.Font;

/**
 * 单个 HUD 元素
 */
public interface HudElement {
    String id();

    /**
     * 编辑器中显示的名称
     */
    String displayName();

    /**
     * 元素的基础宽度
     */
    int width(Font font);

    /**
     * 元素的基础高度
     */
    int height(Font font);

    /**
     * 在局部坐标系中绘制元素
     *
     * @see HudRenderContext
     */
    void render(HudRenderContext context);

    default String description() {
        return "";
    }

    /**
     * 默认位置
     */
    default HudPlacement defaultPlacement() {
        return new HudPlacement(HudAnchor.TOP_LEFT, 4, 4, 1.0F, true);
    }
}



