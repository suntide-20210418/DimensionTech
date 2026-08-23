package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
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
    private static final int ENERGY_TOP = 36;
    private static final int ENERGY_HEIGHT = 164;
    private static final int LEFT_CONTROLS_WIDTH = 30;
    private static final int SCREEN_MARGIN = 8;
    private static final int MARKER_INFO_PADDING = 5;
    private static final int MARKER_INFO_ROW_HEIGHT = 20;
    private static final int MARKER_INFO_EXPECTED_Y = 160;
    private static final int PARALLEL_BREAKDOWN_HEIGHT = 36;
    private float uiScale = 1.0F;
    private int selectedMarkerSlot = -1;
    private int markerInfoScroll;
    private int markerInfoContentHeight;
    private int receivedAnalysisSlot = -1;
    private boolean receivedAnalysisDismantling;
    private double effectiveDimensionValue;
    private double effectiveStructureValue;
    private ItemStack hoveredExpectedItem = ItemStack.EMPTY;
    private List<ExpectedItemRow> expectedItemRows = List.of();
    private Set<ResourceLocation> disabledExpectedItems = Set.of();
    private boolean showParallelBreakdown;
    private Page page = Page.WORK;
    private static final int TAB_Y = 20;
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
        super.render(graphics, logicalMouseX, logicalMouseY, partialTick);
        if (page != Page.WORK) {
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
                    topPos + 36,
                    0xFFE45B51);
        }
        graphics.pose().popPose();

        renderTooltip(graphics, mouseX, mouseY);
        int control = -1;
        for (int index = 0; page == Page.WORK && index < Math.min(3, controlCount()); index++) {
            int buttonY = topPos + 38 + index * 29;
            if (logicalMouseX >= leftPos - 29
                    && logicalMouseX < leftPos - 5
                    && logicalMouseY >= buttonY
                    && logicalMouseY < buttonY + 24) {
                control = index;
                break;
            }
        }
        if (page == Page.WORK && control < 0) {
            int centerY = topPos + 118;
            int firstX = leftPos + imageWidth / 2 - 28;
            if (logicalMouseY >= centerY && logicalMouseY < centerY + 24) {
                if (logicalMouseX >= firstX && logicalMouseX < firstX + 24) control = 3;
                else if (menu.supportsEquipmentDismantling()
                        && logicalMouseX >= firstX + 30 && logicalMouseX < firstX + 54) control = 4;
            }
        }
        if (control >= 0) {
            graphics.renderTooltip(
                    font, controlTooltip(control), Optional.empty(), mouseX, mouseY);
        }
        if (page == Page.WORK && logicalMouseX >= leftPos + ENERGY_X
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
        int[] ys = {38, 67, 96, 125, 154};
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
            for (int index = 0; index < menu.getContainerSlotCount(); index++) {
                Slot slot = menu.slots.get(index);
                int barX = MythicMinerLayout.progressBarX(slot.x);
                int barY = MythicMinerLayout.progressBarY(slot.y);
                if (inside(
                        x,
                        y,
                        barX,
                        barY,
                        MythicMinerLayout.PROGRESS_WIDTH,
                        MythicMinerLayout.PROGRESS_HEIGHT)) {
                    int nextSelection =
                            StructMarkerItem.getMarkerInfo(slot.getItem()).isPresent() ? index : -1;
                    if (nextSelection != selectedMarkerSlot) {
                        markerInfoScroll = 0;
                        clearEffectiveAnalysis();
                    }
                    selectedMarkerSlot = nextSelection;
                    if (selectedMarkerSlot >= 0) {
                        ModNetwork.requestMythicMinerAnalysis(
                                menu.containerId, selectedMarkerSlot);
                    }
                    return true;
                }
            }
            if (page != Page.WORK) return true;
            if (x >= -29 && x < -5 && y >= 38 && y < 62) {
                Minecraft.getInstance().setScreen(new OutputFaceScreen(this, menu));
                return true;
            }
            if (x >= -29 && x < -5 && y >= 67 && y < 91) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
                return true;
            }
            if (x >= -29 && x < -5 && y >= 96 && y < 120) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 2);
                return true;
            }
            int centerControlX = imageWidth / 2 - 28;
            if (x >= centerControlX && x < centerControlX + 24 && y >= 118 && y < 142) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 3);
                return true;
            }
            if (menu.supportsEquipmentDismantling()
                    && x >= centerControlX + 30
                    && x < centerControlX + 54
                    && y >= 118
                    && y < 142) {
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
        if (page != Page.INFO) return true;
        double logicalMouseX = mouseX / uiScale;
        double logicalMouseY = mouseY / uiScale;
        int infoX = leftPos + MythicMinerLayout.markerInfoX(menu.getContainerSlotCount());
        int infoY = topPos + MythicMinerLayout.MARKER_BAY_Y;
        int infoWidth = MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth);
        int infoHeight = MythicMinerLayout.markerInfoHeight(menu.getContainerSlotCount());
        if (inside(
                logicalMouseX,
                logicalMouseY,
                infoX,
                infoY,
                infoWidth,
                infoHeight)) {
            int viewportHeight = infoHeight - MARKER_INFO_PADDING * 2;
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
            drawInfoPage(graphics);
        } else {
            drawAttributesPage(graphics);
        }
    }

    private void drawWorkOverview(GuiGraphics graphics) {
        int x = 28;
        int width = imageWidth - 56;
        int overviewY = 86;
        boolean externalActive = menu.isExternalAccelerationActive();
        int overviewHeight = externalActive ? 70 : 54;
        graphics.fill(x, overviewY, x + width, overviewY + overviewHeight, PANEL_RAISED);
        graphics.fill(x, overviewY, x + width, overviewY + 2, CYAN_DARK);
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.mythic_miner.overview"),
                x + 7,
                overviewY + 5,
                MUTED,
                false);
        int half = width / 2;
        drawOverviewMetric(graphics, x + 7, overviewY + 14, Items.REDSTONE,
                formatDecimal(menu.getEfficiencyHundredths()), CYAN);
        drawOverviewMetric(graphics, x + half, overviewY + 14, Items.RABBIT_FOOT,
                formatDecimal(menu.getLuckHundredths()), CYAN);
        drawOverviewMetric(graphics, x + 7, overviewY + 29, Items.ARROW,
                Integer.toString(menu.getBaseParallel()), CYAN);
        drawOverviewMetric(graphics, x + half, overviewY + 29, Items.ANVIL,
                Integer.toString(menu.getTotalUpgradeCount()), TEXT);
        if (externalActive) {
            drawOverviewMetric(graphics, x + 7, overviewY + 44, Items.LIGHTNING_ROD,
                    Long.toString(menu.getExternalEquivalentAccelerationTicks()), GREEN);
        }
        drawCentralWorkControls(graphics, overviewY + overviewHeight + 5);

        int progressY = overviewY + overviewHeight + 38;
        int columnWidth = width / 2;
        for (int index = 0; index < menu.getContainerSlotCount(); index++) {
            int column = index % 2;
            int row = index / 2;
            int rowY = progressY + row * 18;
            int rowX = x + column * columnWidth;
            drawWorkProgressRow(graphics, rowX, rowY, columnWidth - 6, index);
        }
    }

    private void drawOverviewMetric(
            GuiGraphics graphics, int x, int y, Item icon, String value, int color) {
        graphics.renderItem(new ItemStack(icon), x, y - 3);
        graphics.drawString(font, value, x + 20, y + 1, color, false);
    }

    private void drawWorkProgressRow(GuiGraphics graphics, int x, int y, int width, int slotIndex) {
        Slot slot = menu.slots.get(slotIndex);
        boolean configured = StructMarkerItem.getMarkerInfo(slot.getItem()).isPresent();
        graphics.renderItem(slot.getItem(), x, y - 3);
        graphics.drawString(font, Integer.toString(slotIndex + 1), x + 18, y + 1, MUTED, false);
        int barX = x + 31;
        int barWidth = Math.max(24, width - 72);
        graphics.fill(barX, y + 2, barX + barWidth, y + 6, PANEL_INSET);
        if (configured) {
            long progress = menu.getMarkerProgress(slotIndex);
            int processing = Math.max(1, menu.getMarkerProcessingTime(slotIndex));
            int filled = (int) Math.min(barWidth, barWidth * progress / processing);
            graphics.fill(barX, y + 2, barX + filled, y + 6, CYAN);
        }
        graphics.drawString(
                font,
                configured
                        ? Component.translatable(
                                "screen.dimension_tech.mythic_miner.overview.progress",
                                menu.getMarkerProgress(slotIndex),
                                menu.getMarkerProcessingTime(slotIndex))
                        : Component.literal("-"),
                barX + barWidth + 4,
                y + 1,
                configured ? TEXT : MUTED,
                false);
    }

    private void drawCentralWorkControls(GuiGraphics graphics, int y) {
        int firstX = imageWidth / 2 - 28;
        drawCenteredControl(graphics, firstX, y, 3);
        if (menu.supportsEquipmentDismantling()) {
            drawCenteredControl(graphics, firstX + 30, y, 4);
        }
    }

    private void drawCenteredControl(GuiGraphics graphics, int x, int y, int control) {
        graphics.fill(x - 1, y - 1, x + 25, y + 25, RULE);
        graphics.fill(x, y, x + 24, y + 24, PANEL_RAISED);
        ItemStack icon = control == 3 ? new ItemStack(Items.BRICKS) : new ItemStack(Items.ANVIL);
        graphics.renderItem(icon, x + 4, y + 4);
        if (control == 4) {
            graphics.fill(x + 3, y + 21, x + 21, y + 23,
                    menu.isEquipmentDismantlingEnabled() ? CYAN : RULE);
        }
    }

    private void drawInfoPage(GuiGraphics graphics) {
        int x = 28;
        int y = 42;
        int width = imageWidth - 56;
        int height = menu.getPlayerInventoryY() + 70 - y;
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.info.slots"), x + 8, y + 8, MUTED, false);
        int slotX = x + 8;
        for (int index = 0; index < menu.getContainerSlotCount(); index++) {
            int buttonX = slotX + index * 24;
            boolean selected = index == selectedMarkerSlot;
            graphics.fill(buttonX, y + 22, buttonX + 20, y + 42, selected ? CYAN_DARK : RULE);
            graphics.fill(buttonX + 1, y + 23, buttonX + 19, y + 41, PANEL_INSET);
            if (!menu.slots.get(index).getItem().isEmpty()) {
                graphics.renderItem(menu.slots.get(index).getItem(), buttonX + 2, y + 24);
            }
            graphics.drawCenteredString(font, Integer.toString(index + 1), buttonX + 10, y + 28, selected ? TEXT : MUTED);
        }
        if (selectedMarkerSlot < 0) {
            graphics.drawCenteredString(font, Component.translatable("screen.dimension_tech.mythic_miner.marker_info.select"), x + width / 2, y + 82, MUTED);
            return;
        }
        int detailY = y + 58;
        drawInfoLine(graphics, x + 8, detailY, Items.BRICKS, "screen.dimension_tech.mythic_miner.info.structure", menu.getMarkerProcessingTime(selectedMarkerSlot));
        drawInfoLine(graphics, x + 8, detailY + 18, Items.CLOCK, "screen.dimension_tech.mythic_miner.info.natural", menu.getMarkerCurrentNaturalTicks(selectedMarkerSlot));
        boolean externalActive = menu.isMarkerExternalAccelerationActive(selectedMarkerSlot);
        if (externalActive) {
            drawInfoLine(graphics, x + 8, detailY + 36, Items.LIGHTNING_ROD, "screen.dimension_tech.mythic_miner.info.actual", menu.getMarkerCurrentExternalAccelerationMachineTicks(selectedMarkerSlot));
            drawInfoLine(graphics, x + 8, detailY + 54, Items.SPECTRAL_ARROW, "screen.dimension_tech.mythic_miner.info.external", menu.getMarkerExternalEquivalentAccelerationTicks(selectedMarkerSlot));
        }
        if (externalActive && menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot)) {
            graphics.drawString(font, Component.translatable("screen.dimension_tech.mythic_miner.waiting_for_natural_window"), x + 8, detailY + 74, AMBER, false);
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
        int y = topPos + 42 + 58;
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
        int y = 42;
        int width = imageWidth - 56;
        int height = menu.getPlayerInventoryY() + 70 - y;
        graphics.fill(x, y, x + width, y + height, PANEL_RAISED);
        graphics.fill(x, y, x + width, y + 2, CYAN_DARK);
        drawAttributeCell(graphics, x + 8, y + 12, width / 2 - 12, Component.translatable("screen.dimension_tech.mythic_miner.attribute.efficiency"), menu.getEfficiencyUpgradeCount() + menu.getAggregateUpgradeCount(), formatDecimal(menu.getEfficiencyHundredths()), menu.getEfficiencyBonusHundredths(), false, CYAN);
        drawAttributeCell(graphics, x + width / 2 + 4, y + 12, width / 2 - 12, Component.translatable("screen.dimension_tech.mythic_miner.attribute.capacity"), menu.getEnergyUpgradeCount() + menu.getAggregateUpgradeCount(), compactNumber(menu.getEnergyCapacity()) + " FE", menu.getCapacityBonusHundredths(), false, AMBER);
        drawAttributeCell(graphics, x + 8, y + 41, width / 2 - 12, Component.translatable("screen.dimension_tech.mythic_miner.attribute.consumption"), menu.getEnergyUpgradeCount() + menu.getAggregateUpgradeCount(), compactNumber(menu.getEffectiveEnergyConsumption()) + " FE/t", menu.getConsumptionReductionHundredths(), true, AMBER);
        drawAttributeCell(graphics, x + width / 2 + 4, y + 41, width / 2 - 12, Component.translatable("screen.dimension_tech.mythic_miner.attribute.parallel"), menu.getParallelUpgradeCount() + menu.getAggregateUpgradeCount(), compactNumber(menu.getBaseParallel()), menu.getParallelBonusHundredths(), false, CYAN);
        drawAttributeCell(graphics, x + 8, y + 70, width / 2 - 12, Component.translatable("screen.dimension_tech.mythic_miner.attribute.luck"), menu.getLuckUpgradeCount() + menu.getAggregateUpgradeCount(), formatDecimal(menu.getLuckHundredths()), menu.getLuckBonusHundredths(), false, CYAN);
        drawUpgradeSummaryCell(graphics, x + width / 2 + 4, y + 70, width / 2 - 12);
        if (menu.isExternalAccelerationActive()) drawExternalAccelerationCell(graphics, x + 8, y + 110, width - 16);
    }

    private boolean selectPageAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (y < TAB_Y || y >= TAB_Y + TAB_HEIGHT) return false;
        int tabWidth = imageWidth / 3;
        if (x < 0 || x >= imageWidth) return false;
        page = x < tabWidth ? Page.WORK : x < tabWidth * 2 ? Page.INFO : Page.ATTRIBUTES;
        markerInfoScroll = 0;
        return true;
    }

    private int infoSlotAt(double x, double y) {
        int startX = 36;
        int startY = 64;
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
        graphics.drawString(font, title, 8, 8, TEXT, false);
        int tabWidth = imageWidth / 3;
        String[] labels = {"screen.dimension_tech.mythic_miner.tab.work", "screen.dimension_tech.mythic_miner.tab.info", "screen.dimension_tech.mythic_miner.tab.attributes"};
        Page[] pages = Page.values();
        for (int index = 0; index < pages.length; index++) {
            int x = index * tabWidth;
            boolean selected = page == pages[index];
            graphics.drawCenteredString(font, Component.translatable(labels[index]), x + tabWidth / 2, TAB_Y, selected ? TEXT : MUTED);
            if (selected) graphics.fill(x + 8, TAB_Y + 10, x + tabWidth - 8, TAB_Y + 11, CYAN);
        }
        graphics.fill(24, 34, imageWidth - 24, 35, RULE);
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
                graphics.renderItem(slot.getItem(), slotX, slotY);
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
        int infoX = MythicMinerLayout.markerInfoX(menu.getContainerSlotCount());
        int infoWidth = MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth);
        return new InfoBounds(
                infoX + MARKER_INFO_PADDING + 2,
                MythicMinerLayout.MARKER_BAY_Y + MARKER_INFO_PADDING - markerInfoScroll + 77
                        + (menu.isMarkerWaitingForNaturalWindow(selectedMarkerSlot) ? 12 : 0),
                Math.max(1, infoWidth - MARKER_INFO_PADDING * 2 - 5),
                12);
    }

    private int markerInfoExpectedY() {
        return MARKER_INFO_EXPECTED_Y
                + (showParallelBreakdown ? PARALLEL_BREAKDOWN_HEIGHT : 0);
    }

    private boolean insideMarkerInfoViewport(double mouseX, double mouseY) {
        int infoX = MythicMinerLayout.markerInfoX(menu.getContainerSlotCount());
        int infoWidth = MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth);
        int infoHeight = MythicMinerLayout.markerInfoHeight(menu.getContainerSlotCount());
        return inside(
                mouseX,
                mouseY,
                infoX + MARKER_INFO_PADDING,
                MythicMinerLayout.MARKER_BAY_Y + MARKER_INFO_PADDING,
                infoWidth - MARKER_INFO_PADDING * 2,
                infoHeight - MARKER_INFO_PADDING * 2);
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
        int x = MythicMinerLayout.markerInfoX(menu.getContainerSlotCount());
        int width = MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth);
        int toggleX = x + MARKER_INFO_PADDING + (width - MARKER_INFO_PADDING * 2) - 20;
        int toggleY = MythicMinerLayout.MARKER_BAY_Y + MARKER_INFO_PADDING + 3;
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
        int x =
                MythicMinerLayout.markerInfoX(menu.getContainerSlotCount())
                        + MARKER_INFO_PADDING
                        + 2;
        int y =
                MythicMinerLayout.MARKER_BAY_Y
                        + MARKER_INFO_PADDING
                        + markerInfoExpectedY()
                        - markerInfoScroll;
        int width =
                MythicMinerLayout.markerInfoWidth(menu.getContainerSlotCount(), imageWidth)
                        - MARKER_INFO_PADDING * 2;
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
        int x = leftPos + 28;
        int y = topPos + attributeY();
        int width = imageWidth - 56;
        int cellWidth = width / 2;
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
                logicalMouseX, logicalMouseY, x + cellWidth, y, width - cellWidth, 27)) {
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
                x + cellWidth,
                y + 29,
                width - cellWidth,
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
                x + cellWidth,
                y + 58,
                width - cellWidth,
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
