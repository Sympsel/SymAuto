package com.symauto.gui.screen.function;

import com.symauto.gui.base.SymBaseScreen;
import com.symauto.gui.widget.SimpleScrollbar;
import com.symauto.storage.KeepInvPresetStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class AutoDiscardMoreThingManagerScreen extends SymBaseScreen {
    private final Screen parent;

    private static final int LIST_TOP = 42;
    private static final int ROW_H = 20;
    private static final int BOTTOM_MARGIN = 26;

    private static final int W_ENABLE = 26;
    private static final int W_NAME = 110;
    private static final int W_INPUT = 34;
    private static final int W_APPLY = 30;
    private static final int W_GUARD = 44;
    private static final int W_DEL = 46;
    private static final int GAP = 2;

    private int scrollRow = 0;
    private int visibleRows = 1;

    private final SimpleScrollbar scrollbar = new SimpleScrollbar(new SimpleScrollbar.ScrollTarget() {
        @Override
        public int totalRows() {
            return KeepInvPresetStore.size();
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
            scrollRow = v;
        }
    });

    public AutoDiscardMoreThingManagerScreen(Screen parent) {
        this(parent, 0);
    }

    private AutoDiscardMoreThingManagerScreen(Screen parent, int scrollRow) {
        super(Component.literal("自动丢弃过多物品 · 预设管理器").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
        this.scrollRow = scrollRow;
    }

    @Override
    protected void init() {
        super.init();

        int contentBottom = this.height - BOTTOM_MARGIN;
        visibleRows = Math.max(1, (contentBottom - LIST_TOP) / (ROW_H + GAP));
        int total = KeepInvPresetStore.size();
        int maxScroll = Math.max(0, total - visibleRows);
        scrollRow = Math.clamp(scrollRow, 0, maxScroll);
        scrollbar.setBounds(this.width - 12, LIST_TOP - 4, this.height - BOTTOM_MARGIN + 2);

        int y = LIST_TOP;
        for (int i = scrollRow; i < total && i < scrollRow + visibleRows; i++) {
            buildRow(i, y);
            y += ROW_H + GAP;
        }

        int btnY = this.height - 20;
        addRenderableWidget(Button.builder(Component.literal("+ 新增手持物品"), b -> {
            KeepInvPresetStore.addFromHeld(currentHeld());
            scrollRow = Math.max(0, KeepInvPresetStore.size() - visibleRows);
            reopen();
        }).bounds(6, btnY, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("清空"), b -> {
            KeepInvPresetStore.clearAll();
            scrollRow = 0;
            reopen();
        }).bounds(102, btnY, 40, 20).build());

        addRenderableWidget(Button.builder(Component.literal("返回"), b ->
                this.minecraft.gui.setScreen(parent)).bounds(this.width - 56, btnY, 50, 20).build());
    }

    private void buildRow(int index, int y) {
        boolean active = KeepInvPresetStore.isActive(index);
        addRenderableWidget(Button.builder(
                        Component.literal(active ? "启用" : "禁用")
                                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED),
                        b -> {
                            KeepInvPresetStore.setActive(index, !active);
                            reopen();
                        }).bounds(6, y, W_ENABLE, ROW_H)
                .tooltip(Tooltip.create(Component.literal("点击切换该预设是否生效")))
                .build());

        String label = KeepInvPresetStore.displayName(index) + " · " + KeepInvPresetStore.modeName(index);

        addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    KeepInvPresetStore.toggleMode(index);
                    reopen();
                }).bounds(6 + W_ENABLE + GAP, y, W_NAME, ROW_H)
                .tooltip(Tooltip.create(Component.literal("点击切换制约模式：数量 → 槽位 → 两者")))
                .build());

        int x = 6 + W_ENABLE + GAP + W_NAME + GAP;
        int inputY = y + (ROW_H - 16) / 2;

        EditBox countInput = new EditBox(this.font, x, inputY, W_INPUT, 16, Component.literal("数量"));
        String countVal = String.valueOf(KeepInvPresetStore.maxCount(index));
        countInput.setHint(Component.literal(countVal));
        countInput.setTooltip(Tooltip.create(Component.literal("最多保留的数量，当前 " + countVal)));
        countInput.setMaxLength(6);
        x += W_INPUT + GAP;

        final EditBox countRef = countInput;
        addRenderableWidget(Button.builder(Component.literal("确认"), b -> {
            try {
                if (KeepInvPresetStore.setMaxCount(index, Integer.parseInt(countRef.getValue().trim()))) {
                    reopen();
                }
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x, y, W_APPLY, ROW_H)
                .tooltip(Tooltip.create(Component.literal("应用输入框的数量上限")))
                .build());
        x += W_APPLY + GAP;

        EditBox slotInput = new EditBox(this.font, x, inputY, W_INPUT, 16, Component.literal("槽位"));
        String slotVal = String.valueOf(KeepInvPresetStore.maxSlot(index));
        slotInput.setHint(Component.literal(slotVal));
        slotInput.setTooltip(Tooltip.create(Component.literal("最多占用的槽位，当前 " + slotVal)));
        slotInput.setMaxLength(6);
        x += W_INPUT + GAP;

        final EditBox slotRef = slotInput;
        addRenderableWidget(Button.builder(Component.literal("确认"), b -> {
            try {
                if (KeepInvPresetStore.setMaxSlot(index, Integer.parseInt(slotRef.getValue().trim()))) {
                    reopen();
                }
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x, y, W_APPLY, ROW_H)
                .tooltip(Tooltip.create(Component.literal("应用输入框的槽位上限")))
                .build());
        x += W_APPLY + GAP;

        boolean guard = KeepInvPresetStore.isGuard(index);
        addRenderableWidget(Button.builder(
                Component.literal("附魔" + (guard ? "保护" : "不管"))
                        .withStyle(guard ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                b -> {
                    KeepInvPresetStore.toggleGuard(index);
                    reopen();
                }).bounds(x, y, W_GUARD, ROW_H)
                .tooltip(Tooltip.create(Component.literal("是否保护带附魔的同类物品不被丢弃")))
                .build());
        x += W_GUARD + GAP;

        addRenderableWidget(Button.builder(Component.literal("删除预设").withStyle(ChatFormatting.RED), b -> {
            KeepInvPresetStore.remove(index);
            reopen();
        }).bounds(x, y, W_DEL, ROW_H).build());

        addRenderableWidget(countInput);
        addRenderableWidget(slotInput);
    }

    private void reopen() {
        this.minecraft.gui.setScreen(new AutoDiscardMoreThingManagerScreen(this.parent, this.scrollRow));
    }

    private ItemStack currentHeld() {
        Minecraft client = this.minecraft;
        if (client.player == null) return ItemStack.EMPTY;
        return client.player.getInventory().getSelectedItem();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(4, LIST_TOP - 4, this.width - 4, this.height - BOTTOM_MARGIN + 2, 0x66000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderTitle(graphics);

        if (KeepInvPresetStore.size() == 0) {
            String empty = "暂无预设，手持目标物品后点击底部“+ 新增手持物品”";
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, LIST_TOP + 4, style.muted, false);
        }
        scrollbar.render(graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (scrollbar.mouseClicked(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbar.mouseDragged(event.y())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollbar.mouseReleased()) {
            reopen();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, KeepInvPresetStore.size() - visibleRows);
        if (scrollY != 0 && maxScroll > 0) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY) * 2L, 0, maxScroll);
            reopen();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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