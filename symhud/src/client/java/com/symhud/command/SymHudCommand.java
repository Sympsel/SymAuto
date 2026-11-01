package com.symhud.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.symbase.config.ConfigServices;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.gui.screen.HudEditScreen;
import com.symbase.model.common.SymList;
import com.symhud.config.ConfigManager;
import com.symhud.core.HudFunctions;
import com.symhud.function.highlight.HighLightItemDropFunction;
import com.symhud.function.highlight.HighLightMasterFunction;
import com.symhud.function.highlight.HighLightSpecificVillagersFunction;
import com.symhud.storage.VillagerTradeStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * 客户端指令
 */
public final class SymHudCommand {
    private static final String NS = "sym:hud";
    private static final String NS_SIMPLIFIED = "sym:h";

    private record BWTarget<T>(SymList<T> list, Function<T, String> toId, Function<String, T> parser,
                               String noun, boolean isEntity) {
        String functionId() {
            return list.getFunctionId();
        }
    }

    private static final List<BWTarget<?>> WHITELIST_TARGETS = List.of(
            itemTarget(HighLightItemDropFunction.INSTANCE.getWHITE_LIST()),
            entityTarget(HighLightMasterFunction.INSTANCE.getWHITE_LIST())
    );

    private static final List<BWTarget<?>> BLACKLIST_TARGETS = List.of(
            itemTarget(HighLightItemDropFunction.INSTANCE.getBLACK_LIST()),
            entityTarget(HighLightMasterFunction.INSTANCE.getBLACK_LIST()),
            enchantmentTarget(HighLightSpecificVillagersFunction.INSTANCE.getBLACK_LIST())
    );

    private static final SuggestionProvider<FabricClientCommandSource> FUNCTION_IDS =
            (context, builder) -> {
                for (SymAbstractFunction function : HudFunctions.ALL) {
                    builder.suggest(function.getId());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> BW_FEATURES =
            (context, builder) -> {
                Set<String> seen = new HashSet<>();
                for (BWTarget<?> target : allTargets()) {
                    if (seen.add(target.functionId())) {
                        builder.suggest(target.functionId());
                    }
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> BW_VALUE_IDS =
            (context, builder) -> {
                Iterable<Identifier> ids = isEntityFunction(safeFunction(context))
                        ? BuiltInRegistries.ENTITY_TYPE.keySet()
                        : BuiltInRegistries.ITEM.keySet();
                for (Identifier id : ids) {
                    builder.suggest(id.toString());
                }
                return builder.buildFuture();
            };

    private SymHudCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        register(dispatcher, NS);
        register(dispatcher, NS_SIMPLIFIED);
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, String ns) {
        dispatcher.register(literal(ns).executes(SymHudCommand::openHudEditor)
                .then(literal("hud").executes(SymHudCommand::openHudEditor)));
        dispatcher.register(literal(ns + ":hud").executes(SymHudCommand::openHudEditor));
        dispatcher.register(literal(ns + ":list").executes(SymHudCommand::listFunctions));
        dispatcher.register(literal(ns + ":info")
                .then(argument("function", StringArgumentType.word())
                        .suggests(FUNCTION_IDS)
                        .executes(SymHudCommand::showFunctionInfo)));
        dispatcher.register(literal(ns + ":enable")
                .then(argument("function", StringArgumentType.word())
                        .suggests(FUNCTION_IDS)
                        .executes(ctx -> setEnable(ctx, null))
                        .then(argument("state", BoolArgumentType.bool())
                                .executes(ctx -> setEnable(ctx, BoolArgumentType.getBool(ctx, "state"))))));
        dispatcher.register(literal(ns + ":toggle")
                .then(argument("function", StringArgumentType.word())
                        .suggests(FUNCTION_IDS)
                        .executes(SymHudCommand::toggleFunction)));
        dispatcher.register(literal(ns + ":trades")
                .then(literal("count").executes(SymHudCommand::showTradeCount))
                .then(literal("clear").executes(SymHudCommand::clearTrades)));
        dispatcher.register(literal(ns + ":bwlist")
                .then(argument("function", StringArgumentType.word())
                        .suggests(BW_FEATURES)
                        .then(argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> builder.suggest("blacklist").suggest("whitelist").buildFuture())
                                .then(argument("action", StringArgumentType.word())
                                        .suggests((ctx, builder) -> builder.suggest("add").suggest("remove")
                                                .suggest("list").suggest("clear").buildFuture())
                                        .executes(SymHudCommand::handleBWList)
                                        .then(argument("value", StringArgumentType.greedyString())
                                                .suggests(BW_VALUE_IDS)
                                                .executes(SymHudCommand::handleBWListWithValue))))));
    }

    private static int openHudEditor(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new HudEditScreen(null)));
        return 1;
    }

