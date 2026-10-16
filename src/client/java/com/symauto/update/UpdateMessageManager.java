package com.symauto.update;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UpdateMessageManager {
    public static final UpdateMessageManager INSTANCE = new UpdateMessageManager();

    // 运行时
    private static final String UPDATE_LOG_RESOURCE = "/assets/symauto/update_log.txt";
    // 开发期
    private static final String UPDATE_LOG_SOURCE = "src/client/resources/assets/symauto/update_log.txt";
    private static final String GRADLE_FILE = "gradle.properties";

    @Getter
    private final List<String> updateMessages;

    static void main() {
        sendUpdate("不再显示更新日志异常显示的时分秒");
    }

    private static void sendUpdate(String context) {
        long timeStamp = System.currentTimeMillis();
        String createTime = new SimpleDateFormat("yyyy-MM-dd").format(timeStamp);
        File file = new File(UPDATE_LOG_SOURCE);
        File gradle = new File(GRADLE_FILE);
        if (!file.exists()) {
            System.out.println("更新日志文件不存在: " + file.getAbsolutePath());
            return;
        }
        if (!gradle.exists()) {
            System.out.println("gradle.properties 文件不存在: " + gradle.getAbsolutePath());
            return;
        }

        String version = "";
        try {
            List<String> lines = Files.readAllLines(gradle.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("mod_version")) {
                    version = line.split("=")[1].trim();
                    break;
                }
            }
            if (version.isEmpty()) {
                System.err.println("未识别到版本号");
                return;
            }
            System.out.println("当前版本号为: " + version);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            List<String> lines = new ArrayList<>(
                    Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            lines.addFirst("[" + createTime + "][" + version + "] " + context);
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            System.out.println("更新日志已保存到 " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UpdateMessageManager() {
        updateMessages = new ArrayList<>();
        try (InputStream in = UpdateMessageManager.class.getResourceAsStream(UPDATE_LOG_RESOURCE)) {
            if (in == null) {
                System.err.println("[SymAuto] 未找到更新日志资源: " + UPDATE_LOG_RESOURCE);
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        updateMessages.add(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}