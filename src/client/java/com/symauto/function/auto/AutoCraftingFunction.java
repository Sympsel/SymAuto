package com.symauto.function.auto;

import com.symauto.config.option.IntOption;
import com.symauto.function.utils.ContainerActions;
import com.symauto.storage.CraftingPresetStore;
import com.symauto.model.preset.CraftingPreset;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.gui.screen.function.CraftingPresetsManagerScreen;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.*;

public class AutoCraftingFunction extends SymAbstractFunction {
    public static final AutoCraftingFunction INSTANCE = new AutoCraftingFunction();

    private static final int RESULT_SLOT = 0;
    private static final int GRID_FIRST_SLOT = 1;
    private static final int GRID_SIZE = 9;
    private static final int STACK_MAX = 64;
    // 合成后等待服务器回包同步背包的 tick 数
    private static final int SETTLE_TICKS_DEFAULT = 2;
    // 失败、缺料、补料超时后的退避时间
    private static final int WAIT_TICKS_DEFAULT = 0;

    @Getter
    @Setter
    private int settleTicks = SETTLE_TICKS_DEFAULT;
    @Getter
    @Setter
    private int waitTicks = WAIT_TICKS_DEFAULT;

    private static final int LOG_LIMIT = 200;
    private static final Object LOG_LOCK = new Object();
    private static final Deque<LogLine> EXECUTION_LOGS = new ArrayDeque<>();

    private record LogLine(String message, int repeat) {
        String display() {
            return repeat > 1 ? message + "（重复 ×" + repeat + "）" : message;
        }

        LogLine nextRepeat() {
            return new LogLine(message, repeat + 1);
        }
    }

    public static List<String> getExecutionLogs() {
        synchronized (LOG_LOCK) {
            return EXECUTION_LOGS.stream().map(LogLine::display).toList();
        }
    }

    public static void clearExecutionLogs() {
        synchronized (LOG_LOCK) {
            EXECUTION_LOGS.clear();
        }
    }

    private static void debug(String message, Object... args) {
        String formatted = formatLog(message, args);
        synchronized (LOG_LOCK) {
            LogLine last = EXECUTION_LOGS.peekLast();
            if (last != null && last.message().equals(formatted)) {
                EXECUTION_LOGS.pollLast();
                EXECUTION_LOGS.addLast(last.nextRepeat());
            } else {
                EXECUTION_LOGS.addLast(new LogLine(formatted, 1));
                while (EXECUTION_LOGS.size() > LOG_LIMIT) {
                    EXECUTION_LOGS.pollFirst();
                }
            }
        }
    }

    private static String formatLog(String message, Object... args) {
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int start = 0;
        while (true) {
            int marker = message.indexOf("{}", start);
            if (marker < 0) {
                result.append(message, start, message.length());
                break;
            }
            result.append(message, start, marker);
            if (argIndex < args.length) {
                result.append(args[argIndex++]);
            } else {
                result.append("{}");
            }
            start = marker + 2;
        }
        return result.toString();
    }

    private static String describeItem(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static String describeIngredient(Ingredient ingredient) {
        List<String> names = ingredient.items()
                .map(Holder::value)
                .map(AutoCraftingFunction::describeItem)
                .distinct()
                .limit(3)
                .toList();
        if (names.isEmpty()) {
            return "空";
        }
        String text = String.join("/", names);
        return ingredient.items().count() > 3 ? text + " 等" : text;
    }

    private static String describeRecipe(CraftingRecipe recipe) {
        if (recipe == null) {
            return "无";
        }
        ItemStack output = recipe.assemble(CraftingInput.EMPTY);
        return recipe.getClass().getSimpleName()
                + " → " + output.getHoverName().getString() + " ×" + output.getCount();
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
        super("auto_crafting", "自动合成");
        addOption(new IntOption("settleTicks", "服务器同步等待（tick，过高影响效率）",
                this::getSettleTicks, this::setSettleTicks, 0, 10, 1));
        addOption(new IntOption("waitTicks", "失败、缺料、补料超时后的退避时间（tick）",
                this::getWaitTicks, this::setWaitTicks, 0, 10, 1));
        presets = new ArrayList<>();
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("打开工作台自动线性轮询执行预设管理器里的合成配方，全自动输入输出，输出可接漏斗矿车")
                .keyValueLine(ChatFormatting.YELLOW, "服务器同步等待（延迟越高调越高）", ChatFormatting.GRAY, String.valueOf(settleTicks))
                .keyValueLine(ChatFormatting.YELLOW, "失败、缺料、补料超时后的退避时间", ChatFormatting.GRAY, String.valueOf(waitTicks))
                .line(ChatFormatting.GOLD, "支持配置是否递归合成原材料")
                .line(ChatFormatting.GOLD, "支持配置是否丢出产物")
                .line(ChatFormatting.GOLD, "支持配置合成次数")
                .line(ChatFormatting.GOLD, "多人模式下仅能读到原版配方，如果服务器修改了部分原版配方，将因配方被覆盖无法自动合成")
                .line(ChatFormatting.YELLOW, "需要自动化建议开启递归合成材料，不过将导致合成效率大幅下降；需要高效自动合成建议关闭递归合成，手动配齐材料或者将需要的材料也放入预设中")
                .toString();
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new CraftingPresetsManagerScreen(parent);
    }

    /**
     * 预设存储变化时重建运行时列表并重置状态（编辑预设即从头开始，符合预期）
     */
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

        Decision d = advance(current, task, player, idx);
        cursor = (d == Decision.ADVANCE) ? idx + 1 : idx;
    }

