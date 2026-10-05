package com.symauto.entity;

import com.mojang.blaze3d.platform.InputConstants;

public record KeyCombination(String keyName, int modifiers) {
    public static final KeyCombination NONE = new KeyCombination("key.keyboard.unknown", 0);

    public InputConstants.Key key() {
        return InputConstants.getKey(keyName);
    }

    public boolean matches(InputConstants.Key pressedKey, int pressedModifiers) {
        return key().equals(pressedKey) && (pressedModifiers & modifiers) == modifiers;
    }
}
