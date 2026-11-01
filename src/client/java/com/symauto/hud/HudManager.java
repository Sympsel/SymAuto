package com.symauto.hud;

import com.symauto.core.Constants;
import com.symauto.hud.element.ClockHudElement;
import com.symauto.hud.element.CoordinatesHudElement;
import com.symauto.hud.element.FpsHudElement;
import com.symauto.storage.base.PersistentStore;
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

/**
 * HUD 元素注册表 + 布局管理 + 实际 HUD 渲染入口。
 *
 * <p>布局数据由 {@link HudLayoutStore} 持久化，编辑器 {@code HudEditorScreen}
 * 通过 {@link #copyPlacements()} / {@link #apply(Map)} 读写布局。</p>
 */
public final class HudManager {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud_layout");

    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static final Map<String, HudElement> BY_ID = new LinkedHashMap<>();
    private static final HudLayoutStore LAYOUT = new HudLayoutStore(() -> ELEMENTS);

    private static boolean initialized;
    private static boolean editing;

    private HudManager() {
    }

    /**
     * 注册内置 HUD 元素并挂载渲染层。必须在布局存储加载之前调用。
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 内置示例元素，后续新功能只需在此追加 register(...)
        register(new FpsHudElement());
        register(new CoordinatesHudElement());
        register(new ClockHudElement());

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, LAYER_ID, HudManager::extract);
        Constants.LOG.info("Registered {} HUD elements", ELEMENTS.size());
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
     * 取得元素的实时布局对象；不要在编辑器里直接修改它，请使用副本。
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
     * 把编辑器中的布局副本写回实时布局（尚未落盘）。
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
     * 编辑器打开期间抑制普通 HUD 渲染，由编辑器绘制可拖拽的预览。
     */
    public static void setEditing(boolean value) {
        editing = value;
    }

    public static boolean isEditing() {
        return editing;
    }

    /**
     * 按布局绘制单个元素。编辑器也复用此方法，保证预览与实际 HUD 完全一致。
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
            element.render(new HudRenderContext(graphics, font, baseWidth, baseHeight, preview));
        } finally {
            pose.popMatrix();
        }
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (editing) {
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
