package com.symauto.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 试点屏
 */
public class PilotScreen extends Screen {
    private String lastAction = "还没有操作";

    public PilotScreen() {
        super(Component.literal("FunctionButton 试点"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = this.height / 5;

//        addRenderableWidget(new SymButton(Component.literal("左键计数"), cx - 76, y, 152, 20)
//                .tooltip(Component.literal("§e单级提示\n第二行硬换行\n第三行"))
//                .onLeftClick(() -> {
//                    leftClickCount++;
//                    lastAction = "左键计数：" + leftClickCount;
//                }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title,
                (this.width - this.font.width(this.title)) / 2, this.height / 12, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal(lastAction),
                (this.width - this.font.width(lastAction)) / 2, this.height - 40, 0xFFFFFFA0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}