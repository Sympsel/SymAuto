package com.symauto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symauto.config.option.ConfigOption;
import com.symauto.function.auto.AutoAttackFunction;
import com.symauto.function.highlight.HighLightSpecificVillagersFunction;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.auto.AutoEatFunction;
import com.symauto.function.highlight.HighLightItemDropFunction;
import com.symauto.function.highlight.HighLightMasterFunction;
import com.symauto.function.oneclick.OneClickDiscardItemsFunction;
import com.symauto.model.common.SymList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("symauto");
    private static final Path CONFIG_PATH = BASE_DIR.resolve("symauto.json");
    private static final Path BWLIST_DIR = BASE_DIR.resolve("bwlists");
    private static final Path LEGACY_ROOT_CONFIG =
            FabricLoader.getInstance().getConfigDir().resolve("symauto.json");

    private static final Function<Item, String> ITEM_TO_ID =
            item -> BuiltInRegistries.ITEM.getKey(item).toString();
    private static final Function<String, Item> ITEM_FROM_ID = ConfigManager::parseItem;

    private static final Function<EntityType<?>, String> ENTITY_TO_ID =
            type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    private static final Function<String, EntityType<?>> ENTITY_FROM_ID = ConfigManager::parseEntityType;

    private static final Function<ResourceKey<Enchantment>, String> ENCHANT_TO_ID =
            key -> key.identifier().toString();
    private static final Function<String, ResourceKey<Enchantment>> ENCHANT_FROM_ID = id -> {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : ResourceKey.create(Registries.ENCHANTMENT, identifier);
    };

    /**
     * 描述一个黑白名单如何与磁盘上的独立 JSON 文件互转
     */
     private record SymListHandle<T>(SymList<T> list, Function<T, String> toId, Function<String, T> fromId) {
        String functionId() {
            return list.getFunctionId();
        }

        String type() {
            return list.getType();
        }

        String displayName() {
            return functionId() + "/" + type();
        }

        String fileName() {
            return functionId() + "-" + type() + ".json";
        }
    }

    private static final List<SymListHandle<?>> BW_LISTS = List.of(
            new SymListHandle<>(OneClickDiscardItemsFunction.INSTANCE.getWHITE_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(OneClickDiscardItemsFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(HighLightItemDropFunction.INSTANCE.getWHITE_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(HighLightItemDropFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(AutoEatFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(AutoAttackFunction.INSTANCE.getBLACK_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(HighLightMasterFunction.INSTANCE.getWHITE_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(HighLightMasterFunction.INSTANCE.getBLACK_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(HighLightSpecificVillagersFunction.INSTANCE.getBLACK_LIST(), ENCHANT_TO_ID, ENCHANT_FROM_ID)
    );

    public static void load() {
        ModConfig config;
        if (!Files.exists(CONFIG_PATH)) {
            config = new ModConfig();
        } else {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }
            } catch (Exception e) {
                System.err.println("[SymAuto] 读取配置失败: " + e.getMessage());
                config = new ModConfig();
            }
        }
        applyFeatures(config);
        saveConfig(config);

        for (SymListHandle<?> handle : BW_LISTS) {
            try {
                loadSymList(handle);
            } catch (Exception e) {
                System.err.println("[SymAuto] 读取黑白名单失败(" + handle.displayName() + "): " + e.getMessage());
            }
        }
        deleteLegacyRootFiles();
    }

    /**
     * 删除旧版本配置
     */
    private static void deleteLegacyRootFiles() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        deleteQuietly();
    }

    public static void save() {
        ModConfig config = new ModConfig();
        captureFeatures(config);
        saveConfig(config);
        for (SymListHandle<?> handle : BW_LISTS) {
            try {
                saveSymList(handle);
            } catch (IOException e) {
                System.err.println("[SymAuto] 保存黑白名单失败(" + handle.displayName() + "): " + e.getMessage());
            }
        }
    }

    private static <T> void loadSymList(SymListHandle<T> handle) throws IOException {
        Path path = BWLIST_DIR.resolve(handle.fileName());
        SymList<T> list = handle.list();
        if (!Files.exists(path)) {
            // 首次运行：保留构造时写入的默认条目
            saveSymList(handle);
            return;
        }
        String json = Files.readString(path);
        String[] ids = GSON.fromJson(json, String[].class);
        list.clear();
        if (ids != null) {
            for (String id : ids) {
                T value = handle.fromId().apply(id);
                if (value != null) {
                    list.add(value);
                }
            }
        }
    }

    private static <T> void saveSymList(SymListHandle<T> handle) throws IOException {
        List<String> ids = new ArrayList<>();
        for (T value : handle.list().getList()) {
            ids.add(handle.toId().apply(value));
        }
        Files.createDirectories(BWLIST_DIR);
        Files.writeString(BWLIST_DIR.resolve(handle.fileName()), GSON.toJson(ids));
    }

    private static void saveConfig(ModConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            System.err.println("[SymAuto] 保存配置失败: " + e.getMessage());
        }
    }

    private static void captureFeatures(ModConfig config) {
        for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
            config.features.put(f.getId(), f.isEnable());
            if (f.hasConfig()) {
                Map<String, String> opts = new LinkedHashMap<>();
                for (ConfigOption o : f.getConfigOptions()) {
                    opts.put(o.getKey(), o.serialize());
                }
                config.featureOptions.put(f.getId(), opts);
            }
        }
    }

    private static void applyFeatures(ModConfig config) {
        for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
            Boolean enabled = config.features.get(f.getId());
            if (enabled != null) {
                f.setEnable(enabled);
            } else {
                config.features.put(f.getId(), f.isEnable());
            }
            if (f.hasConfig()) {
                Map<String, String> saved = config.featureOptions.get(f.getId());
                if (saved != null) {
                    for (ConfigOption o : f.getConfigOptions()) {
                        String v = saved.get(o.getKey());
                        if (v != null) {
                            o.deserialize(v);
                        }
                    }
                }
            }
        }
    }

    public static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }

    private static EntityType<?> parseEntityType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
    }

    private static void deleteQuietly() {
        try {
            if (Files.deleteIfExists(ConfigManager.LEGACY_ROOT_CONFIG)) {
                System.out.println("[SymAuto] 已删除旧配置文件: " + ConfigManager.LEGACY_ROOT_CONFIG.getFileName());
            }
        } catch (IOException e) {
            System.err.println("[SymAuto] 删除旧配置文件失败(" + ConfigManager.LEGACY_ROOT_CONFIG.getFileName() + "): " + e.getMessage());
        }
    }

    /**
     * 在系统文件管理器中打开配置文件夹（config/symauto）。
     */
    public static void openConfigFolder() {
        try {
            Files.createDirectories(BASE_DIR);
            Util.getPlatform().openFile(BASE_DIR.toFile());
        } catch (Exception e) {
            System.err.println("[SymAuto] 打开配置文件夹失败: " + e.getMessage());
        }
    }
}