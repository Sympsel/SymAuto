package com.symauto.gui.screen;

import com.symauto.storage.CraftingPresetStore;
import com.symauto.gui.widget.CraftingPresetItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class CraftingPresetsManagerScreen extends Screen {
    private final Screen parent;

    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 42;
    private static final int ROW_H = 20;
    private static final int BOTTOM_MARGIN = 26;
    private static final int SCROLLBAR_W = 6;

    private int scrollRow = 0;
    private int visibleRows = 1;

    public CraftingPresetsManagerScreen(Screen parent) {
        this(parent, 0);
    }

    public CraftingPresetsManagerScreen(Screen parent, int scrollRow) {
        super(Component.literal("自动合成 · 预设管理器")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
        this.scrollRow = scrollRow;
    }

    @Override
    protected void init() {
        super.init();

        int contentBottom = this.height - BOTTOM_MARGIN;
        visibleRows = Math.max(1, (contentBottom - LIST_TOP) / (ROW_H + CraftingPresetItem.GAP));
        int total = CraftingPresetStore.size();
        int maxScroll = Math.max(0, total - visibleRows);
        scrollRow = Math.clamp(scrollRow, 0, maxScroll);

        int y = LIST_TOP;
        for (int i = scrollRow; i < total && i < scrollRow + visibleRows; i++) {
            buildRow(i, y);
            y += ROW_H + CraftingPresetItem.GAP;
        }

        int btnY = this.height - 20;
        addRenderableWidget(Button.builder(Component.literal("+ 新增手持物品"), b -> {
            CraftingPresetStore.addFromHeld(currentHeld());
            scrollRow = Math.max(0, CraftingPresetStore.size() - visibleRows);
            reopen();
        }).bounds(6, btnY, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            CraftingPresetStore.restoreDefaults();
            reopen();
        }).bounds(100, btnY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("清空"), b -> {
            CraftingPresetStore.clearAll();
            reopen();
        }).bounds(162, btnY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b ->
                this.minecraft.gui.setScreen(parent)).bounds(this.width - 56, btnY, 50, 20).build());
    }

    private void buildRow(int index, int y) {
        CraftingPresetItem.RowWidgets rw =
                new CraftingPresetItem(index).build(6, y, ROW_H, this.font, this::currentHeld, this::reopen);
        rw.buttons.forEach(this::addRenderableWidget);
        addRenderableWidget(rw.input);
    }

    private void reopen() {
        this.minecraft.gui.setScreen(new CraftingPresetsManagerScreen(this.parent, this.scrollRow));
    }

    private ItemStack currentHeld() {
        Minecraft client = this.minecraft;
        if (client.player == null) return ItemStack.EMPTY;
        return client.player.getInventory().getSelectedItem();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(4, LIST_TOP - 4, this.width - 4, this.height - BOTTOM_MARGIN + 2, 0x66000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, 0xFFFFFFFF, true);

        if (CraftingPresetStore.size() == 0) {
            String empty = "暂无预设，点击底部将手持物品新增到预设管理器";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, LIST_TOP + 4, 0xFFAAAAAA, false);
        }
        drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int total = CraftingPresetStore.size();
        int maxScroll = total - visibleRows;
        if (maxScroll <= 0) {
            return;
        }
        int top = LIST_TOP - 4;
        int bottom = this.height - BOTTOM_MARGIN + 2;
        int trackH = bottom - top;
        int trackLeft = this.width - 12;

        graphics.fill(trackLeft, top, trackLeft + SCROLLBAR_W, bottom, 0x40FFFFFF);
        int thumbH = Math.max(12, trackH * visibleRows / total);
        int thumbY = top + (int) ((long) (trackH - thumbH) * scrollRow / maxScroll);
        graphics.fill(trackLeft, thumbY, trackLeft + SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int total = CraftingPresetStore.size();
        int maxScroll = Math.max(0, total - visibleRows);
        if (scrollY != 0 && maxScroll > 0) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll);
            reopen();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}