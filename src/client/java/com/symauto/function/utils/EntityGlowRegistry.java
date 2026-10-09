package com.symauto.function.utils;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯客户端描边登记表：记录需要强制发光的实体
 */
public final class EntityGlowRegistry {
    private static final Set<UUID> GLOWING = ConcurrentHashMap.newKeySet();

    private EntityGlowRegistry() {
    }

    /** 用新的集合整体替换当前发光列表 */
    public static void replace(Collection<UUID> ids) {
        GLOWING.clear();
        GLOWING.addAll(ids);
    }

    public static boolean isGlowing(UUID id) {
        return GLOWING.contains(id);
    }

    public static void clear() {
        GLOWING.clear();
    }
}