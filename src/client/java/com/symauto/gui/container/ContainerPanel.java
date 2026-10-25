package com.symauto.gui.container;

import com.betterbundle.gui.BundlePanelInteraction;
import com.symauto.mixin.accessor.AbstractRecipeBookScreenAccessor;
import com.symauto.function.base.SymContainer;
import com.symauto.function.container.ContainerPanelFunction;
import com.symauto.mixin.accessor.ScreenRebuildWidgetsAccessor;
import com.symauto.model.container.ContainerCategory;
import com.symauto.model.container.ContainerPanelModel;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ContainerPanel {
    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 5;
    public static final int VISIBLE_ROWS = 8;
    public static final int SLOT_SPACING = 1;
    public static final int PADDING = 3;
    public static final int SCROLL_BAR_WIDTH = 4;
    public static final int CAT_BAR_WIDTH = 18;
    public static final int SEARCH_BAR_HEIGHT = 14;
    public static final int TOGGLE_SIZE = 20;

    private static final WidgetSprites NONE_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("symauto", "empty_button"),
            Identifier.fromNamespaceAndPath("symauto", "empty_button"));

    private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("symauto", "bundle_button"),
            Identifier.fromNamespaceAndPath("symauto", "bundle_button_highlighted"));

    private static final WidgetSprites SHULKER_BOX_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath("symauto", "shulker_box_button"),
            Identifier.fromNamespaceAndPath("symauto", "shulker_box_button_highlighted"));

    @Getter
    private static boolean visible = false;
    private static SymContainer.Type activeType = SymContainer.Type.BUNDLE;
    private static int scrollOffset = 0;
    @Getter
    private static boolean searchFocused = false;
    private static String query = "";

    private static final GuiEventListener SEARCH_HANDLER = new GuiEventListener() {
        @Override
        public boolean charTyped(CharacterEvent event) {
            if (!visible || !searchFocused) {
                return false;
            }
            handleCharTyped(event.codepoint());
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
            searchFocused = focused;
        }

        @Override
        public boolean isFocused() {
            return searchFocused;
        }

    };

    private static void updateSearchFocus(boolean focused) {
        searchFocused = focused;
        syncSearchFocus();
    }

    private static void syncSearchFocus() {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null) {
            return;
        }
        if (searchFocused) {
            if (screen.getFocused() != SEARCH_HANDLER) {
                screen.setFocused(SEARCH_HANDLER);
            }
        } else if (screen.getFocused() == SEARCH_HANDLER) {
            screen.setFocused(null);
        }
    }

    private static final ContainerPanelModel model = new ContainerPanelModel();

    private ContainerPanel() {
    }

    // --- 状态 ---

    public static SymContainer.Type activeType() {
        return activeType;
    }

    /**
     * 隐藏面板（功能关闭时调用）。
     */
    public static void hide() {
        visible = false;
        resetSearch();
    }

    /** 清空搜索状态 */
    private static void resetSearch() {
        updateSearchFocus(false);
        query = "";
        model.setQuery("");
    }

    public static void toggle() {
        closeRecipeBook();
        if (!visible) {
            SymContainer.Type first = firstEnabledType();
            if (first == null) {
                return;
            }
            visible = true;
            activeType = first;
            scrollOffset = 0;
            return;
        }
        SymContainer.Type next = nextEnabledType(activeType);
        if (next == null) {
            visible = false;
            resetSearch();
        } else {
            activeType = next;
            scrollOffset = 0;
        }
    }

    /** 搜索框有焦点时接收字符键入 */
    public static void handleCharTyped(int codepoint) {
        if (!searchFocused) {
            return;
        }
        if (codepoint >= 32 && codepoint < 127 && codepoint != ' ') {
            query += (char) codepoint;
            scrollOffset = 0;
            model.setQuery(query);
        } else if (codepoint == ' ') {
            query += ' ';
            scrollOffset = 0;
            model.setQuery(query);
        }
    }

    /** 搜索框有焦点时处理功能键 */
    public static void handleSearchKey(int key) {
        if (!searchFocused) {
            return;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scrollOffset = 0;
                model.setQuery(query);
            }
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) {
                query = "";
                scrollOffset = 0;
                model.setQuery(query);
            } else {
                updateSearchFocus(false);
            }
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_TAB) {
            updateSearchFocus(false);
        }
    }

    /**
     * 关闭配方书
     */
    private static void closeRecipeBook() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AbstractRecipeBookScreen<?> screen) {
            RecipeBookComponent component = ((AbstractRecipeBookScreenAccessor) screen).getRecipeBookComponent();
            if (component != null && component.isVisible()) {
                component.toggleVisibility();
                ((ScreenRebuildWidgetsAccessor) (Object) screen).symauto$rebuildWidgets();
            }
        }
    }

    private static boolean typeEnabled(SymContainer.Type t) {
        ContainerPanelFunction f = ContainerPanelFunction.INSTANCE;
        return t == SymContainer.Type.BUNDLE ? f.isBundlePanelEnabled() : f.isShulkerBoxPanelEnabled();
    }

    private static SymContainer.Type firstEnabledType() {
        if (typeEnabled(SymContainer.Type.BUNDLE)) {
            return SymContainer.Type.BUNDLE;
        }
        if (typeEnabled(SymContainer.Type.SHULKER_BOX)) {
            return SymContainer.Type.SHULKER_BOX;
        }
        return null;
    }

    private static SymContainer.Type nextEnabledType(SymContainer.Type current) {
        if (current == SymContainer.Type.BUNDLE && typeEnabled(SymContainer.Type.SHULKER_BOX)) {
            return SymContainer.Type.SHULKER_BOX;
        }
        return null;
    }

    private static boolean isRecipeBookExtended() {
        if (Minecraft.getInstance().gui.screen() instanceof AbstractRecipeBookScreen<?> screen) {
            RecipeBookComponent component = ((AbstractRecipeBookScreenAccessor) screen).getRecipeBookComponent();
            return component != null && component.isVisible();
        }
        return false;
    }


    public static int panelWidth() {
        return CAT_BAR_WIDTH + 2 + SCROLL_BAR_WIDTH + 2
                + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING + PADDING * 2;
    }

    public static int toggleX(int leftPos, int imageWidth) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client.gui.screen();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        if (screen instanceof InventoryScreen) {
            return Math.clamp(leftPos + 130, 4, screenWidth - TOGGLE_SIZE - 4);
        }
        if (screen instanceof CreativeModeInventoryScreen) {
            // 创造界面：由于分类标签占据顶部/左侧，按钮贴 GUI 右上外侧，放不下翻到左上外侧
            int x = leftPos + imageWidth + 4;
            if (x + TOGGLE_SIZE > screenWidth - 4) {
                x = leftPos - TOGGLE_SIZE - 4;
            }
            return Math.clamp(x, 4, screenWidth - TOGGLE_SIZE - 4);
        }
        int desired = leftPos + (imageWidth - TOGGLE_SIZE) / 2;
        return Math.clamp(desired, 4, screenWidth - TOGGLE_SIZE - 4);
    }

    public static int toggleY(int topPos) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client.gui.screen();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int desired;
        if (screen instanceof InventoryScreen) {
            desired = topPos + 60;
        } else if (screen instanceof CreativeModeInventoryScreen) {
            desired = topPos + 4;
        } else {
            desired = topPos - 22;
        }
        return Math.clamp(desired, 4, screenHeight - TOGGLE_SIZE - 4);
    }

    private static int panelX(int leftPos) {
        return leftPos - panelWidth() - 4;
    }

    private static int searchH() {
        return SEARCH_BAR_HEIGHT + 3;
    }

    private static int gridH() {
        return PADDING * 2 + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING;
    }

    private static int panelHeight(int imageHeight) {
        return Math.min(imageHeight, searchH() + gridH()) + 16;
    }

    public static boolean isInsidePanel(double mx, double my, int leftPos, int topPos, int imageHeight) {
        int px = panelX(leftPos);
        int pw = panelWidth();
        int ph = panelHeight(imageHeight);
        return mx >= px && mx <= px + pw && my >= topPos && my <= topPos + ph;
    }

    /**
     * 消费面板区域内的点击
     */
    public static boolean handleClick(double mx, double my, int leftPos, int topPos,
                                      int imageHeight, int button) {
        int px = panelX(leftPos);
        ContainerCategory cat = categoryAt(mx, my, px, topPos, imageHeight);
        if (cat != null) {
            model.setCategory(cat);
            scrollOffset = 0;
            return true;
        }
        if (isInsideSearchBar(mx, my, px, topPos)) {
            updateSearchFocus(true);
            return true;
        }
        updateSearchFocus(false);

        int gridTop = topPos + searchH();
        int gridX = px + PADDING + CAT_BAR_WIDTH + 2 + SCROLL_BAR_WIDTH + 2;
        int gridY = gridTop + PADDING;
        int col = (int) (mx - gridX) / (SLOT_SIZE + SLOT_SPACING);
        int row = (int) (my - gridY) / (SLOT_SIZE + SLOT_SPACING);
        if (col >= 0 && col < COLUMNS && row >= 0 && row < VISIBLE_ROWS) {
            int idx = (scrollOffset + row) * COLUMNS + col;
            List<ContainerPanelModel.Entry> items = model.visible();
            if (idx >= 0 && idx < items.size()) {
                if (button == 0) {
                    onEntryClicked(items.get(idx));
                }
                return true;
            }
        }
        return isInsidePanel(mx, my, leftPos, topPos, imageHeight);
    }

    public static void handleScroll(double mx, double my, double scrollDelta,
                                    int leftPos, int topPos, int imageHeight) {
        if (!isInsidePanel(mx, my, leftPos, topPos, imageHeight)) {
            return;
        }
        scrollBy(scrollDelta > 0 ? -1 : 1);
    }

    private static void scrollBy(int delta) {
        List<ContainerPanelModel.Entry> items = model.visible();
        if (items.isEmpty()) {
            scrollOffset = 0;
            return;
        }
        int totalRows = (items.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollOffset = Math.clamp(scrollOffset + delta, 0, maxScroll);
    }

    /**
     * 收纳袋：取回该物品；潜影盒等：只读，无动作
     */
    private static void onEntryClicked(ContainerPanelModel.Entry entry) {
        if (activeType != SymContainer.Type.BUNDLE) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            return;
        }
        List<BundlePanelInteraction.ExtractTarget> targets = new ArrayList<>();
        for (ContainerPanelModel.SourceRef sr : entry.sources()) {
            int menuSlot = menuSlotFor(player, sr.hostInvIndex());
            if (menuSlot < 0) {
                continue;
            }
            targets.add(new BundlePanelInteraction.ExtractTarget(menuSlot, sr.entryIndex(), sr.count()));
        }
        if (!targets.isEmpty()) {
            BundlePanelInteraction.extract(targets, entry.stack().getMaxStackSize());
        }
    }

    private static int menuSlotFor(Player player, int invIndex) {
        if (invIndex < 0) {
            return -1;
        }
        Inventory inv = player.getInventory();
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == inv && slot.getContainerSlot() == invIndex) {
                return slot.index;
            }
        }
        return invIndex;
    }

    private static ContainerCategory categoryAt(double mx, double my, int px, int topPos, int imageHeight) {
        int ph = panelHeight(imageHeight);
        int catX = px + PADDING - 5;
        ContainerCategory[] cats = ContainerCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int by = topPos + PADDING - 3 + i * CAT_BAR_WIDTH;
            if (by + CAT_BAR_WIDTH > topPos + ph) {
                break;
            }
            boolean selected = cats[i] == model.category();
            int bx = selected ? catX - 5 : catX;
            int bw = selected ? CAT_BAR_WIDTH + 5 : CAT_BAR_WIDTH;
            if (mx >= bx && mx < bx + bw && my >= by && my < by + CAT_BAR_WIDTH) {
                return cats[i];
            }
        }
        return null;
    }

    private static boolean isInsideSearchBar(double mx, double my, int px, int topPos) {
        int sbx = px + PADDING + CAT_BAR_WIDTH + 2;
        int sby = topPos + 2;
        int sbw = panelWidth() - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
        return mx >= sbx && mx <= sbx + sbw && my >= sby && my <= sby + SEARCH_BAR_HEIGHT;
    }

    // --- 渲染 ---

    public static void render(GuiGraphicsExtractor g, int leftPos, int topPos, int imageHeight,
                              int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        if (isRecipeBookExtended()) {
            hide();
            return;
        }
        syncSearchFocus();
        if (!typeEnabled(activeType)) {
            SymContainer.Type fallback = firstEnabledType();
            if (fallback == null) {
                hide();
                return;
            }
            activeType = fallback;
            scrollOffset = 0;
        }
        model.setType(activeType);
        model.rebuild();

        int pw = panelWidth();
        int px = panelX(leftPos);
        int py = topPos;
        int sh = searchH();
        int ph = panelHeight(imageHeight);

        int bg = 0x80202020;
        int borderColor = 0xCC606060;
        g.fill(px, py, px + pw, py + ph, bg);
        g.fill(px, py, px + pw, py + 1, borderColor);
        g.fill(px, py + ph - 1, px + pw, py + ph, borderColor);
        g.fill(px, py, px + 1, py + ph, borderColor);
        g.fill(px + pw - 1, py, px + pw, py + ph, borderColor);

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        renderCategories(g, font, px, py, ph, mouseX, mouseY);
        renderSearch(g, font, px, py, pw, mouseX, mouseY);

        List<ContainerPanelModel.Entry> items = model.visible();
        if (items.isEmpty()) {
            String hint = model.containerCount() == 0
                    ? (activeType == SymContainer.Type.BUNDLE ? "背包中没有收纳袋" : "背包中没有潜影盒")
                    : "（当前筛选为空）";
            int textW = font.width(hint);
            g.text(font, hint, px + (pw - textW) / 2, py + ph / 2 - font.lineHeight / 2, 0xFF888888, false);
            return;
        }

        int gridTop = py + sh;
        int sbX = px + PADDING + CAT_BAR_WIDTH + 2;
        int sbY = gridTop + PADDING;
        int sbH = ph - sh - PADDING * 2;
        int totalRows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        g.fill(sbX, sbY, sbX + SCROLL_BAR_WIDTH, sbY + sbH, 0x30FFFFFF);
        if (maxScroll > 0) {
            int thumbH = Math.max(12, sbH * VISIBLE_ROWS / totalRows);
            int thumbY = sbY + (sbH - thumbH) * scrollOffset / maxScroll;
            g.fill(sbX, thumbY, sbX + SCROLL_BAR_WIDTH, thumbY + thumbH, 0x50FFFFFF);
        }

        int gridX = sbX + SCROLL_BAR_WIDTH + 2;
        int gridY = gridTop + PADDING;
        int hovered = -1;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int idx = (scrollOffset + row) * COLUMNS + col;
                if (idx >= items.size()) {
                    break;
                }
                int sx = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int sy = gridY + row * (SLOT_SIZE + SLOT_SPACING);
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0x40FFFFFF);
                g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x50FFFFFF);
                ContainerPanelModel.Entry e = items.get(idx);
                g.item(e.stack(), sx + 1, sy + 1);
                g.itemDecorations(font, e.stack(), sx + 1, sy + 1);
                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                    hovered = idx;
                }
            }
        }

        if (hovered >= 0) {
            int hRow = hovered / COLUMNS - scrollOffset;
            int hCol = hovered % COLUMNS;
            int hx = gridX + hCol * (SLOT_SIZE + SLOT_SPACING);
            int hy = gridY + hRow * (SLOT_SIZE + SLOT_SPACING);
            g.fill(hx, hy, hx + SLOT_SIZE, hy + SLOT_SIZE, 0x60FFFFFF);
            g.setTooltipForNextFrame(font, items.get(hovered).stack(), mouseX, mouseY);
        }

        // 底部统计：已展示数量 + 面板类型
        String mode = activeType == SymContainer.Type.BUNDLE ? "收纳袋" : "潜影盒";
        String countText = model.size() + " 项 · " + mode;
        int textW = font.width(countText);
        int countX = gridX + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING - textW;
        int countY = py + ph - 11;
        g.fill(countX - 2, countY, countX + textW + 2, countY + font.lineHeight, 0x30FFFFFF);
        g.text(font, countText, countX, countY, 0xFFAAAAAA, false);
    }

    private static void renderCategories(GuiGraphicsExtractor g, Font font, int px, int py, int ph,
                                         int mouseX, int mouseY) {
        int catX = px + PADDING - 5;
        ContainerCategory[] cats = ContainerCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int by = py + PADDING - 3 + i * CAT_BAR_WIDTH;
            if (by + CAT_BAR_WIDTH > py + ph) {
                break;
            }
            boolean selected = cats[i] == model.category();
            int bx = selected ? catX - 5 : catX;
            int bw = selected ? CAT_BAR_WIDTH + 5 : CAT_BAR_WIDTH;
            boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + CAT_BAR_WIDTH;
            int bg = selected ? 0x25101010 : (hovered ? 0x40FFFFFF : 0x30FFFFFF);
            g.fill(bx, by, bx + bw, by + CAT_BAR_WIDTH, bg);
            int off = (CAT_BAR_WIDTH - 16) / 2;
            g.item(cats[i].icon(), bx + off, by + off);
        }
    }

    private static void renderSearch(GuiGraphicsExtractor g, Font font, int px, int py, int pw,
                                                           int mouseX, int mouseY) {
        int sbx = px + PADDING + CAT_BAR_WIDTH + 2;
        int sby = py + 2;
        int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
        boolean active = searchFocused;
        g.fill(sbx, sby, sbx + sbw, sby + SEARCH_BAR_HEIGHT, active ? 0x60000000 : 0x40FFFFFF);
        int textY = sby + (SEARCH_BAR_HEIGHT - font.lineHeight) / 2;

        // 闪烁
        boolean blinkOn = Util.getMillis() / 300L % 2L == 0L;
        String shown = query + (active && blinkOn ? "_" : "");
        if (shown.isEmpty()) {
            if (!active) {
                g.text(font, "Search...", sbx + 3, textY, 0xFF666666, false);
            }
        } else {
            g.text(font, shown, sbx + 3, textY, 0xFFFFFFFF, false);
        }

        boolean hovered = mouseX >= sbx && mouseX < sbx + sbw && mouseY >= sby && mouseY < sby + SEARCH_BAR_HEIGHT;
        if (hovered) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("目前仅支持物品 id 搜索"),
                    Component.literal("如 minecraft:stone 或 stone"),
                    Component.literal("不支持中文")), mouseX, mouseY);
        }
    }

    /**
     * 渲染切换按钮
     */
    public static void renderToggleButton(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + TOGGLE_SIZE && mouseY >= y && mouseY < y + TOGGLE_SIZE;
        ContainerPanelFunction instance = ContainerPanelFunction.INSTANCE;
        boolean isBundlePanelEnabled = instance.isBundlePanelEnabled();
        boolean isShulkerBoxPanelEnabled = instance.isShulkerBoxPanelEnabled();
        Identifier sprite;
        if (!visible) {
            if (isBundlePanelEnabled) {
                sprite = BUTTON_SPRITES.get(true, hovered);
            } else if (isShulkerBoxPanelEnabled) {
                sprite = SHULKER_BOX_SPRITES.get(true, hovered);
            } else {
                sprite = NONE_SPRITES.get(true, hovered);
            }
        } else if (activeType == SymContainer.Type.BUNDLE) {
            if (isShulkerBoxPanelEnabled) {
                sprite = SHULKER_BOX_SPRITES.get(true, hovered);
            } else {
                sprite = NONE_SPRITES.get(true, hovered);
            }
        } else {
            sprite = NONE_SPRITES.get(true, hovered);
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TOGGLE_SIZE, TOGGLE_SIZE);
    }
}