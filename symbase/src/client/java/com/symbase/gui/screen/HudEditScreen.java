package com.symbase.gui.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.symbase.function.utils.KeyUtils;
import com.symbase.gui.widget.component.SymButton;
import com.symbase.gui.widget.component.SymDialog;
import com.symbase.gui.widget.component.SymMenuBar;
import com.symbase.gui.widget.component.SymTooltip;
import com.symbase.gui.widget.utils.ButtonFactory;
import com.symbase.hud.HudBounds;
import com.symbase.hud.HudElement;
import com.symbase.hud.HudManager;
import com.symbase.hud.HudPlacement;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通用 HUD 编辑界面
 */
public final class HudEditScreen extends Screen {
    private static final int TOP_H = 26;
    private static final int BOTTOM_H = 26;
    private static final int SIDEBAR_W = 172;
    private static final int SIDEBAR_MARGIN = 8;
    private static final int SIDEBAR_INFO_H = 100;

    private static final int GRID_SIZE = 8;
    private static final int SNAP_DISTANCE = 5;
    private static final int HIT_PADDING = 2;

    private static final int BOX_SIZE = 10;

    private static final int PANEL_BG = 0xE6101418;
    private static final int PANEL_BORDER = 0x66FFFFFF;
    private static final int TEXT = 0xF0FFFFFF;
    private static final int MUTED = 0xFF9AA4AE;
    private static final int ACCENT = 0xFFFFC857;
    private static final int GUIDE = 0x99FFC857;
    private static final int ELEMENT_OUTLINE = 0x33FFFFFF;
    private static final int HIDDEN_OUTLINE = 0xAAFF6B6B;
    private static final int GRID_LINE = 0x10FFFFFF;
    private static final int GRID_AXIS = 0x22FFFFFF;

    private final Screen parent;
    private final Map<String, HudPlacement> draft = new LinkedHashMap<>();
    private final Map<String, HudPlacement> original = new LinkedHashMap<>();
    private final List<SymButton> buttons = new ArrayList<>();
    private final List<Integer> guidesX = new ArrayList<>();
    private final List<Integer> guidesY = new ArrayList<>();

    private String selectedId;
    private boolean dragging;
    private int grabX;
    private int grabY;

    private int boxX;
    private int boxY;

    private boolean showGrid = true;
    private boolean snapEnabled = true;
    private boolean chromeVisible = true;

    @Getter
    private SymDialog helpDialog;

    private SymButton saveButton;
    private SymButton cancelButton;
    private SymButton gridButton;
    private SymButton snapButton;

    private SymMenuBar menuBar;

    /**
     * 当前被菜单条目“间接悬浮”的 HUD 元素 ID
     */
    private String hoveredMenuElemId;

