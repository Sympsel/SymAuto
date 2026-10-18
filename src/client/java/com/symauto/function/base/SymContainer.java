package com.symauto.function.base;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * 统一容器抽象
 */
public interface SymContainer {

    enum Type {
        SHULKER_BOX,
        BUNDLE
    }

    Type type();

    /** 容器的物品形态 */
    ItemStack host();

    /** 该容器实现是否支持把变更写回 host 组件 */
    boolean supportsDirectWrite();

    /** 条目数 */
    int entryCount();

    /** 通用填充率 0~1 */
    double fillRatio();

    /** 还能放入多少个 toInsert */
    int maxAcceptable(ItemStack toInsert);

    boolean canFit(ItemStack toInsert);

    boolean isEmpty();

    boolean isFull();

    /** 全部非空条目副本 */
    List<ItemStack> entries();

    /** 按当前顺序取第 index 个条目 */
    Optional<ItemStack> entryAt(int index);

    /** 首个与 probe 同种同类物品的条目下标，无则 -1 */
    int indexOf(ItemStack probe);

    /** 容器内与 probe 同种物品的累计数量 */
    int countOf(ItemStack probe);

    boolean contains(ItemStack probe);

    /**
     * 尽量并入同类已有条目，再新建
     * @return 实际加入的数量
     */
    int add(ItemStack stack);

    /**
     * 移除与 probe 同种物品的数量
     * @param count 要移除的数量
     * @return 实际移除数量
     */
    int remove(ItemStack probe, int count);

    /**
     * 用 stack 替换第 index 个条目
     * @param index 下标
     * @param stack 新条目
     * @return 是否成功替换
     */
    boolean setEntry(int index, ItemStack stack);

    /**
     * 清空全部内容条目
     */
    void clear();

    /**
     * 把上面的逻辑变更提交
     * 若 supportsDirectWrite() 为真：直接 set 到 host 的 DataComponents；否则，通过容器交互实现
     * @return 是否成功提交
     */
    boolean commit();
}