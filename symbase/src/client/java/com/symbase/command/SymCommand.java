package com.symbase.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.symbase.config.ConfigServices;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.gui.screen.FunctionMenuScreen;
import com.symbase.registry.FunctionRegistry;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /sym 指令
 */
public final class SymCommand {
    private static final String NS = "sym";

    private SymCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal(NS)
                .executes(SymCommand::showMenu)
                .then(literal("help").executes(SymCommand::showUsage))
                .then(literal("list").executes(SymCommand::listFunctions))
                .then(literal("info")
                        .then(argument("function", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (SymAbstractFunction f : FunctionRegistry.all()) {
                                        builder.suggest(f.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(SymCommand::showFunctionInfo)))
                .then(literal("enable")
                        .then(argument("function", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (SymAbstractFunction f : FunctionRegistry.all()) {
                                        builder.suggest(f.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> setEnable(ctx, null))
                                .then(argument("state", BoolArgumentType.bool())
                                        .executes(ctx -> setEnable(ctx, BoolArgumentType.getBool(ctx, "state"))))))
                .then(literal("toggle")
                        .then(argument("function", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (SymAbstractFunction f : FunctionRegistry.all()) {
                                        builder.suggest(f.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(SymCommand::toggleFunction))));

        // 兼容 /sym:xxx 形式
        dispatcher.register(literal(NS + ":menu").executes(SymCommand::showMenu));
        dispatcher.register(literal(NS + ":help").executes(SymCommand::showUsage));
        dispatcher.register(literal(NS + ":list").executes(SymCommand::listFunctions));
        dispatcher.register(literal(NS + ":info")
                .then(argument("function", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SymAbstractFunction f : FunctionRegistry.all()) {
                                builder.suggest(f.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(SymCommand::showFunctionInfo)));
        dispatcher.register(literal(NS + ":enable")
                .then(argument("function", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SymAbstractFunction f : FunctionRegistry.all()) {
                                builder.suggest(f.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> setEnable(ctx, null))
                        .then(argument("state", BoolArgumentType.bool())
                                .executes(ctx -> setEnable(ctx, BoolArgumentType.getBool(ctx, "state"))))));
        dispatcher.register(literal(NS + ":toggle")
                .then(argument("function", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SymAbstractFunction f : FunctionRegistry.all()) {
                                builder.suggest(f.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(SymCommand::toggleFunction)));
    }

    private static int showMenu(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new FunctionMenuScreen(null)));
        return 1;
    }

    private static int showUsage(CommandContext<FabricClientCommandSource> ctx) {
        for (Component line : usageLines()) {
            ctx.getSource().sendFeedback(line);
        }
        return 1;
    }

    private static int listFunctions(CommandContext<FabricClientCommandSource> ctx) {
        for (SymAbstractFunction function : FunctionRegistry.all()) {
            String state = function.isEnable() ? "§a开启§r" : "§c关闭§r";
            ctx.getSource().sendFeedback(Component.literal(
                    state + " §7" + function.getId() + "§r " + function.getName()));
        }
        return 1;
    }

    private static int showFunctionInfo(CommandContext<FabricClientCommandSource> ctx) {
        SymAbstractFunction function = findFunction(StringArgumentType.getString(ctx, "function"));
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + StringArgumentType.getString(ctx, "function")));
            return 0;
        }
        String state = function.isEnable() ? "§a开启§r" : "§c关闭§r";
        ctx.getSource().sendFeedback(Component.literal("功能：§e" + function.getName() + "§r（" + state + "§r）"));
        ctx.getSource().sendFeedback(Component.literal("ID：§7" + function.getId()));
        ctx.getSource().sendFeedback(Component.literal("说明：§f" + function.getTooltip()));
        return 1;
    }

    private static int setEnable(CommandContext<FabricClientCommandSource> ctx, Boolean state) {
        SymAbstractFunction function = findFunction(StringArgumentType.getString(ctx, "function"));
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + StringArgumentType.getString(ctx, "function")));
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
        SymAbstractFunction function = findFunction(StringArgumentType.getString(ctx, "function"));
        if (function == null) {
            ctx.getSource().sendError(Component.literal("未知功能：§c" + StringArgumentType.getString(ctx, "function")));
            return 0;
        }
        function.toggle();
        ConfigServices.saveAll();
        String result = function.isEnable() ? "§a已开启§r" : "§c已关闭§r";
        ctx.getSource().sendFeedback(Component.literal("§e" + function.getName() + "§r " + result));
        return 1;
    }

    private static SymAbstractFunction findFunction(String id) {
        for (SymAbstractFunction function : FunctionRegistry.all()) {
            if (function.getId().equalsIgnoreCase(id)) {
                return function;
            }
        }
        return null;
    }

    private static Component[] usageLines() {
        return new Component[]{
                Component.literal("—— 公共客户端命令 ——").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN),
                Component.literal("/sym").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  打开公共功能菜单").withStyle(ChatFormatting.WHITE)),
                Component.literal("/sym:list").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  列出全部功能").withStyle(ChatFormatting.WHITE)),
                Component.literal("/sym:info <function>").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  查看功能详情").withStyle(ChatFormatting.WHITE)),
                Component.literal("/sym:enable <function> [true|false]").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  查询或设置功能开关").withStyle(ChatFormatting.WHITE)),
                Component.literal("/sym:toggle <function>").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("  切换功能开关").withStyle(ChatFormatting.WHITE))
        };
    }
}
