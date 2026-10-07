package com.symauto.entity;

import lombok.Data;

import java.util.BitSet;

@Data
public class TagList {
    private BitSet tags;

    public void addTag(int tag) {
        if (tag < 0) {
            return;
        }
        if (tags == null) {
            tags = new BitSet();
        }
        tags.set(tag);
    }

    public boolean has(int tag) {
        return tag >= 0 && tags.get(tag);
    }

    public void removeTag(int tag) {
        tags.clear(tag);
    }

    public void clear() {
        tags.clear();
    }
}
