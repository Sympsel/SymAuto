package com.symauto.gui.container;

import com.symauto.function.base.SymContainer;
import com.symauto.function.container.FasterContainerPanelFunction;
import com.symauto.function.container.depend.Containers;
import com.symauto.function.utils.ClientGuard;
import com.symauto.gui.UiStyle;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器预览
 */
public class ContainerPreviewRenderer {
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 4;
    private static final int TITLE_HEIGHT = 12;
    private static final int COLS = 9;

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("symauto", "hand_container_preview_hud");

    private ContainerPreviewRenderer() {
    }

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, ContainerPreviewRenderer::drawHandPreview);
    }

    /**
     * 无界面世界中：按住中键 + 主手持容器 → 在鼠标处预览容器内容
     */
    private static void drawHandPreview(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!FasterContainerPanelFunction.INSTANCE.isEnableMiddleClickViewContainer()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client) || client.gui.screen() != null) {
            return;
        }
        if (!isMiddleDown(client)) {
            return;
        }
        ItemStack host = client.player.getMainHandItem();
        List<ItemStack> contents = contentsFor(client, host);
        if (contents == null || contents.isEmpty()) {
            return;
        }
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;
        renderPopup(g, client, host, contents, cx, cy);
    }

    @Nullable
    private static List<ItemStack> contentsFor(Minecraft client, ItemStack host) {
        SymContainer container = Containers.of(host).orElse(null);
        if (container != null) {
            return container.entries();
        }
        if (host.is(Items.ENDER_CHEST)) {
            return readLocalEnderChest(client);
        }
        return null;
    }

    /**
     * 读取本地末影箱内容，仅在单人模式下有效
     */
    @Nullable
    private static List<ItemStack> readLocalEnderChest(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            return null;
        }
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(client.player.getUUID());
        if (serverPlayer == null) {
            return null;
        }
        PlayerEnderChestContainer enderChest = serverPlayer.getEnderChestInventory();
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack item = enderChest.getItem(i);
            if (!item.isEmpty()) {
                out.add(item.copy());
            }
        }
        return out;
    }

    public static void render(GuiGraphicsExtractor g, Slot hoveredSlot, int mouseX, int mouseY) {
        SymContainer container = previewContainer(hoveredSlot);
        if (container == null) {
            return;
        }
        renderPopup(g, Minecraft.getInstance(), hoveredSlot.getItem(), container.entries(), mouseX, mouseY);
    }

    private static boolean isShiftDown(Minecraft client) {
        long win = client.getWindow().handle();
        return GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isMiddleDown(Minecraft client) {
        long win = client.getWindow().handle();
        return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
    }

    @Nullable
    public static SymContainer previewContainer(@Nullable Slot hoveredSlot) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (!ClientGuard.ready(client) || !isShiftDown(client)) {
            return null;
        }
        return Containers.of(hoveredSlot.getItem()).orElse(null);
    }

    public static boolean isPreviewActive(@Nullable Slot hoveredSlot) {
        return previewContainer(hoveredSlot) != null;
    }

    private static void renderPopup(GuiGraphicsExtractor g, Minecraft client, ItemStack host,
                                    List<ItemStack> contents, int mouseX, int mouseY) {
        int rows = Math.max(1, (contents.size() + COLS - 1) / COLS);
        int width = PADDING * 2 + COLS * SLOT_SIZE;
        int height = PADDING * 2 + TITLE_HEIGHT + rows * SLOT_SIZE;

        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + width > sw) {
            x = mouseX - width - 12;
        }
        if (x < 2) {
            x = 2;
        }
        if (y + height > sh) {
            y = sh - height - 2;
        }
        if (y < 2) {
            y = 2;
        }

        UiStyle style = UiStyle.DEFAULT;
        g.fill(x, y, x + width, y + height, style.panelBg);
        g.fill(x, y, x + width, y + 1, style.panelOutline);
        g.fill(x, y + height - 1, x + width, y + height, style.panelOutline);
        g.fill(x, y, x + 1, y + height, style.panelOutline);
        g.fill(x + width - 1, y, x + width, y + height, style.panelOutline);

        Font font = client.font;
        String title = host.getHoverName().getString();
        int textX = x + PADDING;
        int textY = y + PADDING;
        g.text(font, title, textX, textY, style.title, false);

        int gridY = textY + TITLE_HEIGHT;
        for (int i = 0; i < contents.size(); i++) {
            ItemStack s = contents.get(i);
            if (s.isEmpty()) {
                continue;
            }
            int col = i % COLS;
            int row = i / COLS;
            int sx = x + PADDING + col * SLOT_SIZE;
            int sy = gridY + row * SLOT_SIZE;
            g.item(s, sx + 1, sy + 1);
            g.itemDecorations(font, s, sx + 1, sy + 1);
        }
    }
}
