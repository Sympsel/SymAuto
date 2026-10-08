package com.symauto.gui;

import com.betterbundle.util.BundleContentsHelper;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.List;

public class BundleQuickViewScreen extends Screen {
    private static final int SLOT = 18;
    private static final int PAD = 4;
    private static final int MAX_COLS = 9;

    /** 手持袋在 inventoryMenu 中的槽位（= 36 + 主手序号），打开时锁定。 */
    private final int bundleSlot;

    private final List<ItemStack> items = new ArrayList<>();
    // 每帧按当前条目数重算的几何
    private int cols = 1, rows = 0, panelX = 0, panelY = 0, panelW = 0, panelH = 0, gridY = 0;

    public BundleQuickViewScreen(int bundleSlot) {
        super(Component.literal("收纳袋").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.bundleSlot = bundleSlot;
    }

    /** 依据当前 items 数量自适应布局。 */
    private void relayout() {
        int n = items.size();
        cols = n == 0 ? 1 : Math.min(MAX_COLS, n);
        rows = (n + cols - 1) / cols;
        panelW = cols * SLOT + PAD * 2;
        panelH = PAD * 2 + this.font.lineHeight + 2 + rows * SLOT;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        gridY = panelY + PAD + this.font.lineHeight + 2;
    }

    private void refreshContents() {
        items.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        BundleContents contents = BundleContentsHelper.getContents(client.player.getMainHandItem());
        if (contents != null && !contents.isEmpty()) {
            items.addAll(contents.itemCopyStream().toList());
        }
    }

    private int indexAt(int mx, int my) {
        for (int i = 0; i < items.size(); i++) {
            int r = i / cols, c = i % cols;
            int sx = panelX + PAD + c * SLOT;
            int sy = gridY + r * SLOT;
            if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        refreshContents();
        relayout();

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0101010);
        String title = "共 " + items.size() + " 项";
        graphics.text(this.font, title, panelX + PAD, panelY + PAD - 1, 0xFFAAAAAA, false);

        if (items.isEmpty()) {
            graphics.text(this.font, "（袋子是空的）", panelX + PAD, gridY, 0xFF888888, false);
        }

        int hovered = -1;
        for (int i = 0; i < items.size(); i++) {
            int r = i / cols, c = i % cols;
            int sx = panelX + PAD + c * SLOT;
            int sy = gridY + r * SLOT;
            graphics.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0x40FFFFFF);
            ItemStack stack = items.get(i);
            graphics.item(stack, sx + 1, sy + 1);
            graphics.itemDecorations(this.font, stack, sx + 1, sy + 1);
            if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
                hovered = i;
            }
        }
        if (hovered >= 0) {
            graphics.setTooltipForNextFrame(this.font, items.get(hovered), mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        int idx = indexAt((int) event.x(), (int) event.y());
        if (idx >= 0) {
            extractToInventory(idx);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * 取出袋内第 itemIndex 个条目并放入玩家背包第一个空槽：
     * select(-1)→select(index)→点击袋子槽(整条取到光标)→点击空槽(放下)。
     */
    private void extractToInventory(int itemIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;
        ClientPacketListener conn = client.getConnection();
        if (conn == null) return;

        AbstractContainerMenu menu = client.player.inventoryMenu;
        int containerId = menu.containerId; // 0

        conn.send(new ServerboundSelectBundleItemPacket(bundleSlot, -1));
        conn.send(new ServerboundSelectBundleItemPacket(bundleSlot, itemIndex));
        click(client, containerId, bundleSlot, 0, ContainerInput.PICKUP);

        int empty = findFirstEmptyInventorySlot(client.player.getInventory(), menu);
        if (empty >= 0) {
            click(client, containerId, empty, 0, ContainerInput.PICKUP);
        }
        // 若没有空槽，物品会留在光标；关闭界面时下面 onClose 兜底处理
    }

    private void click(Minecraft client, int containerId, int slot, int button, ContainerInput type) {
        client.gameMode.handleContainerInput(containerId, slot, button, type, client.player);
    }

    /** 主背包(9-35) 优先，其次快捷栏(0-8) 找空槽，返回 inventoryMenu 槽号。 */
    private int findFirstEmptyInventorySlot(Inventory inv, AbstractContainerMenu menu) {
        for (int pass = 0; pass < 2; pass++) {
            int min = pass == 0 ? 9 : 0;
            int max = pass == 0 ? 36 : 9;
            for (Slot slot : menu.slots) {
                if (slot.container == inv && !slot.hasItem()) {
                    int idx = slot.getContainerSlot();
                    if (idx >= min && idx < max) return slot.index;
                }
            }
        }
        return -1;
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.gameMode != null) {
            ItemStack carried = client.player.inventoryMenu.getCarried();
            if (!carried.isEmpty()) {
                int empty = findFirstEmptyInventorySlot(client.player.getInventory(), client.player.inventoryMenu);
                if (empty >= 0) {
                    client.gameMode.handleContainerInput(0, empty, 0, ContainerInput.PICKUP, client.player);
                } else {
                    // 背包满：整组丢出，避免关闭后卡在光标
                    client.gameMode.handleContainerInput(0, -999, 0, ContainerInput.PICKUP, client.player);
                }
            }
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
