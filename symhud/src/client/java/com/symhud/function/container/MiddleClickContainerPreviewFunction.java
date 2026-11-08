package com.symhud.function.container;

import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.Tooltip;
import net.minecraft.client.Minecraft;

/**
 * 按住鼠标中键预览主手容器
 */
public class MiddleClickContainerPreviewFunction extends SymAbstractFunction {
    public static final MiddleClickContainerPreviewFunction INSTANCE = new MiddleClickContainerPreviewFunction();

    private MiddleClickContainerPreviewFunction() {
        super("middle_click_container_preview", "中键容器预览");
        setEnable(true);
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("无界面时，按住鼠标中键预览主手中的收纳袋/潜影盒/末影箱")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }
}


