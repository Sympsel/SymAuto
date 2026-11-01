package com.symbase.gui.widget.model;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;

public record SymActionContext(
        AbstractWidget widget,
        MouseButtonEvent event,
        boolean doubleClick
) {
}


