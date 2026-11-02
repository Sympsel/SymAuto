package com.symbase.registry;

import com.symbase.function.base.SymAbstractFunction;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能注册表
 */
public final class FunctionRegistry {
    private static final List<SymAbstractFunction> FUNCTIONS = new ArrayList<>();
    private static final List<Runnable> CHANGE_LISTENERS = new ArrayList<>();

    private FunctionRegistry() {
    }

    public static void register(SymAbstractFunction function) {
        for (SymAbstractFunction existing : FUNCTIONS) {
            if (existing.getId().equalsIgnoreCase(function.getId())) {
                throw new IllegalStateException("Duplicate function id: " + function.getId());
            }
        }
        FUNCTIONS.add(function);

        // 必须在 Minecraft.options 初始化前注册；功能注册阶段正好满足这个时机。
        // 各模块只注册功能，按键注册在这里统一完成。
        KeyMapping keyMapping = function.getKeyMapping();
        if (keyMapping != null) {
            KeyMappingHelper.registerKeyMapping(keyMapping);
        }
    }

    public static List<SymAbstractFunction> all() {
        return List.copyOf(FUNCTIONS);
    }

    public static void notifyChanged() {
        for (Runnable listener : List.copyOf(CHANGE_LISTENERS)) {
            listener.run();
        }
    }

    public static void addChangeListener(Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }
}

