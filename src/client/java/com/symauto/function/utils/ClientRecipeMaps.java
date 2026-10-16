package com.symauto.function.utils;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.Collection;
import java.util.List;

public final class ClientRecipeMaps {
    private static Collection<RecipeHolder<?>> cached;

    private ClientRecipeMaps() {}

    public static Collection<RecipeHolder<?>> get(Minecraft mc) {
        if (cached == null) {
            cached = load(mc);
        }
        return cached;
    }

    public static void invalidate() {
        cached = null;
    }

    private static Collection<RecipeHolder<?>> load(Minecraft mc) {
        // 集成了所有数据包的配方
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            var list = server.getRecipeManager().getRecipes();
            System.out.println("[SymAuto] 单人模式，配方 " + list.size() + " 条");
            return List.copyOf(list);
        }
        // 多人模式下，用客户端原版数据包驱动一个临时 RecipeManager
        try (CloseableResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA,
                List.of(ServerPacksSource.createVanillaPackSource()))) {
            TempRecipeManager temp = new TempRecipeManager(mc.level.registryAccess());
            temp.loadNow(resources);
            var list = temp.getRecipes();
            System.out.println("[SymAuto] 多人模式，加载配方 " + list.size() + " 条，仅能读到原版数据包的配方");
            return List.copyOf(list);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load vanilla recipes on client", e);
        }
    }

    // 暴露 protected 的 prepare/apply
    private static final class TempRecipeManager extends RecipeManager {
        TempRecipeManager(HolderLookup.Provider registries) {
            super(registries);
        }

        void loadNow(ResourceManager resourceManager) {
            RecipeMap prepared = this.prepare(resourceManager, Profiler.get());
            this.apply(prepared, resourceManager, Profiler.get());
        }
    }
}