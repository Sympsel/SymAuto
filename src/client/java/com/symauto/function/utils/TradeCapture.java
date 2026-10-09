package com.symauto.function.utils;

import com.symauto.config.VillagerTradeStore;
import com.symauto.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TradeCapture {
    private TradeCapture() {
    }

    private static Screen lockedScreen;
    private static UUID lockedVillagerId;

    public static void tick(Minecraft client) {
        Screen screen = client.gui.screen();
        if (!(screen instanceof MerchantScreen merchantScreen)) {
            lockedScreen = null;
            lockedVillagerId = null;
            return;
        }

        if (screen != lockedScreen) {
            lockedScreen = screen;
            lockedVillagerId = resolveVillagerId(client);
        }
        if (lockedVillagerId == null) {
            return;
        }
        MerchantMenu merchantMenu = merchantScreen.getMenu();

        List<MerchantOffer> offers = merchantMenu.getOffers();
        if (offers.isEmpty()) {
            return;
        }

        Set<String> enchantIds = new LinkedHashSet<>();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                continue;
            }
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                holder.unwrapKey().ifPresent(key -> enchantIds.add(key.identifier().toString()));
            }
        }

        if (!enchantIds.isEmpty()) {
            VillagerTradeStore.record(lockedVillagerId, enchantIds);
        }
    }

    private static UUID resolveVillagerId(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof AbstractVillager villager) {
                return villager.getUUID();
            }
        }
        AbstractVillager nearest = null;
        double best = Double.MAX_VALUE;
        for (AbstractVillager villager : client.level.getEntitiesOfClass(
                AbstractVillager.class, client.player.getBoundingBox().inflate(8))) {
            double d = villager.distanceToSqr(client.player);
            if (d < best) {
                best = d;
                nearest = villager;
            }
        }
        return nearest != null ? nearest.getUUID() : null;
    }
}
