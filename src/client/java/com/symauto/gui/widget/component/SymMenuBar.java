package com.symauto.gui.widget.component;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.symauto.gui.utils.ColorConstants;
import com.symauto.gui.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

public class SymMenuBar extends AbstractWidget {
    public record Entry(Component label, @Nullable Component tooltip, Runnable action, BooleanSupplier selected) {
        public Entry(Component label, Runnable action) {
            this(label, null, action, () -> false);
        }

        public Entry(Component label, @Nullable Component tooltip, Runnable action) {
            this(label, tooltip, action, () -> false);
        }

        public Entry withSelected(BooleanSupplier selected) {
            return new Entry(this.label, this.tooltip, this.action, selected);
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

    private final Font font = Minecraft.getInstance().font;
    private final List<Menu> menus;
    private Component barTitle;
    private final int[] itemWidths;
    private int openMenu = -1;

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

        int hoveredItem = itemAt(mouseX, mouseY);
        if (hoveredItem >= 0) {
            this.openMenu = hoveredItem;
        } else if (this.openMenu >= 0 && !inDropdown(mouseX, mouseY)) {
            this.openMenu = -1;
        }
        for (int i = 0; i < this.menus.size(); i++) {
            int x0 = itemX(i);
            if (i == this.openMenu || i == hoveredItem) {
                g.fill(x0, this.getY(), x0 + this.itemWidths[i], this.getY() + BAR_H, BAR_BG_ACTIVE);
            }
            g.text(this.font, this.menus.get(i).title(), x0 + ITEM_PAD_X, this.getY() + (BAR_H - 8) / 2, TEXT, false);
        }

        int hoveredEntry = entryAt(mouseX, mouseY);
        if (this.openMenu >= 0) {
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
                if (i == hoveredEntry) {
                    g.fill(dx + 1, ey, dx + dw - 1, ey + ENTRY_H, DROPDOWN_HOVER);
                } else if (e.selected().getAsBoolean()) {
                    g.fill(dx + 1, ey, dx + dw - 1, ey + ENTRY_H, SELECTED_BG);
                }
                g.text(this.font, e.label(), dx + ITEM_PAD_X, ey + (ENTRY_H - 8) / 2, TEXT, false);
                if (i == hoveredEntry && e.tooltip() != null) {
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
