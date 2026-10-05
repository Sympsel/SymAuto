package com.symauto.function;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

public abstract class SymAbstractFunction {
    protected static final Random RANDOM = new Random();

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String tooltip;
    @Getter
    private boolean enable = false;

    public SymAbstractFunction(String id, String name, String tooltip) {
        this.id = id;
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

    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Collections.emptySet();
    }

    protected void onDisable() {}

    public void setEnable(boolean enable) {
        if (this.enable && !enable) {
            onDisable();
        }
        this.enable = enable;
    }
}
