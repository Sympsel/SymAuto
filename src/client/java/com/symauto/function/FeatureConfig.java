package com.symauto.function;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.*;

import java.util.List;

public class FeatureConfig {
    public static final AutoAttackFunction AUTO_ATTACK_FUNCTION = AutoAttackFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_ATTACK_SAFETY_FUNCTION = AutoAttackSafetyFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_FISHING_FUNCTION = AutoFishingFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_EAT_FUNCTION = AutoEatFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_SWIFT_TOOLS_FUNCTION = AutoSwiftToolsFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_LOOT_CONTAINER_FUNCTION = AutoLootContainerFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION = AutoSwiftSameItemsToContainerFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION = AutoSwiftSameItemsToInventoryFunction.INSTANCE;
    public static final SymAbstractFunction ONE_CLICK_DISCARD_ITEMS = OneClickDiscardItems.INSTANCE;
    public static final SymAbstractFunction FIX_Y_PLACE_OR_DESTROY_FUNCTION = FixYPlaceOrDestroyFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_SELL_EMC_FUNCTION = AutoSellEmcFunction.INSTANCE;

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_ATTACK_FUNCTION,
            AUTO_ATTACK_SAFETY_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_SWIFT_TOOLS_FUNCTION,
            AUTO_LOOT_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION,
            ONE_CLICK_DISCARD_ITEMS,
            FIX_Y_PLACE_OR_DESTROY_FUNCTION,
            AUTO_SELL_EMC_FUNCTION
    );
}
