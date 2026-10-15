package com.symauto.function.auto;

import com.mojang.logging.LogUtils;
import com.symauto.storage.CraftingPresetStore;
import com.symauto.model.crafting.CraftingPreset;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.gui.screen.CraftingPresetsManagerScreen;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.slf4j.Logger;

import java.util.*;

public class AutoCraftingFunction extends SymAbstractFunction {
    public static final AutoCraftingFunction INSTANCE = new AutoCraftingFunction();

    private static final int RESULT_SLOT = 0;
    private static final int GRID_FIRST_SLOT = 1;
    private static final int GRID_SIZE = 9;
    private static final int STACK_MAX = 64;
    // 合成后等待服务器回包同步背包的 tick 数
    private static final int SETTLE_TICKS = 2;

    private static final int WAIT_TICKS = 10;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = true;

    private static void debug(String message, Object... args) {
        if (DEBUG) {
            LOGGER.info("[AutoCrafting] " + message, args);
        }
    }

    private static String describeRecipe(CraftingRecipe recipe) {
        if (recipe == null) {
            return "null";
        }
        ItemStack output = recipe.assemble(CraftingInput.EMPTY);
        return recipe.getClass().getSimpleName() + " -> " + output.getItem() + " x" + output.getCount();
    }

    private enum State {
        IDLE, // 待初始化一轮计数
        CHECK, // 取配方并合成
    }

    /**
     *
     * STAY 保持当前预设
     * ADVANCE 让位到下一个
     */
    private enum Decision {
        STAY,
        ADVANCE,
    }

    private enum MaterialCheck {
        READY,
        WAITING,
        UNAVAILABLE,
    }

    private final List<CraftingPreset> presets;

    // 每个预设独立状态机；以预设实例为键。
    private final Map<CraftingPreset, TaskState> tasks = new IdentityHashMap<>();

    /**
     * 单个自动合成预设的私有状态机
     */
    private static final class TaskState {
        State state = State.IDLE;
        int cooldown = 0;
        int remainingOps = 0;
        // 该预设是否已做完
        boolean completed = false;
        // 等待递归补料结果同步回背包
        Ingredient pendingIngredient = null;
        int pendingBefore = 0;
        int pendingWait = 0;
    }

    // 调度游标：指向独占处理的预设下标
    private int cursor = 0;
    // 上次同步的预设版本
    private long loadedRevision = -1;

    private AutoCraftingFunction() {
        super("auto_crafting", "自动合成",
                Tooltip.create()
                        .line("打开工作台自动线性轮询执行预设管理器里的合成配方，全自动输入输出，输出可接漏斗矿车")
                        .line(ChatFormatting.YELLOW, "支持配置是否递归合成原材料")
                        .line(ChatFormatting.YELLOW, "支持配置是否丢出产物")
                        .line(ChatFormatting.YELLOW, "支持配置合成次数")
                        .toString()
        );
        presets = new ArrayList<>();
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new CraftingPresetsManagerScreen(parent);
    }

    /** 预设存储变化时重建运行时列表并重置状态（编辑预设即从头开始，符合预期） */
    private void syncPresetsFromStore() {
        long rev = CraftingPresetStore.getRevision();
        if (rev == loadedRevision) {
            return;
        }
        loadedRevision = rev;
        presets.clear();
        presets.addAll(CraftingPresetStore.toAssumptions());
        tasks.clear();
        cursor = 0;
    }


    @Override
    protected void onTrigger(Minecraft client) {
        syncPresetsFromStore();
        Player player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        // 工作台未打开或光标占用：整体等待，不推进
        if (!(player.containerMenu instanceof CraftingMenu)) {
            return;
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            return;
        }

        int n = presets.size();
        if (n == 0) {
            return;
        }

        // 从游标起，找到第一个未完成且 active 的预设作为当前独占任务
        int idx = -1;
        CraftingPreset current = null;
        TaskState task = null;
        for (int i = 0; i < n; i++) {
            int k = Math.floorMod(cursor + i, n);
            CraftingPreset cand = presets.get(k);
            if (!cand.isActive()) {
                continue;
            }
            TaskState ct = taskFor(cand);
            if (ct.completed) {
                continue;
            }
            idx = k;
            current = cand;
            task = ct;
            break;
        }
        // 所有预设都已完成
        if (idx < 0) {
            return;
        }

        Decision d = advance(current, task, player, client, idx);
        cursor = (d == Decision.ADVANCE) ? idx + 1 : idx;
    }

