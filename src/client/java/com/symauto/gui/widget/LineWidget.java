package com.symauto.gui.widget;

import lombok.Data;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public class LineWidget {
    public static final int GAP = 2;
    private static final int ROW_H = 20;
    private static final int EDIT_BOX_H = 16;

    private static final int PLAIN_W = 84;
    private static final int ENABLE_W = 30;
    private static final int PULL_ROD_W = 25;
    private static final int TOGGLE_W = 30;
    private static final int DEL_W = 30;
    private static final int APPLY_W = 30;
    private static final int MOVE_W = 30;
    private static final int EDIT_BOX_W = 35;

    private final Font font;

    private int y;
    private int x;
    private List<AbstractWidget> widgets = new ArrayList<>();

    public LineWidget(Font font, int x0, int y) {
        this.font = font;
        this.x = x0;
        this.y = y;
    }

    public List<AbstractWidget> widgets() {
        return widgets;
    }

    public LineWidget appendEnableButton(Supplier<Boolean> enabled, String tooltip, Runnable action, Runnable refresh) {
        boolean on = Boolean.TRUE.equals(enabled.get());
        return appendLabelButton(on ? ChatFormatting.GREEN : ChatFormatting.RED,
                on ? "启用" : "禁用", ENABLE_W, tooltip, action, refresh);
    }

    public LineWidget appendToggleButton(boolean on, ChatFormatting onStyle, ChatFormatting offStyle,
                                         String tooltip, Runnable action, Runnable refresh) {
        return appendLabelButton(on ? onStyle : offStyle, on ? "是" : "否", TOGGLE_W, tooltip, action, refresh);
    }

    public LineWidget appendDeleteButton(String label, String tooltip, Runnable action, Runnable refresh) {
        return appendLabelButton(ChatFormatting.RED, label, DEL_W, tooltip, action, refresh);
    }

    public LineWidget appendApplyButton(String tooltip, Runnable action, Runnable refresh) {
        return appendLabelButton(ChatFormatting.WHITE, "确认", APPLY_W, tooltip, action, refresh);
    }

    public LineWidget appendMoveButton(boolean isUp, String tooltip, Runnable action, Runnable refresh) {
        return appendLabelButton(ChatFormatting.AQUA, isUp ? "上移" : "下移", MOVE_W, tooltip, action, refresh);
    }

    public LineWidget appendPlainButton(String text, String tooltip, Runnable action, Runnable refresh) {
        return appendLabelButton(ChatFormatting.WHITE, text, PLAIN_W, tooltip, action, refresh);
    }

    public LineWidget appendLabelButton(ChatFormatting style, String label, int width, String tooltip,
                                        Runnable action, Runnable refresh) {
        Button button = Button.builder(Component.literal(label).withStyle(style), b -> {
                    action.run();
                    refresh.run();
                }).bounds(x, y, width, ROW_H)
                .build();
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        widgets.add(button);
        x += width + GAP;
        return this;
    }


    public LineWidget appendEditBox(String hint, String tooltip, int maxLength,
                                    Consumer<String> applier, String applyTooltip) {
        int inputY = y + (ROW_H - EDIT_BOX_H) / 2;
        EditBox input = new EditBox(font, x, inputY, EDIT_BOX_W, EDIT_BOX_H, Component.literal(hint));
        input.setHint(Component.literal(hint));
        if (tooltip != null) {
            input.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        input.setMaxLength(maxLength);
        widgets.add(input);
        x += EDIT_BOX_W + GAP;
        return appendApplyButton(applyTooltip, () -> applier.accept(input.getValue()), () -> {
        });
    }
}
