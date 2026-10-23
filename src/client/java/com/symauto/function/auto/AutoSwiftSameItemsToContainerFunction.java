package com.symauto.function.auto;

import com.symauto.config.option.BooleanOption;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

public class AutoSwiftSameItemsToContainerFunction extends SymAbstractFunction {
    public static final AutoSwiftSameItemsToContainerFunction INSTANCE = new AutoSwiftSameItemsToContainerFunction();

    private static final int PLAYER_SLOT_COUNT = 36;
    private boolean executed = false;

    @Getter
    @Setter
    private boolean compareWithDataComponent;

    private AutoSwiftSameItemsToContainerFunction() {
        super("auto_swift_same_items_to_container", "自动转移相同物品到容器");
        addOption(new BooleanOption("compareWithDataComponent", "比较物品数据组件",
                this::isCompareWithDataComponent, this::setCompareWithDataComponent, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("打开容器会将背包内的而且存在于容器的物品尽可能填满容器")
                .keyValueLine(ChatFormatting.YELLOW, "比较物品数据组件",
                        ChatFormatting.GRAY, compareWithDataComponent ? "是" : "否")
                .line("在比较数据组件模式关闭时，对于潜影盒，当盒内只有一种物品且两盒物品种类相同时视作同一物品")
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
        return Set.of(
                AutoSwiftSameItemsToInventoryFunction.class,
                AutoLootContainerFunction.class
        );
    }
}
