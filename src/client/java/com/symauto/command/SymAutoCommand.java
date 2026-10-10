package com.symauto.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.symauto.config.ConfigManager;
import com.symauto.config.VillagerTradeStore;
import com.symauto.entity.BWList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;


public class SymAutoCommand {
    /**
     * 泛型目标：BWList + 其序列化/解析适配器 + 名词标签。
     */
    private record BWTarget<T>(BWList<T> list, Function<T, String> toId, Function<String, T> parser, String noun, boolean isEntity) {

        String featureId() {
            return list.getFeatureId();
        }
    }

    // 黑白名单数据源
    private static final List<BWTarget<?>> BW_TARGETS = List.of(
            itemTarget(OneClickDiscardItemsFunction.INSTANCE.getBW_LIST()),
            itemTarget(AutoEatFunction.INSTANCE.getFOOD_BLACKLIST()),
            itemTarget(HighLightItemDropFunction.INSTANCE.getBW_LIST()),
            entityTarget(AutoAttackSafetyFunction.INSTANCE.getBW_LIST()),
            entityTarget(HighLightMasterFunction.INSTANCE.getBW_LIST())
    );

    private static final SuggestionProvider<FabricClientCommandSource> FEATURE_IDS =
            (context, builder) -> {
                for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                    builder.suggest(f.getId());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> BW_FEATURES =
            (context, builder) -> {
                for (BWTarget<?> t : BW_TARGETS) {
                    builder.suggest(t.featureId());
                }
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
                .then(literal("trades")
                        .then(literal("count").executes(SymAutoCommand::showTradeCount))
                        .then(literal("clear").executes(SymAutoCommand::clearTrades)))
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
        ctx.getSource().sendFeedback(Component.literal("用法：/symauto <list|info|enable|toggle|trades|bwlist>"));
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
        if (featureId == null) {
            return false;
        }
        for (BWTarget<?> t : BW_TARGETS) {
            if (t.featureId().equalsIgnoreCase(featureId)) {
                return t.isEntity;
            }
        }
        return false;
    }




    private static BWTarget<?> resolveTarget(String featureId) {
        for (BWTarget<?> t : BW_TARGETS) {
            if (t.featureId().equalsIgnoreCase(featureId)) {
                return t;
            }
        }
        return null;
    }

    private static BWTarget<Item> itemTarget(BWList<Item> list) {
        return new BWTarget<>(list,
                item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                SymAutoCommand::parseItem,
                "物品",
                false
        );
    }

    private static BWTarget<EntityType<?>> entityTarget(BWList<EntityType<?>> list) {
        return new BWTarget<>(list,
                type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                SymAutoCommand::parseEntityType,
                "实体类型",
                true
        );
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

    // 供 CommandUsageScreen 渲染
    public static List<Component> usageLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(section("—— 客户端命令（前缀 /symauto，简写 /sa）——"));
        lines.add(plain("均为客户端命令，单人/联机均可使用", ChatFormatting.GRAY));
        lines.add(spacer());

        lines.add(section("功能管理"));
        lines.add(cmd("/sa list", "列出全部功能及其开关状态"));
        lines.add(cmd("/sa info <feature>", "查看指定功能的详细信息与说明"));
        lines.add(cmd("/sa enable <feature> [true|false]", "查询或设置某功能的开关；不带参数则查询"));
        lines.add(cmd("/sa toggle <feature>", "切换某功能的开关状态"));
        lines.add(spacer());

        lines.add(section("黑白名单"));
        lines.add(cmd("/sa bwlist <feature> <blacklist|whitelist> list", "查看指定名单的全部条目"));
        lines.add(cmd("/sa bwlist <feature> <blacklist|whitelist> clear", "清空指定名单"));
        lines.add(cmd("/sa bwlist <feature> <blacklist|whitelist> add|remove <id>", "增删一个条目（id 为物品或实体类型）"));
        lines.add(plain("可管理的名单 feature（由命令注册自动列出）：", ChatFormatting.YELLOW));
        for (BWTarget<?> t : BW_TARGETS) {
            lines.add(plain("  · " + t.featureId() + "（" + t.noun + "）", ChatFormatting.DARK_GRAY));
        }
        lines.add(spacer());

        lines.add(section("村民交易缓存"));
        lines.add(cmd("/sa trades count", "查看已缓存的村民交易条数"));
        lines.add(cmd("/sa trades clear", "清空全部村民交易缓存（清空后需重新打开交易列表才会再次高亮）"));
        return lines;
    }

    private static Component section(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
    }

    private static Component plain(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static Component cmd(String usage, String desc) {
        return Component.literal(usage).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  " + desc).withStyle(ChatFormatting.WHITE));
    }

    private static Component spacer() {
        return Component.literal(" ");
    }

}