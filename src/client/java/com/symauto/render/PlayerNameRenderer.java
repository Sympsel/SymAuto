package com.symauto.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.symauto.function.functions.HighLightPlayerFunction;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 浮空玩家名字渲染：在世界渲染阶段投影坐标，在 HUD 阶段绘制文本（可穿墙）。AI 生成
 */
public final class PlayerNameRenderer {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("symauto", "player_name_hud");
    private static final int BOX_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    // 渲染线程与客户端 tick 主线程在同一游戏循环线程，读写无需额外同步
    private static final List<Draw> QUEUE = new ArrayList<>();

    private record Draw(int x, int y, int width, Component text) {
    }

    private PlayerNameRenderer() {
    }

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(PlayerNameRenderer::extract);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HUD_ID, PlayerNameRenderer::draw);
    }

    private static void extract(LevelRenderContext context) {
        QUEUE.clear();
        Minecraft client = Minecraft.getInstance();
        HighLightPlayerFunction function = HighLightPlayerFunction.INSTANCE;
        if (!function.isEnable() || client.player == null || client.level == null) {
            return;
        }
        Map<UUID, String> revealed = function.getRevealedIds();
        if (revealed.isEmpty()) {
            return;
        }

        Font font = client.font;
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        Matrix4f view = RenderSystem.getModelViewMatrixCopy();
        float fovRad = (float) Math.toRadians(client.gameRenderer.mainCamera().getFov());
        float tanHalfFov = (float) Math.tan(fovRad * 0.5F);
        float aspect = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();

        for (Map.Entry<UUID, String> entry : revealed.entrySet()) {
            UUID uuid = entry.getKey();
            if (uuid.equals(client.player.getUUID())) {
                continue;
            }
            Entity e = client.level.getEntity(uuid);
            if (!(e instanceof Player player) || !player.isAlive()) {
                continue;
            }
            // 头顶锚点
            Vec3 anchor = player.position().add(0.0, player.getBbHeight() + 0.35, 0.0);
            Vector4f v = new Vector4f(
                    (float) (anchor.x - cam.x),
                    (float) (anchor.y - cam.y),
                    (float) (anchor.z - cam.z),
                    1.0F);
            view.transform(v); // 世界相对坐标 -> 视图空间（-Z 为前方）
            float forward = -v.z();
            if (forward <= 1.0E-4F) {
                continue; // 相机背后
            }
            double ndcX = v.x() / (aspect * tanHalfFov * forward);
            double ndcY = v.y() / (tanHalfFov * forward);
            int sx = (int) ((ndcX * 0.5 + 0.5) * guiW);
            int sy = (int) ((0.5 - ndcY * 0.5) * guiH);
            if (sx < -guiW || sx > guiW * 2 || sy < -guiH || sy > guiH * 2) {
                continue; // 明显出屏，跳过
            }

            int dist = Math.round(client.player.distanceTo(player));
            float hp = player.getHealth();
            String hpText = String.format("%.1f", hp) + "♥";
            String plain = entry.getValue() + " " + hpText + " " + dist + "m";
            Component text = Component.literal(entry.getValue()).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" " + hpText).withStyle(healthColor(hp)))
                    .append(Component.literal(" " + dist + "m").withStyle(ChatFormatting.RED));
            int w = font.width(plain);
            QUEUE.add(new Draw(sx - w / 2, sy, w, text));
        }
    }

    private static ChatFormatting healthColor(float hp) {
        if (hp >= 12.0F) {
            return ChatFormatting.GREEN;
        }
        if (hp >= 6.0F) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.RED;
    }

    private static void draw(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (QUEUE.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        for (Draw d : QUEUE) {
            graphics.fill(d.x() - 2, d.y() - 1, d.x() + d.width() + 2, d.y() + font.lineHeight + 1, BOX_COLOR);
            graphics.text(font, d.text(), d.x(), d.y(), TEXT_COLOR, true);
        }
    }
}