package com.symauto.gui.screen;

import com.symauto.gui.base.SymBaseScreen;
import com.symauto.gui.widget.ScrollableTextList;
import com.symauto.update.UpdateMessageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateLogScreen extends SymBaseScreen {
    private final Screen parent;
    private ScrollableTextList list;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern SPLIT = Pattern.compile("(?m)^\\[([^]]+)]\\[([^]]+)]\\s*");
    private List<String> cachedRaw;
    private List<ScrollableTextList.Line> cachedLines;

    public UpdateLogScreen(Screen parent) {
        super(Component.literal("SymAuto 更新日志").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = new ScrollableTextList(this::parseLines)
                .setBounds(8, 30, this.width - 8, this.height - 24)
                .setEmptyText("暂无更新日志")
                .setHintText("滚轮滚动")
                .setScrollbarEnabled(true);
        addRenderableWidget(backButton(parent, "返回主菜单", 72));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return list.mouseClicked(event.x(), event.y())
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return list.mouseDragged(event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return list.mouseReleased() || super.mouseReleased(event);
    }

    private List<ScrollableTextList.Line> parseLines() {
        List<String> raw = UpdateMessageManager.INSTANCE.getUpdateMessages();
        if (raw == cachedRaw && cachedLines != null) {
            return cachedLines;
        }
        cachedLines = doParse();
        cachedRaw = raw;
        return cachedLines;
    }

    private List<ScrollableTextList.Line> doParse() {
        List<ScrollableTextList.Line> out = new ArrayList<>();
        for (String rawText : UpdateMessageManager.INSTANCE.getUpdateMessages()) {
            String text = rawText.trim();
            Matcher matcher = SPLIT.matcher(text);
            List<Integer> starts = new ArrayList<>();
            List<String[]> groups = new ArrayList<>();
            while (matcher.find()) {
                starts.add(matcher.start());
                groups.add(new String[]{matcher.group(1), matcher.group(2)});
            }
            for (int i = 0; i < starts.size(); i++) {
                int end = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();
                int headerLength = ("[" + groups.get(i)[0] + "][" + groups.get(i)[1] + "]").length();
                String content = text.substring(starts.get(i) + headerLength, end).stripTrailing();
                if (content.isEmpty()) continue;

                String time;
                try {
                    java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(groups.get(i)[0]);
                    time = DATE_FMT.format(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()));
                } catch (Exception e) {
                    time = groups.get(i)[0];
                }

                Component header = Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(groups.get(i)[1]).withStyle(ChatFormatting.GOLD));
                out.add(new ScrollableTextList.Line(header, 0xFFFFFFFF));
                for (String s : content.split("\n", -1)) {
                    out.add(new ScrollableTextList.Line(Component.literal(s), 0xFFFFFFFF));
                }
                out.add(new ScrollableTextList.Line(Component.literal(" "), 0xFFFFFFFF));
            }
        }
        return out;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        list.render(g, this.font);
        super.extractRenderState(g, mouseX, mouseY, delta);
        renderTitle(g);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return list.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}