package com.betterbundle.sort.net;

import com.betterbundle.sort.model.ItemKey;
import com.betterbundle.sort.plan.MoveAction;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.List;

/**
 * 唯一的发包出口。
 *
 * <p>26.2 实测关键点：{@code BundleContents.Mutable.removeOne()} 实际会移除
 * **整个选中条目（整叠）** 并把选中重置为 -1；右键只有在光标为空时才走该分支。
 * 因此一次「取出」就把整叠拿到光标，必须立刻放入目标袋，绝不能连续右键。
 */
public final class BundlePacketSender {

    private BundlePacketSender() {}

    /** 一次逻辑移动：选中源条目 → 右键取出整叠到光标 → 左键放入目标袋。 */
    public static boolean sendMove(MoveAction action) {
        try {
            Minecraft client = Minecraft.getInstance();
            Player player = client.player;
            ClientPacketListener connection = client.getConnection();
            if (player == null || connection == null) return false;
            if (!player.containerMenu.getCarried().isEmpty()) return false;

            int slots = player.containerMenu.slots.size();
            if (action.srcBagSlot() < 0 || action.srcBagSlot() >= slots) return false;
            if (action.dstBagSlot() < 0 || action.dstBagSlot() >= slots) return false;

            Slot srcSlot = player.containerMenu.getSlot(action.srcBagSlot());
            if (srcSlot == null || !srcSlot.hasItem()) return false;

            int index = currentIndexInBundle(srcSlot.getItem(), action.key());
            if (index < 0) return false;

            int containerId = player.containerMenu.containerId;

            // 选中是 toggle 语义：先 -1 清空，再设目标索引，确定选中。
            connection.send(new ServerboundSelectBundleItemPacket(action.srcBagSlot(), -1));
            connection.send(new ServerboundSelectBundleItemPacket(action.srcBagSlot(), index));
            connection.send(click(containerId, action.srcBagSlot(), (byte) 1));
            connection.send(click(containerId, action.dstBagSlot(), (byte) 0));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 把光标上的物品放进指定空槽（熔断时的安全兜底）。 */
    public static void depositCursor(int slot) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        ClientPacketListener connection = client.getConnection();
        if (player == null || connection == null) return;
        connection.send(click(player.containerMenu.containerId, slot, (byte) 0));
    }

    private static int currentIndexInBundle(ItemStack bagStack, ItemKey key) {
        BundleContents contents = bagStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return -1;
        List<ItemStack> items = contents.itemCopyStream().toList();
        for (int i = 0; i < items.size(); i++) {
            if (ItemKey.of(items.get(i)).equals(key)) return i;
        }
        return -1;
    }

    private static ServerboundContainerClickPacket click(int containerId, int slot, byte button) {
        return new ServerboundContainerClickPacket(
                containerId, -1, (short) slot, button,
                ContainerInput.PICKUP, new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY);
    }
}
