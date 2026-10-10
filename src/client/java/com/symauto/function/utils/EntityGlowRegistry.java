package com.symauto.function.utils;

import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯客户端描边登记表：记录需要强制发光的实体
 */
public final class EntityGlowRegistry {
    private static final Map<String, Source> SOURCES = new ConcurrentHashMap<>();

    private record Source(TeamColor color, Set<UUID> ids) {
    }

    private EntityGlowRegistry() {
    }

    /** 用新的集合整体替换当前发光列表 */
    public static void replace(String source, TeamColor color, Collection<UUID> ids) {
        Set<UUID> set = ConcurrentHashMap.newKeySet();
        set.addAll(ids);
        SOURCES.put(source, new Source(color, set));
    }

    public static @Nullable TeamColor colorOf(UUID id) {
        for (Source src : SOURCES.values()) {
            if (src.ids().contains(id)) {
                return src.color();
            }
        }
        return null;
    }

    public static boolean isGlowing(UUID id) {
        return colorOf(id) != null;
    }

    public static void clear(String source) {
        SOURCES.remove(source);
    }

    public static void clear() {
        SOURCES.clear();
    }
}