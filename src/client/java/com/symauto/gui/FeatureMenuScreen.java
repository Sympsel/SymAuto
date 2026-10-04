package com.symauto.gui;

import com.symauto.function.FeatureConfig;
import com.symauto.function.SymAbstractFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FeatureMenuScreen extends Screen {
    private final Screen parent;

    // 更紧凑的按钮尺寸
    private static final int BTN_W = 120;
    private static final int BTN_H = 20;
    private static final int GAP_X = 6;
    private static final int GAP_Y = 5;

    private static final int SIDE_MARGIN = 12;
    private static final int TITLE_Y = 10;
    private static final int GRID_TOP = 34;
    private static final int MAX_COLUMNS = 3;

    // 持有功能按钮引用，便于精确刷新，避免依赖 children() 顺序
    private final List<Button> featureButtons = new ArrayList<>();

    public FeatureMenuScreen(Screen parent) {
        super(Component.literal("SymAuto 功能菜单"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        featureButtons.clear();

        List<SymAbstractFunction> functions = FeatureConfig.AUTO_ALL;

        int columns = computeColumns();
        int gridWidth = columns * BTN_W + (columns - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = GRID_TOP;

        for (int i = 0; i < functions.size(); i++) {
            final SymAbstractFunction func = functions.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (BTN_W + GAP_X);
            int y = startY + row * (BTN_H + GAP_Y);

            Button btn = Button.builder(buildLabel(func), b -> {
                func.toggle();
                refreshAllButtons();
            })
            .tooltip(Tooltip.create(Component.literal(func.getTooltip())))
            .bounds(x, y, BTN_W, BTN_H)
            .build();

            featureButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        if (parent != null) {
            int rows = (functions.size() + columns - 1) / columns;
            int backY = startY + rows * (BTN_H + GAP_Y) + 6;
            this.addRenderableWidget(
                    Button.builder(Component.literal("返回"), b -> this.minecraft.gui.setScreen(parent))
                            .bounds(startX, backY, gridWidth, BTN_H)
                            .build()
            );
        }
    }

    // 根据可用宽度自适应列数（1 ~ MAX_COLUMNS）
    private int computeColumns() {
        int columns = (this.width - 2 * SIDE_MARGIN + GAP_X) / (BTN_W + GAP_X);
        return Math.clamp(columns, 1, MAX_COLUMNS);
    }

    private void refreshAllButtons() {
        List<SymAbstractFunction> functions = FeatureConfig.AUTO_ALL;
        for (int i = 0; i < featureButtons.size() && i < functions.size(); i++) {
            featureButtons.get(i).setMessage(buildLabel(functions.get(i)));
        }
    }

    private Component buildLabel(SymAbstractFunction func) {
        String mark = func.isEnable() ? "§a开§r " : "§c关§r ";
        return Component.literal(mark + func.getName());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        // 标题居中
        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, 0xFFFFFF, true);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}