package com.symauto;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.function.FixYPlaceFunction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.InputKey;
import net.pemiridosa.combind.api.KeyCombo;
import org.lwjgl.glfw.GLFW;

public class SymautoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyCombo combo = new KeyCombo(
                InputKey.keyboard(GLFW.GLFW_KEY_X),
                new InputKey[]{
                        InputKey.keyboard(GLFW.GLFW_KEY_LEFT_SHIFT)}
        );

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("symauto", "main")
        );

        // 先创建原始 KeyMapping
        KeyMapping rawMapping = new KeyMapping(
                "key.symauto.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_X,
                category
        );

        KeyMappingHelper.registerKeyMapping(rawMapping);

        CombindKeyBinding openMenuBinding = CombindKeyBinding.of(rawMapping, combo);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (FixYPlaceFunction.isHolding() && !client.options.keyUse.isDown()) {
                FixYPlaceFunction.endHold();
            }

            // 左键松手
            if (FixYPlaceFunction.isMining() && !client.options.keyAttack.isDown()) {
                FixYPlaceFunction.endMining();
            }

            while (openMenuBinding.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new FeatureMenuScreen());
                }
            }
        });
    }
}