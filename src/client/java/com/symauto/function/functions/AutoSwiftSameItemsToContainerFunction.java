package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Collections;
import java.util.Set;

public class AutoSwiftSameItemsToContainerFunction extends SymAbstractFunction {
    private static final int PLAYER_SLOT_COUNT = 36;
    private boolean executed = false;

    public AutoSwiftSameItemsToContainerFunction() {
        super("auto_swift_same_items_to_container", "自动转移相同物品到容器",
                "打开容器会将背包内的而且存在于容器的物品尽可能填满容器\n对于潜影盒，当盒内只有一种物品且两盒物品相同时视作同一物品");
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

        for (int i = containerSlotCount; i < menu.slots.size(); ++i) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            if (ItemUtils.containsInContainer(slot, menu)) {
                client.gameMode.handleContainerInput(
                        containerId,
                        i,
                        0,
                        ContainerInput.QUICK_MOVE,
                        client.player
                );
            }
        }
    }

    @Override
    protected void onDisable() {
        executed = false;
    }

    @Override
    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Collections.singleton(AutoSwiftSameItemsToInventoryFunction.class);
    }
}
