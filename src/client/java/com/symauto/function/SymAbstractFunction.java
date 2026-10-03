package com.symauto.function;

import net.minecraft.client.Minecraft;

import java.util.Random;

public abstract class SymAbstractFunction {
    private static final Random RANDOM = new Random();

    private final String name;
    private final String description;
    private boolean enable = false;

    private int intervalMs = 100;
    private float floatFactor = 0.1f;
    private long nextTriggerTime = 0L;

    protected SymAbstractFunction(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected SymAbstractFunction(String name) {
        this(name, "暂无");
    }

    protected abstract void onTrigger(Minecraft client);

    public void tick() {
        if (!enable) return;
        long now = System.currentTimeMillis();
        if (now < nextTriggerTime) return;

        onTrigger(Minecraft.getInstance());
        nextTriggerTime = now + nextDelay();
    }

    private int nextDelay() {
        float min = intervalMs * (1f - floatFactor);
        float max = intervalMs * (1f + floatFactor);
        int delay = (int) (min + RANDOM.nextFloat() * (max - min));
        return Math.max(10, delay);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
        if (enable) nextTriggerTime = 0L; // 开启时立刻可触发
    }

    public void toggle() {
        setEnable(!enable);
    }

    public int getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(int intervalMs) {
        this.intervalMs = Math.max(10, intervalMs);
    }

    public float getFloatFactor() {
        return floatFactor;
    }

    public void setFloatFactor(float floatFactor) {
        this.floatFactor = Math.max(0f, floatFactor);
    }
}