    /**
     * 推进单个预设状态机的一步，返回本 tick 结束后游标是否应让位
     */
    private Decision advance(CraftingPreset assumption, TaskState task,
                             Player player, int storeIndex) {
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
            clearGrid(player.containerMenu);
            task.cooldown = waitTicks;
            // 暂时做不了就让位，退避后再试
            return Decision.ADVANCE;
        }
        return doCraftStep(assumption, task, recipe, player, storeIndex);
    }

    /**
     * 一次合成尝试：优先原料槽堆叠批量合成，否则回退逐件（含递归中间产物）
     * 成功则继续当前预设；缺料/失败就让位重试
     */
    private Decision doCraftStep(CraftingPreset assumption, TaskState task, CraftingRecipe recipe,
                                 Player player, int storeIndex) {
        // 以当前预设产物为根重建本链保留集，禁止靠“拆自己/拆祖先”凑料
        int targetOps = (task.remainingOps == CraftingPreset.UNTIL_EXHAUSTED)
                ? Integer.MAX_VALUE : task.remainingOps;
        int crafted = tryBatchCraft(assumption, recipe, player, targetOps);
        if (crafted > 0) {
            debug("批量合成完成：{}，执行 {} 次", describeItem(assumption.getResult().getItem()), crafted);
            return finishBatch(task, crafted, storeIndex);
        }

        // 需要中间产物或存货不足以堆叠时，逐件合成
        MaterialCheck materials = ensureMaterials(assumption, recipe, player,
                Set.of(assumption.getResult().getItem()), task);
        if (materials == MaterialCheck.WAITING) {
            debug("等待中间材料同步：{}", describeItem(assumption.getResult().getItem()));
            task.cooldown = settleTicks;
            return Decision.STAY;
        }
        if (materials == MaterialCheck.UNAVAILABLE) {
            debug("暂时无法补齐材料：{}（{}）",
                    describeItem(assumption.getResult().getItem()), describeRecipe(recipe));
            clearGrid(player.containerMenu);
            task.cooldown = waitTicks;
            return Decision.ADVANCE;
        }
        if (!doCraft(assumption, recipe, player)) {
            debug("合成操作失败：{}（{}）",
                    describeItem(assumption.getResult().getItem()), describeRecipe(recipe));
            clearGrid(player.containerMenu);
            task.cooldown = waitTicks;
            return Decision.ADVANCE;
        }
        debug("合成完成：{} ×1", describeItem(assumption.getResult().getItem()));
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
        task.cooldown = settleTicks;
        return Decision.STAY;
    }

    /**
     * 批量合成
     *
     * @return 实际合成的配方执行次数；0 表示本次不适合批量
     */
    private int tryBatchCraft(CraftingPreset assumption, CraftingRecipe recipe,
                              Player player, int targetOps) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu)) {
            return 0;
        }
        Inventory inventory = player.getInventory();

        PlacementInfo placement = recipe.placementInfo();
        List<Ingredient> ingredients = placement.ingredients();
        IntList slotsToIngredientIndex = placement.slotsToIngredientIndex();
        int width = (recipe instanceof ShapedRecipe shaped) ? shaped.getWidth() : 3;

        // 确定每个非空网格位对应哪种原料
        List<int[]> positions = new ArrayList<>(); // {gridSlot, ingredientIndex}
        int placementSize = slotsToIngredientIndex.isEmpty() ? ingredients.size() : slotsToIngredientIndex.size();
        for (int p = 0; p < placementSize; p++) {
            int ingredientIndex = slotsToIngredientIndex.isEmpty()
                    ? p
                    : slotsToIngredientIndex.getInt(p);
            if (ingredientIndex == PlacementInfo.EMPTY_SLOT) {
                continue;
            }
            int row = p / width;
            int col = p % width;
            int gridSlot = GRID_FIRST_SLOT + row * 3 + col;
            if (gridSlot >= GRID_FIRST_SLOT + GRID_SIZE) {
                debug("批量规划失败: 配方={}, 网格槽={} 超出范围", describeRecipe(recipe), gridSlot);
                return 0;
            }
            positions.add(new int[]{gridSlot, ingredientIndex});
        }
        if (positions.isEmpty()) {
            return 0;
        }

        // 合并相同 Ingredient
        int[] required = requiredCounts(recipe);
        int batch = Math.min(targetOps, STACK_MAX);
        for (int j = 0; j < ingredients.size(); j++) {
            Ingredient ingredient = ingredients.get(j);
            if (ingredient.isEmpty() || hasSameIngredientBefore(ingredients, j)) {
                continue;
            }
            int requiredTotal = totalRequiredFor(ingredients, required, j);
            int available = countIngredient(inventory, ingredient);
            int ingredientBatch = available / requiredTotal;
            int maxStackSize = maxAvailableStackSize(menu, inventory, ingredient);
            batch = Math.min(batch, Math.min(ingredientBatch, maxStackSize));
        }
        if (batch <= 0) {
            debug("批量规划失败: 配方={}, 材料总数不足", describeRecipe(recipe));
            return 0;
        }

        // 为每个网格位精确分配 batch 个材料；允许同一背包槽服务多个网格位
        Map<Integer, Integer> remainingBySlot = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container == inventory && slot.hasItem()) {
                remainingBySlot.put(slot.index, slot.getItem().getCount());
            }
        }
        List<int[]> plan = new ArrayList<>(); // {gridSlot, srcSlot, count}
        for (int[] position : positions) {
            int gridSlot = position[0];
            Ingredient ingredient = ingredients.get(position[1]);
            int needed = batch;
            for (Slot slot : menu.slots) {
                if (slot.container != inventory || needed <= 0) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }
                int available = remainingBySlot.getOrDefault(slot.index, 0);
                int take = Math.min(available, needed);
                if (take <= 0) {
                    continue;
                }
                remainingBySlot.put(slot.index, available - take);
                plan.add(new int[]{gridSlot, slot.index, take});
                needed -= take;
            }
            if (needed > 0) {
                debug("批量分配失败：{} 第 {} 格还缺 {} 个 {}",
                        describeRecipe(recipe), gridSlot, needed, describeIngredient(ingredient));
                return 0;
            }
        }

        clearGrid(menu);

        Map<Integer, List<int[]>> planBySource = new LinkedHashMap<>();
        for (int[] entry : plan) {
            planBySource.computeIfAbsent(entry[1], k -> new ArrayList<>()).add(entry);
        }
        for (Map.Entry<Integer, List<int[]>> sourcePlan : planBySource.entrySet()) {
            int src = sourcePlan.getKey();
            ContainerActions.leftClickSlot(src);
            for (int[] entry : sourcePlan.getValue()) {
                int gridSlot = entry[0];
                int count = entry[2];
                for (int i = 0; i < count; ++i) {
                    ContainerActions.rightClickSlot(gridSlot);
                }
            }
            if (!menu.getCarried().isEmpty()) {
                ContainerActions.leftClickSlot(src);
            }
        }

        // 网格每格恰好 batch 个，结果槽一次取出即执行 batch 次
        if (assumption.isThrowResult()) {
            for (int i = 0; i < batch; i++) {
                ContainerActions.dropSlotStack(RESULT_SLOT);
            }
        } else {
            ContainerActions.quickMove(RESULT_SLOT);
        }

        clearGrid(menu);
        return batch;
    }

    private int maxAvailableStackSize(AbstractContainerMenu menu, Inventory inventory, Ingredient ingredient) {
        int max = STACK_MAX;
        boolean found = false;
        for (Slot slot : menu.slots) {
            if (slot.container != inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                max = Math.min(max, stack.getMaxStackSize());
                found = true;
            }
        }
        return found ? max : 0;
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
            return craftable;
        }
        if (!assumption.isRecursiveSearchMaterial()) {
            return null;
        }
        return assumption.getRecipes().stream()
                .max(Comparator.comparingInt(r -> r.assemble(CraftingInput.EMPTY).getCount()))
                .orElse(null);
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
     * 等待服务器回包，不能在同一 tick 内立刻重试父配方
     */
    private MaterialCheck pendingMaterialStatus(TaskState task, Inventory inventory) {
        if (task.pendingIngredient == null) {
            return MaterialCheck.READY;
        }
        int now = countIngredient(inventory, task.pendingIngredient);
        if (now > task.pendingBefore) {
            debug("中间材料已到位：{}（{} → {}）",
                    describeIngredient(task.pendingIngredient), task.pendingBefore, now);
            task.pendingIngredient = null;
            task.pendingBefore = 0;
            task.pendingWait = 0;
            return MaterialCheck.READY;
        }
        if (--task.pendingWait <= 0) {
            debug("中间材料同步超时：{}（仍为 {}）",
                    describeIngredient(task.pendingIngredient), now);
            // 超时后放弃本次补料状态，进入正常退避；下一轮仍可重新尝试
            task.pendingIngredient = null;
            task.pendingBefore = 0;
            return MaterialCheck.UNAVAILABLE;
        }
        return MaterialCheck.WAITING;
    }

    private void waitForMaterial(TaskState task, Ingredient ingredient, int before) {
        debug("已发起中间材料合成：{}（当前 {} 个）", describeIngredient(ingredient), before);
        task.pendingIngredient = ingredient;
        task.pendingBefore = before;
        task.pendingWait = waitTicks;
    }

    private MaterialCheck ensureMaterials(CraftingPreset assumption, CraftingRecipe recipe,
                                          Player player, Set<Item> path,
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
            if (have >= need) {
                continue;
            }
            debug("材料不足：{} 需要 {} 个，当前 {} 个",
                    describeIngredient(ingredient), need, have);
            if (!assumption.isRecursiveSearchMaterial()) {
                debug("未开启递归合成，无法自动补齐：{}", describeIngredient(ingredient));
                return MaterialCheck.UNAVAILABLE;
            }
            Item representative = representativeItem(ingredient);
            if (representative == null || path.contains(representative)) {
                debug("无法自动补齐 {}：未找到可用配方或存在循环配方", describeIngredient(ingredient));
                return MaterialCheck.UNAVAILABLE;
            }
            CraftingPreset sub = new CraftingPreset(
                    new ItemStack(representative), 1, true, false, true);
            // 拆解配方不得动用本链上的任何目标（含要产出的代表物品）
            Set<Item> subPath = new HashSet<>(path);
            subPath.add(representative);
            List<CraftingRecipe> subCandidates = sub.getRecipes();
            CraftingRecipe subRecipe = subCandidates.stream()
                    .filter(candidate -> !recipeUsesReserved(candidate, subPath))
                    .filter(candidate -> CraftingPreset.hasIngredientsFor(candidate, player, 1))
                    .max(Comparator.comparingInt(candidate ->
                            candidate.assemble(CraftingInput.EMPTY).getCount()))
                    .orElse(null);
            if (subRecipe == null) {
                debug("没有可用的中间材料配方：{}", describeItem(representative));
                return MaterialCheck.UNAVAILABLE;
            }
            debug("准备合成中间材料：{}（{}）", describeItem(representative), describeRecipe(subRecipe));
            MaterialCheck subMaterials = ensureMaterials(sub, subRecipe, player, subPath, task);
            if (subMaterials != MaterialCheck.READY) {
                return subMaterials;
            }
            boolean craftedSub = doCraft(sub, subRecipe, player);
            if (!craftedSub) {
                debug("中间材料合成失败：{}", describeItem(representative));
                return MaterialCheck.UNAVAILABLE;
            }
            // 避免同一 tick 内空转和闪烁
            waitForMaterial(task, ingredient, have);
            return MaterialCheck.WAITING;
        }
        return MaterialCheck.READY;
    }

    /**
     * 逐件合成：依据配方网格把材料各放 1 个进 3x3 工作台并产出一次
     *
     * @return 是否成功产出一次
     */
    private boolean doCraft(CraftingPreset assumption, CraftingRecipe recipe,
                            Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu)) {
            return false;
        }
        // 凑不满网格就一次都不点
        if (!canFillGrid(player.getInventory(), recipe)) {
            debug("材料不足以填满合成格：{}", describeRecipe(recipe));
            return false;
        }
        // 清理网格残留
        clearGrid(menu);
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
                continue;
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
            ContainerActions.leftClickSlot(src); // 取整叠
            ContainerActions.rightClickSlot(gridSlot); // 放 1 个
            ContainerActions.leftClickSlot(src); // 剩余放回
        }
        if (assumption.isThrowResult()) {
            ContainerActions.dropSlotStack(RESULT_SLOT); // 合成并丢出
        } else {
            ContainerActions.quickMove(RESULT_SLOT); // 合成入背包
        }
        return true;
    }

    /**
     * 清理工作台网格残留
     */
    private void clearGrid(AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu) {
            for (int pos = 0; pos < GRID_SIZE; pos++) {
                int gridSlot = GRID_FIRST_SLOT + pos;
                if (menu.getSlot(gridSlot).hasItem()) {
                    ContainerActions.quickMove(gridSlot);
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