package com.symauto.model.preset;

import com.symauto.function.utils.ClientRecipeMaps;
import com.symauto.function.utils.ItemUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CraftingPreset {
    public static final int UNTIL_EXHAUSTED = -1;
    private ItemStack result;
    // 执行次数，-1表示一直合成直到背包材料耗尽
    private int executeCount;
    // 是否递归查询并合成原材料
    private boolean isRecursiveSearchMaterial;
    // 是否将产物丢出，不占用背包空间
    private boolean isThrowResult;
    private boolean isActive;

    public List<CraftingRecipe> getRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        List<CraftingRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : ClientRecipeMaps.get(mc)) {
            if (!(holder.value() instanceof ShapedRecipe) && !(holder.value() instanceof ShapelessRecipe)) {
                continue;
            }
            CraftingRecipe recipe = (CraftingRecipe) holder.value();
            try {
                ItemStack output = recipe.assemble(CraftingInput.EMPTY);
                if (output.is(result.getItem())) {
                    recipes.add(recipe);
                }
            } catch (Throwable ignored) {
            }
        }
        return recipes;
    }

    public CraftingRecipe chooseRecipe(Player player, int crafts) {
        return getRecipes().stream()
                // 材料可凑齐，按实际网格占用次数计算
                .filter(r -> hasIngredientsFor(r, player, crafts))
                // 打分排序
                .max(Comparator
                        // 单次产量高者优先
                        .comparingInt((CraftingRecipe r) -> r.assemble(CraftingInput.EMPTY).getCount())
                        // 材料种类需求更低
                        .thenComparingInt(r -> -nonEmptyIngredientCount(r))
                        .thenComparing(r -> r.getClass().getName()))
                .orElse(null);
    }

    /**
     * placementInfo().ingredients() 是同种材料去重后的列表；
     */
    public static boolean hasIngredientsFor(CraftingRecipe recipe, Player player, int crafts) {
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        IntList slots = recipe.placementInfo().slotsToIngredientIndex();
        List<Ingredient> demands = new ArrayList<>();
        if (slots.isEmpty()) {
            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < crafts; i++) {
                    demands.add(ingredient);
                }
            }
            return ItemUtils.hasIngredientsFor(demands, player, 1);
        }
        for (int slot = 0; slot < slots.size(); slot++) {
            int ingredientIndex = slots.getInt(slot);
            if (ingredientIndex == PlacementInfo.EMPTY_SLOT) {
                continue;
            }
            Ingredient ingredient = ingredients.get(ingredientIndex);
            for (int i = 0; i < crafts; i++) {
                demands.add(ingredient);
            }
        }
        return ItemUtils.hasIngredientsFor(demands, player, 1);
    }

    /**
     * @return 返回配方中非空材料的种类数
     */
    private static int nonEmptyIngredientCount(CraftingRecipe recipe) {
        int n = 0;
        for (Ingredient ing : recipe.placementInfo().ingredients()) {
            if (!ing.isEmpty()) {
                ++n;
            }
        }
        return n;
    }
}