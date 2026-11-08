package com.symhud.function.container;

import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.Tooltip;
import net.minecraft.client.Minecraft;

/**
 * Shift + 鼠标悬浮预览容器
 */
public class ShiftHoverContainerPreviewFunction extends SymAbstractFunction {
    public static final ShiftHoverContainerPreviewFunction INSTANCE = new ShiftHoverContainerPreviewFunction();

    private ShiftHoverContainerPreviewFunction() {
        super("shift_hover_container_preview", "容器预览");
        setEnable(true);
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("按住 Shift + 鼠标悬浮在背包容器内预览其中物品")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }
}


