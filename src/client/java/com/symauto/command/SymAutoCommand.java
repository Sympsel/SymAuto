package com.symauto.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.symauto.config.ConfigManager;
import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoEatFunction;
import com.symauto.function.functions.OneClickDiscardItems;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;


public class SymAutoCommand {
    private static final SuggestionProvider<FabricClientCommandSource> FEATURE_IDS =
            (context, builder) -> {
                for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                    builder.suggest(f.getId());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> BW_FEATURES =
            (context, builder) -> {
                builder.suggest(OneClickDiscardItems.INSTANCE.getBW_LIST().getFeatureId());
                builder.suggest(AutoEatFunction.INSTANCE.getBW_LISTED_FOOD().getFeatureId());
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> ITEM_IDS =
            (context, builder) -> {
                for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                    builder.suggest(id.toString());
                }
                return builder.buildFuture();
            };

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildRoot("symauto"));
            dispatcher.register(buildRoot("sa"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRoot(String name) {
        return literal(name)
                .executes(SymAutoCommand::showUsage)
                .then(literal("list").executes(SymAutoCommand::listFeatures))
                .then(literal("info")
                        .then(argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_IDS)
                                .executes(SymAutoCommand::showFeatureInfo)))
                .then(literal("enable")
                        .then(argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_IDS)
                                .executes(ctx -> setEnable(ctx, null))
                                .then(argument("state", BoolArgumentType.bool())
                                        .executes(ctx -> setEnable(ctx, BoolArgumentType.getBool(ctx, "state"))))))
                .then(literal("toggle")
                        .then(argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_IDS)
                                .executes(SymAutoCommand::toggleFeature)))
                .then(literal("bwlist")
                        .then(argument("feature", StringArgumentType.word())
                                .suggests(BW_FEATURES)
                                .then(argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> builder.suggest("blacklist").suggest("whitelist").buildFuture())
                                        .then(argument("action", StringArgumentType.word())
                                                .suggests((ctx, builder) -> builder.suggest("add").suggest("remove").suggest("list").suggest("clear").buildFuture())
                                                .executes(SymAutoCommand::handleBWList)
                                                .then(argument("item", StringArgumentType.greedyString())
                                                        .suggests(ITEM_IDS)
                                                        .executes(SymAutoCommand::handleBWListWithItem))))));
    }

    private static int showUsage(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal("用法：/symauto <list|info|enable|toggle|bwlist>"));
        return 1;
    }

    private static int listFeatures(CommandContext<FabricClientCommandSource> ctx) {
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            String state = f.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal(state + " §7" + f.getId() + "§r " + f.getName()));
        }
        return 1;
    }

    private static int showFeatureInfo(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "feature");
        SymAbstractFunction f = findFeature(id);
        if (f == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        String state = f.isEnable() ? "§a开启§r" : "§c关闭§r";
        ctx.getSource().sendFeedback(Component.literal("功能：§e" + f.getName() + "§r（" + state + "§r）"));
        ctx.getSource().sendFeedback(Component.literal("ID：§7" + f.getId()));
        ctx.getSource().sendFeedback(Component.literal("说明：§f" + f.getTooltip()));
        return 1;
    }

    private static int setEnable(CommandContext<FabricClientCommandSource> ctx, Boolean state) {
        String id = StringArgumentType.getString(ctx, "feature");
        SymAbstractFunction f = findFeature(id);
        if (f == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        if (state == null) {
            String current = f.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal("§e" + f.getName() + "§r 当前状态：" + current));
            return 1;
        }
        f.setEnable(state);
        ConfigManager.save();
        String result = f.isEnable() ? "§a已开启§r" : "§c已关闭§r";
        ctx.getSource().sendFeedback(Component.literal("§e" + f.getName() + "§r " + result));
        return 1;
    }

    private static int toggleFeature(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "feature");
        SymAbstractFunction f = findFeature(id);
        if (f == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + id));
            return 0;
        }
        f.toggle();
        ConfigManager.save();
        String result = f.isEnable() ? "§a已开启§r" : "§c已关闭§r";
        ctx.getSource().sendFeedback(Component.literal("§e" + f.getName() + "§r " + result));
        return 1;
    }

    private static int handleBWList(CommandContext<FabricClientCommandSource> ctx) {
        return handleBWListWithItem(ctx, null);
    }

    private static int handleBWListWithItem(CommandContext<FabricClientCommandSource> ctx) {
        String itemId = StringArgumentType.getString(ctx, "item");
        return handleBWListWithItem(ctx, itemId);
    }

    private static int handleBWListWithItem(CommandContext<FabricClientCommandSource> ctx, String itemId) {
        String featureId = StringArgumentType.getString(ctx, "feature");
        String type = StringArgumentType.getString(ctx, "type");
        String action = StringArgumentType.getString(ctx, "action");

        BWList<Item> bwList = getBWList(featureId);
        if (bwList == null) {
            ctx.getSource().sendError(Component.literal("功能 §c" + featureId + "§r 没有黑白名单"));
            return 0;
        }

        return switch (action.toLowerCase()) {
            case "list" -> listBWList(ctx, bwList, type);
            case "clear" -> clearBWList(ctx, bwList, type);
            case "add", "remove" -> {
                if (itemId == null || itemId.isBlank()) {
                    ctx.getSource().sendError(Component.literal("add/remove 需要指定物品 ID"));
                    yield 0;
                }
                yield modifyBWList(ctx, bwList, type, action, itemId);
            }
            default -> {
                ctx.getSource().sendError(Component.literal("未知操作：§c" + action));
                yield 0;
            }
        };
    }

    private static int listBWList(CommandContext<FabricClientCommandSource> ctx, BWList<Item> bwList, String type) {
        Set<Item> items = type.equalsIgnoreCase("whitelist") ? bwList.getWhitelist() : bwList.getBlacklist();
        String title = type.equalsIgnoreCase("whitelist") ? "白名单" : "黑名单";
        if (items.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("§e" + title + "§r 为空"));
            return 1;
        }
        ctx.getSource().sendFeedback(Component.literal("§e" + title + "§r 共 " + items.size() + " 项："));
        for (Item item : items) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            ctx.getSource().sendFeedback(Component.literal("  §7- §f" + id));
        }
        return 1;
    }

    private static int clearBWList(CommandContext<FabricClientCommandSource> ctx, BWList<Item> bwList, String type) {
        if (type.equalsIgnoreCase("whitelist")) {
            bwList.getWhitelist().clear();
        } else {
            bwList.getBlacklist().clear();
        }
        ctx.getSource().sendFeedback(Component.literal("§e" + (type.equalsIgnoreCase("whitelist") ? "白名单" : "黑名单") + "§r 已清空"));
        return 1;
    }

    private static int modifyBWList(CommandContext<FabricClientCommandSource> ctx, BWList<Item> bwList,
                                    String type, String action, String itemId) {
        Item item = parseItem(itemId);
        if (item == null) {
            ctx.getSource().sendError(Component.literal("无效物品 ID：§c" + itemId));
            return 0;
        }
        String itemName = BuiltInRegistries.ITEM.getKey(item).toString();
        String title = type.equalsIgnoreCase("whitelist") ? "白名单" : "黑名单";

        if (action.equalsIgnoreCase("add")) {
            if (type.equalsIgnoreCase("whitelist")) {
                bwList.addToWhitelist(item);
            } else {
                bwList.addToBlacklist(item);
            }
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + itemName + "§r 加入 §e" + title));
        } else {
            if (type.equalsIgnoreCase("whitelist")) {
                bwList.removeFromWhitelist(item);
            } else {
                bwList.removeFromBlacklist(item);
            }
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + itemName + "§r 从 §e" + title + "§r 移除"));
        }
        return 1;
    }

    private static SymAbstractFunction findFeature(String id) {
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            if (f.getId().equalsIgnoreCase(id)) {
                return f;
            }
        }
        return null;
    }

    private static BWList<Item> getBWList(String featureId) {
        if (OneClickDiscardItems.INSTANCE.getBW_LIST().getFeatureId().equalsIgnoreCase(featureId)) {
            return OneClickDiscardItems.INSTANCE.getBW_LIST();
        }
        if (AutoEatFunction.INSTANCE.getBW_LISTED_FOOD().getFeatureId().equalsIgnoreCase(featureId)) {
            return AutoEatFunction.INSTANCE.getBW_LISTED_FOOD();
        }
        return null;
    }

    private static Item parseItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
    }
}