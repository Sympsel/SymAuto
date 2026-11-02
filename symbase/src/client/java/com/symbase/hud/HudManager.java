package com.symbase.hud;

import com.symbase.core.SymBaseConstants;
import com.symbase.storage.base.PersistentStore;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HudManager {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(SymBaseConstants.MOD_ID, "hud_layout");

    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static final Map<String, HudElement> BY_ID = new LinkedHashMap<>();
    private static final HudLayoutStore LAYOUT = new HudLayoutStore(() -> ELEMENTS);

    private static boolean initialized;
    /**
     * -- SETTER --
     *  编辑器打开期间抑制普通 HUD 渲染，由编辑器绘制可拖拽的预览
     */
    @Setter
    @Getter
    private static boolean editing;

    @Setter
    @Getter
    private static boolean visible = true;

    private HudManager() {
    }

    /**
     * 挂载 HUD 渲染层
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LAYER_ID, HudManager::extract);
        SymBaseConstants.LOG.info("HUD framework initialized");
    }

    public static void loadLayout() {
        LAYOUT.load();
    }

    public static void saveLayout() {
        LAYOUT.saveIfDirty();
    }

    public static void register(HudElement element) {
        Objects.requireNonNull(element, "element");
        String id = element.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("HUD element id must not be blank");
        }
        if (BY_ID.putIfAbsent(id, element) != null) {
            throw new IllegalStateException("Duplicate HUD element id: " + id);
        }
        ELEMENTS.add(element);
        LAYOUT.getOrCreate(element);
    }

    public static List<HudElement> elements() {
        return Collections.unmodifiableList(ELEMENTS);
    }

    public static HudElement element(String id) {
        return BY_ID.get(id);
    }

    /**
     * 取得元素的实时布局对象
     */
    public static HudPlacement placement(String id) {
        HudElement element = BY_ID.get(id);
        return element == null ? null : LAYOUT.getOrCreate(element);
    }

    public static HudPlacement placement(HudElement element) {
        return LAYOUT.getOrCreate(element);
    }

    public static Map<String, HudPlacement> copyPlacements() {
        Map<String, HudPlacement> copy = new LinkedHashMap<>();
        for (HudElement element : ELEMENTS) {
            copy.put(element.id(), LAYOUT.getOrCreate(element).copy());
        }
        return copy;
    }

    /**
     * 把编辑器中的布局副本写回实时布局
     */
    public static void apply(Map<String, HudPlacement> placements) {
        for (HudElement element : ELEMENTS) {
            HudPlacement placement = placements.get(element.id());
            if (placement != null) {
                LAYOUT.put(element.id(), placement);
            }
        }
        LAYOUT.ensureDefaults();
    }

    public static void save() {
        LAYOUT.save();
    }

    public static PersistentStore store() {
        return LAYOUT;
    }

    /**
     * 按布局绘制单个元素
     */
    public static void renderElement(GuiGraphicsExtractor graphics, Font font, HudElement element,
                                     HudPlacement placement, int screenWidth, int screenHeight,
                                     boolean preview) {
        int baseWidth = Math.max(1, element.width(font));
        int baseHeight = Math.max(1, element.height(font));
        HudBounds bounds = placement.resolve(font, element, screenWidth, screenHeight);

        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(bounds.x(), bounds.y());
            if (placement.scale() != 1.0F) {
                pose.scale(placement.scale(), placement.scale());
            }
            element.render(new HudRenderContext(graphics, font, baseWidth, baseHeight, preview, placement.textColor()));
        } finally {
            pose.popMatrix();
        }
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (editing || !visible) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        Font font = client.font;
        for (HudElement element : ELEMENTS) {
            HudPlacement placement = LAYOUT.getOrCreate(element);
            if (!placement.visible()) {
                continue;
            }
            renderElement(graphics, font, element, placement, screenWidth, screenHeight, false);
        }
    }
}




