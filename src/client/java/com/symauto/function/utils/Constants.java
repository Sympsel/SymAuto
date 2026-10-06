package com.symauto.function.utils;

public class Constants {
    private Constants() {
    }

    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 9;
    public static final int INV_START = 9;
    public static final int INV_END = 36;
    public static final int SYNC_TICKS = 2;

    // 背包界面常量
    public static final int RESULT = 0; // 合成结果
    // 合成格起始
    public static final int MATERIAL_START = 1;
    public static final int MATERIAL_END = 5; // 左闭右开区间
    // 盔甲栏起始
    public static final int ARMOR_START = 5;
    public static final int ARMOR_END = 9; // 左闭右开区间
    // 主背包
    public static final int MAIN_INVENTORY_START = 9;
    public static final int MAIN_INVENTORY_END = 36; // 左闭右开区间
    // 快捷栏
    public static final int HOTBAR_START_IN_INVENTORY = 36;
    public static final int HOTBAR_END_IN_INVENTORY = 45; // 左闭右开区间
    // 副手栏
    public static final int OFFHAND = 45;

}
