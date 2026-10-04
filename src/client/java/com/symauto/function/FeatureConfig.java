package com.symauto.function;

import com.symauto.function.functions.*;

import java.util.List;

public class FeatureConfig {
    public static final SymAbstractFunction AUTO_ATTACK_FUNCTION = new AutoAttackFunction();
    public static final SymAbstractFunction AUTO_ATTACK_SAFETY_FUNCTION = new AutoAttackSafetyFunction();
    public static final SymAbstractFunction AUTO_FISHING_FUNCTION = new AutoFishingFunction();
    public static final SymAbstractFunction AUTO_EAT_FUNCTION = new AutoEatFunction();
    public static final SymAbstractFunction AUTO_SWIFT_TOOLS_FUNCTION = new AutoSwiftToolsFunction();
    public static final SymAbstractFunction AUTO_LOOT_CONTAINER_FUNCTION = new AutoLootContainerFunction();
    public static final SymAbstractFunction AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION = new AutoSwiftSameItemsToContainerFunction();
    public static final SymAbstractFunction FIX_Y_PLACE_OR_DESTROY_FUNCTION = new FixYPlaceOrDestroyFunction();

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_ATTACK_FUNCTION,
            AUTO_ATTACK_SAFETY_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_SWIFT_TOOLS_FUNCTION,
            AUTO_LOOT_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION,
            FIX_Y_PLACE_OR_DESTROY_FUNCTION
    );
}
