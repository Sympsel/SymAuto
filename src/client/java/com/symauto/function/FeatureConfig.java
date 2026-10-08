package com.symauto.function;

import com.symauto.entity.SymFunctionTags;
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
    public static final SymAbstractFunction AUTO_FILL_ITEM_CLASSIFICATION_MACHINE_FUNCTION = AutoFillItemClassificationMachine.INSTANCE;
    public static final SymAbstractFunction ONE_CLICK_DISCARD_ITEMS_FUNCTION = OneClickDiscardItems.INSTANCE;
    public static final SymAbstractFunction ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION = OneClickDiscardSameItems.INSTANCE;
    public static final SymAbstractFunction ONE_CLICK_FLAT_ITEMS_FUNCTION = OneClickFlatItems.INSTANCE;
    public static final SymAbstractFunction FIX_Y_PLACE_OR_DESTROY_FUNCTION = FixYPlaceOrDestroyFunction.INSTANCE;
    public static final SymAbstractFunction AUTO_SELL_EMC_FUNCTION = AutoSellEmcFunction.INSTANCE;
    public static final SymAbstractFunction BETTER_BUNDLE_FUNCTION = BetterBundleFunction.INSTANCE;
    public static final SymAbstractFunction BUNDLE_QUICK_OPEN_FUNCTION = BundleQuickOpenFunction.INSTANCE;
    public static final SymAbstractFunction SPEED_BRIDGE_FUNCTION = SpeedBridgeFunction.INSTANCE;

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_ATTACK_FUNCTION,
            AUTO_ATTACK_SAFETY_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_SWIFT_TOOLS_FUNCTION,
            AUTO_LOOT_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION,
            AUTO_FILL_ITEM_CLASSIFICATION_MACHINE_FUNCTION,
            ONE_CLICK_DISCARD_ITEMS_FUNCTION,
            ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION,
            ONE_CLICK_FLAT_ITEMS_FUNCTION,
            FIX_Y_PLACE_OR_DESTROY_FUNCTION,
            AUTO_SELL_EMC_FUNCTION,
            BETTER_BUNDLE_FUNCTION,
            BUNDLE_QUICK_OPEN_FUNCTION,
            SPEED_BRIDGE_FUNCTION
    );

    static {
        AUTO_ATTACK_FUNCTION.addTag(SymFunctionTags.IDLE);
        AUTO_ATTACK_SAFETY_FUNCTION.addTag(SymFunctionTags.IDLE);
        AUTO_FISHING_FUNCTION.addTag(SymFunctionTags.IDLE);
        AUTO_EAT_FUNCTION.addTag(SymFunctionTags.IDLE);
        AUTO_SWIFT_TOOLS_FUNCTION.
                addTag(SymFunctionTags.BUILDING_AND_DESTROYING)
                .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        AUTO_LOOT_CONTAINER_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);

        AUTO_FILL_ITEM_CLASSIFICATION_MACHINE_FUNCTION
                .addTag(SymFunctionTags.PRODUCING)
                .addTag(SymFunctionTags.CONVENIENCE_OPERATION);

        ONE_CLICK_DISCARD_ITEMS_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        ONE_CLICK_FLAT_ITEMS_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        FIX_Y_PLACE_OR_DESTROY_FUNCTION.addTag(SymFunctionTags.BUILDING_AND_DESTROYING);
        AUTO_SELL_EMC_FUNCTION.addTag(SymFunctionTags.OTHER);
        BETTER_BUNDLE_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        BUNDLE_QUICK_OPEN_FUNCTION.addTag(SymFunctionTags.CONVENIENCE_OPERATION);
        SPEED_BRIDGE_FUNCTION
                .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
                .addTag(SymFunctionTags.BUILDING_AND_DESTROYING);
    }
}
