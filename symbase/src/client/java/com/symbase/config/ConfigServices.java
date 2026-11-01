package com.symbase.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置保存入口
 */
public final class ConfigServices {
    public interface Service {
        void save();
    }

    private static final List<Service> SERVICES = new ArrayList<>();

    private ConfigServices() {
    }

    public static void register(Service service) {
        SERVICES.add(service);
    }

    public static void saveAll() {
        for (Service service : List.copyOf(SERVICES)) {
            service.save();
        }
    }
}

