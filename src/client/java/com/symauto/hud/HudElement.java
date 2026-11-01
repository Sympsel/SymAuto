package com.symauto.hud;

import net.minecraft.client.gui.Font;

/**
 * 单个 HUD 元素
 */
public interface HudElement {
    /**
     * 稳定的唯一 ID，用于布局文件；修改后旧布局会丢失。
     */
    String id();

    /**
     * 编辑器中显示的名称。
     */
    String displayName();

    /**
     * 元素未缩放时的宽度。
     */
    int width(Font font);

    /**
     * 元素未缩放时的高度。
     */
    int height(Font font);

    /**
     * 在局部坐标系中绘制元素。
     *
     * @see HudRenderContext
     */
    void render(HudRenderContext context);

    /**
     * 编辑器列表中的说明，可为空。
     */
    default String description() {
        return "";
    }

    /**
     * 首次出现在布局中时使用的默认位置。
     */
    default HudPlacement defaultPlacement() {
        return new HudPlacement(HudAnchor.TOP_LEFT, 4, 4, 1.0F, true);
    }

    /**
     * 是否透明背景
     */
    default boolean transparentBackground() {
        return false;
    }
}
