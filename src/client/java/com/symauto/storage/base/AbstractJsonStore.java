package com.symauto.storage.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractJsonStore implements PersistentStore {
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected final Path path;
    @Setter
    private boolean dirty = false;

    protected AbstractJsonStore(String category, String fileName) {
        this.path = FabricLoader.getInstance().getConfigDir()
                .resolve("symauto")
                .resolve(category)
                .resolve(fileName);
    }

    protected abstract void clearData();

    /** 返回用于 gson 序列化的数据对象 */
    protected abstract Object snapshot();

    /** 从 JSON 文本解析并回填内存 */
    protected abstract void restore(String json);

    /** 文件缺失时的处理 */
    protected void onMissingFile() {
    }

    protected void afterLoad() {
    }

    protected String describe() {
        return path.getFileName().toString();
    }

    @Override
    public synchronized void load() {
        clearData();
        if (!Files.exists(path)) {
            onMissingFile();
            return;
        }
        try {
            restore(Files.readString(path));
            afterLoad();
        } catch (Exception e) {
            System.err.println("[SymAuto] 读取 " + describe() + " 失败: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(snapshot()));
            dirty = false;
        } catch (IOException e) {
            System.err.println("[SymAuto] 保存 " + describe() + " 失败: " + e.getMessage());
        }
    }

    @Override
    public synchronized void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }
}
