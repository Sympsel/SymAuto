package com.symauto.function.functions;

import com.symauto.function.abstracts.SymAbstractFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

public class FixYPlaceOrDestroyFunction extends SymAbstractFunction {
    @Setter
    @Getter
    private static int lockedY = 64;
    @Getter
    private static boolean holding = false;

    @Getter
    private static int mineLockedY = 64;
    @Getter
    private static boolean mining = false;

    public FixYPlaceOrDestroyFunction() {
        super("fix_y_place_or_destroy", "锁定 Y 轴放置/破坏", "每次长按破坏/放置按键会阻止影响首次破坏/放置的Y轴之外的方块\n如果与投影的轻松放置冲突，需要启用“轻松放置-重写后（Easy Place - Easy Place Post Rewrite）");
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