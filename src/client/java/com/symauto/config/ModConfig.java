package com.symauto.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public Map<String, Boolean> features = new LinkedHashMap<>();
    public Map<String, BWListConfig> bwlists = new LinkedHashMap<>();

    public static class BWListConfig {
        public List<String> blacklist = new ArrayList<>();
        public List<String> whitelist = new ArrayList<>();
    }
}
