package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AutoCraftTransmitterFunction extends SymAbstractFunction {
    public static final AutoCraftTransmitterFunction INSTANCE = new AutoCraftTransmitterFunction();
    private static final int RESULT_SLOT = 0;
    private static final int GRID_FIRST_SLOT = 1;
    private static final int GRID_SIZE = 9;
    // 合成后等待 ticks 同步
    private static final int SETTLE_TICKS = 0;

    private int cooldown = 0;

    private enum State {
        WAITING_FOR_OPEN_CRAFTING_TABLE,
        CHECK_INGREDIENTS_FOR_BOW,
        CRAFT_BOW,
        CHECK_INGREDIENTS_FOR_TRANSMITTER,
        CRAFT_TRANSMITTER
    }

    private static final Item[] GRID_BOW = {
            null, Items.STICK, Items.STRING,
            Items.STICK, null, Items.STRING,
            null, Items.STICK, Items.STRING
    };

    private static final Item[] GRID_DISPENSER = {
            Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE,
            Items.COBBLESTONE, Items.BOW, Items.COBBLESTONE,
            Items.COBBLESTONE, Items.REDSTONE, Items.COBBLESTONE,
    };

    private State state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
    private static final Logger logger = Logger.getLogger(AutoCraftTransmitterFunction.class.getName());

    private AutoCraftTransmitterFunction() {
        super("auto_craft_transmitter", "自动合成发射器",
                Tooltip.create()
                        .line("打开工作台将尽可能多得将背包内物品合成发射器")
                        .keyValueLine(ChatFormatting.YELLOW, "原材料", ChatFormatting.GRAY, "木棍、线、原石、红石")
                        .toString()
                );
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            reset();
            return;
        }
        // 光标有物品时直接返回
        if (!client.player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        advanceStateMachine(client);
    }

    private void advanceStateMachine(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        boolean craftingOpen = client.gui.screen() instanceof CraftingScreen;
        if (!craftingOpen) {
            if (state != State.WAITING_FOR_OPEN_CRAFTING_TABLE) {
                state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
            }
            return;
        }
        logger.info("state: " + state);
        switch (state) {
            case WAITING_FOR_OPEN_CRAFTING_TABLE -> state = State.CHECK_INGREDIENTS_FOR_BOW;

            case CHECK_INGREDIENTS_FOR_BOW -> {
                if (hasPlainBow(player.getInventory())) {
                    state = State.CHECK_INGREDIENTS_FOR_TRANSMITTER;
                } else if (canCraftFromGrid(GRID_BOW, player.getInventory())) {
                    state = State.CRAFT_BOW;
                } else {
                    state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
                }
            }
            case CRAFT_BOW -> {
                if (craft(client, GRID_BOW)) {
                    cooldown = SETTLE_TICKS;
                    state = State.CHECK_INGREDIENTS_FOR_BOW;
                } else {
                    state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
                }
            }
            case CHECK_INGREDIENTS_FOR_TRANSMITTER -> {
                if (canCraftFromGrid(GRID_DISPENSER, player.getInventory())) {
                    state = State.CRAFT_TRANSMITTER;
                } else {
                    state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
                }
            }
            case CRAFT_TRANSMITTER -> {
                if (craft(client, GRID_DISPENSER)) {
                    cooldown = SETTLE_TICKS;
                    state = State.CHECK_INGREDIENTS_FOR_BOW;
                } else {
                    state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
                }
            }
        }
    }

    private boolean canCraftFromGrid(Item[] grid, Inventory inventory) {
        Map<Item, Integer> required = new HashMap<>();
        for (Item item : grid) {
            if (item != null) {
                required.merge(item, 1, Integer::sum);
            }
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            Item item = entry.getKey();
            int need = entry.getValue();
            int have = 0;
            for (ItemStack stack : inventory) {
                if (stack.isEmpty() || !stack.is(item)) {
                    continue;
                }
                // 弓作为发射器原料时只计未附魔的
                if (item == Items.BOW && stack.isEnchanted()) {
                    continue;
                }
                have += stack.getCount();
                if (have >= need) {
                    break;
                }
            }
            if (have < need) {
                return false;
            }
        }
        return true;
    }

    private void reset() {
        state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
        cooldown = 0;
    }

    private boolean craft(Minecraft client, Item[] grid) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        AbstractContainerMenu menu = player.containerMenu;
        int containerId = menu.containerId;

        for (int pos = 0; pos < GRID_SIZE; pos++) {
            Item need = grid[pos];
            if (need == null) {
                continue;
            }
            int gridSlot = GRID_FIRST_SLOT + pos;
            // 网格该槽位已有残留，放弃本次
            if (menu.getSlot(gridSlot).hasItem()) {
                return false;
            }
            int src = findInventorySlot(menu, player.getInventory(), need);
            if (src < 0) {
                return false; // 材料位置丢失
            }
            click(client, containerId, src, 0, ContainerInput.PICKUP);   // 取整叠
            click(client, containerId, gridSlot, 1, ContainerInput.PICKUP); // 放 1 个
            click(client, containerId, src, 0, ContainerInput.PICKUP);   // 剩余放回
        }
        click(client, containerId, RESULT_SLOT, 0, ContainerInput.QUICK_MOVE); // 合成入背包
        return true;
    }

    private int findInventorySlot(AbstractContainerMenu menu, Inventory inventory, Item need) {
        for (Slot slot : menu.slots) {
            if (slot.container == inventory) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || !stack.is(need)) {
                    continue;
                }
                // 弓作为发射器原料时只取未附魔的
                if (need == Items.BOW && stack.isEnchanted()) {
                    continue;
                }
                return slot.index;
            }
        }
        return -1;
    }

    private void click(Minecraft client, int containerId, int slot, int button, ContainerInput type) {
        if (client.gameMode == null) {
            return;
        }
        client.gameMode.handleContainerInput(containerId, slot, button, type, client.player);
    }

    @Override
    protected void onDisable() {
        state = State.WAITING_FOR_OPEN_CRAFTING_TABLE;
    }

    private boolean hasPlainBow(Inventory inventory) {
        return inventory.contains(stack -> !stack.isEmpty() && stack.is(Items.BOW) && !stack.isEnchanted());
    }
}
