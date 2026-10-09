package com.symauto.function.functions;

import com.symauto.entity.SymFunctionTags;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Constants;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Collections;
import java.util.Set;

public class AutoSwiftSameItemsToInventoryFunction extends SymAbstractFunction {
    public static final AutoSwiftSameItemsToInventoryFunction INSTANCE = new AutoSwiftSameItemsToInventoryFunction();
    private boolean executed = false;

    private AutoSwiftSameItemsToInventoryFunction() {
        super("auto_swift_same_items_to_inventory",
                "自动转移相同物品到背包", Tooltip.create()
                        .line("打开容器会将容器内的而且存在于背包的物品尽可能填满背包")
                        .line("对于潜影盒，当盒内只有一种物品且两盒物品种类相同时视作同一物品").toString()
        );
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
            if (ItemUtils.containsInInventory(slot, menu)) {
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
        return Collections.singleton(AutoSwiftSameItemsToContainerFunction.class);
    }
}
