package com.symauto.config.option;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 布尔配置项
 */
public class BooleanOption extends ConfigOption {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public BooleanOption(String key, String label,
                         Supplier<Boolean> getter, Consumer<Boolean> setter,
                         boolean defaultValue) {
        super(key, label, String.valueOf(defaultValue));
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String displayValue() {
        return getter.get() ? "是" : "否";
    }

    @Override
    public void cycle(int direction) {
        setter.accept(!getter.get());
    }

    @Override
    public String serialize() {
        return String.valueOf(getter.get());
    }

    @Override
    public void deserialize(String value) {
        if (value != null) {
            setter.accept(Boolean.parseBoolean(value));
        }
    }
}
