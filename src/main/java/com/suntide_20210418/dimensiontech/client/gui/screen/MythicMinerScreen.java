package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact mining console with centered marker lanes and live machine telemetry. */
public final class MythicMinerScreen extends AbstractContainerScreen<MythicMinerMenu> {
    private enum Page { WORK, INFO, ATTRIBUTES }

    private static final int WIDTH = 320;
    private static final int PANEL = 0xFF11181D;
    private static final int PANEL_INSET = 0xFF0B1115;
    private static final int PANEL_RAISED = 0xFF1B282F;
    private static final int RULE = 0xFF2B3941;
    private static final int TEXT = 0xFFE7EEF0;
    private static final int MUTED = 0xFF8EA2A9;
    private static final int CYAN = 0xFF4DD6D0;
    private static final int CYAN_DARK = 0xFF1D7778;
    private static final int AMBER = 0xFFF0B45C;
    private static final int GREEN = 0xFF7BD88F;
    private static final int RED = 0xFFE45B51;
    private static final int SLOT_FACE = 0xFF33434A;
    private static final int ENERGY_X = 5;
    private static final int ENERGY_TOP = 46;
    private static final int ENERGY_HEIGHT = 164;
    private static final int LEFT_CONTROLS_WIDTH = 30;
    private static final int SCREEN_MARGIN = 8;
    private static final int MARKER_INFO_PADDING = 5;
    private static final int MARKER_INFO_ROW_HEIGHT = 20;
    private static final int MARKER_INFO_EXPECTED_Y = 180;
    private static final int PARALLEL_BREAKDOWN_HEIGHT = 36;
    private static final int INFO_PANEL_Y = 52;
    private static final int INFO_SLOT_Y = INFO_PANEL_Y + 22;
    private static final int INFO_VIEWPORT_Y = INFO_PANEL_Y + 58;
    private float uiScale = 1.0F;
    private int selectedMarkerSlot = -1;
    private int markerInfoScroll;
    private int markerInfoContentHeight;
    private int attributeScroll;
    private int attributeContentHeight;
    private final Set<Integer> expandedUpgradeRows = new HashSet<>();
    private int receivedAnalysisSlot = -1;
    private boolean receivedAnalysisDismantling;
    private double effectiveDimensionValue;
    private double effectiveStructureValue;
    private ItemStack hoveredExpectedItem = ItemStack.EMPTY;
    private List<ExpectedItemRow> expectedItemRows = List.of();
    private Set<ResourceLocation> disabledExpectedItems = Set.of();
    private boolean showParallelBreakdown;
    private Page page = Page.WORK;
    private static final int TAB_Y = 33;
    private static final int TAB_HEIGHT = 12;