    public HudEditScreen(@Nullable Screen parent) {
        super(Component.literal("HUD 编辑").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;

        for (HudElement element : HudManager.elements()) {
            HudPlacement placement = HudManager.placement(element).copy();
            draft.put(element.id(), placement);
            original.put(element.id(), placement.copy());
            if (selectedId == null && placement.visible()) {
                selectedId = element.id();
            }
        }
        if (selectedId == null && !draft.isEmpty()) {
            selectedId = draft.keySet().iterator().next();
        }
    }

    @Override
    protected void init() {
        HudManager.setEditing(true);
        buttons.clear();

        int y = this.height - BOTTOM_H + 4;
        saveButton = ButtonFactory.function(Component.literal("保存"),
                        this.width - SIDEBAR_MARGIN - 52, y, 52, 18)
                .onLeftClick(this::save)
                .tooltip(Component.literal("保存布局并返回（Ctrl+S）"));
        addEditorButton(saveButton);

        cancelButton = ButtonFactory.navigation(Component.literal("取消"),
                        this.width - SIDEBAR_MARGIN - 52 - 4 - 52, y, 52, 18)
                .onLeftClick(this::cancel)
                .tooltip(Component.literal("放弃本次改动并返回（Esc）"));
        addEditorButton(cancelButton);

        gridButton = ButtonFactory.toggle(Component.literal("网格 开"), 8, y, 52, 18, showGrid)
                .onLeftClick(() -> {
                    showGrid = !showGrid;
                    updateToggleLabels();
                })
                .tooltip(Component.literal("显示网格并吸附到 8px 网格（拖动时按住 Shift 临时关闭）"));
        addEditorButton(gridButton);

        snapButton = ButtonFactory.toggle(Component.literal("吸附 开"), 64, y, 52, 18, snapEnabled)
                .onLeftClick(() -> {
                    snapEnabled = !snapEnabled;
                    updateToggleLabels();
                })
                .tooltip(Component.literal("与其他 HUD 元素及屏幕边缘对齐吸附"));
        addEditorButton(snapButton);

        updateToggleLabels();
        setChromeVisible(chromeVisible);

        helpDialog = new SymDialog(160, 120,
                Component
                        .literal("""
                                鼠标左键拖动元素
                                方向键微调
                                Ctrl + 滚轮缩放
                                Shift + 无吸附拖动
                                Tab 显示/隐藏编辑面板
                                Ctrl + S 保存并退出""")
                , this);
        helpDialog.setEnsureBtn(ButtonFactory.normal(Component.literal("确定"), 0, 0, 40, 20)
                .onLeftClick(() -> helpDialog.close())).setModal(true);
        helpDialog.close();
        initMenuBar();
    }

    private boolean isModalDialogOpen() {
        return helpDialog != null && helpDialog.isOpen() && helpDialog.isModal();
    }

    private void initMenuBar() {
        SymMenuBar.Entry hudEntry = new SymMenuBar.Entry(Component.literal("HUD 编辑"),
                () -> this.minecraft.gui.setScreen(new HudEditScreen(null)));
        SymModMenuRegistry.INSTANCE.registerMenuEntry(hudEntry).setCurrOpenMenu(hudEntry);
        hoveredMenuElemId = null;
        List<SymMenuBar.Entry> elemEntries = new ArrayList<>();
        for (HudElement elem : HudManager.elements()) {
            elemEntries.add(new SymMenuBar.Entry(Component.literal(elem.displayName()),
                    () -> select(elem.id()))
                    .withHover(hovered -> {
                        if (hovered) {
                            hoveredMenuElemId = elem.id();
                        } else if (elem.id().equals(hoveredMenuElemId)) {
                            hoveredMenuElemId = null;
                        }
                    }));
        }
        List<SymMenuBar.Entry> controlEntries = new ArrayList<>();
        controlEntries.add(
                new SymMenuBar.Entry(Component.literal("重置全部"),
                        Component.literal("把所有 HUD 元素恢复到默认布局")
                        , this::resetAll));
        controlEntries.add(new SymMenuBar.Entry(Component.literal("重置选中"),
                Component.literal("把当前选中的 HUD 元素恢复到默认位置与缩放"),
                this::resetSelected));

        List<SymMenuBar.Entry> helpEntries = new ArrayList<>();
        helpEntries.add(new SymMenuBar.Entry(Component.literal("操作说明"),
                () -> helpDialog.open()));
        helpEntries.add(new SymMenuBar.Entry(Component.literal("SymHud 更新日志"),
                () -> this.minecraft.gui.setScreen(
                        new UpdateLogScreen(this, "symhud", Component.literal("SymHud 更新日志")))));
        this.menuBar = new SymMenuBar(this.title, List.of(
                SymModMenuRegistry.INSTANCE.getSymModMenu(),
                new SymMenuBar.Menu(Component.literal("元素"), elemEntries),
                new SymMenuBar.Menu(Component.literal("控制"), controlEntries),
                new SymMenuBar.Menu(Component.literal("帮助"), helpEntries)
        ));
        this.addRenderableWidget(menuBar);
    }

    private void addEditorButton(SymButton button) {
        buttons.add(button);
        this.addRenderableWidget(button);
    }

    private void updateToggleLabels() {
        if (gridButton != null) {
            gridButton.setMessage(Component.literal("网格 " + (showGrid ? "开" : "关")));
            gridButton.state(showGrid);
        }
        if (snapButton != null) {
            snapButton.setMessage(Component.literal("吸附 " + (snapEnabled ? "开" : "关")));
            snapButton.state(snapEnabled);
        }
    }

    private void setChromeVisible(boolean visible) {
        chromeVisible = visible;
        for (SymButton button : buttons) {
            button.visible = visible;
        }
    }

    private void save() {
        HudManager.apply(draft);
        HudManager.save();
        this.minecraft.gui.setScreen(parent);
    }

    private void cancel() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        HudManager.setEditing(false);
        super.removed();
    }

