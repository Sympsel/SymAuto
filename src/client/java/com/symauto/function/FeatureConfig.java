package com.symauto.function;

import java.util.List;

public class FeatureConfig {
    public static final AutoLeftClickFunction AUTO_LEFT_CLICK_FUNCTION = new AutoLeftClickFunction();
    public static final AutoRightClickFunction AUTO_RIGHT_CLICK_FUNCTION = new AutoRightClickFunction();
    public static final AutoFishingFunction AUTO_FISHING_FUNCTION = new AutoFishingFunction();
    public static final AutoEatFunction AUTO_EAT_FUNCTION = new AutoEatFunction();
    public static final AutoSwiftToolsFunction AUTO_SWIFT_TOOLS_FUNCTION = new AutoSwiftToolsFunction();

    public static final List<SymAbstractFunction> AUTO_ALL = List.of(
            AUTO_LEFT_CLICK_FUNCTION,
            AUTO_RIGHT_CLICK_FUNCTION,
            AUTO_FISHING_FUNCTION,
            AUTO_EAT_FUNCTION,
            AUTO_SWIFT_TOOLS_FUNCTION
    );

}
