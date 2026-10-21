package com.symauto.function.auto;

import com.symauto.config.option.IntOption;
import com.symauto.model.common.SymList;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
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
    private static final int HUNGER_THRESHOLD_DEFAULT = 16;
    @Getter
    @Setter
    private int hungerThreshold = HUNGER_THRESHOLD_DEFAULT;
    @Getter
    private boolean eating = false;

    @Getter
    private final SymList<Item> BLACK_LIST;


    // 主手拿着这些物品时，禁止自动进食（避免右键误触发）
    private static final Set<Item> BLOCKED_MAIN_HAND_USABLES = Set.of(
            Items.FIREWORK_ROCKET,
            Items.FIREWORK_STAR
    );

    @Override
    public String tooltip() {
        return Tooltip.create()
                .line("饥饿值低于阈值（" + HUNGER_THRESHOLD_DEFAULT + "）时自动进食")
                .display(BLACK_LIST, item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toString();
    }


    private InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    private int initialFoodCount = 0;
    private boolean startedUsingItem = false;

    private AutoEatFunction() {
        String id = "auto_eat";
        BLACK_LIST = new SymList<>(id, SymList.Type.BLACKLIST);
        super(id, "自动吃食物");
        BLACK_LIST.withDefaultItems(
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.ROTTEN_FLESH,
                Items.SPIDER_EYE,
                Items.POTION,
                Items.PUFFERFISH,
                Items.POISONOUS_POTATO,
                Items.CHICKEN
        );
        addOption(new IntOption("hunger_threshold", "饥饿值阈值",
                this::getHungerThreshold, this::setHungerThreshold,
                0, 20, 1));
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
        if (foodData.getFoodLevel() >= HUNGER_THRESHOLD_DEFAULT) {
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
        return !BLACK_LIST.contains(stack.getItem());
    }
}