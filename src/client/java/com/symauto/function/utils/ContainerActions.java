package com.symauto.function.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/**
 * 客户端容器操作原语
 */
public class ContainerActions {
    // 虚槽位
    public static final int OUTSIDE = -999;

    private ContainerActions() {
    }

    /** 左键槽位 */
    public static void leftClickSlot(int slotId) {
        click(slotId, 0, ContainerInput.PICKUP);
    }

    /** 右键槽位 */
    public static void rightClickSlot(int slotId) {
        click(slotId, 1, ContainerInput.PICKUP);
    }

    /** 把光标整堆丢出 */
    public static void dropCarried() {
        click(OUTSIDE, 0, ContainerInput.PICKUP);
    }

    /** 把某槽位的整堆丢出 */
    public static void dropSlotStack(int slotId) {
        click(slotId, 1, ContainerInput.THROW);
    }

    /** 把某槽位丢出一件 */
    public static void dropSlotSingle(int slotId) {
        click(slotId, 0, ContainerInput.THROW);
    }

    /** Shift+左键，容器与背包间快速转移 */
    public static void quickMove(int slotId) {
        click(slotId, 0, ContainerInput.QUICK_MOVE);
    }

    /** 与快捷栏交换：hotbarButton 为 0-8 */
    public static void swapToHotbar(int slotId, int hotbarButton) {
        click(slotId, hotbarButton, ContainerInput.SWAP);
    }

    /** 左键槽位（显式指定菜单） */
    public static void leftClickSlot(AbstractContainerMenu menu, int slotId) {
        click(menu, slotId, 0, ContainerInput.PICKUP);
    }

    /** 把光标整堆丢出（显式指定菜单） */
    public static void dropCarried(AbstractContainerMenu menu) {
        click(menu, OUTSIDE, 0, ContainerInput.PICKUP);
    }

    public static void click(int slotId, int button, ContainerInput type) {
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client)) {
            return;
        }
        click(client.player.containerMenu, slotId, button, type);
    }

    public static void click(AbstractContainerMenu menu, int slotId, int button, ContainerInput type) {
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client) || menu == null) {
            return;
        }
        client.gameMode.handleContainerInput(menu.containerId, slotId, button, type, client.player);
    }
}
