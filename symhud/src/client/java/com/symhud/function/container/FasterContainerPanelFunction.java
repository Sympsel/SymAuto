package com.symhud.function.container;

import com.symbase.config.option.BooleanOption;
import com.symbase.function.base.SymAbstractFunction;
import com.symbase.function.util.Tooltip;
import com.symhud.gui.container.ContainerPanel;
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

    @Getter
    @Setter
    private boolean highlightContainerContainsThisItem;

    @Getter
    @Setter
    private boolean isShiftAndLeftClickSwiftToBundle;

    private FasterContainerPanelFunction() {
        super("faster_container_panel", "快捷容器面板");
        addOption(new BooleanOption("bundle_panel_enabled", "收纳袋面板",
                "在容器界面显示收纳袋通用面板",
                this::isBundlePanelEnabled, this::setBundlePanelEnabled, true));
        addOption(new BooleanOption("shulker_box_panel_enabled", "潜影盒面板",
                "在容器界面显示潜影盒通用面板；原版客户端不支持存取功能，因此只能用于预览",
                this::isShulkerBoxPanelEnabled, this::setShulkerBoxPanelEnabled, true));
        addOption(new BooleanOption("highlight_container_contains_this_item", "高亮包含此物品的容器",
                "在容器界面高亮包含面板所选物品的容器",
                this::isHighlightContainerContainsThisItem, this::setHighlightContainerContainsThisItem, true));
        addOption(new BooleanOption("is_shift_and_left_click_swift_to_bundle", "Shift + 左键物品优先进入收纳袋",
                "背包界面 Shift + 左键物品优先进入收纳袋",
                this::isShiftAndLeftClickSwiftToBundle, this::setShiftAndLeftClickSwiftToBundle, true));
    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("在容器界面显示收纳袋/潜影盒通用面板")
                .keyValueLine("收纳袋面板", bundlePanelEnabled ? "启用" : "禁用")
                .keyValueLine("潜影盒面板", shulkerBoxPanelEnabled ? "启用" : "禁用")
                .keyValueLine("高亮包含此物品的容器", highlightContainerContainsThisItem ? "启用" : "禁用")
                .keyValueLine("背包界面Shift + 左键物品优先进入收纳袋", isShiftAndLeftClickSwiftToBundle ? "启用" : "禁用")
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


