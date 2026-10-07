package com.symauto.update;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateMessageManager {
    public static final UpdateMessageManager INSTANCE = new UpdateMessageManager();

    private final List<UpdateMessage> updateMessages;

    private void addUpdateMessage(String formatedText) {
        // 格式：[标准时间][版本] 更新内容
        Pattern pattern = Pattern.compile("^\\[([^\\]]+)\\]\\[([^\\]]+)\\]\\s*(.+)$");
        Matcher matcher = pattern.matcher(formatedText);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid update message format: " + formatedText);
        }

        String formatedTime = matcher.group(1);
        String version = matcher.group(2);
        String message = matcher.group(3);

        long createTime;
        try {
            createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(formatedTime).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("时间格式错误: " + formatedTime, e);
        }

        UpdateMessage updateMessage = new UpdateMessage(createTime, version, message);
        updateMessages.add(updateMessage);
    }

    private void setUpdateMessages() {
        updateMessages.clear();
        addUpdateMessage("[2026-10-07 11:14:00][v26.2-2.8.2] 添加了更新日志模块；");
    }

    private UpdateMessageManager() {
        updateMessages = new ArrayList<>();
    }
}