    /**
     * 编辑时暂停单人游戏
     */
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fillGradient(0, 0, this.width, this.height, 0x66000000, 0x99000000);
        if (showGrid) {
            drawGrid(g);
        }

        if (chromeVisible) {
            // 顶/底栏背景先画，保证拖到屏幕边缘的元素仍然可见；侧栏后画以遮住列表下的元素
            drawTopBottomBackgrounds(g);
        }

        drawPreview(g);
        drawGuides(g);

        // 菜单条目 hover 需要先于 HUD 元素轮廓更新，避免一帧延迟
        if (menuBar != null) {
            menuBar.updateHoverStates(mouseX, mouseY);
        }

        drawElementFrames(g, mouseX, mouseY);

        if (chromeVisible) {
            drawSidebarBackground(g);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);

        if (chromeVisible) {
            drawChromeContent(g);
        } else {
            drawHiddenChromeHint(g);
        }

        if (helpDialog != null && helpDialog.isOpen()) {
            if (helpDialog.isModal()) {
                g.fill(0, 0, this.width, this.height, 0x99000000);
            }
            helpDialog.extractRenderState(g, mouseX, mouseY, delta);
        }
        SymTooltip.renderAndClear(g, this.font, this.width, this.height);
    }

    private void drawGrid(GuiGraphicsExtractor g) {
        for (int x = 0; x < this.width; x += 16) {
            g.fill(x, 0, x + 1, this.height, GRID_LINE);
        }
        for (int y = 0; y < this.height; y += 16) {
            g.fill(0, y, this.width, y + 1, GRID_LINE);
        }
        g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, GRID_AXIS);
        g.fill(0, this.height / 2, this.width, this.height / 2 + 1, GRID_AXIS);
    }

    private void drawPreview(GuiGraphicsExtractor g) {
        for (HudElement element : HudManager.elements()) {
            HudPlacement placement = draft.get(element.id());
            if (placement == null || !placement.visible()) {
                continue;
            }
            HudManager.renderElement(g, this.font, element, placement, this.width, this.height, true);
        }
    }

    private void drawGuides(GuiGraphicsExtractor g) {
        for (int x : guidesX) {
            g.fill(x, 0, x + 1, this.height, GUIDE);
        }
        for (int y : guidesY) {
            g.fill(0, y, this.width, y + 1, GUIDE);
        }
    }

    private void drawElementFrames(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean menuCovers = menuBar != null && menuBar.blocksHoverAt(mouseX, mouseY);
        String hoveredId = dragging ? selectedId : (menuCovers ? null : hitTest(mouseX, mouseY));
        if (!dragging && hoveredId == null) {
            hoveredId = hoveredMenuElemId;
        }

        for (HudElement element : HudManager.elements()) {
            HudPlacement placement = draft.get(element.id());
            if (placement == null) {
                continue;
            }
            HudBounds bounds = placement.resolve(this.font, element, this.width, this.height);
            boolean selected = element.id().equals(selectedId);
            boolean hovered = element.id().equals(hoveredId);

            if (placement.visible()) {
                int color = selected ? ACCENT : hovered ? 0x99FFFFFF : ELEMENT_OUTLINE;
                g.outline(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, color);
            } else if (selected || hovered) {
                g.outline(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, HIDDEN_OUTLINE);
            }
        }

        if (dragging || hoveredId != null) {
            g.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }

    private void drawTopBottomBackgrounds(GuiGraphicsExtractor g) {
        g.fill(0, this.height - BOTTOM_H, this.width, this.height, PANEL_BG);
        g.fill(0, this.height - BOTTOM_H, this.width, this.height - BOTTOM_H + 1, 0x44FFFFFF);
    }

    private void drawSidebarBackground(GuiGraphicsExtractor g) {
        int left = sidebarX();
        int top = sidebarY();
        int bottom = sidebarBottom();
        if (bottom > top) {
            g.fill(left, top, left + SIDEBAR_W, bottom, PANEL_BG);
            g.outline(left, top, SIDEBAR_W, bottom - top, PANEL_BORDER);
        }
    }

    private void drawChromeContent(GuiGraphicsExtractor graphics) {
        drawSidebarContent(graphics);

        if (HudManager.elements().isEmpty()) {
            String empty = "暂无已注册的 HUD 元素";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2,
                    this.height / 2, MUTED, false);
        }
    }

    private void drawSidebarContent(GuiGraphicsExtractor g) {
        int left = sidebarX();
        int top = sidebarY();
        int lineBegin = 6 + top;
        int lineH = 12;
        int bottom = sidebarBottom();
        if (bottom <= top) {
            return;
        }

        HudElement selected = selectedElement();
        HudPlacement selectedPlacement = selected == null ? null : draft.get(selected.id());
        if (selected != null) {
            g.text(this.font, selected.displayName(), left + 8, lineBegin, TEXT, true);
            String count = String.valueOf(HudManager.elements().size());
            g.text(this.font, count, left + SIDEBAR_W - 8 - this.font.width(count), lineBegin, MUTED, false);
            if (selectedPlacement != null) {
                HudBounds bounds = bounds(selected);
                g.text(this.font, fit("ID: " + selected.id(), SIDEBAR_W - 16), left + 8, lineBegin + lineH, MUTED, false);
                g.text(this.font, fit("位置: " + selectedPlacement.anchor().label(), SIDEBAR_W - 16), left + 8, lineBegin + lineH * 2, MUTED, false);
                g.text(this.font, fit("坐标: [" + bounds.x() + "," + bounds.y() + "]", SIDEBAR_W - 16), left + 8, lineBegin + lineH * 3, MUTED, false);
                g.text(this.font, fit("尺寸: " + String.format(Locale.ROOT, "%.2f", selectedPlacement.scale()) + "x", SIDEBAR_W - 16), left + 8, lineBegin + lineH * 4, MUTED, false);
            }

            boxX = left + 30;
            boxY = lineBegin + lineH * 5 - 1;
            boolean visible = selectedPlacement == null || selectedPlacement.visible();
            int eyeColor = visible ? 0xFF3FA36B : 0xFF333A42;
            g.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, eyeColor);
            g.outline(boxX, boxY, BOX_SIZE, BOX_SIZE, visible ? 0xFFB8F5D0 : 0xFF6A737D);
            if (visible) {
                int border = BOX_SIZE / 4 + 1;
                g.fill(boxX + border, boxY + border, boxX + BOX_SIZE - border, boxY + BOX_SIZE - border, 0xFF102018);
            }
            g.text(this.font, "显示", left + 8, lineBegin + lineH * 5, visible ? TEXT : MUTED, false);
            g.text(this.font, fit("描述: " + selected.description(), SIDEBAR_W - 16), left + 8, lineBegin + lineH * 6, MUTED, false);
        } else {
            g.text(this.font, "未选中元素", left + 8, lineBegin, TEXT, true);
        }
    }

    private void drawHiddenChromeHint(GuiGraphicsExtractor graphics) {
        String text = "按 Tab 显示编辑面板";
        int width = this.font.width(text) + 12;
        int x = (this.width - width) / 2;
        graphics.fill(x, 6, x + width, 22, 0x99101418);
        graphics.outline(x, 6, width, 16, PANEL_BORDER);
        graphics.text(this.font, text, x + 6, 10, TEXT, false);
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (isModalDialogOpen()) {
            helpDialog.mouseClicked(event, doubleClick);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        double mouseX = event.x();
        double mouseY = event.y();
        // 吃掉显示切换按钮点击
        if (inSideEye(mouseX, mouseY)) {
            HudPlacement placement = draft.get(selectedId);
            if (placement != null) {
                placement.setVisible(!placement.visible());
            }
            return true;
        }

        // 吃掉侧边栏点击
        if (chromeVisible && mouseX >= sidebarX() && mouseX <= sidebarX() + SIDEBAR_W
                && mouseY >= sidebarY() && mouseY <= sidebarBottom()) {
            return true;
        }

        if (event.button() != 0) {
            return false;
        }

        String hit = hitTest(mouseX, mouseY);
        if (hit != null) {
            select(hit);
            HudElement element = HudManager.element(hit);
            if (element != null) {
                HudBounds bounds = bounds(element);
                dragging = true;
                grabX = (int) Math.round(mouseX) - bounds.x();
                grabY = (int) Math.round(mouseY) - bounds.y();
                guidesX.clear();
                guidesY.clear();
            }
            return true;
        }

        selectedId = null;
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (isModalDialogOpen()) {
            return true;
        }
        if (!dragging || selectedId == null || event.button() != 0) {
            return super.mouseDragged(event, dragX, dragY);
        }
        HudElement element = HudManager.element(selectedId);
        HudPlacement placement = draft.get(selectedId);
        if (element == null || placement == null) {
            dragging = false;
            return true;
        }

        int width = placement.scaledWidth(this.font, element);
        int height = placement.scaledHeight(this.font, element);
        int x = (int) Math.round(event.x()) - grabX;
        int y = (int) Math.round(event.y()) - grabY;

        guidesX.clear();
        guidesY.clear();
        boolean snap = (showGrid || snapEnabled) && !KeyUtils.isShiftDown(this.minecraft);
        if (snap) {
            Snap snappedX = snapX(x, width, snapEnabled, showGrid);
            Snap snappedY = snapY(y, height, snapEnabled, showGrid);
            x = snappedX.position;
            y = snappedY.position;
            if (snappedX.guide >= 0) {
                guidesX.add(snappedX.guide);
            }
            if (snappedY.guide >= 0) {
                guidesY.add(snappedY.guide);
            }
        }

        x = Math.clamp(x, 0, Math.max(0, this.width - width));
        y = Math.clamp(y, 0, Math.max(0, this.height - height));
        placement.moveTo(x, y, width, height, this.width, this.height);
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (isModalDialogOpen()) {
            return true;
        }
        if (dragging && event.button() == 0) {
            dragging = false;
            guidesX.clear();
            guidesY.clear();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isModalDialogOpen()) {
            return true;
        }

        if (chromeVisible && inSidebar(mouseX, mouseY)) {
            return true;
        }

        if (selectedId != null && KeyUtils.isCtrlDown(this.minecraft) && scrollY != 0) {
            HudElement element = HudManager.element(selectedId);
            HudPlacement placement = draft.get(selectedId);
            if (element != null && placement != null) {
                float next = Math.round((placement.scale() + (float) Math.signum(scrollY) * 0.05F) * 100.0F) / 100.0F;
                placement.setScale(next);
                HudBounds bounds = placement.resolve(this.font, element, this.width, this.height);
                int x = Math.clamp(bounds.x(), 0, Math.max(0, this.width - bounds.width()));
                int y = Math.clamp(bounds.y(), 0, Math.max(0, this.height - bounds.height()));
                placement.moveTo(x, y, bounds.width(), bounds.height(), this.width, this.height);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (isModalDialogOpen()) {
            if (event.isEscape()) {
                helpDialog.close();
            }
            return true;
        }
        if (event.isEscape()) {
            cancel();
            return true;
        }
        if (event.hasControlDown() && event.input() == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        if (event.input() == GLFW.GLFW_KEY_TAB) {
            setChromeVisible(!chromeVisible);
            return true;
        }
        if (event.input() == GLFW.GLFW_KEY_LEFT_BRACKET) {
            cycleSelection(true);
            return true;
        }
        if (event.input() == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            cycleSelection(false);
            return true;
        }
        if (selectedId != null && (event.isLeft() || event.isRight() || event.isUp() || event.isDown())) {
            int step = event.hasShiftDown() ? 10 : 1;
            int dx = event.isLeft() ? -step : event.isRight() ? step : 0;
            int dy = event.isUp() ? -step : event.isDown() ? step : 0;
            nudge(dx, dy);
            return true;
        }
        return super.keyPressed(event);
    }

    private void resetSelected() {
        HudElement element = selectedElement();
        if (element == null) {
            return;
        }
        draft.put(element.id(), element.defaultPlacement());
        guidesX.clear();
        guidesY.clear();
    }

    private void resetAll() {
        for (HudElement element : HudManager.elements()) {
            draft.put(element.id(), element.defaultPlacement());
        }
        guidesX.clear();
        guidesY.clear();
    }

    private void nudge(int dx, int dy) {
        HudElement element = selectedElement();
        HudPlacement placement = selectedId == null ? null : draft.get(selectedId);
        if (element == null || placement == null) {
            return;
        }
        HudBounds bounds = placement.resolve(this.font, element, this.width, this.height);
        int x = Math.clamp(bounds.x() + dx, 0, Math.max(0, this.width - bounds.width()));
        int y = Math.clamp(bounds.y() + dy, 0, Math.max(0, this.height - bounds.height()));
        placement.moveTo(x, y, bounds.width(), bounds.height(), this.width, this.height);
        guidesX.clear();
        guidesY.clear();
    }

    private void cycleSelection(boolean reverse) {
        List<HudElement> elements = HudManager.elements();
        if (elements.isEmpty()) {
            return;
        }
        int index = indexOf(selectedId);
        int next;
        if (index < 0) {
            next = 0;
        } else {
            next = (index + (reverse ? -1 : 1) + elements.size()) % elements.size();
        }
        select(elements.get(next).id());
    }

    private void select(String id) {
        selectedId = id;
    }

    private boolean inSidebar(double mouseX, double mouseY) {
        return mouseX >= sidebarX() && mouseX < sidebarX() + SIDEBAR_W
                && mouseY >= sidebarY() && mouseY < sidebarBottom();
    }

    private boolean inSideEye(double mouseX, double mouseY) {
        return mouseX >= boxX && mouseX < boxX + 10
                && mouseY >= boxY && mouseY < boxY + 10;
    }

    private @Nullable String hitTest(double mouseX, double mouseY) {
        if (chromeVisible && inSidebar(mouseX, mouseY)) {
            return null;
        }
        List<HudElement> elements = HudManager.elements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            HudPlacement placement = draft.get(element.id());
            if (placement == null) {
                continue;
            }
            if (!placement.visible() && !element.id().equals(selectedId)) {
                continue;
            }
            HudBounds bounds = placement.resolve(this.font, element, this.width, this.height);
            if (bounds.contains(mouseX, mouseY, HIT_PADDING)) {
                return element.id();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 吸附
    // ------------------------------------------------------------------

    private Snap snapX(int x, int width, boolean align, boolean grid) {
        if (align) {
            int bestAbs = SNAP_DISTANCE + 1;
            int bestDelta = 0;
            int bestGuide = -1;
            int[] features = {x, x + width / 2, x + width};
            List<Integer> targets = new ArrayList<>();
            targets.add(0);
            targets.add(this.width / 2);
            targets.add(this.width);
            for (HudElement other : HudManager.elements()) {
                if (other.id().equals(selectedId)) {
                    continue;
                }
                HudPlacement placement = draft.get(other.id());
                if (placement == null || !placement.visible()) {
                    continue;
                }
                HudBounds bounds = placement.resolve(this.font, other, this.width, this.height);
                targets.add(bounds.x());
                targets.add(bounds.centerX());
                targets.add(bounds.right());
            }
            for (int target : targets) {
                for (int feature : features) {
                    int delta = target - feature;
                    int abs = Math.abs(delta);
                    if (abs <= SNAP_DISTANCE && abs < bestAbs) {
                        bestAbs = abs;
                        bestDelta = delta;
                        bestGuide = target;
                    }
                }
            }
            if (bestGuide >= 0) {
                return new Snap(x + bestDelta, bestGuide);
            }
        }
        if (grid) {
            return new Snap(Math.round(x / (float) GRID_SIZE) * GRID_SIZE, -1);
        }
        return new Snap(x, -1);
    }

    private Snap snapY(int y, int height, boolean align, boolean grid) {
        if (align) {
            int bestAbs = SNAP_DISTANCE + 1;
            int bestDelta = 0;
            int bestGuide = -1;
            int[] features = {y, y + height / 2, y + height};
            List<Integer> targets = new ArrayList<>();
            targets.add(0);
            targets.add(this.height / 2);
            targets.add(this.height);
            for (HudElement other : HudManager.elements()) {
                if (other.id().equals(selectedId)) {
                    continue;
                }
                HudPlacement placement = draft.get(other.id());
                if (placement == null || !placement.visible()) {
                    continue;
                }
                HudBounds bounds = placement.resolve(this.font, other, this.width, this.height);
                targets.add(bounds.y());
                targets.add(bounds.centerY());
                targets.add(bounds.bottom());
            }
            for (int target : targets) {
                for (int feature : features) {
                    int delta = target - feature;
                    int abs = Math.abs(delta);
                    if (abs <= SNAP_DISTANCE && abs < bestAbs) {
                        bestAbs = abs;
                        bestDelta = delta;
                        bestGuide = target;
                    }
                }
            }
            if (bestGuide >= 0) {
                return new Snap(y + bestDelta, bestGuide);
            }
        }
        if (grid) {
            return new Snap(Math.round(y / (float) GRID_SIZE) * GRID_SIZE, -1);
        }
        return new Snap(y, -1);
    }

    private record Snap(int position, int guide) {
    }

    // ------------------------------------------------------------------
    // 侧栏 / 工具
    // ------------------------------------------------------------------

    private int sidebarX() {
        return this.width - SIDEBAR_W - SIDEBAR_MARGIN;
    }

    private int sidebarY() {
        return TOP_H + 4;
    }

    private int sidebarBottom() {
        return Math.min(sidebarY() + SIDEBAR_INFO_H, this.height - BOTTOM_H - 4);
    }


    private HudElement selectedElement() {
        return selectedId == null ? null : HudManager.element(selectedId);
    }

    private int indexOf(String id) {
        List<HudElement> elements = HudManager.elements();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private HudBounds bounds(HudElement element) {
        HudPlacement placement = draft.get(element.id());
        if (placement == null) {
            return new HudBounds(0, 0, 1, 1);
        }
        return placement.resolve(this.font, element, this.width, this.height);
    }

    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int budget = maxWidth - this.font.width(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = builder.toString() + text.charAt(i);
            if (this.font.width(next) > budget) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    @Override
    public void onClose() {
        if (isModalDialogOpen()) {
            helpDialog.close();
            return;
        }
        cancel();
    }
}


