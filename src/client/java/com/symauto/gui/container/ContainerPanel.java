package com.symauto.gui.container;

import com.symauto.function.container.depend.BundleAccess;
import com.symauto.function.container.depend.Containers;
import com.symauto.function.utils.ClientGuard;
import com.symauto.gui.utils.SlotHighlighter;
import com.symauto.mixin.accessor.AbstractRecipeBookScreenAccessor;
import com.symauto.function.base.SymContainer;
import com.symauto.function.container.FasterContainerPanelFunction;
import com.symauto.mixin.accessor.ScreenRebuildWidgetsAccessor;
import com.symauto.model.container.ContainerCategory;
import com.symauto.model.container.ContainerPanelModel;
import lombok.Getter;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContainerPanel {
    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 5;
    public static final int VISIBLE_ROWS = 8;
    public static final int SLOT_SPACING = 1;
    public static final int PADDING = 3;
    public static final int SCROLL_BAR_WIDTH = 4;
    public static final int CAT_BAR_WIDTH = 18;
    // 选中页签相对普通页签的左凸出量
    private static final int CAT_SELECT_EXPAND = 4;
    // 搜索栏与分类列之间保留的间隙
    private static final int CAT_SEARCH_GAP = 6;
    public static final int SEARCH_BAR_HEIGHT = 14;
    public static final int TOGGLE_SIZE = 20;
    public static final int TOGGLE_HEIGHT = 18;

    public static final int FRAME_COLOR = 0xFB00FFFF;

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
    /**
     * 悬浮面板条目时，该条目来源容器在背包(0-35)中的槽位集合，用于角标高亮
     */
    private static final Set<Integer> hoveredHostInv = new HashSet<>();
    /** Shift+左键划过式入袋 */
    private static int lastShiftDragSlotIndex = -1;

    /**
     * 分类栏纵向滚动偏移
     */
    private static int catScrollOffset = 0;
    private static boolean pendingReleaseGuard = false;

    private record CategoryBar(int top, int slots, int scrollMax, boolean scrollable) {
    }

    /**
     * 当前类型下有内容的分类列表："全部" 恒在；
     * 其余分类仅当至少一件属于它的物品时保留
     */
    private static List<ContainerCategory> visibleCategories() {
        List<ContainerCategory> out = new ArrayList<>();
        for (ContainerCategory c : ContainerCategory.list()) {
            if (c.isAll() || model.hasContent(c)) {
                out.add(c);
            }
        }
        return out;
    }

    public static boolean isShiftDragActive() {
        if (!swiftToBundleEnabled()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client) || !visible) {
            return false;
        }
        Player player = client.player;
        if (player.containerMenu != player.inventoryMenu) {
            return false;
        }
        long win = client.getWindow().handle();
        boolean shiftDown = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean mouseDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        return shiftDown && mouseDown;
    }

    private static CategoryBar categoryBar(int py, int ph) {
        int total = Math.max(1, (ph - 4) / CAT_BAR_WIDTH);
        int size = visibleCategories().size();
        boolean scrollable = size > total;
        int slots = scrollable ? Math.max(1, total - 2) : total;
        int scrollMax = Math.max(0, size - slots);
        catScrollOffset = Math.clamp(catScrollOffset, 0, scrollMax);
        return new CategoryBar(py + 2, slots, scrollMax, scrollable);
    }

    private static int categorySlotY(CategoryBar bar, int slot) {
        return bar.top() + (bar.scrollable() ? 1 + slot : slot) * CAT_BAR_WIDTH;
    }

    /**
     * 命中上下翻钮：返回 -1 / 1，未命中返回 0
     */
    private static int categoryArrowAt(double mx, double my, int px, int py, int ph) {
        CategoryBar bar = categoryBar(py, ph);
        if (!bar.scrollable()) {
            return 0;
        }
        int x0 = px + PADDING + 2;
        if (mx < x0 - CAT_SELECT_EXPAND || mx > x0 + CAT_BAR_WIDTH + CAT_SELECT_EXPAND) {
            return 0;
        }
        if (my >= bar.top() && my < bar.top() + CAT_BAR_WIDTH) {
            return catScrollOffset > 0 ? -1 : 0;
        }
        int downY = categorySlotY(bar, bar.slots());
        if (my >= downY && my < downY + CAT_BAR_WIDTH) {
            return catScrollOffset < bar.scrollMax() ? 1 : 0;
        }
        return 0;
    }

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

    public static boolean consumeReleaseGuard() {
        if (pendingReleaseGuard) {
            pendingReleaseGuard = false;
            return true;
        }
        return false;
    }

    /**
     * 隐藏面板
     */
    public static void hide() {
        visible = false;
        resetSearch();
    }

    /**
     * 清空搜索状态
     */
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
            catScrollOffset = 0;
            return;
        }
        SymContainer.Type next = nextEnabledType(activeType);
        if (next == null) {
            hide();
        } else {
            activeType = next;
            scrollOffset = 0;
            catScrollOffset = 0;
        }
    }

    private static SymContainer.Type nextEnabledType(SymContainer.Type current) {
        if (current == SymContainer.Type.BUNDLE && typeEnabled(SymContainer.Type.SHULKER_BOX)) {
            return SymContainer.Type.SHULKER_BOX;
        }
        return null;
    }

    /**
     * 搜索框有焦点时接收字符键入
     */
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

    /**
     * 搜索框有焦点时处理功能键
     */
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
                ((ScreenRebuildWidgetsAccessor) screen).symauto$rebuildWidgets();
            }
        }
    }

    private static boolean typeEnabled(SymContainer.Type t) {
        FasterContainerPanelFunction f = FasterContainerPanelFunction.INSTANCE;
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
            desired = topPos + 61;
        } else if (screen instanceof CreativeModeInventoryScreen) {
            desired = topPos + 4;
        } else {
            desired = topPos - 20;
        }
        return Math.clamp(desired, 4, screenHeight - TOGGLE_HEIGHT - 4);
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
                                      int imageHeight, int button, boolean shift) {
        int px = panelX(leftPos);
        int arrow = categoryArrowAt(mx, my, px, topPos, panelHeight(imageHeight));
        if (arrow != 0) {
            catScrollOffset += arrow;
            pendingReleaseGuard = true;
            return true;
        }
        ContainerCategory cat = categoryAt(mx, my, px, topPos, imageHeight);
        if (cat != null) {
            model.setCategory(cat);
            scrollOffset = 0;
            pendingReleaseGuard = true;
            return true;
        }
        if (isInsideSearchBar(mx, my, px, topPos)) {
            updateSearchFocus(true);
            pendingReleaseGuard = true;
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
                    onEntryClicked(items.get(idx), shift);
                }
                pendingReleaseGuard = true;
                return true;
            }
        }
        if (isInsidePanel(mx, my, leftPos, topPos, imageHeight)) {
            if (button == 0 && activeType == SymContainer.Type.BUNDLE) {
                tryInsertCarried();
            }
            pendingReleaseGuard = true;
            return true;
        }
        return false;
    }

    public static void handleScroll(double mx, double my, double scrollDelta,
                                    int leftPos, int topPos, int imageHeight) {
        if (!isInsidePanel(mx, my, leftPos, topPos, imageHeight)) {
            return;
        }
        int px = panelX(leftPos);
        CategoryBar bar = categoryBar(topPos, panelHeight(imageHeight));
        if (bar.scrollable() && mx < px + PADDING + CAT_BAR_WIDTH + 2) {
            catScrollOffset = Math.clamp(catScrollOffset + (scrollDelta > 0 ? 1 : -1), 0, bar.scrollMax());
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
     * 收纳袋：左键面板物品；空光标 = 倒出一整条（Shift 则直接进入背包），有光标 = 塞入最佳收纳袋
     */
    private static void onEntryClicked(ContainerPanelModel.Entry entry, boolean shift) {
        if (activeType != SymContainer.Type.BUNDLE) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        Player player = client.player;
        if (!player.containerMenu.getCarried().isEmpty()) {
            tryInsertCarried();
            return;
        }
        if (entry.sources().isEmpty()) {
            return;
        }
        ContainerPanelModel.SourceRef src = entry.sources().getFirst();
        int menuSlot = menuSlotFor(player, src.hostInvIndex());
        if (menuSlot >= 0) {
            if (shift) {
                BundleAccess.extractToInventory(menuSlot, src.entryIndex(), entry.stack());
            } else {
                BundleAccess.extract(menuSlot, src.entryIndex());
            }
        }
    }

    /**
     * 拖入面板区域，把光标物品塞入最优收纳袋（优先同种聚集）
     */
    private static void tryInsertCarried() {
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        ItemStack carried = client.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        int bundleSlot = BundleAccess.findBestBundle(client.player, carried, -1);
        if (bundleSlot >= 0) {
            BundleAccess.insertCarried(bundleSlot);
        }
    }

    /**
     * "Shift + 左键优先进收纳袋"开关
     */
    private static boolean swiftToBundleEnabled() {
        FasterContainerPanelFunction cfg = FasterContainerPanelFunction.INSTANCE;
        return cfg.isBundlePanelEnabled() && cfg.isShiftAndLeftClickSwiftToBundle();
    }

    /**
     * 面板可见时：Shift + 左键 背包物品槽，存入最佳收纳袋
     */
    public static boolean tryShiftInsert(Slot hovered) {
        if (!swiftToBundleEnabled()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || !visible) {
            return false;
        }
        // 纯背包界面才启用自动入袋，否则改变原版行为
        if (player.containerMenu != player.inventoryMenu) {
            return false;
        }
        if (hovered == null || !hovered.hasItem() || hovered.container != player.getInventory()) {
            return false;
        }
        ItemStack stack = hovered.getItem();
        if (stack.getMaxStackSize() <= 1) {
            return false;
        }
        int bundleSlot = BundleAccess.findBestBundle(player, stack, hovered.index);
        if (bundleSlot < 0) {
            return false;
        }
        BundleAccess.insert(hovered.index, bundleSlot);
        return true;
    }

    /**
     * 按住 Shift + 左键不松，鼠标划过的每个背包槽自动存入最佳收纳袋（每槽只触发一次）
     */
    public static void handleShiftDragInsert(Slot hovered) {
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        long win = client.getWindow().handle();
        boolean shiftDown = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean mouseDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!shiftDown || !mouseDown) {
            lastShiftDragSlotIndex = -1;
            return;
        }
        if (!visible || hovered == null || !hovered.hasItem()) {
            return;
        }
        if (hovered.index == lastShiftDragSlotIndex) {
            return;
        }
        if (tryShiftInsert(hovered)) {
            lastShiftDragSlotIndex = hovered.index;
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
        CategoryBar bar = categoryBar(topPos, ph);
        List<ContainerCategory> cats = visibleCategories();
        int x0 = px + PADDING + 2;
        for (int i = 0; i < bar.slots(); i++) {
            int idx = catScrollOffset + i;
            if (idx >= cats.size()) {
                break;
            }
            int by = categorySlotY(bar, i);
            if (my < by || my >= by + CAT_BAR_WIDTH) {
                continue;
            }
            ContainerCategory c = cats.get(idx);
            boolean selected = c == model.category();
            int bx = selected ? x0 - CAT_SELECT_EXPAND : x0;
            int bw = selected ? CAT_BAR_WIDTH + CAT_SELECT_EXPAND : CAT_BAR_WIDTH;
            if (mx >= bx && mx < bx + bw) {
                return c;
            }
        }
        return null;
    }

    private static boolean isInsideSearchBar(double mx, double my, int px, int topPos) {
        int sbx = px + PADDING + CAT_BAR_WIDTH + 2 + CAT_SEARCH_GAP;
        int sby = topPos + 3;
        int sbw = panelWidth() - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10 - CAT_SEARCH_GAP;
        return mx >= sbx && mx <= sbx + sbw && my >= sby && my <= sby + SEARCH_BAR_HEIGHT;
    }

    public static void render(GuiGraphicsExtractor g, int leftPos, int topPos, int imageHeight,
                              int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        hoveredHostInv.clear();
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
        if (!visibleCategories().contains(model.category())) {
            model.setCategory(ContainerCategory.all());
        }

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
            String hint = getHint();
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
                renderEntryCount(g, font, e.stack(), sx + 1, sy + 1);
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
            ItemStack hoveredStack = items.get(hovered).stack();
            for (ContainerPanelModel.SourceRef src : items.get(hovered).sources()) {
                int inv = src.hostInvIndex();
                if (inv >= 0) {
                    hoveredHostInv.add(inv);
                }
            }
            g.setComponentTooltipForNextFrame(font, List.of(
                    hoveredStack.getDisplayName(),
                    Component.literal("数量: " + hoveredStack.getCount()).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
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

    /**
     * 悬浮面板物品时，在背包中“装有该物品的容器本体”槽位右下角角标
     */
    public static void renderHostHighlights(GuiGraphicsExtractor g, int leftPos, int topPos) {
        if (!visible) {
            return;
        }
        SlotHighlighter.highlightInventorySlots(g, leftPos, topPos, hoveredHostInv, FRAME_COLOR);
    }

    /**
     * 悬浮在界面内任意槽位物品上时，高亮背包中所有装有该物品的容器槽位
     */
    public static void renderHoveredItemHighlights(GuiGraphicsExtractor g, Slot hoveredSlot,
                                                   int leftPos, int topPos) {
        if (!FasterContainerPanelFunction.INSTANCE.isHighlightContainerContainsThisItem()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client) || hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }
        ItemStack target = hoveredSlot.getItem();
        List<Slot> hits = new ArrayList<>();
        for (Slot slot : client.player.containerMenu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            SymContainer container = Containers.of(stack).orElse(null);
            if (container == null) {
                continue;
            }
            for (ItemStack entry : container.entries()) {
                if (ItemStack.isSameItemSameComponents(entry, target)) {
                    hits.add(slot);
                    break;
                }
            }
        }
        SlotHighlighter.highlightSlots(g, leftPos, topPos, hits, FRAME_COLOR);
    }

    private static @NonNull String getHint() {
        String hint = "";
        if (model.containerCount() == 0) {
            if (activeType == SymContainer.Type.BUNDLE) {
                hint = "背包中没有收纳袋";
            } else if (activeType == SymContainer.Type.SHULKER_BOX) {
                hint = "背包中没有潜影盒";
            }
        } else if (model.totalItems() == 0) {
            if (activeType == SymContainer.Type.BUNDLE) {
                hint = "收纳袋暂无物品";
            } else if (activeType == SymContainer.Type.SHULKER_BOX) {
                hint = "潜影盒暂无物品";
            }
        } else {
            hint = "（当前筛选为空）";
        }
        return hint;
    }

    /**
     * 计数显示，<=999 用原版角标；> 1000 缩写为 k/M
     */
    private static void renderEntryCount(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y) {
        int count = stack.getCount();
        if (count <= 0) {
            return;
        }
        if (count < 1000) {
            g.itemDecorations(font, stack, x, y);
            return;
        }
        String text = abbreviate(count);
        g.text(font, text, x + SLOT_SIZE - 3 - font.width(text), y + SLOT_SIZE - 3 - font.lineHeight / 2,
                0xFFFFFFFF, true);
    }

    private static String abbreviate(int count) {
        if (count < 1_000_000) {
            return (count / 1000) + "k";
        }
        return (count / 1_000_000) + "M";
    }

    private static void renderCategories(GuiGraphicsExtractor g, Font font, int px, int py, int ph,
                                         int mouseX, int mouseY) {
        CategoryBar bar = categoryBar(py, ph);
        List<ContainerCategory> cats = visibleCategories();
        int x0 = px + PADDING + 2;
        if (bar.scrollable()) {
            renderArrow(g, x0, bar.top(), true, catScrollOffset > 0);
            renderArrow(g, x0, categorySlotY(bar, bar.slots()), false, catScrollOffset < bar.scrollMax());
        }
        int off = (CAT_BAR_WIDTH - 16) / 2;
        for (int i = 0; i < bar.slots(); i++) {
            int idx = catScrollOffset + i;
            if (idx >= cats.size()) {
                break;
            }
            ContainerCategory c = cats.get(idx);
            int by = categorySlotY(bar, i);
            boolean selected = c == model.category();
            int bx = selected ? x0 - CAT_SELECT_EXPAND : x0;
            int bw = selected ? CAT_BAR_WIDTH + CAT_SELECT_EXPAND : CAT_BAR_WIDTH;
            boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + CAT_BAR_WIDTH;
            int bg = selected ? 0x25101010 : (hovered ? 0x40FFFFFF : 0x30FFFFFF);
            g.fill(bx, by, bx + bw, by + CAT_BAR_WIDTH, bg);
            g.item(c.icon(), x0 + off, by + off);
            if (hovered) {
                g.setComponentTooltipForNextFrame(font, List.of(Component.literal(c.displayName())),
                        mouseX, mouseY);
            }
        }
    }

    private static void renderArrow(GuiGraphicsExtractor g, int x, int y, boolean up, boolean enabled) {
        int cx = x + CAT_BAR_WIDTH / 2;
        int cy = y + CAT_BAR_WIDTH / 2;
        int color = enabled ? 0xFFFFFFFF : 0xFF555555;
        for (int i = 0; i < 5; i++) {
            int w = 9 - 2 * i;
            int row = up ? cy + 2 - i : cy - 2 + i;
            g.fill(cx - w / 2, row, cx - w / 2 + w, row + 1, color);
        }
    }

    private static void renderSearch(GuiGraphicsExtractor g, Font font, int px, int py, int pw,
                                     int mouseX, int mouseY) {
        int sbx = px + PADDING + CAT_BAR_WIDTH + 2 + CAT_SEARCH_GAP;
        int sby = py + 3;
        int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10 - CAT_SEARCH_GAP;
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
        boolean hovered = mouseX >= x && mouseX < x + TOGGLE_SIZE && mouseY >= y && mouseY < y + TOGGLE_HEIGHT;
        FasterContainerPanelFunction instance = FasterContainerPanelFunction.INSTANCE;
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
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TOGGLE_SIZE, TOGGLE_HEIGHT);
    }
}