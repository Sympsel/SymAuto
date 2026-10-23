package com.symauto.model.common;

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

    public void set(int tag, boolean value) {
        if (tag < 0) {
            return;
        }
        if (tags == null) {
            tags = new BitSet();
        }
        if (value) {
            tags.set(tag);
        } else {
            tags.clear(tag);
        }
    }

    public void toggle(int tag) {
        if (tag < 0) {
            return;
        }
        if (tags == null) {
            tags = new BitSet();
        }
        if (tags.get(tag)) {
            tags.clear(tag);
        } else {
            tags.set(tag);
        }
    }

    public int[] toIntArray() {
        if (tags == null || tags.isEmpty()) {
            return new int[0];
        }
        int[] out = new int[tags.cardinality()];
        int i = 0;
        for (int t = tags.nextSetBit(0); t >= 0; t = tags.nextSetBit(t + 1)) {
            out[i++] = t;
        }
        return out;
    }

    public void replaceAll(int[] tagArray) {
        tags = new BitSet();
        if (tagArray != null) {
            for (int t : tagArray) {
                if (t >= 0) {
                    tags.set(t);
                }
            }
        }
    }

    public void clear() {
        tags.clear();
    }
}
