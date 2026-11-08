package com.symauto.function.place;

import com.symauto.function.auto.AutoEatFunction;
import com.symauto.mixin.accessor.ClientLevelAccessor;
import com.symbase.config.option.BooleanOption;
import com.symbase.config.option.IntOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.ClientGuard;
import com.symbase.function.util.Tooltip;
import com.symbase.model.common.SymList;
import com.symauto.function.auto.AutoSwiftToolsFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScanDestroyFunction extends SymAbstractFunction {
    public static final ScanDestroyFunction INSTANCE = new ScanDestroyFunction();

    @Getter
    @Setter
    private double radius = 4.0;

    @Getter
    @Setter
    private boolean excludeBelowCurrentY;

    @Getter
    @Setter
    private boolean disableBreakCooldown;

    @Getter
    @Setter
    private boolean toolsProtect;

    @Getter
    @Setter
    private boolean onlyWhitelist;

    @Getter
    private final SymList<Block> WHITE_LIST;
    @Getter
    private final SymList<Block> BLACK_LIST;

    @Getter
    @Setter
    private int maxActionsPerTick = 20;

    private static final int TIP_COOLDOWN = 100;
    private int tipCooldown = 0;

    private BlockPos breakingPos;
    private Direction breakingFace = Direction.UP;

    private ScanDestroyFunction() {
        super("scan_destroy", "扫描式破坏");
        WHITE_LIST = new SymList<>(getId(), SymList.Type.WHITELIST);
        BLACK_LIST = new SymList<>(getId(), SymList.Type.BLACKLIST);
        // 提供默认黑名单，防止挖到信标及底座、容器类方块
        BLACK_LIST.withDefaultItems(
                Blocks.BEACON,
                Blocks.IRON_BLOCK,
                Blocks.GOLD_BLOCK,
                Blocks.DIAMOND_BLOCK,
                Blocks.EMERALD_BLOCK,
                Blocks.NETHERITE_BLOCK,
                Blocks.CHEST,
                Blocks.TRAPPED_CHEST,
                Blocks.ENDER_CHEST,
                Blocks.BARREL,
                Blocks.HOPPER,
                Blocks.DROPPER,
                Blocks.DISPENSER,
                Blocks.FURNACE,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER,
                Blocks.BREWING_STAND,
                Blocks.SHULKER_BOX,
                Blocks.CRAFTER,
                Blocks.DECORATED_POT
        );
        for (Block block : Blocks.DYED_SHULKER_BOX.asList()) {
            BLACK_LIST.add(block);
        }
        for (Block block : Blocks.COPPER_CHEST.asList()) {
            BLACK_LIST.add(block);
        }
        addOption(new BooleanOption("exclude_below_current_y", "排除扫描当前y值以下方块",
                "扫描时排除当前 y 值以下的方块",
                this::isExcludeBelowCurrentY, this::setExcludeBelowCurrentY, true));
        addOption(new BooleanOption("disable_break_cooldown", "禁用挖掘冷却",
                "跳过方块破坏之间的冷却",
                this::isDisableBreakCooldown, this::setDisableBreakCooldown, false));
        addOption(new BooleanOption("only_whitelist", "仅破坏白名单方块",
                "只破坏白名单内的方块",
                this::isOnlyWhitelist, this::setOnlyWhitelist, false));
        addOption(new BooleanOption("enable_high_value_tools_protect", "工具保护",
                "工具耐久低于10%时停止破坏",
                this::isToolsProtect, this::setToolsProtect, true));
        addOption(new IntOption("max_actions_per_tick", "每tick最大可挖掘数",
                "每 tick 最多破坏的秒破方块数",
                this::getMaxActionsPerTick, this::setMaxActionsPerTick, 1, 64, 1));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("破坏自身范围内所有可破坏的方块，优先准心所指向的方块，平地神器")
                .keyValueLine("排除扫描当前y值以下方块", excludeBelowCurrentY ? "是" : "否")
                .keyValueLine("禁用挖掘冷却", disableBreakCooldown ? "是" : "否")
                .keyValueLine("仅破坏白名单方块", onlyWhitelist ? "是" : "否")
                .keyValueLine("工具保护", toolsProtect ? "是" : "否")
                .display(WHITE_LIST, block -> block.getName().getString())
                .display(BLACK_LIST, block -> block.getName().getString())
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (!ClientGuard.ready(client)) {
            return;
        }

        // 不打断自动进食
        if (AutoEatFunction.INSTANCE.isEating()) {
            return;
        }

        Vec3 eye = client.player.getEyePosition();
        double reach = Math.min(radius, client.player.blockInteractionRange());
        double reachSq = reach * reach;
        int r = (int) Math.ceil(reach);
        BlockPos center = client.player.blockPosition();
        double currentY = client.player.getY();

        BlockHitResult crosshairHit = crosshairHit(client, reachSq);
        BlockPos crosshair = crosshairHit == null ? null : crosshairHit.getBlockPos();

        List<BlockPos> blocks = new ArrayList<>();
        collectTargets(client, eye, reachSq, r, center, currentY, blocks);

        if (crosshair != null) {
            blocks.remove(crosshair);
            blocks.addFirst(crosshair);
        }

        if (breakingPos != null && !canBreak(client, breakingPos)) {
            breakingPos = null;
        }

        // 兼容自动切换破坏工具
        BlockPos toolTarget = crosshair != null ? crosshair
                : breakingPos != null ? breakingPos
                : blocks.isEmpty() ? null : blocks.getFirst();
        if (toolTarget != null && AutoSwiftToolsFunction.INSTANCE.isEnable()) {
            AutoSwiftToolsFunction.INSTANCE.ensureBestTool(client, toolTarget);
        }

        // 路径 1：批量破坏所有"当前可秒破"的方块
        boolean creative = client.player.isCreative();
        if (!creative && toolsProtect && durabilityBudget(client) <= 0) {
            if (breakingPos != null) {
                client.gameMode.stopDestroyBlock();
                breakingPos = null;
            }
            if (tipCooldown < TIP_COOLDOWN) {
                ++tipCooldown;
            } else {
                client.player.sendOverlayMessage(
                        Component.literal("工具耐久过低，已停止破坏")
                                .withStyle(ChatFormatting.YELLOW));
                tipCooldown = 0;
            }
            return;
        }
        tipCooldown = 0;
        int done = 0;
        for (BlockPos pos : blocks) {
            if (done >= maxActionsPerTick) {
                break;
            }
            if (!creative && !canBreakInstantly(client, pos)) {
                continue;
            }
            if (instantBreak(client, pos, Direction.UP)) {
                ++done;
            }
        }

        if (creative) {
            return;
        }
        // 不和秒破预测在同一个 tick 启动普通挖掘
        if (done > 0) {
            return;
        }
        // 路径 2：处理剩余需要正常挖掘的方块
        BlockPos target = crosshair != null ? crosshair
                : breakingPos != null ? breakingPos
                : blocks.isEmpty() ? null : blocks.getFirst();

        // target 可能刚被快速路径破成空气，重选第一个仍存在的方块
        if (target == null || client.level.getBlockState(target).isAir()) {
            breakingPos = null;
            target = null;
            for (BlockPos pos : blocks) {
                if (!client.level.getBlockState(pos).isAir()) {
                    target = pos;
                    break;
                }
            }
            if (target == null) {
                return;
            }
        }

        Direction face = target.equals(crosshair)
                ? crosshairHit.getDirection()
                : Direction.UP;

        if (!target.equals(breakingPos)) {
            breakingPos = target;
            breakingFace = face;
            client.gameMode.startDestroyBlock(target, face);
        } else {
            client.gameMode.continueDestroyBlock(target, breakingFace);
        }

        if (client.level.getBlockState(target).isAir()) {
            breakingPos = null;
        }
    }

    /**
     * 本次破坏需要耐久预算，避免工具直接爆掉
     */
    private static int durabilityBudget(Minecraft client) {
        ItemStack stack = client.player.getMainHandItem();
        if (!stack.isDamageableItem()) {
            return Integer.MAX_VALUE;
        }
        int max = stack.getMaxDamage();
        int remaining = max - stack.getDamageValue();
        return Math.max(0, remaining - (int) (max * 0.1f));
    }

    /**
     * 当前手持工具下能否被立即破坏
     */
    private static boolean canBreakInstantly(Minecraft client, BlockPos pos) {
        return client.level.getBlockState(pos)
                .getDestroyProgress(client.player, client.level, pos) >= 1.0F;
    }

    private static boolean instantBreak(Minecraft client, BlockPos pos, Direction face) {
        var handler =
                ((ClientLevelAccessor) client.level).symauto$getBlockStatePredictionHandler();
        try (var ignored = handler.startPredicting()) {
            int sequence = handler.currentSequence();
            // 空气 / 创造持剑时拒绝
            if (!client.gameMode.destroyBlock(pos)) {
                return false;
            }
            client.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face, sequence));
            return true;
        }
    }

    private void collectTargets(Minecraft client, Vec3 eye, double reachSq, int r, BlockPos center,
                                double currentY, List<BlockPos> breakTargets) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distToCenterSqr(eye) > reachSq) {
                continue;
            }
            if (excludeBelowCurrentY && pos.getY() < currentY) {
                continue;
            }

            BlockPos target = pos.immutable();
            if (canBreak(client, target)) {
                breakTargets.add(target);
            }
        }

        breakTargets.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(eye)));
    }

    /**
     * 准心射线命中的方块；超出扫描范围或不可破坏时返回 null
     */
    private BlockHitResult crosshairHit(Minecraft client, double reachSq) {
        if (!(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (pos.distToCenterSqr(client.player.getEyePosition()) > reachSq) {
            return null;
        }
        if (!canBreak(client, pos)) {
            return null;
        }
        return hit;
    }

    /**
     * 空气和不可破坏方块不进入扫描列表
     */
    private boolean canBreak(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(client.level, pos) < 0.0F) {
            return false;
        }
        Block block = state.getBlock();
        if (BLACK_LIST.contains(block)) {
            return false;
        }
        if (onlyWhitelist && !WHITE_LIST.contains(block)) {
            return false;
        }
        return state.getDestroyProgress(client.player, client.level, pos) > 0.0F;
    }
}

