package com.symauto.config;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class SymAutoKeys {
    private SymAutoKeys() {}

    public static final KeyMapping.Category CATEGORY_MAIN = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("symauto", "main"));
}
