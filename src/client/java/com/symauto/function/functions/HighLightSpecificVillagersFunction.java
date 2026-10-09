package com.symauto.function.functions;

import com.symauto.config.VillagerTradeStore;
import com.symauto.entity.BWList;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.EntityGlowRegistry;
import com.symauto.function.utils.ItemUtils;
import com.symauto.function.utils.Tooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HighLightSpecificVillagersFunction extends SymAbstractFunction {
    public static final HighLightSpecificVillagersFunction INSTANCE = new HighLightSpecificVillagersFunction();
    private static final int SCAN_RADIUS = 30;
    private static String TOOLTIP = "高亮曾交易过、且出售与主手物品相关附魔书的村民\n";

    // 配置项：是否仅高亮拥有更高相关附魔村民
    private static final boolean ONLY_NEED_BETTER_ENCHANTMENT = true;
    private static final boolean INTERCEPTING_CONFLICT_ENCHANTMENT = true;

    // 原版附魔互斥集
    private static final Set<TagKey<Enchantment>> EXCLUSIVE_SETS = Set.of(
            exclusiveSet("armor"), exclusiveSet("boots"), exclusiveSet("bow"),
            exclusiveSet("crossbow"), exclusiveSet("damage"), exclusiveSet("mining"),
            exclusiveSet("riptide"));
    // 黑名单
    private final BWList<ResourceKey<Enchantment>> ENCHANTMENT_BLACKLIST;

    private HighLightSpecificVillagersFunction() {
        String id = "high_light_specific_villagers";
        super(id, "高亮特定村民", true, TOOLTIP);
        ENCHANTMENT_BLACKLIST = new BWList<>(id);
        // 绑定诅咒、消失诅咒
        ENCHANTMENT_BLACKLIST.addAllToBlacklist(Set.of(
                Enchantments.BINDING_CURSE,
                Enchantments.VANISHING_CURSE
        ));
    }

    @Override
    protected String describe() {
        return Tooltip.create()
                .line("主手持可附魔物品时，高亮扫描范围（边长" + SCAN_RADIUS * 2 + "格）内、你曾打开过交易且出售相关附魔书的村民")
                .line(ChatFormatting.DARK_BLUE, "仅高亮拥有更高相关附魔村民：" + (ONLY_NEED_BETTER_ENCHANTMENT ? "是" : "否"))
                .line(ChatFormatting.DARK_BLUE, "非创造模式下排除带有冲突附魔的村民：" + (INTERCEPTING_CONFLICT_ENCHANTMENT ? "是" : "否"))
                .line(ENCHANTMENT_BLACKLIST.displayBlacklist(enhancement -> enhancement.identifier().toString()))
                .toString();
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
                if (hasCachedRelevantTrade(enchantmentRegistry, villager.getUUID(), stack, client.player.isCreative())) {
                    glowIds.add(villager.getUUID());
                }
            }
        }
        EntityGlowRegistry.replace(glowIds);
    }

    private boolean hasCachedRelevantTrade(HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry,
                                           UUID villagerId, ItemStack heldStack, boolean isCreative) {
        Map<String, Integer> trades = VillagerTradeStore.getEnchantments(villagerId);
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
            if (ENCHANTMENT_BLACKLIST.isBlacklisted(enchantmentKey)) {
                continue;
            }
            Holder<Enchantment> holder = enchantmentRegistry.get(enchantmentKey).orElse(null);
            if (holder == null
                    || !ItemUtils.isEnchantmentCanBeAppliedToItem(holder.value(), mainHandItem)) {
                continue;
            }
            if (INTERCEPTING_CONFLICT_ENCHANTMENT && !isCreative && conflictsWithHeldOther(holder, enchantmentKey, heldEnchants.keySet())) {
                continue;
            }

            if (!ONLY_NEED_BETTER_ENCHANTMENT) {
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
        EntityGlowRegistry.clear();
    }
}