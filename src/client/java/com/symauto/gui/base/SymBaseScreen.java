package com.symauto.gui.base;

import com.symauto.gui.UiStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SymBaseScreen extends Screen {
    protected static final int TITLE_Y = 8;
    protected UiStyle style;

    protected SymBaseScreen(Component title) {
        super(title);
        style = UiStyle.DEFAULT;
    }

    protected SymBaseScreen(Component title, UiStyle style) {
        super(title);
        setStyle(style);
    }

    protected SymBaseScreen setStyle(UiStyle style) {
        this.style = style;
        return this;
    }

    protected void renderTitle(GuiGraphicsExtractor graphics) {
        int x = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, TITLE_Y, style.title, true);
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
