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
    private static final int SLOT_WIDTH = 20;
    private static final int ICON_SIZE = 16;
    private static final int HEIGHT = 27;

    public ArmorDurabilityHudElement() {
        super("armor_durability", "装备耐久", "显示盔甲与主副手物品的图标和剩余耐久",
                HudAnchor.MIDDLE_LEFT, 0, -38);
    }

    @Override
    public int width(Font font) {
        int count = filledSlotCount();
        return count == 0 ? 0 : count * SLOT_WIDTH + 2;
    }

    @Override
    public int height(Font font) {
        return filledSlotCount() == 0 ? 0 : HEIGHT;
    }

    @Override
    public void render(HudRenderContext context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int slot = 0;
        for (EquipmentSlot equipmentSlot : SLOTS) {
            ItemStack stack = player.getItemBySlot(equipmentSlot);
            if (stack.isEmpty()) {
                continue;
            }

            int x = 1 + slot * SLOT_WIDTH;
            int y = 1;
            context.g().item(stack, x, y);
            context.g().itemDecorations(context.font(), stack, x, y);

            int max = stack.getMaxDamage();
            if (max > 0) {
                int remaining = Math.max(0, max - stack.getDamageValue());
                float ratio = remaining / (float) max;
                int color = ratio > 0.5F ? 0xFF55FF55 : ratio > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;
                drawCentered(context, String.valueOf(remaining), x, y + ICON_SIZE + 1, color);
            }

            slot++;
        }
    }

    private int filledSlotCount() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void drawCentered(HudRenderContext context, String text, int x, int y, int color) {
        int textX = x + (ICON_SIZE - context.font().width(text)) / 2;
        context.g().text(context.font(), text, textX, y, color, false);
    }
}