    public MythicMinerScreen(MythicMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = WIDTH;
        imageHeight = menu.getPlayerInventoryY() + 82;
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
        if (page == Page.INFO && selectedMarkerSlot >= 0
                && receivedAnalysisSlot == selectedMarkerSlot
                && receivedAnalysisDismantling != menu.isEquipmentDismantlingEnabled()) {
            clearEffectiveAnalysis();
            ModNetwork.requestMythicMinerAnalysis(menu.containerId, selectedMarkerSlot);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int logicalMouseX = toLogical(mouseX);
        int logicalMouseY = toLogical(mouseY);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        if (page == Page.WORK) {
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
        if (page == Page.WORK) {
            drawControlButtons(graphics, logicalMouseX, logicalMouseY);
        }
        if (menu.getTelemetry(14) == 0) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.structure_incomplete"),
                    leftPos + imageWidth / 2,
                    topPos + 46,
                    0xFFE45B51);
        }
        graphics.pose().popPose();

        renderTooltip(graphics, mouseX, mouseY);
        int control = -1;
        for (int index = 0; page == Page.WORK && index < Math.min(3, controlCount()); index++) {
            int buttonY = topPos + 48 + index * 29;
            if (logicalMouseX >= leftPos - 29
                    && logicalMouseX < leftPos - 5
                    && logicalMouseY >= buttonY
                    && logicalMouseY < buttonY + 24) {
                control = index;
                break;
            }
        }
        if (page == Page.WORK && control < 0) {
            int centerY = topPos + 96 + 58 + 5;
            int firstX = leftPos + imageWidth / 2 - 88;
            if (logicalMouseY >= centerY && logicalMouseY < centerY + 24) {
                if (logicalMouseX >= firstX && logicalMouseX < firstX + 88) control = 3;
                else if (menu.supportsEquipmentDismantling()
                        && logicalMouseX >= firstX + 94 && logicalMouseX < firstX + 182) control = 4;
            }
        }
        if (control >= 0) {
            graphics.renderTooltip(
                    font, controlTooltip(control), Optional.empty(), mouseX, mouseY);
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
        if (page == Page.ATTRIBUTES) {
            renderAttributeTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
        }
        if (page == Page.INFO) {
            renderInfoTooltip(graphics, logicalMouseX, logicalMouseY, mouseX, mouseY);
        }
        if (page == Page.INFO && selectedMarkerSlot >= 0
                && insideMarkerSlotToggle(
                        logicalMouseX - leftPos, logicalMouseY - topPos)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.isMarkerSlotEnabled(selectedMarkerSlot)
                                    ? "screen.dimension_tech.mythic_miner.slot.disable"
                                    : "screen.dimension_tech.mythic_miner.slot.enable"),
                    mouseX,
                    mouseY);
        }
        for (int index = 0; page == Page.WORK && index < menu.getContainerSlotCount(); index++) {
            Slot slot = menu.slots.get(index);
            int barX = leftPos + MythicMinerLayout.progressBarX(slot.x);
            int barY = topPos + MythicMinerLayout.progressBarY(slot.y);
            if (logicalMouseX >= barX
                    && logicalMouseX < barX + MythicMinerLayout.PROGRESS_WIDTH
                    && logicalMouseY >= barY
                    && logicalMouseY < barY + MythicMinerLayout.PROGRESS_HEIGHT) {
                boolean configuredMarker =
                        StructMarkerItem.getMarkerInfo(slot.getItem()).isPresent();
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_progress",
                                configuredMarker ? menu.getMarkerProgress(index) : 0,
                                configuredMarker ? menu.getMarkerProcessingTime(index) : 0));
                if (configuredMarker && menu.isMarkerWaitingForNaturalWindow(index)) {
                    tooltip.add(Component.translatable(
                            "screen.dimension_tech.mythic_miner.waiting_for_natural_window"));
                }
                tooltip.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                                configuredMarker ? menu.getMarkerCurrentNaturalTicks(index) : 0));
                if (configuredMarker && menu.isMarkerExternalAccelerationActive(index)) {
                    tooltip.add(
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                                    menu.getMarkerCurrentExternalAccelerationMachineTicks(index)));
                }
                graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
                break;
            }
        }
        if (!hoveredExpectedItem.isEmpty()) {
            graphics.renderTooltip(font, hoveredExpectedItem, mouseX, mouseY);
        }
    }

    private int toLogical(double coordinate) {
        return (int) Math.floor(coordinate / uiScale);
    }

    private List<Component> controlTooltip(int control) {
        if (control == 0) {
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.output_face"),
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output_faces_enabled",
                            Integer.bitCount(menu.getTelemetry(13))));
        }
        if (control == 1) {
            BaseMinerBlockEntity.OutputState state =
                    BaseMinerBlockEntity.OutputState.values()[
                            Math.max(0, Math.min(2, menu.getTelemetry(5)))];
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.output_mode"),
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output."
                                    + state.name().toLowerCase(java.util.Locale.ROOT)));
        }
        if (control == 3) {
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.place_structure"));
        }
        if (control == 4) {
            return List.of(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.equipment_dismantling"),
                    Component.translatable(
                            menu.isEquipmentDismantlingEnabled()
                                    ? "screen.dimension_tech.mythic_miner.enabled"
                                    : "screen.dimension_tech.mythic_miner.disabled"));
        }
        BaseMinerBlockEntity.RedstoneMode mode =
                BaseMinerBlockEntity.RedstoneMode.values()[
                        Math.max(0, Math.min(3, menu.getTelemetry(11)))];
        return List.of(
                Component.translatable("screen.dimension_tech.mythic_miner.redstone"),
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.redstone."
                                + mode.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private void drawControlButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = -29;
        int[] ys = {48, 77, 106, 135, 164};
        for (int i = 0; i < Math.min(3, controlCount()); i++) {
            boolean hovered =
                    mouseX >= leftPos + x
                            && mouseX < leftPos + x + 24
                            && mouseY >= topPos + ys[i]
                            && mouseY < topPos + ys[i] + 24;
            int face = hovered ? 0xFF41545B : PANEL_RAISED;
            graphics.fill(
                    leftPos + x - 1,
                    topPos + ys[i] - 1,
                    leftPos + x + 25,
                    topPos + ys[i] + 25,
                    RULE);
            graphics.fill(leftPos + x, topPos + ys[i], leftPos + x + 24, topPos + ys[i] + 24, face);
            ItemStack icon =
                    switch (i) {
                        case 0 -> new ItemStack(Items.CHEST);
                        case 1 -> outputIcon();
                        case 2 -> redstoneIcon();
                        case 3 -> new ItemStack(Items.BRICKS);
                        default -> new ItemStack(Items.ANVIL);
                    };
            graphics.renderItem(icon, leftPos + x + 4, topPos + ys[i] + 4);
            if (i == 2) drawRedstoneStateOverlay(graphics, leftPos + x + 4, topPos + ys[i] + 4);
        }
    }

    private int controlCount() {
        return menu.supportsEquipmentDismantling() ? 5 : 4;
    }

    private ItemStack redstoneIcon() {
        return switch (Math.max(0, Math.min(3, menu.getTelemetry(11)))) {
            case 0 -> new ItemStack(Items.REDSTONE_TORCH);
            case 1 -> new ItemStack(Items.REDSTONE_TORCH);
            case 2 -> new ItemStack(Items.TORCH);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    private void drawRedstoneStateOverlay(GuiGraphics graphics, int x, int y) {
        int mode = Math.max(0, Math.min(3, menu.getTelemetry(11)));
        if (mode == 0) {
            for (int offset = 0; offset < 7; offset++)
                graphics.fill(x + 1 + offset, y + 1 + offset, x + 3 + offset, y + 3 + offset, RED);
        } else if (mode == 3) {
            graphics.fill(x + 11, y + 2, x + 14, y + 5, AMBER);
            graphics.fill(x + 12, y + 5, x + 13, y + 11, AMBER);
            graphics.fill(x + 12, y + 13, x + 13, y + 14, AMBER);
        }
    }

    private ItemStack outputIcon() {
        return menu.getTelemetry(5) == BaseMinerBlockEntity.OutputState.ME_NETWORK.ordinal()
                ? new ItemStack(Items.ENDER_CHEST)
                : new ItemStack(Items.CHEST);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX /= uiScale;
        mouseY /= uiScale;
        if (button == 0 && selectPageAt(mouseX, mouseY)) {
            return true;
        }
        if (page == Page.INFO && button == 0) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            int selected = infoSlotAt(x, y);
            if (selected >= 0) {
                selectMarkerSlot(selected);
                return true;
            }
        }
        if (button == 0) {
            double x = mouseX - leftPos;
            double y = mouseY - topPos;
            InfoBounds parallelSummary = parallelSummaryBounds();
            if (page == Page.INFO && selectedMarkerSlot >= 0
                    && inside(
                            x,
                            y,
                            parallelSummary.x(),
                            parallelSummary.y(),
                            parallelSummary.width(),
                            parallelSummary.height())
                    && insideMarkerInfoViewport(x, y)) {
                showParallelBreakdown = !showParallelBreakdown;
                return true;
            }
            if (page == Page.INFO) {
                int expectedIndex = expectedItemRowAt(x, y);
                if (expectedIndex >= 0) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(
                            expectedItemRows.get(expectedIndex).item());
                    if (itemId != null) {
                        ModNetwork.toggleMythicMinerExpectedItem(
                                menu.containerId, selectedMarkerSlot, itemId);
                    }
                    return true;
                }
                if (selectedMarkerSlot >= 0 && insideMarkerSlotToggle(x, y)) {
                    ModNetwork.toggleMythicMinerSlot(menu.containerId, selectedMarkerSlot);
                    return true;
                }
                return true;
            }
            if (page == Page.ATTRIBUTES) {
                int upgradeKey = upgradeRowAt(x, y);
                if (upgradeKey >= 0) {
                    if (!expandedUpgradeRows.add(upgradeKey)) expandedUpgradeRows.remove(upgradeKey);
                    attributeScroll = 0;
                }
                return true;
            }
            for (int index = 0; index < menu.getContainerSlotCount(); index++) {
                Slot slot = menu.slots.get(index);
                int barX = MythicMinerLayout.progressBarX(slot.x);
                int barY = MythicMinerLayout.progressBarY(slot.y);
                if (inside(x, y, barX, barY, MythicMinerLayout.PROGRESS_WIDTH,
                        MythicMinerLayout.PROGRESS_HEIGHT)) {
                    ModNetwork.toggleMythicMinerSlot(menu.containerId, index);
                    return true;
                }
            }
            if (x >= -29 && x < -5 && y >= 48 && y < 72) {
                Minecraft.getInstance().setScreen(new OutputFaceScreen(this, menu));
                return true;
            }
            if (x >= -29 && x < -5 && y >= 77 && y < 101) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
                return true;
            }
            if (x >= -29 && x < -5 && y >= 106 && y < 130) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 2);
                return true;
            }
            int centerControlX = imageWidth / 2 - 88;
            int centerControlY = 96 + 58 + 5;
            if (x >= centerControlX && x < centerControlX + 88
                    && y >= centerControlY && y < centerControlY + 24) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 3);
                return true;
            }
            if (menu.supportsEquipmentDismantling()
                    && x >= centerControlX + 94
                    && x < centerControlX + 182
                    && y >= centerControlY
                    && y < centerControlY + 24) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 4);
                return true;
            }
            int expectedIndex = expectedItemRowAt(x, y);
            if (expectedIndex >= 0) {
                ResourceLocation itemId =
                        BuiltInRegistries.ITEM.getKey(expectedItemRows.get(expectedIndex).item());
                if (itemId != null) {
                    ModNetwork.toggleMythicMinerExpectedItem(
                            menu.containerId, selectedMarkerSlot, itemId);
                    return true;
                }
            }
            if (selectedMarkerSlot >= 0 && insideMarkerSlotToggle(x, y)) {
                ModNetwork.toggleMythicMinerSlot(menu.containerId, selectedMarkerSlot);
                return true;
            }
        }
        if (page != Page.WORK) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (page != Page.WORK) return true;
        return super.mouseReleased(mouseX / uiScale, mouseY / uiScale, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page != Page.WORK && keyCode != 256) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (page != Page.WORK) return true;
        return super.mouseDragged(
                mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double logicalMouseX = mouseX / uiScale;
        double logicalMouseY = mouseY / uiScale;
        if (page == Page.ATTRIBUTES) {
            int viewportY = attributeUpgradeViewportY();
            if (inside(logicalMouseX, logicalMouseY, leftPos + 28, topPos + viewportY,
                    imageWidth - 56, imageHeight - viewportY - 8)) {
                int viewportHeight = Math.max(1, imageHeight - viewportY - 8);
                int maxScroll = Math.max(0, attributeContentHeight - viewportHeight);
                attributeScroll = Math.max(0, Math.min(maxScroll,
                        attributeScroll - (int) Math.signum(delta) * MARKER_INFO_ROW_HEIGHT));
            }
            return true;
        }
        if (page != Page.INFO) return true;
        int infoX = leftPos + 36;
        int infoY = topPos + INFO_VIEWPORT_Y;
        int infoWidth = imageWidth - 72;
        int infoHeight = infoViewportHeight();
        if (inside(
                logicalMouseX,
                logicalMouseY,
                infoX,
                infoY,
                infoWidth,
                infoHeight)) {
            int viewportHeight = infoHeight;
            int maxScroll = Math.max(0, markerInfoContentHeight - viewportHeight);
            markerInfoScroll =
                    Math.max(
                            0,
                            Math.min(
                                    maxScroll,
                                    markerInfoScroll
                                            - (int) Math.signum(delta) * MARKER_INFO_ROW_HEIGHT));
            return true;
        }
        return super.mouseScrolled(logicalMouseX, logicalMouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0D);
        drawPanel(graphics, 0, 0, imageWidth, imageHeight);
        drawHeader(graphics);
        drawEnergyRail(graphics);
        drawPage(graphics, mouseX - leftPos, mouseY - topPos);
        if (page == Page.WORK) drawPlayerDivider(graphics);
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            if (page == Page.WORK) {
                drawSlotFrame(graphics, slot.x, slot.y, index < menu.getContainerSlotCount());
            }
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (page == Page.WORK) {
            graphics.drawCenteredString(
                    font, playerInventoryTitle, imageWidth / 2, inventoryLabelY, MUTED);
        }
    }

    private void drawPage(GuiGraphics graphics, int mouseX, int mouseY) {
        if (page == Page.WORK) {
            drawMarkerBay(graphics);
            drawWorkOverview(graphics);
            return;
        }
        if (page == Page.INFO) {
            drawInfoPage(graphics, mouseX, mouseY);
        } else {
            drawAttributesPage(graphics);
        }
    }

    private void drawWorkOverview(GuiGraphics graphics) {
        int x = 28;
        int width = imageWidth - 56;
        int overviewY = 96;
        int overviewHeight = 58;
        graphics.fill(x, overviewY, x + width, overviewY + overviewHeight, PANEL_RAISED);
        graphics.fill(x, overviewY, x + width, overviewY + 2, CYAN_DARK);
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.overview"),
                x + 7,
                overviewY + 8,
                MUTED,
                false);
        int metricWidth = (width - 14) / 3;
        drawOverviewMetric(graphics, x + 7, overviewY + 19, metricWidth, Items.CLOCK,
                menu.getWorkingThreadCount() + "/" + menu.getContainerSlotCount(), TEXT);
        drawOverviewMetric(graphics, x + 7 + metricWidth, overviewY + 19, metricWidth,
                Items.REDSTONE, formatDecimal(menu.getEfficiencyHundredths()), CYAN);
        drawOverviewMetric(graphics, x + 7 + metricWidth * 2, overviewY + 19, metricWidth,
                Items.RABBIT_FOOT, formatDecimal(menu.getLuckHundredths()), CYAN);
        drawOverviewMetric(graphics, x + 7, overviewY + 38, metricWidth, Items.ARROW,
                Integer.toString(menu.getBaseParallel()), CYAN);
        drawOverviewMetric(graphics, x + 7 + metricWidth, overviewY + 38, metricWidth,
                Items.NETHER_STAR, Integer.toString(menu.getTotalParallel()), CYAN);
        drawOverviewMetric(graphics, x + 7 + metricWidth * 2, overviewY + 38, metricWidth,
                Items.ANVIL, Integer.toString(menu.getTotalUpgradeCount()), TEXT);
        drawCentralWorkControls(graphics, overviewY + overviewHeight + 5);
    }

    private void drawOverviewMetric(
            GuiGraphics graphics, int x, int y, int width, Item icon, String value, int color) {
        graphics.renderItem(new ItemStack(icon), x, y - 3);
        graphics.drawString(font, font.plainSubstrByWidth(value, width - 21), x + 20, y + 1, color, false);
    }

    private void drawCentralWorkControls(GuiGraphics graphics, int y) {
        int firstX = imageWidth / 2 - 88;
        drawCenteredControl(graphics, firstX, y, 3);
        if (menu.supportsEquipmentDismantling()) {
            drawCenteredControl(graphics, firstX + 94, y, 4);
        }
    }

    private void drawCenteredControl(GuiGraphics graphics, int x, int y, int control) {
        int width = 88;
        graphics.fill(x - 1, y - 1, x + width + 1, y + 25, RULE);
        graphics.fill(x, y, x + width, y + 24, PANEL_RAISED);
        ItemStack icon = control == 3 ? new ItemStack(Items.BRICKS) : new ItemStack(Items.ANVIL);
        graphics.renderItem(icon, x + 4, y + 4);
        graphics.drawString(
                font,
                Component.translatable(control == 3
                        ? "screen.dimension_tech.mythic_miner.place_structure_short"
                        : "screen.dimension_tech.mythic_miner.equipment_dismantling"),
                x + 24,
                y + 8,
                TEXT,
                false);
        if (control == 4) {
            graphics.fill(x + 3, y + 21, x + width - 3, y + 23,
                    menu.isEquipmentDismantlingEnabled() ? CYAN : RULE);
        }
    }

    private void drawInfoPage(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 28;
        int y = INFO_PANEL_Y;
        int width = imageWidth - 56;
        int height = menu.getPlayerInventoryY() + 70 - y;
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.info.slots"), x + 8, y + 8, MUTED, false);
        int slotX = x + 8;
        for (int index = 0; index < menu.getContainerSlotCount(); index++) {
            int buttonX = slotX + index * 24;
            boolean selected = index == selectedMarkerSlot;
            graphics.fill(buttonX, INFO_SLOT_Y, buttonX + 20, INFO_SLOT_Y + 20, selected ? CYAN_DARK : RULE);
            graphics.fill(buttonX + 1, INFO_SLOT_Y + 1, buttonX + 19, INFO_SLOT_Y + 19, PANEL_INSET);
            graphics.drawCenteredString(font, Integer.toString(index + 1), buttonX + 10, INFO_SLOT_Y + 6, selected ? TEXT : MUTED);
        }
        if (selectedMarkerSlot < 0) {
            graphics.drawCenteredString(font, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.select"), x + width / 2, y + 82, MUTED);
            return;
        }
        int viewportX = x + 8;
        int viewportY = INFO_VIEWPORT_Y;
        int viewportWidth = width - 16;
        int viewportHeight = height - 66;
        boolean externalActive = menu.isMarkerExternalAccelerationActive(selectedMarkerSlot);
        boolean waiting = externalActive && menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot);
        int expectedY = markerInfoExpectedY();
        markerInfoContentHeight = expectedY + Math.max(18, expectedItemRows.size() * MARKER_INFO_ROW_HEIGHT + 4);
        int maxScroll = Math.max(0, markerInfoContentHeight - viewportHeight);
        markerInfoScroll = Math.min(markerInfoScroll, maxScroll);
        graphics.fill(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight, PANEL_INSET);
        MythicMinerLayout.ScissorBounds scissor = MythicMinerLayout.scaleToScreen(
                leftPos + viewportX, topPos + viewportY, viewportWidth, viewportHeight, uiScale);
        graphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        int contentY = viewportY - markerInfoScroll;
        int toggleX = viewportX + viewportWidth - 20;
        int toggleY = contentY + 3;
        boolean enabled = menu.isMarkerSlotEnabled(selectedMarkerSlot);
        graphics.fill(toggleX - 1, toggleY - 1, toggleX + 18, toggleY + 18, RULE);
        graphics.fill(toggleX, toggleY, toggleX + 17, toggleY + 17, PANEL_INSET);
        graphics.fill(toggleX, toggleY, toggleX + 2, toggleY + 17, enabled ? CYAN : RED);
        graphics.drawCenteredString(font, Component.literal(enabled ? "I" : "X"), toggleX + 8, toggleY + 4, enabled ? CYAN : RED);
        graphics.renderItem(new ItemStack(Items.BRICKS), viewportX + viewportWidth - 18, contentY + 2);
        StructMarkerItem.MarkerInfo info = StructMarkerItem.getMarkerInfo(menu.slots.get(selectedMarkerSlot).getItem()).orElse(null);
        if (info != null) {
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.structure", selectedMarkerSlot + 1, info.structure().id()), viewportX + 2, contentY, viewportWidth - 24, TEXT);
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.dimension", info.dimension()), viewportX + 2, contentY + 13, viewportWidth - 24, MUTED);
        }
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.dimension_value", formatAnalysisValue(effectiveDimensionValue)), viewportX + 2, contentY + 29, viewportWidth - 4, CYAN);
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.structure_value", formatAnalysisValue(effectiveStructureValue)), viewportX + 2, contentY + 42, viewportWidth - 4, AMBER);
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_progress", menu.getMarkerProgress(selectedMarkerSlot), menu.getMarkerProcessingTime(selectedMarkerSlot)), viewportX + 2, contentY + 58, viewportWidth - 4, TEXT);
        graphics.fill(viewportX + 2, contentY + 69, viewportX + viewportWidth - 22, contentY + 70, RULE);
        graphics.renderItem(new ItemStack(Items.CLOCK), viewportX + viewportWidth - 18, contentY + 72);
        if (waiting) drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.waiting_for_natural_window"), viewportX + 2, contentY + 72, viewportWidth - 4, AMBER);
        int lineY = contentY + (waiting ? 88 : 74);
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.natural_ticks", menu.getMarkerCurrentNaturalTicks(selectedMarkerSlot)), viewportX + 2, lineY, viewportWidth - 4, CYAN);
        lineY += 13;
        if (externalActive) {
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.actual_ticks", menu.getMarkerCurrentExternalAccelerationMachineTicks(selectedMarkerSlot)), viewportX + 2, lineY, viewportWidth - 4, GREEN);
            lineY += 13;
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.info.external", menu.getMarkerExternalEquivalentAccelerationTicks(selectedMarkerSlot)), viewportX + 2, lineY, viewportWidth - 4, GREEN);
            lineY += 13;
        }
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.parallel", menu.getMarkerTotalParallel(selectedMarkerSlot), showParallelBreakdown ? "-" : "+"), viewportX + 2, lineY, viewportWidth - 4, CYAN);
        lineY += 13;
        if (showParallelBreakdown) {
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.parallel.base",
                    menu.getBaseParallel()), viewportX + 10, lineY, viewportWidth - 12, MUTED);
            lineY += 12;
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                    menu.getMarkerExtraEfficiencyParallel(selectedMarkerSlot)), viewportX + 10, lineY, viewportWidth - 12, AMBER);
            lineY += 12;
            if (externalActive) {
                drawClippedInfoLine(graphics, Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                        formatDecimal(menu.getMarkerExternalAccelerationParallelHundredths(selectedMarkerSlot))),
                        viewportX + 10, lineY, viewportWidth - 12, GREEN);
                lineY += 12;
            }
        }
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.previous_cycle_ticks", menu.getMarkerPreviousExternalAccelerationMachineTicks(selectedMarkerSlot)), viewportX + 2, lineY, viewportWidth - 4, MUTED);
        lineY += 13;
        if (externalActive) {
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.previous_cycle_parallel", formatDecimal(menu.getMarkerPreviousExternalAccelerationParallelHundredths(selectedMarkerSlot))), viewportX + 2, lineY, viewportWidth - 4, GREEN);
            lineY += 13;
            drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.actual_parallel", formatDecimal(menu.getMarkerExternalAccelerationParallelHundredths(selectedMarkerSlot))), viewportX + 2, lineY, viewportWidth - 4, GREEN);
        }
        graphics.fill(viewportX + 2, contentY + expectedY - 22, viewportX + viewportWidth - 22, contentY + expectedY - 21, RULE);
        graphics.renderItem(new ItemStack(Items.CHEST), viewportX + viewportWidth - 18, contentY + expectedY - 18);
        drawClippedInfoLine(graphics, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.expected_items"), viewportX + 2, contentY + expectedY - 14, viewportWidth - 4, AMBER);
        hoveredExpectedItem = ItemStack.EMPTY;
        drawExpectedItemRows(graphics, viewportX + 2, contentY + expectedY, viewportWidth - 4, viewportY, viewportHeight, mouseX, mouseY);
        graphics.disableScissor();
        if (markerInfoContentHeight > viewportHeight) {
            int thumbHeight = Math.max(12, viewportHeight * viewportHeight / markerInfoContentHeight);
            int travel = viewportHeight - thumbHeight;
            int thumbY = viewportY + travel * markerInfoScroll / Math.max(1, maxScroll);
            graphics.fill(viewportX + viewportWidth - 3, viewportY, viewportX + viewportWidth - 2, viewportY + viewportHeight, RULE);
            graphics.fill(viewportX + viewportWidth - 4, thumbY, viewportX + viewportWidth - 1, thumbY + thumbHeight, CYAN);
        }
    }

    private void drawInfoLine(GuiGraphics graphics, int x, int y, Item item, String key, Object value) {
        graphics.renderItem(new ItemStack(item), x, y - 3);
        graphics.drawString(font, Component.translatable(key, value), x + 22, y, TEXT, false);
    }

    private void renderInfoTooltip(
            GuiGraphics graphics, int logicalMouseX, int logicalMouseY, int screenMouseX, int screenMouseY) {
        if (selectedMarkerSlot < 0) return;
        int x = leftPos + 28 + 8;
        int y = topPos + INFO_VIEWPORT_Y;
        if (inside(logicalMouseX, logicalMouseY, x, y - 4, 18, 18)) {
            graphics.renderTooltip(font, Component.translatable("screen.dimension_tech.mythic_miner.info.structure"), screenMouseX, screenMouseY);
        } else if (inside(logicalMouseX, logicalMouseY, x, y + 14, 18, 18)) {
            graphics.renderTooltip(font, Component.translatable("screen.dimension_tech.mythic_miner.info.natural"), screenMouseX, screenMouseY);
        } else if (menu.isMarkerExternalAccelerationActive(selectedMarkerSlot)
                && inside(logicalMouseX, logicalMouseY, x, y + 32, 18, 18)) {
            graphics.renderTooltip(font, Component.translatable("screen.dimension_tech.mythic_miner.info.actual"), screenMouseX, screenMouseY);
        }
    }

    private void drawAttributesPage(GuiGraphics graphics) {
        int x = 28;
        int y = INFO_PANEL_Y;
        int width = imageWidth - 56;
        int height = menu.getPlayerInventoryY() + 70 - y;
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        int headerY = y + 8;
        graphics.fill(x + 8, headerY, x + width - 8, headerY + 24, PANEL_INSET);
        graphics.renderItem(new ItemStack(Items.COMPARATOR), x + 11, headerY + 4);
        graphics.drawString(font,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.installed"),
                x + 31, headerY + 8, TEXT, false);
        int listTop = y + 42;
        graphics.fill(x + 8, listTop, x + width - 8, listTop + 1, RULE);
        int viewportY = listTop + 7;
        int viewportHeight = Math.max(1, height - (viewportY - y) - 8);
        int upgradeContentHeight = 0;
        for (MythicMinerUpgradeBlock.Type type : upgradeTypes()) {
            for (int tier = 1; tier <= 6; tier++) {
                if (menu.getUpgradeCount(type, tier) > 0) {
                    upgradeContentHeight += upgradeRowHeight(type, tier);
                }
            }
        }
        attributeContentHeight = Math.max(viewportHeight, upgradeContentHeight);
        int maxScroll = Math.max(0, attributeContentHeight - viewportHeight);
        attributeScroll = Math.min(attributeScroll, maxScroll);
        graphics.fill(x + 8, viewportY, x + width - 8, viewportY + viewportHeight, PANEL_INSET);
        MythicMinerLayout.ScissorBounds scissor = MythicMinerLayout.scaleToScreen(
                leftPos + x + 8, topPos + viewportY, width - 16, viewportHeight, uiScale);
        graphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        int rowY = viewportY - attributeScroll;
        for (MythicMinerUpgradeBlock.Type type : upgradeTypes()) {
            for (int tier = 1; tier <= 6; tier++) {
                int count = menu.getUpgradeCount(type, tier);
                if (count <= 0) continue;
                drawUpgradeDetailRow(graphics, x + 10, rowY, width - 20, type, tier, count);
                rowY += upgradeRowHeight(type, tier);
            }
        }
        graphics.disableScissor();
        if (attributeContentHeight > viewportHeight) {
            int thumbHeight = Math.max(12, viewportHeight * viewportHeight / attributeContentHeight);
            int travel = viewportHeight - thumbHeight;
            int thumbY = viewportY + travel * attributeScroll / Math.max(1, maxScroll);
            graphics.fill(x + width - 11, viewportY, x + width - 10, viewportY + viewportHeight, RULE);
            graphics.fill(x + width - 12, thumbY, x + width - 9, thumbY + thumbHeight, GREEN);
        }
    }

    private MythicMinerUpgradeBlock.Type[] upgradeTypes() {
        return new MythicMinerUpgradeBlock.Type[] {
            MythicMinerUpgradeBlock.Type.EFFICIENCY,
            MythicMinerUpgradeBlock.Type.ENERGY,
            MythicMinerUpgradeBlock.Type.PARALLEL,
            MythicMinerUpgradeBlock.Type.LUCK,
            MythicMinerUpgradeBlock.Type.AGGREGATE};
    }

    private int attributeUpgradeViewportY() {
        return INFO_PANEL_Y + 49;
    }

    private void drawUpgradeDetailRow(GuiGraphics graphics, int x, int y, int width,
            MythicMinerUpgradeBlock.Type type, int tier, int count) {
        Item icon = switch (type) {
            case EFFICIENCY -> Items.REDSTONE;
            case ENERGY -> Items.BEACON;
            case PARALLEL -> Items.ARROW;
            case LUCK -> Items.RABBIT_FOOT;
            case AGGREGATE -> Items.NETHER_STAR;
            default -> Items.STONE;
        };
        graphics.renderItem(new ItemStack(icon), x, y);
        String typeName = Component.translatable(
                "block.dimension_tech.mythic_miner_upgrade_" + type.name().toLowerCase(java.util.Locale.ROOT)
                        + "_tier_" + tier).getString();
        boolean expanded = expandedUpgradeRows.contains(upgradeRowKey(type, tier));
        graphics.drawString(font, typeName + "  x" + count, x + 20, y + 3, TEXT, false);
        graphics.drawString(font, Component.literal(expanded ? "-" : "+"), x + width - 10, y + 4,
                expanded ? GREEN : MUTED, false);
        if (!expanded) {
            graphics.fill(x, y + 20, x + width, y + 21, RULE);
            return;
        }
        ModConfigs.MythicMinerUpgradeTierConfig cfg = type == MythicMinerUpgradeBlock.Type.AGGREGATE
                ? ModConfigs.AGGREGATE_UPGRADE_TIERS[tier - 1] : ModConfigs.UPGRADE_TIERS[tier - 1];
        int lineY = y + 23;
        if (type == MythicMinerUpgradeBlock.Type.EFFICIENCY || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            drawUpgradeBonusLine(graphics, x + 20, lineY, width - 24,
                    "tooltip.dimension_tech.mythic_miner.upgrade.efficiency", cfg.efficiencyIncreasePercent(), CYAN);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.ENERGY || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            drawUpgradeBonusLine(graphics, x + 20, lineY, width - 24,
                    "tooltip.dimension_tech.mythic_miner.upgrade.energy_capacity", cfg.energyCapacityIncreasePercent(), AMBER);
            lineY += 12;
            drawUpgradeBonusLine(graphics, x + 20, lineY, width - 24,
                    type == MythicMinerUpgradeBlock.Type.AGGREGATE
                            ? "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.additive"
                            : "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.multiplicative",
                    cfg.energyConsumptionReductionPercent(), AMBER);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.PARALLEL || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            drawUpgradeBonusLine(graphics, x + 20, lineY, width - 24,
                    "tooltip.dimension_tech.mythic_miner.upgrade.parallel", cfg.parallelIncreasePercent(), CYAN);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.LUCK || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            drawUpgradeBonusLine(graphics, x + 20, lineY, width - 24,
                    "tooltip.dimension_tech.mythic_miner.upgrade.luck", cfg.luckIncreasePercent(), CYAN);
            lineY += 12;
        }
        graphics.fill(x, lineY + 1, x + width, lineY + 2, RULE);
    }

    private void drawUpgradeBonusLine(GuiGraphics graphics, int x, int y, int width,
            String key, double value, int color) {
        Component text = Component.translatable(key, formatConfigPercent(value));
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private int upgradeRowAt(double x, double y) {
        if (x < 36 || x >= imageWidth - 36) return -1;
        int rowY = attributeUpgradeViewportY() - attributeScroll;
        for (MythicMinerUpgradeBlock.Type type : upgradeTypes()) {
            for (int tier = 1; tier <= 6; tier++) {
                if (menu.getUpgradeCount(type, tier) <= 0) continue;
                if (inside(x, y, 38, rowY, imageWidth - 76, 20)) return upgradeRowKey(type, tier);
                rowY += upgradeRowHeight(type, tier);
            }
        }
        return -1;
    }

    private int upgradeRowHeight(MythicMinerUpgradeBlock.Type type, int tier) {
        if (!expandedUpgradeRows.contains(upgradeRowKey(type, tier))) return 22;
        int lines = switch (type) {
            case ENERGY -> 2;
            case AGGREGATE -> 5;
            default -> 1;
        };
        return 23 + lines * 12 + 3;
    }

    private static int upgradeRowKey(MythicMinerUpgradeBlock.Type type, int tier) {
        return type.ordinal() * 10 + tier;
    }

    private static String formatConfigPercent(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private boolean selectPageAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (y < TAB_Y || y >= TAB_Y + TAB_HEIGHT) return false;
        int tabWidth = imageWidth / 3;
        if (x < 0 || x >= imageWidth) return false;
        page = x < tabWidth ? Page.WORK : x < tabWidth * 2 ? Page.INFO : Page.ATTRIBUTES;
        markerInfoScroll = 0;
        attributeScroll = 0;
        return true;
    }

    private int infoSlotAt(double x, double y) {
        int startX = 36;
        int startY = INFO_SLOT_Y;
        if (y < startY || y >= startY + 20) return -1;
        int index = (int) ((x - startX) / 24);
        if (x >= startX && index >= 0 && index < menu.getContainerSlotCount()) return index;
        return -1;
    }

    private void selectMarkerSlot(int slot) {
        if (slot < 0 || slot >= menu.getContainerSlotCount()) return;
        if (selectedMarkerSlot != slot) {
            markerInfoScroll = 0;
            clearEffectiveAnalysis();
        }
        selectedMarkerSlot = slot;
        if (StructMarkerItem.getMarkerInfo(menu.slots.get(slot).getItem()).isPresent()) {
            ModNetwork.requestMythicMinerAnalysis(menu.containerId, slot);
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x99000000);
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 3, CYAN);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, RULE);
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.drawString(font, title, 8, 18, TEXT, false);
        int tabWidth = imageWidth / 3;
        String[] labels = {"screen.dimension_tech.mythic_miner.tab.work", "screen.dimension_tech.mythic_miner.tab.info", "screen.dimension_tech.mythic_miner.tab.attributes"};
        Page[] pages = Page.values();
        for (int index = 0; index < pages.length; index++) {
            int x = index * tabWidth;
            boolean selected = page == pages[index];
            int tabColor = selected ? (index == 0 ? CYAN : index == 1 ? AMBER : GREEN) : MUTED;
            graphics.fill(x + 2, TAB_Y - 2, x + tabWidth - 2, TAB_Y + 10, selected ? PANEL_RAISED : PANEL_INSET);
            graphics.drawCenteredString(font, Component.translatable(labels[index]), x + tabWidth / 2, TAB_Y, tabColor);
        }
    }

    private void drawMarkerBay(GuiGraphics graphics) {
        int x = MythicMinerLayout.MARKER_BAY_X;
        int y = MythicMinerLayout.MARKER_BAY_Y;
        int width = MythicMinerLayout.markerBayWidth(menu.getContainerSlotCount());
        int height = MythicMinerLayout.markerBayHeight(menu.getContainerSlotCount());
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        int progressWidth = MythicMinerLayout.PROGRESS_WIDTH;
        for (int index = 0; index < menu.getContainerSlotCount(); index++) {
            Slot slot = menu.slots.get(index);
            long progress = menu.getMarkerProgress(index);
            int processingTime = menu.getMarkerProcessingTime(index);
            int processing = Math.max(1, processingTime);
            int slotX = slot.x;
            int slotY = slot.y;
            int barX = MythicMinerLayout.progressBarX(slotX);
            int barY = MythicMinerLayout.progressBarY(slotY);
            boolean configuredMarker = StructMarkerItem.getMarkerInfo(slot.getItem()).isPresent();
            if (page == Page.INFO && configuredMarker) {
                if (index == selectedMarkerSlot) {
                    graphics.fill(slotX - 1, slotY - 1, slotX + 18, slotY + 18, CYAN);
                }
                graphics.drawString(font, Integer.toString(index + 1), slotX + 18, slotY + 4, MUTED, false);
            }
            if (index == selectedMarkerSlot && configuredMarker) {
                graphics.fill(
                        barX - 1,
                        barY - 1,
                        barX + progressWidth + 1,
                        barY + MythicMinerLayout.PROGRESS_HEIGHT + 1,
                        CYAN);
            }
            graphics.fill(
                    barX,
                    barY,
                    barX + progressWidth,
                    barY + MythicMinerLayout.PROGRESS_HEIGHT,
                    PANEL_INSET);
            if (configuredMarker && processingTime > 0 && progress > 0) {
                int filled =
                        (int)
                                Math.max(
                                        0L,
                                        Math.min(
                                                progressWidth,
                                                (long) progressWidth * progress / processing));
                graphics.fill(
                        barX, barY, barX + filled, barY + MythicMinerLayout.PROGRESS_HEIGHT, CYAN);
                graphics.fill(
                        barX + filled,
                        barY,
                        barX + Math.min(progressWidth, filled + 1),
                        barY + MythicMinerLayout.PROGRESS_HEIGHT,
                        AMBER);
            }
            if (configuredMarker && !menu.isMarkerSlotEnabled(index)) {
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x99000000);
                graphics.fill(barX, barY, barX + progressWidth, barY + MythicMinerLayout.PROGRESS_HEIGHT, RED);
            }
        }
    }

    private void drawMarkerInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = MythicMinerLayout.markerInfoX(menu.getContainerSlotCount());
        int y = MythicMinerLayout.MARKER_BAY_Y;
        int width = MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth);
        int height = MythicMinerLayout.markerInfoHeight(menu.getContainerSlotCount());
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        hoveredExpectedItem = ItemStack.EMPTY;

        if (selectedMarkerSlot < 0 || selectedMarkerSlot >= menu.getContainerSlotCount()) {
            clearMarkerInfoSelection();
            drawClippedInfoLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.select"),
                    x + 6,
                    y + 16,
                    width - 12,
                    MUTED);
            return;
        }

        ItemStack marker = menu.slots.get(selectedMarkerSlot).getItem();
        var markerInfo = StructMarkerItem.getMarkerInfo(marker);
        if (markerInfo.isEmpty()) {
            clearMarkerInfoSelection();
            drawClippedInfoLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.select"),
                    x + 6,
                    y + 16,
                    width - 12,
                    MUTED);
            return;
        }

        boolean analysisReady =
                receivedAnalysisSlot == selectedMarkerSlot
                        && receivedAnalysisDismantling
                                == menu.isEquipmentDismantlingEnabled();
        StructMarkerItem.MarkerInfo info = markerInfo.get();
        int viewportX = x + MARKER_INFO_PADDING;
        int viewportY = y + MARKER_INFO_PADDING;
        int viewportWidth = width - MARKER_INFO_PADDING * 2;
        int viewportHeight = height - MARKER_INFO_PADDING * 2;
        int contentWidth = Math.max(1, viewportWidth - 5);
        markerInfoContentHeight =
                markerInfoExpectedY()
                        + Math.max(
                                16,
                                analysisReady
                                        ? expectedItemRows.size() * MARKER_INFO_ROW_HEIGHT
                                        : 16);
        markerInfoScroll =
                Math.min(markerInfoScroll, Math.max(0, markerInfoContentHeight - viewportHeight));
        int contentY = viewportY - markerInfoScroll;

        graphics.fill(
                viewportX,
                viewportY,
                viewportX + viewportWidth,
                viewportY + viewportHeight,
                PANEL_INSET);
        MythicMinerLayout.ScissorBounds scissor =
                MythicMinerLayout.scaleToScreen(
                        leftPos + viewportX,
                        topPos + viewportY,
                        viewportWidth,
                        viewportHeight,
                        uiScale);
        graphics.enableScissor(
                scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        int toggleX = viewportX + viewportWidth - 20;
        int toggleY = contentY + 3;
        boolean enabled = menu.isMarkerSlotEnabled(selectedMarkerSlot);
        graphics.fill(toggleX - 1, toggleY - 1, toggleX + 18, toggleY + 18, RULE);
        graphics.fill(toggleX, toggleY, toggleX + 17, toggleY + 17, PANEL_INSET);
        graphics.fill(toggleX, toggleY, toggleX + 2, toggleY + 17, enabled ? CYAN : RED);
        graphics.drawCenteredString(
                font,
                Component.literal(enabled ? "I" : "X"),
                toggleX + 8,
                toggleY + 4,
                enabled ? CYAN : RED);
        int headingWidth = Math.max(1, contentWidth - 24);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.structure",
                        selectedMarkerSlot + 1,
                        info.structure().id()),
                viewportX + 2,
                contentY,
                headingWidth,
                TEXT);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.dimension",
                        info.dimension()),
                viewportX + 2,
                contentY + 12,
                headingWidth,
                MUTED);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.dimension_value",
                        analysisReady ? formatAnalysisValue(effectiveDimensionValue) : "..."),
                viewportX + 2,
                contentY + 28,
                contentWidth,
                CYAN);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.structure_value",
                        analysisReady ? formatAnalysisValue(effectiveStructureValue) : "..."),
                viewportX + 2,
                contentY + 40,
                contentWidth,
                AMBER);

        long progress = menu.getMarkerProgress(selectedMarkerSlot);
        int processingTime = menu.getMarkerProcessingTime(selectedMarkerSlot);
        boolean externalActive = menu.isMarkerExternalAccelerationActive(selectedMarkerSlot);
        boolean waitingForNaturalWindow =
                externalActive && menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_progress",
                        progress,
                        processingTime),
                viewportX + 2,
                contentY + 55,
                contentWidth,
                TEXT);
        if (externalActive && menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot)) {
            drawClippedInfoLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.waiting_for_natural_window"),
                    viewportX + 2,
                    contentY + 77,
                    contentWidth,
                    AMBER);
        }
        int progressY = contentY + (waitingForNaturalWindow ? 89 : 67);
        graphics.fill(
                viewportX + 2,
                progressY,
                viewportX + 2 + contentWidth,
                progressY + 4,
                PANEL_RAISED);
        if (processingTime > 0 && progress > 0) {
            int filled =
                    (int) Math.min(contentWidth, (long) contentWidth * progress / processingTime);
            graphics.fill(viewportX + 2, progressY, viewportX + 2 + filled, progressY + 4, CYAN);
            graphics.fill(
                    viewportX + 2 + filled,
                    progressY,
                    viewportX + 3 + filled,
                    progressY + 4,
                    AMBER);
        }

        int totalParallel = menu.getMarkerTotalParallel(selectedMarkerSlot);
        InfoBounds parallelSummary = parallelSummaryBounds();
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.parallel",
                        totalParallel,
                        Component.translatable(
                                showParallelBreakdown
                                        ? "screen.dimension_tech.mythic_miner.marker_info.parallel.collapse"
                                        : "screen.dimension_tech.mythic_miner.marker_info.parallel.expand")),
                parallelSummary.x(),
                parallelSummary.y(),
                parallelSummary.width(),
                CYAN);
        int detailY = contentY + (waitingForNaturalWindow ? 101 : 89);
        if (showParallelBreakdown) {
            int detailLineX = viewportX + 4;
            graphics.fill(detailLineX, detailY + 4, detailLineX + 1, detailY + 29, RULE);
            drawParallelBreakdownLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.parallel.base",
                            menu.getBaseParallel()),
                    detailLineX,
                    detailY,
                    contentWidth - 2,
                    CYAN);
            detailY += 12;
            drawParallelBreakdownLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                            menu.getMarkerExtraEfficiencyParallel(selectedMarkerSlot)),
                    detailLineX,
                    detailY,
                    contentWidth - 2,
                    AMBER);
            detailY += 12;
            if (externalActive) {
                drawParallelBreakdownLine(
                        graphics,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                                formatDecimal(
                                        menu.getMarkerExternalAccelerationParallelHundredths(
                                                selectedMarkerSlot))),
                        detailLineX,
                        detailY,
                        contentWidth - 2,
                        GREEN);
                detailY += 12;
            }
        }
        if (externalActive) {
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_ticks",
                    menu.getMarkerPreviousExternalAccelerationMachineTicks(selectedMarkerSlot)),
                    viewportX + 2, detailY, contentWidth, AMBER);
            detailY += 12;
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_parallel",
                    formatDecimal(menu.getMarkerPreviousExternalAccelerationParallelHundredths(selectedMarkerSlot))),
                    viewportX + 2, detailY, contentWidth, GREEN);
            detailY += 12;
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                    menu.getMarkerCurrentExternalAccelerationMachineTicks(selectedMarkerSlot)),
                    viewportX + 2, detailY, contentWidth, TEXT);
            detailY += 12;
            drawClippedInfoLine(graphics, Component.translatable(
                    "screen.dimension_tech.mythic_miner.marker_info.actual_parallel",
                    formatDecimal(menu.getMarkerExternalAccelerationParallelHundredths(selectedMarkerSlot))),
                    viewportX + 2, detailY, contentWidth, GREEN);
        }
        graphics.fill(
                viewportX + 2, detailY + 12, viewportX + 2 + contentWidth, detailY + 13, RULE);
        drawClippedInfoLine(
                graphics,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.expected_items"),
                viewportX + 2,
                detailY + 19,
                contentWidth,
                AMBER);
        if (analysisReady) {
            drawExpectedItemRows(
                    graphics,
                    viewportX + 2,
                    contentY + markerInfoExpectedY(),
                    contentWidth,
                    viewportY,
                    viewportHeight,
                    mouseX,
                    mouseY);
        } else {
            drawClippedInfoLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.loading"),
                    viewportX + 2,
                    contentY + markerInfoExpectedY() + 3,
                    contentWidth,
                    MUTED);
        }
        graphics.disableScissor();
        drawMarkerInfoScrollBar(graphics, x, y, width, height, viewportHeight);
    }

    private void drawParallelBreakdownLine(
            GuiGraphics graphics, Component text, int lineX, int y, int width, int color) {
        graphics.fill(lineX, y + 4, lineX + 5, y + 5, color);
        drawClippedInfoLine(
                graphics, text, lineX + 8, y, Math.max(1, width - 8), color);
    }

    private InfoBounds parallelSummaryBounds() {
        int infoX = 36;
        int infoWidth = imageWidth - 72;
        return new InfoBounds(
                infoX + 2,
                INFO_VIEWPORT_Y - markerInfoScroll
                        + (menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot) ? 88 : 74)
                        + (menu.isMarkerExternalAccelerationActive(selectedMarkerSlot) ? 39 : 0),
                Math.max(1, infoWidth - 5),
                12);
    }

    private int markerInfoExpectedY() {
        if (selectedMarkerSlot < 0) return MARKER_INFO_EXPECTED_Y;
        boolean external = menu.isMarkerExternalAccelerationActive(selectedMarkerSlot);
        int y = menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot) ? 88 : 74;
        y += 13; // Natural tick row.
        if (external) y += 26; // Actual ticks and equivalent acceleration.
        y += 13; // Parallel summary.
        if (showParallelBreakdown) y += external ? 36 : 24;
        y += 13; // Previous cycle ticks.
        if (external) y += 26; // Previous and live external parallel.
        return y + 16;
    }

    private boolean insideMarkerInfoViewport(double mouseX, double mouseY) {
        int infoX = 36;
        int infoWidth = imageWidth - 72;
        int infoHeight = infoViewportHeight();
        return inside(
                mouseX,
                mouseY,
                infoX,
                INFO_VIEWPORT_Y,
                infoWidth,
                infoHeight);
    }

    private int infoViewportHeight() {
        return menu.getPlayerInventoryY() + 70 - INFO_PANEL_Y - 66;
    }

    private void clearMarkerInfoSelection() {
        selectedMarkerSlot = -1;
        markerInfoScroll = 0;
        markerInfoContentHeight = 0;
        clearEffectiveAnalysis();
    }

    private void clearEffectiveAnalysis() {
        receivedAnalysisSlot = -1;
        receivedAnalysisDismantling = false;
        effectiveDimensionValue = 0.0D;
        effectiveStructureValue = 0.0D;
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
                || screen.selectedMarkerSlot != slot) {
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
                        .thenComparing(row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        expectedItemRows = List.copyOf(refreshed);
        disabledExpectedItems = Set.copyOf(disabledItems);
        receivedAnalysisSlot = slot;
        receivedAnalysisDismantling = equipmentDismantling;
        effectiveDimensionValue = dimensionValue;
        effectiveStructureValue = structureValue;
    }

    private boolean insideMarkerSlotToggle(double mouseX, double mouseY) {
        if (selectedMarkerSlot < 0) return false;
        int x = 28;
        int width = imageWidth - 56;
        int toggleX = x + 8 + width - 16 - 20;
        int toggleY = INFO_VIEWPORT_Y + 3 - markerInfoScroll;
        return inside(mouseX, mouseY, toggleX, toggleY, 18, 18);
    }

    private void drawExpectedItemRows(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int viewportY,
            int viewportHeight,
            int mouseX,
            int mouseY) {
        if (expectedItemRows.isEmpty()) {
            drawClippedInfoLine(
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.no_items"),
                    x,
                    y + 3,
                    width,
                    MUTED);
            return;
        }
        for (int index = 0; index < expectedItemRows.size(); index++) {
            int rowY = y + index * MARKER_INFO_ROW_HEIGHT;
            ExpectedItemRow row = expectedItemRows.get(index);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(row.item());
            boolean disabled = itemId != null && disabledExpectedItems.contains(itemId);
            ItemStack stack = new ItemStack(row.item());
            String expected = formatExpectedValue(row.expected());
            int expectedWidth = font.width(expected);
            int nameWidth = Math.max(1, width - 22 - expectedWidth - 5);
            if (index % 2 != 0) {
                graphics.fill(x - 1, rowY - 1, x + width, rowY + 18, PANEL_RAISED);
            }
            if (disabled) {
                graphics.fill(x - 2, rowY - 2, x + width, rowY + 18, 0x883A1E25);
                graphics.fill(x - 2, rowY - 2, x, rowY + 18, RED);
            }
            graphics.renderItem(stack, x, rowY);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(stack.getHoverName().getString(), nameWidth),
                    x + 20,
                    rowY + 4,
                    disabled ? RED : TEXT,
                    false);
            graphics.drawString(
                    font,
                    expected,
                    x + width - expectedWidth,
                    rowY + 4,
                    disabled ? RED : CYAN,
                    false);
            if (disabled) {
                graphics.fill(
                        x + 19,
                        rowY + 9,
                        x + width - expectedWidth - 3,
                        rowY + 10,
                        RED);
            }
            if (rowY + 18 > viewportY
                    && rowY < viewportY + viewportHeight
                    && inside(mouseX, mouseY, x, rowY, width, 18)) {
                hoveredExpectedItem = stack;
            }
        }
    }

    private int expectedItemRowAt(double mouseX, double mouseY) {
        if (selectedMarkerSlot < 0 || expectedItemRows.isEmpty()) {
            return -1;
        }
        int x = 38;
        int y = INFO_VIEWPORT_Y + markerInfoExpectedY() - markerInfoScroll;
        int width = imageWidth - 66;
        int index = (int) Math.floor((mouseY - y) / (double) MARKER_INFO_ROW_HEIGHT);
        return index >= 0
                        && index < expectedItemRows.size()
                        && inside(
                                mouseX,
                                mouseY,
                                x,
                                y + index * MARKER_INFO_ROW_HEIGHT,
                                width,
                                18)
                ? index
                : -1;
    }

    private void drawMarkerInfoScrollBar(
            GuiGraphics graphics, int x, int y, int width, int height, int viewportHeight) {
        if (markerInfoContentHeight <= viewportHeight) {
            return;
        }
        int trackY = y + MARKER_INFO_PADDING;
        int trackHeight = height - MARKER_INFO_PADDING * 2;
        int thumbHeight = Math.max(12, trackHeight * viewportHeight / markerInfoContentHeight);
        int maxScroll = markerInfoContentHeight - viewportHeight;
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + thumbTravel * markerInfoScroll / Math.max(1, maxScroll);
        graphics.fill(x + width - 3, trackY, x + width - 2, trackY + trackHeight, RULE);
        graphics.fill(x + width - 4, thumbY, x + width - 1, thumbY + thumbHeight, CYAN);
    }

    private static String formatAnalysisValue(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String formatExpectedValue(double value) {
        if (value > 0.0D && value < 0.0001D) {
            return String.format(java.util.Locale.ROOT, "%.2e", value);
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }

    private void drawClippedInfoLine(
            GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private void drawEnergyRail(GuiGraphics graphics) {
        int x = ENERGY_X;
        int y = ENERGY_TOP;
        int energy = menu.getEnergyStored();
        int capacity = Math.max(1, menu.getEnergyCapacity());
        int filled = Math.max(0, Math.min(ENERGY_HEIGHT, ENERGY_HEIGHT * energy / capacity));
        graphics.fill(x - 1, y - 1, x + 15, y + ENERGY_HEIGHT + 1, RULE);
        graphics.fill(x + 2, y + 2, x + 12, y + ENERGY_HEIGHT - 2, PANEL_INSET);
        int innerHeight = ENERGY_HEIGHT - 4;
        for (int offset = 0; offset < innerHeight; offset++) {
            int red = 18 + (offset * 190 / innerHeight);
            int green = 18 + (offset * 35 / innerHeight);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | 12;
            graphics.fill(
                    x + 3,
                    y + 2 + innerHeight - offset - 1,
                    x + 11,
                    y + 2 + innerHeight - offset,
                    color);
        }
        int filledHeight = Math.min(innerHeight, filled * innerHeight / ENERGY_HEIGHT);
        graphics.fill(x + 3, y + 2 + innerHeight - filledHeight, x + 11, y + 2 + innerHeight, RED);
        graphics.drawString(
                font, Component.literal("FE"), x - 1, y + ENERGY_HEIGHT + 7, MUTED, false);
    }

    private void drawAttributes(GuiGraphics graphics) {
        int x = 28;
        int y = attributeY();
        int width = imageWidth - 56;
        int cellWidth = width / 2;
        int aggregateCount = menu.getAggregateUpgradeCount();
        drawAttributeCell(
                graphics,
                x,
                y,
                cellWidth,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.efficiency"),
                menu.getEfficiencyUpgradeCount() + aggregateCount,
                formatDecimal(menu.getEfficiencyHundredths()),
                menu.getEfficiencyBonusHundredths(),
                false,
                CYAN);
        drawAttributeCell(
                graphics,
                x + cellWidth,
                y,
                width - cellWidth,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.capacity"),
                menu.getEnergyUpgradeCount() + aggregateCount,
                compactNumber(menu.getEnergyCapacity()) + " FE",
                menu.getCapacityBonusHundredths(),
                false,
                AMBER);
        drawAttributeCell(
                graphics,
                x,
                y + 29,
                cellWidth,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.consumption"),
                menu.getEnergyUpgradeCount() + aggregateCount,
                compactNumber(menu.getEffectiveEnergyConsumption()) + " FE/t",
                menu.getConsumptionReductionHundredths(),
                true,
                AMBER);
        drawAttributeCell(
                graphics,
                x + cellWidth,
                y + 29,
                width - cellWidth,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.parallel"),
                menu.getParallelUpgradeCount() + aggregateCount,
                compactNumber(menu.getBaseParallel()),
                menu.getParallelBonusHundredths(),
                false,
                CYAN);
        drawAttributeCell(
                graphics,
                x,
                y + 58,
                cellWidth,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.luck"),
                menu.getLuckUpgradeCount() + aggregateCount,
                formatDecimal(menu.getLuckHundredths()),
                menu.getLuckBonusHundredths(),
                false,
                CYAN);
        drawUpgradeSummaryCell(graphics, x + cellWidth, y + 58, width - cellWidth);
        if (menu.isExternalAccelerationActive()) {
            drawExternalAccelerationCell(graphics, x, y + 87, width);
        }
    }

    private void drawExternalAccelerationCell(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 27, PANEL_INSET);
        graphics.fill(x, y, x + 2, y + 27, GREEN);
        graphics.fill(x + width - 1, y, x + width, y + 27, RULE);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.external_acceleration"),
                x + 7,
                y + 4,
                MUTED,
                false);
        graphics.drawString(
                font,
                Long.toString(menu.getExternalEquivalentAccelerationTicks()) + "x",
                x + 7,
                y + 15,
                TEXT,
                false);
    }

    private void drawAttributeCell(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            Component label,
            int upgradeCount,
            String value,
            int bonusHundredths,
            boolean reduction,
            int accent) {
        graphics.fill(x, y, x + width, y + 27, PANEL_INSET);
        graphics.fill(x, y, x + 2, y + 27, accent);
        graphics.fill(x + width - 1, y, x + width, y + 27, RULE);
        graphics.drawString(font, label, x + 7, y + 4, MUTED, false);
        Component count =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.upgrade_count", upgradeCount);
        graphics.drawString(font, count, x + width - 7 - font.width(count), y + 4, accent, false);
        graphics.drawString(font, value, x + 7, y + 15, TEXT, false);
        String bonus = formatBonus(bonusHundredths, reduction);
        graphics.drawString(font, bonus, x + width - 7 - font.width(bonus), y + 15, accent, false);
    }

    private void drawUpgradeSummaryCell(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 27, PANEL_INSET);
        graphics.fill(x, y, x + 2, y + 27, CYAN_DARK);
        graphics.fill(x + width - 1, y, x + width, y + 27, RULE);
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.upgrades"),
                x + 7,
                y + 4,
                MUTED,
                false);
        Component total =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.total_short",
                        menu.getTotalUpgradeCount());
        graphics.drawString(font, total, x + width - 7 - font.width(total), y + 4, CYAN, false);
        Component counts =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.summary_counts",
                        menu.getTotalUpgradeCount() - menu.getAggregateUpgradeCount(),
                        menu.getAggregateUpgradeCount());
        graphics.drawCenteredString(font, counts, x + width / 2, y + 15, TEXT);
    }

    private void renderAttributeTooltip(
            GuiGraphics graphics,
            int logicalMouseX,
            int logicalMouseY,
            int screenMouseX,
            int screenMouseY) {
        int x = leftPos + 36;
        int y = topPos + 64;
        int width = imageWidth - 72;
        int cellWidth = width / 2 - 12;
        int aggregateCount = menu.getAggregateUpgradeCount();
        List<Component> tooltip = null;
        if (inside(logicalMouseX, logicalMouseY, x, y, cellWidth, 27)) {
            tooltip =
                    attributeTooltip(
                            "screen.dimension_tech.mythic_miner.attribute.efficiency_value",
                            formatDecimal(menu.getEfficiencyHundredths()),
                            menu.getEfficiencyBonusHundredths(),
                            false,
                            menu.getEfficiencyUpgradeCount(),
                            aggregateCount);
        } else if (inside(
                logicalMouseX, logicalMouseY, leftPos + imageWidth / 2 + 4, y, cellWidth, 27)) {
            tooltip =
                    attributeTooltip(
                            "screen.dimension_tech.mythic_miner.attribute.capacity_value",
                            menu.getEnergyCapacity(),
                            menu.getCapacityBonusHundredths(),
                            false,
                            menu.getEnergyUpgradeCount(),
                            aggregateCount);
        } else if (inside(logicalMouseX, logicalMouseY, x, y + 29, cellWidth, 27)) {
            tooltip =
                    attributeTooltip(
                            "screen.dimension_tech.mythic_miner.attribute.consumption_value",
                            menu.getEffectiveEnergyConsumption(),
                            menu.getConsumptionReductionHundredths(),
                            true,
                            menu.getEnergyUpgradeCount(),
                            aggregateCount);
        } else if (inside(
                logicalMouseX,
                logicalMouseY,
                leftPos + imageWidth / 2 + 4,
                y + 29,
                cellWidth,
                27)) {
            tooltip =
                    attributeTooltip(
                            "screen.dimension_tech.mythic_miner.attribute.parallel_value",
                            menu.getBaseParallel(),
                            menu.getParallelBonusHundredths(),
                            false,
                            menu.getParallelUpgradeCount(),
                            aggregateCount);
        } else if (inside(logicalMouseX, logicalMouseY, x, y + 58, cellWidth, 27)) {
            tooltip =
                    attributeTooltip(
                            "screen.dimension_tech.mythic_miner.attribute.luck_value",
                            formatDecimal(menu.getLuckHundredths()),
                            menu.getLuckBonusHundredths(),
                            false,
                            menu.getLuckUpgradeCount(),
                            aggregateCount);
        } else if (inside(
                logicalMouseX,
                logicalMouseY,
                leftPos + imageWidth / 2 + 4,
                y + 58,
                cellWidth,
                27)) {
            tooltip =
                    List.of(
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.attribute.total_count",
                                    menu.getTotalUpgradeCount()),
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                                    menu.getTotalUpgradeCount() - aggregateCount,
                                    aggregateCount));
        }
        if (tooltip != null) {
            graphics.renderTooltip(
                    font, tooltip, Optional.empty(), screenMouseX, screenMouseY);
        }
    }

    private List<Component> attributeTooltip(
            String valueKey,
            Object value,
            int bonusHundredths,
            boolean reduction,
            int specializedCount,
            int aggregateCount) {
        return List.of(
                Component.translatable(valueKey, value),
                Component.translatable(
                        reduction
                                ? "screen.dimension_tech.mythic_miner.attribute.reduction"
                                : "screen.dimension_tech.mythic_miner.attribute.bonus",
                        formatPercentValue(bonusHundredths)),
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                        specializedCount,
                        aggregateCount));
    }

    private int attributeY() {
        return MythicMinerLayout.attributeY(menu.getContainerSlotCount());
    }

    private static String formatDecimal(int hundredths) {
        return String.format(java.util.Locale.ROOT, "%.2f", hundredths / 100.0D);
    }

    private static String formatBonus(int hundredths, boolean reduction) {
        if (hundredths == 0) return "(0%)";
        return "(" + (reduction ? "-" : "+") + formatPercentValue(hundredths) + "%)";
    }

    private static String formatPercentValue(int hundredths) {
        return java.math.BigDecimal.valueOf(hundredths, 2).stripTrailingZeros().toPlainString();
    }

    private static String compactNumber(int value) {
        if (value >= 1_000_000_000) {
            return String.format(java.util.Locale.ROOT, "%.2fB", value / 1_000_000_000.0D);
        }
        if (value >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.2fM", value / 1_000_000.0D);
        }
        if (value >= 1_000) {
            return String.format(java.util.Locale.ROOT, "%.2fK", value / 1_000.0D);
        }
        return Integer.toString(value);
    }

    private static boolean inside(
            double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawPlayerDivider(GuiGraphics graphics) {
        int y = menu.getPlayerInventoryY() - 22;
        graphics.fill(24, y, imageWidth - 24, y + 1, RULE);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, boolean markerSlot) {
        int border = markerSlot ? CYAN_DARK : RULE;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FACE);
        graphics.fill(x, y, x + 16, y + 1, 0xFF6C858B);
        graphics.fill(x, y + 15, x + 16, y + 16, PANEL_INSET);
    }

    private record InfoBounds(int x, int y, int width, int height) {}

    private record ExpectedItemRow(Item item, double expected) {}
}
