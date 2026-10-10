package com.symauto.config.option;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class ConfigOption {
    protected final String key;
    @Getter
    protected final String label;
    protected final String defaultValue;

    public abstract String displayValue();

    public abstract void cycle(int direction);

    public abstract String serialize();

    public abstract void deserialize(String value);

    public void reset() {
        deserialize(defaultValue);
    }
}
