package com.symbase.gui.widget.component;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.symbase.gui.utils.ColorUtils;
import com.symbase.gui.widget.model.SymActionContext;
import com.symbase.gui.widget.model.SymInteractions;
import com.symbase.gui.widget.model.SymTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SymButton extends AbstractButton {
    private final Font font = Minecraft.getInstance().font;


    private int bg;
    private int bgHover;
    private int bgDisabled;

    private final SymInteractions interactions = new SymInteractions();

    private boolean stateMode = false;
    private int stateOnColor;
    private int stateOffColor;

    public SymButton(Component text, int x, int y, int width, int height, int bg) {
        super(x, y, width, height, text);
        this.bg = bg;
        this.bgHover = ColorUtils.onHover(bg);
        this.bgDisabled = ColorUtils.onDisabled(bg);
    }

    @FunctionalInterface
    public interface HoverBarrier {
        boolean blocks(double mouseX, double mouseY);
    }

    private HoverBarrier hoverBarrier;

    private boolean hovered(int mouseX, int mouseY) {
        return isHovered() && (this.hoverBarrier == null || !this.hoverBarrier.blocks(mouseX, mouseY));
    }

    public SymButton hoverBarrier(HoverBarrier hoverBarrier) {
        this.hoverBarrier = hoverBarrier;
        return this;
    }

    private Component tooltipText;

    public SymButton stateMode(int onColor, int offColor) {
        this.stateMode = true;
        this.stateOnColor = onColor;
        this.stateOffColor = offColor;
        return this;
    }

    public SymButton state(boolean on) {
        if (this.stateMode) {
            this.bg = on ? this.stateOnColor : this.stateOffColor;
            this.bgHover = ColorUtils.onHover(this.bg);
            this.bgDisabled = ColorUtils.onDisabled(this.bg);
        }
        return this;
    }

    public SymButton noBackground() {
        this.bg = 0;
        return this;
    }

    public SymButton tooltip(Component tooltipText) {
        this.tooltipText = tooltipText;
        return this;
    }

    public SymButton onLeftClick(Runnable action) {
        interactions.bind(SymTrigger.LEFT_CLICK, action);
        return this;
    }

    public SymButton onRightClick(Runnable action) {
        interactions.bind(SymTrigger.RIGHT_CLICK, action);
        return this;
    }

    public SymButton onMiddleClick(Runnable action) {
        interactions.bind(SymTrigger.MIDDLE_CLICK, action);
        return this;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        SymTrigger trigger = SymTrigger.of(event, doubleClick);
        boolean handled = interactions.dispatch(
                trigger,
                new SymActionContext(this, event, doubleClick)
        );
        if (handled) {
            playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
        return handled;
    }

    @Override
    public void onPress(InputWithModifiers input) {
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        boolean hot = hovered(mouseX, mouseY);
        int color = !this.active ? this.bgDisabled
                : hot ? this.bgHover
                : this.bg;
        g.fill(this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
        this.extractDefaultLabel(
                g.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY));
        if (this.active && hot && this.tooltipText != null) {
            SymTooltip.offer(this.font.split(this.tooltipText, 230), mouseX, mouseY);
        }
    }

    @Override
    protected void handleCursor(GuiGraphicsExtractor g) {
        if (isHovered()) {
            g.requestCursor(this.active ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", this.getMessage()));
    }
}


