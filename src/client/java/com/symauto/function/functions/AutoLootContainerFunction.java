package com.symauto.function.functions;

import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class AutoLootContainerFunction extends SymAbstractFunction {
    private static final int PLAYER_SLOT_COUNT = 36;
    private boolean executed = false;

    public AutoLootContainerFunction() {
        super("auto_loot_container", "一键拿取容器物品", "打开容器会将容器内物品尽可能填满背包");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            executed = false;
            return;
        }
        AbstractContainerMenu menu = client.player.containerMenu;
        if (menu == client.player.inventoryMenu) {
            executed = false;
            return;
        }
        if (executed) {
            return;
        }
        executed = true;
        int containerId = menu.containerId;
        int containerSlotCount = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (containerSlotCount <= 0) {
            return;
        }
        for (int i = 0; i < containerSlotCount; ++i) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            client.gameMode.handleContainerInput(
                    containerId,
                    i,
                    0,
                    ContainerInput.QUICK_MOVE,
                    client.player
            );
        }
    }

    @Override
    protected void onDisable() {
        executed = false;
    }
}
