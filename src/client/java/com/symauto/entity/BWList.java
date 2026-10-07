package com.symauto.entity;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class BWList<T> {
    @Getter
    private final String featureId;
    @Getter
    private final Set<T> blacklist;
    @Getter
    private final Set<T> whitelist;
    private Runnable defaultsApplier;

    public BWList(String featureId) {
        this.featureId = featureId;
        blacklist = new HashSet<>();
        whitelist = new HashSet<>();
    }

    // 注册默认值回调
    public BWList<T> withDefaultsApplier(Runnable defaultsApplier) {
        this.defaultsApplier = defaultsApplier;
        return this;
    }

    // 触发已注册的默认值回调
    public void applyDefaults() {
        if (defaultsApplier != null) {
            defaultsApplier.run();
        }
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

    /**
     * 黑名单展示接口
     *
     * @param linePrefix 每行前缀（如换行+缩进+颜色码，例："\n\t§7"）
     * @param toLabel 单个条目的展示文本转换
     * @param emptyText 黑名单为空时返回的文本（不含 linePrefix）
     */
    public String displayBlacklist(String linePrefix, Function<T, String> toLabel, String emptyText) {
        if (blacklist.isEmpty()) {
            return emptyText;
        }
        StringBuilder sb = new StringBuilder();
        for (T value : blacklist) {
            sb.append(linePrefix).append(toLabel.apply(value));
        }
        return sb.toString();
    }

    /**
     * 白名单展示接口
     *
     * @param linePrefix 每行前缀（如换行+缩进+颜色码，例："\n\t§7"）
     * @param toLabel 单个条目的展示文本转换
     * @param emptyText 黑名单为空时返回的文本（不含 linePrefix）
     */
    public String displayWhitelist(String linePrefix, Function<T, String> toLabel, String emptyText) {
        if (whitelist.isEmpty()) {
            return emptyText;
        }
        StringBuilder sb = new StringBuilder();
        for (T value : whitelist) {
            sb.append(linePrefix).append(toLabel.apply(value));
        }
        return sb.toString();
    }

    public int getBlacklistSize() {
        return blacklist.size();
    }

    public int getWhitelistSize() {
        return whitelist.size();
    }

    public void clear() {
        blacklist.clear();
        whitelist.clear();
    }
}
