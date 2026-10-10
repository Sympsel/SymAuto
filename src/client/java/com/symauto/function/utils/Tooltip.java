package com.symauto.function.utils;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class Tooltip {
    private final List<String> lines = new ArrayList<>();
    private Tooltip() {

    }

    public static Tooltip create() {
        return new Tooltip();
    }

    // 追加一行普通文本
    public Tooltip line(String text) {
        if (text != null && !text.isEmpty()) {
            lines.add(text);
        }
        return this;
    }

    // 追加一行着色文本
    public Tooltip line(ChatFormatting color, String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        return line(color + text + ChatFormatting.RESET);
    }

    // 追加一行缩进文本
    public Tooltip line(int indent, String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        return line(" ".repeat(Math.max(0, indent)) + text);
    }

    // 组合追加一行着色的缩进文本
    public Tooltip line(int indent, ChatFormatting color, String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        return line(" ".repeat(Math.max(0, indent)) + color + text + ChatFormatting.RESET);
    }

    // 插入一个空行
    public Tooltip blank() {
        lines.add("");
        return this;
    }

    // 插入到第一行
    public Tooltip prepend(String text) {
        if (text != null && !text.isEmpty()) {
            lines.addFirst(text);
        }
        return this;
    }

    /**
     * 追加一行着色的键值对文本
     */
    public Tooltip keyValueLine(ChatFormatting keyColor, String key, ChatFormatting valueColor, String value) {
        return line(keyColor.toString() + key + ChatFormatting.RESET + ": " + valueColor.toString() + value + ChatFormatting.RESET);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public Tooltip clear() {
        lines.clear();
        return this;
    }

    @Override
    public String toString() {
        return String.join("\n", lines);
    }
}
