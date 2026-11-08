package com.symauto.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symauto.function.auto.AutoAttackFunction;
import com.symauto.function.auto.AutoEatFunction;
import com.symauto.function.oneclick.OneClickDiscardItemsFunction;
import com.symauto.function.place.ScanDestroyFunction;
import com.symauto.function.place.SpeedBridgeFunction;
import com.symbase.model.common.SymList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 黑白名单
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("symauto");
    private static final Path BWLIST_DIR = BASE_DIR.resolve("bwlists");

    private static final Function<Item, String> ITEM_TO_ID =
            item -> BuiltInRegistries.ITEM.getKey(item).toString();
    private static final Function<String, Item> ITEM_FROM_ID = ConfigManager::parseItem;
    private static final Function<EntityType<?>, String> ENTITY_TO_ID =
            type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    private static final Function<String, EntityType<?>> ENTITY_FROM_ID = ConfigManager::parseEntityType;
    private static final Function<Block, String> BLOCK_TO_ID =
            block -> BuiltInRegistries.BLOCK.getKey(block).toString();
    private static final Function<String, Block> BLOCK_FROM_ID = ConfigManager::parseBlock;

    private record SymListHandle<T>(SymList<T> list, Function<T, String> toId, Function<String, T> fromId) {
        String displayName() {
            return list.getFunctionId() + "/" + list.getType();
        }

        String fileName() {
            return list.getFunctionId() + "-" + list.getType() + ".json";
        }
    }

    private static final List<SymListHandle<?>> BW_LISTS = List.of(
            new SymListHandle<>(OneClickDiscardItemsFunction.INSTANCE.getWHITE_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(OneClickDiscardItemsFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(AutoEatFunction.INSTANCE.getBLACK_LIST(), ITEM_TO_ID, ITEM_FROM_ID),
            new SymListHandle<>(AutoAttackFunction.INSTANCE.getBLACK_LIST(), ENTITY_TO_ID, ENTITY_FROM_ID),
            new SymListHandle<>(ScanDestroyFunction.INSTANCE.getWHITE_LIST(), BLOCK_TO_ID, BLOCK_FROM_ID),
            new SymListHandle<>(ScanDestroyFunction.INSTANCE.getBLACK_LIST(), BLOCK_TO_ID, BLOCK_FROM_ID),
            new SymListHandle<>(SpeedBridgeFunction.INSTANCE.getBREAK_LIST(), BLOCK_TO_ID, BLOCK_FROM_ID)
    );

    public static void load() {
        for (SymListHandle<?> handle : BW_LISTS) {
            try {
                loadSymList(handle);
            } catch (Exception e) {
                System.err.println("[SymAuto] 读取黑白名单失败(" + handle.displayName() + "): " + e.getMessage());
            }
        }
    }

    public static void save() {
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

    public static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }

    private static EntityType<?> parseEntityType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
    }

    private static Block parseBlock(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
    }

    public static void openConfigFolder() {
        try {
            Files.createDirectories(BASE_DIR);
            Util.getPlatform().openFile(BASE_DIR.toFile());
        } catch (Exception e) {
            System.err.println("[SymAuto] 打开配置文件夹失败: " + e.getMessage());
        }
    }
}

