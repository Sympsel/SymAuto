package com.symauto.gui.screen;

import com.symauto.function.auto.AutoCraftingFunction;
import com.symauto.storage.CraftingPresetStore;
import com.symauto.gui.widget.CraftingPresetItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CraftingPresetsManagerScreen extends Screen {
    private final Screen parent;

    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 42;
    private static final int ROW_H = 20;
    private static final int LOG_LINE_H = 10;
    private static final int BOTTOM_MARGIN = 26;
    private static final int SCROLLBAR_W = 6;

    private int scrollRow = 0;
    private int visibleRows = 1;
    private final boolean showLogs;
    private boolean draggingScrollbar = false;
    private int scrollbarDragOffset = 0;

    public CraftingPresetsManagerScreen(Screen parent) {
        this(parent, 0, false);
    }

    private CraftingPresetsManagerScreen(Screen parent, int scrollRow, boolean showLogs) {
        super(Component.literal(showLogs ? "自动合成 · 执行日志" : "自动合成 · 预设管理器")
                .withStyle(ChatFormatting.BOLD, showLogs ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        this.parent = parent;
        this.scrollRow = scrollRow;
        this.showLogs = showLogs;
    }

    @Override
    protected void init() {
        super.init();

        int contentBottom = this.height - BOTTOM_MARGIN;
        int rowHeight = showLogs ? LOG_LINE_H : ROW_H + CraftingPresetItem.GAP;
        visibleRows = Math.max(1, (contentBottom - LIST_TOP) / rowHeight);
        int total = entryCount();
        int maxScroll = Math.max(0, total - visibleRows);
        scrollRow = Math.clamp(scrollRow, 0, maxScroll);

        if (!showLogs) {
            int y = LIST_TOP;
            for (int i = scrollRow; i < total && i < scrollRow + visibleRows; i++) {
                buildRow(i, y);
                y += ROW_H + CraftingPresetItem.GAP;
            }
        }

        int btnY = this.height - 20;
        if (!showLogs) {
            addRenderableWidget(Button.builder(Component.literal("+ 新增手持物品"), b -> {
                CraftingPresetStore.addFromHeld(currentHeld());
                scrollRow = Math.max(0, CraftingPresetStore.size() - visibleRows);
                reopen();
            }).bounds(6, btnY, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
                CraftingPresetStore.restoreDefaults();
                reopen();
            }).bounds(100, btnY, 60, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal(showLogs ? "清空日志" : "清空"), b -> {
            if (showLogs) {
                AutoCraftingFunction.clearExecutionLogs();
            } else {
                CraftingPresetStore.clearAll();
            }
            scrollRow = 0;
            reopen();
        }).bounds(showLogs ? 6 : 162, btnY, showLogs ? 60 : 40, 20).build());

        addRenderableWidget(Button.builder(Component.literal(showLogs ? "返回预设" : "执行日志"), b -> {
                    scrollRow = showLogs ? 0 : Integer.MAX_VALUE;
                    reopen(!showLogs);
                }).bounds(showLogs ? 70 : 205, btnY, 60, 20)
                .tooltip(Tooltip.create(Component.literal(showLogs
                        ? "返回预设管理器"
                        : "查看最近 200 条自动合成执行记录；连续重复记录会自动合并")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b ->
                this.minecraft.gui.setScreen(parent)).bounds(this.width - 56, btnY, 50, 20).build());
    }

    private int entryCount() {
        return showLogs ? AutoCraftingFunction.getExecutionLogs().size() : CraftingPresetStore.size();
    }

    private void buildRow(int index, int y) {
        CraftingPresetItem.RowWidgets rw =
                new CraftingPresetItem(index).build(6, y, ROW_H, this.font, this::currentHeld, this::reopen);
        rw.buttons.forEach(this::addRenderableWidget);
        addRenderableWidget(rw.input);
    }

    private void reopen() {
        reopen(showLogs);
    }

    private void reopen(boolean logs) {
        this.minecraft.gui.setScreen(new CraftingPresetsManagerScreen(this.parent, this.scrollRow, logs));
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

        if (showLogs) {
            renderLogs(graphics);
        } else if (CraftingPresetStore.size() == 0) {
            String empty = "暂无预设，点击底部将手持物品新增到预设管理器";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, LIST_TOP + 4, 0xFFAAAAAA, false);
        }
        drawScrollbar(graphics);
    }

    private void renderLogs(GuiGraphicsExtractor graphics) {
        List<String> logs = AutoCraftingFunction.getExecutionLogs();
        if (logs.isEmpty()) {
            String empty = "暂无执行日志；打开工作台并启动自动合成后会在这里显示";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2,
                    LIST_TOP + 4, 0xFFAAAAAA, false);
            return;
        }

        int y = LIST_TOP;
        int end = Math.min(logs.size(), scrollRow + visibleRows);
        for (int i = scrollRow; i < end; i++) {
            String line = logs.get(i);
            String fitted = this.font.plainSubstrByWidth(line, this.width - 28);
            graphics.text(this.font, fitted, 10, y, logColor(line), false);
            y += LOG_LINE_H;
        }
    }

    private int logColor(String line) {
        if (line.contains("失败") || line.contains("不足") || line.contains("超时") || line.contains("无法")) {
            return 0xFFFF6666;
        }
        if (line.contains("完成") || line.contains("已到位")) {
            return 0xFF66DD66;
        }
        if (line.contains("等待") || line.contains("准备")) {
            return 0xFFFFD166;
        }
        return 0xFFE0E0E0;
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int total = entryCount();
        int maxScroll = total - visibleRows;
        if (maxScroll <= 0) {
            return;
        }
        int top = scrollbarTop();
        int bottom = scrollbarBottom();
        int trackLeft = scrollbarLeft();

        graphics.fill(trackLeft, top, trackLeft + SCROLLBAR_W, bottom, 0x40FFFFFF);
        int thumbH = scrollbarThumbHeight();
        int thumbY = scrollbarThumbTop();
        int thumbColor = draggingScrollbar ? 0xFFFFFFFF : 0xC0FFFFFF;
        graphics.fill(trackLeft, thumbY, trackLeft + SCROLLBAR_W, thumbY + thumbH, thumbColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, entryCount() - visibleRows);
        if (scrollY != 0 && maxScroll > 0) {
            int step = showLogs ? 6 : 2;
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY) * step, 0, maxScroll);
            reopen();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isOverScrollbar(event.x(), event.y())) {
            int thumbTop = scrollbarThumbTop();
            int thumbHeight = scrollbarThumbHeight();
            if (event.y() >= thumbTop && event.y() <= thumbTop + thumbHeight) {
                scrollbarDragOffset = (int) event.y() - thumbTop;
            } else {
                scrollbarDragOffset = thumbHeight / 2;
                updateScrollFromMouse(event.y());
            }
            draggingScrollbar = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            // 预设页需要重建行控件；日志页无行控件，已经随 scrollRow 实时刷新
            if (!showLogs) {
                reopen();
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (entryCount() - visibleRows <= 0) {
            return false;
        }
        int left = scrollbarLeft();
        return mouseX >= left - 3 && mouseX <= left + SCROLLBAR_W + 3
                && mouseY >= scrollbarTop() && mouseY <= scrollbarBottom();
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = Math.max(0, entryCount() - visibleRows);
        int usable = scrollbarBottom() - scrollbarTop() - scrollbarThumbHeight();
        if (usable <= 0) {
            scrollRow = 0;
            return;
        }
        double ratio = (mouseY - scrollbarTop() - scrollbarDragOffset) / usable;
        scrollRow = Math.clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private int scrollbarTop() {
        return LIST_TOP - 4;
    }

    private int scrollbarBottom() {
        return this.height - BOTTOM_MARGIN + 2;
    }

    private int scrollbarLeft() {
        return this.width - 12;
    }

    private int scrollbarThumbHeight() {
        int total = entryCount();
        int trackHeight = scrollbarBottom() - scrollbarTop();
        return Math.max(12, trackHeight * visibleRows / Math.max(1, total));
    }

    private int scrollbarThumbTop() {
        int maxScroll = Math.max(0, entryCount() - visibleRows);
        int trackHeight = scrollbarBottom() - scrollbarTop();
        int thumbHeight = scrollbarThumbHeight();
        if (maxScroll <= 0) {
            return scrollbarTop();
        }
        return scrollbarTop() + (int) ((long) (trackHeight - thumbHeight) * scrollRow / maxScroll);
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