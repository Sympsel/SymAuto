package com.symauto.gui.screen;

import com.symauto.command.SymAutoCommand;
import com.symauto.gui.base.SymBaseScreen;
import com.symauto.gui.widget.ScrollableTextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CommandUsageScreen extends SymBaseScreen {
    private final Screen parent;
    private ScrollableTextList list;

    public CommandUsageScreen(Screen parent) {
        super(Component.literal("SymAuto 指令用法").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = new ScrollableTextList(() -> SymAutoCommand.usageLines().stream()
                .map(c -> new ScrollableTextList.Line(c, 0xFFFFFFFF)).toList())
                .setBounds(8, 30, this.width - 8, this.height - 24)
                .setEmptyText("暂无内容")
                .setHintText("滚轮滚动")
                .setScrollbarEnabled(true);
        addRenderableWidget(backButton(parent, "返回主菜单", 72));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        list.render(graphics, this.font);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderTitle(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return list.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}