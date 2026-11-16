package com.symauto.function.oneclick;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.config.SymAutoKeys;
import com.symbase.function.base.SymAbstractFunction;
import com.symauto.core.Constants;
import com.symauto.function.util.ContainerScreenUtils;
import com.symbase.function.util.Tooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class OneClickFlatItemsFunction extends SymAbstractFunction {
    public static final OneClickFlatItemsFunction INSTANCE = new OneClickFlatItemsFunction();

    private final KeyMapping key;

    private OneClickFlatItemsFunction() {
        super("one_click_flat_items", "一键平铺物品",
                Tooltip.create()
                        .line("鼠标悬浮在要平铺的物品上，按下 Ctrl + A 进行平铺")
                        .line("在容器界面则铺满容器")
                        .line("在背包界面则铺满背包")
                        .line(ChatFormatting.AQUA, "可以用来快速合成块状物品")
                        .toString()
        );
        this.key = new KeyMapping(
                "key.symauto.one_click_flat_items",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                SymAutoKeys.CATEGORY_MAIN);
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    @Override
    public KeyMapping getKeyMapping() {
        return key;
    }

    @Override
    public boolean requireCtrl() {
        return true;
    }

    @Override
    public ScreenContext requireScreenContext() {
        return ScreenContext.CONTAINER_SCREEN;
    }

    @Override
    public void onKeyAction(Minecraft client) {
        if (client.gui.screen() instanceof AbstractContainerScreen<?> cs) {
            flatItems(client, cs);
        }
    }

    /**
     * 容器界面内的按键拦截。
     *
     * <p>界面打开时原版不会向 KeyMapping 派发点击事件，{@code consumeClick()} 永远为空，
     * 因此该功能只能通过按键事件分发直接调用到这里（见 {@code ContainerScreenKeyMixin}）</p>
     */
    @Override
    public boolean allowContainerKeyPress(Minecraft client,
                                          AbstractContainerScreen<?> screen,
                                          int keyCode, int scanCode, int modifiers) {
        return !flatItems(client, screen);
    }

    /**
     * 把鼠标悬浮的整组物品逐个铺到区域内每个空槽位。
     *
     * @return 是否真的执行了平铺
     */
    private boolean flatItems(Minecraft client, AbstractContainerScreen<?> cs) {
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        AbstractContainerMenu menu = cs.getMenu();
        if (!menu.getCarried().isEmpty()) {
            return false;
        }
        Slot hovered = ContainerScreenUtils.findSlotUnderMouse(client, cs);

        if (hovered == null || !hovered.hasItem()) {
            return false;
        }

        int regionStart, regionEnd;

        boolean inventoryMenu = (menu == client.player.inventoryMenu);
        if (inventoryMenu) {
            regionStart = Constants.MAIN_INVENTORY_START;
            regionEnd = Constants.HOTBAR_END_IN_INVENTORY;
        } else {
            regionStart = 0;
            regionEnd = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = regionStart; i < regionEnd; i++) {
            if (i == hovered.index) {
                // PICKUP 后一定为空槽
                slots.add(i);
                continue;
            }
            if (menu.getSlot(i).getItem().isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return false;
        }

        int cid = menu.containerId;

        client.gameMode.handleContainerInput(cid, hovered.index, 0, ContainerInput.PICKUP, client.player);

        for (int idx : slots) {
            if (menu.getCarried().isEmpty()) {
                break;
            }
            Slot s = menu.getSlot(idx);
            if (s.getItem().isEmpty()) {
                client.gameMode.handleContainerInput(cid, idx, 1, ContainerInput.PICKUP, client.player);
            }
        }
        return true;
    }
}


