package com.symauto.core;

import com.symauto.function.auto.AutoAttackFunction;
import com.symauto.function.auto.AutoHarvestCropFunction;
import com.symauto.function.place.ScanDestroyFunction;
import com.symbase.config.ConfigServices;
import com.symbase.function.base.SymAbstractFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Set;

/**
 * 玩家实例切换时的处理，包括但不限于死亡、重生、切换维度、切换世界等
 */
public final class PlayChangedHandler {
    private static final Set<Class<? extends SymAbstractFunction>> DISABLE_ON_PLAYER_CHANGE = Set.of(
            ScanDestroyFunction.class,
            AutoAttackFunction.class,
            AutoHarvestCropFunction.class
    );

    private static LocalPlayer lastPlayer;

    private PlayChangedHandler() {
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            lastPlayer = null;
            return;
        }
        if (player == lastPlayer) {
            return;
        }
        lastPlayer = player;

        for (SymAbstractFunction function : AutoFunctions.AUTO_ALL) {
            function.onPlayerChanged(client);
        }
        disableDangerous();
    }

    private static void disableDangerous() {
        boolean changed = false;
        for (SymAbstractFunction function : AutoFunctions.AUTO_ALL) {
            if (function.isEnable() && DISABLE_ON_PLAYER_CHANGE.contains(function.getClass())) {
                function.setEnable(false);
                changed = true;
            }
        }
        if (changed) {
            ConfigServices.saveAll();
        }
    }
}
