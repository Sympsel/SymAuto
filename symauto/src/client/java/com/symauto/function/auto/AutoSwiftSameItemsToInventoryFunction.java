package com.symauto.function.auto;

import com.symbase.config.option.BooleanOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symauto.core.Constants;
import com.symbase.function.util.ItemUtils;
import com.symbase.function.util.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Collections;
import java.util.Set;

public class AutoSwiftSameItemsToInventoryFunction extends SymAbstractFunction {
    public static final AutoSwiftSameItemsToInventoryFunction INSTANCE = new AutoSwiftSameItemsToInventoryFunction();
    private boolean executed = false;

    @Getter
    @Setter
    private boolean compareWithDataComponent;

    private AutoSwiftSameItemsToInventoryFunction() {
        super("auto_swift_same_items_to_inventory","自动转移相同物品到背包");
        addOption(new BooleanOption("compareWithDataComponent", "比较物品数据组件",
                "比较物品是否包含数据组件；关闭时，潜影盒内只有一种物品且两盒种类相同时视作同一物品",
                this::isCompareWithDataComponent, this::setCompareWithDataComponent, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("打开容器会将容器内的而且存在于背包的物品尽可能填满容器")
                .keyValueLine(ChatFormatting.YELLOW, "比较物品数据组件",
                        ChatFormatting.GRAY, compareWithDataComponent ? "是" : "否")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
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


