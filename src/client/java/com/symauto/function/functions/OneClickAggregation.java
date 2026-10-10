package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.CraftingMenu;

@Deprecated
public class OneClickAggregation extends SymAbstractFunction {
    public static final OneClickAggregation INSTANCE = new OneClickAggregation();

    private OneClickAggregation() {
        super("one_click_aggregation", "一键将原材料合成块 / 锭", Tooltip.create()
                .line("Shift + A 将背包中所有原材料聚合（仅聚合可逆的）")
                .line("支持粒->锭->方块，粗矿->粗矿块")
                .toString());
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        if (client.gui.screen() instanceof CraftingScreen craftingScreen) {
            CraftingMenu menu = craftingScreen.getMenu();
        }
    }


}
