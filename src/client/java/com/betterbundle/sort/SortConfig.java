package com.betterbundle.sort;

public final class SortConfig {
    /** 袋子总重量单位（唯一容量约束，对应 BundleContents 的 Fraction.ONE）。 */
    public static final int W_FULL = 64;

    /** 内嵌收纳袋额外重量：BundleContents.BUNDLE_IN_BUNDLE_WEIGHT = 1/16 = 4/64。 */
    public static final int NESTED_BUNDLE_WEIGHT = 4;

    public static final int MIN_DELAY_TICKS = 2;
    public static final int MAX_DELAY_TICKS = 5;

    /** 服务端确认超时：20 tick = 1s。 */
    public static final int CONFIRM_TIMEOUT_TICKS = 20;

    public static final int MAX_EVAC_DEPTH = 3;
    public static final int MAX_TOTAL_MOVES = 256;

    /** 单次整理最多移动的物品个数（逐件执行，防止规模过大）。 */
    public static final int MAX_TOTAL_ITEMS = 512;

    private SortConfig() {}
}
