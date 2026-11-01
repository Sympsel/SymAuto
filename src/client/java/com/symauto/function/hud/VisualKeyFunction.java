package com.symauto.function.hud;

import com.symauto.function.base.SymAbstractFunction;
import net.minecraft.client.Minecraft;

public class VisualKeyFunction extends SymAbstractFunction {
    public static final VisualKeyFunction INSTANCE = new VisualKeyFunction();

    private VisualKeyFunction() {
        super("visual_key", "可视化按键");
    }

    @Override
    protected void onTrigger(Minecraft client) {

    }
}
