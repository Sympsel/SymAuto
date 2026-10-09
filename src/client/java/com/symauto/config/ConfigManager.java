package com.symauto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoAttackSafetyFunction;
import com.symauto.function.functions.AutoEatFunction;
import com.symauto.function.functions.OneClickDiscardItemsFunction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
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

    /** 描述一个黑白名单如何与磁盘上的独立 JSON 文件互转 */
    private record BWListHandle<T>(BWList<T> list, Function<T, String> toId, Function<String, T> fromId) {
        String featureId() {
            return list.getFeatureId();
        }
    }

    private static final List<BWListHandle<?>> BW_LISTS = List.of(
            new BWListHandle<>(OneClickDiscardItemsFunction.INSTANCE.getBW_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new BWListHandle<>(AutoEatFunction.INSTANCE.getBW_LISTED_FOOD(), ITEM_TO_ID, ITEM_FROM_ID),
            new BWListHandle<>(AutoAttackSafetyFunction.INSTANCE.getBLACK_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID)
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

        for (BWListHandle<?> handle : BW_LISTS) {
            try {
                loadBwList(handle);
            } catch (Exception e) {
                System.err.println("[SymAuto] 读取黑白名单失败(" + handle.featureId() + "): " + e.getMessage());
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
        for (BWListHandle<?> handle : BW_LISTS) {
            try {
                saveBwList(handle);
            } catch (IOException e) {
                System.err.println("[SymAuto] 保存黑白名单失败(" + handle.featureId() + "): " + e.getMessage());
            }
        }
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
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            config.features.put(f.getId(), f.isEnable());
        }
    }

    private static void applyFeatures(ModConfig config) {
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            Boolean enabled = config.features.get(f.getId());
            if (enabled != null) {
                f.setEnable(enabled);
            } else {
                config.features.put(f.getId(), f.isEnable());
            }
        }
    }

    private static <T> void loadBwList(BWListHandle<T> handle) throws IOException {
        Path path = BWLIST_DIR.resolve(handle.featureId() + ".json");
        BWList<T> bwList = handle.list();
        if (!Files.exists(path)) {
            bwList.applyDefaults();
            saveBwList(handle);
            return;
        }
        String json = Files.readString(path);
        ModConfig.BWListConfig cfg = GSON.fromJson(json, ModConfig.BWListConfig.class);
        if (cfg == null) {
            cfg = new ModConfig.BWListConfig();
        }
        bwList.getBlacklist().clear();
        bwList.getWhitelist().clear();
        loadIds(cfg.blacklist, bwList::addToBlacklist, handle.fromId());
        loadIds(cfg.whitelist, bwList::addToWhitelist, handle.fromId());
    }

    private static <T> void saveBwList(BWListHandle<T> handle) throws IOException {
        ModConfig.BWListConfig cfg = new ModConfig.BWListConfig();
        for (T value : handle.list().getBlacklist()) {
            cfg.blacklist.add(handle.toId().apply(value));
        }
        for (T value : handle.list().getWhitelist()) {
            cfg.whitelist.add(handle.toId().apply(value));
        }
        Files.createDirectories(BWLIST_DIR);
        Files.writeString(BWLIST_DIR.resolve(handle.featureId() + ".json"), GSON.toJson(cfg));
    }

    private static <T> void loadIds(List<String> ids, Consumer<T> adder, Function<String, T> fromId) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            T value = fromId.apply(id);
            if (value != null) {
                adder.accept(value);
            }
        }
    }

    private static Item parseItem(String id) {
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
}