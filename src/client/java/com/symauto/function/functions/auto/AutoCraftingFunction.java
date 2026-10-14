package com.symauto.function.functions.auto;

import com.symauto.entity.CraftingAssumption;
import com.symauto.function.abstracts.SymAbstractFunction;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.*;

public class AutoCraftingFunction extends SymAbstractFunction {
    public static final AutoCraftingFunction INSTANCE = new AutoCraftingFunction();

    private static final int RESULT_SLOT = 0;
    private static final int GRID_FIRST_SLOT = 1;
    private static final int GRID_SIZE = 9;
    private static final int STACK_MAX = 64;
    // 合成后等待服务器回包同步背包的 tick 数
    private static final int SETTLE_TICKS = 2;

    private enum State {
        IDLE,   // 待初始化一轮计数
        CHECK,  // 取配方并合成
    }

    // advance/doCraftStep 的调度反馈：STAY=继续盯当前预设；ADVANCE=让位到下一个
    private enum Decision {
        STAY,
        ADVANCE,
    }

    private final List<CraftingAssumption> assumptions;

    // 每个预设独立状态机；以预设实例为键。
    // 注意：若后续改为每 tick 从 CraftingPresetStore 重建实例，需换成稳定 id 作键，避免身份抖动。
    private final Map<CraftingAssumption, TaskState> tasks = new IdentityHashMap<>();

    /**
     * 单个自动合成预设的私有状态机
     */
    private static final class TaskState {
        State state = State.IDLE;
        int cooldown = 0;
        int remainingOps = 0;
        // 该预设是否已做完
        boolean completed = false;
        // 本链保留集：根目标 + 递归中间成品，防止“拆解自身/祖先再合成”
        final Set<Item> reserved = new HashSet<>();
    }

    // 调度游标：指向独占处理的预设下标
    private int cursor = 0;
    // 全局保留集：当前所有 active 且未完成的预设根目标
    private final Set<Item> activeTargets = new HashSet<>();

