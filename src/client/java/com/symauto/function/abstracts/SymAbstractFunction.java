package com.symauto.function.abstracts;

import com.symauto.entity.TagList;
import com.symauto.function.FeatureConfig;
import com.symauto.function.utils.Tooltip;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SymAbstractFunction {
    private static final Map<Class<?>, SymAbstractFunction> REGISTRY = new ConcurrentHashMap<>();

    protected static final Random RANDOM = new Random();

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Setter
    private String baseToolTip;
    @Getter
    private boolean enable = false;
    @Getter
    private final boolean isConfigurable;

    // 标签 - 用于分类
    @Getter
    protected TagList tags = new TagList();

    public enum ScreenContext {
        NO_SCREEN, //无界面
        ANY_SCREEN, // 任意 Screen
        CONTAINER_SCREEN // 抽象容器界面
    }

    protected SymAbstractFunction(String id, String name, String tooltip) {
        this(id, name, false, tooltip);
    }

    protected SymAbstractFunction(String id, String name, boolean isConfigurable, String baseToolTip) {
        this.id = id;
        this.name = name;
        this.baseToolTip = baseToolTip;
        this.isConfigurable = isConfigurable;
        if (REGISTRY.putIfAbsent(getClass(), this) != null) {
            throw new IllegalStateException(
                    getClass().getName() + "已存在实例");
        }
    }

    public static <T extends SymAbstractFunction> T of(Class<T> clazz) {
        SymAbstractFunction f = REGISTRY.get(clazz);
        if (f == null) {
            throw new IllegalArgumentException(
                    clazz.getName() + "未初始化");
        }
        return clazz.cast(f);
    }

    protected abstract void onTrigger(Minecraft client);

    public @Nullable KeyMapping getKeyMapping() {
        return null;
    }

    public boolean requireCtrl() {
        return false;
    }

    public boolean requireShift() {
        return false;
    }

    public ScreenContext requireScreenContext() {
        return ScreenContext.NO_SCREEN;
    }

    public void onKeyAction(Minecraft client) {
        onTrigger(client);
    }

    /**
     * 容器界面鼠标点击拦截
     * @return 是否放行其余逻辑
     */
    public boolean allowContainerMouseClick(Minecraft client,
                                            AbstractContainerScreen<?> screen,
                                            double mouseX, double mouseY, int button) {
        return true;
    }

    public boolean allowContainerKeyPress(Minecraft client,
                                          AbstractContainerScreen<?> screen,
                                          int keyCode, int scanCode, int modifiers) {
        return true;
    }


    public void tick() {
        if (!enable) return;
        onTrigger(Minecraft.getInstance());
    }

    public void toggle() {
        setEnable(!enable);
    }

    public Set<Class<? extends SymAbstractFunction>> getConflicts() {
        return Collections.emptySet();
    }

    protected void onDisable() {
    }

    public void setEnable(boolean enable) {
        if (this.enable && !enable) {
            onDisable();
        }
        if (enable && !this.enable) {
            disableConflicts();
        }
        this.enable = enable;
    }

    /**
     * 遍历所有已注册功能，关闭与当前功能冲突且已启用的项
     * 采用双向判定：只要任意一方声明了冲突即生效
     */
    private void disableConflicts() {
        Set<Class<? extends SymAbstractFunction>> mine = getConflicts();
        for (SymAbstractFunction other : FeatureConfig.AUTO_ALL) {
            if (other == this || !other.isEnable()) {
                continue;
            }
            boolean conflict = mine.contains(other.getClass())
                    || other.getConflicts().contains(this.getClass());
            if (conflict) {
                other.setEnable(false);
            }
        }
    }

    public SymAbstractFunction addTag(int tag) {
        tags.addTag(tag);
        return this;
    }

    public boolean hasTag(int tag) {
        return tags.has(tag);
    }

    public String tooltipLineIfConfigurable() {
        return isConfigurable
                ? "左键切换开启状态，右键进入配置菜单"
                : "左键切换开启状态";
    }

    public final String getTooltip() {
        Tooltip tooltip = Tooltip.create()
                .line(ChatFormatting.GOLD, tooltipLineIfConfigurable());
        String desc = describe();
        if (desc != null && !desc.isBlank()) {
            tooltip.line(desc);
        }
        return tooltip.toString();
    }

    protected String describe() {
        return baseToolTip;
    }
}
