package com.symauto.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.symauto.config.ConfigManager;
import com.symauto.core.Constants;
import com.symauto.function.auto.AutoAttackFunction;
import com.symauto.function.highlight.HighLightSpecificVillagersFunction;
import com.symauto.gui.screen.PilotScreen;
import com.symauto.gui.screen.FunctionMenuScreen;
import com.symauto.model.common.SymList;
import com.symauto.storage.DeathInfoStore;
import com.symauto.storage.VillagerTradeStore;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.auto.AutoEatFunction;
import com.symauto.function.highlight.HighLightItemDropFunction;
import com.symauto.function.highlight.HighLightMasterFunction;
import com.symauto.function.oneclick.OneClickDiscardItemsFunction;
import com.symauto.gui.screen.function.DeathRecordScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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


public class SymAutoCommand {
    private static final String NS = Constants.MOD_ID;
    private static final String NS_SIMPLIFIED = "sa";

    /**
     * 泛型目标：BWList + 其序列化/解析适配器 + 名词标签。
     */
    private record BWTarget<T>(SymList<T> list, Function<T, String> toId, Function<String, T> parser, String noun,
                               boolean isEntity) {

        String functionId() {
            return list.getFunctionId();
        }
    }

    // 黑白名单数据源
    private static final List<BWTarget<?>> WHITELIST_TARGETS = List.of(
            itemTarget(OneClickDiscardItemsFunction.INSTANCE.getWHITE_LIST()),
            itemTarget(HighLightItemDropFunction.INSTANCE.getWHITE_LIST()),
            entityTarget(HighLightMasterFunction.INSTANCE.getWHITE_LIST())
    );

    private static final List<BWTarget<?>> BLACKLIST_TARGETS = List.of(
            itemTarget(AutoEatFunction.INSTANCE.getBLACK_LIST()),
            itemTarget(HighLightItemDropFunction.INSTANCE.getBLACK_LIST()),
            itemTarget(OneClickDiscardItemsFunction.INSTANCE.getBLACK_LIST()),
            entityTarget(AutoAttackFunction.INSTANCE.getBLACK_LIST()),
            entityTarget(HighLightMasterFunction.INSTANCE.getBLACK_LIST()),
            enchantmentTarget(HighLightSpecificVillagersFunction.INSTANCE.getBLACK_LIST())
    );

