package com.symauto.gui.widget.model;

import com.symauto.gui.widget.inter.SymAction;

import java.util.EnumMap;
import java.util.Map;

public class SymInteractions {
    private final Map<SymTrigger, SymAction> actions = new EnumMap<>(SymTrigger.class);

    public SymInteractions bind(SymTrigger trigger, SymAction action) {
        actions.put(trigger, action);
        return this;
    }

    public SymInteractions bind(SymTrigger trigger, Runnable action) {
        return bind(trigger, context -> {
            action.run();
            return true;
        });
    }

    public boolean dispatch(SymTrigger trigger, SymActionContext context) {
        if (trigger == null) {
            return false;
        }
        SymAction action = actions.get(trigger);
        return action != null && action.run(context);
    }
}
