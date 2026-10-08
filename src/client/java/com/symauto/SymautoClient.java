package com.symauto;

import com.betterbundle.gui.BundlePanelInteraction;
import com.betterbundle.gui.BundlePanelRenderer;
import com.betterbundle.mixin.AbstractContainerScreenAccessor;
import com.betterbundle.util.BundleContentsHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.command.SymAutoCommand;
import com.symauto.config.ConfigManager;
import com.symauto.function.FeatureConfig;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.functions.AutoSwiftToolsFunction;
import com.symauto.function.functions.FixYPlaceOrDestroyFunction;
import com.symauto.function.functions.OneClickFlatItems;
import com.symauto.function.utils.Constants;
import com.symauto.function.utils.ItemUtils;
import com.symauto.gui.BundleQuickViewScreen;
import com.symauto.gui.FeatureMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class SymautoClient implements ClientModInitializer {

    private KeyMapping openMenuKey;
    private KeyMapping bundleQuickOpenKey;
    private KeyMapping oneClickFlatItems;
    private boolean flatComboWasDown = false;



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

        bundleQuickOpenKey = new KeyMapping(
                "key.symauto.bundle_quick_open",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                category
        );

        oneClickFlatItems = new KeyMapping(
                "key.symauto.one_click_flat_items",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_A,
                category
        );

        registerKeyMapping();


        // 客户端停止时禁用所有功能
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            for (SymAbstractFunction f : FeatureConfig.AUTO_ALL) {
                f.setEnable(false);
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                    if (!FeatureConfig.ONE_CLICK_DISCARD_SAME_ITEMS_FUNCTION.isEnable()) {
                        return true;
                    }
                    if (!(s instanceof AbstractContainerScreen<?> cs)) {
                        return true;
                    }
                    if (client.player == null || client.gameMode == null) {
                        return true;
                    }

                    AbstractContainerMenu menu = cs.getMenu();
                    ItemStack carried = menu.getCarried();
                    if (carried.isEmpty()) return true;

                    double mouseX = event.x();
                    double mouseY = event.y();

                    AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) cs;
                    int xo = acc.getLeftPos();
                    int yo = acc.getTopPos();
                    int imgW = acc.getImageWidth();
                    int imgH = acc.getImageHeight();
                    boolean outside = mouseX < xo || mouseY < yo
                            || mouseX >= xo + imgW || mouseY >= yo + imgH;
                    if (!outside) return true;

                    // 兼容便捷收纳袋：拖到收纳袋界面不丢弃
                    if (FeatureConfig.BETTER_BUNDLE_FUNCTION.isEnable()
                    && BundlePanelRenderer.visible
                    && BundlePanelInteraction.isInsidePanel(mouseX, mouseY, xo, yo, imgH)) {
                        return true;
                    }

                    ItemStack target = carried.copy();

                    client.gameMode.handleContainerInput(menu.containerId, -999, 0, ContainerInput.PICKUP, client.player);

                    // 再遍历所有槽位丢相同的
                    if (menu == client.player.inventoryMenu) {
                        for (int i = Constants.MAIN_INVENTORY_START; i < menu.slots.size(); i++) {
                            if (i == Constants.OFFHAND) continue;
                            Slot slot = menu.getSlot(i);
                            if (slot.hasItem() && ItemUtils.isSameItem(target, slot.getItem())) {
                                client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                            }
                        }
                    } else {
                        int containerEnd = menu.slots.size() - Constants.PLAYER_SLOT_COUNT;
                        for (int i = 0; i < containerEnd; i++) {
                            Slot slot = menu.getSlot(i);
                            if (slot.hasItem() && ItemUtils.isSameItem(target, slot.getItem())) {
                                client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                            }
                        }
                        for (int i = containerEnd; i < menu.slots.size(); i++) {
                            if (i == Constants.OFFHAND) continue;
                            Slot slot = menu.getSlot(i);
                            if (slot.hasItem() && ItemUtils.isSameItem(target, slot.getItem())) {
                                client.gameMode.handleContainerInput(menu.containerId, i, 1, ContainerInput.THROW, client.player);
                            }
                        }
                    }

                    return false;
                });
            }
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

            if (FeatureConfig.BUNDLE_QUICK_OPEN_FUNCTION.isEnable()
                    && client.gui.screen() == null
                    && bundleQuickOpenKey.isDown()
                    && BundleContentsHelper.isNonEmptyBundle(client.player.getMainHandItem())) {
                int bundleSlot = 36 + client.player.getInventory().getSelectedSlot();
                client.gui.setScreen(new BundleQuickViewScreen(bundleSlot));
            }

            if (FeatureConfig.ONE_CLICK_FLAT_ITEMS_FUNCTION.isEnable()
                    && client.gui.screen() instanceof AbstractContainerScreen<?>
            ) {
                int bindKey = oneClickFlatItems.getDefaultKey().getValue();
                boolean keyDown = bindKey != GLFW.GLFW_KEY_UNKNOWN
                        && InputConstants.isKeyDown(client.getWindow(), bindKey);
                boolean ctrlDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean comboDown = keyDown && ctrlDown;

                // 上升沿触发一次；松手后重置
                if (comboDown && !flatComboWasDown) {
                    ((OneClickFlatItems) FeatureConfig.ONE_CLICK_FLAT_ITEMS_FUNCTION).trigger(client);
                }
                flatComboWasDown = comboDown;

                // 把 KeyMapping 的 clickCount 排空，防止它污染其它逻辑
                while (oneClickFlatItems.consumeClick()) {}
            } else {
                flatComboWasDown = false;
            }
        });
        ConfigManager.load();
        SymAutoCommand.register();
    }

    private void registerKeyMapping() {
        KeyMappingHelper.registerKeyMapping(openMenuKey);
        KeyMappingHelper.registerKeyMapping(bundleQuickOpenKey);
        KeyMappingHelper.registerKeyMapping(oneClickFlatItems);
    }
}