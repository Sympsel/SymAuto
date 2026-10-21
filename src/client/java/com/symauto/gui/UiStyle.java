package com.symauto.gui;

import lombok.NoArgsConstructor;

public class UiStyle {
    public static final UiStyle DEFAULT = builder().build();

    public final int panelBg;
    public final int panelOutline; // 外边框
    public final int title; // 标题
    public final int text; // 正文
    public final int muted; // 空提示 / hint 等次级文字
    public final int scrollbarTrack;
    public final int scrollbarThumb;
    public final int scrollbarThumbActive; // 拖拽中
    public final int scrollbarWidth;

    private UiStyle(Builder b) {
        this.panelBg = b.panelBg;
        this.panelOutline = b.panelOutline;
        this.title = b.title;
        this.text = b.text;
        this.muted = b.muted;
        this.scrollbarTrack = b.scrollbarTrack;
        this.scrollbarThumb = b.scrollbarThumb;
        this.scrollbarThumbActive = b.scrollbarThumbActive;
        this.scrollbarWidth = b.scrollbarWidth;
    }

    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }

    @NoArgsConstructor
    public static final class Builder {
        int panelBg = 0x60000000;
        int panelOutline = 0;
        int title = 0xFFFFFFFF;
        int text = 0xFFFFFFFF;
        int muted = 0xFFAAAAAA;
        int scrollbarTrack = 0x40FFFFFF;
        int scrollbarThumb = 0xC0FFFFFF;
        int scrollbarThumbActive = 0xFFFFFFFF;
        int scrollbarWidth = 6;

        private Builder(UiStyle s) {
            panelBg = s.panelBg;
            panelOutline = s.panelOutline;
            title = s.title;
            text = s.text;
            muted = s.muted;
            scrollbarTrack = s.scrollbarTrack;
            scrollbarThumb = s.scrollbarThumb;
            scrollbarThumbActive = s.scrollbarThumbActive;
            scrollbarWidth = s.scrollbarWidth;
        }

        public Builder panelBg(int c) { panelBg = c; return this; }
        public Builder panelOutline(int c) { panelOutline = c; return this; }
        public Builder title(int c) { title = c; return this; }
        public Builder text(int c) { text = c; return this; }
        public Builder muted(int c) { muted = c; return this; }
        public Builder scrollbar(int track, int thumb, int thumbActive) {
            scrollbarTrack = track; scrollbarThumb = thumb; scrollbarThumbActive = thumbActive; return this;
        }
        public Builder scrollbarWidth(int w) { scrollbarWidth = w; return this; }
        public UiStyle build() { return new UiStyle(this); }
    }
}
