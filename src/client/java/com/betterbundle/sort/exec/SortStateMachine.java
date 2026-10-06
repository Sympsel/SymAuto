package com.betterbundle.sort.exec;

import com.betterbundle.sort.SortConfig;
import com.betterbundle.sort.model.BagEntry;
import com.betterbundle.sort.model.BagModel;
import com.betterbundle.sort.model.BundleSnapshotBuilder;
import com.betterbundle.sort.model.InventoryModel;
import com.betterbundle.sort.net.BundlePacketSender;
import com.betterbundle.sort.net.BundleSignature;
import com.betterbundle.sort.plan.MoveAction;
import com.betterbundle.sort.plan.PlannedMove;
import com.betterbundle.sort.plan.SortPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 执行层：节流异步状态机（整叠移动）。
 *
 * <p>每次逻辑动作移动一个袋内条目（整叠）：取出→放入，然后等待服务端把源袋/目标袋更新到
 * 预期内容，再随机等 2~5 tick 发下一条。超时 1s 熔断回滚；玩家任意输入立即中止不回滚。
 */
public final class SortStateMachine {

    public enum State { IDLE, READY, WAIT_CONFIRM, ROLLBACK_PREPARE, ROLLBACK_WAIT, DONE, ABORTED }

    private static final SortStateMachine INSTANCE = new SortStateMachine();

    private final Random random = new Random();
    private final List<PlannedMove> ordered = new ArrayList<>();
    private final Deque<MoveAction> history = new ArrayDeque<>();

    private State state = State.IDLE;
    private int actionIndex;
    private int waitTicks;
    private int delayTicks;
    private int activeContainerId = -1;
    private MoveAction rollbackPending;
    private String message = "";
    private int messageTicks;

    private Map<String, Integer> expectedSrc;
    private Map<String, Integer> expectedDst;
    private int expectedSrcSlot = -1;
    private int expectedDstSlot = -1;

    private SortStateMachine() {}

    public static SortStateMachine get() {
        return INSTANCE;
    }

    public boolean isRunning() {
        return state == State.READY || state == State.WAIT_CONFIRM
                || state == State.ROLLBACK_PREPARE || state == State.ROLLBACK_WAIT;
    }

    public State state() {
        return state;
    }

    public int progressDone() {
        return actionIndex;
    }

    public int progressTotal() {
        return ordered.size();
    }

    public String message() {
        return messageTicks > 0 ? message : "";
    }

    /** 返回空串表示可开始；否则为拒绝原因。 */
    public String start(SortPlan plan) {
        if (isRunning()) return "整理进行中";
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return "未进入世界";
        if (!player.containerMenu.getCarried().isEmpty()) return "请先放下光标上的物品";

        int items = 0;
        for (PlannedMove m : plan.moves()) items += m.action().count();
        if (items > SortConfig.MAX_TOTAL_ITEMS) return "需要整理物品过多，请分批整理";

        ordered.clear();
        ordered.addAll(plan.moves());
        history.clear();
        actionIndex = 0;
        waitTicks = 0;
        delayTicks = 0;
        rollbackPending = null;
        clearExpectation();
        activeContainerId = player.containerMenu.containerId;
        message = "";
        messageTicks = 0;
        state = ordered.isEmpty() ? State.DONE : State.READY;
        if (ordered.isEmpty()) setMessage("无需整理");
        return "";
    }

    public void abortByUser() {
        if (!isRunning()) return;
        clearExpectation();
        state = State.ABORTED;
        setMessage("已取消");
    }

    public void onClientTick() {
        if (messageTicks > 0) messageTicks--;
        try {
            switch (state) {
                case READY -> tickReady();
                case WAIT_CONFIRM -> tickWaitConfirm();
                case ROLLBACK_PREPARE -> tickRollbackPrepare();
                case ROLLBACK_WAIT -> tickRollbackWait();
                default -> { }
            }
        } catch (Throwable t) {
            // 任何意外（槽位/容器变化、越界等）都优雅中止，绝不让异常冒泡导致客户端崩溃。
            clearExpectation();
            rollbackPending = null;
            state = State.ABORTED;
            setMessage("整理异常，已中止");
        }
    }

    private void tickReady() {
        if (actionIndex >= ordered.size()) {
            finish("整理完成");
            return;
        }
        if (delayTicks-- > 0) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || player.containerMenu.containerId != activeContainerId) {
            abortByUser();
            return;
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            abortByUser();
            return;
        }

