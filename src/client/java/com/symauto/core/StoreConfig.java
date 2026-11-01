package com.symauto.core;

import com.symauto.hud.HudManager;
import com.symauto.storage.*;
import com.symauto.storage.base.PersistentStore;

import java.util.List;

public class StoreConfig {
    public static final PersistentStore VILLAGER_TRADE_STORE = VillagerTradeStore.INSTANCE;
    public static final PersistentStore DEATH_INFO_STORE = DeathInfoStore.INSTANCE;
    public static final PersistentStore CRAFTING_PRESET_STORE = CraftingPresetStore.INSTANCE;
    public static final PersistentStore KEEP_INV_PRESET_STORE = KeepInvPresetStore.INSTANCE;
    public static final PersistentStore HUD_LAYOUT_STORE = HudManager.store();

    public static final List<PersistentStore> ALL = List.of(
            VILLAGER_TRADE_STORE,
            DEATH_INFO_STORE,
            CRAFTING_PRESET_STORE,
            KEEP_INV_PRESET_STORE,
            HUD_LAYOUT_STORE
    );


    private StoreConfig() {
    }
}
