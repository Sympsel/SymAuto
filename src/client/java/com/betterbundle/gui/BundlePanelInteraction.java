package com.betterbundle.gui;

import com.betterbundle.util.BundleContentsHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BundlePanelInteraction {

    private static final int GLFW_MOD_SHIFT = 0x1;

    private BundlePanelInteraction() {}

    private static int gridX(int leftPos) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        return panelX + BundlePanelRenderer.PADDING
                + BundlePanelRenderer.CAT_BAR_WIDTH + 2
                + BundlePanelRenderer.SCROLL_BAR_WIDTH + 2;
    }

    private static int gridY(int topPos) {
        return topPos + BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3 + BundlePanelRenderer.PADDING;
    }

    private static BundlePanelRenderer.FlatItem getClickedItem(double mouseX, double mouseY,
                                                                int leftPos, int topPos) {
        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getBundles();
        if (bundles.isEmpty()) return null;

        List<BundlePanelRenderer.FlatItem> allItems = BundlePanelRenderer.buildFlatItemList(bundles);
        if (allItems.isEmpty()) return null;

        // Use filtered items to match rendered panel
        List<BundlePanelRenderer.FlatItem> items = BundlePanelRenderer.filterItems(allItems, BundlePanelRenderer.searchQuery);
        if (items.isEmpty()) return null;

        int gx = gridX(leftPos);
        int gy = gridY(topPos);

        int relX = (int) mouseX - gx;
        int relY = (int) mouseY - gy;

        int col = relX / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);
        int row = relY / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);

        if (col < 0 || col >= BundlePanelRenderer.COLUMNS) return null;
        if (row < 0 || row >= BundlePanelRenderer.VISIBLE_ROWS) return null;

        int flatIndex = (BundlePanelRenderer.getScrollOffset() + row) * BundlePanelRenderer.COLUMNS + col;
        if (flatIndex >= items.size()) return null;
        return items.get(flatIndex);
    }

    public static boolean handlePanelClick(double mouseX, double mouseY, int button, int modifiers,
                                            int leftPos, int topPos,
                                            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        BundlePanelRenderer.FlatItem clicked = getClickedItem(mouseX, mouseY, leftPos, topPos);
        if (clicked == null) return false;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        List<BundlePanelRenderer.SourceEntry> sources = clicked.sources();
        if (sources.isEmpty()) return false;

        int containerId = player.containerMenu.containerId;
        int maxStack = Math.max(1, clicked.stack().getMaxStackSize());

        extractFromSources(connection, containerId, player, sources, maxStack);
        return true;
    }

    public static boolean handleSpaceClick(Slot hoveredSlot) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getWindow() == null) return false;

        long window = client.getWindow().handle();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) != GLFW.GLFW_PRESS) return false;

        Player player = client.player;
        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty() || BundleContentsHelper.isNonEmptyBundle(stack)) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        List<Integer> targets = buildInsertTargets(bundles, stack, hoveredSlot.index);
        if (targets.isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        int containerId = player.containerMenu.containerId;
        int itemSlot = hoveredSlot.index;

        // 拿起整叠 → 依次放入多个袋子（每个尽量填充，余量留在光标）→ 余量放回原槽。
        connection.send(makeClickPacket(containerId, itemSlot, (byte) 0));
        for (int target : targets) {
            connection.send(makeClickPacket(containerId, target, (byte) 0));
        }
        connection.send(makeClickPacket(containerId, itemSlot, (byte) 0));

        return true;
    }

    /**
     * 计算把 stack 放入哪些袋子（类似 XFS 聚堆预分配）：
     * <ol>
     *   <li>优先已有同种物品的袋子（同种越多越靠前）；</li>
     *   <li>没有同种时，选当前能容纳最多的袋子（“找最大的一个”），尽量把整叠聚到一处；</li>
     *   <li>依次累计容量，凑够整叠就停，装不下的部分才继续找下一个袋子。</li>
     * </ol>
     * 返回目标袋子槽位（按放入顺序）。
     */
    private static List<Integer> buildInsertTargets(
            List<BundlePanelRenderer.BundleSlotEntry> bundles, ItemStack stack, int excludeSlot) {
        List<BundlePanelRenderer.BundleSlotEntry> cands = new ArrayList<>();
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (entry.bundleSlot() == excludeSlot) continue;
            if (BundleContentsHelper.maxAcceptable(entry.bundleStack(), stack) > 0) {
                cands.add(entry);
            }
        }
        cands.sort(Comparator
                .comparingInt((BundlePanelRenderer.BundleSlotEntry e) ->
                        BundleContentsHelper.sameItemCount(e.bundleStack(), stack) > 0 ? 0 : 1)
                .thenComparingInt(e -> -BundleContentsHelper.sameItemCount(e.bundleStack(), stack))
                .thenComparingInt(e -> -BundleContentsHelper.maxAcceptable(e.bundleStack(), stack)));

        List<Integer> targets = new ArrayList<>();
        int remaining = stack.getCount();
        for (BundlePanelRenderer.BundleSlotEntry entry : cands) {
            int cap = BundleContentsHelper.maxAcceptable(entry.bundleStack(), stack);
            if (cap <= 0) continue;
            targets.add(entry.bundleSlot());
            remaining -= Math.min(cap, remaining);
            if (remaining <= 0) break;
        }
        return targets;
    }

    private static ServerboundContainerClickPacket makeClickPacket(int containerId, int slot, byte button) {
        return new ServerboundContainerClickPacket(
                containerId, -1, (short) slot, button,
                ContainerInput.PICKUP, new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY);
    }

    private static long bulkInsertStart = 0;
    private static final long BULK_INSERT_DELAY = 50; // 0.05s

    /** Start the bulk-insert timer (called on space+left-click inside panel with empty cursor). */
    public static void startBulkInsert() {
        bulkInsertStart = System.currentTimeMillis();
    }

    /** Whether the bulk-insert state is active (left button held > 0.05s). */
    public static boolean isBulkInsertActive() {
        return bulkInsertStart > 0 && (System.currentTimeMillis() - bulkInsertStart) >= BULK_INSERT_DELAY;
    }

    /** Exit bulk-insert state. */
    public static void stopBulkInsert() {
        bulkInsertStart = 0;
    }

    /** Put cursor item into available bundles, distributing across several if needed.
     *  单袋放不下时自动拆到多个袋子（优先聚堆）。 */
    public static boolean handlePanelInsert(int button) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;

        ItemStack cursor = player.containerMenu.getCarried();
        if (cursor.isEmpty()) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        List<Integer> targets = buildInsertTargets(bundles, cursor, -1);
        if (targets.isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;
        int containerId = player.containerMenu.containerId;

        // 光标已有整叠，直接依次放入多个袋子；每个尽量填充，余量留在光标上。
        for (int target : targets) {
            connection.send(makeClickPacket(containerId, target, (byte) 0));
        }
        return true;
    }

    /**
     * 当面板可见时，拦截对背包中收纳袋格子的点击：
     * 不拿起收纳袋，而是从面板分组列表中提取该收纳袋里的物品。
     */
    public static boolean handleBundleSlotClick(Slot hoveredSlot,
                                                 net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;
        if (!BundlePanelRenderer.visible) return false;
        if (!BundleContentsHelper.isNonEmptyBundle(hoveredSlot.getItem())) return false;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;
        if (!player.containerMenu.getCarried().isEmpty()) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        int slotIndex = hoveredSlot.index;

        // 在面板分组列表中查找包含该收纳袋的分组物品
        List<BundlePanelRenderer.FlatItem> allItems = BundlePanelRenderer.buildFlatItemList(
                BundlePanelRenderer.getBundles());
        List<BundlePanelRenderer.FlatItem> items = BundlePanelRenderer.filterItems(allItems, BundlePanelRenderer.searchQuery);

        BundlePanelRenderer.FlatItem target = null;
        for (BundlePanelRenderer.FlatItem fi : items) {
            for (BundlePanelRenderer.SourceEntry src : fi.sources()) {
                if (src.bundleSlot() == slotIndex) {
                    target = fi;
                    break;
                }
            }
            if (target != null) break;
        }
        if (target == null) return false;

        int containerId = player.containerMenu.containerId;
        int maxStack = Math.max(1, target.stack().getMaxStackSize());

        extractFromSources(connection, containerId, player, target.sources(), maxStack);
        return true;
    }

    /**
     * 核心提取逻辑（之前验证有效的模式）：
     * 逐个条目提取到光标，中间条目放入空槽，最后一个留在光标。
     */
    private static void extractFromSources(ClientPacketListener connection, int containerId,
                                            Player player,
                                            List<BundlePanelRenderer.SourceEntry> sources, int maxStack) {
        List<Integer> emptySlots = findAllEmptyPlayerSlots(player);
        int emptyIdx = 0;
        int accumulated = 0;

        for (int i = 0; i < sources.size(); i++) {
            if (accumulated >= maxStack) break;
            BundlePanelRenderer.SourceEntry src = sources.get(i);
            boolean isLast = (i == sources.size() - 1) || (accumulated + src.itemCount() >= maxStack);

            connection.send(new ServerboundSelectBundleItemPacket(src.bundleSlot(), -1));
            connection.send(new ServerboundSelectBundleItemPacket(src.bundleSlot(), src.itemIndex()));
            connection.send(makeClickPacket(containerId, src.bundleSlot(), (byte) 1));

            if (!isLast && emptyIdx < emptySlots.size()) {
                // 中间条目：放入空槽，清空光标以便下次提取
                connection.send(makeClickPacket(containerId, emptySlots.get(emptyIdx++), (byte) 0));
            }
            // 最后一条目：留在光标上

            accumulated += Math.min(src.itemCount(), maxStack - accumulated);
        }
    }

    /** 找出所有空的玩家背包槽位，先主背包(9-35)后快捷栏(0-8) */
    private static List<Integer> findAllEmptyPlayerSlots(Player player) {
        List<Integer> result = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int min = (pass == 0) ? 9 : 0;
            int max = (pass == 0) ? 36 : 9;
            for (Slot slot : player.containerMenu.slots) {
                if (slot.container == player.getInventory() && !slot.hasItem()) {
                    int idx = slot.getContainerSlot();
                    if (idx >= min && idx < max) result.add(slot.index);
                }
            }
        }
        return result;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double scrollDelta,
                                        int leftPos, int topPos, int imageHeight) {
        if (!BundlePanelRenderer.visible) return false;
        if (!isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) return false;
        BundlePanelRenderer.scrollBy(scrollDelta > 0 ? -1 : 1);
        return true;
    }

    public static boolean isInsidePanel(double mouseX, double mouseY,
                                         int leftPos, int topPos, int imageHeight) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        if (mouseX < panelX || mouseX > panelX + pw) return false;
        int pTop = topPos;
        int searchH = BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3;
        int gridH = BundlePanelRenderer.PADDING * 2
                + BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        int panelH = Math.min(imageHeight, searchH + gridH) + 16;
        if (mouseY < pTop || mouseY > pTop + panelH) return false;
        return true;
    }
}
