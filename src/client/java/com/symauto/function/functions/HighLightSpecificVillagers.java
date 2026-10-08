package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.Minecraft;

public class HighLightSpecificVillagers extends SymAbstractFunction {

    public static final HighLightSpecificVillagers INSTANCE = new HighLightSpecificVillagers();
    private HighLightSpecificVillagers() {
        String tooltip = "主手持可附魔物品时，自动扫描半径30格内卖相关附魔书的村民";
        super("high_light_specific_villagers", "高亮特定村民", tooltip);
    }
    @Override
    protected void onTrigger(Minecraft client) {

    }
}
