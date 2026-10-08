package com.symauto;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.command.SymAutoCommand;
import com.symauto.config.ConfigManager;
import com.symauto.config.SymAutoKeys;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoSwiftToolsFunction;
import com.symauto.function.functions.FixYPlaceOrDestroyFunction;
import com.symauto.function.utils.KeyUtils;
import com.symauto.function.utils.ScreenUtils;
import com.symauto.gui.FeatureMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class SymautoClient implements ClientModInitializer {
    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = new KeyMapping(
                "key.symauto.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                SymAutoKeys.CATEGORY_MAIN);
        KeyMappingHelper.registerKeyMapping(openMenuKey);

        // 每个功能自己交出 KeyMapping，主类统一注册
        for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
            KeyMapping km = f.getKeyMapping();
            if (km != null) {
                KeyMappingHelper.registerKeyMapping(km);
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) f.setEnable(false);
        });

        // 容器界面事件分发：鼠标点击
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                if (!(s instanceof AbstractContainerScreen<?> cs)) return true;
                for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                    if (!f.isEnable()) continue;
                    if (!f.allowContainerMouseClick(client, cs, event.x(), event.y(), event.button())) {
                        return false;
                    }
                }
                return true;
            });
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (FeatureConfig.AUTO_SWIFT_TOOLS_FUNCTION.isEnable() && client.player != null) {
                ((AutoSwiftToolsFunction) FeatureConfig.AUTO_SWIFT_TOOLS_FUNCTION).earlySwitchBack(client);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.gameMode == null || client.player == null) {
                return;
            }

            if (FixYPlaceOrDestroyFunction.isHolding() && !client.options.keyUse.isDown()) {
                FixYPlaceOrDestroyFunction.endHold();
            }
            if (FixYPlaceOrDestroyFunction.isMining() && !client.options.keyAttack.isDown()) {
                FixYPlaceOrDestroyFunction.endMining();
            }

            // 主菜单入口（元操作）
            if (openMenuKey.isDown()
                    && KeyUtils.isShiftDown(client)
                    && ScreenUtils.isNoScreen(client)) {
                client.gui.setScreen(new FeatureMenuScreen(null));
            }

            // 遍历所有功能：持续型 tick + 事件型 key
            for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                if (!f.isEnable()) continue;
                f.tick();
                dispatchKey(client, f);
            }
        });

        ConfigManager.load();
        SymAutoCommand.register();
    }

    /** 快捷键分发：边沿触发 + 修饰键 + 界面上下文过滤 */
    private static void dispatchKey(Minecraft client, SymAbstractFunction f) {
        KeyMapping km = f.getKeyMapping();
        if (km == null) return;
        if (!KeyUtils.risingEdge(client, f.getId(), km)) return;
        if (f.requireCtrl() && !KeyUtils.isCtrlDown(client)) return;
        if (f.requireShift() && !KeyUtils.isShiftDown(client)) return;
        if (!ScreenUtils.screenContextMatches(client, f.requireScreenContext())) return;
        while (km.consumeClick()) {}
        f.onKeyAction(client);
    }
}