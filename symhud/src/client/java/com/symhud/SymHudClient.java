package com.symhud;

import com.symbase.command.CommandRegistry;
import com.symbase.config.ConfigServices;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.utils.ClientGuard;
import com.symbase.gui.screen.HudEditScreen;
import com.symbase.gui.screen.SymModMenuRegistry;
import com.symbase.gui.screen.UpdateLogScreen;
import com.symbase.gui.widget.component.SymMenuBar;
import com.symbase.hud.HudManager;
import com.symbase.registry.FunctionRegistry;
import com.symbase.registry.MenuRegistry;
import com.symhud.command.SymHudCommand;
import com.symhud.config.ConfigManager;
import com.symhud.core.Constants;
import com.symhud.core.HudFunctions;
import com.symhud.function.utils.TradeCapture;
import com.symhud.gui.container.ContainerPanel;
import com.symhud.gui.container.ContainerPreviewRenderer;
import com.symhud.hud.element.*;
import com.symhud.render.PlayerNameRenderer;
import com.symhud.storage.VillagerTradeStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class SymHudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SymMenuBar.Entry hudEntry = new SymMenuBar.Entry(Component.literal("HUD 编辑"),
                () -> Minecraft.getInstance().gui.setScreen(new HudEditScreen(null)));
        SymModMenuRegistry.INSTANCE.registerMenuEntry(hudEntry);

        MenuRegistry.register(MenuRegistry.Section.ABOUT,
                Component.literal("SymHud 更新日志"),
                parent -> () -> Minecraft.getInstance().gui.setScreen(
                        new UpdateLogScreen(parent, Constants.MOD_ID, Component.literal("SymHud 更新日志"))));

        MenuRegistry.register(MenuRegistry.Section.OPEN,
                Component.literal("打开 SymHud 配置"),
                parent -> ConfigManager::openConfigFolder);

        for (SymAbstractFunction function : HudFunctions.ALL) {
            FunctionRegistry.register(function);
        }
        ConfigServices.register(ConfigManager::save);
        ConfigManager.load();
        CommandRegistry.register(SymHudCommand::register);
        VillagerTradeStore.INSTANCE.load();
        PlayerNameRenderer.register();
        ContainerPreviewRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(TradeCapture::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ClientGuard.ready(client)) {
                return;
            }
            for (SymAbstractFunction function : HudFunctions.ALL) {
                if (function.isEnable()) {
                    function.tick();
                }
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return;
            }
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) ->
                    event.button() != 0 || !ContainerPanel.consumeReleaseGuard());
        });
        registerHudElements();
    }

    private void registerHudElements() {
        HudManager.register(new FpsHudElement());
        HudManager.register(new CoordinatesHudElement());
        HudManager.register(new AnotherCoordinateHudElement());
        HudManager.register(new ClockHudElement());
        HudManager.register(new HitResultIdHudElement());
        HudManager.register(new TargetDistanceHudElement());
        HudManager.register(new ArmorDurabilityHudElement());
        HudManager.register(new ItemTotalCountHudElement());
    }
}

