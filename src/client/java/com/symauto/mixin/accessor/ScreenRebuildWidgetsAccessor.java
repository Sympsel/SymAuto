package com.symauto.mixin.accessor;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenRebuildWidgetsAccessor {
    @Invoker("rebuildWidgets")
    void symauto$rebuildWidgets();
}
