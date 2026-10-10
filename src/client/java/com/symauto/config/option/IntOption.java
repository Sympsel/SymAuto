package com.symauto.config.option;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class IntOption extends ConfigOption {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min;
    private final int max;
    private final int step;

    public IntOption(String key, String label,
                     IntSupplier getter, IntConsumer setter,
                     int min, int max, int step) {
        super(key, label, String.valueOf(getter.getAsInt()));
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
    }
    @Override
    public String displayValue() {
        return String.valueOf(getter.getAsInt());
    }

    @Override
    public void cycle(int direction) {
        int v = getter.getAsInt() + direction * step;
        setter.accept(Math.clamp(v, min, max));
    }

    @Override
    public String serialize() {
        return String.valueOf(getter.getAsInt());
    }

    @Override
    public void deserialize(String value) {
        try {
            setter.accept(Math.clamp(Integer.parseInt(value), min, max));
        } catch (NumberFormatException ignored) {
        }
    }
}
