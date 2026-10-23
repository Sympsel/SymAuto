package com.symauto.gui.widget.item;

import com.symauto.gui.widget.LineWidget;
import com.symauto.storage.KeepInvPresetStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.List;

/**
 * 此处 Item 语义为 “条目”
 */
public class KeepInvPresetItem {
    private static final int W_NAME = 110;
    private static final int W_GUARD = 44;

    private final int index;

    public KeepInvPresetItem(int index) {
        this.index = index;
    }

    public List<AbstractWidget> build(int x0, int y, Font font, Runnable refresh) {
        boolean active = KeepInvPresetStore.isActive(index);
        boolean guard = KeepInvPresetStore.isGuard(index);
        String label = KeepInvPresetStore.displayName(index) + " · " + KeepInvPresetStore.modeName(index);
        String countVal = String.valueOf(KeepInvPresetStore.maxCount(index));
        String slotVal = String.valueOf(KeepInvPresetStore.maxSlot(index));

        return new LineWidget(font, x0, y)
                .appendEnableButton(() -> KeepInvPresetStore.isActive(index),
                        "点击切换该预设是否生效",
                        () -> KeepInvPresetStore.setActive(index, !active), refresh)
                .appendLabelButton(ChatFormatting.WHITE, label, W_NAME,
                        "点击切换制约模式：数量 → 槽位 → 两者",
                        () -> KeepInvPresetStore.toggleMode(index), refresh)
                .appendEditBox(countVal, "最多保留的数量，当前 " + countVal, 5,
                        v -> {
                            try {
                                if (KeepInvPresetStore.setMaxCount(index, Integer.parseInt(v.trim()))) {
                                    refresh.run();
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        },
                        "应用输入框的数量上限")
                .appendEditBox(slotVal, "最多占用的槽位，当前 " + slotVal, 5,
                        v -> {
                            try {
                                if (KeepInvPresetStore.setMaxSlot(index, Integer.parseInt(v.trim()))) {
                                    refresh.run();
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        },
                        "应用输入框的槽位上限")
                .appendLabelButton(guard ? ChatFormatting.GREEN : ChatFormatting.GRAY,
                        guard ? "附魔保护" : "附魔不管", W_GUARD,
                        "是否保护带附魔的同类物品不被丢弃",
                        () -> KeepInvPresetStore.toggleGuard(index), refresh)
                .appendDeleteButton("删除", "删除此预设",
                        () -> KeepInvPresetStore.remove(index), refresh)
                .widgets();
    }
}
