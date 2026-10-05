package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.abstracts.SymAbstractFunction;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public class AutoEatFunction extends SymAbstractFunction {
    private static final int HUNGER_THRESHOLD = 16;
    @Getter
    private boolean eating = false;

    private static BWList<Item> BW_LISTED_FOOD;

    private InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    private int initialFoodCount = 0;
    private boolean startedUsingItem = false;

    public AutoEatFunction() {
        BW_LISTED_FOOD = new BWList<>();
        // todo 从配置文件中读取黑名单
        BW_LISTED_FOOD.addAllToBlacklist(Set.of(
                        Items.GOLDEN_APPLE,
                        Items.ENCHANTED_GOLDEN_APPLE,
                        Items.ROTTEN_FLESH,
                        Items.SPIDER_EYE,
                        Items.POTION,
                        Items.PUFFERFISH,
                        Items.POISONOUS_POTATO,
                        Items.CHICKEN
                )
        );
        String blacklist = String.join(", ", BW_LISTED_FOOD.getBlacklist().stream().map(Item::toString).toArray(String[]::new));
        super("auto_eat", "自动吃食物", "饥饿值低于阈值（" + HUNGER_THRESHOLD + "）时自动进食（仅主副手）\n黑名单：" + blacklist);
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        client.options.keyUse.setDown(false);
        if (client.player != null && client.player.isUsingItem()) {
            client.player.stopUsingItem();
        }
        resetState();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (client.player.isCreative()) {
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

        InteractionHand hand = findFoodHand(client);
        if (hand == null) {
            return;
        }

        eatingHand = hand;
        initialFoodCount = client.player.getItemInHand(hand).getCount();
        startedUsingItem = false;
        eating = true;
    }

    private void handleEating(Minecraft client) {
        if (client.player == null) {
            return;
        }
        ItemStack heldStack = client.player.getItemInHand(eatingHand);

        if (heldStack.isEmpty() || !heldStack.has(DataComponents.FOOD)) {
            stopEating(client);
            return;
        }

        client.options.keyUse.setDown(true);

        if (client.player.isUsingItem()) {
            if (client.player.getUsedItemHand() == eatingHand) {
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

    private void stopEating(Minecraft client) {
        client.options.keyUse.setDown(false);
        if (client.player != null && client.player.isUsingItem()) {
            client.player.stopUsingItem();
        }
        resetState();
    }

    private void resetState() {
        eating = false;
        eatingHand = InteractionHand.MAIN_HAND;
        initialFoodCount = 0;
        startedUsingItem = false;
    }

    private InteractionHand findFoodHand(Minecraft client) {
        if (client.player == null) {
            return null;
        }
        if (isEdible(client.player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isEdible(client.player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private boolean isEdible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.has(DataComponents.FOOD)) return false;
        return !BW_LISTED_FOOD.isBlacklisted(stack.getItem());
    }
}