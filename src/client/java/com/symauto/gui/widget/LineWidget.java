package com.symauto.gui.widget;

import com.symauto.gui.widget.component.ArrowButton;
import com.symauto.gui.widget.component.SymButton;
import com.symauto.gui.widget.component.SymEditBox;
import com.symauto.gui.widget.utils.ButtonFactory;
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
    private static final int MOVE_W = 20;
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
        SymButton button = ButtonFactory.toggle(Component.literal(on ? "启用" : "禁用"), x, y, ENABLE_W, ROW_H, on)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                }).tooltip(Component.literal(tooltip));
        widgets.add(button);
        x += ENABLE_W + GAP;
        return this;
    }

    public LineWidget appendToggleButton(boolean on, String tooltip, Runnable action, Runnable refresh) {
        SymButton button = ButtonFactory.toggle(Component.literal(on ? "是" : "否"), x, y, TOGGLE_W, ROW_H, on)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                }).tooltip(Component.literal(tooltip));
        widgets.add(button);
        x += TOGGLE_W + GAP;
        return this;
    }

    public LineWidget appendDeleteButton(String label, String tooltip, Runnable action, Runnable refresh) {
        SymButton button = ButtonFactory.delete(Component.literal(label), x, y, DEL_W, ROW_H)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                }).tooltip(Component.literal(tooltip));
        widgets.add(button);
        x += DEL_W + GAP;
        return this;
    }

    public LineWidget appendApplyButton(String tooltip, Runnable action, Runnable refresh) {
        SymButton button = ButtonFactory.normal(Component.literal("确认"), x, y, ENABLE_W, ROW_H)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                }).tooltip(Component.literal(tooltip));
        widgets.add(button);
        x += ENABLE_W + GAP;
        return this;
    }

    public LineWidget appendMoveButton(boolean isUp, String tooltip, Runnable action, Runnable refresh) {
        SymButton button;
        if (isUp) {
            button = ButtonFactory.move(Component.literal("上移"), x, y, MOVE_W, ROW_H, ArrowButton.Direction.UP)
                    .onLeftClick(() -> {
                        action.run();
                        refresh.run();
                    }).tooltip(Component.literal(tooltip));
        } else {
            button = ButtonFactory.move(Component.literal("下移"), x, y, MOVE_W, ROW_H, ArrowButton.Direction.DOWN)
                    .onLeftClick(() -> {
                        action.run();
                        refresh.run();
                    }).tooltip(Component.literal(tooltip));
        }
        widgets.add(button);
        x += MOVE_W + GAP;
        return this;
    }

    public LineWidget appendPlainButton(String text, String tooltip, Runnable action, Runnable refresh) {
        SymButton button = ButtonFactory.label(Component.literal(text), x, y, PLAIN_W, ROW_H)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                }).tooltip(Component.literal(tooltip));
        widgets.add(button);
        x += PLAIN_W + GAP;
        return this;
    }

    public LineWidget appendLabelButton(ChatFormatting style, String label, int width, String tooltip,
                                        Runnable action, Runnable refresh) {
        SymButton button = ButtonFactory.label(Component.literal(label).withStyle(style),
                        x, y, width, ROW_H)
                .onLeftClick(() -> {
                    action.run();
                    refresh.run();
                });
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
        EditBox input = new SymEditBox(x, inputY, EDIT_BOX_W, EDIT_BOX_H, Component.literal(hint));
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
