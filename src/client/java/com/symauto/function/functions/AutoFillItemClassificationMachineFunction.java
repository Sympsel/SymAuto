package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class AutoFillItemClassificationMachineFunction extends SymAbstractFunction {
    public static final AutoFillItemClassificationMachineFunction INSTANCE = new AutoFillItemClassificationMachineFunction();

    // 每次打开漏斗界面只尝试填充一次
    private boolean executed = false;

    private AutoFillItemClassificationMachineFunction() {
        super("auto_fill_item_classification_machine", "自动填充物品分类机", "自动将手持物品填充到漏斗对应格子，目前仅支持64堆叠");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            executed = false;
            return;
        }

        if (!(client.gui.screen() instanceof HopperScreen)) {
            executed = false;
            return;
        }
        if (executed) {
            return;
        }
        LocalPlayer player = client.player;
        AbstractContainerMenu menu = player.containerMenu;

        ItemStack itemStack = player.getMainHandItem();
        if (itemStack.isEmpty()) {
            return;
        }
        if (itemStack.getItem().getDefaultMaxStackSize() != 64) {
            fail(player, "填充失败，主手物品不是64堆叠");
            return;
        }
        if (itemStack.getCount() < 4) {
            fail(player, "填充失败，主手物品需要至少4个");
            return;
        }
        int containerSlotCount = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
        for (int i = 1; i < containerSlotCount; ++i) {
            if (menu.getSlot(i).hasItem() && !menu.getSlot(i).getItem().is(itemStack.getItem())) {
                fail(player, "填充失败，漏斗内有杂物");
                return;
            }
        }

        // 找到主手物品在容器中的槽位
        int mainHandSlot = findMainHandSlot(player, menu);
        if (mainHandSlot < 0) {
            fail(player, "填充失败，找不到主手槽位");
            return;
        }

        client.gameMode.handleContainerInput(
                menu.containerId, mainHandSlot, 0, ContainerInput.PICKUP, client.player
        );

        for (int i = 1; i < containerSlotCount; ++i) {
            if (!menu.getSlot(i).hasItem()) {
                client.gameMode.handleContainerInput(
                        menu.containerId, i, 1, ContainerInput.PICKUP, client.player
                );
            }
        }
        client.gameMode.handleContainerInput(
                menu.containerId, mainHandSlot, 0, ContainerInput.PICKUP, client.player);

        executed = true;
    }

    private int findMainHandSlot(LocalPlayer player, AbstractContainerMenu menu) {
        int selected = player.getInventory().getSelectedSlot();
        return menu.findSlot(player.getInventory(), selected).orElse(-1);
    }

    private void fail(LocalPlayer player, String message) {
        player.sendOverlayMessage(
                Component.literal(message).withStyle(ChatFormatting.RED));
        executed = true;
    }

    @Override
    protected void onDisable() {
        executed = false;
    }

    @Override
    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Set.of(
                AutoLootContainerFunction.INSTANCE.getClass(),
                AutoSwiftSameItemsToInventoryFunction.INSTANCE.getClass(),
                AutoSwiftSameItemsToContainerFunction.INSTANCE.getClass()
        );
    }
}
