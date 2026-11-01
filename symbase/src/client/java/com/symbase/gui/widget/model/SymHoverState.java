package com.symbase.gui.widget.model;

import net.minecraft.client.gui.components.AbstractWidget;

public class SymHoverState {
    public interface Listener {
        default void onEnter(AbstractWidget widget, double mouseX, double mouseY) {}
        default void onExit(AbstractWidget widget, double mouseX, double mouseY) {}
    }

    private final Listener listener;
    private boolean hovered;

    public SymHoverState(Listener listener) {
        this.listener = listener;
    }

    public void update(boolean nowHovered, AbstractWidget widget, double mouseX, double mouseY) {
        if (nowHovered == hovered) {
            return; // 只在状态变化时触发
        }
        hovered = nowHovered;
        if (nowHovered) {
            listener.onEnter(widget, mouseX, mouseY);
        } else {
            listener.onExit(widget, mouseX, mouseY);
        }
    }

    public boolean isHovered() {
        return hovered;
    }
}


