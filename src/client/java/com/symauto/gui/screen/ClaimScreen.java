package com.symauto.gui.screen;

import com.symauto.gui.base.SymBaseScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ClaimScreen extends SymBaseScreen {
    private static final int TEXT_W = 220;
    private static final int TEXT_TOP = 32;
    private static final int LINE_H = 10;
    private static final int PARA_GAP = 4;

    private static final List<String> CLAIM = List.of(
            "本 MOD 部分功能属于“作弊”功能，使用本 MOD 即同意以下条例：",
            "1. 禁止用于 PVP 或使用部分被服务器明确禁止的功能",
            "2. 请仅在游玩时遭隐身玩家骚扰时才考虑采取“高亮周围玩家”的措施",
            "若由于违反上述条例导致被服务器封禁或承担其它损失，本 MOD 作者概不负责"
    );

    private final Screen parent;

    public ClaimScreen(Screen parent) {
        super(Component.literal("声明").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(backButton(parent, "返回", 60));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderTitle(graphics);

        int x = (this.width - TEXT_W) / 2;
        int y = TEXT_TOP;
        for (String paragraph : CLAIM) {
            for (var line : this.font.split(Component.literal(paragraph), TEXT_W)) {
                graphics.text(this.font, line, x, y, this.style.text, false);
                y += LINE_H;
            }
            y += PARA_GAP;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}