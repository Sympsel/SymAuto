package com.symauto.function;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.Random;

public abstract class SymAbstractFunction {
    protected static final Random RANDOM = new Random();

    @Getter
    private final String name;
    @Getter
    private final String tooltip;
    @Setter
    @Getter
    private boolean enable = false;

    public SymAbstractFunction(String name, String tooltip) {
        this.name = name;
        this.tooltip = tooltip;
    }

    protected abstract void onTrigger(Minecraft client);

    public void tick() {
        if (!enable) return;
        onTrigger(Minecraft.getInstance());
    }

    public void toggle() {
        setEnable(!enable);
    }

    public void compatibility() {
    }
}