        MoveAction action = ordered.get(actionIndex).action();
        if (!preconditionsHold(player, action)) {
            circuitBreak("执行前状态不符");
            return;
        }
        captureExpectation(player, action);
        if (!BundlePacketSender.sendMove(action)) {
            clearExpectation();
            circuitBreak("发包前置条件不满足");
            return;
        }
        waitTicks = 0;
        state = State.WAIT_CONFIRM;
    }

    private void tickWaitConfirm() {
        waitTicks++;
        if (isConfirmed()) {
            clearExpectation();
            MoveAction action = ordered.get(actionIndex).action();
            history.push(new MoveAction(action.dstBagSlot(), action.key(), action.count(), action.srcBagSlot()));
            actionIndex++;
            delayTicks = SortConfig.MIN_DELAY_TICKS
                    + random.nextInt(SortConfig.MAX_DELAY_TICKS - SortConfig.MIN_DELAY_TICKS + 1);
            state = State.READY;
        } else if (waitTicks > SortConfig.CONFIRM_TIMEOUT_TICKS) {
            clearExpectation();
            circuitBreak("服务端 1s 未确认");
        }
    }

    private void tickRollbackPrepare() {
        if (rollbackPending == null) {
            if (history.isEmpty()) {
                state = State.ABORTED;
                setMessage("已回滚");
                return;
            }
            rollbackPending = history.pop();
            waitTicks = 0;
            delayTicks = 0;
        }
        if (delayTicks-- > 0) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) {
            rollbackPending = null;
            state = State.ABORTED;
            return;
        }
        captureExpectation(player, rollbackPending);
        if (!BundlePacketSender.sendMove(rollbackPending)) {
            clearExpectation();
            rollbackPending = null;
            delayTicks = 0;
            return;
        }
        waitTicks = 0;
        state = State.ROLLBACK_WAIT;
    }

    private void tickRollbackWait() {
        waitTicks++;
        if (isConfirmed()) {
            clearExpectation();
            rollbackPending = null;
            delayTicks = SortConfig.MIN_DELAY_TICKS + random.nextInt(3);
            state = State.ROLLBACK_PREPARE;
        } else if (waitTicks > SortConfig.CONFIRM_TIMEOUT_TICKS) {
            clearExpectation();
            rollbackPending = null;
            delayTicks = 0;
            state = State.ROLLBACK_PREPARE;
        }
    }

    private void circuitBreak(String reason) {
        setMessage(reason);
        rollbackPending = null;
        delayTicks = 0;
        state = history.isEmpty() ? State.ABORTED : State.ROLLBACK_PREPARE;
    }

    private boolean preconditionsHold(Player player, MoveAction action) {
        InventoryModel model = BundleSnapshotBuilder.build(player);
        BagModel src = model.bySlot(action.srcBagSlot());
        BagModel dst = model.bySlot(action.dstBagSlot());
        if (src == null || dst == null || dst.locked) return false;
        BagEntry entry = src.findEntry(action.key());
        if (entry == null || entry.count < action.count()) return false;
        return dst.freeWeight() >= entry.weight;
    }

    /** 预期：源袋该物品 -count，目标袋该物品 +count。 */
    private void captureExpectation(Player player, MoveAction action) {
        expectedSrcSlot = action.srcBagSlot();
        expectedDstSlot = action.dstBagSlot();
        ItemStack srcStack = stackAt(player, expectedSrcSlot);
        ItemStack dstStack = stackAt(player, expectedDstSlot);

        Map<String, Integer> srcMap = BundleSignature.of(srcStack);
        Map<String, Integer> dstMap = BundleSignature.of(dstStack);
        String key = BundleSignature.keyOf(action.key().representative());

        int srcCount = srcMap.getOrDefault(key, 0) - action.count();
        if (srcCount > 0) srcMap.put(key, srcCount);
        else srcMap.remove(key);

        dstMap.merge(key, action.count(), Integer::sum);

        expectedSrc = srcMap;
        expectedDst = dstMap;
    }

    private boolean isConfirmed() {
        if (expectedSrc == null || expectedDst == null) return false;
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || player.containerMenu.containerId != activeContainerId) return false;

        ItemStack srcStack = stackAt(player, expectedSrcSlot);
        ItemStack dstStack = stackAt(player, expectedDstSlot);
        return BundleSignature.of(srcStack).equals(expectedSrc)
                && BundleSignature.of(dstStack).equals(expectedDst);
    }

    /** 安全读取槽位物品；越界/异常一律返回空，绝不抛出。 */
    private static ItemStack stackAt(Player player, int slotIndex) {
        if (player == null) return ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= player.containerMenu.slots.size()) return ItemStack.EMPTY;
        try {
            Slot slot = player.containerMenu.getSlot(slotIndex);
            return slot == null ? ItemStack.EMPTY : slot.getItem();
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private void clearExpectation() {
        expectedSrc = null;
        expectedDst = null;
        expectedSrcSlot = -1;
        expectedDstSlot = -1;
    }

    private void finish(String text) {
        state = State.DONE;
        setMessage(text);
    }

    private void setMessage(String text) {
        message = text == null ? "" : text;
        messageTicks = 40;
    }
}
