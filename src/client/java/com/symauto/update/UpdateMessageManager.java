package com.symauto.update;

import lombok.Getter;
import net.minecraft.ChatFormatting;

import java.io.File;
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
        sendUpdates(
                "修复了误把甜菜根作为种子导致无法种植甜菜根的bug",
                "对退出游戏时偶现的异常问题进行了兜底防护，提升程序健壮性"
        );
    }

    private static String bug(String bug) {
        return ChatFormatting.DARK_GRAY + bug + ChatFormatting.WHITE;
    }

    private static String file(String path) {
        return ChatFormatting.AQUA + path + ChatFormatting.WHITE;
    }

    private static String cmd(String command) {
        return ChatFormatting.YELLOW + command + ChatFormatting.WHITE;
    }

    private static String key(String key) {
        return ChatFormatting.GOLD + key + ChatFormatting.WHITE;
    }

    private static String func(String func) {
        return ChatFormatting.DARK_AQUA + func + ChatFormatting.WHITE;
    }

    private static String enhance(String text) {
        return ChatFormatting.GRAY + text + ChatFormatting.WHITE;
    }


    private static String colored(ChatFormatting color, String text) {
        return color + text + ChatFormatting.WHITE;
    }

    private static String configuration(String config) {
        return ChatFormatting.YELLOW + config + ChatFormatting.WHITE;
    }

    private static String item(String item, int id) {
        return "\n  " + ChatFormatting.AQUA + id + ". " + ChatFormatting.WHITE + item;
    }

    private static String item(String item) {
        return "\n  " + item;
    }

    private static void sendUpdates(String... updates) {
        if (updates.length == 0) {
            return;
        }
        if (updates.length == 1) {
            sendUpdate(item(updates[0]));
            return;
        }
        int id = 1;
        StringBuilder items = new StringBuilder();
        for (String update : updates) {
            items.append(item(update, id++));
        }
        sendUpdate(items.toString());
    }

    private static void sendUpdate(String context) {
        long timeStamp = System.currentTimeMillis();
        String createTime = new SimpleDateFormat("yyyy-MM-dd").format(timeStamp);
        File file = new File(UPDATE_LOG_SOURCE);
        File gradle = new File(GRADLE_FILE);
        if (!file.exists() || !gradle.exists()) {
            return; // 略微简化异常处理
        }

        String version = "";
        try {
            List<String> lines = Files.readAllLines(gradle.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("mod_version")) {
                    version = line.split("=")[1].trim();
                    break;
                }
            }
            if (version.isEmpty()) return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            String existingContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            // 【核心修改】头部和时间在一行，内容加 \n 放到下一行
            String newEntry = "[" + createTime + "][" + version + "]\n" + context + "\n";

            String finalContent = newEntry + (existingContent.startsWith("\n") ? "" : "\n") + existingContent;

            Files.writeString(file.toPath(), finalContent, StandardCharsets.UTF_8);
            System.out.println("更新日志已保存到 " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UpdateMessageManager() {
        updateMessages = new ArrayList<>();
        // 将整个文件内容作为一个元素加入列表，交由 UI 去正则匹配
        try (var in = UpdateMessageManager.class.getResourceAsStream(UPDATE_LOG_RESOURCE)) {
            if (in == null) {
                System.err.println("[SymAuto] 未找到更新日志资源: " + UPDATE_LOG_RESOURCE);
                return;
            }
            String fullText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!fullText.isBlank()) {
                updateMessages.add(fullText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}