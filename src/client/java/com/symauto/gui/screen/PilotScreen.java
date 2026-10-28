package com.symauto.gui.screen;

import com.symauto.gui.widget.component.SymMenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.symauto.core.Constants.LOG;

/**
 * 试点屏
 */
public class PilotScreen extends Screen {
    private String lastAction = "没有操作";

    public PilotScreen() {
        super(Component.literal("试点屏幕"));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new SymMenuBar(Component.literal("SymAuto功能菜单"), List.of(
        new SymMenuBar.Menu(Component.literal("文件"), List.of(
                new SymMenuBar.Entry(Component.literal("打开配置目录"), () -> {
                    LOG.info("打开配置目录");
                }),
                new SymMenuBar.Entry(Component.literal("重载配置"),
                        Component.literal("丢弃未保存改动，从磁盘重新读取"),
                        () -> {
                            LOG.info("重载配置");
                        }))),
        new SymMenuBar.Menu(Component.literal("视图"), List.of(
                new SymMenuBar.Entry(Component.literal("刷新分类"), () -> {
                    LOG.info("刷新分类");
                }))))));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(this.font, this.title,
                (this.width - this.font.width(this.title)) / 2, this.height / 12, 0xFFFFFFFF);
        g.text(this.font, Component.literal(lastAction),
                (this.width - this.font.width(lastAction)) / 2, this.height - 40, 0xFFFFFFA0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}