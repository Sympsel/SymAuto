package com.symbase;

import com.mojang.blaze3d.platform.InputConstants;
import com.symbase.command.CommandRegistry;
import com.symbase.command.SymCommand;
import com.symbase.config.ConfigFolders;
import com.symbase.config.ConfigServices;
import com.symbase.config.FunctionConfig;
import com.symbase.config.SymBaseKeys;
import com.symbase.core.SymFunctionTags;
import com.symbase.function.base.HudVisibilityFunction;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.ClientGuard;
import com.symbase.function.util.KeyUtils;
import com.symbase.function.util.ScreenUtils;
import com.symbase.gui.screen.FunctionMenuScreen;
import com.symbase.registry.FunctionRegistry;
import com.symbase.registry.MenuRegistry;
import com.symbase.hud.HudManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SymBaseClient implements ClientModInitializer {
    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = new KeyMapping(
                "key.symbase.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                SymBaseKeys.CATEGORY_MAIN);
        registerKeyMapping();

        HudManager.init();
        FunctionRegistry.register(HudVisibilityFunction.INSTANCE.addTag(SymFunctionTags.HUD));
        ConfigServices.register(FunctionConfig::save);
        CommandRegistry.register(SymCommand::register);
        CommandRegistry.init();

        MenuRegistry.register(MenuRegistry.Section.OPEN,
                Component.literal("打开 SymBase 配置"),
                parent -> () -> ConfigFolders.open("symbase"));

        // 等所有功能 MOD 注册完读取配置
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FunctionConfig.load();
            HudManager.loadLayout();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            FunctionConfig.save();
            HudManager.saveLayout();
        });

        // 公共功能菜单入口：Shift + X
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !ScreenUtils.isNoScreen(client)) {
                return;
            }
            if (openMenuKey.isDown() && KeyUtils.isShiftDown(client)) {
                client.gui.setScreen(new FunctionMenuScreen(null));
            }
        });

        // 分发所有功能的快捷键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ClientGuard.ready(client)) {
                return;
            }
            for (SymAbstractFunction function : FunctionRegistry.all()) {
                dispatchKey(client, function);
            }
        });
    }

    private static void dispatchKey(Minecraft client, SymAbstractFunction function) {
        KeyMapping keyMapping = function.getKeyMapping();
        if (keyMapping == null) {
            return;
        }
        if (!KeyUtils.risingEdge(client, function.getId(), keyMapping)) {
            return;
        }
        if (function.requireCtrl() && !KeyUtils.isCtrlDown(client)) {
            return;
        }
        if (function.requireShift() && !KeyUtils.isShiftDown(client)) {
            return;
        }
        if (!ScreenUtils.screenContextMatches(client, function.requireScreenContext())) {
            return;
        }
        while (keyMapping.consumeClick()) {
        }
        function.onKeyAction(client);
    }

    /**
     * 容器界面内的快捷键分发。
     *
     * <p>原版 {@code KeyboardHandler} 只在没有打开任何界面时才调用 {@link KeyMapping#click}，
     * 所以界面打开期间 {@link KeyMapping#consumeClick()} 永远为空，{@link #dispatchKey} 不可能触发
     * 任何需要“界面打开”的功能（例如“一键平铺物品”）。这里改为直接拿原始按键事件匹配功能键位。</p>
     *
     * @return 按键是否已被某个功能消费；为 true 时调用方应取消原版处理，
     *         避免按键字符继续传给界面（如搜索框）
     */
    public static boolean handleContainerKeyPress(Minecraft client,
                                                  AbstractContainerScreen<?> screen,
                                                  KeyEvent event) {
        if (!ClientGuard.ready(client)) {
            return false;
        }
        // 焦点在文本框时优先交给界面处理（例如搜索栏的 Ctrl + A 全选）
        if (screen.getFocused() instanceof EditBox editBox && editBox.canConsumeInput()) {
            return false;
        }
        for (SymAbstractFunction function : FunctionRegistry.all()) {
            if (!function.isEnable()) {
                continue;
            }
            if (function.requireScreenContext() != SymAbstractFunction.ScreenContext.CONTAINER_SCREEN) {
                continue;
            }
            KeyMapping keyMapping = function.getKeyMapping();
            if (keyMapping == null || keyMapping.isUnbound() || !keyMapping.matches(event)) {
                continue;
            }
            if (function.requireCtrl() && !event.hasControlDown()) {
                continue;
            }
            if (function.requireShift() && !event.hasShiftDown()) {
                continue;
            }
            if (!function.allowContainerKeyPress(client, screen,
                    event.key(), event.scancode(), event.modifiers())) {
                return true;
            }
        }
        return false;
    }

    private void registerKeyMapping() {
        KeyMappingHelper.registerKeyMapping(openMenuKey);
    }
}

