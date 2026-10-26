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

    @Getter
    @Setter
    private boolean enableContainerPreview;

    @Getter
    @Setter
    private boolean highlightContainerContainsThisItem;

    @Getter
    @Setter
    private boolean isShiftAndLeftClickSwiftToBundle;

    @Getter
    @Setter
    private boolean enableMiddleClickViewContainer;

    private FasterContainerPanelFunction() {
        super("faster_container_panel", "快捷容器面板");
        addOption(new BooleanOption("bundle_panel_enabled", "收纳袋面板",
                this::isBundlePanelEnabled, this::setBundlePanelEnabled, true));
        addOption(new BooleanOption("shulker_box_panel_enabled", "潜影盒面板",
                this::isShulkerBoxPanelEnabled, this::setShulkerBoxPanelEnabled, true));
        addOption(new BooleanOption("shulker_box_panel_enabled", "容器预览（Shift + 鼠标悬浮 触发）",
                this::isEnableContainerPreview, this::setEnableContainerPreview, true));
        addOption(new BooleanOption("highlight_container_contains_this_item", "高亮包含此物品的容器",
                this::isHighlightContainerContainsThisItem, this::setHighlightContainerContainsThisItem, true));
        addOption(new BooleanOption("enable_middle_click_preview_container_when_mainhand", "主手持容器中键预览",
                this::isEnableMiddleClickViewContainer, this::setEnableMiddleClickViewContainer, true));
        addOption(new BooleanOption("is_shift_and_left_click_swift_to_bundle", "背包界面Shift + 左键物品优先进入收纳袋",
                this::isShiftAndLeftClickSwiftToBundle, this::setShiftAndLeftClickSwiftToBundle, true));

    }

    @Override
    protected String tooltip() {
        return Tooltip.create()
                .line("在容器界面显示收纳袋/潜影盒通用面板")
                .keyValueLine("收纳袋面板", bundlePanelEnabled ? "启用" : "禁用")
                .keyValueLine("潜影盒面板", shulkerBoxPanelEnabled ? "启用" : "禁用")
                .line("  对于潜影盒，原版客户端不支持存取功能，因此潜影盒面板只能用于预览")
                .keyValueLine("容器预览", enableContainerPreview ? "启用" : "禁用")
                .keyValueLine("高亮包含此物品的容器", highlightContainerContainsThisItem ? "启用" : "禁用")
                .keyValueLine("主手持容器中键预览", enableMiddleClickViewContainer ? "启用" : "禁用")
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
