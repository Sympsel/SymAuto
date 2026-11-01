package com.symbase;

import com.mojang.blaze3d.platform.InputConstants;
import com.symbase.command.CommandRegistry;
import com.symbase.command.SymCommand;
import com.symbase.config.ConfigServices;
import com.symbase.config.FunctionConfig;
import com.symbase.config.SymBaseKeys;
import com.symbase.function.utils.KeyUtils;
import com.symbase.function.utils.ScreenUtils;
import com.symbase.gui.screen.FunctionMenuScreen;
import com.symbase.hud.HudManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
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
        KeyMappingHelper.registerKeyMapping(openMenuKey);

        HudManager.init();
        ConfigServices.register(FunctionConfig::save);
        CommandRegistry.register(SymCommand::register);
        CommandRegistry.init();

        // 等所有功能 MOD 注册完功能与 HUD 元素后再读取配置
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
    }
}
