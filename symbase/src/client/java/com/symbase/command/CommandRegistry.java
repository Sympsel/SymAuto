package com.symbase.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端指令注册表
 */
public final class CommandRegistry {
    public interface Contributor {
        void register(CommandDispatcher<FabricClientCommandSource> dispatcher);
    }

    private static final List<Contributor> CONTRIBUTORS = new ArrayList<>();
    private static boolean initialized;

    private CommandRegistry() {
    }

    public static void register(Contributor contributor) {
        CONTRIBUTORS.add(contributor);
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (Contributor contributor : List.copyOf(CONTRIBUTORS)) {
                contributor.register(dispatcher);
            }
        });
    }
}
