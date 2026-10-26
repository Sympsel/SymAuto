package com.symauto.function.auto;

import com.symauto.config.option.BooleanOption;
import com.symauto.core.Constants;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.container.depend.BundleAccess;
import com.symauto.function.utils.BundleContentsHelper;
import com.symauto.function.utils.ContainerActions;
import com.symauto.function.utils.Tooltip;
import com.symauto.mixin.accessor.AbstractContainerScreenAccessor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class AutoHarvestCropFunction extends SymAbstractFunction {
    public static final AutoHarvestCropFunction INSTANCE = new AutoHarvestCropFunction();

    @Getter
    @Setter
    private double radius = 4.0;

    /**
     * 剩余多少时开始补充
     */
    @Getter
    @Setter
    private int refillMinCount = 4;

    @Getter
    @Setter
    private boolean enableDiscardResultIfNotSeed;

    @Getter
    @Setter
    private boolean autoRefill;

    @Getter
    @Setter
    private boolean onlyRipeningCrop;

    private int boneMealSlot = -1;

    private static final Set<Item> PLANTABLE_ON_FARMLAND = Set.of(
            Items.WHEAT_SEEDS,
            Items.CARROT,
            Items.POTATO,
            Items.BEETROOT_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.MELON_SEEDS,
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES
    );

    private static final Set<Item> DISCARDABLE_BYPRODUCTS = Set.of(
            Items.WHEAT,
            Items.POISONOUS_POTATO, // 毒马铃薯
            Items.MELON_SLICE,
            Items.MELON,
            Items.PUMPKIN,
            Items.BEETROOT
    );

    private AutoHarvestCropFunction() {
        super("auto_harvest_corp", "自动收获作物");
        addOption(new BooleanOption("auto_refill", "自动补充种子和骨粉",
                this::isAutoRefill, this::setAutoRefill, true));
        addOption(new BooleanOption("enable_discard_result_if_not_seed", "自动丢出非种子产物",
                this::isEnableDiscardResultIfNotSeed, this::setEnableDiscardResultIfNotSeed, false));
        addOption(new BooleanOption("only_ripening_crop", "仅使用骨粉催熟作物",
                this::isOnlyRipeningCrop, this::setOnlyRipeningCrop, false));

    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .keyValueLine("自动补充种子和骨粉", autoRefill ? "是" : "否")
                .keyValueLine("自动丢出非种子产物", enableDiscardResultIfNotSeed ? "是" : "否")
                .keyValueLine("仅使用骨粉催熟作物", onlyRipeningCrop ? "是" : "否")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        // 不打断自动进食
        if (AutoEatFunction.INSTANCE.isEating()) {
            return;
        }

        if (enableDiscardResultIfNotSeed) {
            discardByproducts(client);
        }
        Vec3 eye = client.player.getEyePosition();
        double reach = Math.min(radius, client.player.blockInteractionRange());
        double reachSq = reach * reach;
        int r = (int) Math.ceil(reach);
        BlockPos center = client.player.blockPosition();

        if (!onlyRipeningCrop) {
            if (harvestOne(client, eye, reachSq, r, center)) {
                return;
            }
            if (replantOne(client, eye, reachSq, r, center)) {
                return;
            }
        }
        boneMealOne(client, eye, reachSq, r, center);
        if (autoRefill) {
            ensurePlantSupply(client);
        }
    }

    /**
     * 自动丢出副产物
     */
    private void discardByproducts(Minecraft client) {
        Inventory inv = client.player.getInventory();
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !DISCARDABLE_BYPRODUCTS.contains(stack.getItem())) {
                continue;
            }
            ContainerActions.dropSlotStack(invToMenuSlot(i));
            return;
        }
    }

    private boolean isHoveringItem(Minecraft client) {
        Screen screen = client.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        Slot hovered = ((AbstractContainerScreenAccessor) screen).symauto$getHoveredSlot();
        return hovered != null && hovered.hasItem();
    }

    private void ensurePlantSupply(Minecraft client) {
        if (client.player.isCreative()) {
            return;
        }
        if (client.player.containerMenu != client.player.inventoryMenu) {
            return;
        }
        ItemStack carried = client.player.inventoryMenu.getCarried();
        if (!carried.isEmpty()) {
            stashCarried(client, carried);
            return;
        }
        if (refillOffhandPlant(client)) {
            return;
        }
        refillMainhandBoneMeal(client);
    }

    /**
     * 仅当副手是可种植物品且数量不足时，补充种子到副手
     */
    private boolean refillOffhandPlant(Minecraft client) {
        Item desired = desiredPlantItem(client);
        if (desired == null) {
            return false;
        }
        ItemStack probe = new ItemStack(desired);
        ItemStack off = client.player.getOffhandItem();
        if (off.is(desired) && off.getCount() >= refillMinCount) {
            return false;
        }

        int src = findInventorySlotFor(client, probe);
        if (src >= 0) {
            swapToOffhand(src);
            return true;
        }

        int[] loc = findBundleSlotFor(client, probe);
        if (loc != null) {
            BundleAccess.extractToInventory(loc[0], loc[1], probe);
            return true;
        }
        return false;
    }

    /**
     * 仅当主手骨粉、副手是可催熟作物且骨粉不足时，补充骨粉到主手
     */
    private void refillMainhandBoneMeal(Minecraft client) {
        if (!isPlantable(client.player.getOffhandItem())) {
            return;
        }
        ItemStack main = client.player.getMainHandItem();
        Inventory inv = client.player.getInventory();
        ItemStack probe = new ItemStack(Items.BONE_MEAL);
        int selected = inv.getSelectedSlot();

        boolean needsRefill;
        if (main.is(Items.BONE_MEAL)) {
            boneMealSlot = selected;
            needsRefill = main.getCount() < refillMinCount;
        } else if (main.isEmpty() && boneMealSlot == selected) {
            // 骨粉用完 + 该槽此前是骨粉
            needsRefill = true;
        } else {
            return;
        }
        if (!needsRefill) {
            return;
        }

        int src = findInventorySlotFor(client, probe);
        if (src >= 0) {
            int targetMenu = Constants.HOTBAR_START_IN_INVENTORY + selected;
            if (src >= Constants.INV_START && src < Constants.INV_END) {
                ContainerActions.swapToHotbar(src, selected);
            } else if (src != targetMenu) {
                // 快捷栏来源：点击对调两个快捷栏槽
                swapMenuSlots(src, targetMenu);
            }
        }
    }

    /**
     * 把光标残留物品放回背包
     */
    private void stashCarried(Minecraft client, ItemStack carried) {
        Inventory inv = client.player.getInventory();
        int empty = -1;
        for (int i = Constants.INV_START; i < Constants.INV_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                if (empty < 0) {
                    empty = i;
                }
            } else if (ItemStack.isSameItemSameComponents(stack, carried)
                    && stack.getCount() < stack.getMaxStackSize()) {
                ContainerActions.leftClickSlot(i);
                return;
            }
        }
        if (empty >= 0) {
            ContainerActions.leftClickSlot(empty);
        } else {
            ContainerActions.dropCarried();
        }
    }

    private Item desiredPlantItem(Minecraft client) {
        ItemStack off = client.player.getOffhandItem();
        if (!off.isEmpty() && isPlantable(off)) {
            return off.getItem();
        }
        return firstPlantableInInventory(client);
    }

    /**
     * 找到第一个可种植的物品
     */
    private Item firstPlantableInInventory(Minecraft client) {
        Inventory inv = client.player.getInventory();
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            Item item = inv.getItem(i).getItem();
            if (PLANTABLE_ON_FARMLAND.contains(item)) {
                return item;
            }
        }
        AbstractContainerMenu menu = client.player.inventoryMenu;
        for (Slot slot : menu.slots) {
            if (slot.container != inv
                    || slot.index < Constants.INV_START
                    || slot.index >= Constants.INV_END) {
                continue;
            }
            ItemStack bundle = slot.getItem();
            if (!BundleContentsHelper.isNonEmptyBundle(bundle)) {
                continue;
            }
            BundleContents contents = BundleContentsHelper.getContents(bundle);
            if (contents == null) {
                continue;
            }
            for (ItemStack s : contents.itemCopyStream().toList()) {
                if (PLANTABLE_ON_FARMLAND.contains(s.getItem())) {
                    return s.getItem();
                }
            }
        }
        return null;
    }

    /**
     * 找到收纳袋中首个含 probe 的条目，返回 {收纳袋菜单槽号, 袋内条目索引}，无则 null
     */
    private int[] findBundleSlotFor(Minecraft client, ItemStack probe) {
        AbstractContainerMenu menu = client.player.inventoryMenu;
        Inventory inv = client.player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.container != inv
                    || slot.index < Constants.INV_START
                    || slot.index >= Constants.INV_END) {
                continue;
            }
            ItemStack bundle = slot.getItem();
            if (!BundleContentsHelper.isNonEmptyBundle(bundle)) {
                continue;
            }
            BundleContents contents = BundleContentsHelper.getContents(bundle);
            if (contents == null) {
                continue;
            }
            int idx = 0;
            for (ItemStack s : contents.itemCopyStream().toList()) {
                if (ItemStack.isSameItemSameComponents(s, probe)) {
                    return new int[]{slot.index, idx};
                }
                idx++;
            }
        }
        return null;
    }

    private int findInventorySlotFor(Minecraft client, ItemStack probe) {
        Inventory inv = client.player.getInventory();
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, probe)) {
                return invToMenuSlot(i);
            }
        }
        return -1;
    }

    private static int invToMenuSlot(int invIndex) {
        if (invIndex < Constants.HOTBAR_START || invIndex >= Constants.INV_END) {
            return -1;
        }
        return invIndex < Constants.HOTBAR_END
                ? Constants.HOTBAR_START_IN_INVENTORY + invIndex
                : invIndex;
    }

    private void swapMenuSlots(int menuA, int menuB) {
        ContainerActions.leftClickSlot(menuA);
        ContainerActions.leftClickSlot(menuB);
        ContainerActions.leftClickSlot(menuA);
    }

    private boolean harvestOne(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            BlockPos target = pos.immutable();

            // 发光浆果
            if (state.hasProperty(BlockStateProperties.BERRIES)
                    && state.getValue(BlockStateProperties.BERRIES)) {
                if (pickBlock(client, target)) {
                    return true;
                }
                continue;
            }

            // 浆果丛
            if (state.is(Blocks.SWEET_BERRY_BUSH)) {
                if (!isFullyGrown(state)) {
                    continue;
                }
                if (pickBlock(client, target)) {
                    return true;
                }
                continue;
            }

            // 甘蔗：只收第二层及以上，保留最底层继续生长
            if (state.is(Blocks.SUGAR_CANE)) {
                if (client.level.getBlockState(target.below()).is(Blocks.SUGAR_CANE)) {
                    breakInstant(client, target);
                    return true;
                }
                continue;
            }

            // 地狱疣
            if (state.is(Blocks.NETHER_WART)) {
                if (!isFullyGrown(state)) {
                    continue;
                }
                if (breakInstant(client, target)) {
                    replantHarvestedTile(client, target.below());
                    return true;
                }
                continue;
            }

            // 通用作物
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                if (breakInstant(client, target)) {
                    replantHarvestedTile(client, target.below());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 定向补种，收获当 tick 立即补种
     */
    private void replantHarvestedTile(Minecraft client, BlockPos surfacePos) {
        InteractionHand hand = resolvePlantHand(client);
        if (hand == null) {
            return;
        }
        ItemStack plantStack = hand == InteractionHand.MAIN_HAND
                ? client.player.getMainHandItem()
                : client.player.getOffhandItem();
        Block surface = plantStack.is(Items.NETHER_WART) ? Blocks.SOUL_SAND : Blocks.FARMLAND;
        if (!client.level.getBlockState(surfacePos).is(surface)) {
            return;
        }
        BlockPos target = surfacePos.immutable();
        Vec3 hitVec = new Vec3(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
        client.gameMode.useItemOn(
                client.player,
                hand,
                new BlockHitResult(hitVec, Direction.UP, target, false)
        );
    }

    /**
     * 主手优先取可种植物的手，无可种植物返回 null
     */
    private InteractionHand resolvePlantHand(Minecraft client) {
        if (isPlantable(client.player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isPlantable(client.player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isFullyGrown(BlockState state) {
        IntegerProperty age = null;
        for (Property<?> p : state.getProperties()) {
            String name = p.getName();
            if ((name.equals("age") || name.equals("stage") || name.equals("growth"))
                    && p instanceof IntegerProperty ip) {
                age = ip;
                break;
            }
        }
        if (age == null) {
            return false;
        }
        int max = Integer.MIN_VALUE;
        for (Integer v : age.getPossibleValues()) {
            if (v > max) {
                max = v;
            }
        }
        return state.getValue(age) >= max;
    }

    private boolean replantOne(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
        InteractionHand hand = resolvePlantHand(client);
        if (hand == null) {
            return false;
        }
        ItemStack plantStack = hand == InteractionHand.MAIN_HAND
                ? client.player.getMainHandItem()
                : client.player.getOffhandItem();
        boolean cane = plantStack.is(Items.SUGAR_CANE);
        Block surface = cane ? null
                : (plantStack.is(Items.NETHER_WART) ? Blocks.SOUL_SAND : Blocks.FARMLAND);

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            BlockPos target;
            if (cane) {
                if (!state.isAir()) {
                    continue;
                }
                // 脚下已是甘蔗，跳过
                if (client.level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)) {
                    continue;
                }
                if (!Blocks.SUGAR_CANE.defaultBlockState().canSurvive(client.level, pos)) {
                    continue;
                }
                target = pos;
            } else {
                if (!state.is(surface)) {
                    continue;
                }
                target = pos.above();
                if (!client.level.getBlockState(target).isAir()) {
                    continue;
                }
            }

            BlockPos support = target.below();
            Vec3 hitVec = new Vec3(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
            InteractionResult result = client.gameMode.useItemOn(
                    client.player,
                    hand,
                    new BlockHitResult(hitVec, Direction.UP, support.immutable(), false)
            );
            return true;
        }
        return false;
    }

    private boolean isPlantable(ItemStack stack) {
        return !stack.isEmpty()
                && (PLANTABLE_ON_FARMLAND.contains(stack.getItem())
                || stack.is(Items.NETHER_WART)
                || stack.is(Items.SUGAR_CANE));
    }

    /**
     * 右键采摘
     */
    private boolean pickBlock(Minecraft client, BlockPos target) {
        Vec3 hitVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        InteractionResult result = client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hitVec, Direction.UP, target, false)
        );
        return result.consumesAction();
    }

    private boolean breakInstant(Minecraft client, BlockPos target) {
        if (client.gameMode.startDestroyBlock(target, Direction.UP)) {
            client.gameMode.stopDestroyBlock();
            return true;
        }
        return false;
    }

    /**
     * 催熟
     */
    private void boneMealOne(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
        if (!client.player.getMainHandItem().is(Items.BONE_MEAL)) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            if (!isGrowTarget(state)) {
                continue;
            }
            BlockPos target = pos.immutable();
            Vec3 hitVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            InteractionResult result = client.gameMode.useItemOn(
                    client.player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(hitVec, Direction.UP, target, false)
            );
            if (result.consumesAction()) {
                return;
            }
        }
    }

    private static boolean isGrowTarget(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.NETHER_WART)) {
            return !isFullyGrown(state);
        }
        if (state.is(Blocks.CAVE_VINES_PLANT)) {
            return !state.getValue(BlockStateProperties.BERRIES);
        }
        return false;
    }

    private void swapToOffhand(int srcSlot) {
        ContainerActions.leftClickSlot(srcSlot);
        ContainerActions.leftClickSlot(Constants.OFFHAND);
        ContainerActions.leftClickSlot(srcSlot);
    }
}
