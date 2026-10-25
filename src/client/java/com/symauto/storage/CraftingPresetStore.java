package com.symauto.storage;

import com.google.common.reflect.TypeToken;
import com.symauto.model.preset.CraftingPreset;
import com.symauto.storage.base.AbstractJsonStore;
import lombok.AllArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.symauto.config.ConfigManager.parseItem;

public class CraftingPresetStore extends AbstractJsonStore {
    public static final CraftingPresetStore INSTANCE = new CraftingPresetStore();

    private static final List<CraftingPresetData> PRESETS = new ArrayList<>();
    private static long revision = 0;

    private static final Supplier<List<CraftingPresetData>> defaultsSupplier =CraftingPresetStore::defaultPresets;

    // 旧版配置文件位置：symauto/crafting/presets.json（现已迁移到 symauto/preset/crafting.json）
    private static final Path LEGACY_PATH =
            FabricLoader.getInstance().getConfigDir()
                    .resolve("symauto")
                    .resolve("crafting")
                    .resolve("presets.json");

    private CraftingPresetStore() {
        super("preset", "crafting.json");
    }

    @AllArgsConstructor
    public static final class CraftingPresetData {
        public String itemId;
        public int count;
        public int executeCount;
        public boolean recursive;
        public boolean throwResult;
        public boolean active;

        CraftingPreset toAssumption() {
            Item item = parseItem(itemId);
            ItemStack result = new ItemStack(item == null ? Items.AIR : item, Math.max(1, count));
            return new CraftingPreset(result, executeCount, recursive, throwResult, active);
        }

        public CraftingPresetData(Item item, int count, int executeCount, boolean recursive, boolean throwResult, boolean active) {
            this.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            this.count = count;
            this.executeCount = executeCount;
            this.recursive = recursive;
            this.throwResult = throwResult;
            this.active = active;
        }
    }

