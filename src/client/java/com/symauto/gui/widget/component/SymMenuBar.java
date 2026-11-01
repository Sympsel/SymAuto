package com.symauto.gui.widget.component;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.symauto.gui.utils.ColorConstants;
import com.symauto.gui.utils.ColorUtils;
import com.symauto.gui.widget.model.SymHoverState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SymMenuBar extends AbstractWidget {
    public record Entry(Component label, @Nullable Component tooltip, Runnable action,
                        BooleanSupplier selected, @Nullable Consumer<Boolean> hover) {
        public Entry(Component label, Runnable action) {
            this(label, null, action, () -> false, null);
        }

        public Entry(Component label, @Nullable Component tooltip, Runnable action) {
            this(label, tooltip, action, () -> false, null);
        }

        public Entry withSelected(BooleanSupplier selected) {
            return new Entry(this.label, this.tooltip, this.action, selected, this.hover);
        }
        /**
         * 鼠标进入/离开该条目时回调，参数为 true 表示进入；false 表示离开
         */
        public Entry withHover(@Nullable Consumer<Boolean> hover) {
            return new Entry(this.label, this.tooltip, this.action, this.selected, hover);
        }
    }

    public record Menu(Component title, List<Entry> entries) {
    }

    public static final int BAR_H = 18;
    private static final int ITEM_PAD_X = 8;
    private static final int ENTRY_H = 16;
    private static final int DROPDOWN_MIN_W = 64;
    private static final int TOOLTIP_W = 200;

    private static final int BAR_BG = 0x001B2028 | ColorConstants.SEMI_ALPHA;
    private static final int BAR_BG_ACTIVE = ColorUtils.onHover(BAR_BG);

    private static final int DROPDOWN_BG = 0xE6101418;
    private static final int DROPDOWN_BORDER = 0x66FFFFFF;
    private static final int DROPDOWN_HOVER = 0x40FFFFFF;
    private static final int SELECTED_BG = 0x402F6FD0;
    private static final int TEXT = 0xF0FFFFFF;
    private static final int BAR_TITLE = 0xB0FFFFFF;

    private static final SymHoverState.Listener NOOP_LISTENER = new SymHoverState.Listener() {
    };

    private final Font font = Minecraft.getInstance().font;
    private final List<Menu> menus;
    private Component barTitle;
    private final int[] itemWidths;
    private int openMenu = -1;

    /**
     * 每个顶层菜单项一个 hover 状态
     */
    private final List<SymHoverState> itemHoverStates = new ArrayList<>();

    /**
     * 每个下拉条目一个 hover 状态，外层按菜单索引分组
     */
    private final List<List<SymHoverState>> entryHoverStates = new ArrayList<>();

    public SymMenuBar(Component barTitle, List<Menu> menus) {
        super(0, 0, Minecraft.getInstance().getWindow().getGuiScaledWidth(), BAR_H,
                Component.literal("菜单栏"));
        this.barTitle = barTitle;
        this.menus = menus;
        this.itemWidths = new int[menus.size()];
        Font f = Minecraft.getInstance().font;
        for (int i = 0; i < menus.size(); i++) {
            this.itemWidths[i] = f.width(menus.get(i).title().getString()) + ITEM_PAD_X * 2;
        }
        initHoverStates();
    }

    public SymMenuBar(List<Menu> menus) {
        super(0, 0, Minecraft.getInstance().getWindow().getGuiScaledWidth(), BAR_H,
                Component.literal("菜单栏"));
        this.menus = menus;
        this.itemWidths = new int[menus.size()];
        Font f = Minecraft.getInstance().font;
        for (int i = 0; i < menus.size(); i++) {
            this.itemWidths[i] = f.width(menus.get(i).title().getString()) + ITEM_PAD_X * 2;
        }
        initHoverStates();
    }

    private void initHoverStates() {
        this.itemHoverStates.clear();
        this.entryHoverStates.clear();

        for (Menu menu : this.menus) {
            this.itemHoverStates.add(new SymHoverState(NOOP_LISTENER));

            List<SymHoverState> states = new ArrayList<>();
            for (int j = 0; j < menu.entries().size(); j++) {
                final Entry entry = menu.entries().get(j);
                states.add(new SymHoverState(new SymHoverState.Listener() {
                    @Override
                    public void onEnter(AbstractWidget widget, double mouseX, double mouseY) {
                        if (entry.hover() != null) {
                            entry.hover().accept(true);
                        }
                    }

                    @Override
                    public void onExit(AbstractWidget widget, double mouseX, double mouseY) {
                        if (entry.hover() != null) {
                            entry.hover().accept(false);
                        }
                    }
                }));
            }
            this.entryHoverStates.add(states);
        }
    }

    /**
     * 只更新 hover 状态
     */
    public void updateHoverStates(int mouseX, int mouseY) {
        if (!this.visible) {
            this.clearHoverStates();
            return;
        }

        int hoveredItem = itemAt(mouseX, mouseY);

        // 先更新顶层菜单 hover 状态
        for (int i = 0; i < this.itemHoverStates.size(); i++) {
            this.itemHoverStates.get(i).update(i == hoveredItem, this, mouseX, mouseY);
        }

        // 保留原来的菜单展开/关闭规则
        if (hoveredItem >= 0) {
            this.openMenu = hoveredItem;
        } else if (this.openMenu >= 0 && !inDropdown(mouseX, mouseY)) {
            this.openMenu = -1;
        }

        // 再根据当前展开的菜单更新所有条目 hover 状态。
        // 未展开菜单的条目也会 update(false)，保证切菜单时旧状态被清除。
        int hoveredEntryIndex = entryAt(mouseX, mouseY);
        for (int menuIndex = 0; menuIndex < this.entryHoverStates.size(); menuIndex++) {
            List<SymHoverState> states = this.entryHoverStates.get(menuIndex);
            for (int entryIndex = 0; entryIndex < states.size(); entryIndex++) {
                boolean hovered = menuIndex == this.openMenu && entryIndex == hoveredEntryIndex;
                states.get(entryIndex).update(hovered, this, mouseX, mouseY);
            }
        }
    }

    /**
     * 清空所有 hover 状态并关闭菜单，用于菜单栏隐藏或界面重建。
     */
    public void clearHoverStates() {
        for (SymHoverState state : this.itemHoverStates) {
            state.update(false, this, 0, 0);
        }
        for (List<SymHoverState> states : this.entryHoverStates) {
            for (SymHoverState state : states) {
                state.update(false, this, 0, 0);
            }
        }
        this.openMenu = -1;
    }

    private int itemX(int index) {
        int x = this.getX();
        for (int i = 0; i < index; i++) {
            x += this.itemWidths[i];
        }
        return x;
    }

    private int itemAt(int mx, int my) {
        if (my < this.getY() || my >= this.getY() + BAR_H) {
            return -1;
        }
        for (int i = 0; i < this.itemWidths.length; i++) {
            int x0 = itemX(i);
            if (mx >= x0 && mx < x0 + this.itemWidths[i]) {
                return i;
            }
        }
        return -1;
    }

    private int dropdownWidth() {
        int w = DROPDOWN_MIN_W;
        for (Entry e : this.menus.get(this.openMenu).entries()) {
            w = Math.max(w, this.font.width(e.label().getString()) + ITEM_PAD_X * 2 + 8);
        }
        return w;
    }

    private int dropdownX() {
        return itemX(this.openMenu);
    }

    private int dropdownY() {
        return this.getY() + BAR_H;
    }

    private int dropdownHeight() {
        return this.menus.get(this.openMenu).entries().size() * ENTRY_H + 2;
    }

    private boolean inDropdown(int mx, int my) {
        return this.openMenu >= 0
                && mx >= dropdownX() && mx < dropdownX() + dropdownWidth()
                && my >= dropdownY() && my < dropdownY() + dropdownHeight();
    }

    private int entryAt(int mx, int my) {
        if (!inDropdown(mx, my)) {
            return -1;
        }
        int e = (my - dropdownY()) / ENTRY_H;
        return e < this.menus.get(this.openMenu).entries().size() ? e : -1;
    }

    /**
     * 指定坐标是否被展开的菜单模态区域覆盖
     */
    public boolean blocksHoverAt(double x, double y) {
        if (this.openMenu < 0) {
            return false;
        }
        if (x >= this.getX() && x < this.getX() + this.getWidth()
                && y >= this.getY() && y < this.getY() + BAR_H) {
            return true;
        }
        return inDropdown((int) x, (int) y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int item = itemAt(mx, my);
        if (item >= 0) {
            this.openMenu = item;
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        int entry = entryAt(mx, my);
        if (entry >= 0) {
            this.menus.get(this.openMenu).entries().get(entry).action().run();
            this.openMenu = -1;
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (this.openMenu >= 0) {
            // 菜单展开时点空白
            this.openMenu = -1;
            return true;
        }
        return false;
    }

    /**
     * 菜单展开期间把命中放宽为全屏
     */
    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return this.openMenu >= 0 || super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + BAR_H, BAR_BG);

        if (this.barTitle != null) {
            int tw = this.font.width(this.barTitle);
            g.text(this.font, this.barTitle,
                    this.getX() + this.getWidth() - ITEM_PAD_X - tw,
                    this.getY() + (BAR_H - 8) / 2,
                    BAR_TITLE, false);
        }

        this.updateHoverStates(mouseX, mouseY);
        int hoveredItem = itemAt(mouseX, mouseY);

        for (int i = 0; i < this.menus.size(); i++) {
            int x0 = itemX(i);
            if (i == this.openMenu || this.itemHoverStates.get(i).isHovered()) {
                g.fill(x0, this.getY(), x0 + this.itemWidths[i], this.getY() + BAR_H, BAR_BG_ACTIVE);
            }
            g.text(this.font, this.menus.get(i).title(), x0 + ITEM_PAD_X,
                    this.getY() + (BAR_H - 8) / 2, TEXT, false);
        }

        int hoveredEntry = -1;
        if (this.openMenu >= 0) {
            List<SymHoverState> states = this.entryHoverStates.get(this.openMenu);
            for (int i = 0; i < states.size(); i++) {
                if (states.get(i).isHovered()) {
                    hoveredEntry = i;
                    break;
                }
            }

            int dx = dropdownX();
            int dy = dropdownY();
            int dw = dropdownWidth();
            int dh = dropdownHeight();
            g.fill(dx, dy, dx + dw, dy + dh, DROPDOWN_BG);
            g.outline(dx, dy, dw, dh, DROPDOWN_BORDER);
            List<Entry> entries = this.menus.get(this.openMenu).entries();
            for (int i = 0; i < entries.size(); ++i) {
                int ey = dy + 1 + i * ENTRY_H;
                Entry e = entries.get(i);
                boolean entryHovered = states.get(i).isHovered();
                if (entryHovered) {
                    g.fill(dx + 1, ey, dx + dw - 1, ey + ENTRY_H, DROPDOWN_HOVER);
                } else if (e.selected().getAsBoolean()) {
                    g.fill(dx + 1, ey, dx + dw - 1, ey + ENTRY_H, SELECTED_BG);
                }
                g.text(this.font, e.label(), dx + ITEM_PAD_X, ey + (ENTRY_H - 8) / 2, TEXT, false);
                if (entryHovered && e.tooltip() != null) {
                    SymTooltip.offer(this.font.split(e.tooltip(), TOOLTIP_W), mouseX, mouseY);
                }
            }
        }

        if (hoveredItem >= 0 || hoveredEntry >= 0) {
            g.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}