    private static final SuggestionProvider<FabricClientCommandSource> FUNCTION_IDS =
            (context, builder) -> {
                for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
                    builder.suggest(f.getId());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<FabricClientCommandSource> BW_FEATURES =
            (context, builder) -> {
                Set<String> seen = new HashSet<>();
                for (BWTarget<?> t : allTargets()) {
                    if (seen.add(t.functionId())) {
                        builder.suggest(t.functionId());
                    }
                }
                return builder.buildFuture();
            };

    // 值补全：实体类型功能补全 entity type id，其余补全 item id
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

    public static void register() {
        register(NS);
        register(NS_SIMPLIFIED);
    }

    public static void register(String ns) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // 构建命令森林
            dispatcher.register(literal(ns).executes(SymAutoCommand::showMenu));
            dispatcher.register(literal(ns + ":menu").executes(SymAutoCommand::showMenu));
            dispatcher.register(literal(ns + ":help").executes(SymAutoCommand::showUsage));
            dispatcher.register(literal(ns + ":list").executes(SymAutoCommand::listFunctions));
            dispatcher.register(literal(ns + ":info")
                    .then(argument("function", StringArgumentType.word())
                            .suggests(FUNCTION_IDS)
                            .executes(SymAutoCommand::showFunctionInfo)));
            dispatcher.register(literal(ns + ":enable")
                    .then(argument("function", StringArgumentType.word())
                            .suggests(FUNCTION_IDS)
                            .executes(ctx -> setEnable(ctx, null))
                            .then(argument("state", BoolArgumentType.bool())
                                    .executes(ctx -> setEnable(ctx, BoolArgumentType.getBool(ctx, "state"))))));
            dispatcher.register(literal(ns + ":toggle")
                    .then(argument("function", StringArgumentType.word())
                            .suggests(FUNCTION_IDS)
                            .executes(SymAutoCommand::toggleFunction)));
            dispatcher.register(literal(ns + ":trades")
                    .then(literal("count").executes(SymAutoCommand::showTradeCount))
                    .then(literal("clear").executes(SymAutoCommand::clearTrades)));
            dispatcher.register(literal(ns + ":deaths")
                    .executes(SymAutoCommand::showDeathList)
                    .then(literal("gui").executes(SymAutoCommand::openDeathGui))
                    .then(literal("latest").executes(SymAutoCommand::showLatestDeath))
                    .then(literal("clear").executes(SymAutoCommand::clearDeaths)));
            //  调出试点屏幕
            dispatcher.register(literal(ns + ":pilot").executes(SymAutoCommand::openPilot));
            dispatcher.register(literal(ns + ":bwlist")
                    .then(argument("function", StringArgumentType.word())
                            .suggests(BW_FEATURES)
                            .then(argument("type", StringArgumentType.word())
                                    .suggests((ctx, builder) -> builder.suggest("blacklist").suggest("whitelist").buildFuture())
                                    .then(argument("action", StringArgumentType.word())
                                            .suggests((ctx, builder) -> builder.suggest("add").suggest("remove").suggest("list").suggest("clear").buildFuture())
                                            .executes(SymAutoCommand::handleBWList)
                                            .then(argument("value", StringArgumentType.greedyString())
                                                    .suggests(BW_VALUE_IDS)
                                                    .executes(SymAutoCommand::handleBWListWithValue))))));
        });
    }

    private static int openPilot(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new PilotScreen()));
        return 1;
    }

    private static int showMenu(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new FunctionMenuScreen(null)));
        return 1;
    }

    private static int showUsage(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal(
                "用法：/<" + NS + "|" + NS_SIMPLIFIED + ">:<help|list|info|enable|toggle|trades|deaths|bwlist>"
        ));
        return 1;
    }

    private static int listFunctions(CommandContext<FabricClientCommandSource> ctx) {
        for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
            String state = f.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal(state + " §7" + f.getId() + "§r " + f.getName()));
        }
        return 1;
    }

    private static int showFunctionInfo(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction f = findFunction(id);
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
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction f = findFunction(id);
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

    private static int toggleFunction(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "function");
        SymAbstractFunction f = findFunction(id);
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

    private static int showDeathList(CommandContext<FabricClientCommandSource> ctx) {
        List<DeathInfoStore.DeathRecord> records = DeathInfoStore.getRecords();
        if (records.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("§e死亡记录§r 为空"));
            return 0;
        }
        int total = records.size();
        ctx.getSource().sendFeedback(Component.literal("§e死亡记录§r 共 " + total + " 条（最新在前）："));
        for (int i = total - 1; i >= 0; i--) {
            ctx.getSource().sendFeedback(deathLine(total - i, records.get(i)));
        }
        ctx.getSource().sendFeedback(Component.literal(
                "§7输入 /<" + NS + "|" + NS_SIMPLIFIED + ">:deaths gui 打开图形界面查看")
        );
        return total;
    }

    private static Component deathLine(int index, DeathInfoStore.DeathRecord r) {
        return Component.literal("§7#" + index + " §f" + r.formatWallTime()
                + " §7| §a" + r.dimension + " §7| §e(" + r.formatCoordinate() + ")"
                + " §7| §c" + r.formatCause());
    }

    private static int openDeathGui(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new DeathRecordScreen(null)));
        return 1;
    }

    private static int showLatestDeath(CommandContext<FabricClientCommandSource> ctx) {
        DeathInfoStore.DeathRecord latest = DeathInfoStore.latest();
        if (latest == null) {
            ctx.getSource().sendFeedback(Component.literal("§e死亡记录§r 为空"));
            return 0;
        }
        ctx.getSource().sendFeedback(Component.literal("§e最近一次死亡§r（共 " + DeathInfoStore.size() + " 条）："));
        ctx.getSource().sendFeedback(deathLine(DeathInfoStore.size(), latest));
        return 1;
    }

    private static int clearDeaths(CommandContext<FabricClientCommandSource> ctx) {
        int removed = DeathInfoStore.clear();
        ctx.getSource().sendFeedback(Component.literal("已清空死亡记录（移除 §f" + removed + "§r 条）"));
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
        for (SymAbstractFunction f : FunctionConfig.AUTO_ALL) {
            if (f.getId().equalsIgnoreCase(id)) {
                return f;
            }
        }
        return null;
    }

    private static boolean isEntityFunction(String functionId) {
        if (functionId == null) {
            return false;
        }
        for (BWTarget<?> t : allTargets()) {
            if (t.functionId().equalsIgnoreCase(functionId)) {
                return t.isEntity;
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
        for (BWTarget<?> t : source) {
            if (t.functionId().equalsIgnoreCase(functionId)) {
                return t;
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
                SymAutoCommand::parseItem,
                "物品",
                false
        );
    }

    private static BWTarget<EntityType<?>> entityTarget(SymList<EntityType<?>> list) {
        return new BWTarget<>(list,
                type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                SymAutoCommand::parseEntityType,
                "实体类型",
                true
        );
    }

    private static BWTarget<ResourceKey<Enchantment>> enchantmentTarget(SymList<ResourceKey<Enchantment>> list) {
        return new BWTarget<>(list,
                key -> key.identifier().toString(),
                SymAutoCommand::parseEnchantmentType,
                "附魔书类型",
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

    private static ResourceKey<Enchantment> parseEnchantmentType(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return ResourceKey.create(Registries.ENCHANTMENT, identifier);
    }

    // 供 CommandUsageScreen 渲染
    public static List<Component> usageLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(section("—— 客户端命令（/symauto:xxx） ——"));
        lines.add(plain("均为客户端命令，单人/联机均可使用", ChatFormatting.GRAY));
        lines.add(spacer());

        lines.add(section("功能管理"));
        lines.add(cmd("/symauto:list", "列出全部功能及其开关状态"));
        lines.add(cmd("/symauto:info <function>", "查看指定功能的详细信息与说明"));
        lines.add(cmd("/symauto:enable <function> [true|false]", "查询或设置某功能的开关；不带参数则查询"));
        lines.add(cmd("/symauto:toggle <function>", "切换某功能的开关状态"));
        lines.add(spacer());

        lines.add(section("黑白名单"));
        lines.add(cmd("/symauto:bwlist <function> <blacklist|whitelist> list", "查看指定名单的全部条目"));
        lines.add(cmd("/symauto:bwlist <function> <blacklist|whitelist> clear", "清空指定名单"));
        lines.add(cmd("/symauto:bwlist <function> <blacklist|whitelist> add|remove <id>", "增删一个条目（id 为物品或实体类型）"));
        lines.add(plain("可管理的名单 function：", ChatFormatting.YELLOW));
        for (BWTarget<?> t : allTargets()) {
            lines.add(plain("  · " + t.functionId() + "（" + t.list.getType() + "·" + t.noun + "）", ChatFormatting.DARK_GRAY));
        }
        lines.add(spacer());

        lines.add(section("村民交易缓存"));
        lines.add(cmd("/symauto:trades count", "查看已缓存的村民交易条数"));
        lines.add(cmd("/symauto:trades clear", "清空全部村民交易缓存（清空后需重新打开交易列表才会再次高亮）"));

        lines.add(section("死亡记录"));
        lines.add(cmd("/symauto:deaths", "在聊天框列出全部死亡记录"));
        lines.add(cmd("/symauto:deaths gui", "打开死亡记录图形界面"));
        lines.add(cmd("/symauto:deaths latest", "仅查看最近一次死亡的详情"));
        lines.add(cmd("/symauto:deaths clear", "清空全部死亡记录"));
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