    private static List<CraftingPresetData> defaultPresets() {
        List<CraftingPresetData> list = new ArrayList<>();
        list.add(new CraftingPresetData(Items.COAL_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                -1, false, false, false));
        list.add(new CraftingPresetData(Items.IRON_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                -1, false, false, false));
        list.add(new CraftingPresetData(Items.GOLD_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                -1, false, false, false));
        list.add(new CraftingPresetData(Items.DIAMOND_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                -1, false, false, false));
        list.add(new CraftingPresetData(Items.REDSTONE_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.DISPENSER, CraftingPreset.UNTIL_EXHAUSTED,
                64, true, false, false));
        list.add(new CraftingPresetData(Items.STICKY_PISTON, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.REPEATER, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.COMPARATOR, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.OBSERVER, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.TNT, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        list.add(new CraftingPresetData(Items.SLIME_BLOCK, CraftingPreset.UNTIL_EXHAUSTED,
                64, false, false, false));
        return list;
    }

    public static synchronized long getRevision() {
        return revision;
    }

    public static synchronized List<CraftingPreset> toAssumptions() {
        List<CraftingPreset> list = new ArrayList<>(PRESETS.size());
        for (CraftingPresetData d : PRESETS) {
            list.add(d.toAssumption());
        }
        return list;
    }

    public static synchronized int size() {
        return PRESETS.size();
    }

    public static synchronized CraftingPresetData getPreset(int index) {
        return (index >= 0 && index < PRESETS.size()) ? PRESETS.get(index) : null;
    }

    public static synchronized String displayName(int index) {
        CraftingPresetData d = getPreset(index);
        if (d == null) return "?";
        Item item = parseItem(d.itemId);
        if (item == null || item == Items.AIR) return d.itemId == null ? "?" : d.itemId;
        return new ItemStack(item).getHoverName().getString();
    }

    public static synchronized String countText(int index) {
        CraftingPresetData d = getPreset(index);
        if (d == null) {
            return "";
        }
        return d.executeCount == CraftingPreset.UNTIL_EXHAUSTED ? "∞" : String.valueOf(d.executeCount);
    }

    public static synchronized void addFromHeld(ItemStack held) {
        if (held == null || held.isEmpty()) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        PRESETS.add(new CraftingPresetData(id, 1, 64, true, false, true));
        touch();
    }

    public static synchronized void setItem(int index, ItemStack held) {
        CraftingPresetData d = safeGet(index);
        if (d == null || held == null || held.isEmpty()) {
            return;
        }
        d.itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        touch();
    }

    public static synchronized void setActive(int index, boolean value) {
        CraftingPresetData d = safeGet(index);
        if (d == null) {
            return;
        }
        d.active = value;
        touch();
    }

    public static synchronized void toggleRecursive(int index) {
        CraftingPresetData d = safeGet(index);
        if (d == null) {
            return;
        }
        d.recursive = !d.recursive;
        touch();
    }

    public static synchronized void toggleThrow(int index) {
        CraftingPresetData d = safeGet(index);
        if (d == null) {
            return;
        }
        d.throwResult = !d.throwResult;
        touch();
    }

    public static synchronized boolean setExecuteCount(int index, int value) {
        CraftingPresetData d = safeGet(index);
        if (d == null) return false;
        if (value != CraftingPreset.UNTIL_EXHAUSTED && value <= 0) return false;
        if (d.executeCount == value) return false;
        d.executeCount = value;
        touch();
        return true;
    }

    /**
     * 运行态消耗后回写剩余执行次数
     */
    public static synchronized void updateRemaining(int index, int remaining) {
        CraftingPresetData d = safeGet(index);
        if (d == null) return;
        if (d.executeCount == CraftingPreset.UNTIL_EXHAUSTED) return;
        if (d.executeCount == remaining) return;
        d.executeCount = remaining;
        INSTANCE.setDirty(true);
    }

    public static synchronized boolean isUnlimited(int index) {
        CraftingPresetData d = getPreset(index);
        return d != null && d.executeCount == CraftingPreset.UNTIL_EXHAUSTED;
    }

    public static synchronized void toggleUnlimited(int index) {
        CraftingPresetData d = safeGet(index);
        if (d == null) return;
        d.executeCount = (d.executeCount == CraftingPreset.UNTIL_EXHAUSTED)
                ? 64 : CraftingPreset.UNTIL_EXHAUSTED;
        touch();
    }

    public static synchronized void remove(int index) {
        if (index < 0 || index >= PRESETS.size()) {
            return;
        }
        PRESETS.remove(index);
        touch();
    }

    public static synchronized void move(int index, int dir) {
        if (index < 0 || index >= PRESETS.size()) {
            return;
        }
        int target = index + dir;
        if (target < 0 || target >= PRESETS.size()) {
            return;
        }
        Collections.swap(PRESETS, index, target);
        touch();
    }

    public static synchronized void clearAll() {
        if (PRESETS.isEmpty()) {
            return;
        }
        PRESETS.clear();
        touch();
    }

    public static synchronized void restoreDefaults() {
        PRESETS.clear();
        List<CraftingPresetData> defaults = defaultsSupplier.get();
        if (defaults != null) {
            PRESETS.addAll(defaults);
        }
        touch();
    }

    private static CraftingPresetData safeGet(int index) {
        return (index >= 0 && index < PRESETS.size()) ? PRESETS.get(index) : null;
    }

    private static void touch() {
        INSTANCE.setDirty(true);
        ++revision;
        INSTANCE.save();
    }

    @Override
    protected void clearData() {
        PRESETS.clear();
    }

    @Override
    protected Object snapshot() {
        return PRESETS;
    }

    @Override
    protected void restore(String json) {
        List<CraftingPresetData> parsed = gson.fromJson(json,
                new TypeToken<List<CraftingPresetData>>() {
                }.getType());
        if (parsed != null) {
            PRESETS.addAll(parsed);
        }
    }

    @Override
    protected void onMissingFile() {
        // 新路径不存在时：优先尝试从旧版文件迁移，迁移不成再恢复默认预设
        if (migrateFromLegacy()) {
            return;
        }
        restoreDefaults();
    }

    private boolean migrateFromLegacy() {
        if (!Files.exists(LEGACY_PATH)) {
            return false;
        }
        try {
            String json = Files.readString(LEGACY_PATH);
            List<CraftingPresetData> parsed = gson.fromJson(json,
                    new TypeToken<List<CraftingPresetData>>() {
                    }.getType());
            if (parsed == null || parsed.isEmpty()) {
                return false;
            }
            PRESETS.addAll(parsed);
            touch();
            try {
                Files.move(LEGACY_PATH,
                        LEGACY_PATH.resolveSibling("presets.json.migrated.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            System.out.println("[SymAuto] 已迁移旧版合成预设 " + parsed.size() + " 条 -> " + path);
            // 删除旧版文件
            Files.delete(LEGACY_PATH);
            return true;
        } catch (Exception e) {
            System.err.println("[SymAuto] 迁移旧版合成预设失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void afterLoad() {
        revision++;
    }

    @Override
    protected String describe() {
        return "合成预设";
    }
}
