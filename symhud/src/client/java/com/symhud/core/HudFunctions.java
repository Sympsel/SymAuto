package com.symhud.core;

import com.symbase.core.SymFunctionTags;
import com.symbase.function.base.SymAbstractFunction;
import com.symhud.function.container.FasterContainerPanelFunction;
import com.symhud.function.container.MiddleClickContainerPreviewFunction;
import com.symhud.function.container.ShiftHoverContainerPreviewFunction;
import com.symhud.function.highlight.HighLightItemDropFunction;
import com.symhud.function.highlight.HighLightMasterFunction;
import com.symhud.function.highlight.HighLightPlayerFunction;
import com.symhud.function.highlight.HighLightSpecificVillagersFunction;

import java.util.List;

public class HudFunctions {
    public static final SymAbstractFunction FASTER_CONTAINER_PANEL_FUNCTION = FasterContainerPanelFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction MIDDLE_CLICK_CONTAINER_PREVIEW_FUNCTION = MiddleClickContainerPreviewFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction SHIFT_HOVER_CONTAINER_PREVIEW_FUNCTION = ShiftHoverContainerPreviewFunction.INSTANCE
            .addTag(SymFunctionTags.CONVENIENCE_OPERATION)
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_SPECIFIC_VILLAGERS_FUNCTION = HighLightSpecificVillagersFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_MASTER_FUNCTION = HighLightMasterFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_ITEM_DROP_FUNCTION = HighLightItemDropFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);
    public static final SymAbstractFunction HIGH_LIGHT_PLAYER_FUNCTION = HighLightPlayerFunction.INSTANCE
            .addTag(SymFunctionTags.HUD);

    public static final List<SymAbstractFunction> ALL = List.of(
            FASTER_CONTAINER_PANEL_FUNCTION,
            MIDDLE_CLICK_CONTAINER_PREVIEW_FUNCTION,
            SHIFT_HOVER_CONTAINER_PREVIEW_FUNCTION,
            HIGH_LIGHT_SPECIFIC_VILLAGERS_FUNCTION,
            HIGH_LIGHT_MASTER_FUNCTION,
            HIGH_LIGHT_ITEM_DROP_FUNCTION,
            HIGH_LIGHT_PLAYER_FUNCTION
    );
}
