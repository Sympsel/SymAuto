package com.symauto.function.auto;

import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import net.minecraft.client.Minecraft;

public class AutoHoldRightButtonFunction extends SymAbstractFunction {
    private AutoHoldRightButtonFunction() {
        super("auto_hold_right_button", "模拟持续按住右键",
                Tooltip.create()
                        .line("模拟持续按住右键")
                        .toString()
        );
    }

    @Override
    protected void onTrigger(Minecraft client) {

    }
}
