package com.symbase.gui.widget.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 横向滑杆
 */
public class SymSlider extends AbstractWidget {
    private final Font font = Minecraft.getInstance().font;
    private final float min;
    private final float max;
    private final String label;
    private float value;
    private Consumer<Float> onChange;
    private boolean dragging;

    public SymSlider(int x, int y, int width, int height, float min, float max, float value, String label) {
        super(x, y, width, height, Component.literal(label));
        this.min = min;
        this.max = max;
        this.label = label;
        this.value = Math.clamp(value, min, max);
    }

    public SymSlider onChange(Consumer<Float> onChange) {
        this.onChange = onChange;
        return this;
    }

    public float value() {
        return value;
    }

    public void setValue(float value) {
        float next = Math.clamp(value, min, max);
        if (next == this.value) {
            return;
        }
        this.value = next;
        if (onChange != null) {
            onChange.accept(next);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int trackY = y + h - 5;
        g.fill(x + 4, trackY, x + w - 4, trackY + 1, 0x88FFFFFF);

        float ratio = max == min ? 0.0F : (value - min) / (max - min);
        int knobX = x + 4 + Math.round(ratio * (w - 9));
        g.fill(knobX, y + 3, knobX + 2, y + h - 3, 0xFF6FA8FF);

        String text = label + ": " + String.format(Locale.ROOT, "%.0f", value);
        g.text(font, text, x + 4, y + 2, 0xFFF0F0F0, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        dragging = true;
        updateFromMouse(event.x());
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging) {
            return false;
        }
        updateFromMouse(event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    private void updateFromMouse(double mouseX) {
        float ratio = (float) ((mouseX - getX() - 4) / Math.max(1.0, getWidth() - 9));
        setValue(min + Math.clamp(ratio, 0.0F, 1.0F) * (max - min));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(label + " " + value));
    }
}


