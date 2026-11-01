package com.symbase.gui.screen;

import com.symbase.gui.widget.component.SymMenuBar;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class SymModMenuRegistry {
    public static final SymModMenuRegistry INSTANCE = new SymModMenuRegistry();

    @Getter
    private final SymMenuBar.Menu symModMenu;
    @Getter
    private SymMenuBar.Entry defaultOpenMenu;
    @Getter
    private SymMenuBar.Entry currOpenMenu;

    private SymModMenuRegistry() {
        symModMenu = new SymMenuBar.Menu(Component.literal("SymMod"), new ArrayList<>());
    }

    public SymModMenuRegistry registerMenuEntry(SymMenuBar.Entry entry) {
        if (symModMenu.contains(entry.label())) {
            return this;
        }
        SymMenuBar.Entry tracked = new SymMenuBar.Entry(
                entry.label(),
                entry.tooltip(),
                () -> {
                    setCurrOpenMenu(entry);
                    entry.action().run();
                },
                () -> currOpenMenu != null && currOpenMenu.label().equals(entry.label()),
                entry.hover());
        symModMenu.add(tracked);
        if (defaultOpenMenu == null) {
            defaultOpenMenu = tracked;
        }
        if (currOpenMenu == null) {
            currOpenMenu = tracked;
        }
        return this;
    }

    public SymModMenuRegistry setDefaultOpenMenu(SymMenuBar.Entry entry) {
        defaultOpenMenu = entry;
        if (currOpenMenu == null) {
            currOpenMenu = entry;
        }
        return this;
    }

    public SymModMenuRegistry setCurrOpenMenu(SymMenuBar.Entry entry) {
        currOpenMenu = entry;
        if (defaultOpenMenu == null) {
            defaultOpenMenu = entry;
        }
        return this;
    }
}
