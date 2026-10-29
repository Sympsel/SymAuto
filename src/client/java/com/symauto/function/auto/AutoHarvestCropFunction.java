package com.symauto.function.auto;

import com.symauto.config.option.BooleanOption;
import com.symauto.config.option.IntOption;
import com.symauto.core.Constants;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.container.depend.BundleAccess;
import com.symauto.function.utils.BundleContentsHelper;
import com.symauto.function.utils.ContainerActions;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
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

    @Getter
    @Setter
    private int maxActionsPerTick = 32;

    private int boneMealSlot = -1;

    // 两次背包整理动作之间的最小间隔
    private static final int REFILL_INTERVAL_TICKS = 3;

    // ContainerInput.SWAP 中代表“与副手交换”的按钮代码
    private static final int OFFHAND_SWAP_BUTTON = 40;

    private int refillCooldown;

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
        addOption(new IntOption("max_actions_per_tick", "每 tick 最大交互数",
                this::getMaxActionsPerTick, this::setMaxActionsPerTick, 1, 128, 1));
        addOption(new IntOption("refill_min_count", "补充阈值（低于该数量时补充）",
                this::getRefillMinCount, this::setRefillMinCount, 1, 64, 1));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .keyValueLine("自动补充种子和骨粉", autoRefill ? "是" : "否")
                .keyValueLine("自动丢出非种子产物", enableDiscardResultIfNotSeed ? "是" : "否")
                .keyValueLine("仅使用骨粉催熟作物", onlyRipeningCrop ? "是" : "否")
                .keyValueLine("每 tick 最大交互数", String.valueOf(maxActionsPerTick))
                .keyValueLine("补充阈值", String.valueOf(refillMinCount))
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        // 不打断自动进食
        if (AutoEatFunction.INSTANCE.isEating()) {
            return;
        }

        // 先补货再干活：保证这一 tick 的批量交互手里有足够的种子/骨粉
        if (autoRefill) {
            ensureSupplies(client);
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
            // harvest 内部已经包含“破块 → 同 tick 补种 → 右键采摘”的批量流程
            harvest(client, eye, reachSq, r, center);
            // 兜底补种：把范围内“空着的耕地/灵魂沙”补上
            replant(client, eye, reachSq, r, center);
        }
        // 催熟放在最后：这样刚补种的作物也能在同一 tick 吃到骨粉
        boneMeal(client, eye, reachSq, r, center);
    }

    /**
     * 自动丢出非种子产物
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

    /**
     * 补给维护入口
     */
    private void ensureSupplies(Minecraft client) {
        if (client.player.isCreative()) {
            return;
        }
        if (client.player.containerMenu != client.player.inventoryMenu) {
            return;
        }
        AbstractContainerMenu menu = client.player.inventoryMenu;
        if (!menu.getCarried().isEmpty()) {
            if (stashCarried(client, menu.getCarried())) {
                refillCooldown = REFILL_INTERVAL_TICKS;
            }
            return;
        }
        if (refillCooldown > 0) {
            --refillCooldown;
            return;
        }
        if (refillHandInUse(client)) {
            refillCooldown = REFILL_INTERVAL_TICKS;
        }
    }

    /**
     * 只补当前真正在用的那只手
     *
     * @return 是否执行了一次背包动作
     */
    private boolean refillHandInUse(Minecraft client) {
        Inventory inv = client.player.getInventory();
        ItemStack main = client.player.getMainHandItem();
        ItemStack off = client.player.getOffhandItem();
        int selected = inv.getSelectedSlot();

        if (main.is(Items.BONE_MEAL)) {
            boneMealSlot = selected;
        }
        boolean boneMealWanted = main.is(Items.BONE_MEAL)
                || (main.isEmpty() && (boneMealSlot == selected || isPlantable(off)))
                || onlyRipeningCrop;
        boolean boneMealUseful = isPlantable(off) || onlyRipeningCrop;
        int currentBoneMeal = main.is(Items.BONE_MEAL) ? main.getCount() : 0;
        if (boneMealWanted && boneMealUseful
                && (!main.is(Items.BONE_MEAL) || currentBoneMeal < refillMinCount)
                && refillSelectedHotbar(client, Items.BONE_MEAL, currentBoneMeal)) {
            boneMealSlot = inv.getSelectedSlot();
            return true;
        }

        if (onlyRipeningCrop) {
            return false;
        }

        InteractionHand hand = isPlantable(main) ? InteractionHand.MAIN_HAND
                : isPlantable(off) ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            // 两只手都没有可种植物：从背包里找一种放进副手
            Item plant = firstPlantableInInventory(client);
            return plant != null && refillOffhand(client, plant, 0);
        }

        ItemStack stack = hand == InteractionHand.MAIN_HAND ? main : off;
        if (stack.getCount() >= refillMinCount) {
            return false;
        }
        return hand == InteractionHand.MAIN_HAND
                ? refillSelectedHotbar(client, stack.getItem(), stack.getCount())
                : refillOffhand(client, stack.getItem(), stack.getCount());
    }

    /**
     * 把同种物品换到当前选中的快捷栏槽
     */
    private boolean refillSelectedHotbar(Minecraft client, Item item, int currentCount) {
        Inventory inv = client.player.getInventory();
        int best = findBestStack(client, item, inv.getSelectedSlot(), currentCount);
        if (best < 0) {
            return extractFromBundle(client, item);
        }
        if (best < Constants.HOTBAR_END) {
            inv.setSelectedSlot(best);
            return true;
        }
        int menuSlot = invToMenuSlot(best);
        if (menuSlot < 0) {
            return false;
        }
        ContainerActions.swapToHotbar(menuSlot, inv.getSelectedSlot());
        return true;
    }

    /**
     * 补副手
     */
    private boolean refillOffhand(Minecraft client, Item item, int currentCount) {
        int best = findBestStack(client, item, Constants.OFFHAND, currentCount);
        if (best < 0) {
            return extractFromBundle(client, item);
        }
        int menuSlot = invToMenuSlot(best);
        if (menuSlot < 0) {
            return false;
        }
        ContainerActions.click(menuSlot, OFFHAND_SWAP_BUTTON, ContainerInput.SWAP);
        return true;
    }

    /**
     * 在快捷栏和背包里找同种物品数量最多的槽位
     */
    private int findBestStack(Minecraft client, Item item, int excludeInvIndex, int currentCount) {
        Inventory inv = client.player.getInventory();
        int best = -1;
        int bestCount = currentCount;
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            if (i == excludeInvIndex) {
                continue;
            }
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            if (stack.getCount() > bestCount) {
                best = i;
                bestCount = stack.getCount();
            }
        }
        return best;
    }

    /**
     * 背包里没有现成堆时从收纳袋里取一组到背包
     */
    private boolean extractFromBundle(Minecraft client, Item item) {
        Inventory inv = client.player.getInventory();
        AbstractContainerMenu menu = client.player.inventoryMenu;
        ItemStack probe = new ItemStack(item);
        for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
            Slot slot = menu.getSlot(slotId);
            if (slot.container != inv) {
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
            int index = 0;
            for (ItemStack s : contents.itemCopyStream().toList()) {
                if (ItemStack.isSameItemSameComponents(s, probe)) {
                    BundleAccess.extractToInventory(slotId, index, probe);
                    return true;
                }
                index++;
            }
        }
        return false;
    }

    /**
     * 把光标残留物品放回背包：优先并入同类堆，其次任意空槽
     *
     * @return 是否发出了点击
     */
    private boolean stashCarried(Minecraft client, ItemStack carried) {
        Inventory inv = client.player.getInventory();
        int empty = -1;
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                if (empty < 0) {
                    empty = i;
                }
            } else if (ItemStack.isSameItemSameComponents(stack, carried)
                    && stack.getCount() < stack.getMaxStackSize()) {
                ContainerActions.leftClickSlot(invToMenuSlot(i));
                return true;
            }
        }
        if (empty >= 0) {
            ContainerActions.leftClickSlot(invToMenuSlot(empty));
        } else {
            ContainerActions.dropCarried();
        }
        return true;
    }

    /**
     * 找到第一个可种植的物品
     */
    private Item firstPlantableInInventory(Minecraft client) {
        Inventory inv = client.player.getInventory();
        for (int i = Constants.HOTBAR_START; i < Constants.INV_END; i++) {
            Item item = inv.getItem(i).getItem();
            if (isPlantableItem(item)) {
                return item;
            }
        }
        for (Slot slot : client.player.inventoryMenu.slots) {
            if (slot.container != inv) {
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
                if (isPlantableItem(s.getItem())) {
                    return s.getItem();
                }
            }
        }
        return null;
    }

    /**
     * 背包索引映射到玩家背包菜单槽号
     */
    private static int invToMenuSlot(int invIndex) {
        if (invIndex < Constants.HOTBAR_START || invIndex >= Constants.INV_END) {
            return -1;
        }
        return invIndex < Constants.HOTBAR_END
                ? Constants.HOTBAR_START_IN_INVENTORY + invIndex
                : invIndex;
    }

    private boolean harvest(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
        List<BlockPos> breakTargets = new ArrayList<>(); // 破坏目标
        List<BlockPos> pickTargets = new ArrayList<>(); // 采摘目标
        List<BlockPos> replantTiles = new ArrayList<>(); // 支撑方块

        collectTargets(client, eye, reachSq, r, center, breakTargets, pickTargets, replantTiles);

        int budget = maxActionsPerTick;

        for (BlockPos pos : breakTargets) {
            if (budget-- <= 0) {
                break;
            }
            client.gameMode.startDestroyBlock(pos, Direction.UP);
        }

        for (BlockPos support : replantTiles) {
            if (budget-- <= 0) {
                break;
            }
            plantAt(client, support);
        }

        for (BlockPos pos : pickTargets) {
            if (budget-- <= 0) {
                break;
            }
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false
                    ));
        }

        return false;
    }

    private void collectTargets(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center,
                                List<BlockPos> breakTargets, List<BlockPos> pickTargets, List<BlockPos> replantTiles) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            BlockPos target = pos.immutable();

            if (state.hasProperty(BlockStateProperties.BERRIES)
                    && state.getValue(BlockStateProperties.BERRIES)) {
                pickTargets.add(target);
                continue;
            }
            if (state.is(Blocks.SWEET_BERRY_BUSH)) {
                if (isFullyGrown(state)) {
                    pickTargets.add(target);
                }
                continue;
            }

            // 甘蔗：只收第二层及以上
            if (state.is(Blocks.SUGAR_CANE)) {
                if (client.level.getBlockState(target.below()).is(Blocks.SUGAR_CANE)) {
                    breakTargets.add(target);
                }
                continue;
            }
