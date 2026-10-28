package com.symauto.gui.base;

import com.symauto.gui.UiStyle;
import com.symauto.gui.widget.component.SymTooltip;
import com.symauto.gui.widget.utils.ButtonFactory;
import com.symauto.gui.widget.component.SymButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    protected SymButton backButton(Screen parent, String label, int width) {
        return ButtonFactory.back(Component.literal(label),
                this.width - width - 6, 6, width, 16)
                .onLeftClick(() -> this.minecraft.gui.setScreen(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        SymTooltip.renderAndClear(g, this.font, this.width, this.height);
    }
}
