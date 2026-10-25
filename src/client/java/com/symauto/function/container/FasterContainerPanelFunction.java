package com.symauto.function.container;

import com.symauto.config.option.BooleanOption;
import com.symauto.function.base.SymAbstractFunction;
import com.symauto.function.utils.Tooltip;
import com.symauto.gui.container.ContainerPanel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

public class FasterContainerPanelFunction extends SymAbstractFunction {
    public static final FasterContainerPanelFunction INSTANCE = new FasterContainerPanelFunction();

    @Getter
    @Setter
    private boolean bundlePanelEnabled;

    @Getter
    @Setter
    private boolean shulkerBoxPanelEnabled;

    private FasterContainerPanelFunction() {
        super("faster_container_panel", "快捷容器面板");
        addOption(new BooleanOption("bundle_panel_enabled", "收纳袋面板",
                this::isBundlePanelEnabled, this::setBundlePanelEnabled, true));
        addOption(new BooleanOption("shulker_box_panel_enabled", "潜影盒面板",
                this::isShulkerBoxPanelEnabled, this::setShulkerBoxPanelEnabled, true));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("在容器界面显示收纳袋/潜影盒通用面板")
                .keyValueLine("收纳袋面板", bundlePanelEnabled ? "启用" : "禁用")
                .keyValueLine("潜影盒面板", shulkerBoxPanelEnabled ? "启用" : "禁用")
                .toString();
    }

    @Override
    protected void onTrigger(Minecraft client) {
    }

    @Override
    protected void onDisable() {
        ContainerPanel.hide();
    }
}