//
            // 地狱疣
            if (state.is(Blocks.NETHER_WART)) {
                if (isFullyGrown(state)) {
                    breakTargets.add(target);
                    replantTiles.add(target.below());
                }
                continue;
            }

            // 通用作物
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                breakTargets.add(target);
                replantTiles.add(target.below());
            }
        }
    }

    private boolean plantAt(Minecraft client, BlockPos support) {
        BlockPos cropPos = support.above();
        if (!client.level.getBlockState(cropPos).isAir()) {
            return false;
        }
        InteractionHand hand = resolvePlantHand(client);
        if (hand == null) {
            return false;
        }
        ItemStack stack = hand == InteractionHand.MAIN_HAND
                ? client.player.getMainHandItem() : client.player.getOffhandItem();
        if (stack.getCount() <= 0) {
            return false;
        }
        Block surface = stack.is(Items.NETHER_WART) ? Blocks.SOUL_SAND : Blocks.FARMLAND;
        if (!client.level.getBlockState(support).is(surface)) {
            return false;
        }
        Vec3 hit = new Vec3(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
        return client.gameMode.useItemOn(client.player, hand,
                new BlockHitResult(hit, Direction.UP, support, false)).consumesAction();
    }

    /**
     * 优先取可种植物，无可种植物返回 null
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

    /**
     * 作物的年龄属性，找不到返回 null
     */
    private static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty ip
                    && (p.getName().equals("age")
                    || p.getName().equals("stage")
                    || p.getName().equals("growth"))) {
                return ip;
            }
        }
        return null;
    }

    private static int maxAge(IntegerProperty age) {
        int max = Integer.MIN_VALUE;
        for (Integer v : age.getPossibleValues()) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private static boolean isFullyGrown(BlockState state) {
        IntegerProperty age = ageProperty(state);
        return age != null && state.getValue(age) >= maxAge(age);
    }

    /**
     * 成熟度，用于骨粉催熟的优先级
     */
    private static float growthFraction(BlockState state) {
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return 0.0F;
        }
        int max = maxAge(age);
        return max <= 0 ? 0.0F : (float) state.getValue(age) / max;
    }

    private boolean replant(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
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

    private static boolean isPlantableItem(Item item) {
        return PLANTABLE_ON_FARMLAND.contains(item)
                || item == Items.NETHER_WART
                || item == Items.SUGAR_CANE;
    }

    private static boolean isPlantable(ItemStack stack) {
        return !stack.isEmpty() && isPlantableItem(stack.getItem());
    }

    private void boneMeal(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center) {
        if (!client.player.getMainHandItem().is(Items.BONE_MEAL)) {
            return;
        }
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            BlockState state = client.level.getBlockState(pos);
            if (isGrowTarget(client.level, pos, state)) {
                targets.add(pos.immutable());
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        targets.sort((a, b) -> Float.compare(
                growthFraction(client.level.getBlockState(b)),
                growthFraction(client.level.getBlockState(a))));

        int budget = Math.min(maxActionsPerTick, client.player.getMainHandItem().getCount());
        for (BlockPos pos : targets) {
            if (budget-- <= 0) {
                break;
            }
            if (!client.player.getMainHandItem().is(Items.BONE_MEAL)) {
                break;
            }
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        }
    }

    /**
     * 是否值得用骨粉催熟
     * 避免误催树苗/草/蘑菇
     */
    private static boolean isGrowTarget(LevelReader level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof CropBlock)
                && !(block instanceof SweetBerryBushBlock)
                && !(block instanceof CaveVinesPlantBlock)) {
            return false;
        }
        BonemealableBlock bonemealable = (BonemealableBlock) block;
        return bonemealable.isValidBonemealTarget(level, pos, state);
    }
}
