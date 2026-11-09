package com.symbase.function.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockInteractionUtils {
    private BlockInteractionUtils() {
    }

    public static boolean isInteractiveBlock(Block block) {
        return block instanceof EntityBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof BedBlock
                || block instanceof CakeBlock
                || block instanceof ComparatorBlock
                || block instanceof RepeaterBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || block instanceof ComposterBlock
                || block instanceof CampfireBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof BellBlock
                || block instanceof DragonEggBlock
                || block instanceof FlowerPotBlock
                || block instanceof CandleBlock
                || block instanceof CandleCakeBlock
                || block instanceof TntBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof CauldronBlock
                || block instanceof BeehiveBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof CaveVines;
    }

    public static boolean isInteractiveTarget(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult) {
            return true;
        }
        if (!(hit instanceof BlockHitResult blockHit) || client.level == null) {
            return false;
        }
        return isInteractiveBlock(client.level.getBlockState(blockHit.getBlockPos()).getBlock());
    }
}
