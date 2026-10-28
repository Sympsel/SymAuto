package com.symauto.gui.widget.component;

import com.symauto.gui.utils.ColorConstants;
import com.symauto.gui.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class SymEditBox extends EditBox {
    private static final int BASE = 0x00202830 | ColorConstants.SEMI_ALPHA;
    private static final int HOVER = ColorUtils.onHover(BASE);
    private static final int FOCUS_OUTLINE = 0x004C8CE6 | ColorConstants.SEMI_ALPHA;
    private static final int OUTLINE = 0x00666666 | ColorConstants.SEMI_ALPHA;
    private static final int PADDING_X = 4;

    public SymEditBox(int x, int y, int width, int height, Component message) {
        super(Minecraft.getInstance().font, x, y, width, height, message);
        this.setBordered(false);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (this.isVisible()) {
            int y = this.getY() - 1;
            int bottom = this.getY() + this.getHeight() + 1;
            g.fill(this.getX(), y,
                    this.getX() + this.getWidth(), bottom,
                    this.isHovered() ? HOVER : BASE);
            if (this.isFocused()) {
                g.outline(this.getX(), y, this.getWidth(), this.getHeight() + 2, FOCUS_OUTLINE);
            } else {
                g.outline(this.getX(), y, this.getWidth(), this.getHeight() + 2, OUTLINE);
            }
        }
        int ox = this.getX();
        int oy = this.getY();
        this.setX(ox + PADDING_X);
        this.setY(oy + (this.getHeight() - 7) / 2);
        super.extractWidgetRenderState(g, mouseX, mouseY, delta);
        this.setX(ox);
        this.setY(oy);
    }
}
