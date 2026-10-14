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
        IDLE,
        CHECK,
        CRAFT,
        DONE,
    }

    private final List<CraftingAssumption> assumptions;
    private State state = State.IDLE;
    private int cooldown = 0;
    private int remainingOps = 0;
    // 记录当前调用栈上正在被合成的物品
    private final Set<Item> recursionGuard = new HashSet<>();

    private AutoCraftingFunction() {
        super("auto_crafting", "自动合成",
                "todo"
        );
        assumptions = new ArrayList<>();
    }


    public void execute(CraftingAssumption assumption, Minecraft client) {
        if (assumption == null || client.player == null || client.gameMode == null) {
            return;
        }
        Player player = client.player;
        if (!player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        if (cooldown > 0) {
            --cooldown;
            return;
        }
        switch (state) {
            case IDLE -> {
                remainingOps = assumption.getExecuteCount();
                state = State.CHECK;
            }
            case CHECK -> {
                CraftingRecipe recipe = pickRecipe(assumption, player);
                if (recipe == null) {
                    state = State.DONE;
                    break;
                }
                state = State.CRAFT;
                doCraftStep(assumption, recipe, player, client);
            }
            case CRAFT -> {
                CraftingRecipe recipe = pickRecipe(assumption, player);
                if (recipe == null) {
                    state = State.DONE;
                    break;
                }
                doCraftStep(assumption, recipe, player, client);
            }
            case DONE -> {
                reset();
            }
        }
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (assumptions.isEmpty()) {
//            assumptions.add(new CraftingAssumption(
//                    new ItemStack(Items.DIAMOND_SWORD), 1, true, true, true
//            ));
            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.DISPENSER), 1, true, true, true
            ));
//            assumptions.add(new CraftingAssumption(
//                    new ItemStack(Items.PISTON), 1, true, true, true
//            ));

            assumptions.add(new CraftingAssumption(
                    new ItemStack(Items.IRON_BLOCK), 1, true, true, true
            ));
        }
        for (CraftingAssumption assumption : assumptions) {
            if (assumption.isActive()) {
                execute(assumption, client);
            }
        }
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

    private void doCraftStep(CraftingAssumption assumption, CraftingRecipe recipe,
                             Player player, Minecraft client) {
        recursionGuard.clear();
        // 根目标纳入保留集，组织后续递归通过消耗该目标本身来合成其原料
        recursionGuard.add(assumption.getResult().getItem());

        if (!ensureMaterials(assumption, recipe, player, client)) {
            state = State.DONE;
            return;
        }
        if (!doCraft(assumption, recipe, player, client)) {
            state = State.DONE;
            return;
        }
        if (remainingOps != CraftingAssumption.UNTIL_EXHAUSTED) {
            --remainingOps;
            if (remainingOps <= 0) {
                state = State.DONE;
                return;
            }
        }
        cooldown = SETTLE_TICKS;
        state = State.CHECK;
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

    private boolean recipeUsesReserved(CraftingRecipe recipe, Set<Item> recursionGuard) {
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (ingredient.items().map(Holder::value).anyMatch(recursionGuard::contains)) {
                return true;
            }
        }
        return false;
    }

    private boolean ensureMaterials(CraftingAssumption assumption, CraftingRecipe recipe,
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
            if (representative == null || recursionGuard.contains(representative)) {
                // 无法定位代表物品或检测到环形配方
                return false;
            }

            CraftingAssumption sub = new CraftingAssumption(
                    new ItemStack(representative), 1, true, false, true);
            recursionGuard.add(representative);
            CraftingRecipe subRecipe = sub.chooseRecipe(player, 1);
            if (subRecipe == null) {
                return false;
            }
            // 拒绝自我分解再合成
            boolean selfDecomposition = recipeUsesReserved(subRecipe, recursionGuard);
            System.out.println("[AutoCraft] selfDecomposition: " + selfDecomposition);
            if (selfDecomposition) {
                return false;
            }
            recursionGuard.add(representative);
            boolean made = ensureMaterials(sub, subRecipe, player, client)
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
        for (int pos = 0; pos < GRID_SIZE; pos++) {
            int gridSlot = GRID_FIRST_SLOT + pos;
            if (menu.getSlot(gridSlot).hasItem()) {
                click(client, containerId, gridSlot, 0, ContainerInput.QUICK_MOVE);
            }
        }
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


    private void reset() {
        state = State.IDLE;
        cooldown = 0;
        remainingOps = 0;
        recursionGuard.clear();
    }

    @Override
    protected void onDisable() {
        reset();
    }
}
