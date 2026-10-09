
package com.symauto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class VillagerTradeStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir()
                    .resolve("symauto").resolve("highlight").resolve("villager_trades.json");
    private static final Map<String, Entry> DATA = new ConcurrentHashMap<>();
    private static boolean dirty = false;

    private VillagerTradeStore() {
    }

    static class Entry {
        List<String> enchantments = new ArrayList<>();
        long updated;
    }

    /**
     * 记录某村民的附魔书报价
     */
    public static void record(UUID villagerId, Set<String> enchantIds) {
        String key = villagerId.toString();
        List<String> sorted = new ArrayList<>(enchantIds);
        Collections.sort(sorted);

        Entry existing = DATA.get(key);
        if (existing != null && existing.enchantments.equals(sorted)) {
            return;
        }
        Entry entry = existing != null ? existing : new Entry();
        entry.enchantments = sorted;
        entry.updated = System.currentTimeMillis();
        DATA.put(key, entry);

        dirty = true;
        save();
    }

    /**
     * 返回该村民缓存的附魔 ID 集合；从未记录过则返回 null
     */
    public static Set<String> getEnchantments(UUID villagerId) {
        Entry entry = DATA.get(villagerId.toString());
        return entry == null ? null : new HashSet<>(entry.enchantments);
    }

    public static boolean has(UUID villagerId) {
        return DATA.containsKey(villagerId.toString());
    }

    public static int size() {
        return DATA.size();
    }

    public static int clear() {
        int removed = DATA.size();
        DATA.clear();
        save();
        return removed;
    }

    public static void load() {
        DATA.clear();
        if (!Files.exists(PATH)) {
            return;
        }
        try {
            String json = Files.readString(PATH);
            Map<String, Entry> parsed = GSON.fromJson(json,
                    new TypeToken<Map<String, Entry>>() {
                    }.getType());
            if (parsed != null) {
                DATA.putAll(parsed);
            }
        } catch (Exception e) {
            System.err.println("[SymAuto] 读取村民交易缓存失败: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(DATA));
            dirty = false;
        } catch (IOException e) {
            System.err.println("[SymAuto] 保存村民交易缓存失败: " + e.getMessage());
        }
    }

    public static void saveIfDirty() {
        if (dirty) {
            save();
        }
    }
}