package com.symhud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symbase.model.common.SymList;
import com.symhud.core.Constants;
import com.symhud.function.highlight.HighLightItemDropFunction;
import com.symhud.function.highlight.HighLightMasterFunction;
import com.symhud.function.highlight.HighLightSpecificVillagersFunction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 黑白名单存储
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID);
    private static final Path BWLIST_DIR = BASE_DIR.resolve("bwlists");

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

    private record SymListHandle<T>(SymList<T> list, Function<T, String> toId, Function<String, T> fromId) {
        String displayName() {
            return list.getFunctionId() + "/" + list.getType();
        }

        String fileName() {
            return list.getFunctionId() + "-" + list.getType() + ".json";
        }
    }

    private static final List<SymListHandle<?>> BW_LISTS = List.of(
            new SymListHandle<>(HighLightItemDropFunction.INSTANCE.getWHITE_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(HighLightItemDropFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(HighLightMasterFunction.INSTANCE.getWHITE_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(HighLightMasterFunction.INSTANCE.getBLACK_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(HighLightSpecificVillagersFunction.INSTANCE.getBLACK_LIST(), ENCHANT_TO_ID, ENCHANT_FROM_ID)
    );

    private ConfigManager() {
    }

    public static void load() {
        for (SymListHandle<?> handle : BW_LISTS) {
            try {
                loadSymList(handle);
            } catch (Exception e) {
                System.err.println("[SymHud] 读取黑白名单失败(" + handle.displayName() + "): " + e.getMessage());
            }
        }
    }

    public static void save() {
        for (SymListHandle<?> handle : BW_LISTS) {
            try {
                saveSymList(handle);
            } catch (IOException e) {
                System.err.println("[SymHud] 保存黑白名单失败(" + handle.displayName() + "): " + e.getMessage());
            }
        }
    }

    private static <T> void loadSymList(SymListHandle<T> handle) throws IOException {
        Path path = BWLIST_DIR.resolve(handle.fileName());
        SymList<T> list = handle.list();
        if (!Files.exists(path)) {
            saveSymList(handle);
            return;
        }
        String[] ids = GSON.fromJson(Files.readString(path), String[].class);
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

    private static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }

    private static EntityType<?> parseEntityType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
    }
}

