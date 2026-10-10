package com.symauto.gui;

import com.symauto.config.ConfigManager;
import com.symauto.entity.SymFunctionTags;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FeatureMenuScreen extends Screen {
    private final Screen parent;
    /** 当前选中的分类标签；SymFunctionTags.ALL(-1) 表示全部。 */
    private final int activeTag;

    private static final int TITLE_COLOR = 0xFFFFFFFF;

    private static final int BTN_W = 120;
    private static final int BTN_H = 20;
    private static final int GAP_X = 6;
    private static final int GAP_Y = 5;

    private static final int SIDE_MARGIN = 12;
    private static final int TITLE_Y = 10;
    private static final int TAG_BAR_Y = 24;
    private static final int TAG_BTN_H = 16;
    private static final int TAG_GAP = 4;
    private static final int MAX_COLUMNS = 3;

    // 固定高度列表（可滚动区域）
    private static final int LIST_MAX_HEIGHT = 180;
    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_GAP = 3;

    private final List<Button> featureButtons = new ArrayList<>();
    private List<SymAbstractFunction> currentFunctions = List.of();

    // 列表布局与滚动状态
    private int columns = 1;
    private int listLeft = 0;
    private int listTop = TAG_BAR_Y + TAG_BTN_H + GAP_Y;
    private int listWidth = 0;
    private int listHeight = LIST_MAX_HEIGHT;
    private int visibleRows = 1;
    private int totalRows = 0;
    private int scrollRow = 0;

    public FeatureMenuScreen(Screen parent) {
        this(parent, SymFunctionTags.ALL);
    }

    public FeatureMenuScreen(Screen parent, int activeTag) {
        super(Component.literal("SymAuto 功能菜单")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
        this.activeTag = activeTag;
    }

    @Override
    protected void init() {
        featureButtons.clear();

        // 顶部标签栏（点击切换分类 → 重开界面并带上选中标签）
        buildTagBar();

        // 功能区顶部：位于标签栏下方
        this.listTop = TAG_BAR_Y + TAG_BTN_H + GAP_Y;

        List<SymAbstractFunction> functions = filter(activeTag);
        this.currentFunctions = functions;

        columns = computeColumns();
        listWidth = columns * BTN_W + (columns - 1) * GAP_X;
        listLeft = (this.width - listWidth) / 2;

        int step = BTN_H + GAP_Y;

        // 底部按钮行数：更新日志/指令用法共用一行，返回另占一行（仅在有 parent 时）
        int footerCount = 1 + (parent != null ? 1 : 0);
        int footerReserve = footerCount * step + GAP_Y;

        int available = this.height - listTop - footerReserve - 8;
        listHeight = Math.clamp(available, step, LIST_MAX_HEIGHT);

        visibleRows = Math.max(1, (listHeight + GAP_Y) / step);
        totalRows = (functions.size() + columns - 1) / columns;

        int maxScrollRow = Math.max(0, totalRows - visibleRows);
        scrollRow = Math.clamp(scrollRow, 0, maxScrollRow);

        for (int i = 0; i < functions.size(); i++) {
            final SymAbstractFunction func = functions.get(i);
            int col = i % columns;
            int x = listLeft + col * (BTN_W + GAP_X);

            Button btn = Button.builder(buildLabel(func), b -> {
                        func.toggle();
                        ConfigManager.save();
                        refreshAllButtons();
                    }).tooltip(Tooltip.create(Component.literal(func.getTooltip())))
                    .bounds(x, rowToY(i / columns), BTN_W, BTN_H)
                    .build();

            featureButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        // 底部按钮：自适应追加，共享游标 nextY
        int nextY = listTop + listHeight + GAP_Y;

        int halfW = (listWidth - GAP_X) / 2;
        this.addRenderableWidget(
                Button.builder(Component.literal("更新日志"), b ->
                                this.minecraft.gui.setScreen(new UpdateLogScreen(this)))
                        .bounds(listLeft, nextY, halfW, BTN_H)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.literal("指令用法"), b ->
                                this.minecraft.gui.setScreen(new CommandUsageScreen(this)))
                        .bounds(listLeft + halfW + GAP_X, nextY, halfW, BTN_H)
                        .build()
        );
        nextY += step;

        if (parent != null) {
            this.addRenderableWidget(
                    Button.builder(Component.literal("返回"), b -> this.minecraft.gui.setScreen(parent))
                            .bounds(listLeft, nextY, listWidth, BTN_H)
                            .build()
            );
        }
    }

    /** 构建顶部标签栏：「全部」+ 各分类，居中排布。 */
    private void buildTagBar() {
        int count = 1 + SymFunctionTags.COUNT;
        int[] tagIds = new int[count];
        tagIds[0] = SymFunctionTags.ALL;
        for (int i = 0; i < SymFunctionTags.COUNT; i++) {
            tagIds[i + 1] = i;
        }

        int[] widths = new int[count];
        int total = 0;
        for (int i = 0; i < count; i++) {
            widths[i] = this.font.width(SymFunctionTags.getTagName(tagIds[i])) + 12;
            total += widths[i];
        }
        total += TAG_GAP * (count - 1);

        int x = (this.width - total) / 2;
        for (int i = 0; i < count; i++) {
            final int tag = tagIds[i];
            this.addRenderableWidget(
                    Button.builder(Component.literal(tagText(tag)), b ->
                                    this.minecraft.gui.setScreen(new FeatureMenuScreen(this.parent, tag)))
                            .bounds(x, TAG_BAR_Y, widths[i], TAG_BTN_H)
                            .build()
            );
            x += widths[i] + TAG_GAP;
        }
    }

    private String tagText(int tag) {
        boolean active = (tag == activeTag);
        return active ? "§a[§r" + SymFunctionTags.getTagName(tag) + "§a]§r"
                : "§7" + SymFunctionTags.getTagName(tag);
    }

    private static List<SymAbstractFunction> filter(int tag) {
        if (tag == SymFunctionTags.ALL) {
            return FeatureConfig.AUTO_ALL;
        }
        List<SymAbstractFunction> result = new ArrayList<>();
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            if (f.hasTag(tag)) {
                result.add(f);
            }
        }
        return result;
    }

    /** 把「内容行」映射为可见行 Y；不在可视区的行移到屏幕外隐藏。 */
    private int rowToY(int row) {
        int displayRow = row - scrollRow;
        if (displayRow < 0 || displayRow >= visibleRows) {
            return -1000;
        }
        return listTop + displayRow * (BTN_H + GAP_Y);
    }

    /** 根据当前 scrollRow 重新摆放所有功能按钮。 */
    private void relayoutFeatureButtons() {
        for (int i = 0; i < featureButtons.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = listLeft + col * (BTN_W + GAP_X);
            featureButtons.get(i).setPosition(x, rowToY(row));
        }
    }

    private int maxScrollRow() {
        return Math.max(0, totalRows - visibleRows);
    }

    // 根据可用宽度自适应列数（1 ~ MAX_COLUMNS）
    private int computeColumns() {
        int columns = (this.width - 2 * SIDE_MARGIN + GAP_X) / (BTN_W + GAP_X);
        return Math.clamp(columns, 1, MAX_COLUMNS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScrollRow = maxScrollRow();
        if (scrollY != 0 && maxScrollRow > 0) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow);
            relayoutFeatureButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            SymAbstractFunction f = findConfigurableAt(event.x(), event.y());
            if (f != null) {
                this.minecraft.gui.setScreen(new ConfigMenuScreen(f, this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private SymAbstractFunction findConfigurableAt(double mx, double my) {
        for (int i = 0; i < featureButtons.size() && i < currentFunctions.size(); ++i) {
            Button b = featureButtons.get(i);
            if (mx >= b.getX() && mx <= b.getX() + b.getWidth()
                    && my >= b.getY() && my <= b.getY() + b.getHeight()) {
                SymAbstractFunction f = currentFunctions.get(i);
                return f.hasConfig() ? f : null;
            }
        }
        return null;
    }

    private void refreshAllButtons() {
        for (int i = 0; i < featureButtons.size() && i < currentFunctions.size(); i++) {
            featureButtons.get(i).setMessage(buildLabel(currentFunctions.get(i)));
        }
    }

    private Component buildLabel(SymAbstractFunction func) {
        String mark = func.isEnable() ? "§a开§r " : "§c关§r ";
        return Component.literal(mark + func.getName());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(listLeft - 3, listTop - 3, listLeft + listWidth + 3, listTop + listHeight + 3, 0x66000000);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (this.width - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, titleX, TITLE_Y, TITLE_COLOR, true);

        // 空分类提示
        if (currentFunctions.isEmpty()) {
            String empty = "该分类下暂无功能";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, listTop, 0xFFAAAAAA, false);
        }

        graphics.outline(listLeft - 3, listTop - 3, listWidth + 6, listHeight + 6, 0x80FFFFFF);

        drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        int maxScrollRow = maxScrollRow();
        if (maxScrollRow <= 0) {
            return;
        }

        int sbX = Math.min(listLeft + listWidth + SCROLLBAR_GAP, this.width - SCROLLBAR_W - 1);
        int trackTop = listTop;
        int trackBottom = listTop + listHeight;

        graphics.fill(sbX, trackTop, sbX + SCROLLBAR_W, trackBottom, 0x40FFFFFF);

        int trackH = trackBottom - trackTop;
        int thumbH = Math.max(8, trackH * visibleRows / Math.max(1, totalRows));
        int range = trackH - thumbH;
        int thumbY = trackTop + (int) ((long) range * scrollRow / maxScrollRow);
        graphics.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbH, 0x80FFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}