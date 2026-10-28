package com.symauto.gui.widget.component;

import java.util.List;

public class SymMenu {
    private SymMenuBar parent;

    private String name;
    private List<SymMenuItem> items;

    public SymMenu(SymMenuBar parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public SymMenu addItem(SymMenuItem item) {
        this.items.add(item);
        return this;
    }
}
