package com.symbase.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 打开各模块配置目录
 */
public final class ConfigFolders {
    private ConfigFolders() {
    }

    public static void open(String modId) {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(modId);
            Files.createDirectories(dir);
            Util.getPlatform().openFile(dir.toFile());
        } catch (Exception ignored) {
        }
    }
}
