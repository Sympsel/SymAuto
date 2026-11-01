package com.symbase.gui.widget.inter;

import com.symbase.gui.widget.model.SymActionContext;

@FunctionalInterface
public interface SymAction {
    /**
     * @return 是否消费本次事件
     */
    boolean run(SymActionContext context);
}

