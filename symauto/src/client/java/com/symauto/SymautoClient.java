package com.symauto;

import com.symauto.command.SymAutoCommand;
import com.symbase.command.CommandRegistry;
import com.symauto.config.*;
import com.symauto.core.Constants;
import com.symauto.core.AutoFunctions;
import com.symauto.core.StoreConfig;
import com.symbase.config.ConfigServices;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.registry.FunctionRegistry;
import com.symbase.registry.MenuRegistry;
import com.symauto.function.auto.AutoSwiftToolsFunction;
import com.symauto.function.place.FixYPlaceOrDestroyFunction;
import com.symbase.function.utils.ClientGuard;
import com.symbase.function.utils.KeyUtils;
import com.symbase.function.utils.ScreenUtils;
import com.symauto.gui.screen.ClaimScreen;
import com.symauto.gui.screen.CommandUsageScreen;
import com.symbase.gui.screen.UpdateLogScreen;
import com.symbase.storage.base.PersistentStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class SymautoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册事件
        this.eventRegisters();

        // 先注册功能，再加载配置
        for (SymAbstractFunction function : AutoFunctions.AUTO_ALL) {
            FunctionRegistry.register(function);
        }
        ConfigServices.register(ConfigManager::save);
        ConfigManager.load();
        // 读取功能 MOD 自己的存储
        for (PersistentStore store : StoreConfig.ALL) {
            store.load();
        }

        registerMenuEntries();
        CommandRegistry.register(SymAutoCommand::register);
        Constants.LOG.info("SymAuto Client initialized");
    }

    private void registerMenuEntries() {
        MenuRegistry.register(MenuRegistry.Section.OPEN,
                Component.literal("打开 SymAuto 配置"),
                parent -> ConfigManager::openConfigFolder);
        MenuRegistry.register(MenuRegistry.Section.HELP,
                Component.literal("指令用法"),
                parent -> () -> Minecraft.getInstance().gui.setScreen(new CommandUsageScreen(parent)));
        MenuRegistry.register(MenuRegistry.Section.ABOUT,
                Component.literal("SymAuto 更新日志"),
                parent -> () -> Minecraft.getInstance().gui.setScreen(new UpdateLogScreen(parent, "symauto", Component.literal("SymAuto 更新日志"))));
        MenuRegistry.register(MenuRegistry.Section.ABOUT,
                Component.literal("声明"),
                parent -> () -> Minecraft.getInstance().gui.setScreen(new ClaimScreen(parent)));
    }

    private void eventRegisters() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            for (SymAbstractFunction f : AutoFunctions.AUTO_ALL) {
                try {
                    f.setEnable(false);
                } catch (Throwable t) {
                    // 关游戏时世界/注册表可能已拆除，个别功能清理失败不应阻断其余清理与配置落盘
                    Constants.LOG.error("Failed to disable function '{}' on shutdown", f.getId(), t);
                }
            }
            for (PersistentStore store : StoreConfig.ALL) {
                store.saveIfDirty();
            }
        });

        // 容器界面事件分发：鼠标点击
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                if (!(s instanceof AbstractContainerScreen<?> cs)) return true;
                for (SymAbstractFunction f : AutoFunctions.AUTO_ALL) {
                    if (!f.isEnable()) continue;
                    if (!f.allowContainerMouseClick(client, cs, event.x(), event.y(), event.button())) {
                        return false;
                    }
                }
                return true;
            });

        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!ClientGuard.ready(client)) {
                return;
            }
            if (AutoFunctions.AUTO_SWIFT_TOOLS_FUNCTION.isEnable()) {
                ((AutoSwiftToolsFunction) AutoFunctions.AUTO_SWIFT_TOOLS_FUNCTION).earlySwitchBack(client);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ClientGuard.ready(client)) {
                return;
            }

            if (FixYPlaceOrDestroyFunction.isHolding() && !client.options.keyUse.isDown()) {
                FixYPlaceOrDestroyFunction.endHold();
            }
            if (FixYPlaceOrDestroyFunction.isMining() && !client.options.keyAttack.isDown()) {
                FixYPlaceOrDestroyFunction.endMining();
            }

            // 遍历所有功能：持续型 tick + 事件型 key
            for (SymAbstractFunction f : AutoFunctions.AUTO_ALL) {
                if (!f.isEnable()) {
                    continue;
                }
                f.tick();
            }
        });
    }
}