    /**
     * 推进单个预设状态机的一步，返回本 tick 结束后游标是否应让位
     */
    private Decision advance(CraftingPreset assumption, TaskState task,
                             Player player, Minecraft client, int storeIndex) {
        if (task.completed) {
            return Decision.ADVANCE;
        }
        if (task.cooldown > 0) {
            --task.cooldown;
            return Decision.STAY;
        }
        if (task.state == State.IDLE) {
            task.remainingOps = assumption.getExecuteCount();
            task.state = State.CHECK;
            return Decision.STAY;
        }
        // CHECK
        if (task.remainingOps != CraftingPreset.UNTIL_EXHAUSTED && task.remainingOps <= 0) {
            task.completed = true;
            return Decision.ADVANCE;
        }
        CraftingRecipe recipe = pickRecipe(assumption, player);
        if (recipe == null) {
            clearGrid(client, player.containerMenu);
            task.cooldown = WAIT_TICKS;
            // 暂时做不了就让位，退避后再试
            return Decision.ADVANCE;
        }
        return doCraftStep(assumption, task, recipe, player, client, storeIndex);
    }

    /**
     * 一次合成尝试：优先原料槽堆叠批量合成，否则回退逐件（含递归中间产物）
     * 成功则继续盯当前预设；缺料/失败就让位重试
     */
    private Decision doCraftStep(CraftingPreset assumption, TaskState task, CraftingRecipe recipe,
                                 Player player, Minecraft client, int storeIndex) {
        // 以当前预设产物为根重建本链保留集，禁止靠“拆自己/拆祖先”凑料
        int targetOps = (task.remainingOps == CraftingPreset.UNTIL_EXHAUSTED)
                ? Integer.MAX_VALUE : task.remainingOps;
        int crafted = tryBatchCraft(assumption, recipe, player, client, targetOps);
        debug("批量合成尝试: 目标={}, 配方={}, 实际产出={}",
                assumption.getResult().getItem(), describeRecipe(recipe), crafted);
        if (crafted > 0) {
            return finishBatch(task, crafted, storeIndex);
        }

        // 回退：需要中间产物或库存不足以堆叠时，逐件合成
        MaterialCheck materials = ensureMaterials(assumption, recipe, player, client,
                Set.of(assumption.getResult().getItem()), task);
        if (materials == MaterialCheck.WAITING) {
            debug("等待递归补料同步: 目标={}", assumption.getResult().getItem());
            // 已发起一次分解/中间产物合成，等待服务器把新物品同步进背包
            task.cooldown = SETTLE_TICKS;
            return Decision.STAY;
        }
        if (materials == MaterialCheck.UNAVAILABLE) {
            debug("递归补料不可用: 目标={}, 配方={}",
                    assumption.getResult().getItem(), describeRecipe(recipe));
            clearGrid(client, player.containerMenu);
            task.cooldown = WAIT_TICKS;
            return Decision.ADVANCE;
        }
        if (!doCraft(assumption, recipe, player, client)) {
            debug("根配方点击合成失败: 目标={}, 配方={}",
                    assumption.getResult().getItem(), describeRecipe(recipe));
            clearGrid(client, player.containerMenu);
            task.cooldown = WAIT_TICKS;
            return Decision.ADVANCE;
        }
        return finishBatch(task, 1, storeIndex);
    }

