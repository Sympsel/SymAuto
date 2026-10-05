package com.symauto.function.functions;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.function.abstracts.SymAbstractFunction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class AutoSellEmcFunction extends SymAbstractFunction {
    public static final AutoSellEmcFunction INSTANCE = new AutoSellEmcFunction();

    private enum State {
        // 未开始 / 已结束
        IDLE,
        // 已发出 Shift+F，等待服务器主菜单出现
        WAIT_MAIN_MENU,
        // 主菜单已打开，准备点 21 号槽
        MAIN_MENU,
        // 已点 21，等待商店菜单出现
        WAIT_SHOPS_MENU,
        // 商店菜单已打开，准备点 31 号槽
        SHOPS_MENU,
        // 已点 31，等待 EMC 商店菜单出现
        WAIT_EMC_SHOP_MENU,
        // EMC 商店菜单已打开，准备点 53 号槽
        EMC_SHOP_MENU,
        // 已点 53，等待 EMC 出售菜单出现
        WAIT_SELL_MENU,
        // 出售菜单已打开，循环点 49 号槽
        SELL_MENU
    }
    // 步骤间等待
    private static final int STEP_DELAY_TICKS = 6;
    // 出售循环间隔 10s
    private static final int SELL_INTERVAL_TICKS = 200;
    // 等待菜单超时 5s
    private static final int MENU_TIMEOUT_TICKS = 100;
    // 鼠标左键
    private static final int MOUSE_LEFT = 0;

    // 各步骤对应的槽位
    private static final int SLOT_TO_SHOPS = 21;
    private static final int SLOT_TO_EMC_SHOP = 31;
    private static final int SLOT_TO_SELL = 53;
    private static final int SLOT_SELL_CONFIRM = 49;

    private State state = State.IDLE;

    // 通用步骤延迟
    private int delay = 0;
    // 出售循环计时
    private int sellTimer = 0;
    // 等待菜单计时（用于超时重置）
    private int menuTimer = 0;

    private AutoSellEmcFunction() {
        super("auto_sell_emc", "自动 EMC 出售",
                "小水果服务器专用，Shift + F 打开商店后，自动进入 EMC 出售并每 10 秒出售一次");
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            reset();
            return;
        }

        // 任何非商店界面出现时中止，避免误操作聊天框等
        if (client.gui.screen() != null && !isShopMenuOpen(client)) {
            reset();
            return;
        }

        switch (state) {
            case IDLE -> start(client);
            case WAIT_MAIN_MENU -> waitForMenu(client, State.MAIN_MENU);
            case MAIN_MENU -> doStep(client, SLOT_TO_SHOPS, State.WAIT_SHOPS_MENU);
            case WAIT_SHOPS_MENU -> waitForMenu(client, State.SHOPS_MENU);
            case SHOPS_MENU -> doStep(client, SLOT_TO_EMC_SHOP, State.WAIT_EMC_SHOP_MENU);
            case WAIT_EMC_SHOP_MENU -> waitForMenu(client, State.EMC_SHOP_MENU);
            case EMC_SHOP_MENU -> doStep(client, SLOT_TO_SELL, State.WAIT_SELL_MENU);
            case WAIT_SELL_MENU -> waitForMenu(client, State.SELL_MENU);
            case SELL_MENU -> tickSell(client);
        }
    }

    /**
     * 如果没开菜单就发 Shift+F；如果已开就直接进入主菜单状态
     */
    private void start(Minecraft client) {
        if (client.gui.screen() == null) {
            openShop();
            state = State.WAIT_MAIN_MENU;
            menuTimer = 0;
        } else if (isShopMenuOpen(client)) {
            state = State.MAIN_MENU;
            delay = STEP_DELAY_TICKS;
        } else {
            reset();
        }
    }

    /**
     *  等待某个菜单打开，超时则重置
     */
    private void waitForMenu(Minecraft client, State next) {
        menuTimer++;
        if (menuTimer > MENU_TIMEOUT_TICKS) {
            reset();
            return;
        }
        if (isShopMenuOpen(client)) {
            state = next;
            delay = STEP_DELAY_TICKS;
            menuTimer = 0;
        }
    }

    /** 延迟后点击指定槽位，然后跳转到下一状态 */
    private void doStep(Minecraft client, int slot, State next) {
        if (delay > 0) {
            delay--;
            return;
        }
        clickSlot(client, slot);
        state = next;
        menuTimer = 0;
    }

    /** 出售循环：每 SELL_INTERVAL_TICKS 点一次 49 号槽 */
    private void tickSell(Minecraft client) {
        // 出售界面若被关闭，直接结束
        if (!isShopMenuOpen(client)) {
            reset();
            return;
        }
        sellTimer++;
        if (sellTimer >= SELL_INTERVAL_TICKS) {
            sellTimer = 0;
            clickSlot(client, SLOT_SELL_CONFIRM);
        }
    }

    private boolean isShopMenuOpen(Minecraft client) {
        AbstractContainerMenu menu = client.player != null ? client.player.containerMenu : null;
        return menu != null && menu.slots.size() >= 54;
    }

    private void openShop() {
        InputConstants.Key shiftKey =
                InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT);
        InputConstants.Key fKey =
                InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);

        KeyMapping.set(shiftKey, true);
        KeyMapping.set(fKey, true);
        KeyMapping.click(fKey);
        KeyMapping.set(fKey, false);
        KeyMapping.set(shiftKey, false);
    }

    private void clickSlot(Minecraft client, int slotIndex) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        AbstractContainerMenu menu = client.player.containerMenu;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return;
        }
        client.gameMode.handleContainerInput(
                menu.containerId,
                slotIndex,
                MOUSE_LEFT,
                ContainerInput.PICKUP,
                client.player
        );
    }

    private void reset() {
        state = State.IDLE;
        delay = 0;
        sellTimer = 0;
        menuTimer = 0;
    }

    @Override
    protected void onDisable() {
        reset();
    }
}