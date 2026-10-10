package com.symauto.function.functions;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.KeyUtils;
import com.symauto.function.utils.Tooltip;
import com.symauto.mixin.MerchantScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import org.lwjgl.glfw.GLFW;

public class OneClickTradeFunction extends SymAbstractFunction {
    public static final OneClickTradeFunction INSTANCE = new OneClickTradeFunction();

    private static final int RESULT_SLOT = 2;
    private static final int LEFT_BUTTON = 0;

    private static final int RESULT_TIMEOUT_TICKS = 20;
    private static final int CLEAR_TIMEOUT_TICKS = 10;
    private static final int REPOP_TIMEOUT_TICKS = 8;

    private enum State {IDLE, SELECT, WAIT_RESULT, TRADE, WAIT_CLEAR, WAIT_REPOP}

    private State state = State.IDLE;
    private boolean wasSpaceDown = false;
    private int waitTicks = 0;

    private OneClickTradeFunction() {
        super("one_click_trade", "一键交易", Tooltip.create()
                .line("选中需要交易的栏目，空格补货并交易此项直到缺货或背包满，支持长按")
                .toString());
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            resetState();
            return;
        }
        if (!(client.gui.screen() instanceof MerchantScreen merchantScreen)) {
            resetState();
            return;
        }
        handleKeyPress(client);
        advanceStateMachine(client, merchantScreen);
    }

    private void handleKeyPress(Minecraft client) {
        boolean spaceDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_SPACE);
        if (spaceDown && !wasSpaceDown && state == State.IDLE) {
            state = State.SELECT;
            waitTicks = 0;
        }
        wasSpaceDown = spaceDown;
    }

    /**
     * 批量交易是否应保持：仅看空格是否仍被按住（含 Shift 兜底，但不依赖 Shift，规避输入法吞键）
     */
    private boolean isTradeAllHeld(Minecraft client) {
        return InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_SPACE)
                || KeyUtils.isShiftDown(client);
    }

    private void advanceStateMachine(Minecraft client, MerchantScreen screen) {
        if (client.player == null || client.gameMode == null || state == State.IDLE) {
            return;
        }
        MerchantMenu menu = screen.getMenu();
        int containerId = menu.containerId;
        boolean resultHasItem = menu.getSlot(RESULT_SLOT).hasItem();

        switch (state) {
            case SELECT -> {
                int shopItem = ((MerchantScreenAccessor) screen).symauto$getShopItem();
                if (shopItem < 0 || shopItem >= menu.getOffers().size()) {
                    resetState();
                    return;
                }
                ClientPacketListener connection = client.getConnection();
                if (connection == null) {
                    resetState();
                    return;
                }
                menu.setSelectionHint(shopItem);
                menu.tryMoveItems(shopItem);
                connection.send(new ServerboundSelectTradePacket(shopItem));
                state = State.WAIT_RESULT;
                waitTicks = 0;
            }
            case WAIT_RESULT -> {
                if (resultHasItem) {
                    state = State.TRADE;
                } else if (++waitTicks >= RESULT_TIMEOUT_TICKS) {
                    resetState(); // 材料耗尽 / 售罄，补不出货
                }
            }
            case TRADE -> {
                client.gameMode.handleContainerInput(
                        containerId, RESULT_SLOT, LEFT_BUTTON,
                        ContainerInput.QUICK_MOVE, client.player);
                state = State.WAIT_CLEAR;
                waitTicks = 0;
            }
            case WAIT_CLEAR -> {
                if (!resultHasItem) {
                    // 结果被取走 = 本笔交易完成
                    if (isTradeAllHeld(client)) {
                        state = State.WAIT_REPOP;
                        waitTicks = 0;
                    } else {
                        resetState();
                    }
                } else if (++waitTicks >= CLEAR_TIMEOUT_TICKS) {
                    resetState();
                }
            }
            case WAIT_REPOP -> {
                if (resultHasItem) {
                    state = State.TRADE;
                } else if (++waitTicks >= REPOP_TIMEOUT_TICKS) {
                    if (isTradeAllHeld(client)) {
                        state = State.SELECT;
                        waitTicks = 0;
                    } else {
                        resetState();
                    }
                }
            }
            default -> resetState();
        }
    }

    private void resetState() {
        state = State.IDLE;
        waitTicks = 0;
    }

    @Override
    protected void onDisable() {
        resetState();
    }
}