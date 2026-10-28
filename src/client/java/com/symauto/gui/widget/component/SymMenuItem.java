package com.symauto.gui.widget.component;

public class SymMenuItem {
    private SymMenu parent;
    Runnable action;

    public SymMenuItem(SymMenu parent) {
        this.parent = parent;
    }
}