    private AutoCraftingFunction() {
        super("auto_crafting", "自动合成",
                "todo"
        );
        assumptions = new ArrayList<>();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (assumptions.isEmpty()) {
            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.PISTON), 1024, true, true, true
            ));
            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.DISPENSER), 1024, true, true, true
            ));
            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.IRON_BLOCK), 1024, true, true, true
            ));
        }
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

        int n = assumptions.size();
        if (n == 0) {
            return;
        }

        // 每 tick 重算所有 active 且未完成的预设根目标
        activeTargets.clear();
        for (CraftingAssumption a : assumptions) {
            if (a.isActive() && !taskFor(a).completed) {
                activeTargets.add(a.getResult().getItem());
            }
        }

        // 从游标起，找到第一个未完成且 active 的预设作为当前独占任务
        int idx = -1;
        CraftingAssumption current = null;
        TaskState task = null;
        for (int i = 0; i < n; i++) {
            int k = Math.floorMod(cursor + i, n);
            CraftingAssumption cand = assumptions.get(k);
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

        Decision d = advance(current, task, player, client);
        cursor = (d == Decision.ADVANCE) ? idx + 1 : idx;
    }

    /**
     * 推进单个预设状态机的一步，返回本 tick 结束后游标是否应让位
     */
    private Decision advance(CraftingAssumption assumption, TaskState task,
                             Player player, Minecraft client) {
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
        CraftingRecipe recipe = pickRecipe(assumption, player);
        if (recipe == null) {
            clearGrid(client, player.containerMenu);
            // 暂时做不了就让位，之后轮询再试
            return Decision.ADVANCE;
        }
        return doCraftStep(assumption, task, recipe, player, client);
    }

    /**
     * 一次合成尝试：优先原料槽堆叠批量合成，否则回退逐件（含递归中间产物）
     * 成功则继续盯当前预设；缺料/失败就让位重试。
     */
    private Decision doCraftStep(CraftingAssumption assumption, TaskState task, CraftingRecipe recipe,
                                 Player player, Minecraft client) {
        // 以当前预设产物为根重建本链保留集，禁止靠“拆自己/拆祖先”凑料
        task.reserved.clear();
        task.reserved.add(assumption.getResult().getItem());

        int targetOps = (task.remainingOps == CraftingAssumption.UNTIL_EXHAUSTED)
                ? Integer.MAX_VALUE : task.remainingOps;
        int crafted = tryBatchCraft(assumption, recipe, player, client, targetOps);
        if (crafted > 0) {
            return finishBatch(task, assumption, crafted);
        }

        // 回退：需要中间产物或库存不足以堆叠时，逐件合成
        if (!ensureMaterials(assumption, task, recipe, player, client)) {
            clearGrid(client, player.containerMenu);
            return Decision.ADVANCE;
        }
        if (!doCraft(assumption, recipe, player, client)) {
            clearGrid(client, player.containerMenu);
            return Decision.ADVANCE;
        }
        return finishBatch(task, assumption, 1);
    }

    /**
     * 结算本次（批量或单件）产出对 executeCount 的影响
     */
    private Decision finishBatch(TaskState task, CraftingAssumption assumption, int crafted) {
        if (task.remainingOps != CraftingAssumption.UNTIL_EXHAUSTED) {
            task.remainingOps -= crafted;
            if (task.remainingOps <= 0) {
                task.remainingOps = 0;
                task.completed = true;
                task.reserved.clear();
                return Decision.ADVANCE;
            }
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
    private int tryBatchCraft(CraftingAssumption assumption, CraftingRecipe recipe,
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
        for (int p = 0; p < slotsToIngredientIndex.size(); p++) {
            int ingredientIndex = slotsToIngredientIndex.getInt(p);
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
    private TaskState taskFor(CraftingAssumption assumption) {
        return tasks.computeIfAbsent(assumption, k -> new TaskState());
    }

    private CraftingRecipe pickRecipe(CraftingAssumption assumption, Player player) {
        CraftingRecipe craftable = assumption.chooseRecipe(player, 1);
        if (craftable != null) {
            return craftable;
        }
        if (!assumption.isRecursiveSearchMaterial()) {
            return null;
        }
        return assumption.getRecipes().stream()
                .max(Comparator.comparingInt(r -> r.assemble(CraftingInput.EMPTY).getCount()))
                .orElse(null);
    }

    private boolean hasIngredient(Player player, Ingredient ingredient) {
        for (ItemStack stack : player.getInventory()) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
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

    private boolean ensureMaterials(CraftingAssumption assumption, TaskState task, CraftingRecipe recipe,
                                    Player player, Minecraft client) {
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (hasIngredient(player, ingredient)) {
                continue;
            }
            // 如果原材料缺失且不允许递归搜索则返回 false
            if (!assumption.isRecursiveSearchMaterial()) {
                return false;
            }
            Item representative = representativeItem(ingredient);
            if (representative == null || task.reserved.contains(representative)) {
                // 无法定位代表物品或检测到环形配方
                return false;
            }

            CraftingAssumption sub = new CraftingAssumption(
                    new ItemStack(representative), 1, true, false, true);
            task.reserved.add(representative);
            CraftingRecipe subRecipe = sub.chooseRecipe(player, 1);
            if (subRecipe == null) {
                return false;
            }
            // 拒绝自我分解（本链）以及跨预设互逆分解（全局目标）
            if (recipeUsesReserved(subRecipe, task.reserved)
                    || recipeUsesReserved(subRecipe, activeTargets)) {
                return false;
            }
            boolean made = ensureMaterials(sub, task, subRecipe, player, client)
                    && doCraft(sub, subRecipe, player, client);
            if (!made) {
                return false;
            }
            if (!hasIngredient(player, ingredient)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 逐件合成：依据配方网格把材料各放 1 个进 3x3 工作台并产出一次（用于需要中间产物的回退路径）
     *
     * @return 是否成功产出一次
     */
    private boolean doCraft(CraftingAssumption assumption, CraftingRecipe recipe,
                            Player player, Minecraft client) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu)) {
            return false;
        }
        int containerId = menu.containerId;
        // 清理网格残留
        clearGrid(client, menu);
        PlacementInfo placement = recipe.placementInfo();
        List<Ingredient> ingredients = placement.ingredients();
        IntList slotsToIngredientIndex = placement.slotsToIngredientIndex();
        int width = (recipe instanceof ShapedRecipe shaped) ? shaped.getWidth() : 3;

        for (int p = 0; p < slotsToIngredientIndex.size(); p++) {
            int ingredientIndex = slotsToIngredientIndex.getInt(p);
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
            if (src < 0) {
                return false;
            }
            click(client, containerId, src, 0, ContainerInput.PICKUP);       // 取整叠
            click(client, containerId, gridSlot, 1, ContainerInput.PICKUP);  // 放 1 个
            click(client, containerId, src, 0, ContainerInput.PICKUP);       // 剩余放回
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
        activeTargets.clear();
    }
}