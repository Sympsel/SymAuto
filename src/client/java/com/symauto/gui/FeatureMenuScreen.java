package com.symauto.gui;

import com.symauto.function.FeatureConfig;
import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class FeatureMenuScreen extends Screen {
    private static final int BTN_W = 220;
    private static final int BTN_H = 24;
    private static final int GAP = 5;

    // 居左流式布局的边距
    private static final int MARGIN_LEFT = 20;
    private static final int MARGIN_TOP = 20;

    public FeatureMenuScreen() {
        super(Component.literal("SymAuto 功能菜单"));
    }

    @Override
    protected void init() {
        int autoCount = FeatureConfig.AUTO_ALL.size();

        int startX = MARGIN_LEFT;
        int startY = MARGIN_TOP + 20; // 给标题留一行

        for (int i = 0; i < autoCount; i++) {
            final SymAbstractFunction func = FeatureConfig.AUTO_ALL.get(i);
            Button btn = Button.builder(buildLabel(func), b -> {
                func.toggle();
                b.setMessage(buildLabel(func));
            }).tooltip(Tooltip.create(Component.literal(func.getTooltip()))
            ).bounds(startX, startY + i * (BTN_H + GAP), BTN_W, BTN_H).build();
            this.addRenderableWidget(btn);
        }
    }

    private Component buildLabel(SymAbstractFunction func) {
        String state = func.isEnable() ? "§a开启" : "§c关闭";
        return Component.literal(func.getName() + "  " + state);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, MARGIN_LEFT, MARGIN_TOP, 0xFFFFFF, true);
    }
}