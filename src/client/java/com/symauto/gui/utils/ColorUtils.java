package com.symauto.gui.utils;

public class ColorUtils {
    public static int onPress(int argb) {
        int a = argb >>> 24;
        float amount = 0.2F;
        int r = (int) (((argb >> 16) & 0xFF) * (1 - amount));
        int g = (int) (((argb >> 8) & 0xFF) * (1 - amount));
        int b = (int) ((argb & 0xFF) * (1 - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int onHover(int argb) {
        int a = argb >>> 24;
        float amount = 0.2F;
        int r = (int) (((argb >> 16) & 0xFF) + (255 - ((argb >> 16) & 0xFF)) * amount);
        int g = (int) (((argb >> 8) & 0xFF) + (255 - ((argb >> 8) & 0xFF)) * amount);
        int b = (int) ((argb & 0xFF) + (255 - (argb & 0xFF)) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int onActive(int argb) {
        int a = argb >>> 24;
        float amount = 0.25F;
        int r = (int) (((argb >> 16) & 0xFF) + (255 - ((argb >> 16) & 0xFF)) * amount);
        int g = (int) (((argb >> 8) & 0xFF) + (255 - ((argb >> 8) & 0xFF)) * amount);
        int b = (int) ((argb & 0xFF) + (255 - (argb & 0xFF)) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int onDisabled(int argb) {
        int a = argb >>> 24;
        float amount = 0.35F;
        int r = (int) (((argb >> 16) & 0xFF) * (1 - amount));
        int g = (int) (((argb >> 8) & 0xFF) * (1 - amount));
        int b = (int) ((argb & 0xFF) * (1 - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
