package com.symhud.storage;

import com.google.gson.reflect.TypeToken;
import com.symbase.storage.base.AbstractJsonStore;
import com.symhud.core.Constants;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class VillagerTradeStore extends AbstractJsonStore {
    public static final VillagerTradeStore INSTANCE = new VillagerTradeStore();
    private static final Map<String, Entry> DATA = new java.util.concurrent.ConcurrentHashMap<>();

    private VillagerTradeStore() {
        super(Constants.MOD_ID, "highlight", "villager_trades.json");
    }

    static class Entry {
        Map<String, Integer> enchantments = new TreeMap<>();
        long updated;
    }

    /**
     * 记录某村民的附魔书报价
     */
    public static void record(UUID villagerId, Map<String, Integer> enchantLevels) {
        String key = villagerId.toString();
        Map<String, Integer> sorted = new TreeMap<>(enchantLevels);

        Entry existing = DATA.get(key);
        if (existing != null && existing.enchantments.equals(sorted)) {
            return;
        }
        Entry entry = existing != null ? existing : new Entry();
        entry.enchantments = sorted;
        entry.updated = System.currentTimeMillis();
        DATA.put(key, entry);

        synchronized (INSTANCE) {
            INSTANCE.setDirty(true);
            INSTANCE.save();
        }
    }

    /**
     * 返回该村民缓存的附魔 ID -> 等级；从未记录过则返回 null
     */
    public static Map<String, Integer> getEnchantments(UUID villagerId) {
        Entry entry = DATA.get(villagerId.toString());
        return entry == null ? null : new HashMap<>(entry.enchantments);
    }

    public static boolean has(UUID villagerId) {
        return DATA.containsKey(villagerId.toString());
    }

    public static int size() {
        return DATA.size();
    }

    public static int clear() {
        synchronized (INSTANCE) {
            int removed = DATA.size();
            DATA.clear();
            INSTANCE.save();
            return removed;
        }
    }

    @Override
    protected void onMissingFile() {
        // 旧版本把村民交易缓存放在 symauto 下，迁移一次
        Path legacy = FabricLoader.getInstance().getConfigDir()
                .resolve("symauto")
                .resolve("highlight")
                .resolve("villager_trades.json");
        if (!Files.exists(legacy)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.copy(legacy, path);
            restore(Files.readString(path));
            save();
        } catch (Exception e) {
            System.err.println("[SymHud] 迁移村民交易缓存失败: " + e.getMessage());
        }
    }

    @Override
    protected void clearData() {
        DATA.clear();
    }

    @Override
    protected Object snapshot() {
        return DATA;
    }

    @Override
    protected void restore(String json) {
        Map<String, Entry> parsed = gson.fromJson(json,
                new TypeToken<Map<String, Entry>>() {
                }.getType());
        if (parsed != null) {
            DATA.putAll(parsed);
        }
    }

    @Override
    protected String describe() {
        return "村民交易缓存";
    }
}

