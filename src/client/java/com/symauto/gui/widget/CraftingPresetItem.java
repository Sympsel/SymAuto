package com.symauto.gui.widget;

import com.symauto.storage.CraftingPresetStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 此处 Item 语义为 “条目”
 */
public class CraftingPresetItem {
    public static final int GAP = 2;

    private static final int W_ENABLE = 26;
    private static final int W_NAME = 84;
    private static final int W_TOGGLE = 30;
    private static final int W_DEL = 46;
    private static final int W_INPUT = 44;
    private static final int W_MOVE = 18;
    private static final int W_APPLY = 30;

    public static final class RowWidgets {
        public final List<Button> buttons;
        public final EditBox input;

        RowWidgets(List<Button> buttons, EditBox input) {
            this.buttons = buttons;
            this.input = input;
        }
    }

    public record Cell(int width, Supplier<Component> label, String tooltip, Runnable action) {
    }

    private final int index;

    public CraftingPresetItem(int index) {
        this.index = index;
    }

    public RowWidgets build(int x0, int y, int rowH, Font font,
                            Supplier<ItemStack> held, Runnable refresh) {
        List<Button> buttons = new ArrayList<>();
        int x = x0;
        for (Cell cell : cells(held)) {
            Button button = Button.builder(cell.label().get(), b -> {
                cell.action().run();
                refresh.run();
            }).bounds(x, y, cell.width(), rowH).build();
            if (cell.tooltip() != null) {
                button.setTooltip(Tooltip.create(Component.literal(cell.tooltip())));
            }
            buttons.add(button);
            x += cell.width() + GAP;
        }

        int inputY = y + (rowH - 16) / 2;
        EditBox input = new EditBox(font, x, inputY, W_INPUT, 16, Component.literal("执行次数"));
        String val = CraftingPresetStore.countText(index);
        input.setHint(Component.literal(val));
        input.setTooltip(Tooltip.create(Component.literal("执行次数，当前值 " + val)));
        input.setMaxLength(6);
        x += W_INPUT + GAP;
        Button apply = Button.builder(Component.literal("确认"), b -> {
                    try {
                        int v = Integer.parseInt(input.getValue().trim());
                        if (CraftingPresetStore.setExecuteCount(index, v)) {
                            refresh.run();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }).bounds(x, y, W_APPLY, rowH)
                .tooltip(Tooltip.create(Component.literal("应用输入框的执行次数")))
                .build();
        buttons.add(apply);
        return new RowWidgets(buttons, input);
    }

    private List<Cell> cells(Supplier<ItemStack> held) {
        List<Cell> cells = new ArrayList<>();

        cells.add(new Cell(W_ENABLE,
                () -> Component.literal(data().active ? "启用" : "禁用")
                        .withStyle(data().active ? ChatFormatting.GREEN : ChatFormatting.RED),
                null,
                () -> CraftingPresetStore.setActive(index, !data().active)));

        cells.add(new Cell(W_NAME,
                () -> Component.literal(CraftingPresetStore.displayName(index)),
                "点击替换为手持物品",
                () -> CraftingPresetStore.setItem(index, held.get())));

        cells.add(new Cell(W_TOGGLE,
                () -> Component.literal(CraftingPresetStore.isUnlimited(index) ? "是" : "否")
                        .withStyle(CraftingPresetStore.isUnlimited(index) ? ChatFormatting.BLUE : ChatFormatting.GRAY),
                "是否不限制次数（直到材料用完）",
                () -> CraftingPresetStore.toggleUnlimited(index)));

        cells.add(new Cell(W_TOGGLE,
                () -> Component.literal(data().recursive ? "是" : "否")
                        .withStyle(data().recursive ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
                "是否递归合成原材料",
                () -> CraftingPresetStore.toggleRecursive(index)));

        cells.add(new Cell(W_TOGGLE,
                () -> Component.literal(data().throwResult ? "是" : "否")
                        .withStyle(data().throwResult ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                "是否丢出产物",
                () -> CraftingPresetStore.toggleThrow(index)));

        cells.add(new Cell(W_DEL,
                () -> Component.literal("删除预设").withStyle(ChatFormatting.RED),
                null,
                () -> CraftingPresetStore.remove(index)));
        cells.add(new Cell(W_MOVE,
                () -> Component.literal("▲").withStyle(ChatFormatting.AQUA),
                "上移（调整执行优先级）",
                () -> CraftingPresetStore.move(index, -1)));

        cells.add(new Cell(W_MOVE,
                () -> Component.literal("▼").withStyle(ChatFormatting.AQUA),
                "下移（调整执行优先级）",
                () -> CraftingPresetStore.move(index, 1)));
        return cells;
    }

    private CraftingPresetStore.CraftingPresetData data() {
        return CraftingPresetStore.getPreset(index);
    }
}
