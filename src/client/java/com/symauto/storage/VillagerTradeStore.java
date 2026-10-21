
package com.symauto.storage;

import com.google.gson.reflect.TypeToken;
import com.symauto.storage.base.AbstractJsonStore;

import java.util.*;


public final class VillagerTradeStore extends AbstractJsonStore {
    public static final VillagerTradeStore INSTANCE = new VillagerTradeStore();
    private static final Map<String, Entry> DATA = new java.util.concurrent.ConcurrentHashMap<>();

    private VillagerTradeStore() {
        super("highlight", "villager_trades.json");
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
