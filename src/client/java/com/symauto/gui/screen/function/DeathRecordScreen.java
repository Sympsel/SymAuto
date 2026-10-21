package com.symauto.gui.screen.function;

import com.symauto.storage.DeathInfoStore;
import com.symauto.storage.DeathInfoStore.DeathRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DeathRecordScreen extends Screen {
    private final Screen parent;

    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 30;
    private static final int LIST_BOTTOM_MARGIN = 24;
    private static final int ROW_H = 12;
    private static final int SIDE_PAD = 8;

    private static final int BTN_W = 64;
    private static final int BTN_H = 16;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public DeathRecordScreen(Screen parent) {
        super(Component.literal("SymAuto 死亡记录")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int clearX = this.width - (BTN_W * 2 + 10) - 6;
        this.addRenderableWidget(
                Button.builder(Component.literal("清空记录"), b -> {
                    DeathInfoStore.clear();
                    this.scrollOffset = 0;
                }).bounds(clearX, 6, BTN_W, BTN_H).build());
        int backX = this.width - BTN_W - 6;
        this.addRenderableWidget(
                Button.builder(Component.literal("返回"), b ->
                        this.minecraft.gui.setScreen(parent))
                        .bounds(backX, 6, BTN_W, BTN_H).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, TITLE_COLOR, true);

        List<DeathRecord> records = DeathInfoStore.getRecords();
        int total = records.size();
        int contentBottom = this.height - LIST_BOTTOM_MARGIN;
        int maxWidth = this.width - 2 * SIDE_PAD;

        List<FormattedCharSequence> allLines = new ArrayList<>();
        for (int i = total - 1; i >= 0; i--) {
            allLines.addAll(this.font.split(formatRecord(total - i, records.get(i)), maxWidth));
        }

        int visibleRows = Math.max(1, (contentBottom - LIST_TOP) / ROW_H);
        maxScroll = Math.max(0, allLines.size() - visibleRows);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        graphics.fill(SIDE_PAD - 2, LIST_TOP - 4, this.width - SIDE_PAD + 2, contentBottom + 4, 0x60000000);

        if (records.isEmpty()) {
            String empty = "暂无死亡记录";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, LIST_TOP, 0xFFAAAAAA, false);
        }

        int y = LIST_TOP;
        for (int i = scrollOffset; i < allLines.size(); i++) {
            if (y + ROW_H > contentBottom) {
                break;
            }
            graphics.text(this.font, allLines.get(i), SIDE_PAD, y, 0xFFFFFFFF, false);
            y += ROW_H;
        }

        String hint = "共 " + total + " 条  滚轮滚动";
        graphics.text(this.font, hint, this.width - this.font.width(hint) - SIDE_PAD,
                contentBottom + 8, 0xFFAAAAAA, false);
    }

    private Component formatRecord(int index, DeathRecord r) {
        return Component.literal("#" + index + " ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(r.formatWallTime() + "  ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("第" + r.formatGameDay() + "天  ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(r.dimension + "  ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("(" + r.formatCoordinate() + ")  ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("死因: " + r.formatCause()).withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scrollOffset = Math.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
        }
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}