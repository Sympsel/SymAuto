package com.symbase.gui.widget.model;

import com.symbase.gui.widget.inter.SymAction;

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
        if (action == null) {
            action = actions.get(singleClickTrigger(trigger));
        }
        return action != null && action.run(context);
    }

    private static SymTrigger singleClickTrigger(SymTrigger trigger) {
        return switch (trigger) {
            case LEFT_DOUBLE_CLICK -> SymTrigger.LEFT_CLICK;
            case RIGHT_DOUBLE_CLICK -> SymTrigger.RIGHT_CLICK;
            case MIDDLE_DOUBLE_CLICK -> SymTrigger.MIDDLE_CLICK;
            default -> trigger;
        };
    }
}


