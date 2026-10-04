package com.symauto.function.functions;

import com.symauto.function.SymAbstractFunction;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoSwiftToolsFunction extends SymAbstractFunction {
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 9;
    private static final int INV_START = 9;
    private static final int INV_END = 36;

    private static final int SYNC_TICKS = 2;

    private int prevSlot = -1;
    private BlockPos currBlockPos = null;
    private int syncTicks = 0;
    @Getter
    private int intervalMs = 100;
    @Getter
    private float floatFactor = 0.1f;

    public AutoSwiftToolsFunction() {
        super("自动切换破坏工具", "自动切换破坏工具，快捷栏工具优先，其次精准采集工具优先，最后挖掘速度优先");
        setIntervalMs(50);
        setFloatFactor(0.1f);
    }


    public void setIntervalMs(int intervalMs) {
        this.intervalMs = Math.max(10, intervalMs);
    }

    public void setFloatFactor(float floatFactor) {
        this.floatFactor = Math.max(0f, floatFactor);
    }

    protected int nextDelay() {
        float min = intervalMs * (1f - floatFactor);
        float max = intervalMs * (1f + floatFactor);
        int delay = (int) (min + RANDOM.nextFloat() * (max - min));
        return Math.max(10, delay);
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }

        HitResult hitResult = client.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            resetTarget();
            switchBack(client);
            return;
        }

        if (!client.options.keyAttack.isDown()) {
            resetTarget();
            switchBack(client);
            return;
        }

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState = client.level.getBlockState(blockPos);

        if (blockState.isAir()) {
            resetTarget();
            switchBack(client);
            return;
        }

        // 刚切换或交换过物品时，先停止破坏，等待服务器按顺序处理同步包
        if (syncTicks > 0) {
            abortDestroy(client);
            syncTicks--;
            return;
        }

        Inventory inv = client.player.getInventory();
        int currSlot = inv.getSelectedSlot();

        if (blockPos.equals(currBlockPos) && isBest(inv, blockState, currSlot)) {
            return;
        }
        currBlockPos = blockPos;

        // 第一阶段：快捷栏
        int hotBarSlot = findBestInRange(inv, blockState, HOTBAR_START, HOTBAR_END);
        if (hotBarSlot >= 0) {
            if (hotBarSlot != currSlot) {
                if (prevSlot == -1) {
                    prevSlot = currSlot;
                }
                selectSlot(client, hotBarSlot);
            }
            return;
        }

        // 第二阶段：背包，使用服务器认可的容器交换
        int invSlot = findBestInRange(inv, blockState, INV_START, INV_END);
        if (invSlot < 0) {
            return;
        }

        if (prevSlot == -1) {
            prevSlot = currSlot;
        }

        swapInventoryIntoSelectedHotbar(client, invSlot);
    }

    private void selectSlot(Minecraft client, int slot) {
        if (client.player == null || slot < HOTBAR_START || slot >= HOTBAR_END) {
            return;
        }

        Inventory inv = client.player.getInventory();
        if (inv.getSelectedSlot() == slot) {
            return;
        }

        // 防止客户端用旧工具开始或继续预测破坏
        abortDestroy(client);

        inv.setSelectedSlot(slot);

        var connection = client.getConnection();
        if (connection != null) {
            connection.send(new ServerboundSetCarriedItemPacket(slot));
        }

        syncTicks = SYNC_TICKS;
    }

    private void swapInventoryIntoSelectedHotbar(Minecraft client, int inventorySlot) {
        if (client.player == null || client.gameMode == null) {
            return;
        }
        if (inventorySlot < INV_START || inventorySlot >= INV_END) {
            return;
        }

        abortDestroy(client);

        int selectedHotbarSlot = client.player.getInventory().getSelectedSlot();

        // inventorySlot 为 9–35 时，与玩家背包容器槽位一致。
        // ClickType.SWAP 的 mouseButton 参数是目标快捷栏序号 0–8。
        client.gameMode.handleContainerInput(
                client.player.inventoryMenu.containerId,
                inventorySlot,
                selectedHotbarSlot,
                ContainerInput.SWAP,
                client.player
        );

        syncTicks = SYNC_TICKS;
    }

    private void abortDestroy(Minecraft client) {
        if (client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
    }

    private void resetTarget() {
        currBlockPos = null;
        syncTicks = 0;
    }

    private boolean isBest(Inventory inv, BlockState state, int slot) {
        if (slot < HOTBAR_START || slot >= HOTBAR_END) {
            return false;
        }

        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }

        float mySpeed = stack.getDestroySpeed(state);
        if (mySpeed <= 1.0f) {
            return false;
        }

        Holder.Reference<Enchantment> silkTouchHolder = getSilkTouchHolder(inv);
        boolean mySilk = stack.getEnchantments().getLevel(silkTouchHolder) > 0;

        for (int i = HOTBAR_START; i < HOTBAR_END; i++) {
            if (i == slot) {
                continue;
            }

            ItemStack other = inv.getItem(i);
            if (other.isEmpty()) {
                continue;
            }

            float otherSpeed = other.getDestroySpeed(state);
            if (otherSpeed <= 1.0f) {
                continue;
            }

            boolean otherSilk = other.getEnchantments().getLevel(silkTouchHolder) > 0;
            if (isBetter(otherSpeed, otherSilk, mySpeed, mySilk)) {
                return false;
            }
        }

        return true;
    }

    private int findBestInRange(Inventory inv, BlockState state, int from, int to) {
        Holder.Reference<Enchantment> silkTouchHolder = getSilkTouchHolder(inv);

        int bestSlot = -1;
        float bestSpeed = 1.0f;
        boolean bestSilk = false;

        for (int i = from; i < to; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            float speed = stack.getDestroySpeed(state);
            if (speed <= 1.0f) {
                continue;
            }

            boolean silk = stack.getEnchantments().getLevel(silkTouchHolder) > 0;

            if (bestSlot == -1 || isBetter(speed, silk, bestSpeed, bestSilk)) {
                bestSlot = i;
                bestSpeed = speed;
                bestSilk = silk;
            }
        }

        return bestSlot;
    }

    private boolean isBetter(
            float candidateSpeed,
            boolean candidateSilk,
            float currentSpeed,
            boolean currentSilk
    ) {
        // 精准采集优先，同类再比较速度
        if (candidateSilk != currentSilk) {
            return candidateSilk;
        }
        return candidateSpeed > currentSpeed;
    }

    private Holder.Reference<Enchantment> getSilkTouchHolder(Inventory inv) {
        return inv.player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
    }

    private void switchBack(Minecraft client) {
        if (prevSlot == -1 || client.player == null) {
            return;
        }

        Inventory inv = client.player.getInventory();
        int targetSlot = prevSlot;
        prevSlot = -1;

        if (inv.getSelectedSlot() == targetSlot) {
            return;
        }

        inv.setSelectedSlot(targetSlot);

        var connection = client.getConnection();
        if (connection != null) {
            connection.send(new ServerboundSetCarriedItemPacket(targetSlot));
        }
    }
}