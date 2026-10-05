package com.symauto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoEatFunction;
import com.symauto.function.functions.OneClickDiscardItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("symauto.json");


    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            ModConfig config = new ModConfig();
            applyFeatures(config);
            applyBWLists(config);
            save(config);
            return;
        }

        try {
            String json = Files.readString(CONFIG_PATH);
            ModConfig config = GSON.fromJson(json, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            applyFeatures(config);
            boolean merged = applyBWLists(config);
            if (merged) {
                save(config);
            }
        } catch (IOException e) {
            System.err.println("[SymAuto] 读取配置失败: " + e.getMessage());
        }
    }

    public static void save() {
        ModConfig config = new ModConfig();
        captureFeatures(config);
        captureBWLists(config);
        save(config);
    }

    private static void save(ModConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            System.err.println("[SymAuto] 保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 捕获所有功能的启用状态
     */
    private static void captureFeatures(ModConfig config) {
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            config.features.put(f.getId(), f.isEnable());
        }
    }

    /**
     * 将配置中的功能开关应用到所有已注册的功能实例上
     */
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

    private static boolean applyBWLists(ModConfig config) {
        boolean merged = false;
        // 没有配置应用默认值
        merged |= applyBWList(config, OneClickDiscardItems.INSTANCE.getId(), OneClickDiscardItems.INSTANCE.getBW_LIST(), OneClickDiscardItems::applyDefaults);
        merged |= applyBWList(config, AutoEatFunction.INSTANCE.getId(), AutoEatFunction.INSTANCE.getBW_LISTED_FOOD(), AutoEatFunction::applyDefaults);
        return merged;
    }

    private static boolean applyBWList(ModConfig config, String featureId, BWList<Item> bwList, Runnable applyDefaults) {
        ModConfig.BWListConfig cfg = config.bwlists.get(featureId);
        if (cfg == null) {
            applyDefaults.run();
            cfg = new ModConfig.BWListConfig();
            for (Item item : bwList.getBlacklist()) {
                cfg.blacklist.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            for (Item item : bwList.getWhitelist()) {
                cfg.whitelist.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            config.bwlists.put(featureId, cfg);
            return true;
        }

        // 配置文件里有，就按配置应用
        bwList.getBlacklist().clear();
        bwList.getWhitelist().clear();

        loadItems(cfg.blacklist, bwList::addToBlacklist);
        loadItems(cfg.whitelist, bwList::addToWhitelist);

        return false;
    }

    private static void loadItems(List<String> ids, Consumer<Item> adder) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            Item item = parseItem(id);
            if (item != null) {
                adder.accept(item);
            }
        }
    }

    /**
     * 将当前所有功能的黑白名单状态抓取到配置对象中
     */
    private static void captureBWLists(ModConfig config) {
        captureBWLists(config, OneClickDiscardItems.INSTANCE.getId(), OneClickDiscardItems.INSTANCE.getBW_LIST());
        captureBWLists(config, AutoEatFunction.INSTANCE.getId(), AutoEatFunction.INSTANCE.getBW_LISTED_FOOD());
    }

    private static void captureBWLists(ModConfig config, String featureId, BWList<Item> bwList) {
        ModConfig.BWListConfig cfg = new ModConfig.BWListConfig();
        for (Item item : bwList.getBlacklist()) {
            cfg.blacklist.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        for (Item item : bwList.getWhitelist()) {
            cfg.whitelist.add(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        config.bwlists.put(featureId, cfg);
    }

    private static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }
}
