package com.symbase.gui.widget.model;

import net.minecraft.client.input.MouseButtonEvent;

public enum SymTrigger {
    LEFT_CLICK,
    RIGHT_CLICK,
    MIDDLE_CLICK,
    LEFT_DOUBLE_CLICK,
    RIGHT_DOUBLE_CLICK,
    MIDDLE_DOUBLE_CLICK;

    public static SymTrigger of(MouseButtonEvent event, boolean doubleClick) {
        return switch (event.button()) {
            case 0 -> doubleClick ? LEFT_DOUBLE_CLICK : LEFT_CLICK;
            case 1 -> doubleClick ? RIGHT_DOUBLE_CLICK : RIGHT_CLICK;
            case 2 -> doubleClick ? MIDDLE_DOUBLE_CLICK : MIDDLE_CLICK;
            default -> null;
        };
    }
}


