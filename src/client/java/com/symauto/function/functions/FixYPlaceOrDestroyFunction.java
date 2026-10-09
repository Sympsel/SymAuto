package com.symauto.function.functions;

import com.symauto.entity.SymFunctionTags;
import com.symauto.function.abstracts.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class FixYPlaceOrDestroyFunction extends SymAbstractFunction {
    public static final FixYPlaceOrDestroyFunction INSTANCE = new FixYPlaceOrDestroyFunction();
    @Setter
    @Getter
    private static int lockedY = 64;
    @Getter
    private static boolean holding = false;

    @Getter
    private static int mineLockedY = 64;
    @Getter
    private static boolean mining = false;

    private FixYPlaceOrDestroyFunction() {
        super("fix_y_place_or_destroy", "锁定 Y 轴放置/破坏",
                Tooltip.create()
                        .line("每次长按破坏/放置按键会阻止影响首次破坏/放置的Y轴之外的方块")
                        .line(ChatFormatting.YELLOW, "如果与投影的轻松放置冲突，需要启用“轻松放置-重写后（Easy Place - Easy Place Post Rewrite）")
                        .toString()
        );
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    public static void startHold(int y) {
        lockedY = y;
        holding = true;
    }

    public static void endHold() {
        holding = false;
    }

    public static void startMining(int y) {
        mineLockedY = y;
        mining = true;
    }

    public static void endMining() {
        mining = false;
    }
}