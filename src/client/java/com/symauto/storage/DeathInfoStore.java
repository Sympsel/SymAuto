package com.symauto.storage;

import com.google.common.reflect.TypeToken;
import com.symauto.storage.base.AbstractJsonStore;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DeathInfoStore extends AbstractJsonStore {
    public static final DeathInfoStore INSTANCE = new DeathInfoStore();
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final List<DeathRecord> RECORDS = new ArrayList<>();

    private DeathInfoStore() {
        super("death_info", "death_records.json");
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

    public static void record(DeathRecord record, int maxSize) {
        if (maxSize <= 0) {
            return;
        }
        synchronized (INSTANCE) {
            RECORDS.add(record);
            while (RECORDS.size() > maxSize) {
                RECORDS.removeFirst();
            }
            INSTANCE.setDirty(true);
            INSTANCE.save();
        }
    }

    public static List<DeathRecord> getRecords() {
        synchronized (INSTANCE) {
            return new ArrayList<>(RECORDS);
        }
    }

    public static DeathRecord latest() {
        synchronized (INSTANCE) {
            return RECORDS.isEmpty() ? null : RECORDS.getLast();
        }
    }
    public static int size() {
        synchronized (INSTANCE) {
            return RECORDS.size();
        }
    }
    /**
     *
     * @return 清空的记录数
     */
    public static int clear() {
        synchronized (INSTANCE) {
            int size = RECORDS.size();
            RECORDS.clear();
            INSTANCE.save();
            return size;
        }
    }

    @Override
    protected void clearData() {
        RECORDS.clear();
    }

    @Override
    protected Object snapshot() {
        return RECORDS;
    }

    @Override
    protected void restore(String json) {
        List<DeathRecord> parsed = gson.fromJson(json,
                new TypeToken<List<DeathRecord>>() {
                }.getType());
        if (parsed != null) {
            RECORDS.addAll(parsed);
        }
    }

    @Override
    protected String describe() {
        return "死亡记录";
    }
}
