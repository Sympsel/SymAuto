package com.symauto.function.auto;

import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.gui.screen.function.AutoDiscardMoreThingManagerScreen;
import com.symauto.model.preset.KeepInvPreset;
import com.symauto.storage.KeepInvPresetStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoDiscardMoreThingFunction extends SymAbstractFunction {
    public static final AutoDiscardMoreThingFunction INSTANCE = new AutoDiscardMoreThingFunction();

    private AutoDiscardMoreThingFunction() {
        super("auto_discard_more_thing", "自动丢弃过多物品");
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line(ChatFormatting.YELLOW, "对主手持有物品不作判定")
                .line(ChatFormatting.YELLOW, "功能为逐tick检查背包，不用时记得关掉，以免影响游戏体验")
                .toString();
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new AutoDiscardMoreThingManagerScreen(parent);
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        // 检查玩家是否没有打开屏幕
        if (client.gui.screen() != null) {
            return;
        }

        List<KeepInvPreset> presets = KeepInvPresetStore.toModels();
        if (presets.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = client.player.inventoryMenu;
        int containerId = menu.containerId;

        for (KeepInvPreset preset : presets) {
            if (!preset.isActive()) {
                continue;
            }
            Item item = preset.getItem();
            if (item == null || item == Items.AIR) {
                continue;
            }
            processPreset(client, menu, containerId, preset);
        }
    }

    private void processPreset(Minecraft client, AbstractContainerMenu menu, int containerId, KeepInvPreset preset) {
        Item item = preset.getItem();
        boolean guard = preset.isEnchantedItemGuard();
        KeepInvPreset.Mode mode = preset.getMode();

        boolean applyCount = mode == KeepInvPreset.Mode.COUNT_CONSTRAINT || mode == KeepInvPreset.Mode.BOTH_CONSTRAINT;
        boolean applySlot = mode == KeepInvPreset.Mode.SLOT_CONSTRAINT || mode == KeepInvPreset.Mode.BOTH_CONSTRAINT;
        int mainHandSlot = client.player.getInventory().getSelectedSlot();

        // 收集匹配且可丢弃的槽位（跳过穿戴与受保护附魔物品），并统计总持有量
        List<Integer> discardable = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            if (slot.getContainerSlot() >= 36) {
                continue;
            }
            if (slot.getContainerSlot() == mainHandSlot) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.getItem() != item) {
                continue;
            }
            total += stack.getCount();
            if (guard && stack.isEnchanted()) {
                continue;
            }
            discardable.add(i);
        }

        if (discardable.isEmpty()) {
            return;
        }

        int keepCount = applyCount ? Math.max(0, preset.getMaxCount()) : Integer.MAX_VALUE;
        int keepSlots = applySlot ? Math.max(0, preset.getMaxSlot()) : Integer.MAX_VALUE;
        int occupiedSlots = discardable.size();

        // 槽位制约：超出保留槽位的整叠丢弃，从末尾开始
        int removeWholeStacks = Math.max(0, occupiedSlots - keepSlots);
        int cursor = discardable.size() - 1;
        while (cursor >= 0 && removeWholeStacks > 0) {
            int slotIndex = discardable.get(cursor);
            total -= menu.slots.get(slotIndex).getItem().getCount();
            client.gameMode.handleContainerInput(containerId, slotIndex, 1, ContainerInput.THROW, client.player);
            --removeWholeStacks;
            --cursor;
        }

        // 数量制约：仍超过 maxCount 时，从末尾逐件丢弃
        while (cursor >= 0 && total > keepCount) {
            int slotIndex = discardable.get(cursor);
            int stackCount = menu.slots.get(slotIndex).getItem().getCount();
            int toThrow = Math.min(stackCount, total - keepCount);
            for (int k = 0; k < toThrow; k++) {
                client.gameMode.handleContainerInput(containerId, slotIndex, 0, ContainerInput.THROW, client.player);
            }
            total -= toThrow;
            --cursor;
        }
    }
}