package com.symauto.model.preset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.Item;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class KeepInvPreset {
    @AllArgsConstructor
    public enum Mode {
        COUNT_CONSTRAINT("数量制约"),
        SLOT_CONSTRAINT("槽位制约"),
        BOTH_CONSTRAINT("数量和槽位制约");
        @Getter
        private final String label;
    }

    private Item item;
    // 最多数量
    int maxCount;
    // 最多占用槽位
    int maxSlot;
    // 模式
    private Mode mode;
    // 是否保护附魔物品
    private boolean enchantedItemGuard;
    // 是否启用该条预设
    private boolean active;
}
