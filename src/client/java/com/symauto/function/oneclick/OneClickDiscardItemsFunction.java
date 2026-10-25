package com.symauto.function.oneclick;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.symauto.config.option.BooleanOption;
import com.symauto.function.container.depend.BundleAccess;
import com.symauto.function.utils.BundleContentsHelper;
import com.symauto.function.utils.ContainerActions;
import com.symauto.model.common.SymList;
import com.symauto.model.common.KeyCombination;
import com.symauto.core.FunctionConfig;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.KeyUtils;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class OneClickDiscardItemsFunction extends SymAbstractFunction {
    public static final OneClickDiscardItemsFunction INSTANCE = new OneClickDiscardItemsFunction();
    // 边沿触发检测
    private boolean wasDown = false;
    @Getter
    @Setter
    private boolean discardItemInBundles = true;

    @Getter
    private final SymList<Item> WHITE_LIST;
    @Getter
    private final SymList<Item> BLACK_LIST;
    private static final KeyCombination KEY_COMBINATION =
            new KeyCombination("key.keyboard.q", GLFW.GLFW_MOD_ALT);

    private OneClickDiscardItemsFunction() {
        String id = "one_click_discard_items";
        WHITE_LIST = new SymList<>(id, SymList.Type.WHITELIST);
        BLACK_LIST = new SymList<>(id, SymList.Type.BLACKLIST);
        super(id, "一键丢弃垃圾物品");
        addOption(new BooleanOption(
                "discard_item_in_bundles",
                "是否丢弃收纳袋中的物品",
                this::isDiscardItemInBundles,
                this::setDiscardItemInBundles,
                true
        ));
        applyDefaults();
    }

    @Override
    public String tooltip() {
        return Tooltip.create()
                .line("Alt + Q 一键丢弃白名单内（" + WHITE_LIST.size() + "）物品")
                .keyValueLine("是否丢弃收纳袋里的垃圾物品", discardItemInBundles ? "是" : "否")
                .display(BLACK_LIST, item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (!isCombinationDown(client)) {
            wasDown = false;
            return;
        }

        if (wasDown) {
            // 松开才会再次触发
            return;
        }
        wasDown = true;

        discardWhitelisted(client);
    }

    private void discardWhitelisted(Minecraft client) {
        AbstractContainerMenu menu = client.player.containerMenu;
        int containerId = menu.containerId;

        // 先将收纳袋内的白名单物品提取到背包槽位
        extractFromBundles(client, menu, containerId);

        // 统一丢弃背包中所有白名单物品
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }

            // 跳过玩家身上穿的（盔甲 + 副手）
            if (isWornSlot(slot)) {
                continue;
            }

            Item item = slot.getItem().getItem();
            if (!WHITE_LIST.contains(item)) {
                continue;
            }

            client.gameMode.handleContainerInput(
                    containerId,
                    i,
                    1,
                    ContainerInput.THROW,
                    client.player
            );
        }
    }

    /**
     * 丢弃收纳袋中的白名单物品
     */
    private void extractFromBundles(Minecraft client, AbstractContainerMenu menu, int containerId) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;

        int emptySlot = -1;
        for (int i = 9; i <= 44 && i < menu.slots.size(); i++) {
            if (!menu.slots.get(i).hasItem()) {
                emptySlot = i;
                break;
            }
        }
        if (emptySlot < 0) return;

        for (int s = 0; s < menu.slots.size(); s++) {
            Slot slot = menu.slots.get(s);
            if (!slot.hasItem() || isWornSlot(slot)) continue;

            ItemStack bundleStack = slot.getItem();
            if (!BundleContentsHelper.isNonEmptyBundle(bundleStack)) continue;

            BundleContents contents = BundleContentsHelper.getContents(bundleStack);
            if (contents == null) continue;

            List<ItemStack> items = contents.itemCopyStream().toList();

            for (int i = items.size() - 1; i >= 0; i--) {
                if (!WHITE_LIST.contains(items.get(i).getItem())) {
                    continue;
                }

                BundleAccess.extract(s, i);
                ContainerActions.leftClickSlot(emptySlot);
                ContainerActions.dropSlotStack(emptySlot);
            }
        }
    }

    private static boolean isWornSlot(Slot slot) {
        if (slot.container instanceof Inventory inv) {
            // 玩家背包：36~39 盔甲，40 副手
            return slot.getContainerSlot() >= 36;
        }
        return false;
    }

    private boolean isCombinationDown(Minecraft client) {
        Window window = client.getWindow();
        if (!InputConstants.isKeyDown(window, KEY_COMBINATION.key().getValue())) {
            return false;
        }
        int mods = KEY_COMBINATION.modifiers();
        if ((mods & GLFW.GLFW_MOD_SHIFT) != 0
                && !KeyUtils.isAnyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return false;
        }
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0
                && !KeyUtils.isAnyDown(client, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            return false;
        }
        if ((mods & GLFW.GLFW_MOD_ALT) != 0
                && !KeyUtils.isAnyDown(client, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)) {
            return false;
        }
        if ((mods & GLFW.GLFW_MOD_SUPER) != 0
                && !KeyUtils.isAnyDown(client, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            return false;
        }
        return true;
    }

    /**
     * 供 Mixin 调用：启用本功能且按下的键命中组合键时，返回 true 表示应拦截原版按键
     *
     * @param glfwKey       GLFW 物理键码
     * @param glfwModifiers GLFW 修饰键位掩码（GLFW_MOD_*）
     */
    public static boolean shouldInterceptVanillaKey(int glfwKey, int glfwModifiers) {
        if (!FunctionConfig.ONE_CLICK_DISCARD_ITEMS_FUNCTION.isEnable()) {
            return false;
        }
        InputConstants.Key bound = KEY_COMBINATION.key();
        // 仅支持键盘键（KEYSYM）拦截
        if (bound.getType() != InputConstants.Type.KEYSYM || bound.getValue() != glfwKey) {
            return false;
        }
        int required = KEY_COMBINATION.modifiers();
        // 至少包含所需修饰键
        return (glfwModifiers & required) == required;
    }

    @Override
    protected void onDisable() {
        wasDown = false;
    }

    private void applyDefaults() {
        WHITE_LIST.withDefaultItems(
                Items.ROTTEN_FLESH,
                Items.SPIDER_EYE,
                Items.POISONOUS_POTATO,
                Items.CHICKEN,
                Items.STONE_SWORD,
                Items.GOLDEN_SWORD,
                Items.WOODEN_SWORD,
                Items.GOLDEN_AXE,
                Items.STONE_AXE,
                Items.WOODEN_AXE,
                Items.GOLDEN_HOE,
                Items.STONE_HOE,
                Items.WOODEN_HOE,
                Items.GOLDEN_SHOVEL,
                Items.STONE_SHOVEL,
                Items.WOODEN_SHOVEL,
                Items.GOLDEN_PICKAXE,
                Items.WOODEN_PICKAXE,
                Items.WOODEN_SPEAR,
                Items.GOLDEN_SPEAR,
                Items.LEATHER_HELMET,
                Items.LEATHER_CHESTPLATE,
                Items.LEATHER_LEGGINGS,
                Items.LEATHER_BOOTS,
                Items.GOLDEN_HELMET,
                Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS,
                Items.GOLDEN_BOOTS,
                Items.COPPER_HELMET,
                Items.COPPER_CHESTPLATE,
                Items.COPPER_LEGGINGS,
                Items.COPPER_BOOTS,
                Items.SADDLE,
                Items.BONE,
                Items.GOLD_NUGGET,
                Items.COPPER_NUGGET,
                Items.IRON_NUGGET,
                Items.WHEAT_SEEDS,
                Items.SHORT_GRASS,
                Items.FERN,
                Items.STRING,
                Items.BOWL,
                Items.PRISMARINE_CRYSTALS,
                Items.PRISMARINE_SHARD,
                Items.FEATHER,
                Items.CLAY_BALL,
                Items.FIRE_CHARGE,
                Items.RABBIT_FOOT,
                Items.PHANTOM_MEMBRANE,
                Items.GLOWSTONE_DUST,
                Items.DRY_SHORT_GRASS,
                Items.BUSH,
                Items.GLOW_LICHEN,
                Items.PINK_PETALS,
                Items.FIREFLY_BUSH,
                Items.TWISTING_VINES,
                Items.WEEPING_VINES,
                Items.VINE,
                Items.WILDFLOWERS,
                Items.LEAF_LITTER,
                Items.COCOA_BEANS,
                Items.MELON_SEEDS,
                Items.BEETROOT_SEEDS,
                Items.SEAGRASS,
                Items.KELP,
                Items.LILY_PAD,
                Items.CRIMSON_ROOTS,
                Items.WARPED_ROOTS,
                Items.NETHER_SPROUTS,
                Items.BROWN_MUSHROOM,
                Items.RED_MUSHROOM,
                Items.DANDELION,
                Items.POPPY,
                Items.AZURE_BLUET,
                Items.DRIPSTONE_BLOCK,
                Items.POINTED_DRIPSTONE
        );

        BLACK_LIST.withDefaultItems(
                Items.NETHERITE_SWORD,
                Items.NETHERITE_AXE,
                Items.NETHERITE_HOE,
                Items.NETHERITE_PICKAXE,
                Items.NETHERITE_SHOVEL,
                Items.NETHERITE_SPEAR
        );
    }
}
