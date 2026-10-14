package com.symauto.storage;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DeathInfoStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir()
                    .resolve("symauto").resolve("death_info").resolve("death_records.json");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final List<DeathRecord> RECORDS = new ArrayList<>();
    private static boolean dirty = false;

    private DeathInfoStore() {
    }

    @AllArgsConstructor
    public static final class DeathRecord {
        public double x;
        public double y;
        public double z;
        public String dimension;
        // 游戏时间
        public long gameTime;
        // 真实时间
        public long wallTime;

        public String deathCause;

        public String formatCoordinate() {
            return String.format("%.1f, %.1f, %.1f", x, y, z);
        }

        public String formatWallTime() {
            return TIME_FMT.format(Instant.ofEpochMilli(wallTime));
        }

        public long formatGameDay() {
            return gameTime / 24000L + 1L;
        }

        public String formatCause() {
            return deathCause == null || deathCause.isBlank() ? "未知" : deathCause;
        }
    }

    public static synchronized void record(DeathRecord record, int maxSize) {
        if (maxSize <= 0) {
            return;
        }
        RECORDS.add(record);
        while (RECORDS.size() > maxSize) {
            RECORDS.removeFirst();
        }
        dirty = true;
        save();
    }

    public static synchronized List<DeathRecord> getRecords() {
        return new ArrayList<>(RECORDS);
    }

    public static synchronized DeathRecord latest() {
        return RECORDS.isEmpty() ? null : RECORDS.getLast();
    }

    public static synchronized int size() {
        return RECORDS.size();
    }

    /**
     *
     * @return 清空的记录数
     */
    public static synchronized int clear() {
        int size = RECORDS.size();
        RECORDS.clear();
        save();
        return size;
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(RECORDS));
            dirty = false;
        } catch (IOException e) {
            System.err.println("[SymAuto] 保存死亡记录失败: " + e.getMessage());
        }
    }

    public static synchronized void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public static synchronized void load() {
        RECORDS.clear();
        if (!Files.exists(PATH)) {
            return;
        }
        try {
            String json = Files.readString(PATH);
            List<DeathRecord> parsed = GSON.fromJson(json,
                    new TypeToken<List<DeathRecord>>() {
                    }.getType());
            if (parsed != null) {
                RECORDS.addAll(parsed);
            }
        } catch (Exception e) {
            System.err.println("[SymAuto] 读取死亡记录失败: " + e.getMessage());
        }
    }
}
