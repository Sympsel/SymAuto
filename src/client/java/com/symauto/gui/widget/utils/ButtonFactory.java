package com.symauto.gui.widget.utils;

import com.symauto.gui.utils.ColorConstants;
import com.symauto.gui.widget.component.ArrowButton;
import com.symauto.gui.widget.component.SymButton;
import net.minecraft.network.chat.Component;

/**
 * 产出预制按钮
 */
public class ButtonFactory {
    private ButtonFactory() {
    }

    public static SymButton function(Component label, int x, int y, int w, int h) {
        int bg = 0x00777777 | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton navigation(Component label, int x, int y, int w, int h) {
        int bg = 0x00555555 | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton category(Component label, int x, int y, int w, int h) {
        int bg = 0x004A5568 | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton back(Component label, int x, int y, int w, int h) {
        int bg = 0x004A5570 | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg).noBackground();
    }

    public static SymButton label(Component label, int x, int y, int w, int h) {
        int bg = 0x002A5F5A | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton toggle(Component label, int x, int y, int w, int h, boolean initialOn) {
        int on = 0x002E9E4F | ColorConstants.SEMI_ALPHA;
        int off = 0x00C0392B | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, on)
                .stateMode(on, off)
                .state(initialOn);
    }

    public static SymButton delete(Component label, int x, int y, int w, int h) {
        int bg = 0x00C0392B | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton normal(Component label, int x, int y, int w, int h) {
        int bg = 0x00555566 | ColorConstants.SEMI_ALPHA;
        return new SymButton(label, x, y, w, h, bg);
    }

    public static SymButton move(Component tooltip, int x, int y, int w, int h, ArrowButton.Direction dir) {
        int bg = 0x00555555 | ColorConstants.SEMI_ALPHA;
        return new ArrowButton(x, y, w, h, dir, bg).tooltip(tooltip);
    }
}
