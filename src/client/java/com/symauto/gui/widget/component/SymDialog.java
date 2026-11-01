package com.symauto.gui.widget.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 模态对话框
 */
public class SymDialog extends AbstractWidget {
    @Getter
    private final Screen parent;

    @Getter
    private SymButton ensureBtn;
    @Getter
    private SymButton cancelBtn;

    @Getter
    @Setter
    private boolean modal = true;

    private static final int BG = 0xBB101418;
    private static final int BORDER = 0xFFFFC857;

    public SymDialog(int width, int height, Component message, Screen parent) {
        this.parent = parent;
        super((parent.width - width) / 2, (parent.height - height) / 2, width, height, message);
    }

    public SymDialog setCancelBtn(SymButton cancelBtn) {
        this.cancelBtn = cancelBtn;
        cancelBtn.setX(this.getX() + 16);
        cancelBtn.setY(this.getY() + this.getHeight() - 26);
        return this;
    }

    public SymDialog setEnsureBtn(SymButton ensureBtn) {
        this.cancelBtn = ensureBtn;
        ensureBtn.setX(this.getX() + this.getWidth() - 16 - ensureBtn.getWidth());
        ensureBtn.setY(this.getY() + this.getHeight() - 26);
        return this;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(this.getX(), this.getY(),
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight(), BG);
        g.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), BORDER);

        Font font = Minecraft.getInstance().font;
        int centerX = this.getX() + this.getWidth() / 2;
        int textY = this.getY() + 10;
        int maxWidth = Math.max(1, this.getWidth() - 16);

        // 同时处理 \n 和超宽自动换行
        List<FormattedCharSequence> lines = font.split(this.getMessage(), maxWidth);

        for (FormattedCharSequence line : lines) {
            g.centeredText(font, line, centerX, textY, 0xFFFFFFFF);
            textY += font.lineHeight + 1;
        }

        if (ensureBtn != null) {
            ensureBtn.extractRenderState(g, mouseX, mouseY, a);
        }
        if (cancelBtn != null) {
            cancelBtn.extractRenderState(g, mouseX, mouseY, a);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.visible) {
            if (ensureBtn != null && ensureBtn.mouseClicked(event, doubleClick)) {
                return true;
            }
            if (cancelBtn != null && cancelBtn.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    public boolean isOpen() {
        return this.visible;
    }

    public void open() {
        this.visible = true;
        this.active = true;
    }

    public void close() {
        this.visible = false;
    }
}
