package com.symbase.hud;

import com.symbase.storage.base.AbstractJsonStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * HUD 布局存储，默认路径：{@code config/symbase/hud/layout.json}
 */
public final class HudLayoutStore extends AbstractJsonStore {
    private static final int VERSION = 1;

    private final Supplier<List<HudElement>> elements;
    private final Map<String, HudPlacement> placements = new LinkedHashMap<>();

    HudLayoutStore(Supplier<List<HudElement>> elements) {
        super("symbase", "hud", "layout.json");
        this.elements = elements;
    }

    public synchronized HudPlacement getOrCreate(HudElement element) {
        HudPlacement placement = placements.get(element.id());
        if (placement == null) {
            placement = element.defaultPlacement();
            placements.put(element.id(), placement);
            setDirty(true);
        }
        return placement;
    }

    public synchronized void put(String id, HudPlacement placement) {
        placements.put(id, placement.copy());
        setDirty(true);
    }

    public synchronized void ensureDefaults() {
        for (HudElement element : elements.get()) {
            if (!placements.containsKey(element.id())) {
                placements.put(element.id(), element.defaultPlacement());
                setDirty(true);
            }
        }
    }

    @Override
    protected synchronized void clearData() {
        placements.clear();
        ensureDefaults();
    }

    @Override
    protected synchronized Object snapshot() {
        LayoutData data = new LayoutData();
        data.version = VERSION;
        for (Map.Entry<String, HudPlacement> entry : placements.entrySet()) {
            data.elements.put(entry.getKey(), LayoutEntry.from(entry.getValue()));
        }
        return data;
    }

    @Override
    protected synchronized void restore(String json) {
        LayoutData data = gson.fromJson(json, LayoutData.class);
        if (data == null || data.elements == null) {
            return;
        }
        for (Map.Entry<String, LayoutEntry> entry : data.elements.entrySet()) {
            LayoutEntry value = entry.getValue();
            if (value == null) {
                continue;
            }
            placements.put(entry.getKey(), value.toPlacement());
        }
        ensureDefaults();
    }

    @Override
    protected void onMissingFile() {
        setDirty(true);
    }

    @Override
    protected String describe() {
        return "HUD 布局";
    }

    private static final class LayoutData {
        private int version;
        private Map<String, LayoutEntry> elements = new LinkedHashMap<>();
    }

    private static final class LayoutEntry {
        private String anchor;
        private int x;
        private int y;
        private float scale;
        private Boolean visible;

        private static LayoutEntry from(HudPlacement placement) {
            LayoutEntry entry = new LayoutEntry();
            entry.anchor = placement.anchor().name();
            entry.x = placement.offsetX();
            entry.y = placement.offsetY();
            entry.scale = placement.scale();
            entry.visible = placement.visible();
            return entry;
        }

        private HudPlacement toPlacement() {
            float resolvedScale = scale > 0.0F ? scale : 1.0F;
            boolean resolvedVisible = visible == null || visible;
            return new HudPlacement(HudAnchor.byName(anchor), x, y, resolvedScale, resolvedVisible);
        }
    }
}


