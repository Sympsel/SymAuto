package com.symbase.config;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class SymBaseKeys {
    private SymBaseKeys() {
    }

    public static final KeyMapping.Category CATEGORY_MAIN = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("symbase", "main"));
}
