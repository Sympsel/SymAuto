package com.symauto;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoSwiftToolsFunction;
import com.symauto.function.functions.FixYPlaceOrDestroyFunction;
import com.symauto.gui.FeatureMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SymautoClient implements ClientModInitializer {

    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("symauto", "main")
        );

        openMenuKey = new KeyMapping(
                "key.symauto.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                category
        );

        KeyMappingHelper.registerKeyMapping(openMenuKey);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (FeatureConfig.AUTO_SWIFT_TOOLS_FUNCTION.isEnable() && client.player != null) {
                ((AutoSwiftToolsFunction) FeatureConfig.AUTO_SWIFT_TOOLS_FUNCTION).earlySwitchBack(client);
            }
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (FixYPlaceOrDestroyFunction.isHolding() && !client.options.keyUse.isDown()) {
                FixYPlaceOrDestroyFunction.endHold();
            }

            if (FixYPlaceOrDestroyFunction.isMining() && !client.options.keyAttack.isDown()) {
                FixYPlaceOrDestroyFunction.endMining();
            }

            if (openMenuKey.isDown() && InputConstants.isKeyDown(
                    client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new FeatureMenuScreen(null));
                }
            }

            for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                if (f.isEnable()) {
                    f.tick();
                }
            }
        });
    }
}