    private static int listFunctions(CommandContext<FabricClientCommandSource> ctx) {
        for (SymAbstractFunction function : HudFunctions.ALL) {
            String state = function.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal(
                    state + " §7" + function.getId() + "§r " + function.getName()));
        }
        return 1;
    }

    private static int showFunctionInfo(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction function = findFunction(id);
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        String state = function.isEnable() ? "§a开启§r" : "§c关闭§r";
        ctx.getSource().sendFeedback(Component.literal("功能：§e" + function.getName() + "§r（" + state + "§r）"));
        ctx.getSource().sendFeedback(Component.literal("ID：§7" + function.getId()));
        ctx.getSource().sendFeedback(Component.literal("说明：§f" + function.getTooltip()));
        return 1;
    }

    private static int setEnable(CommandContext<FabricClientCommandSource> ctx, Boolean state) {
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction function = findFunction(id);
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        if (state == null) {
            String current = function.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal("§e" + function.getName() + "§r 当前状态：" + current));
            return 1;
        }
        function.setEnable(state);
        ConfigServices.saveAll();
        String result = function.isEnable() ? "§a已开启§r" : "§c已关闭§r";
        ctx.getSource().sendFeedback(Component.literal("§e" + function.getName() + "§r " + result));
        return 1;
    }

    private static int toggleFunction(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction function = findFunction(id);
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        function.toggle();
        ConfigServices.saveAll();
        String result = function.isEnable() ? "§a已开启§r" : "§c已关闭§r";
        ctx.getSource().sendFeedback(Component.literal("§e" + function.getName() + "§r " + result));
        return 1;
    }

    private static int showTradeCount(CommandContext<FabricClientCommandSource> ctx) {
        int size = VillagerTradeStore.size();
        ctx.getSource().sendFeedback(Component.literal("§e村民交易缓存§r 共 " + size + " 条"));
        return size;
    }

    private static int clearTrades(CommandContext<FabricClientCommandSource> ctx) {
        int removed = VillagerTradeStore.clear();
        ctx.getSource().sendFeedback(Component.literal("已清空村民交易缓存（移除 §f" + removed + "§r 条）"));
        return removed;
    }

    private static int handleBWList(CommandContext<FabricClientCommandSource> ctx) {
        return handleBWListWithValue(ctx, null);
    }

    private static int handleBWListWithValue(CommandContext<FabricClientCommandSource> ctx) {
        return handleBWListWithValue(ctx, StringArgumentType.getString(ctx, "value"));
    }

    private static int handleBWListWithValue(CommandContext<FabricClientCommandSource> ctx, String valueId) {
        String functionId = StringArgumentType.getString(ctx, "function");
        String type = StringArgumentType.getString(ctx, "type");
        String action = StringArgumentType.getString(ctx, "action");

        BWTarget<?> target = resolveTarget(functionId, type);
        if (target == null) {
            ctx.getSource().sendError(Component.literal("功能 §c" + functionId + "§r 没有对应的 §c" + type + "§r 名单"));
            return 0;
        }

        return switch (action.toLowerCase()) {
            case "list" -> listBWList(ctx, target, type);
            case "clear" -> clearBWList(ctx, target, type);
            case "add", "remove" -> {
                if (valueId == null || valueId.isBlank()) {
                    ctx.getSource().sendError(Component.literal("add/remove 需要指定 " + target.noun + " ID"));
                    yield 0;
                }
                yield modifyBWList(ctx, target, type, action, valueId);
            }
            default -> {
                ctx.getSource().sendError(Component.literal("未知操作：§c" + action));
                yield 0;
            }
        };
    }

    private static <T> int listBWList(CommandContext<FabricClientCommandSource> ctx, BWTarget<T> target, String type) {
        Set<T> values = target.list.getList();
        String title = isWhitelist(type) ? "白名单" : "黑名单";
        if (values.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("§e" + title + "§r 为空"));
            return 1;
        }
        ctx.getSource().sendFeedback(Component.literal("§e" + title + "§r 共 " + values.size() + " 项（" + target.noun + "）："));
        for (T value : values) {
            ctx.getSource().sendFeedback(Component.literal("  §7- §f" + target.toId.apply(value)));
        }
        return 1;
    }

    private static <T> int clearBWList(CommandContext<FabricClientCommandSource> ctx, BWTarget<T> target, String type) {
        target.list.clear();
        ConfigManager.save();
        ctx.getSource().sendFeedback(Component.literal("§e" + (isWhitelist(type) ? "白名单" : "黑名单") + "§r 已清空"));
        return 1;
    }

    private static <T> int modifyBWList(CommandContext<FabricClientCommandSource> ctx, BWTarget<T> target,
                                        String type, String action, String valueId) {
        T value = target.parser.apply(valueId);
        if (value == null) {
            ctx.getSource().sendError(Component.literal("无效 " + target.noun + " ID：§c" + valueId));
            return 0;
        }
        String shown = target.toId.apply(value);
        String title = isWhitelist(type) ? "白名单" : "黑名单";

        if (action.equalsIgnoreCase("add")) {
            target.list.add(value);
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + shown + "§r 加入 §e" + title));
        } else {
            target.list.remove(value);
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + shown + "§r 从 §e" + title + "§r 移除"));
        }
        return 1;
    }

    private static SymAbstractFunction findFunction(String id) {
        for (SymAbstractFunction function : HudFunctions.ALL) {
            if (function.getId().equalsIgnoreCase(id)) {
                return function;
            }
        }
        return null;
    }

    private static boolean isEntityFunction(String functionId) {
        if (functionId == null) {
            return false;
        }
        for (BWTarget<?> target : allTargets()) {
            if (target.functionId().equalsIgnoreCase(functionId)) {
                return target.isEntity;
            }
        }
        return false;
    }

    private static boolean isWhitelist(String type) {
        return SymList.Type.WHITELIST.equalsIgnoreCase(type);
    }

    private static List<BWTarget<?>> allTargets() {
        List<BWTarget<?>> all = new ArrayList<>(WHITELIST_TARGETS);
        all.addAll(BLACKLIST_TARGETS);
        return all;
    }

    private static BWTarget<?> resolveTarget(String functionId, String type) {
        List<BWTarget<?>> source = isWhitelist(type) ? WHITELIST_TARGETS : BLACKLIST_TARGETS;
        for (BWTarget<?> target : source) {
            if (target.functionId().equalsIgnoreCase(functionId)) {
                return target;
            }
        }
        return null;
    }

    private static String safeFunction(CommandContext<FabricClientCommandSource> context) {
        try {
            return StringArgumentType.getString(context, "function");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BWTarget<Item> itemTarget(SymList<Item> list) {
        return new BWTarget<>(list,
                item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                SymHudCommand::parseItem,
                "物品",
                false);
    }

    private static BWTarget<EntityType<?>> entityTarget(SymList<EntityType<?>> list) {
        return new BWTarget<>(list,
                type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                SymHudCommand::parseEntityType,
                "实体类型",
                true);
    }

    private static BWTarget<ResourceKey<Enchantment>> enchantmentTarget(SymList<ResourceKey<Enchantment>> list) {
        return new BWTarget<>(list,
                key -> key.identifier().toString(),
                SymHudCommand::parseEnchantmentType,
                "附魔书类型",
                true);
    }

    private static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }

    private static EntityType<?> parseEntityType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
    }

    private static ResourceKey<Enchantment> parseEnchantmentType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : ResourceKey.create(Registries.ENCHANTMENT, identifier);
    }
}

