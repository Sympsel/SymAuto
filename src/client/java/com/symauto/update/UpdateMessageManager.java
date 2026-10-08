package com.symauto.update;

import lombok.Getter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateMessageManager {
    public static final UpdateMessageManager INSTANCE = new UpdateMessageManager();

    @Getter
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
            createTime = new SimpleDateFormat("yyyy-MM-dd").parse(formatedTime).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("时间格式错误: " + formatedTime, e);
        }

        UpdateMessage updateMessage = new UpdateMessage(createTime, version, message);
        updateMessages.add(updateMessage);
    }

    private void setUpdateMessages() {
        updateMessages.clear();
        addUpdateMessage("[2026-10-08][v26.2-2.10.1] 添加新功能：快速搭路（目前不支持下半砖）、鼠标悬浮物品时 Ctrl + A 平铺物品");
        addUpdateMessage("[2026-10-07][v26.2-2.9.2] 对菜单的功能进行了分类");
        addUpdateMessage("[2026-10-07][v26.2-2.9.1] 添加新功能：自动填充物品分类机");
        addUpdateMessage("[2026-10-07][v26.2-2.8.6] 菜单添加了\"指令用法\"");
        addUpdateMessage("[2026-10-07][v26.2-2.8.5] 为自动攻击配置了黑名单机制（默认为展示框、画、村民、玩家，可通过指令\"/sa bwlist auto_attack_safety_blacklist blacklist add/remove\"配置）；重构了部分显示效果");
        addUpdateMessage("[2026-10-07][v26.2-2.8.4] 在菜单新增了更新日志按钮，可以便捷查阅；优化了菜单界面");
        addUpdateMessage("[2026-10-07][v26.2-2.8.3] 自动钓鱼：对未落水或非开放水域做了检测，未在水中的会10秒后重新抛竿");
        addUpdateMessage("[2026-10-07][v26.2-2.8.2] 添加了更新日志模块");
    }

    private UpdateMessageManager() {
        updateMessages = new ArrayList<>();
        setUpdateMessages();
    }
}
