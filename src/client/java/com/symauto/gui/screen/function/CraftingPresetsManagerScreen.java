package com.symauto.gui.screen.function;

import com.symauto.function.auto.AutoCraftingFunction;
import com.symauto.gui.base.SymBaseScreen;
import com.symauto.gui.widget.ScrollableTextList;
import com.symauto.gui.widget.SimpleScrollbar;
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

public class CraftingPresetsManagerScreen extends SymBaseScreen {
    private final Screen parent;

    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 42;
    private static final int ROW_H = 20;
    private static final int LOG_LINE_H = 10;
    private static final int BOTTOM_MARGIN = 26;

    private int scrollRow = 0;
    private int visibleRows = 1;
    private final boolean showLogs;
    private ScrollableTextList logList;

    private final SimpleScrollbar scrollbar = new SimpleScrollbar(new SimpleScrollbar.ScrollTarget() {
        @Override
        public int totalRows() {
            return entryCount();
        }

        @Override
        public int visibleRows() {
            return visibleRows;
        }

        @Override
        public int scrollRow() {
            return scrollRow;
        }

        @Override
        public void scrollRow(int v) {
            scrollRow = v;
            if (showLogs && logList != null) {
                logList.setScrollOffset(v);
            }
        }
    });

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
        scrollbar.setBounds(this.width - 12, LIST_TOP - 4, this.height - BOTTOM_MARGIN + 2);
        if (showLogs) {
            logList = new ScrollableTextList(() -> AutoCraftingFunction.getExecutionLogs().stream()
                    .map(s -> new ScrollableTextList.Line(
                            Component.literal(this.font.plainSubstrByWidth(s, this.width - 38)),
                            logColor(s)))
                    .toList())
                    .setBounds(10, LIST_TOP, this.width - 28, this.height - BOTTOM_MARGIN)
                    .setRowHeight(LOG_LINE_H)
                    .setEmptyText("暂无执行日志；打开工作台并启动自动合成后会在这里显示");
            logList.setScrollOffset(scrollRow);
        }
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
        graphics.text(this.font, this.title, titleX, TITLE_Y, style.text, true);

        if (showLogs) {
            logList.render(graphics, this.font);
        } else if (CraftingPresetStore.size() == 0) {
            String empty = "暂无预设，点击底部将手持物品新增到预设管理器";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, LIST_TOP + 4, style.muted, false);
        }
        scrollbar.render(graphics);
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (scrollbar.mouseClicked(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbar.mouseDragged(event.y())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollbar.mouseReleased()) {
            if (!showLogs) {
                reopen();
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, entryCount() - visibleRows);
        if (scrollY != 0 && maxScroll > 0) {
            if (showLogs) {
                if (logList.mouseScrolled(scrollY)) {
                    scrollRow = logList.scrollOffset();
                    return true;
                }
                return false;
            }
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY) * 2L, 0, maxScroll);
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