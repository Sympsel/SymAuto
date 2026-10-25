package com.symauto.function.container.depend;

import com.symauto.function.base.SymContainer;
import com.symauto.function.utils.ItemUtils;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class Containers {
    private Containers() {}

    public static Optional<SymContainer> of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        if (ItemUtils.isShulkerBox(stack)) {
            return Optional.of(new ShulkerContainer(stack));
        }
        if (stack.getItem() instanceof BundleItem) {
            return Optional.of(new BundleContainer(stack));
        }
        return Optional.empty();
    }

    public static boolean isContainer(ItemStack stack) {
        return of(stack).isPresent();
    }
}
