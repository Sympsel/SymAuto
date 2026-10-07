package com.symauto.function.functions;

import com.symauto.entity.BWList;
import com.symauto.function.abstracts.SymAbstractFunction;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public class AutoEatFunction extends SymAbstractFunction {
    public static final AutoEatFunction INSTANCE = new AutoEatFunction();
    private static final int HUNGER_THRESHOLD = 16;
    private static final String TOOLTIP_BASE =
            "饥饿值低于阈值（" + HUNGER_THRESHOLD + "）时自动进食（仅主副手）";


    // 主手拿着这些物品时，禁止自动进食（避免右键误触发）
    private static final Set<Item> BLOCKED_MAIN_HAND_USABLES = Set.of(
            Items.FIREWORK_ROCKET,
            Items.FIREWORK_STAR
    );

    @Override
    public String getTooltip() {
        return TOOLTIP_BASE + "\n黑名单：" + BW_LISTED_FOOD.displayBlacklist(
                "\n §7",
                item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                "\n §7（空）"
        );
    }

    @Getter
    private boolean eating = false;

    @Getter
    private final BWList<Item> BW_LISTED_FOOD;

    private InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    private int initialFoodCount = 0;
    private boolean startedUsingItem = false;

    private AutoEatFunction() {
        String id = "auto_eat";
        String tooltips = "饥饿值低于阈值（" + HUNGER_THRESHOLD + "）时自动进食";
        BW_LISTED_FOOD = new BWList<>(id);
        super(id, "自动吃食物", tooltips);
        BW_LISTED_FOOD.withDefaultsApplier(
                AutoEatFunction::applyDefaults
        );
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        client.options.keyUse.setDown(false);
        if (client.player != null && client.player.isUsingItem() && client.getConnection() != null) {
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

        // 吃副手食物时，如果主手切出了烟花等可触发物品，立刻停止
        if (eatingHand == InteractionHand.OFF_HAND) {
            Item mainItem = client.player.getMainHandItem().getItem();
            if (BLOCKED_MAIN_HAND_USABLES.contains(mainItem)) {
                stopEating(client);
                return;
            }
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
            // 主手是烟花等会右键触发的物品时，不吃副手食物
            Item mainItem = client.player.getMainHandItem().getItem();
            if (BLOCKED_MAIN_HAND_USABLES.contains(mainItem)) {
                return null;
            }
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private boolean isEdible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.has(DataComponents.FOOD)) return false;
        if (BLOCKED_MAIN_HAND_USABLES.contains(stack.getItem())) return false;
        return !BW_LISTED_FOOD.isBlacklisted(stack.getItem());
    }

    public static void applyDefaults() {
        INSTANCE.BW_LISTED_FOOD.addAllToBlacklist(Set.of(
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.ROTTEN_FLESH,
                Items.SPIDER_EYE,
                Items.POTION,
                Items.PUFFERFISH,
                Items.POISONOUS_POTATO,
                Items.CHICKEN
        ));
    }
}