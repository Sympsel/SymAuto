package com.symauto.mixin.accessor;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenTextInputAccessor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener> T symauto$addRenderableWidget(T widget);

    @Invoker("removeWidget")
    void symauto$removeWidget(GuiEventListener widget);

    @Invoker("setInitialFocus")
    void symauto$setInitialFocus(GuiEventListener widget);
}