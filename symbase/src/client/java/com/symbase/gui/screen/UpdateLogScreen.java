package com.symbase.gui.screen;

import com.symbase.gui.base.SymBaseScreen;
import com.symbase.gui.widget.ScrollableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取 {@code CHANGELOG.md} 并渲染为游戏内更新日志
 */
public class UpdateLogScreen extends SymBaseScreen {
    private final Screen parent;
    private final List<String> messages;
    private ScrollableTextList list;

    private List<String> cachedRaw;
    private List<ScrollableTextList.Line> cachedLines;

    public UpdateLogScreen(Screen parent, String modId, Component title) {
        super(title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
        this.messages = loadMessages(modId);
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

    private static List<String> loadMessages(String modId) {
        String resource = "/assets/" + modId + "/update_log.md";
        List<String> result = new ArrayList<>();
        try (InputStream in = UpdateLogScreen.class.getResourceAsStream(resource)) {
            if (in == null) {
                System.err.println("[SymBase] 未找到更新日志资源: " + resource);
            } else {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.copyOf(result);
    }

    private List<ScrollableTextList.Line> parseLines() {
        List<String> raw = messages;
        if (raw == cachedRaw && cachedLines != null) {
            return cachedLines;
        }
        cachedLines = doParse();
        cachedRaw = raw;
        return cachedLines;
    }

    private List<ScrollableTextList.Line> doParse() {
        List<ScrollableTextList.Line> out = new ArrayList<>();
        for (String raw : messages) {
            for (String sourceLine : raw.split("\\R", -1)) {
                appendLine(out, sourceLine);
            }
        }
        return out;
    }

    private void appendLine(List<ScrollableTextList.Line> out, String sourceLine) {
        String line = sourceLine.stripTrailing();
        if (line.isBlank()) {
            return;
        }

        String trimmed = line.strip();
        if (trimmed.startsWith("## ")) {
            appendRelease(out, trimmed.substring(3).trim());
        } else if (trimmed.startsWith("### ")) {
            out.add(new ScrollableTextList.Line(
                    Component.literal("  " + trimmed.substring(4)).withStyle(ChatFormatting.GOLD), 0xFFFFFFFF));
        } else if (trimmed.startsWith("# ")) {
            // 文档标题由界面标题显示
        } else if (trimmed.startsWith("> ")) {
            out.add(new ScrollableTextList.Line(
                    Component.literal("  " + stripMarkdown(trimmed.substring(2)))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), 0xFFFFFFFF));
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            int indent = line.length() - line.stripLeading().length();
            String prefix = indent >= 2 ? "    • " : "  • ";
            out.add(new ScrollableTextList.Line(
                    Component.literal(prefix + stripMarkdown(trimmed.substring(2))), 0xFFFFFFFF));
        } else {
            out.add(new ScrollableTextList.Line(
                    Component.literal("  " + stripMarkdown(trimmed)), 0xFFFFFFFF));
        }
    }

    private void appendRelease(List<ScrollableTextList.Line> out, String heading) {
        String version = heading;
        String date = "";
        String[] parts = heading.split("\\s*[—–]\\s*|\\s+-\\s+", 2);
        if (parts.length == 2) {
            version = parts[0].trim();
            date = parts[1].trim();
        }
        String label = date.isEmpty() ? version : "[" + date + "] " + version;
        if (!out.isEmpty()) {
            out.add(new ScrollableTextList.Line(Component.literal(" "), 0xFFFFFFFF));
        }
        out.add(new ScrollableTextList.Line(
                Component.literal(label).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), 0xFFFFFFFF));
    }

    private String stripMarkdown(String text) {
        return text
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .trim();
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

