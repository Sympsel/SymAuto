package com.symauto.gui.widget.item;

import com.symauto.gui.widget.LineWidget;
import com.symauto.storage.CraftingPresetStore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * 此处 Item 语义为 “条目”
 */
public class CraftingPresetItem {
    private final int index;
    public CraftingPresetItem(int index) {
        this.index = index;
    }

    public List<AbstractWidget> build(int x0, int y, Font font,
                                      Supplier<ItemStack> held, Runnable refresh) {
        String val = CraftingPresetStore.countText(index);
        return new LineWidget(font, x0, y)
                .appendEnableButton(() -> data().active, "",
                        () -> CraftingPresetStore.setActive(index, !data().active), refresh)
                .appendPlainButton(CraftingPresetStore.displayName(index), "",
                        null, refresh)
                .appendToggleButton(CraftingPresetStore.isUnlimited(index),
                        "是否不限制次数（直到材料用完）",
                        () -> CraftingPresetStore.toggleUnlimited(index), refresh)
                .appendToggleButton(data().recursive,
                        "是否递归合成原材料",
                        () -> CraftingPresetStore.toggleRecursive(index), refresh)
                .appendToggleButton(data().throwResult,
                        "是否丢出产物",
                        () -> CraftingPresetStore.toggleThrow(index), refresh)
                .appendDeleteButton("删除", "删除此预设",
                        () -> CraftingPresetStore.remove(index), refresh)
                .appendMoveButton(true, "上移（调整执行优先级）",
                        () -> CraftingPresetStore.move(index, -1), refresh)
                .appendMoveButton(false, "下移（调整执行优先级）",
                        () -> CraftingPresetStore.move(index, 1), refresh)
                .appendEditBox(val, "执行次数，当前值 " + val, 5,
                        v -> {
                            try {
                                if (CraftingPresetStore.setExecuteCount(index, Integer.parseInt(v.trim()))) {
                                    refresh.run();
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        },
                        "应用输入框的执行次数")
                .widgets();
    }

    private CraftingPresetStore.CraftingPresetData data() {
        return CraftingPresetStore.getPreset(index);
    }
}
