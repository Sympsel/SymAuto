package com.symauto.function.functions;

import com.symauto.config.VillagerTradeStore;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.EntityGlowRegistry;
import com.symauto.function.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HighLightSpecificVillagersFunction extends SymAbstractFunction {
    public static final HighLightSpecificVillagersFunction INSTANCE = new HighLightSpecificVillagersFunction();
    private static final int SCAN_RADIUS = 30;
    private static String TOOLTIP = "高亮曾交易过、且出售与主手物品相关附魔书的村民";

    // 当前已点亮发光的村民
    private final Set<Villager> glowingVillagers = ConcurrentHashMap.newKeySet();

    private HighLightSpecificVillagersFunction() {
        super("high_light_specific_villagers", "高亮特定村民", TOOLTIP);
    }

    @Override
    public String getTooltip() {
        TOOLTIP = "主手持可附魔物品时，高亮扫描范围（边长" + SCAN_RADIUS * 2
                + "格）内、你曾打开过交易且出售相关附魔书的村民";
        return TOOLTIP;
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            EntityGlowRegistry.clear();
            return;
        }
        ItemStack stack = client.player.getMainHandItem();
        Item mainHandItem = stack.isEmpty() ? null : stack.getItem();

        Set<UUID> glowIds = new HashSet<>();
        if (mainHandItem != null) {
            // 从世界的 RegistryAccess 获取附魔的动态注册表
            HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry =
                    client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (Villager villager : client.level.getEntitiesOfClass(
                    Villager.class, client.player.getBoundingBox().inflate(SCAN_RADIUS))) {
                if (hasCachedRelevantTrade(enchantmentRegistry, villager.getUUID(), mainHandItem)) {
                    glowIds.add(villager.getUUID());
                }
            }
        }
        EntityGlowRegistry.replace(glowIds);
    }

    private boolean hasCachedRelevantTrade(HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry,
                                           UUID villagerId, Item mainHandItem) {
        Set<String> enchantIds = VillagerTradeStore.getEnchantments(villagerId);
        if (enchantIds == null || enchantIds.isEmpty()) {
            return false;
        }
        for (String id : enchantIds) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                continue;
            }
            ResourceKey<Enchantment> enchantmentKey =
                    ResourceKey.create(Registries.ENCHANTMENT, identifier);
            Enchantment enchantment = enchantmentRegistry.get(enchantmentKey)
                    .map(Holder.Reference::value)
                    .orElse(null);
            if (enchantment != null && ItemUtils.isEnchantmentCanBeAppliedToItem(enchantment, mainHandItem)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDisable() {
        EntityGlowRegistry.clear();
    }
}