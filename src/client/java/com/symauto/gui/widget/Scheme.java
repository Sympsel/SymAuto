package com.symauto.gui.widget;

public class Scheme {
    public enum Color {
        NEUTRAL("原始灰", 0xBB555555, 0xBB777777, 0xBB333333, 0xFFF2F2F2),
        GRAPHITE("石墨灰", 0xBB3F3F46, 0xBB5F5F6A, 0xBB26262B, 0xFFF2F2F2),
        COOL_GRAY("冷灰蓝", 0xBB4A5568, 0xBB6B7891, 0xBB2D3540, 0xFFF2F2F2),
        WARM_GRAY("暖灰", 0xBB5C5550, 0xBB7C7570, 0xBB3A3532, 0xFFF2F2F2),

        DEEP_SEA("深海蓝", 0xBB2F4A6D, 0xBB48688F, 0xBB1E3049, 0xFFF2F2F2),
        INDIGO("靛青", 0xBB3A3F7A, 0xBB585FA3, 0xBB232750, 0xFFF2F2F2),
        TEAL("青碧", 0xBB2A5F5A, 0xBB3F807A, 0xBB173B37, 0xFFF2F2F2),
        SKY("天青", 0xBB2E6E8E, 0xBB4A90B0, 0xBB1A475E, 0xFFF2F2F2),

        FOREST("森林绿", 0xBB3C5F3C, 0xBB567F56, 0xBB253B25, 0xFFF2F2F2),
        MOSS("墨绿", 0xBB356B55, 0xBB4E8C72, 0xBB204334, 0xFFF2F2F2),

        AMBER("琥珀", 0xBB7A5A20, 0xBB9C7836, 0xBB523B10, 0xFFF2F2F2),
        TERRACOTTA("赤陶", 0xBB8A4B33, 0xBBAA6A50, 0xBB5C2E1D, 0xFFF2F2F2),
        BRICK("砖红", 0xBB7A3A34, 0xBB9C5650, 0xBB50231F, 0xFFF2F2F2),
        WINE("酒红", 0xBB6B2F45, 0xBB8C4A60, 0xBB451D2D, 0xFFF2F2F2),
        COFFEE("咖啡", 0xBB5F4A38, 0xBB7F6853, 0xBB3D2F23, 0xFFF2F2F2),

        VIOLET("紫罗兰", 0xBB5A3560, 0xBB7A5283, 0xBB3A213E, 0xFFF2F2F2),
        ROSE("玫瑰", 0xBB7A4453, 0xBB9C6272, 0xBB502C37, 0xFFF2F2F2),

        PRIMARY("品牌蓝", 0xBB2F6FD0, 0xBB4C8CE6, 0xBB1D4A8F, 0xFFFFFFFF),
        SUCCESS("成功绿", 0xBB2E7D4F, 0xBB469668, 0xBB1B5233, 0xFFFFFFFF),
        WARNING("警示橙", 0xBB9A6A1F, 0xBBB98A3A, 0xBB684612, 0xFFFFFFFF),
        DANGER("危险红", 0xBBA63B3B, 0xBBC45C5C, 0xBB702626, 0xFFFFFFFF),

        MIST("雾白", 0xBBE4E6EA, 0xBBCFD3D9, 0xBBF0F1F3, 0xFF2B2B2B),
        IVORY("米白", 0xBBEDE9E2, 0xBBD9D3C8, 0xBBF4F1EC, 0xFF2B2B2B);

        public final String label;
        public final int bg, bgHover, bgDisabled, fg;

        Color(String label, int bg, int bgHover, int bgDisabled, int fg) {
            this.label = label;
            this.bg = bg;
            this.bgHover = bgHover;
            this.bgDisabled = bgDisabled;
            this.fg = fg;
        }
    }

    public static int mix(int argb, int targetRgb, float t) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int tr = (targetRgb >>> 16) & 0xFF;
        int tg = (targetRgb >>> 8) & 0xFF;
        int tb = targetRgb & 0xFF;
        r = Math.round(r + (tr - r) * t);
        g = Math.round(g + (tg - g) * t);
        b = Math.round(b + (tb - b) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 提亮约 20%，用作 hover
     */
    public static int hoverOf(int base) {
        return mix(base, 0xFFFFFF, 0.20f);
    }

    /**
     * 压暗约 30%，用作 disabled
     */
    public static int disabledOf(int base) {
        return mix(base, 0x000000, 0.30f);
    }

    /**
     * 只替换 alpha（0~255），RGB 不动
     */
    public static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | (argb & 0x00FFFFFF);
    }

}
