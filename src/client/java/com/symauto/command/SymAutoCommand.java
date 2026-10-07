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
import com.symauto.function.functions.AutoAttackSafetyFunction;
import com.symauto.function.functions.AutoEatFunction;
import com.symauto.function.functions.OneClickDiscardItems;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.Set;
import java.util.function.Function;

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
                builder.suggest(AutoAttackSafetyFunction.INSTANCE.getBLACK_LIST().getFeatureId());
                return builder.buildFuture();
            };

    // 值补全：实体类型功能补全 entity type id，其余补全 item id
    private static final SuggestionProvider<FabricClientCommandSource> BW_VALUE_IDS =
            (context, builder) -> {
                Iterable<Identifier> ids = isEntityFeature(safeFeature(context))
                        ? BuiltInRegistries.ENTITY_TYPE.keySet()
                        : BuiltInRegistries.ITEM.keySet();
                for (Identifier id : ids) {
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
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .suggests(BW_VALUE_IDS)
                                                        .executes(SymAutoCommand::handleBWListWithValue))))));
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
        return handleBWListWithValue(ctx, null);
    }

    private static int handleBWListWithValue(CommandContext<FabricClientCommandSource> ctx) {
        String valueId = StringArgumentType.getString(ctx, "value");
        return handleBWListWithValue(ctx, valueId);
    }

    private static int handleBWListWithValue(CommandContext<FabricClientCommandSource> ctx, String valueId) {
        String featureId = StringArgumentType.getString(ctx, "feature");
        String type = StringArgumentType.getString(ctx, "type");
        String action = StringArgumentType.getString(ctx, "action");

        BWTarget<?> target = resolveTarget(featureId);
        if (target == null) {
            ctx.getSource().sendError(Component.literal("功能 §c" + featureId + "§r 没有黑白名单"));
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
        boolean white = type.equalsIgnoreCase("whitelist");
        Set<T> values = white ? target.list.getWhitelist() : target.list.getBlacklist();
        String title = white ? "白名单" : "黑名单";
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
        boolean white = type.equalsIgnoreCase("whitelist");
        if (white) {
            target.list.getWhitelist().clear();
        } else {
            target.list.getBlacklist().clear();
        }
        ctx.getSource().sendFeedback(Component.literal("§e" + (white ? "白名单" : "黑名单") + "§r 已清空"));
        return 1;
    }

    private static <T> int modifyBWList(CommandContext<FabricClientCommandSource> ctx, BWTarget<T> target,
                                        String type, String action, String valueId) {
        boolean white = type.equalsIgnoreCase("whitelist");
        T value = target.parser.apply(valueId);
        if (value == null) {
            ctx.getSource().sendError(Component.literal("无效 " + target.noun + " ID：§c" + valueId));
            return 0;
        }
        String shown = target.toId.apply(value);
        String title = white ? "白名单" : "黑名单";

        if (action.equalsIgnoreCase("add")) {
            if (white) {
                target.list.addToWhitelist(value);
            } else {
                target.list.addToBlacklist(value);
            }
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + shown + "§r 加入 §e" + title));
        } else {
            if (white) {
                target.list.removeFromWhitelist(value);
            } else {
                target.list.removeFromBlacklist(value);
            }
            ConfigManager.save();
            ctx.getSource().sendFeedback(Component.literal("已将 §f" + shown + "§r 从 §e" + title + "§r 移除"));
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

    private static String safeFeature(CommandContext<FabricClientCommandSource> context) {
        try {
            return StringArgumentType.getString(context, "feature");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isEntityFeature(String featureId) {
        return featureId != null
                && featureId.equalsIgnoreCase(AutoAttackSafetyFunction.INSTANCE.getBLACK_LIST().getFeatureId());
    }

    /** 泛型目标：BWList + 其序列化/解析适配器 + 名词标签。 */
    private static final class BWTarget<T> {
        final BWList<T> list;
        final Function<T, String> toId;
        final Function<String, T> parser;
        final String noun;

        BWTarget(BWList<T> list, Function<T, String> toId, Function<String, T> parser, String noun) {
            this.list = list;
            this.toId = toId;
            this.parser = parser;
            this.noun = noun;
        }
    }

    private static BWTarget<?> resolveTarget(String featureId) {
        BWList<Item> oneClick = OneClickDiscardItems.INSTANCE.getBW_LIST();
        if (featureId.equalsIgnoreCase(oneClick.getFeatureId())) {
            return itemTarget(oneClick);
        }
        BWList<Item> autoEat = AutoEatFunction.INSTANCE.getBW_LISTED_FOOD();
        if (featureId.equalsIgnoreCase(autoEat.getFeatureId())) {
            return itemTarget(autoEat);
        }
        BWList<EntityType<?>> safety = AutoAttackSafetyFunction.INSTANCE.getBLACK_LIST();
        if (featureId.equalsIgnoreCase(safety.getFeatureId())) {
            return entityTarget(safety);
        }
        return null;
    }

    private static BWTarget<Item> itemTarget(BWList<Item> list) {
        return new BWTarget<>(list,
                item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                SymAutoCommand::parseItem,
                "物品");
    }

    private static BWTarget<EntityType<?>> entityTarget(BWList<EntityType<?>> list) {
        return new BWTarget<>(list,
                type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                SymAutoCommand::parseEntityType,
                "实体类型");
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
}