    private boolean canFillGrid(Inventory inventory, CraftingRecipe recipe) {
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        int[] required = requiredCounts(recipe);
        for (int j = 0; j < ingredients.size(); j++) {
            Ingredient ingredient = ingredients.get(j);
            if (ingredient.isEmpty() || hasSameIngredientBefore(ingredients, j)) {
                continue;
            }
            int totalRequired = totalRequiredFor(ingredients, required, j);
            int available = countIngredient(inventory, ingredient);
            debug("网格总量预检查: 配方={}, 原料索引={}, 需要={}, 现有={}",
                    describeRecipe(recipe), j, totalRequired, available);
            if (available < totalRequired) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSameIngredientBefore(List<Ingredient> ingredients, int index) {
        Ingredient target = ingredients.get(index);
        for (int i = 0; i < index; i++) {
            if (sameIngredient(ingredients.get(i), target)) {
                return true;
            }
        }
        return false;
    }

    private int totalRequiredFor(List<Ingredient> ingredients, int[] required, int index) {
        Ingredient target = ingredients.get(index);
        int total = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            if (sameIngredient(ingredients.get(i), target)) {
                total += required[i];
            }
        }
        return total;
    }

    private boolean sameIngredient(Ingredient a, Ingredient b) {
        return a == b || a.equals(b);
    }

    /**
     * 结算本次（批量或单件）产出对 executeCount 的影响
     */
    private Decision finishBatch(TaskState task, int crafted, int storeIndex) {
        if (task.remainingOps != CraftingPreset.UNTIL_EXHAUSTED) {
            task.remainingOps -= crafted;
            if (task.remainingOps <= 0) {
                task.remainingOps = 0;
                task.completed = true;
                CraftingPresetStore.updateRemaining(storeIndex, 0);
                return Decision.ADVANCE;
            }
            CraftingPresetStore.updateRemaining(storeIndex, task.remainingOps);
        }
        task.cooldown = SETTLE_TICKS;
        return Decision.STAY;
    }

    /**
     * 仅在“所有原料都已直接存在于背包”时启用，需要中间产物时返回 0 交由逐件回退
     * 每个网格位分配背包里一整叠匹配原料
     *
     * @return 实际合成的件数；0 表示本次不适合批量
     */
    private int tryBatchCraft(CraftingPreset assumption, CraftingRecipe recipe,
                              Player player, Minecraft client, int targetOps) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu)) {
            return 0;
        }
        int containerId = menu.containerId;
        Inventory inventory = player.getInventory();

        PlacementInfo placement = recipe.placementInfo();
        List<Ingredient> ingredients = placement.ingredients();
        IntList slotsToIngredientIndex = placement.slotsToIngredientIndex();
        int width = (recipe instanceof ShapedRecipe shaped) ? shaped.getWidth() : 3;

        // 预分配：为每个网格位各分配背包里“一整叠”匹配原料
        List<int[]> plan = new ArrayList<>(); // {gridSlot, srcSlot, count}
        Set<Integer> used = new HashSet<>();
        int placementSize = slotsToIngredientIndex.isEmpty() ? ingredients.size() : slotsToIngredientIndex.size();
        for (int p = 0; p < placementSize; p++) {
            int ingredientIndex = slotsToIngredientIndex.isEmpty()
                    ? p
                    : slotsToIngredientIndex.getInt(p);
            if (ingredientIndex == PlacementInfo.EMPTY_SLOT) {
                continue;
            }
            Ingredient ingredient = ingredients.get(ingredientIndex);
            int row = p / width;
            int col = p % width;
            int gridSlot = GRID_FIRST_SLOT + row * 3 + col;
            if (gridSlot >= GRID_FIRST_SLOT + GRID_SIZE) {
                return 0;
            }
            int src = findUnusedInventorySlot(menu, inventory, ingredient, used);
            if (src < 0) {
                debug("批量规划失败: 配方={}, 网格槽={}, 缺少可用材料叠={}",
                        describeRecipe(recipe), gridSlot, ingredient);
                // 该原料的可用叠数不足以铺满所有网格位，改为逐件回退
                return 0;
            }
            used.add(src);
            plan.add(new int[]{gridSlot, src, menu.getSlot(src).getItem().getCount()});
        }
        if (plan.isEmpty()) {
            return 0;
        }

        int batch = targetOps;
        for (int[] e : plan) {
            batch = Math.min(batch, e[2]);
        }
        batch = Math.min(batch, STACK_MAX);
        if (batch <= 0) {
            return 0;
        }

        clearGrid(client, menu);
        // 整叠左键放入空槽
        for (int[] e : plan) {
            int gridSlot = e[0];
            int src = e[1];
            // 取整叠到光标
            click(client, containerId, src, 0, ContainerInput.PICKUP);
            // 空槽整叠放入，光标清空
            click(client, containerId, gridSlot, 0, ContainerInput.PICKUP);
        }

        // 一次性合成 batch 件
        if (assumption.isThrowResult()) {
            for (int i = 0; i < batch; i++) {
                // 丢出
                click(client, containerId, RESULT_SLOT, 0, ContainerInput.THROW);
            }
        } else {
            // 一次全部合成进背包
            click(client, containerId, RESULT_SLOT, 0, ContainerInput.QUICK_MOVE);
        }

        // 各叠数量可能大于 batch，清掉多出的余料退回背包
        clearGrid(client, menu);
        return batch;
    }

    /**
     * 在背包中找一个匹配原料、本次尚未被占用、且非空的槽位
     */
    private int findUnusedInventorySlot(AbstractContainerMenu menu, Inventory inventory,
                                        Ingredient ingredient, Set<Integer> used) {
        for (Slot slot : menu.slots) {
            if (slot.container != inventory || used.contains(slot.index)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return slot.index;
            }
        }
        return -1;
    }

    /**
     * 获取/创建某预设的私有状态机
     */
    private TaskState taskFor(CraftingPreset assumption) {
        return tasks.computeIfAbsent(assumption, k -> new TaskState());
    }

    private CraftingRecipe pickRecipe(CraftingPreset assumption, Player player) {
        CraftingRecipe craftable = assumption.chooseRecipe(player, 1);
        if (craftable != null) {
            debug("选择可直接合成配方: 目标={}, 配方={}",
                    assumption.getResult().getItem(), describeRecipe(craftable));
            return craftable;
        }
        debug("没有可直接合成配方: 目标={}, 递归={}",
                assumption.getResult().getItem(), assumption.isRecursiveSearchMaterial());
        if (!assumption.isRecursiveSearchMaterial()) {
            return null;
        }
        CraftingRecipe fallback = assumption.getRecipes().stream()
                .max(Comparator.comparingInt(r -> r.assemble(CraftingInput.EMPTY).getCount()))
                .orElse(null);
        debug("递归兜底根配方: 目标={}, 配方={}",
                assumption.getResult().getItem(), describeRecipe(fallback));
        return fallback;
    }

    /**
     * 统计背包中匹配该原料的物品总数
     */
    private int countIngredient(Inventory inventory, Ingredient ingredient) {
        int total = 0;
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 统计本配方网格对每种原料（按原料索引）实际需要占用的槽位数，即单件所需数量
     */
    private int[] requiredCounts(CraftingRecipe recipe) {
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        IntList slots = recipe.placementInfo().slotsToIngredientIndex();
        int[] counts = new int[ingredients.size()];
        if (slots.isEmpty()) {
            // 无序配方可能没有网格映射，按完整材料列表计数（如铁块 -> 铁锭）
            for (int i = 0; i < ingredients.size(); i++) {
                if (!ingredients.get(i).isEmpty()) {
                    counts[i]++;
                }
            }
            return counts;
        }
        for (int p = 0; p < slots.size(); p++) {
            int idx = slots.getInt(p);
            if (idx != PlacementInfo.EMPTY_SLOT) {
                counts[idx]++;
            }
        }
        return counts;
    }

    private int findSlotForIngredient(AbstractContainerMenu menu, Inventory inventory, Ingredient ingredient) {
        for (Slot slot : menu.slots) {
            if (slot.container == inventory) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    return slot.index;
                }
            }
        }
        return -1;
    }

    private void click(Minecraft client, int containerId, int slot, int button, ContainerInput type) {
        if (client.gameMode == null || client.player == null) {
            return;
        }
        client.gameMode.handleContainerInput(containerId, slot, button, type, client.player);
    }

    /**
     * 取一个能满足该 Ingredient 的具体物品，用于递归定位其配方
     */
    private Item representativeItem(Ingredient ingredient) {
        return ingredient.items()
                .map(Holder::value)
                .findFirst()
                .orElse(null);
    }

    private boolean recipeUsesReserved(CraftingRecipe recipe, Set<Item> reserved) {
        if (reserved.isEmpty()) {
            return false;
        }
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (ingredient.items().map(Holder::value).anyMatch(reserved::contains)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查递归补料是否已经同步进背包。
     * 合成铁块 -> 铁锭这类操作需要等待服务器回包，不能在同一 tick 内立刻重试父配方。
     */
    private MaterialCheck pendingMaterialStatus(TaskState task, Inventory inventory) {
        if (task.pendingIngredient == null) {
            return MaterialCheck.READY;
        }
        int now = countIngredient(inventory, task.pendingIngredient);
        if (now > task.pendingBefore) {
            debug("补料已同步: 材料={}, 原数量={}, 当前数量={}",
                    task.pendingIngredient, task.pendingBefore, now);
            task.pendingIngredient = null;
            task.pendingBefore = 0;
            task.pendingWait = 0;
            return MaterialCheck.READY;
        }
        if (--task.pendingWait <= 0) {
            debug("补料同步超时: 材料={}, 原数量={}, 当前数量={}",
                    task.pendingIngredient, task.pendingBefore, now);
            // 超时后放弃本次补料状态，进入正常退避；下一轮仍可重新尝试
            task.pendingIngredient = null;
            task.pendingBefore = 0;
            return MaterialCheck.UNAVAILABLE;
        }
        return MaterialCheck.WAITING;
    }

    private void waitForMaterial(TaskState task, Ingredient ingredient, int before) {
        debug("记录待同步补料: 材料={}, 同步前数量={}", ingredient, before);
        task.pendingIngredient = ingredient;
        task.pendingBefore = before;
        task.pendingWait = WAIT_TICKS;
    }

    private MaterialCheck ensureMaterials(CraftingPreset assumption, CraftingRecipe recipe,
                                          Player player, Minecraft client, Set<Item> path,
                                          TaskState task) {
        Inventory inventory = player.getInventory();
        MaterialCheck pending = pendingMaterialStatus(task, inventory);
        if (pending != MaterialCheck.READY) {
            return pending;
        }

        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        int[] required = requiredCounts(recipe);
        for (int j = 0; j < ingredients.size(); j++) {
            Ingredient ingredient = ingredients.get(j);
            if (ingredient.isEmpty() || hasSameIngredientBefore(ingredients, j)) {
                continue;
            }
            int need = totalRequiredFor(ingredients, required, j);
            int have = countIngredient(inventory, ingredient);
            debug("材料检查: 目标={}, 原料索引={}, 需要={}, 现有={}",
                    recipe.assemble(CraftingInput.EMPTY).getItem(), j, need, have);
            if (have >= need) {
                continue;
            }
            if (!assumption.isRecursiveSearchMaterial()) {
                return MaterialCheck.UNAVAILABLE;
            }
            Item representative = representativeItem(ingredient);
            if (representative == null || path.contains(representative)) {
                // 无法定位代表物品，或检测到“自身/祖先”成环
                return MaterialCheck.UNAVAILABLE;
            }
            CraftingPreset sub = new CraftingPreset(
                    new ItemStack(representative), 1, true, false, true);
            // 拆解配方不得动用本链上的任何目标（含要产出的代表物品）。
            // 必须先过滤再选择，避免选中被禁止的配方后直接放弃铁块分解等可用配方。
            Set<Item> subPath = new HashSet<>(path);
            subPath.add(representative);
            List<CraftingRecipe> subCandidates = sub.getRecipes();
            debug("递归目标: {}, 找到子配方 {} 个: {}",
                    representative,
                    subCandidates.size(),
                    subCandidates.stream().map(AutoCraftingFunction::describeRecipe).toList());
            CraftingRecipe subRecipe = subCandidates.stream()
                    .filter(candidate -> {
                        boolean reserved = recipeUsesReserved(candidate, subPath);
                        if (reserved) {
                            debug("排除会使用保留材料的子配方: {}", describeRecipe(candidate));
                        }
                        return !reserved;
                    })
                    .filter(candidate -> {
                        boolean hasMaterials = CraftingPreset.hasIngredientsFor(candidate, player, 1);
                        if (!hasMaterials) {
                            debug("排除材料不足的子配方: {}", describeRecipe(candidate));
                        }
                        return hasMaterials;
                    })
                    .max(Comparator.comparingInt(candidate ->
                            candidate.assemble(CraftingInput.EMPTY).getCount()))
                    .orElse(null);
            debug("选中子配方: {}", describeRecipe(subRecipe));
            if (subRecipe == null) {
                return MaterialCheck.UNAVAILABLE;
            }
            MaterialCheck subMaterials = ensureMaterials(sub, subRecipe, player, client, subPath, task);
            debug("子配方材料检查结果: 子配方={}, 结果={}", describeRecipe(subRecipe), subMaterials);
            if (subMaterials != MaterialCheck.READY) {
                return subMaterials;
            }
            boolean craftedSub = doCraft(sub, subRecipe, player, client);
            debug("执行子配方: 子配方={}, 点击结果={}", describeRecipe(subRecipe), craftedSub);
            if (!craftedSub) {
                return MaterialCheck.UNAVAILABLE;
            }
            // 本次只补一层料并等待同步；同步完成后再判断父配方，避免同一 tick 内空转和闪烁
            waitForMaterial(task, ingredient, have);
            return MaterialCheck.WAITING;
        }
        return MaterialCheck.READY;
    }

    /**
     * 逐件合成：依据配方网格把材料各放 1 个进 3x3 工作台并产出一次（用于需要中间产物的回退路径）
     *
     * @return 是否成功产出一次
     */
    private boolean doCraft(CraftingPreset assumption, CraftingRecipe recipe,
                            Player player, Minecraft client) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu)) {
            return false;
        }
        // 凑不满网格就一次都不点
        if (!canFillGrid(player.getInventory(), recipe)) {
            debug("doCraft 预检查失败: 配方={}", describeRecipe(recipe));
            return false;
        }
        int containerId = menu.containerId;
        // 清理网格残留
        clearGrid(client, menu);
        PlacementInfo placement = recipe.placementInfo();
        List<Ingredient> ingredients = placement.ingredients();
        IntList slotsToIngredientIndex = placement.slotsToIngredientIndex();
        int width = (recipe instanceof ShapedRecipe shaped) ? shaped.getWidth() : 3;

        int placementSize = slotsToIngredientIndex.isEmpty() ? ingredients.size() : slotsToIngredientIndex.size();
        for (int p = 0; p < placementSize; p++) {
            int ingredientIndex = slotsToIngredientIndex.isEmpty()
                    ? p
                    : slotsToIngredientIndex.getInt(p);
            if (ingredientIndex == PlacementInfo.EMPTY_SLOT) {
                continue; // 空槽
            }
            Ingredient ingredient = ingredients.get(ingredientIndex);
            int row = p / width;
            int col = p % width;
            // 左上对齐到 3x3
            int gridSlot = GRID_FIRST_SLOT + row * 3 + col;
            if (gridSlot >= GRID_FIRST_SLOT + GRID_SIZE) {
                return false;
            }
            int src = findSlotForIngredient(menu, player.getInventory(), ingredient);
            debug("放置材料: 配方={}, 网格槽={}, 背包槽={}, 原料={}",
                    describeRecipe(recipe), gridSlot, src, ingredient);
            if (src < 0) {
                return false;
            }
            click(client, containerId, src, 0, ContainerInput.PICKUP); // 取整叠
            click(client, containerId, gridSlot, 1, ContainerInput.PICKUP); // 放 1 个
            click(client, containerId, src, 0, ContainerInput.PICKUP); // 剩余放回
        }
        if (assumption.isThrowResult()) {
            click(client, containerId, RESULT_SLOT, 0, ContainerInput.THROW); // 合成并丢出
        } else {
            click(client, containerId, RESULT_SLOT, 0, ContainerInput.QUICK_MOVE); // 合成入背包
        }
        return true;
    }

    /**
     * 清理工作台网格残留
     */
    private void clearGrid(Minecraft client, AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu) {
            int containerId = menu.containerId;
            for (int pos = 0; pos < GRID_SIZE; pos++) {
                int gridSlot = GRID_FIRST_SLOT + pos;
                if (menu.getSlot(gridSlot).hasItem()) {
                    click(client, containerId, gridSlot, 0, ContainerInput.QUICK_MOVE);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        tasks.clear();
        cursor = 0;
    }
}