package com.symhud.hud.element;

import com.symbase.hud.AbstractTextHudElement;
import com.symbase.hud.HudAnchor;
import net.minecraft.client.Minecraft;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地时间
 */
public final class ClockHudElement extends AbstractTextHudElement {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClockHudElement() {
        super("clock", "时钟", "显示系统本地时间",
                HudAnchor.BOTTOM_LEFT, 4, -45, 0xFFFFD166, 0x66000000);
    }

    @Override
    protected String text(Minecraft client, boolean preview) {
        return LocalTime.now().format(FORMAT);
    }
}


