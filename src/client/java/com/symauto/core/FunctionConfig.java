package com.symauto.core;

import com.symauto.function.auto.*;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.container.FasterContainerPanelFunction;
import com.symauto.function.bundle.BundleQuickOpenFunction;
import com.symauto.function.highlight.HighLightItemDropFunction;
import com.symauto.function.highlight.HighLightMasterFunction;
import com.symauto.function.highlight.HighLightPlayerFunction;
import com.symauto.function.highlight.HighLightSpecificVillagersFunction;
import com.symauto.function.place.FixYPlaceOrDestroyFunction;
import com.symauto.function.place.SpeedBridgeFunction;
import com.symauto.function.oneclick.OneClickDiscardItemsFunction;
import com.symauto.function.oneclick.OneClickDiscardSameItemsFunction;
import com.symauto.function.oneclick.OneClickFlatItemsFunction;
import com.symauto.function.oneclick.OneClickTradeFunction;

import java.util.List;

public class FunctionConfig {
    public static final SymAbstractFunction AUTO_ATTACK_FUNCTION = AutoAttackFunction.INSTANCE
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
    public static final SymAbstractFunction BETTER_BUNDLE_FUNCTION = FasterContainerPanelFunction.INSTANCE
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
    public static final SymAbstractFunction HIGH_LIGHT_PLAYER_FUNCTION = HighLightPlayerFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction AUTO_RECORD_DEATH_INFO_FUNCTION = AutoRecordDeathInfoFunction.INSTANCE
            .addTag(SymFunctionTags.OTHER);
    public static final SymAbstractFunction AUTO_CRAFTING_FUNCTION = AutoCraftingFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.PRODUCING);
    public static final SymAbstractFunction AUTO_DISCARD_MORE_THING_FUNCTION = AutoDiscardMoreThingFunction.INSTANCE
            .addTag(SymFunctionTags.IDLE);

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_ATTACK_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_RUN_FUNCTION,
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
            SPEED_BRIDGE_FUNCTION,
            ONE_CLICK_TRADE_FUNCTION,
            HIGH_LIGHT_SPECIFIC_VILLAGERS_FUNCTION,
            HIGH_LIGHT_MASTER_FUNCTION,
            HIGH_LIGHT_ITEM_DROP_FUNCTION,
            HIGH_LIGHT_PLAYER_FUNCTION,
            AUTO_RECORD_DEATH_INFO_FUNCTION,
            AUTO_CRAFTING_FUNCTION,
            AUTO_DISCARD_MORE_THING_FUNCTION
    );
}
