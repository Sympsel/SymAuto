package com.symhud.hud.element;

import com.symbase.function.utils.ClientGuard;
import com.symbase.function.utils.ItemUtils;
import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ItemTotalCountHudElement  extends AbstractTextHudElement {
    public ItemTotalCountHudElement() {
        super("item_total_count", "物品总数", "显示手持物品在背包里的总数，不包括容器内的",
                HudAnchor.TOP_LEFT, 1, 73, 0xFFFFD166, 0x00000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        if (!ClientGuard.ready(client)) {
            return "";
        }
        ItemStack item = client.player.getMainHandItem();
        if (item.isEmpty()) {
            return "";
        }
        int count = 0;
        for (ItemStack stack : client.player.getInventory()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (ItemUtils.isSameItemWithDataComponent(item, stack)) {
                count += stack.getCount();
            }
        }
        return "物品总数: " + count;
    }
}
