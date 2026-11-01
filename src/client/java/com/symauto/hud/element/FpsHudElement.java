package com.symauto.hud.element;

import com.symauto.hud.AbstractTextHudElement;
import com.symauto.hud.HudAnchor;
import net.minecraft.client.Minecraft;

/**
 * 内置示例 HUD：当前帧率。
 */
public final class FpsHudElement extends AbstractTextHudElement {
    public FpsHudElement() {
        super("fps", "帧率", "显示当前游戏帧率",
                HudAnchor.TOP_LEFT, 4, 42, 0xFF7CFC8A, 0x66000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        return "FPS " + client.getFps();
    }
}
