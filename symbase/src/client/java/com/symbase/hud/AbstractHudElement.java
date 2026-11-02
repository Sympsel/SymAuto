package com.symbase.hud;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * HUD 元素基类
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractHudElement implements HudElement {
    private final String id;
    private final String displayName;
    private final String description;
    private final HudAnchor defaultAnchor;
    private final int defaultOffsetX;
    private final int defaultOffsetY;

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public HudPlacement defaultPlacement() {
        return new HudPlacement(defaultAnchor, defaultOffsetX, defaultOffsetY, 1.0F, true);
    }
}



