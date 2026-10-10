package com.symauto.gui;

import com.symauto.config.ConfigManager;
import com.symauto.config.option.ConfigOption;
import com.symauto.function.abstracts.SymAbstractFunction;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigMenuScreen extends Screen {
    private final SymAbstractFunction function;
    private final Screen parent;

    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int TITLE_Y = 12;
    private static final int ROW_H = 22;
    private static final int GAP_Y = 6;
    private static final int LABEL_W = 150;
    private static final int ARROW_W = 20;
    private static final int ARROW_H = 20;
    private static final int VALUE_W = 44;

    private int panelLeft;
    private int contentTop;
    private int panelW;

    public ConfigMenuScreen(SymAbstractFunction function, Screen parent) {
        super(Component.literal(function.getName() + " · 配置")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.function = function;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // 布局：◀  [label .... value]  ▶
        panelW = ARROW_W + 2 + LABEL_W + 2 + VALUE_W + 2 + ARROW_W;
        panelLeft = (this.width - panelW) / 2;
        contentTop = TITLE_Y + 22;

        int y = contentTop;
        int midX = panelLeft + ARROW_W + 2;
        for (ConfigOption opt : function.getConfigOptions()) {
            final ConfigOption ref = opt;
            this.addRenderableWidget(Button.builder(Component.literal("◀"),
                            b -> { ref.cycle(-1); ConfigManager.save(); })
                    .bounds(panelLeft, y, ARROW_W, ARROW_H).build());
            this.addRenderableWidget(Button.builder(Component.literal("▶"),
                            b -> { ref.cycle(1); ConfigManager.save(); })
                    .bounds(midX + LABEL_W + 2 + VALUE_W + 2, y, ARROW_W, ARROW_H).build());
            y += ROW_H + GAP_Y;
        }

        y += GAP_Y;
        this.addRenderableWidget(Button.builder(Component.literal("重置本功能"), b -> {
            for (ConfigOption o : function.getConfigOptions()) {
                o.reset();
            }
            ConfigManager.save();
        }).bounds(panelLeft, y, panelW, ARROW_H).build());
        y += ROW_H + GAP_Y;
        this.addRenderableWidget(Button.builder(Component.literal("返回"), b ->
                        this.minecraft.gui.setScreen(parent))
                .bounds(panelLeft, y, panelW, ARROW_H).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(panelLeft - 6, contentTop - 8,
                panelLeft + panelW + 6, contentTop + function.getConfigOptions().size() * (ROW_H + GAP_Y) + 4,
                0x66000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, TITLE_COLOR, true);

        int y = contentTop;
        int midX = panelLeft + ARROW_W + 2;
        for (ConfigOption opt : function.getConfigOptions()) {
            graphics.text(this.font, opt.getLabel(), midX, y + 6, 0xFFFFFFFF, false);
            String value = opt.displayValue();
            int valueX = midX + LABEL_W + 2 + VALUE_W - this.font.width(value);
            graphics.text(this.font, value, valueX, y + 6, 0xFF7FFF7F, false);
            y += ROW_H + GAP_Y;
        }
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
