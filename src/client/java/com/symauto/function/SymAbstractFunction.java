package com.symauto.function;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

public abstract class SymAbstractFunction {
    protected static final Random RANDOM = new Random();

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String tooltip;
    @Getter
    private boolean enable = false;

    public SymAbstractFunction(String id, String name, String tooltip) {
        this.id = id;
        this.name = name;
        this.tooltip = tooltip;
    }

    protected abstract void onTrigger(Minecraft client);

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

    protected void onDisable() {}

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
     * 遍历所有已注册功能，关闭与当前功能冲突且已启用的项。
     * 采用双向判定：只要任意一方声明了冲突即生效，
     * 因此冲突关系只需在一侧声明即可。
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
}
