package com.symauto.storage.base;

public interface PersistentStore {
    /** 从磁盘加载数据到内存；文件缺失时触发默认值钩子 */
    void load();

    /** 将当前内存快照写回磁盘 */
    void save();

    /** 仅当存在未落盘的变更时保存 */
    void saveIfDirty();

    /** 是否存在尚未写盘的改动。 */
    boolean isDirty();
}
