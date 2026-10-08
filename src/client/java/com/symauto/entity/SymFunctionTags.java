package com.symauto.entity;

public class SymFunctionTags {
    // 全部 菜单过滤用（不是 BitSet 位，单独用 -1 表示）
    public static final int ALL = -1;

    // 其它
    public static final int OTHER = 0;
    // 挂机
    public static final int IDLE = 1;
    // 生电
    public static final int PRODUCING = 2;
    // 建筑和破坏
    public static final int BUILDING_AND_DESTROYING = 3;
    // 便捷操作
    public static final int CONVENIENCE_OPERATION = 4;
    // 计算
    public static final int CALCULATION = 5;
    // HUD
    public static final int HUD = 6;

    public static final String[] LABELS = {
            "其它", "挂机", "生电", "建筑和破坏", "便捷操作", "HUD"
    };
    // 标签数量
    public static final int COUNT = LABELS.length;


    /**
     *
     * @param tag 不支持组合标签
     */
    public static String getTagName(int tag) {
        if (tag == ALL) {
            return "全部";
        }
        if (tag < 0 || tag >= LABELS.length) {
            return "未命名标签(" + tag + ")";
        }
        return LABELS[tag];
    }
}
