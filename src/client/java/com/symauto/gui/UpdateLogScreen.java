package com.symauto.gui;

import com.symauto.update.UpdateMessage;
import com.symauto.update.UpdateMessageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UpdateLogScreen extends Screen {
    private final Screen parent;

    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 30;
    private static final int LIST_BOTTOM_MARGIN = 24;
    private static final int ROW_H = 12;
    private static final int SIDE_PAD = 8;

    private static final int BACK_W = 72;
    private static final int BACK_H = 16;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public UpdateLogScreen(Screen parent) {
        super(Component.literal("SymAuto 更新日志")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // 顶部右上角：返回主菜单
        int backX = this.width - BACK_W - 6;
        this.addRenderableWidget(
                Button.builder(Component.literal("返回主菜单"), b ->
                        this.minecraft.gui.setScreen(parent))
                        .bounds(backX, 6, BACK_W, BACK_H)
                        .build()
        );
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // 标题居中
        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, TITLE_COLOR, true);

        List<UpdateMessage> messages = UpdateMessageManager.INSTANCE.getUpdateMessages();
        int contentBottom = this.height - LIST_BOTTOM_MARGIN;
        int maxWidth = this.width - 2 * SIDE_PAD;

        // 按可用宽度自动换行，并展平为“视觉行”（保留每段颜色）
        List<FormattedCharSequence> allLines = new ArrayList<>();
        for (UpdateMessage m : messages) {
            String time = DATE_FMT.format(
                    Instant.ofEpochMilli(m.getCreateTime()).atZone(ZoneId.systemDefault()));
            Component line = Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(m.getVersion() + " ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(m.getMessage()).withStyle(ChatFormatting.WHITE));
            allLines.addAll(this.font.split(line, maxWidth));
        }

        // 滚动范围以“视觉行”为单位计算
        int visibleRows = Math.max(1, (contentBottom - LIST_TOP) / ROW_H);
        maxScroll = Math.max(0, allLines.size() - visibleRows);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        // 列表背景
        graphics.fill(SIDE_PAD - 2, LIST_TOP - 4, this.width - SIDE_PAD + 2, contentBottom + 4, 0x60000000);

        if (messages.isEmpty()) {
            String empty = "暂无更新日志";
            graphics.text(this.font, empty, this.width - this.font.width(empty) / 2, LIST_TOP, 0xFFAAAAAA, false);
        }

        int y = LIST_TOP;
        for (int i = scrollOffset; i < allLines.size(); i++) {
            if (y + ROW_H > contentBottom) {
                break;
            }
            graphics.text(this.font, allLines.get(i), SIDE_PAD, y, 0xFFFFFFFF, false);
            y += ROW_H;
        }

        // 右下角操作提示
        String hint = "滚轮滚动";
        graphics.text(this.font, hint, this.width - this.font.width(hint) - SIDE_PAD,
                contentBottom + 8, 0xFFAAAAAA, false);
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