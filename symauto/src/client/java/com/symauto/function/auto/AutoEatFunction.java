package com.symauto.function.auto;

import com.symbase.config.option.IntOption;
import com.symbase.model.common.SymList;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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

    private static final Set<Item> MAIN_HAND_USABLE_ITEMS = Set.of(
            Items.BOW,
            Items.CROSSBOW,
            Items.TRIDENT,
            Items.ENDER_PEARL,
            Items.SNOWBALL,
            Items.EGG,
            Items.POTION,
            Items.SPLASH_POTION,
            Items.LINGERING_POTION,
            Items.MILK_BUCKET,
            Items.HONEY_BOTTLE,
            Items.FIREWORK_ROCKET,
            Items.FIREWORK_STAR,
            Items.FLINT_AND_STEEL,
            Items.BONE_MEAL,
            Items.LEAD,
            Items.NAME_TAG,
            Items.ARMOR_STAND,
            Items.WRITABLE_BOOK,
            Items.WRITTEN_BOOK,
            Items.DEBUG_STICK,
            Items.SHIELD,
            Items.FISHING_ROD,
            Items.GOAT_HORN,
            Items.HONEYCOMB,
            Items.SPYGLASS,
            Items.CARROT_ON_A_STICK,
            Items.WARPED_FUNGUS_ON_A_STICK
    );

    @Override
    public String tooltip() {
        return Tooltip.create()
                .line("饥饿值低于阈值（" + HUNGER_THRESHOLD_DEFAULT + "）时自动进食")
                .display(BLACK_LIST, item -> Component.translatable(item.getDescriptionId()).getString())
                .toString();
    }


    private InteractionHand eatingHand = InteractionHand.MAIN_HAND;
    private int initialFoodCount = 0;
    private boolean startedUsingItem = false;
    private int eatTicks = 0;
    private int blockedTicks = 0;
    private LocalPlayer trackedPlayer;

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
                "饥饿值低于该值时自动进食",
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
        if (client.player == null) {
            return;
        }
        if (client.player != trackedPlayer) {
            trackedPlayer = client.player;
            resetState();
            client.options.keyUse.setDown(false);
        }
        if (client.player.isDeadOrDying()) {
            if (eating) {
                stopEating(client);
            }
            return;
        }
        if (client.player.isCreative()) {
            return;
        }
        if (blockedTicks > 0) {
            --blockedTicks;
            return;
        }

        if (eating) {
            handleEating(client);
            return;
        }

        FoodData foodData = client.player.getFoodData();
        if (foodData.getFoodLevel() >= hungerThreshold) {
            return;
        }

        if (isInteractiveTarget(client)) {
            return;
        }

        InteractionHand hand = findFoodHand(client);
        if (hand == null) {
            return;
        }

        eatingHand = hand;
        initialFoodCount = client.player.getItemInHand(hand).getCount();
        startedUsingItem = false;
        eatTicks = 0;
        eating = true;
    }

    private void handleEating(Minecraft client) {
        ItemStack heldStack = client.player.getItemInHand(eatingHand);

        if (heldStack.isEmpty() || !heldStack.has(DataComponents.FOOD)) {
            stopEating(client);
            return;
        }

        ++eatTicks;
        if (!startedUsingItem && eatTicks > 10) {
            blockedTicks = 40;
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
        eatTicks = 0;
    }

    private InteractionHand findFoodHand(Minecraft client) {
        ItemStack mainHand = client.player.getMainHandItem();
        if (isEdible(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isUsableOnRightClick(mainHand)) {
            return null;
        }
        if (isEdible(client.player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private boolean isEdible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.has(DataComponents.FOOD)) return false;
        if (BLACK_LIST.contains(stack.getItem())) return false;
        return true;
    }

    private boolean isUsableOnRightClick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.has(DataComponents.FOOD)) return true;
        Item item = stack.getItem();
        return item instanceof BlockItem
                || item instanceof BucketItem
                || item instanceof ProjectileItem
                || item instanceof SpawnEggItem
                || item instanceof BoatItem
                || item instanceof MinecartItem
                || item instanceof HangingEntityItem
                || item instanceof ShovelItem
                || item instanceof HoeItem
                || item instanceof AxeItem
                || item instanceof BrushItem
                || item instanceof FireChargeItem
                || item instanceof ShearsItem
                || MAIN_HAND_USABLE_ITEMS.contains(item);
    }

    private boolean isInteractiveTarget(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult) {
            return true;
        }
        if (!(hit instanceof BlockHitResult blockHit) || client.level == null) {
            return false;
        }
        return isInteractiveBlock(client.level.getBlockState(blockHit.getBlockPos()).getBlock());
    }

    private boolean isInteractiveBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof EntityBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof BedBlock
                || block instanceof CakeBlock
                || block instanceof ComparatorBlock
                || block instanceof RepeaterBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || block instanceof ComposterBlock
                || block instanceof CampfireBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof BellBlock
                || block instanceof DragonEggBlock
                || block instanceof FlowerPotBlock
                || block instanceof CandleBlock
                || block instanceof CandleCakeBlock
                || block instanceof TntBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof CauldronBlock
                || block instanceof BeehiveBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof CaveVines;
    }
}
