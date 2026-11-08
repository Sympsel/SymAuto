package com.symauto.function.auto;

import com.symbase.config.option.BooleanOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symauto.core.Constants;
import com.symbase.function.util.Tooltip;
import com.symauto.function.place.ScanDestroyFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
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
    public static final AutoSwiftToolsFunction INSTANCE = new AutoSwiftToolsFunction();

    /**
     * 低于该耐久比例的易损工具不再参与自动选择，避免选中后无法完成挖掘。
     */
    private static final float MIN_DURABILITY_RATIO = 0.1F;

    private int prevSlot = -1;
    private BlockPos currBlockPos = null;
    private int syncTicks = 0;
    @Getter
    private int intervalMs = 100;
    @Getter
    private float floatFactor = 0.1f;
    @Getter
    @Setter
    private boolean toolsProtect;

    private AutoSwiftToolsFunction() {
        super("auto_swift_tools", "自动切换破坏工具");
        addOption(new BooleanOption("enable_high_value_tools_protect", "工具保护",
                "耐久低于10%的工具不参与自动切换",
                this::isToolsProtect, this::setToolsProtect, true));
        setIntervalMs(50);
        setFloatFactor(0.1f);
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line( "自动切换破坏工具")
                .line(ChatFormatting.BLUE, "优先级：")
                .line(1, "快捷栏 > 精准采集 > 挖掘等级")
                .keyValueLine("工具保护", toolsProtect ? "是" : "否")
                .toString();
    }

    public void setIntervalMs(int intervalMs) {
        this.intervalMs = Math.max(10, intervalMs);
    }

    public void setFloatFactor(float floatFactor) {
        this.floatFactor = Math.max(0f, floatFactor);
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        switchBack(client);
        resetTarget();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player.isCreative()) {
            switchBack(client);
            return;
        }
        // 扫描破坏由自己调用 ensureBestTool，避免这里因为没按攻击键而把工具切回去
        if (ScanDestroyFunction.INSTANCE.isEnable()) {
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

        if (syncTicks > 0) {
            syncTicks--;
            return;
        }

        selectBestTool(client, blockPos, blockState);
    }

    /**
     * 按目标方块切换最佳工具
     */
    public boolean ensureBestTool(Minecraft client, BlockPos blockPos) {
        if (!isEnable() || client.player == null || client.level == null
                || client.player.isCreative()) {
            return false;
        }
        BlockState blockState = client.level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        }
        // 外部功能直接指定目标，不复用旧目标缓存，确保每次都能重新选择最佳工具
        currBlockPos = null;
        return selectBestTool(client, blockPos, blockState);
    }

    private boolean selectBestTool(Minecraft client, BlockPos blockPos, BlockState blockState) {
        Inventory inv = client.player.getInventory();
        int currSlot = inv.getSelectedSlot();

        if (blockPos.equals(currBlockPos) && isBest(inv, blockState, currSlot)) {
            return true;
        }
        currBlockPos = blockPos;

        // 快捷栏
        int hotBarSlot = findBestInRange(inv, blockState, Constants.HOTBAR_START, Constants.HOTBAR_END);
        if (hotBarSlot >= 0) {
            if (hotBarSlot != currSlot) {
                if (prevSlot == -1) {
                    prevSlot = currSlot;
                }
                selectSlot(client, hotBarSlot);
            }
            return true;
        }

        // 背包
        int invSlot = findBestInRange(inv, blockState, Constants.INV_START, Constants.INV_END);
        if (invSlot < 0) {
            return false;
        }

        if (prevSlot == -1) {
            prevSlot = currSlot;
        }

        swapInventoryIntoSelectedHotbar(client, invSlot);
        return true;
    }

    public void earlySwitchBack(Minecraft client) {
        if (client.options.keyAttack.isDown()) {
            return;
        }
        resetTarget();
        switchBack(client);
    }

    private void selectSlot(Minecraft client, int slot) {
        if (client.player == null || slot < Constants.HOTBAR_START || slot >= Constants.HOTBAR_END) {
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

        syncTicks = Constants.SYNC_TICKS;
    }

    private void swapInventoryIntoSelectedHotbar(Minecraft client, int inventorySlot) {
        if (inventorySlot < Constants.INV_START || inventorySlot >= Constants.INV_END) {
            return;
        }

        abortDestroy(client);

        int selectedHotbarSlot = client.player.getInventory().getSelectedSlot();

        client.gameMode.handleContainerInput(
                client.player.inventoryMenu.containerId,
                inventorySlot,
                selectedHotbarSlot,
                ContainerInput.SWAP,
                client.player
        );

        syncTicks = Constants.SYNC_TICKS;
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

    /**
     * 低于 10% 耐久的易损工具不参与选择；不可损坏工具始终可用。
     */
    private static boolean isUsable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.isDamageableItem()) {
            return true;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return true;
        }
        int remaining = max - stack.getDamageValue();
        return remaining > max * MIN_DURABILITY_RATIO;
    }

    private boolean isBest(Inventory inv, BlockState state, int slot) {
        if (slot < Constants.HOTBAR_START || slot >= Constants.HOTBAR_END) {
            return false;
        }

        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty() || !isUsable(stack)) {
            return false;
        }

        float mySpeed = stack.getDestroySpeed(state);
        if (mySpeed <= 1.0f) {
            return false;
        }

        Holder.Reference<Enchantment> silkTouchHolder = getSilkTouchHolder(inv);
        boolean mySilk = stack.getEnchantments().getLevel(silkTouchHolder) > 0;

        for (int i = Constants.HOTBAR_START; i < Constants.HOTBAR_END; i++) {
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
            if (stack.isEmpty() || !isUsable(stack)) {
                continue;
            }

            float speed = stack.getDestroySpeed(state);
            if (speed <= 1.0f) {
                continue;
            }
            // 工具保护
            if (toolsProtect && stack.isDamageableItem()
                    && stack.getDamageValue() > stack.getMaxDamage() * (1 - floatFactor)) {
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
