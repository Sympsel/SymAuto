package com.symauto.entity;

import java.util.HashSet;
import java.util.Set;

public class BWList<T> {
    private static String featureId = "undefined";
    private final Set<T> blacklist;
    private final Set<T> whitelist;

    public BWList(String featureId) {
        BWList.featureId = featureId;
        blacklist = new HashSet<>();
        whitelist = new HashSet<>();
    }

    public void addToBlacklist(T item) {
        blacklist.add(item);
        whitelist.remove(item);
    }

    public void addToWhitelist(T item) {
        whitelist.add(item);
        blacklist.remove(item);
    }

    public void addAllToBlacklist(Set<T> items) {
        blacklist.addAll(items);
        whitelist.removeAll(items);
    }
    public void addAllToWhitelist(Set<T> items) {
        whitelist.addAll(items);
        blacklist.removeAll(items);
    }

    public void removeFromBlacklist(T item) {
        blacklist.remove(item);
    }

    public void removeFromWhitelist(T item) {
        whitelist.remove(item);
    }

    public boolean isBlacklisted(T item) {
        return blacklist.contains(item);
    }

    public boolean isWhitelisted(T item) {
        return whitelist.contains(item);
    }

    public Set<T> getBlacklist() {
        return blacklist;
    }

    public Set<T> getWhitelist() {
        return whitelist;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void clear() {
        blacklist.clear();
        whitelist.clear();
    }
}
