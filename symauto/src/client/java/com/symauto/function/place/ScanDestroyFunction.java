package com.symauto.function.place;

import com.symbase.config.option.BooleanOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.utils.Tooltip;
import com.symauto.function.auto.AutoSwiftToolsFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private BlockPos breakingPos;
    private Direction breakingFace = Direction.UP;

    private ScanDestroyFunction() {
        super("scan_destroy", "扫描式破坏");
        addOption(new BooleanOption("exclude_below_current_y", "排除扫描当前y值以下方块",
                this::isExcludeBelowCurrentY, this::setExcludeBelowCurrentY, true));
        addOption(new BooleanOption("disable_break_cooldown", "禁用挖掘冷却",
                this::isDisableBreakCooldown, this::setDisableBreakCooldown, false));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("破坏自身范围内所有可破坏的方块，优先准心所指向的方块")
                .keyValueLine("排除扫描当前y值以下方块", excludeBelowCurrentY ? "是" : "否")
                .keyValueLine("禁用挖掘冷却", disableBreakCooldown ? "是" : "否")
                .line(ChatFormatting.AQUA, "平地神器")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
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
            blocks.add(0, crosshair);
        }

        if (breakingPos != null && !canBreak(client, breakingPos)) {
            breakingPos = null;
        }

        BlockPos target = crosshair != null ? crosshair
                : breakingPos != null ? breakingPos
                : blocks.isEmpty() ? null : blocks.getFirst();

        if (target == null) {
            return;
        }

        Direction face = target.equals(crosshair) && crosshairHit != null
                ? crosshairHit.getDirection()
                : Direction.UP;

        // 兼容自动切换破坏工具
        if (AutoSwiftToolsFunction.INSTANCE.isEnable()) {
            AutoSwiftToolsFunction.INSTANCE.ensureBestTool(client, target);
        }

        if (!target.equals(breakingPos)) {
            breakingPos = target;
            breakingFace = face;
            client.gameMode.startDestroyBlock(target, face);
        } else {
            client.gameMode.continueDestroyBlock(target, breakingFace);
            if (disableBreakCooldown) {
                client.gameMode.continueDestroyBlock(target, breakingFace);
            }
        }

        if (client.level.getBlockState(target).isAir()) {
            breakingPos = null;
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
    private static boolean canBreak(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(client.level, pos) < 0.0F) {
            return false;
        }
        return state.getDestroyProgress(client.player, client.level, pos) > 0.0F;
    }
}
