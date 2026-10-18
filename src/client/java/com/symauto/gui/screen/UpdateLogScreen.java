package com.symauto.gui.screen;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String pattern = "(?s)^\\[([^]]+)]\\[([^]]+)]\\s*\\n(.*?)(?=\\n\\[[0-9]{4}-[0-9]{2}-[0-9]{2}]\\[[^]]+]]|$)";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

        List<String> messages = UpdateMessageManager.INSTANCE.getUpdateMessages();
        int contentBottom = this.height - LIST_BOTTOM_MARGIN;
        int maxWidth = this.width - 2 * SIDE_PAD;

        List<FormattedCharSequence> allLines = new ArrayList<>();

        // 匹配 [日期][版本] 的正则，用来做切分
        Pattern splitPattern = Pattern.compile("(?m)^\\[([^]]+)]\\[([^]]+)]\\s*");

        for (String rawText : messages) {
            String text = rawText.trim();
            Matcher matcher = splitPattern.matcher(text);

            List<Integer> matchStarts = new ArrayList<>();
            List<String[]> matchGroups = new ArrayList<>();

            while (matcher.find()) {
                matchStarts.add(matcher.start());
                matchGroups.add(new String[]{matcher.group(1), matcher.group(2)});
            }

            for (int i = 0; i < matchStarts.size(); i++) {
                int start = matchStarts.get(i);
                int end = (i + 1 < matchStarts.size()) ? matchStarts.get(i + 1) : text.length();

                String formatedTime = matchGroups.get(i)[0];
                String version = matchGroups.get(i)[1];

                int headerLength = ("[" + formatedTime + "][" + version + "]").length();
                String content = text.substring(start + headerLength, end).stripTrailing();

                if (content.isEmpty()) continue;

                // 格式化日期
                String time;
                try {
                    java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(formatedTime);
                    time = DATE_FMT.format(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()));
                } catch (Exception e) {
                    time = formatedTime; // 解析失败直接显示原字符串
                }

                Component header = Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(version).withStyle(ChatFormatting.GOLD));
                allLines.addAll(this.font.split(header, maxWidth));

                String[] contentLines = content.split("\n", -1);
                for (String lineStr : contentLines) {
                    Component line = Component.literal(lineStr).withStyle(ChatFormatting.WHITE);
                    allLines.addAll(this.font.split(line, maxWidth));
                }

                allLines.addAll(this.font.split(Component.literal(" "), maxWidth));
            }
        }

        // 滚动范围计算
        int visibleRows = Math.max(1, (contentBottom - LIST_TOP) / ROW_H);
        maxScroll = Math.max(0, allLines.size() - visibleRows);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        // 背景板
        graphics.fill(SIDE_PAD - 2, LIST_TOP - 4, this.width - SIDE_PAD + 2, contentBottom + 4, 0x60000000);

        // 空状态
        if (allLines.isEmpty()) {
            String empty = "暂无更新日志";
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

        // 提示文字
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