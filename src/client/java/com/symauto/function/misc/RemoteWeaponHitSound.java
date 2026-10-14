package com.symauto.function.misc;

import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import net.minecraft.client.Minecraft;

@Deprecated
public class RemoteWeaponHitSound extends SymAbstractFunction {
    public static final RemoteWeaponHitSound INSTANCE = new RemoteWeaponHitSound();

    private RemoteWeaponHitSound() {
        super("remote_weapon_hit_sound", "远程武器命中提示音",
                Tooltip.create()
                        .line("远程武器命中会有“叮”的提示")
                        .toString()
        );
    }

    @Override
    protected void onTrigger(Minecraft client) {

    }
}
