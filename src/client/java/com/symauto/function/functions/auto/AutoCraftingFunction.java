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

    private final Map<CraftingAssumption, TaskState> tasks = new IdentityHashMap<>();

    // 每个预设占有一个私有状态机
    private static final class TaskState {
        State state = State.IDLE;
        int cooldown = 0;
        int remainingOps = 0;
        // 该预设是否已把 executeCount 全部做完
        boolean completed = false;
        // 根目标 + 递归中间成品，防止拆解自身再合成
        final Set<Item> reserved = new HashSet<>();
    }

    // 调度游标
    private int cursor = 0;
    // 全局保留集：当前所有 active 且未完成的预设根目标，防止跨预设互逆合成
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
                    new ItemStack(Items.PISTON), 10, true, true, true
            ));
            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.IRON_BLOCK), 10, true, true, true
            ));
        }
        Player player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        // 工作台未打开或光标占用，不推进
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

        // 每 tick 重算全局保留集（所有 active 且未完成的预设根目标）
        activeTargets.clear();
        for (CraftingAssumption a : assumptions) {
            if (a.isActive() && !taskFor(a).completed) {
                activeTargets.add(a.getResult().getItem());
            }
        }

        // 从游标起，找到第一个未完成且处于激活状态的预设作为当前独占任务
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
        // 全部完成
        if (idx < 0) {
            return;
        }

        Decision d = advance(current, task, player, client);
        cursor = (d == Decision.ADVANCE) ? idx + 1 : idx;
    }

    /**
     * 推进单个预设状态机的一步
     * @return 返回本 tick 结束后游标是否应让位
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
            // 暂时做不了：让位，下一遍轮询重试
            return Decision.ADVANCE;
        }
        return doCraftStep(assumption, task, recipe, player, client);
    }

    // 一次合成尝试：成功则继续当前预设；缺料/失败→让位重试
    private Decision doCraftStep(CraftingAssumption assumption, TaskState task, CraftingRecipe recipe,
                                 Player player, Minecraft client) {
        // 禁止靠“拆自己”凑原料
        task.reserved.clear();
        task.reserved.add(assumption.getResult().getItem());

        if (!ensureMaterials(assumption, task, recipe, player, client)) {
            clearGrid(client, player.containerMenu);
            return Decision.ADVANCE;
        }
        // 【批量吞吐接缝】现在每次产出 1 件；将来可替换为“用可堆叠料填满网格一次产多件”，
        // 并按批量数递减 remainingOps，状态机骨架不变。
        if (!doCraft(assumption, recipe, player, client)) {
            clearGrid(client, player.containerMenu);
            return Decision.ADVANCE;
        }
        if (task.remainingOps != CraftingAssumption.UNTIL_EXHAUSTED) {
            --task.remainingOps;
            if (task.remainingOps <= 0) {
                // 该预设彻底完成，游标前进到下一个
                task.completed = true;
                task.reserved.clear();
                return Decision.ADVANCE;
            }
        }
        task.cooldown = SETTLE_TICKS;
        return Decision.STAY;
    }

    // 获取或创建某预设的私有状态机
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
     * 依据配方的网格布局把材料放入 3x3 工作台并产出结果
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
        activeTargets.clear();
    }
}