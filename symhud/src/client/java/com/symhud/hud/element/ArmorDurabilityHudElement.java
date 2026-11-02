package com.symhud.hud.element;

import com.symbase.hud.AbstractHudElement;
import com.symbase.hud.HudAnchor;
import com.symbase.hud.HudRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 盔甲栏与主副手物品的图标与耐久
 */
public final class ArmorDurabilityHudElement extends AbstractHudElement {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };
    private static final int MIN_SLOT_WIDTH = 20;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_PADDING = 4;
    private static final int HEIGHT = 27;

    public ArmorDurabilityHudElement() {
        super("armor_durability", "装备耐久", "显示盔甲与主副手物品的图标和剩余耐久",
                HudAnchor.MIDDLE_LEFT, 0, -38);
    }

    @Override
    public int width(Font font) {
        int total = 0;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    total += slotWidth(stack, font);
                }
            }
        }
        return total == 0 ? 0 : total + 2;
    }

    @Override
    public int height(Font font) {
        return hasItems() ? HEIGHT : 0;
    }

    @Override
    public void render(HudRenderContext context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int x = 1;
        int y = 1;
        for (EquipmentSlot equipmentSlot : SLOTS) {
            ItemStack stack = player.getItemBySlot(equipmentSlot);
            if (stack.isEmpty()) {
                continue;
            }

            int slotWidth = slotWidth(stack, context.font());
            int iconX = x + (slotWidth - ICON_SIZE) / 2;
            context.g().item(stack, iconX, y);
            context.g().itemDecorations(context.font(), stack, iconX, y);

            int max = stack.getMaxDamage();
            if (max > 0) {
                int remaining = Math.max(0, max - stack.getDamageValue());
                float ratio = remaining / (float) max;
                int color = ratio > 0.5F ? 0xFF55FF55 : ratio > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;
                drawCentered(context, String.valueOf(remaining), x, slotWidth, y + ICON_SIZE + 1, color);
            }

            x += slotWidth;
        }
    }

    private static int slotWidth(ItemStack stack, Font font) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return MIN_SLOT_WIDTH;
        }
        int remaining = Math.max(0, max - stack.getDamageValue());
        return Math.max(MIN_SLOT_WIDTH, font.width(String.valueOf(remaining)) + TEXT_PADDING);
    }

    private boolean hasItems() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        for (EquipmentSlot slot : SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void drawCentered(HudRenderContext context, String text, int x, int slotWidth, int y, int color) {
        int textX = x + (slotWidth - context.font().width(text)) / 2;
        context.g().text(context.font(), text, textX, y, color, false);
    }
}
