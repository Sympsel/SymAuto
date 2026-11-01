package com.symauto.hud;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * HUD 元素基类，集中保存 ID、显示名与默认布局。
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractHudElement implements HudElement {
    private final String id;
    private final String displayName;
    private final String description;
    private final HudAnchor defaultAnchor;
    private final int defaultOffsetX;
    private final int defaultOffsetY;
    private boolean transparentBackground;

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
    public boolean transparentBackground() {
        return transparentBackground;
    }

    @Override
    public HudPlacement defaultPlacement() {
        return new HudPlacement(defaultAnchor, defaultOffsetX, defaultOffsetY, 1.0F, true);
    }
}
