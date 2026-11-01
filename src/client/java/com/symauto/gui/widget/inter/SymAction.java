package com.symauto.gui.widget.inter;

import com.symauto.gui.widget.model.SymActionContext;

@FunctionalInterface
public interface SymAction {
    /**
     * @return 是否消费本次事件
     */
    boolean run(SymActionContext context);
}