package com.symauto.function.auto;

import com.symauto.config.option.BooleanOption;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.mixin.accessor.VaultServerDataAccessor;
import com.symauto.model.SymList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.phys.BlockHitResult;

@Deprecated
public class AutoVaultFunction extends SymAbstractFunction {
    public static final AutoVaultFunction INSTANCE = new AutoVaultFunction();


    // 仅单人模式可用
    @Getter
    @Setter
    private boolean limitlessCount;

    @Getter
    private final SymList<Item> WRITE_LIST;

    private AutoVaultFunction() {
        super("auto_vault", "自动蹲点宝库", Tooltip.create()
                .line("指针瞄准宝库，当显示物品为白名单内物品时，打开宝库")
                .toString()
        );
        addOption(new BooleanOption("limitless_count", "无限次数开启（仅单人模式可用）",
                this::isLimitlessCount, this::setLimitlessCount, false));
        WRITE_LIST = new SymList<>(getId(), SymList.Type.WHITELIST);
        WRITE_LIST.withDefaultItems(
                Items.HEAVY_CORE,
                Items.TRIDENT // 三叉戟
        );
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("指针瞄准宝库，当显示物品为白名单内物品时，打开宝库")
                .keyValueLine(ChatFormatting.YELLOW, "宝库无限开启（仅单人模式可用）",
                        ChatFormatting.GRAY, limitlessCount ? "是" : "否")
                .toString();
    }

    private void removeRecordOfVault(MinecraftServer server, ResourceKey<Level> dimension,
                                     BlockPos pos, boolean onlyClearThisClient) {
        if (dimension == null) {
            return;
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof VaultBlockEntity vault) {
            VaultServerData data = vault.getServerData();
            if (data == null) {
                return;
            }
            if (onlyClearThisClient) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) {
                    return;
                }
                ((VaultServerDataAccessor) data).symauto$getRewardedPlayers().remove(client.player.getUUID());
            } else {
                ((VaultServerDataAccessor) data).symauto$getRewardedPlayers().clear();
            }
        }
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult result)) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        if (!(client.level.getBlockState(pos).getBlock() instanceof VaultBlock)) {
            return;
        }
        if (client.level.getBlockState(pos).getValue(VaultBlock.STATE) != VaultState.ACTIVE) {
            return;
        }
        if (!(client.level.getBlockEntity(pos) instanceof VaultBlockEntity vault)) {
            return;
        }
        IntegratedServer server = client.getSingleplayerServer();
        if (server != null && limitlessCount) {
            server.execute(() -> removeRecordOfVault(server, client.level.dimension(), pos, true));
        }

        ItemStack displayItem = vault.getSharedData().getDisplayItem();
        if (displayItem.isEmpty() || !WRITE_LIST.contains(displayItem.getItem())) {
            return;
        }
        ItemStack key = vault.getConfig().keyItem();
        if (key.isEmpty()) {
            return;
        }
        if (!ItemStack.isSameItemSameComponents(client.player.getMainHandItem(), key)) {
            return;
        }
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, result);
    }
}
