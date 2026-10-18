package com.symauto.function.highlight;

import com.symauto.model.SymList;
import com.symauto.storage.VillagerTradeStore;
import com.symauto.config.option.BooleanOption;
import com.symauto.config.option.IntOption;
import com.symauto.function.base.HighLightFunction;
import com.symauto.render.glow.EntityGlowRegistry;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HighLightSpecificVillagersFunction extends HighLightFunction {
    public static final HighLightSpecificVillagersFunction INSTANCE = new HighLightSpecificVillagersFunction();
    private static final int SCAN_RADIUS = 30;
    private static final String GLOW_SOURCE = "high_light_specific_villagers";

    // 配置项：是否仅高亮拥有更高相关附魔村民
    @Getter
    @Setter
    private boolean onlyNeedBetterEnchantment;
    // 配置项：是否在非创造模式下排除带有冲突附魔的村民
    @Getter
    @Setter
    private boolean interceptingConflictingEnchantment = true;
    // 配置项：是否对任何可交易物品生效
    @Getter
    @Setter
    private boolean applyToAnyItemsCouldTrade = false;

    // 原版附魔互斥集
    private static final Set<TagKey<Enchantment>> EXCLUSIVE_SETS = Set.of(
            exclusiveSet("armor"), exclusiveSet("boots"), exclusiveSet("bow"),
            exclusiveSet("crossbow"), exclusiveSet("damage"), exclusiveSet("mining"),
            exclusiveSet("riptide"));
    // 黑名单
    @Getter
    private final SymList<ResourceKey<Enchantment>> BLACK_LIST;

    private HighLightSpecificVillagersFunction() {
        String id = "high_light_specific_villagers";
        super("high_light_specific_villagers",
                "高亮特定村民",
                SCAN_RADIUS);
        BLACK_LIST = new SymList<>(id, SymList.Type.BLACKLIST);
        // 绑定诅咒、消失诅咒
        BLACK_LIST.withDefaultItems(
                Enchantments.BINDING_CURSE,
                Enchantments.VANISHING_CURSE
        );
        addOption(new IntOption("scan_radius", "扫描半径",
                this::getScanRadius, this::setScanRadius, 1, 50, 1));
        addOption(new BooleanOption("only_need_better_enchantment", "仅高亮拥有更高相关附魔村民",
                this::isOnlyNeedBetterEnchantment, this::setOnlyNeedBetterEnchantment, true));
        addOption(new BooleanOption("intercepting_conflicting_enchantment", "非创造模式下排除带有冲突附魔的村民",
                this::isInterceptingConflictingEnchantment, this::setInterceptingConflictingEnchantment, true));
        addOption(new BooleanOption("apply_to_any_items", "是否对任何可交易物品生效（仅单人模式生效）",
                this::isApplyToAnyItemsCouldTrade, this::setApplyToAnyItemsCouldTrade, false));

    }

    @Override
    protected String tooltip() {
        int diameter = getScanRadius() * 2;
        return Tooltip.create()
                .line("主手持可附魔物品时，高亮范围内（" + diameter + "格）卖相关附魔书的村民")
                .line(ChatFormatting.YELLOW, "多人模式下仅高亮\"曾交易过\"、\"且出售与主手物品相关附魔书\"的村民")
                .keyValueLine(ChatFormatting.YELLOW, "仅高亮拥有更高等级相关附魔村民", ChatFormatting.GRAY, onlyNeedBetterEnchantment ? "是" : "否")
                .keyValueLine(ChatFormatting.YELLOW, "非创造模式下排除带有冲突附魔的村民", ChatFormatting.GRAY, interceptingConflictingEnchantment ? "是" : "否")
                .keyValueLine(ChatFormatting.YELLOW, "单人模式增强，是否对任何可交易物品生效", ChatFormatting.GRAY, applyToAnyItemsCouldTrade ? "是" : "否")
                .display(BLACK_LIST, enhancement -> enhancement.identifier().toString())
                .toString();
    }

    @Override
    protected @Nullable Set<UUID> collectGlowIds(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return Set.of();
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            return Set.of();
        }
        HolderLookup.RegistryLookup<Enchantment> enchantments =
                client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        boolean isCreative = client.player.isCreative();
        // 单人模式增强
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null) {
            ServerLevel serverLevel = server.getLevel(client.player.level().dimension());
            if (serverLevel != null) {
                if (applyToAnyItemsCouldTrade) {
                    // 任意可交易物品模式：村民买卖主手物品即高亮
                    Item heldItem = stack.getItem();
                    return scan(client, Villager.class, villager ->
                            tradesHeldItem(serverLevel, villager.getUUID(), heldItem));
                }
                return scan(client, Villager.class, villager -> isRelevantTrade(
                        enchantments,
                        readServerOffers(serverLevel, villager.getUUID()),
                        stack,
                        isCreative));
            }
        }
        return scan(client, Villager.class, villager -> hasCachedRelevantTrade(enchantments, villager.getUUID(), stack, isCreative));
    }

    @Override
    protected TeamColor glowColor() {
        return TeamColor.AQUA;
    }

    /**
     * 单人模式增强：开启任意可交易物品模式时，高亮买卖主手物品的村民
     */
    private static boolean tradesHeldItem(ServerLevel level, UUID villagerId, Item item) {
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getResult().is(item)
                    || offer.getCostA().is(item)
                    || offer.getCostB().is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 单人模式增强：从服务器端读取村民交易数据
     */
    private static Map<String, Integer> readServerOffers(ServerLevel level, UUID villagerId) {
        Entity entity = level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) {
            return Map.of();
        }
        MerchantOffers offers = villager.getOffers();
        Map<String, Integer> trades = new HashMap<>();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            // 附魔书的数据在 STORED_ENCHANTMENTS 组件，不在 getEnchantments()
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null) {
                continue;
            }
            for (Map.Entry<Holder<Enchantment>, Integer> e : stored.entrySet()) {
                e.getKey().unwrapKey().map(ResourceKey::identifier).ifPresent(
                        id -> trades.merge(id.toString(), e.getValue(), Math::max)
                );
            }
        }
        return trades;
    }

    /**
     * 多人模式下从缓存读
     */
    private boolean hasCachedRelevantTrade(HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry,
                                           UUID villagerId, ItemStack heldStack, boolean isCreative) {
        return isRelevantTrade(enchantmentRegistry, VillagerTradeStore.getEnchantments(villagerId), heldStack, isCreative);
    }

    private boolean isRelevantTrade(HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry,
                                    Map<String, Integer> trades, ItemStack heldStack, boolean isCreative) {
        if (trades == null || trades.isEmpty()) {
            return false;
        }
        Item mainHandItem = heldStack.getItem();
        var heldEnchants = heldStack.getEnchantments();
        for (Map.Entry<String, Integer> entry : trades.entrySet()) {
            Identifier identifier = Identifier.tryParse(entry.getKey());
            if (identifier == null) {
                continue;
            }
            int tradeLevel = entry.getValue();
            ResourceKey<Enchantment> enchantmentKey =
                    ResourceKey.create(Registries.ENCHANTMENT, identifier);
            // 跳过黑名单中的附魔
            if (BLACK_LIST.contains(enchantmentKey)) {
                continue;
            }
            Holder<Enchantment> holder = enchantmentRegistry.get(enchantmentKey).orElse(null);
            if (holder == null
                    || !ItemUtils.isEnchantmentCanBeAppliedToItem(holder.value(), mainHandItem)) {
                continue;
            }
            if (interceptingConflictingEnchantment && !isCreative && conflictsWithHeldOther(holder, enchantmentKey, heldEnchants.keySet())) {
                continue;
            }

            if (!onlyNeedBetterEnchantment) {
                return true;
            }
            int currentLevel = heldEnchants.getLevel(holder);
            // 仅当村民出售的附魔等级高于主手物品当前等级时才高亮
            if (tradeLevel > currentLevel) {
                return true;
            }
        }
        return false;
    }

    private static boolean conflictsWithHeldOther(Holder<Enchantment> target, ResourceKey<Enchantment> targetKey, Set<Holder<Enchantment>> held) {
        for (TagKey<Enchantment> exclusive : EXCLUSIVE_SETS) {
            if (!target.is(exclusive)) {
                continue;
            }
            for (Holder<Enchantment> h : held) {
                if (h.is(exclusive) && !h.is(targetKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static TagKey<Enchantment> exclusiveSet(String path) {
        return TagKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/" + path));
    }


    @Override
    protected void onDisable() {
        EntityGlowRegistry.clear(GLOW_SOURCE);
    }
}