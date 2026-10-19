package com.symauto.gui.base;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SymBaseScreen extends Screen {
    protected static final int TITLE_Y = 8;
    protected static final int TITLE_COLOR = 0xffffffff;

    protected SymBaseScreen(Component title) {
        super(title);
    }

    protected void renderTitle(GuiGraphicsExtractor graphics) {
        int x = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, TITLE_Y, TITLE_COLOR, true);
    }

    protected Button backButton(Screen parent, String label, int width) {
        return Button.builder(Component.literal(label),
                        b -> this.minecraft.gui.setScreen(parent))
                .bounds(this.width - width - 6, 6, width, 16)
                .build();
    }

    protected Button backButton(Screen parent) {
        return backButton(parent, "返回", 64);
    }
}
