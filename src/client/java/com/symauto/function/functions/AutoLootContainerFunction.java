package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class AutoLootContainerFunction extends SymAbstractFunction {
    public static final AutoLootContainerFunction INSTANCE = new AutoLootContainerFunction();
    private boolean executed = false;

    private AutoLootContainerFunction() {
        super("auto_loot_container", "自动拿取容器物品", "打开容器会将容器内物品尽可能填满背包");
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
        int containerSlotCount = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
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
