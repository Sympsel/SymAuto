package com.symauto.gui.screen;

import com.symauto.config.ConfigManager;
import com.symauto.core.SymFunctionTags;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.gui.base.SymBaseScreen;
import com.symauto.gui.widget.utils.ButtonFactory;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FunctionTagEditScreen extends SymBaseScreen {
    private final Screen parent;
    private final SymAbstractFunction function;

    private static final int LIST_TOP = 36;
    private static final int ROW_H = 20;
    private static final int GAP = 4;
    private static final int BTN_W = 180;

    public FunctionTagEditScreen(Screen parent, SymAbstractFunction function) {
        super(Component.literal("功能分类 · " + function.getName())
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
        this.function = function;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - BTN_W) / 2;
        int y = LIST_TOP;
        for (int tag = 0; tag < SymFunctionTags.COUNT; tag++) {
            final int t = tag;
            boolean on = function.hasTag(t);
            Component label = Component.literal((on ? "✔ " : "✖ ") + SymFunctionTags.getTagName(t));
            addRenderableWidget(
                    ButtonFactory.toggle(label, x, y, BTN_W, ROW_H, on)
                            .onLeftClick(() -> {
                                function.toggleTag(t);
                                ConfigManager.save();
                                reopen();
                            }).tooltip(Component.literal("点击将该功能加入/移出此分类")));
            y += ROW_H + GAP;
        }
    }

    private void reopen() {
        this.minecraft.gui.setScreen(new FunctionTagEditScreen(parent, function));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        renderTitle(g);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
