package com.betterbundle.gui;

import com.betterbundle.sort.exec.SortStateMachine;
import com.betterbundle.sort.model.BundleSnapshotBuilder;
import com.betterbundle.sort.model.InventoryModel;
import com.betterbundle.sort.plan.ConstrainedTbfdPlanner;
import com.betterbundle.sort.plan.PlanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/** 「一键整理」按钮 + 进度展示（无遮罩、不锁 UI）。 */
public final class SortButton {

    private static final int BUTTON_WIDTH = 45;
    private static final int BUTTON_HEIGHT = 12;

    private SortButton() {}

    private static int buttonX(int leftPos) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        return panelX + BundlePanelRenderer.PADDING + BundlePanelRenderer.CAT_BAR_WIDTH + 2
                + BundlePanelRenderer.SCROLL_BAR_WIDTH + 2;
    }

    private static int buttonY(int topPos, int imageHeight) {
        int searchH = BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3;
        int gridH = BundlePanelRenderer.PADDING * 2
                + BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        int panelHeight = Math.min(imageHeight, searchH + gridH) + 16;
        return topPos + panelHeight - 13;
    }

    public static boolean isInside(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        int x = buttonX(leftPos);
        int y = buttonY(topPos, imageHeight);
        return mouseX >= x && mouseX < x + BUTTON_WIDTH
                && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
    }

    /** 处理按钮点击。返回 true 表示事件已被消化。 */
    public static boolean handleClick(int leftPos, int topPos, int imageHeight,
                                      double mouseX, double mouseY) {
        if (!isInside(mouseX, mouseY, leftPos, topPos, imageHeight)) return false;

        SortStateMachine fsm = SortStateMachine.get();
        if (fsm.isRunning()) {
            fsm.abortByUser();
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return true;

        InventoryModel model = BundleSnapshotBuilder.build(player);
        PlanResult result = new ConstrainedTbfdPlanner().plan(model);
        if (!result.isOk()) {
            setError(result.error());
            return true;
        }
        fsm.start(result.plan());
        return true;
    }

    private static String errorMessage = "";
    private static int errorTicks;

    public static void setError(String message) {
        errorMessage = message == null ? "" : message;
        errorTicks = 60;
    }

    public static void tick() {
        if (errorTicks > 0) errorTicks--;
    }

    public static void render(GuiGraphicsExtractor graphics, Font font,
                              int leftPos, int topPos, int imageHeight,
                              int mouseX, int mouseY) {
        int x = buttonX(leftPos);
        int y = buttonY(topPos, imageHeight);
        SortStateMachine fsm = SortStateMachine.get();

        String label;
        int bg;
        if (fsm.isRunning()) {
            int done = fsm.progressDone();
            int total = Math.max(1, fsm.progressTotal());
            int filled = (BUTTON_WIDTH - 2) * Math.min(done, total) / total;
            bg = 0x80000000;
            graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bg);
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + BUTTON_HEIGHT - 1, 0xFF3C6E3C);
            label = done + "/" + total;
        } else {
            boolean hovered = mouseX >= x && mouseX < x + BUTTON_WIDTH
                    && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
            bg = hovered ? 0x60FFFFFF : 0x40FFFFFF;
            graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bg);
            label = "一键整理";
        }

        String status = fsm.message();
        if (!status.isEmpty()) label = status;
        else if (errorTicks > 0 && !errorMessage.isEmpty()) label = errorMessage;

        int textW = font.width(label);
        int textColor = errorTicks > 0 ? 0xFFFF8080 : 0xFFFFFFFF;
        graphics.text(font, label, x + Math.max(2, (BUTTON_WIDTH - textW) / 2),
                y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, textColor, false);
    }
}
