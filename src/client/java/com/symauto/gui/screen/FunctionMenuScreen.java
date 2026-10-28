package com.symauto.gui.screen;

import com.symauto.config.ConfigManager;
import com.symauto.core.SymFunctionTags;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.KeyUtils;
import com.symauto.gui.UiStyle;
import com.symauto.gui.utils.ColorUtils;
import com.symauto.gui.widget.component.SymMenuBar;
import com.symauto.gui.widget.component.SymTooltip;
import com.symauto.gui.widget.utils.ButtonFactory;
import com.symauto.gui.widget.component.SymButton;
import com.symauto.gui.widget.component.SymScrollbar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FunctionMenuScreen extends Screen {
    private final Screen parent;
    /**
     * 当前选中的分类标签；SymFunctionTags.ALL(-1) 表示全部
     */
    private final int activeTag;

    private static final int TAG_TEXT = 0xFF8A8A8A;

    /**
     * 缓存：记住最后一次选中的分类
     */
    private static int cachedTag = SymFunctionTags.ALL;

    private static final int BTN_W = 120;
    private static final int BTN_H = 20;
    private static final int GAP_X = 6;
    private static final int GAP_Y = 5;

    private static final int SIDE_MARGIN = 12;
    private static final int MAX_COLUMNS = 3;

    private static final int SCROLLBAR_GAP = 3;
    private static final int BOTTOM_GAP = 14;

    private final List<SymButton> functionButtons = new ArrayList<>();
    private List<SymAbstractFunction> currentFunctions = List.of();

    private UiStyle style;
    private SymMenuBar menuBar;

    // 列表布局与滚动状态
    private int columns = 1;
    private int listLeft = 0;
    private int listTop = SymMenuBar.BAR_H + GAP_Y;
    private int listWidth = 0;
    private int listHeight = 0;
    private int visibleRows = 1;
    private int totalRows = 0;
    private int scrollRow = 0;

    private final int[] tagIds;
    private final int count;

    private final SymScrollbar scrollbar = new SymScrollbar(new SymScrollbar.ScrollTarget() {
        @Override
        public int totalRows() {
            return totalRows;
        }

        @Override
        public int visibleRows() {
            return visibleRows;
        }

        @Override
        public int scrollRow() {
            return scrollRow;
        }

        @Override
        public void scrollRow(int v) {
            scrollRow = Math.clamp(v, 0, maxScrollRow());
        }
    });

    public FunctionMenuScreen(Screen parent) {
        this(parent, cachedTag, UiStyle.DEFAULT);
    }

    public FunctionMenuScreen(Screen parent, UiStyle style) {
        this(parent, cachedTag, style);
    }

    public FunctionMenuScreen(Screen parent, int activeTag, UiStyle style) {
        super(Component.literal("SymAuto 功能菜单")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        setStyle(style);
        this.parent = parent;
        this.activeTag = activeTag;
        cachedTag = activeTag;

        count = 1 + SymFunctionTags.COUNT;
        tagIds = new int[count];
        tagIds[0] = SymFunctionTags.ALL;
        for (int i = 0; i < SymFunctionTags.COUNT; ++i) {
            tagIds[i + 1] = i;
        }
    }

    public FunctionMenuScreen setStyle(UiStyle style) {
        this.style = style;
        return this;
    }

    @Override
    protected void init() {
        functionButtons.clear();

        // 功能区顶部
        this.listTop = SymMenuBar.BAR_H + GAP_Y;

        List<SymAbstractFunction> functions = filter(activeTag);
        this.currentFunctions = functions;

        columns = computeColumns();
        listWidth = columns * BTN_W + (columns - 1) * GAP_X;
        listLeft = (this.width - listWidth) / 2;

        int step = BTN_H + GAP_Y;

        int available = this.height - listTop - BOTTOM_GAP;
        listHeight = Math.max(step, available);

        visibleRows = Math.max(1, (listHeight + GAP_Y) / step);
        totalRows = (functions.size() + columns - 1) / columns;

        int maxScrollRow = Math.max(0, totalRows - visibleRows);
        scrollRow = Math.clamp(scrollRow, 0, maxScrollRow);

        for (int i = 0; i < functions.size(); i++) {
            final SymAbstractFunction func = functions.get(i);
            int col = i % columns;
            int x = listLeft + col * (BTN_W + GAP_X);

            SymButton btn = ButtonFactory.function(buildLabel(func), x, rowToY(i / columns), BTN_W, BTN_H)
                    .tooltip(Component.literal(func.getTooltip()))
                    .hoverBarrier(this::menuCovers)
                    .onLeftClick(() -> {
                        func.toggle();
                        ConfigManager.save();
                        refreshAllButtons();
                    });

            functionButtons.add(btn);
            this.addRenderableWidget(btn);
        }
        initMenuBar();
        scrollbar.setBounds(
                Math.min(listLeft + listWidth + SCROLLBAR_GAP, this.width - style.scrollbarWidth - 1),
                listTop, listTop + listHeight);
    }

    private boolean menuCovers(double x, double y) {
        return this.menuBar != null && this.menuBar.blocksHoverAt(x, y);
    }

    private void initMenuBar() {
        List<SymMenuBar.Entry> functionEntries = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            final int tag = tagIds[i];
            functionEntries.add(new SymMenuBar.Entry(tagText(tag),
                    () -> this.minecraft.gui.setScreen(new FunctionMenuScreen(this.parent, tag, this.style))
            ).withSelected(() -> tag == activeTag));
        }

        List<SymMenuBar.Entry> openEntries = new ArrayList<>();
        openEntries.add(new SymMenuBar.Entry(Component.literal("打开主配置文件夹"),
                ConfigManager::openConfigFolder));

        List<SymMenuBar.Entry> helpEntries = new ArrayList<>();
        helpEntries.add(new SymMenuBar.Entry(Component.literal("指令用法"),
                () -> this.minecraft.gui.setScreen(new CommandUsageScreen(this))));

        List<SymMenuBar.Entry> aboutEntries = new ArrayList<>();
        aboutEntries.add(new SymMenuBar.Entry(Component.literal("更新日志"),
                () -> this.minecraft.gui.setScreen(new UpdateLogScreen(this))));
        aboutEntries.add(new SymMenuBar.Entry(Component.literal("声明"),
                () -> this.minecraft.gui.setScreen(new ClaimScreen(this))));

        this.menuBar = new SymMenuBar(this.title, List.of(
                new SymMenuBar.Menu(Component.literal("功能"), functionEntries),
                new SymMenuBar.Menu(Component.literal("打开"), openEntries),
                new SymMenuBar.Menu(Component.literal("帮助"), helpEntries),
                new SymMenuBar.Menu(Component.literal("关于"), aboutEntries)
        ));
        this.addRenderableWidget(menuBar);
    }

    private Component tagText(int tag) {
        boolean active = (tag == activeTag);
        int color = active ? ColorUtils.onActive(TAG_TEXT) : TAG_TEXT;
        return Component.literal(SymFunctionTags.getTagName(tag))
                .withStyle(s -> s.withColor(color));
    }

    private static List<SymAbstractFunction> filter(int tag) {
        if (tag == SymFunctionTags.ALL) {
            return FunctionConfig.AUTO_ALL;
        }
        List<SymAbstractFunction> result = new ArrayList<>();
        for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
            if (f.hasTag(tag)) {
                result.add(f);
            }
        }
        return result;
    }

    /**
     * 把「内容行」映射为可见行 Y
     */
    private int rowToY(int row) {
        int displayRow = row - scrollRow;
        if (displayRow < 0 || displayRow >= visibleRows) {
            return -1000;
        }
        return listTop + displayRow * (BTN_H + GAP_Y);
    }

    /**
     * 根据当前 scrollRow 重新摆放所有功能按钮
     */
    private void relayoutFeatureButtons() {
        for (int i = 0; i < functionButtons.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = listLeft + col * (BTN_W + GAP_X);
            functionButtons.get(i).setPosition(x, rowToY(row));
        }
    }

    private int maxScrollRow() {
        return Math.max(0, totalRows - visibleRows);
    }

    // 根据可用宽度自适应列数
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
        if (this.menuBar != null && this.menuBar.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (scrollbar.mouseClicked(event.x(), event.y())) {
            return true;
        }
        SymAbstractFunction f = findFunctionAt(event.x(), event.y());
        if (f != null) {
            // Shift + 左键：编辑该功能的分类归属
            if (event.button() == 0 && KeyUtils.isShiftDown(this.minecraft)) {
                this.minecraft.gui.setScreen(new FunctionTagEditScreen(this, f));
                return true;
            }
            // 中键：打开专用管理界面
            if (event.button() == 2 && f.hasConfigScreen()) {
                Screen screen = f.createConfigScreen(this);
                if (screen != null) {
                    this.minecraft.gui.setScreen(screen);
                    return true;
                }
            }
            // 右键：优先配置菜单，其次打开专用管理界面
            if (event.button() == 1) {
                if (f.hasConfig()) {
                    this.minecraft.gui.setScreen(new ConfigMenuScreen(f, this));
                    return true;
                }
                if (f.hasConfigScreen()) {
                    Screen screen = f.createConfigScreen(this);
                    if (screen != null) {
                        this.minecraft.gui.setScreen(screen);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbar.mouseDragged(event.y())) {
            relayoutFeatureButtons(); // 拖拽实时重排
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return scrollbar.mouseReleased() || super.mouseReleased(event);
    }

    private SymAbstractFunction findFunctionAt(double mx, double my) {
        for (int i = 0; i < functionButtons.size() && i < currentFunctions.size(); ++i) {
            SymButton b = functionButtons.get(i);
            if (mx >= b.getX() && mx <= b.getX() + b.getWidth()
                    && my >= b.getY() && my <= b.getY() + b.getHeight()) {
                return currentFunctions.get(i);
            }
        }
        return null;
    }

    private void refreshAllButtons() {
        for (int i = 0; i < functionButtons.size() && i < currentFunctions.size(); i++) {
            functionButtons.get(i).setMessage(buildLabel(currentFunctions.get(i)));
        }
    }

    private Component buildLabel(SymAbstractFunction func) {
        String mark = func.isEnable() ? "§a开§r " : "§c关§r ";
        return Component.literal(mark + func.getName());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(listLeft - 3, listTop - 3, listLeft + listWidth + 3, listTop + listHeight + 3, 0x66000000);

        super.extractRenderState(g, mouseX, mouseY, delta);

        // 空分类提示
        String tip = "Shift+左键 调整功能分类";
        g.text(this.font, tip, this.width - this.font.width(tip) - 6,
                listTop + listHeight + 5, this.style.muted, false);

        if (currentFunctions.isEmpty()) {
            String empty = "该分类下暂无功能";
            g.text(this.font, empty, (this.width - this.font.width(empty)) / 2, listTop, this.style.muted, false);
        }

        g.outline(listLeft - 3, listTop - 3, listWidth + 6, listHeight + 6, this.style.panelOutline);
        scrollbar.render(g);
        SymTooltip.renderAndClear(g, this.font, this.width, this.height);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}