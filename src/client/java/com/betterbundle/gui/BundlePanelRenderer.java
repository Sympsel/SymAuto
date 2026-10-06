package com.betterbundle.gui;

import com.betterbundle.mixin.AbstractRecipeBookScreenAccessor;
import com.betterbundle.util.BundleContentsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BundlePanelRenderer {
    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 5;
    public static final int VISIBLE_ROWS = 8;
    public static final int SLOT_SPACING = 1;
    public static final int PADDING = 3;
    public static final int SCROLL_BAR_WIDTH = 4;
    public static final int CAT_BUTTON_SIZE = 18;
    public static final int CAT_BAR_WIDTH = CAT_BUTTON_SIZE;
    public static final int SEARCH_BAR_HEIGHT = 14;

    private static int scrollOffset = 0;
    public static boolean visible = true;

    public static String searchQuery = "";
    public static boolean searchFocused = false;
    private static int searchCursorTick = 0;
    private static int hoveredBundleSlot = -1;

    public static BundleCategory currentCategory = BundleCategory.ALL;

    private BundlePanelRenderer() {}

    /** bundleSlot = container slot index (for clickSlot packets) */
    public record BundleSlotEntry(int bundleSlot, ItemStack bundleStack, BundleContents contents) {}

    public static int panelWidth() {
        return CAT_BAR_WIDTH + 2 + SCROLL_BAR_WIDTH + 2
                + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING + PADDING * 2;
    }


    public static int toggleX(int leftPos, int imageWidth) {
        Minecraft client = Minecraft.getInstance();
        boolean isInventoryScreen = client.gui.screen() instanceof InventoryScreen;
        int screenWidth = client.getWindow().getGuiScaledWidth();

        // 背包界面固定位置；其他容器界面居中于 GUI 顶部
        int desired = isInventoryScreen
                ? leftPos + 130
                : leftPos + (imageWidth - 20) / 2;

        return Math.clamp(desired, 4, screenWidth - 20 - 4);
    }

    public static int toggleY(int topPos) {
        Minecraft client = Minecraft.getInstance();
        int desired = client.gui.screen() instanceof InventoryScreen
                ? topPos + 60
                : topPos - 22;
        return Math.clamp(desired, 4,
                Math.max(4, client.getWindow().getGuiScaledHeight() - 20 - 4));
    }

    public static int getScrollOffset() { return scrollOffset; }
    public static void scrollToTop() { scrollOffset = 0; }

    public static void scrollBy(int delta) {
        List<BundleSlotEntry> bundles = getBundles();
        if (bundles.isEmpty()) { scrollOffset = 0; return; }
        List<FlatItem> items = buildFlatItemList(bundles);
        int totalRows = (items.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollOffset = Math.clamp(scrollOffset + delta, 0, maxScroll);
    }

    /** 记录某个收纳袋中某个条目的位置和数量 */
    public record SourceEntry(int bundleSlot, int itemIndex, int itemCount) {}

    /** 按物品类型分组后的显示条目，同一物品的所有来源聚合在一起 */
    public record FlatItem(List<SourceEntry> sources, ItemStack stack) {}

    /** 按物品类型分组：跨收纳袋合并同种物品，每种物品只占一个格子 */
    public static List<FlatItem> buildFlatItemList(List<BundleSlotEntry> bundles) {
        // 按物品类型分组（保持插入顺序）
        List<FlatItem> groups = new ArrayList<>();
        for (BundleSlotEntry entry : bundles) {
            if (entry.contents() == null) continue;
            List<ItemStack> items = entry.contents().itemCopyStream().toList();
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                // 查找已有的同种物品组
                FlatItem existing = null;
                for (FlatItem g : groups) {
                    if (ItemStack.isSameItemSameComponents(g.stack(), item)) {
                        existing = g;
                        break;
                    }
                }
                if (existing != null) {
                    // 合并到已有组：数量相加，追加来源
                    existing.stack().grow(item.getCount());
                    existing.sources().add(new SourceEntry(entry.bundleSlot(), i, item.getCount()));
                } else {
                    // 新建组
                    List<SourceEntry> sources = new ArrayList<>();
                    sources.add(new SourceEntry(entry.bundleSlot(), i, item.getCount()));
                    groups.add(new FlatItem(sources, item.copy()));
                }
            }
        }
        return groups;
    }

    public static List<FlatItem> filterItems(List<FlatItem> items, String query) {
        List<FlatItem> filtered = new ArrayList<>();
        for (FlatItem fi : items) {
            String key = BuiltInRegistries.ITEM.getKey(fi.stack().getItem()).toString();
            if (currentCategory.matches(key)) filtered.add(fi);
        }
        if (query.isEmpty()) return filtered;
        String q = query.toLowerCase(Locale.ROOT);
        List<FlatItem> sorted = new ArrayList<>(filtered);
        sorted.sort(Comparator.comparing((FlatItem fi) -> matchesSearch(fi, q) ? 0 : 1));
        return sorted;
    }

    private static boolean matchesSearch(FlatItem fi, String q) {
        String name = fi.stack().getDisplayName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(q)) return true;
        if (toPinyin(name).contains(q)) return true;
        var key = BuiltInRegistries.ITEM.getKey(fi.stack().getItem());
        String fullId = key.toString().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);
        return fullId.contains(q) || path.contains(q);
    }

    private static String toPinyin(String text) {
        try {
            HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
            fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                if (arr != null && arr.length > 0) sb.append(arr[0]);
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return "";
        }
    }

    public static List<BundleSlotEntry> getBundles() { return findBundles(false); }
    public static List<BundleSlotEntry> getAllBundles() { return findBundles(true); }

    private static List<BundleSlotEntry> findBundles(boolean includeEmpty) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return List.of();
        List<BundleSlotEntry> result = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            boolean matches = includeEmpty
                    ? BundleContentsHelper.isBundle(stack)
                    : BundleContentsHelper.isNonEmptyBundle(stack);
            if (matches) {
                // Convert inventory index to container slot index
                int containerSlot = findContainerSlot(player, inv, i);
                result.add(new BundleSlotEntry(containerSlot, stack, BundleContentsHelper.getContents(stack)));
            }
        }
        return result;
    }

    /** Convert player inventory index (0-35) to container menu slot index. */
    private static int findContainerSlot(Player player, Inventory inv, int inventoryIndex) {
        for (net.minecraft.world.inventory.Slot slot : player.containerMenu.slots) {
            if (slot.container == inv && slot.getContainerSlot() == inventoryIndex) {
                return slot.index;
            }
        }
        return inventoryIndex; // fallback
    }


    public static boolean isRecipeBookOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AbstractRecipeBookScreen<?> screen) {
            RecipeBookComponent component = ((AbstractRecipeBookScreenAccessor) screen).getRecipeBookComponent();
            return component.isVisible();
        }
        return false;
    }

    public static int getHoveredBundleSlot() { return visible ? hoveredBundleSlot : -1; }
    public static boolean isEffectivelyVisible() { return visible && !isRecipeBookOpen(); }
    public static void toggleVisible() { visible = !visible; }

    /** 点击收纳袋切换按钮：如果配方书开着则关闭配方书并显示收纳袋，否则切换收纳袋。 */
    public static void togglePanel() {
        Minecraft client = Minecraft.getInstance();
        boolean wasRecipeBookOpen = false;
        if (client.gui.screen() instanceof AbstractRecipeBookScreen<?> screen) {
            RecipeBookComponent component = ((AbstractRecipeBookScreenAccessor) screen).getRecipeBookComponent();
            if (component.isVisible()) {
                component.toggleVisibility();
                visible = true;
                wasRecipeBookOpen = true;
            }
        }
        if (!wasRecipeBookOpen) {
            visible = !visible;
        }
    }

    // --- category button layout ---

    /** Shared button layout: returns Y position of category button i. */
    private static int catButtonY(int i, int panelY) {
        return panelY + PADDING - 3 + i * CAT_BAR_WIDTH;
    }

    public static BundleCategory getCategoryAt(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int panelY = topPos;
        int searchH = SEARCH_BAR_HEIGHT + 3;
        int gridH = PADDING * 2 + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING;
        int actualPanelHeight = Math.min(imageHeight, searchH + gridH) + 16;

        BundleCategory[] cats = BundleCategory.values();
        int catStartX = panelX + PADDING - 5;

        for (int i = 0; i < cats.length; i++) {
            int by = catButtonY(i, panelY);
            if (by + CAT_BAR_WIDTH > panelY + actualPanelHeight) {
                break;
            }

            boolean selected = cats[i] == currentCategory;
            int bx = catStartX;
            int bw = CAT_BAR_WIDTH;
            if (selected) {
                bx -= 5;
                bw += 5;
            }
            if (mouseX >= bx && mouseX < bx + bw
                    && mouseY >= by && mouseY < by + CAT_BAR_WIDTH) {
                return cats[i];
            }
        }
        return null;
    }

    // --- search ---

    public static boolean isInsideSearchBar(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        if (currentCategory != BundleCategory.ALL) return false; // Only ALL mode has interactive search
        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int sbx = panelX + PADDING + CAT_BAR_WIDTH + 2;
        int sby = topPos + 2;
        int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
        return mouseX >= sbx && mouseX <= sbx + sbw && mouseY >= sby && mouseY <= sby + SEARCH_BAR_HEIGHT;
    }

    public static void onCharTyped(char c) {
        if (!searchFocused || currentCategory != BundleCategory.ALL) return;
        if (c >= 32 && c != 127) { searchQuery += c; scrollOffset = 0; }
    }

    public static void onSearchKeyPress(int key) {
        if (!searchFocused || currentCategory != BundleCategory.ALL) return;
        if (key == 259) {
            if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); scrollOffset = 0; }
        } else if (key == 256) {
            searchQuery = ""; searchFocused = false; scrollOffset = 0;
        } else if (key == 257 || key == 335) {
            searchFocused = false;
        }
    }

    // --- render ---

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, int imageHeight, int mouseX, int mouseY) {
        if (!isEffectivelyVisible()) return;
        List<BundleSlotEntry> bundles = getBundles();
        if (bundles.isEmpty()) { scrollOffset = 0; return; }

        List<FlatItem> allItems = buildFlatItemList(bundles);
        if (allItems.isEmpty()) { scrollOffset = 0; return; }

        List<FlatItem> items = filterItems(allItems, searchQuery);
        if (items.isEmpty()) scrollOffset = 0;

        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int panelY = topPos;

        boolean isAllMode = currentCategory == BundleCategory.ALL;
        int searchH = SEARCH_BAR_HEIGHT + 3;

        int gridH = PADDING * 2 + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING;
        int panelHeight = Math.min(imageHeight, searchH + gridH) + 16;

        // Panel background (full panel with visible border)
        int bgColor = 0x80202020;
        int borderColor = 0xCC606060;
        // 完整背景
        graphics.fill(panelX, panelY, panelX + pw, panelY + panelHeight, bgColor);
        // 上边框
        graphics.fill(panelX, panelY, panelX + pw, panelY + 1, borderColor);
        // 下边框
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + pw, panelY + panelHeight, borderColor);
        // 左边框
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, borderColor);
        // 右边框
        graphics.fill(panelX + pw - 1, panelY, panelX + pw, panelY + panelHeight, borderColor);

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        int totalRows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // Category buttons always at panel top (no searchH offset)
        int catTop = panelY;
        // Grid starts below search bar
        int gridTop = panelY + searchH;
        int gridContentH = panelHeight - searchH;

        // Category buttons
        BundleCategory[] cats = BundleCategory.values();
        int catX = panelX + PADDING - 5;
        int catAreaH = panelHeight - PADDING * 2;

        for (int i = 0; i < cats.length; i++) {
            int by = catButtonY(i, catTop);
            if (by + CAT_BAR_WIDTH > catTop + panelHeight) break;

            boolean selected = cats[i] == currentCategory;
            int bx = catX;
            int bw = CAT_BAR_WIDTH;
            if (selected) { bx -= 5; bw += 5; }
            boolean hovered = mouseX >= bx && mouseX < bx + bw
                    && mouseY >= by && mouseY < by + CAT_BAR_WIDTH;
            int bg = selected ? 0x25101010 : (hovered ? 0x40FFFFFF : 0x30FFFFFF);
            graphics.fill(bx, by, bx + bw, by + CAT_BAR_WIDTH, bg);
            int iconOff = (CAT_BAR_WIDTH - 16) / 2;
            graphics.item(cats[i].getIcon(), bx + iconOff, by + iconOff);
        }

        // Scroll bar
        int sbX = panelX + PADDING + CAT_BAR_WIDTH + 2;
        int sbY = gridTop + PADDING;
        int sbH = gridContentH - PADDING * 2;

        graphics.fill(sbX, sbY, sbX + SCROLL_BAR_WIDTH, sbY + sbH, 0x30FFFFFF);
        if (maxScroll > 0) {
            int thumbH = Math.max(12, sbH * VISIBLE_ROWS / totalRows);
            int thumbY = sbY + (sbH - thumbH) * scrollOffset / maxScroll;
            graphics.fill(sbX, thumbY, sbX + SCROLL_BAR_WIDTH, thumbY + thumbH, 0x50FFFFFF);
        }

        // Item grid
        int gridX = sbX + SCROLL_BAR_WIDTH + 2;
        int gridY = gridTop + PADDING;
        int startRow = scrollOffset;
        int hoveredFlatIndex = -1;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int flatIndex = (startRow + row) * COLUMNS + col;
                if (flatIndex >= items.size()) break;
                int sx = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int sy = gridY + row * (SLOT_SIZE + SLOT_SPACING);

                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0x40FFFFFF);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x50FFFFFF);

                FlatItem fi = items.get(flatIndex);
                graphics.item(fi.stack(), sx + 1, sy + 1);
                graphics.itemDecorations(font, fi.stack(), sx + 1, sy + 1);

                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                    hoveredFlatIndex = flatIndex;
                }
            }
        }

        if (hoveredFlatIndex >= 0) {
            int hRow = hoveredFlatIndex / COLUMNS - startRow;
            int hCol = hoveredFlatIndex % COLUMNS;
            int hx = gridX + hCol * (SLOT_SIZE + SLOT_SPACING);
            int hy = gridY + hRow * (SLOT_SIZE + SLOT_SPACING);
            graphics.fill(hx, hy, hx + SLOT_SIZE, hy + SLOT_SIZE, 0x60FFFFFF);
            graphics.setTooltipForNextFrame(font, items.get(hoveredFlatIndex).stack(), mouseX, mouseY);
            hoveredBundleSlot = items.get(hoveredFlatIndex).sources().get(0).bundleSlot();
        } else {
            hoveredBundleSlot = -1;
        }

        // Search bar (always visible, only interactive in ALL mode)
        {
            int sbx = panelX + PADDING + CAT_BAR_WIDTH + 2;
            int sby = panelY + 2;
            int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
            boolean active = isAllMode && searchFocused;
            int bg = isAllMode ? (active ? 0x60000000 : 0x40FFFFFF) : 0x30FFFFFF;
            graphics.fill(sbx, sby, sbx + sbw, sby + SEARCH_BAR_HEIGHT, bg);
            if (active) graphics.fill(sbx + 1, sby + 1, sbx + sbw - 1, sby + SEARCH_BAR_HEIGHT - 1, 0x50FFFFFF);
            int textY = sby + (SEARCH_BAR_HEIGHT - font.lineHeight) / 2;
            if (isAllMode && searchQuery.isEmpty() && !searchFocused) {
                graphics.text(font, "Search...", sbx + 3, textY, 0xFF666666, false);
            } else if (isAllMode && !searchQuery.isEmpty()) {
                graphics.text(font, searchQuery, sbx + 3, textY, 0xFFFFFFFF, false);
                searchCursorTick = (searchCursorTick + 1) % 40;
                if (searchFocused && searchCursorTick < 20) {
                    int cursorX = sbx + 3 + font.width(searchQuery);
                    graphics.fill(cursorX, textY, cursorX + 1, textY + font.lineHeight, 0xFFFFFFFF);
                }
            }
        }

        // Category title (on top of search bar)
        if (currentCategory != BundleCategory.ALL) {
            String label = currentCategory.getDisplayName();
            int catTextX = panelX + PADDING + CAT_BAR_WIDTH + 2 + 3;
            int catTextY = panelY + 2 + (SEARCH_BAR_HEIGHT - font.lineHeight) / 2;
            graphics.text(font, label, catTextX, catTextY, 0xFFCCCCCC, false);
        }

       // Bundle count display (bottom-right of panel, same baseline as sort button)
        int[] stats = getBundleStats();
        String countText = stats[0] + "/" + stats[1];
        int textW = font.width(countText);
        int countX = gridX + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING - textW;
        int countY = panelY + panelHeight - 11;
        graphics.fill(countX - 2, countY, countX + textW + 2, countY + font.lineHeight, 0x30FFFFFF);
        graphics.text(font, countText, countX, countY, 0xFFAAAAAA, false);    }

    private static int[] getBundleStats() {
        List<BundleSlotEntry> all = getAllBundles();
        int totalItems = 0;
        Fraction totalWeight = Fraction.ZERO;
        for (BundleSlotEntry entry : all) {
            BundleContents c = entry.contents();
            if (c != null && !c.isEmpty()) {
                totalItems += c.itemCopyStream().mapToInt(ItemStack::getCount).sum();
                totalWeight = totalWeight.add(c.weight().result().orElse(Fraction.ZERO));
            }
        }
        // remaining weight → how many more "standard" items (weight 1/64) would fit
        Fraction maxWeight = Fraction.getFraction(all.size(), 1);
        Fraction remaining = maxWeight.subtract(totalWeight);
        int effectiveMax = totalItems + remaining.multiplyBy(Fraction.getFraction(64, 1)).intValue();
        return new int[] { totalItems, effectiveMax };
    }
}
