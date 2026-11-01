package com.symbase.registry;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 允许各功能 MOD 往共享功能菜单里追加自己的入口
 */
public final class MenuRegistry {
    public enum Section {
        OPEN,
        HELP,
        ABOUT
    }

    /**
     * actionFactory 接收父界面，返回点击时执行的 Runnable
     */
    public record Entry(Component label, Function<Screen, Runnable> actionFactory) {
    }

    private static final Map<Section, List<Entry>> ENTRIES = new EnumMap<>(Section.class);

    static {
        for (Section section : Section.values()) {
            ENTRIES.put(section, new ArrayList<>());
        }
    }

    private MenuRegistry() {
    }

    public static void register(Section section, Component label, Function<Screen, Runnable> actionFactory) {
        ENTRIES.get(section).add(new Entry(label, actionFactory));
    }

    public static List<Entry> entries(Section section) {
        return List.copyOf(ENTRIES.get(section));
    }
}

