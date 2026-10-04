package com.symauto.function.functions;

import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public class AutoEatFunction extends SymAbstractFunction {
    private static final int HUNGER_THRESHOLD = 16;
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 9;

    // 金苹果，附魔金苹果，腐肉，蜘蛛眼，药水，河豚，毒马铃薯，生鸡肉
    private static final Set<Item> BLACKLISTED_FOOD = Set.of(
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.POTION,
            Items.PUFFERFISH,
            Items.POISONOUS_POTATO,
            Items.CHICKEN
    );

    private boolean eating = false;
    private boolean switchedSlot = false;
    private int previousSlot = -1;
    private int foodSlot = -1;
    private int initialFoodCount = 0;
    private boolean startedUsingItem = false;

    public AutoEatFunction() {
        String blacklist = String.join(", ", BLACKLISTED_FOOD.stream().map(Item::toString).toArray(String[]::new));
        super("自动吃食物", "饥饿值低于阈值（" + HUNGER_THRESHOLD + "）时自动进食\n黑名单：" + blacklist);

    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }

        if (client.gui.screen() != null) {
            if (eating) stopEating(client);
            return;
        }

        if (eating) {
            handleEating(client);
            return;
        }

        FoodData foodData = client.player.getFoodData();
        if (foodData.getFoodLevel() >= HUNGER_THRESHOLD) {
            return;
        }

        Inventory inv = client.player.getInventory();
        int slot = findFoodInHotbar(inv);
        if (slot < 0) {
            return;
        }

        startEating(client, slot);
    }

    private void handleEating(Minecraft client) {
        if (client.player.getInventory().getSelectedSlot() != foodSlot) {
            stopEating(client);
            return;
        }

        ItemStack heldStack = client.player.getMainHandItem();
        if (heldStack.isEmpty() || !heldStack.has(DataComponents.FOOD)) {
            stopEating(client);
            return;
        }

        client.options.keyUse.setDown(true);

        if (client.player.isUsingItem()) {
            if (client.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                startedUsingItem = true;
            } else {
                stopEating(client);
            }
            return;
        }

        if (startedUsingItem || heldStack.getCount() < initialFoodCount) {
            stopEating(client);
        }
    }

    private void startEating(Minecraft client, int slot) {
        if (client.player != null) {
            Inventory inv = client.player.getInventory();
            previousSlot = inv.getSelectedSlot();
            foodSlot = slot;
            initialFoodCount = inv.getItem(slot).getCount();
            switchedSlot = (previousSlot != slot);
            startedUsingItem = false;

            if (switchedSlot) {
                inv.setSelectedSlot(slot);
            }

            eating = true;
        }
    }

    private void stopEating(Minecraft client) {
        client.options.keyUse.setDown(false);
        if (client.player != null && client.player.isUsingItem()) {
            client.player.stopUsingItem();
        }

        if (client.player != null && switchedSlot && client.player.getInventory().getSelectedSlot() == foodSlot) {
            Inventory inv = client.player.getInventory();
            inv.setSelectedSlot(previousSlot);
        }

        eating = false;
        switchedSlot = false;
        previousSlot = -1;
        foodSlot = -1;
        initialFoodCount = 0;
        startedUsingItem = false;
    }

    private int findFoodInHotbar(Inventory inv) {
        for (int i = HOTBAR_START; i < HOTBAR_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.has(DataComponents.FOOD)) {
                if (BLACKLISTED_FOOD.contains(stack.getItem())) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }
}