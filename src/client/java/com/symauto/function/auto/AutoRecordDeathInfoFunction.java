package com.symauto.function.auto;

import com.symauto.storage.DeathInfoStore;
import com.symauto.config.option.IntOption;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AutoRecordDeathInfoFunction extends SymAbstractFunction {
    public static final AutoRecordDeathInfoFunction INSTANCE = new AutoRecordDeathInfoFunction();

    // 配置项：最多记录的死亡数
    private static final int MAX_SIZE_DEFAULT = 100;
    @Getter
    @Setter
    private int maxSize = MAX_SIZE_DEFAULT;
    private boolean wasDead = false;
    // 当前等待补全死因的记录
    @Nullable
    private DeathInfoStore.DeathRecord pending;


    private AutoRecordDeathInfoFunction() {
        String tooltip = Tooltip.create()
                .line("生存模式下死亡时自动记录死亡地点")
                .line("保存坐标、维度、游戏时间、真实时间戳与死亡原因")
                .line("最多保留最近" + MAX_SIZE_DEFAULT + "条，超出自动淘汰最旧记录")
                .line(ChatFormatting.YELLOW, "若服务器不返回死亡原因，则无法记录死亡原因")
                .toString();
        super("auto_record_death_info", "自动记录死亡信息", tooltip);
        addOption(new IntOption("death_info_record_max_size", "最多保存死亡记录的条目数",
                this::getMaxSize, this::setMaxSize, 0, 200, 10));
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.player.isCreative()) {
            return;
        }

        boolean dead = client.player.isDeadOrDying();
        // 上边沿触发
        if (dead && !wasDead) {
            wasDead = true;
            if (maxSize > 0) {
                pending = createRecord(client);
                if (pending != null) {
                    DeathInfoStore.record(pending, maxSize);
                    notify(client, Component.literal("已记录死亡位置: ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(pending.formatCoordinate() + " @ " + pending.dimension)
                                    .withStyle(ChatFormatting.YELLOW)));
                }
            }
            return;
        }
        if (dead && pending != null && pending.deathCause == null) {
            String cause = extractDeathCause(client.gui.screen());
            if (cause != null) {
                pending.deathCause = cause;
                DeathInfoStore.INSTANCE.save();
                notify(client, Component.literal("死亡原因: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(cause).withStyle(ChatFormatting.RED)));
            }
        }
    }

    private void notify(Minecraft client, Component message) {
        if (client.player == null) {
            return;
        }
        client.player.sendOverlayMessage(message);
    }

    private DeathInfoStore.DeathRecord createRecord(Minecraft client) {
        if (client.level == null || client.player == null) {
            return null;
        }
        String dimension = client.level.dimension().identifier().toString();
        return new DeathInfoStore.DeathRecord(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                dimension,
                client.level.getGameTime(),
                System.currentTimeMillis(),
                null
        );
    }

    @Nullable
    private String extractDeathCause(@Nullable Screen screen) {
        if (!(screen instanceof DeathScreen deathScreen)) {
            return null;
        }
        // 只从字段名判定死因：cause 优先级高于 death，且排除计分板/标题类字段，
        // 避免服务器场景把“分数：xxxx”之类的 Component 误当成死因。
        String byCause = null;
        String byDeath = null;
        for (Class<?> c = deathScreen.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !Component.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                String name = f.getName().toLowerCase();
                if (name.contains("score") || name.contains("title") || name.contains("subtitle")) {
                    continue;
                }
                String text = readComponentText(deathScreen, f);
                if (text == null) {
                    continue;
                }
                if (name.contains("cause")) {
                    if (byCause == null) {
                        byCause = text;
                    }
                } else if (name.contains("death")) {
                    if (byDeath == null) {
                        byDeath = text;
                    }
                }
            }
        }
        return byCause != null ? byCause : byDeath;
    }

    @Nullable
    private String readComponentText(DeathScreen screen, Field f) {
        try {
            f.setAccessible(true);
            if (f.get(screen) instanceof Component comp) {
                String text = comp.getString();
                if (!text.isBlank()) {
                    return text;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
