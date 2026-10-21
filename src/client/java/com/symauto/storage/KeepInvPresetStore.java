package com.symauto.storage;

import com.google.common.reflect.TypeToken;
import com.symauto.model.preset.KeepInvPreset;
import com.symauto.storage.base.AbstractJsonStore;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.symauto.config.ConfigManager.parseItem;

public class KeepInvPresetStore extends AbstractJsonStore {
    public static final KeepInvPresetStore INSTANCE = new KeepInvPresetStore();
    private static final List<KeepInvPresetData> PRESETS = new ArrayList<>();

    private KeepInvPresetStore() {
        super("preset", "keep_inv.json");
    }

    @AllArgsConstructor
    public static final class KeepInvPresetData {
        public String itemId;
        public int maxCount;
        public int maxSlot;
        public KeepInvPreset.Mode mode;
        boolean enchantedItemGuard;
        public boolean active;

        KeepInvPreset toModel() {
            Item item = parseItem(itemId);
            return new KeepInvPreset(item == null ? Items.AIR : item,
                    maxCount, maxSlot, mode, enchantedItemGuard, active);
        }

        public KeepInvPresetData(Item item, int maxCount, int maxSlot, KeepInvPreset.Mode mode) {
            this.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            this.maxCount = maxCount;
            this.maxSlot = maxSlot;
            this.mode = mode;
            this.enchantedItemGuard = true;
            this.active = true;
        }
    }

    private static KeepInvPresetData safeGet(int index) {
        return (index >= 0 && index < PRESETS.size()) ? PRESETS.get(index) : null;
    }

    private static void touch() {
        INSTANCE.setDirty(true);
        INSTANCE.save();
    }

    public static synchronized int size() {
        return PRESETS.size();
    }

    public static synchronized KeepInvPresetData getPreset(int index) {
        return safeGet(index);
    }

    public static synchronized List<KeepInvPreset> toModels() {
        List<KeepInvPreset> list = new ArrayList<>(PRESETS.size());
        for (KeepInvPresetData d : PRESETS) {
            list.add(d.toModel());
        }
        return list;
    }

    public static synchronized String displayName(int index) {
        KeepInvPresetData d = safeGet(index);
        if (d == null) {
            return "?";
        }
        Item item = parseItem(d.itemId);
        if (item == null || item == Items.AIR) return d.itemId == null ? "?" : d.itemId;
        return new ItemStack(item).getHoverName().getString();
    }

    public static synchronized int maxCount(int index) {
        KeepInvPresetData d = safeGet(index);
        return d == null ? 0 : d.maxCount;
    }

    public static synchronized int maxSlot(int index) {
        KeepInvPresetData d = safeGet(index);
        return d == null ? 0 : d.maxSlot;
    }

    public static synchronized boolean isGuard(int index) {
        KeepInvPresetData d = safeGet(index);
        return d != null && d.enchantedItemGuard;
    }

    public static synchronized boolean isActive(int index) {
        KeepInvPresetData d = safeGet(index);
        return d != null && d.active;
    }

    public static synchronized void setActive(int index, boolean value) {
        KeepInvPresetData d = safeGet(index);
        if (d == null || d.active == value) {
            return;
        }
        d.active = value;
        touch();
    }

    public static synchronized String modeName(int index) {
        KeepInvPresetData d = safeGet(index);
        return d == null || d.mode == null ? "?" : d.mode.getLabel();
    }

    public static synchronized void addFromHeld(ItemStack held) {
        if (held == null || held.isEmpty()) return;
        PRESETS.add(new KeepInvPresetData(held.getItem(), 1, 1, KeepInvPreset.Mode.BOTH_CONSTRAINT));
        touch();
    }

    public static synchronized void toggleMode(int index) {
        KeepInvPresetData d = safeGet(index);
        if (d == null || d.mode == null) return;
        d.mode = switch (d.mode) {
            case COUNT_CONSTRAINT -> KeepInvPreset.Mode.SLOT_CONSTRAINT;
            case SLOT_CONSTRAINT -> KeepInvPreset.Mode.BOTH_CONSTRAINT;
            case BOTH_CONSTRAINT -> KeepInvPreset.Mode.COUNT_CONSTRAINT;
        };
        touch();
    }

    public static synchronized void toggleGuard(int index) {
        KeepInvPresetData d = safeGet(index);
        if (d == null) {
            return;
        }
        d.enchantedItemGuard = !d.enchantedItemGuard;
        touch();
    }

    public static synchronized boolean setMaxCount(int index, int value) {
        KeepInvPresetData d = safeGet(index);
        if (d == null || value < 0 || d.maxCount == value) {
            return false;
        }
        d.maxCount = value;
        touch();
        return true;
    }


    public static synchronized boolean setMaxSlot(int index, int value) {
        KeepInvPresetData d = safeGet(index);
        if (d == null || value < 0 || d.maxSlot == value) {
            return false;
        }
        d.maxSlot = value;
        touch();
        return true;
    }

    public static synchronized void remove(int index) {
        if (index < 0 || index >= PRESETS.size()) {
            return;
        }
        PRESETS.remove(index);
        touch();
    }

    public static synchronized void clearAll() {
        if (PRESETS.isEmpty()) {
            return;
        }
        PRESETS.clear();
        touch();
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
        List<KeepInvPresetData> parsed = gson.fromJson(json,
                new TypeToken<List<KeepInvPresetData>>() {
                }.getType());
        if (parsed != null) {
            PRESETS.addAll(parsed);
        }
    }

    @Override
    protected String describe() {
        return "背包保留预设";
    }
}
