package com.symauto.function.functions;

import com.mojang.blaze3d.platform.InputConstants;
import com.symauto.config.SymAutoKeys;
import com.symauto.config.option.BooleanOption;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.glfw.GLFW;

public class AutoRunFunction extends SymAbstractFunction {
    public static final AutoRunFunction INSTANCE = new AutoRunFunction();
    @Getter
    @Setter
    private boolean autoFixedScreen = true;
    private final KeyMapping toggleKey;
    @Getter
    private boolean isActive = false;
    private AutoRunFunction() {
        super("auto_run", "自动跑路",Tooltip.create().line( "按下 Ctrl + W 激活，并自动八向校正视角，再次按下取消，建议搭配自动搭路实现挂机").toString());
        this.toggleKey = new KeyMapping(
                "key.symauto.auto_run",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_W,
                SymAutoKeys.CATEGORY_MAIN);
        ClientTickEvents.START_CLIENT_TICK.register(this::correctViewBeforeMovement);
        addOption(new BooleanOption("auto_fixed_screen", "自动锁定视角",
                this::isAutoFixedScreen, this::setAutoFixedScreen, false));
    }

    @Override
    public String describe() {
        return Tooltip.create()
                .line("按下 Ctrl + W 激活，并自动八向校正视角，再次按下取消，建议搭配自动搭路实现挂机")
                .keyValueLine(ChatFormatting.YELLOW, "自动锁定视角", ChatFormatting.GRAY, autoFixedScreen ? "是" : "否")
                .toString();
    }

    private void correctViewBeforeMovement(Minecraft client) {
        if (!isActive || client.player == null || client.level == null) {
            return;
        }
        if (client.gui.screen() != null || client.player.isDeadOrDying()) {
            return;
        }
        if (autoFixedScreen) {
            // 仰角校正
            client.player.setXRot(0.0F);
            client.player.xRotO = 0.0F;
            // 八向视角校正
            float yaw = client.player.getYRot();
            float snappedYaw = Math.round(yaw / 45.0F) * 45.0F;
            client.player.setYRot(snappedYaw);
            client.player.yRotO = snappedYaw;
        }
    }

    @Override
    protected void onTrigger(Minecraft client) {
        if (!isActive) return;
        if (client.player == null || client.level == null) {
            stop(client);
            return;
        }

        if (client.player.isDeadOrDying()) {
            stop(client);
            return;
        }

        client.options.keyUp.setDown(true);
        client.options.keySprint.setDown(true);
    }

    @Override
    public KeyMapping getKeyMapping() { return toggleKey; }

    @Override
    public boolean requireCtrl() { return true; }

    @Override
    public ScreenContext requireScreenContext() {
        return super.requireScreenContext();
    }

    @Override
    public void onKeyAction(Minecraft client) {
        if (isActive) {
            stop(client);
        } else {
            start(client);
        }
    }

    @Override
    protected void onDisable() {
        stop(Minecraft.getInstance());
    }

    private void start(Minecraft client) {
        if (client.player == null) {
            return;
        }
        isActive = true;
    }

    private void stop(Minecraft client) {
        if (!isActive) {
            return;
        }
        isActive = false;
        if (client.player == null) {
            return;
        }
        client.options.keyUp.setDown(false);
        client.options.keySprint.setDown(false);
    }
}
