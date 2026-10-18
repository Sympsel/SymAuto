package com.symauto.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SymList<T> {
    @Getter
    private final String functionId;
    @Getter
    private final Set<T> list;
    @Getter
    private final String type;

    public static class Type {
        public static final String BLACKLIST = "blacklist";
        public static final String WHITELIST = "whitelist";
    }

    public SymList(String functionId, String type) {
        this.functionId = functionId;
        this.type = type;
        list = new HashSet<>();
    }

    /**
     * 添加默认项
     *
     * @param items 条目
     */
    public SymList<T> withDefaultItems(T... items) {
        Collections.addAll(list, items);
        return this;
    }

    public void add(T item) {
        list.add(item);
    }

    public void remove(T item) {
        list.remove(item);
    }

    public boolean contains(T item) {
        return list.contains(item);
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
