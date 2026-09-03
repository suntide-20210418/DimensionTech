package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerGeometry;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Compact mining console with centered marker lanes and live machine telemetry. */
public final class MythicMinerScreen extends AbstractContainerScreen<MythicMinerMenu>
        implements MythicMinerScreenContext {
    enum Page {
        WORK,
        INFO,
        ATTRIBUTES
    }

    private static final int WIDTH = 320;
    private static final int PANEL = MythicMinerTheme.FRAME;
    static final int PANEL_INSET = MythicMinerTheme.INSET;
    static final int PANEL_RAISED = MythicMinerTheme.PANEL;
    static final int RULE = MythicMinerTheme.EDGE;
    static final int TEXT = MythicMinerTheme.TEXT;
    static final int MUTED = MythicMinerTheme.MUTED;
    static final int CYAN = MythicMinerTheme.FLUIX;
    static final int CYAN_DARK = MythicMinerTheme.INSET;
    static final int AMBER = MythicMinerTheme.AMBER;
    static final int GREEN = MythicMinerTheme.SUCCESS;
    static final int RED = MythicMinerTheme.ERROR;
    private static final int SLOT_FACE = MythicMinerTheme.INSET;
    private static final int ENERGY_X = 5;
    private static final int ENERGY_TOP = 46;
    private static final int ENERGY_HEIGHT = 164;
    private static final int LEFT_CONTROLS_WIDTH = 30;
    private static final int SCREEN_MARGIN = 8;
    private static final int MARKER_INFO_PADDING = 5;
    static final int MARKER_INFO_ROW_HEIGHT = 20;
    private static final int MARKER_INFO_EXPECTED_Y = 180;
    private static final int PARALLEL_BREAKDOWN_HEIGHT = 36;
    static final int INFO_PANEL_Y = 52;
    static final int INFO_SLOT_Y = INFO_PANEL_Y + 22;
    static final int INFO_VIEWPORT_Y = INFO_PANEL_Y + 58;
    private float uiScale = 1.0F;
    private final MythicMinerGeometry geometry;
    private final MythicMinerUiState uiState = new MythicMinerUiState();
    private int markerInfoContentHeight;
    private int attributeContentHeight;
    private ItemStack hoveredExpectedItem = ItemStack.EMPTY;
    private boolean hoveredExpectedItemDisabled;
    private List<ExpectedItemRow> expectedItemRows = List.of();
    private Set<ResourceLocation> disabledExpectedItems = Set.of();

    @Override
    public MythicMinerMenu menu() {
        return menu;
    }

    @Override
    public net.minecraft.client.gui.Font font() {
        return font;
    }

    @Override
    public int imageWidth() {
        return imageWidth;
    }

    @Override
    public int leftPos() {
        return leftPos;
    }

    @Override
    public int topPos() {
        return topPos;
    }

    @Override
    public float uiScale() {
        return uiScale;
    }

    @Override
    public void openOutputFaceScreen() {
        Minecraft.getInstance().setScreen(new OutputFaceScreen(this, menu));
    }

    @Override
    public int playerInventoryY() {
        return menu.getPlayerInventoryY();
    }

    @Override
    public int attributeScroll() {
        return uiState.attributeScroll;
    }

    @Override
    public void attributeScroll(int value) {
        uiState.attributeScroll = value;
    }

    @Override
    public void attributeContentHeight(int value) {
        attributeContentHeight = value;
    }

    @Override
    public int attributeContentHeight() {
        return attributeContentHeight;
    }

    @Override
    public boolean upgradeRowExpanded(int key) {
        return uiState.expandedUpgradeRows.contains(key);
    }

    @Override
    public void toggleUpgradeRow(int key) {
        if (!uiState.expandedUpgradeRows.add(key)) uiState.expandedUpgradeRows.remove(key);
    }

    @Override
    public int selectedMarkerSlot() {
        return uiState.selectedMarkerSlot;
    }

    @Override
    public int markerInfoScroll() {
        return uiState.markerInfoScroll;
    }

    @Override
    public void markerInfoScroll(int value) {
        uiState.markerInfoScroll = value;
    }

    @Override
    public void markerInfoContentHeight(int value) {
        markerInfoContentHeight = value;
    }

    @Override
    public int markerInfoContentHeight() {
        return markerInfoContentHeight;
    }

    @Override
    public boolean markerPropertiesExpanded() {
        return uiState.markerPropertiesExpanded;
    }

    @Override
    public boolean workStatusExpanded() {
        return uiState.workStatusExpanded;
    }

    @Override
    public boolean productInfoExpanded() {
        return uiState.productInfoExpanded;
    }

    @Override
    public boolean showParallelBreakdown() {
        return uiState.showParallelBreakdown;
    }

    @Override
    public void toggleMarkerProperties() {
        uiState.markerPropertiesExpanded = !uiState.markerPropertiesExpanded;
    }

    @Override
    public void toggleWorkStatus() {
        uiState.workStatusExpanded = !uiState.workStatusExpanded;
    }

    @Override
    public void toggleProductInfo() {
        uiState.productInfoExpanded = !uiState.productInfoExpanded;
    }

    @Override
    public void toggleParallelBreakdown() {
        uiState.showParallelBreakdown = !uiState.showParallelBreakdown;
    }

    @Override
    public List<ExpectedItemRow> expectedItemRows() {
        return expectedItemRows;
    }

    @Override
    public Set<ResourceLocation> disabledExpectedItems() {
        return disabledExpectedItems;
    }

    @Override
    public double effectiveDimensionValue() {
        return uiState.analysis.dimensionValue();
    }

    @Override
    public double effectiveStructureValue() {
        return uiState.analysis.structureValue();
    }

    @Override
    public void resetExpectedHover() {
        hoveredExpectedItem = ItemStack.EMPTY;
        hoveredExpectedItemDisabled = false;
    }

    @Override
    public void setExpectedHover(ItemStack stack, boolean disabled) {
        hoveredExpectedItem = stack;
        hoveredExpectedItemDisabled = disabled;
    }

    private static final int TAB_Y = 31;
    private static final int TAB_HEIGHT = 13;

    public MythicMinerScreen(MythicMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        geometry =
                MythicMinerGeometry.forMenu(
                        menu.getContainerSlotCount(), menu.hasFluidInput(), menu.getMenuWidth());
        imageWidth = menu.getMenuWidth();
        imageHeight = geometry.frame().height();
        inventoryLabelY = menu.getPlayerInventoryY() - 13;
    }

    @Override
    protected void init() {
        super.init();
        float widthScale =
                (float) Math.max(1, width - SCREEN_MARGIN * 2) / (imageWidth + LEFT_CONTROLS_WIDTH);
        float heightScale = (float) Math.max(1, height - SCREEN_MARGIN * 2) / imageHeight;
        uiScale = Math.min(1.0F, Math.min(widthScale, heightScale));
        int logicalWidth = (int) Math.floor(width / uiScale);
        int logicalHeight = (int) Math.floor(height / uiScale);
        leftPos = (logicalWidth - imageWidth + LEFT_CONTROLS_WIDTH) / 2;
        topPos = (logicalHeight - imageHeight) / 2;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (uiState.page == Page.INFO
                && uiState.selectedMarkerSlot >= 0
                && uiState.analysisSlot == uiState.selectedMarkerSlot
                && uiState.analysis.equipmentDismantling()
                        != menu.isEquipmentDismantlingEnabled()) {
            clearEffectiveAnalysis();
            ModNetwork.requestMythicMinerAnalysis(menu.containerId, uiState.selectedMarkerSlot);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int logicalMouseX = toLogical(mouseX);
        int logicalMouseY = toLogical(mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        if (MythicMinerWorkPage.isWorkPage(uiState.page)) {
            super.render(graphics, logicalMouseX, logicalMouseY, partialTick);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos, topPos, 0.0D);
            drawPanel(graphics, 0, 0, imageWidth, imageHeight);
            drawHeader(graphics);
            drawEnergyRail(graphics);
            drawPage(graphics, logicalMouseX - leftPos, logicalMouseY - topPos);
            graphics.pose().popPose();
        }
        if (uiState.page == Page.WORK) {
            MythicMinerWorkPage.renderControls(this, graphics, logicalMouseX, logicalMouseY);
        }
        if (!menu.telemetrySnapshot().structureComplete()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.structure_incomplete"),
                    leftPos + imageWidth / 2,
                    topPos + 46,
                    RED);
        }
        graphics.pose().popPose();

        // AbstractContainerScreen tracks hovered slots while rendering under the
        // logical UI scale. Re-render the item tooltip in screen coordinates so
        // inventory hover information remains visible at every scale.
        renderInventoryItemTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);

        if (uiState.page == Page.WORK) {
            MythicMinerWorkPage.renderTooltip(
                    this, graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
            renderFluidTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
        }
        if (logicalMouseX >= leftPos + ENERGY_X
                && logicalMouseX <= leftPos + ENERGY_X + 14
                && logicalMouseY >= topPos + ENERGY_TOP
                && logicalMouseY <= topPos + ENERGY_TOP + ENERGY_HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(
                    Component.translatable("screen.dimension_tech.mythic_miner.energy_tooltip"));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.energy_value",
                            menu.getEnergyStored(),
                            menu.getEnergyCapacity()));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.energy_consumption",
                            menu.getEffectiveEnergyConsumption()));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
        if (uiState.page == Page.ATTRIBUTES) {
            MythicMinerAttributesPage.renderTooltip(
                    this, graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
        }
        if (!hoveredExpectedItem.isEmpty()) {
            graphics.renderTooltip(
                    font,
                    List.of(
                            hoveredExpectedItem.getHoverName(),
                            Component.translatable(
                                    hoveredExpectedItemDisabled
                                            ? "screen.dimension_tech.mythic_miner.expected_item.enable"
                                            : "screen.dimension_tech.mythic_miner.expected_item.disable")),
                    Optional.empty(),
                    mouseX,
                    mouseY);
        }
    }

    private void renderInventoryItemTooltip(
            GuiGraphics graphics, int logicalMouseX, int logicalMouseY, int screenX, int screenY) {
        if (uiState.page != Page.WORK) return;
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!slot.hasItem()
                    || !inside(
                            logicalMouseX,
                            logicalMouseY,
                            leftPos + slot.x,
                            topPos + slot.y,
                            16,
                            16)) continue;
            graphics.renderTooltip(font, slot.getItem(), screenX, screenY);
            return;
        }
    }

    private int toLogical(double coordinate) {
        return (int) Math.floor(coordinate / uiScale);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (button == 0 && selectPageAt(mouseX, mouseY)) {
            return true;
        }
        if (button == 0) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            if (uiState.page == Page.INFO) {
                return MythicMinerInfoPage.mouseClicked(this, x, y, button);
            }
            if (uiState.page == Page.ATTRIBUTES) {
                return MythicMinerAttributesPage.mouseClicked(this, x, y, button);
            }
            if (uiState.page == Page.WORK && mouseClickedFluidControls(x, y)) return true;
            if (MythicMinerWorkPage.mouseClicked(this, x, y, button)) return true;
        }
        if (uiState.page != Page.WORK) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (uiState.page != Page.WORK) return true;
        return super.mouseReleased(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (uiState.page != Page.WORK && keyCode != 256) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (uiState.page != Page.WORK) return true;
        return super.mouseDragged(
                mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double logicalMouseX = mouseX / uiScale;
        double logicalMouseY = mouseY / uiScale;
        if (uiState.page == Page.ATTRIBUTES) {
            return MythicMinerAttributesPage.mouseScrolled(
                    this, logicalMouseX, logicalMouseY, delta);
        }
        if (uiState.page == Page.INFO) {
            return MythicMinerInfoPage.mouseScrolled(this, logicalMouseX, logicalMouseY, delta);
        }
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0D);
        drawPanel(graphics, 0, 0, imageWidth, imageHeight);
        drawHeader(graphics);
        drawEnergyRail(graphics);
        drawPage(graphics, mouseX - leftPos, mouseY - topPos);
        if (uiState.page == Page.WORK) MythicMinerWorkPage.drawInventoryChrome(this, graphics);
        if (uiState.page == Page.WORK && menu.hasFluidInput()) drawFluidModule(graphics);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (uiState.page == Page.WORK) {
            graphics.drawCenteredString(
                    font, playerInventoryTitle, imageWidth / 2, inventoryLabelY, MUTED);
        }
    }

    private void drawPage(GuiGraphics graphics, int mouseX, int mouseY) {
        if (uiState.page == Page.WORK) {
            MythicMinerWorkPage.render(this, graphics);
            return;
        }
        if (MythicMinerInfoPage.isInfoPage(uiState.page)) {
            MythicMinerInfoPage.render(this, graphics, mouseX, mouseY);
        } else {
            MythicMinerAttributesPage.render(this, graphics);
        }
    }

    private boolean selectPageAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (y < TAB_Y || y >= TAB_Y + TAB_HEIGHT) return false;
        int tabWidth = imageWidth / 3;
        if (x < 0 || x >= imageWidth) return false;
        uiState.page = x < tabWidth ? Page.WORK : x < tabWidth * 2 ? Page.INFO : Page.ATTRIBUTES;
        uiState.markerInfoScroll = 0;
        MythicMinerAttributesPage.resetScroll(this);
        return true;
    }

    @Override
    public void selectMarkerSlot(int slot) {
        if (slot < 0 || slot >= menu.getContainerSlotCount()) return;
        if (uiState.selectedMarkerSlot != slot) {
            uiState.markerInfoScroll = 0;
            clearEffectiveAnalysis();
        }
        uiState.selectedMarkerSlot = slot;
        if (StructMarkerItem.getMarkerInfo(menu.slots.get(slot).getItem()).isPresent()) {
            ModNetwork.requestMythicMinerAnalysis(menu.containerId, slot);
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        MythicMinerTheme.panel(graphics, x, y, width, height, AMBER);
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.drawString(font, title, 8, 18, TEXT, false);
        int tabWidth = imageWidth / 3;
        String[] labels = {
            "screen.dimension_tech.mythic_miner.tab.work",
            "screen.dimension_tech.mythic_miner.tab.info",
            "screen.dimension_tech.mythic_miner.tab.attributes"
        };
        Page[] pages = Page.values();
        for (int index = 0; index < pages.length; index++) {
            int x = index * tabWidth;
            boolean selected = uiState.page == pages[index];
            MythicMinerTheme.tab(
                    graphics,
                    font,
                    x + 2,
                    TAB_Y,
                    tabWidth - 4,
                    Component.translatable(labels[index]),
                    selected);
        }
    }

    private void drawFluidModule(GuiGraphics graphics) {
        MythicMinerGeometry.Rect panel = geometry.fluidPanel();
        int x = panel.x();
        int y = panel.y();
        int width = panel.width();
        int height = panel.height();
        MythicMinerTheme.panel(graphics, x, y, width, height, CYAN_DARK);
        Component fluidTitle =
                Component.translatable("screen.dimension_tech.mythic_miner.fluid_input");
        graphics.drawCenteredString(
                font,
                font.plainSubstrByWidth(fluidTitle.getString(), Math.max(1, width - 4)),
                x + width / 2,
                y + 7,
                MUTED);

        int tankX = x + (width - MythicMinerLayout.FLUID_TANK_WIDTH) / 2;
        int tankY = y + MythicMinerLayout.FLUID_TANK_OFFSET_Y;
        int tankH = MythicMinerLayout.FLUID_TANK_HEIGHT;
        graphics.fill(tankX - 2, tankY - 2, tankX + 20, tankY + tankH + 2, RULE);
        graphics.fill(tankX, tankY, tankX + 18, tankY + tankH, SLOT_FACE);
        graphics.fill(tankX, tankY, tankX + 18, tankY + 1, MythicMinerTheme.SLOT_HIGHLIGHT);
        graphics.fill(tankX, tankY + tankH - 1, tankX + 18, tankY + tankH, RULE);
        int capacity = Math.max(1, menu.getFluidCapacity());
        int amount = Math.max(0, Math.min(capacity, menu.getFluidAmount()));
        int filled = tankH * amount / capacity;
        net.minecraft.world.level.material.Fluid displayedFluid =
                menu.hasFluid() ? menu.getFluid() : menu.getRequiredFluid();
        int color =
                displayedFluid == null || displayedFluid == Fluids.EMPTY
                        ? MythicMinerTheme.FLUID
                        : IClientFluidTypeExtensions.of(displayedFluid).getTintColor();
        if (!menu.hasFluid()) color = (color & 0x00FFFFFF) | 0x66000000;
        graphics.fill(tankX + 2, tankY + tankH - 2 - filled, tankX + 16, tankY + tankH - 2, color);
        graphics.fill(
                tankX + 2,
                tankY + tankH - 2 - filled,
                tankX + 16,
                tankY + tankH - 1 - filled,
                MythicMinerTheme.SLOT_HIGHLIGHT);
        graphics.drawCenteredString(
                font,
                Component.literal(amount + "/" + capacity + " mB"),
                x + width / 2,
                y + height - 14,
                TEXT);
        int controlsY = y + MythicMinerLayout.FLUID_CONTROLS_OFFSET_Y;
        drawFluidButton(graphics, x + 5, controlsY, 16, "F", CYAN);
        drawFluidButton(
                graphics,
                x + 27,
                controlsY,
                16,
                "A",
                menu.isAutoExtractFluidEnabled() ? CYAN : RULE);
    }

    private void drawFluidButton(
            GuiGraphics graphics, int x, int y, int size, String label, int accent) {
        MythicMinerTheme.button(
                graphics, font, x, y, size, size, Component.literal(label), false, true, accent);
    }

    private boolean mouseClickedFluidControls(double x, double y) {
        if (!menu.hasFluidInput()) return false;
        MythicMinerGeometry.Rect panel = geometry.fluidPanel();
        int panelX = panel.x();
        int buttonY = panel.y() + MythicMinerLayout.FLUID_CONTROLS_OFFSET_Y;
        if (inside(x, y, panelX + 5, buttonY, 16, 16)) {
            Minecraft.getInstance()
                    .setScreen(new OutputFaceScreen(this, menu, OutputFaceScreen.Mode.FLUID));
            return true;
        }
        if (inside(x, y, panelX + 27, buttonY, 16, 16)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 26);
            return true;
        }
        return false;
    }

    private void renderFluidTooltip(
            GuiGraphics graphics, int logicalX, int logicalY, int screenX, int screenY) {
        if (!menu.hasFluidInput()) return;
        MythicMinerGeometry.Rect panel = geometry.fluidPanel();
        int localX = logicalX - leftPos;
        int localY = logicalY - topPos;
        if (!panel.contains(localX, localY)) return;
        int buttonY = panel.y() + MythicMinerLayout.FLUID_CONTROLS_OFFSET_Y;
        net.minecraft.world.level.material.Fluid displayedFluid =
                menu.hasFluid() ? menu.getFluid() : menu.getRequiredFluid();
        Component fluidName =
                displayedFluid != null && displayedFluid != Fluids.EMPTY
                        ? Component.translatable(displayedFluid.getFluidType().getDescriptionId())
                        : Component.translatable("screen.dimension_tech.mythic_miner.fluid_empty");
        if (inside(localX, localY, panel.x() + 5, buttonY, 16, 16)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("screen.dimension_tech.mythic_miner.fluid_faces"),
                    screenX,
                    screenY);
            return;
        }
        if (inside(localX, localY, panel.x() + 27, buttonY, 16, 16)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.isAutoExtractFluidEnabled()
                                    ? "screen.dimension_tech.mythic_miner.auto_extract_enabled"
                                    : "screen.dimension_tech.mythic_miner.auto_extract_disabled"),
                    screenX,
                    screenY);
            return;
        }
        graphics.renderTooltip(
                font,
                List.of(
                        Component.translatable("screen.dimension_tech.mythic_miner.fluid_input"),
                        fluidName,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.fluid_amount",
                                menu.getFluidAmount(),
                                menu.getFluidCapacity()),
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.fluid_required",
                                BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB),
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.fluid_required_type"),
                        Component.translatable(
                                menu.getFluidAmount()
                                                >= BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB
                                        ? "screen.dimension_tech.mythic_miner.fluid_status"
                                        : "screen.dimension_tech.mythic_miner.fluid_insufficient")),
                Optional.empty(),
                screenX,
                screenY);
    }

    private void clearEffectiveAnalysis() {
        uiState.clearAnalysis();
        expectedItemRows = List.of();
        disabledExpectedItems = Set.of();
    }

    public static void receiveAnalysis(
            int containerId,
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        if (!(Minecraft.getInstance().screen instanceof MythicMinerScreen screen)
                || screen.menu.containerId != containerId
                || screen.uiState.selectedMarkerSlot != slot) {
            return;
        }
        screen.applyAnalysis(
                slot,
                dimensionValue,
                structureValue,
                equipmentDismantling,
                itemExpectations,
                disabledItems);
    }

    private void applyAnalysis(
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        List<ExpectedItemRow> refreshed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Double> entry : itemExpectations.entrySet()) {
            BuiltInRegistries.ITEM
                    .getOptional(entry.getKey())
                    .ifPresent(item -> refreshed.add(new ExpectedItemRow(item, entry.getValue())));
        }
        refreshed.sort(
                Comparator.comparingDouble(ExpectedItemRow::expected)
                        .reversed()
                        .thenComparing(
                                row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        expectedItemRows = List.copyOf(refreshed);
        disabledExpectedItems = Set.copyOf(disabledItems);
        uiState.analysis =
                new com.suntide_20210418.dimensiontech.block.entity.MythicMinerAnalysisSnapshot(
                        dimensionValue,
                        structureValue,
                        equipmentDismantling,
                        itemExpectations,
                        disabledItems);
        uiState.analysisSlot = slot;
    }

    static String formatAnalysisValue(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    static String formatExpectedValue(double value) {
        if (value > 0.0D && value < 0.0001D) {
            return String.format(java.util.Locale.ROOT, "%.2e", value);
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private void drawEnergyRail(GuiGraphics graphics) {
        int x = ENERGY_X;
        int y = ENERGY_TOP;
        int energy = menu.getEnergyStored();
        int capacity = Math.max(1, menu.getEnergyCapacity());
        int filled = fillPixels(energy, capacity, ENERGY_HEIGHT);
        int stripeX = x + 2;
        int stripeY = y + 2;
        int stripeWidth = 10;
        int stripeHeight = ENERGY_HEIGHT - 4;
        int filledHeight = Math.min(stripeHeight, filled * stripeHeight / ENERGY_HEIGHT);

        // Match AE2's striped vertical meter: draw the grey base first, then the purple fill.
        graphics.fill(x - 1, y - 1, x + stripeWidth + 3, y + ENERGY_HEIGHT + 1, RULE);
        graphics.fill(x, y, x + stripeWidth + 2, y + ENERGY_HEIGHT, MythicMinerTheme.ENERGY_BORDER);
        for (int row = 0; row < stripeHeight; row++) {
            boolean brightRow = (row & 1) == 0;
            int rowY = stripeY + row;
            graphics.fill(stripeX, rowY, stripeX + 1, rowY + 1, MythicMinerTheme.ENERGY_BORDER);
            graphics.fill(
                    stripeX + 1,
                    rowY,
                    stripeX + stripeWidth - 1,
                    rowY + 1,
                    brightRow
                            ? MythicMinerTheme.ENERGY_BASE_LIGHT
                            : MythicMinerTheme.ENERGY_BASE_DARK);
            graphics.fill(
                    stripeX + stripeWidth - 1,
                    rowY,
                    stripeX + stripeWidth,
                    rowY + 1,
                    MythicMinerTheme.ENERGY_BORDER);
        }
        for (int row = Math.max(0, stripeHeight - filledHeight); row < stripeHeight; row++) {
            boolean brightRow = (row & 1) == 0;
            int rowY = stripeY + row;
            graphics.fill(
                    stripeX + 1,
                    rowY,
                    stripeX + stripeWidth - 1,
                    rowY + 1,
                    brightRow
                            ? MythicMinerTheme.ENERGY_FILL_LIGHT
                            : MythicMinerTheme.ENERGY_FILL_DARK);
            graphics.fill(
                    stripeX + 2,
                    rowY,
                    stripeX + 3,
                    rowY + 1,
                    brightRow
                            ? MythicMinerTheme.ENERGY_FILL_BRIGHT
                            : MythicMinerTheme.ENERGY_FILL_MID);
            graphics.fill(
                    stripeX + 4,
                    rowY,
                    stripeX + 5,
                    rowY + 1,
                    brightRow
                            ? MythicMinerTheme.ENERGY_FILL_HOT
                            : MythicMinerTheme.ENERGY_FILL_MID);
            graphics.fill(
                    stripeX + 6,
                    rowY,
                    stripeX + 7,
                    rowY + 1,
                    brightRow
                            ? MythicMinerTheme.ENERGY_FILL_LIGHT
                            : MythicMinerTheme.ENERGY_FILL_DARK);
        }
        drawEnergyIcon(graphics, x + 3, y + ENERGY_HEIGHT + 6);
    }

    /** Small pixel lightning mark used in place of a text-only energy label. */
    private void drawEnergyIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 3, y, x + 6, y + 3, CYAN);
        graphics.fill(x + 2, y + 3, x + 5, y + 6, CYAN);
        graphics.fill(x + 1, y + 6, x + 4, y + 9, CYAN);
        graphics.fill(x + 4, y + 3, x + 7, y + 5, CYAN);
        graphics.fill(x + 3, y + 6, x + 6, y + 8, CYAN);
    }

    static String formatDecimal(int hundredths) {
        return String.format(java.util.Locale.ROOT, "%.2f", hundredths / 100.0D);
    }

    static String formatRatio(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static int fillPixels(int amount, int capacity, int pixels) {
        if (amount <= 0 || capacity <= 0 || pixels <= 0) return 0;
        float fillRatio = (float) amount / (float) capacity;
        fillRatio = Math.max(0.0F, Math.min(1.0F, fillRatio));
        return Math.min(pixels, (int) (pixels * fillRatio));
    }

    record ExpectedItemRow(Item item, double expected) {}
}
