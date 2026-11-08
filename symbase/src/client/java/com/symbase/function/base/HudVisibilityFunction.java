package com.symbase.function.base;

import com.symbase.config.SymBaseKeys;
import com.symbase.function.util.KeyUtils;
import com.symbase.function.util.Tooltip;
import com.symbase.hud.HudManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * 全局 HUD 显示开关
 */
public final class HudVisibilityFunction extends SymAbstractFunction {
    private final KeyMapping key = KeyUtils.createUnbound("key.symbase.hud_visibility", SymBaseKeys.CATEGORY_MAIN);

    public static final HudVisibilityFunction INSTANCE = new HudVisibilityFunction();

    private HudVisibilityFunction() {
        super("hud_visibility", "HUD 显示");
    }

    @Override
    public boolean isEnable() {
        return HudManager.isVisible();
    }

    @Override
    public void setEnable(boolean enable) {
        HudManager.setVisible(enable);
    }

    @Override
    public void toggle() {
        setEnable(!isEnable());
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("显示或隐藏屏幕上的所有 Sym系列的 HUD 元素")
                .line(ChatFormatting.YELLOW, "支持快捷键绑定，默认无绑定")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    @Override
    public void onKeyAction(Minecraft client) {
        toggle();
    }

    @Override
    public @Nullable KeyMapping getKeyMapping() {
        return key;
    }
}
