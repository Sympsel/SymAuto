package com.symbase.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symbase.config.option.ConfigOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.registry.FunctionRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 功能配置
 */
public final class FunctionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("symbase").resolve("functions.json");
    /** 旧版本把功能配置写在 symauto 下，首次加载时迁移旧数据 */
    private static final Path LEGACY_AUTO_CONFIG = FabricLoader.getInstance()
            .getConfigDir().resolve("symauto").resolve("symauto.json");

    private static boolean loaded;

    private FunctionConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        Data data = read(CONFIG_PATH);
        if (data == null) {
            data = read(LEGACY_AUTO_CONFIG);
        }
        if (data == null) {
            data = new Data();
        }

        for (SymAbstractFunction function : FunctionRegistry.all()) {
            Boolean enabled = data.functions.get(function.getId());
            if (enabled != null) {
                function.setEnable(enabled);
            } else {
                data.functions.put(function.getId(), function.isEnable());
            }

            int[] tags = data.functionTags.get(function.getId());
            if (tags != null) {
                function.resetTags(tags);
            } else {
                data.functionTags.put(function.getId(), function.getTagArray());
            }

            if (function.hasConfig()) {
                Map<String, String> saved = data.featureOptions.get(function.getId());
                if (saved != null) {
                    for (ConfigOption option : function.getConfigOptions()) {
                        String value = saved.get(option.getKey());
                        if (value != null) {
                            option.deserialize(value);
                        }
                    }
                }
            }
        }

        loaded = true;
        save();
    }

    public static void save() {
        Data data = new Data();
        for (SymAbstractFunction function : FunctionRegistry.all()) {
            data.functions.put(function.getId(), function.isEnable());
            data.functionTags.put(function.getId(), function.getTagArray());
            if (function.hasConfig()) {
                Map<String, String> options = new LinkedHashMap<>();
                for (ConfigOption option : function.getConfigOptions()) {
                    options.put(option.getKey(), option.serialize());
                }
                data.featureOptions.put(function.getId(), options);
            }
        }
        write(data);
    }

    private static Data read(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, Data.class);
        } catch (Exception e) {
            System.err.println("[SymBase] 读取功能配置失败(" + path + "): " + e.getMessage());
            return null;
        }
    }

    private static void write(Data data) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[SymBase] 保存功能配置失败: " + e.getMessage());
        }
    }

    private static final class Data {
        private int version = 1;
        private Map<String, Boolean> functions = new LinkedHashMap<>();
        private Map<String, int[]> functionTags = new LinkedHashMap<>();
        private Map<String, Map<String, String>> featureOptions = new LinkedHashMap<>();
    }
}
