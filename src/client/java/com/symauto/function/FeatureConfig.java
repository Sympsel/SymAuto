package com.symauto.function;

import com.symauto.entity.SymFunctionTags;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.*;

import java.util.List;

public class FeatureConfig {
    public static final SymAbstractFunction AUTO_ATTACK_FUNCTION = AutoAttackFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);
    public static final SymAbstractFunction AUTO_ATTACK_SAFETY_FUNCTION = AutoAttackSafetyFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);
    public static final SymAbstractFunction AUTO_FISHING_FUNCTION = AutoFishingFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);
    public static final SymAbstractFunction AUTO_EAT_FUNCTION = AutoEatFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);
    public static final SymAbstractFunction AUTO_RUN_FUNCTION = AutoRunFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);
    public static final SymAbstractFunction AUTO_SWIFT_TOOLS_FUNCTION = AutoSwiftToolsFunction.INSTANCE
            .addTag(SymFunctionTags.BUILDING_AND_DESTROYING)
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction AUTO_LOOT_CONTAINER_FUNCTION = AutoLootContainerFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION = AutoSwiftSameItemsToContainerFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION = AutoSwiftSameItemsToInventoryFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction AUTO_FILL_ITEM_CLASSIFICATION_MACHINE_FUNCTION = AutoFillItemClassificationMachineFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.PRODUCING);
    public static final SymAbstractFunction AUTO_CRAFT_TRANSMITTER_FUNCTION = AutoCraftTransmitterFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.PRODUCING);
    public static final SymAbstractFunction ONE_CLICK_DISCARD_ITEMS_FUNCTION = OneClickDiscardItemsFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION = OneClickDiscardSameItemsFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction ONE_CLICK_FLAT_ITEMS_FUNCTION = OneClickFlatItemsFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction FIX_Y_PLACE_OR_DESTROY_FUNCTION = FixYPlaceOrDestroyFunction.INSTANCE
            .addTag(SymFunctionTags.BUILDING_AND_DESTROYING);
    public static final SymAbstractFunction AUTO_SELL_EMC_FUNCTION = AutoSellEmcFunction.INSTANCE
            .addTag(SymFunctionTags.OTHER);
    public static final SymAbstractFunction BETTER_BUNDLE_FUNCTION = BetterBundleFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction BUNDLE_QUICK_OPEN_FUNCTION = BundleQuickOpenFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction SPEED_BRIDGE_FUNCTION = SpeedBridgeFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.BUILDING_AND_DESTROYING);
    public static final SymAbstractFunction ONE_CLICK_TRADE_FUNCTION = OneClickTradeFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION);
    public static final SymAbstractFunction HIGH_LIGHT_SPECIFIC_VILLAGERS_FUNCTION = HighLightSpecificVillagersFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_MASTER_FUNCTION = HighLightMasterFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_ITEM_DROP_FUNCTION = HighLightItemDropFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_ATTACK_FUNCTION,
            AUTO_ATTACK_SAFETY_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_RUN_FUNCTION,
            AUTO_SWIFT_TOOLS_FUNCTION,
            AUTO_LOOT_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_CONTAINER_FUNCTION,
            AUTO_SWIFT_SAME_ITEMS_TO_INVENTORY_FUNCTION,
            AUTO_FILL_ITEM_CLASSIFICATION_MACHINE_FUNCTION,
            AUTO_CRAFT_TRANSMITTER_FUNCTION,
            ONE_CLICK_DISCARD_ITEMS_FUNCTION,
            ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION,
            ONE_CLICK_FLAT_ITEMS_FUNCTION,
            FIX_Y_PLACE_OR_DESTROY_FUNCTION,
            AUTO_SELL_EMC_FUNCTION,
            BETTER_BUNDLE_FUNCTION,
            BUNDLE_QUICK_OPEN_FUNCTION,
            SPEED_BRIDGE_FUNCTION,
            ONE_CLICK_TRADE_FUNCTION,
            HIGH_LIGHT_SPECIFIC_VILLAGERS_FUNCTION,
            HIGH_LIGHT_MASTER_FUNCTION,
            HIGH_LIGHT_ITEM_DROP_FUNCTION
    );
}
