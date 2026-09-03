package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Work-page renderer entry point. State and interaction remain owned by the parent screen. */
final class MythicMinerWorkPage {
    static final int OVERVIEW_Y = 48;
    static final int OVERVIEW_HEIGHT = 94;

    private MythicMinerWorkPage() {}

    static void render(MythicMinerScreenContext context, GuiGraphics graphics) {
        drawMarkerBay(context, graphics);
        int x =
                MythicMinerLayout.MARKER_BAY_X
                        + MythicMinerLayout.markerBayWidth(context.menu().getContainerSlotCount())
                        + MythicMinerLayout.MARKER_INFO_GAP;
        int width = context.menu().getWorkContentWidth() - (x - 28);
        int overviewY = OVERVIEW_Y;
        MythicMinerTheme.panel(
                graphics, x, overviewY, width, OVERVIEW_HEIGHT, MythicMinerTheme.FLUIX);
        graphics.drawString(
                context.font(),
                Component.translatable("screen.dimension_tech.mythic_miner.overview"),
                x + 7,
                overviewY + 8,
                MythicMinerScreen.MUTED,
                false);
        int metricWidth = (width - 14) / 2;
        drawMetric(
                context,
                graphics,
                x + 7,
                overviewY + 19,
                metricWidth,
                Items.CLOCK,
                "screen.dimension_tech.mythic_miner.overview.active",
                context.menu().getWorkingThreadCount()
                        + "/"
                        + context.menu().getContainerSlotCount(),
                MythicMinerScreen.TEXT);
        drawMetric(
                context,
                graphics,
                x + 7 + metricWidth,
                overviewY + 19,
                metricWidth,
                Items.REDSTONE,
                "screen.dimension_tech.mythic_miner.attribute.efficiency",
                MythicMinerScreen.formatDecimal(context.menu().getEfficiencyHundredths()),
                MythicMinerScreen.CYAN);
        drawMetric(
                context,
                graphics,
                x + 7,
                overviewY + 38,
                metricWidth,
                Items.RABBIT_FOOT,
                "screen.dimension_tech.mythic_miner.attribute.luck",
                MythicMinerScreen.formatDecimal(context.menu().getLuckHundredths()),
                MythicMinerScreen.CYAN);
        drawMetric(
                context,
                graphics,
                x + 7 + metricWidth,
                overviewY + 38,
                metricWidth,
                Items.ARROW,
                "screen.dimension_tech.mythic_miner.attribute.parallel",
                Integer.toString(context.menu().getBaseParallel()),
                MythicMinerScreen.CYAN);
        drawMetric(
                context,
                graphics,
                x + 7,
                overviewY + 57,
                metricWidth,
                Items.NETHER_STAR,
                "screen.dimension_tech.mythic_miner.overview.equivalent_acceleration",
                Long.toString(context.menu().getExternalEquivalentAccelerationTicks()),
                MythicMinerScreen.CYAN);
        drawMetric(
                context,
                graphics,
                x + 7 + metricWidth,
                overviewY + 57,
                metricWidth,
                Items.ANVIL,
                "screen.dimension_tech.mythic_miner.attribute.upgrades",
                Integer.toString(context.menu().getTotalUpgradeCount()),
                MythicMinerScreen.TEXT);
        int firstX = context.menu().getWorkContentCenter() - 88;
        drawControl(context, graphics, firstX, overviewY + OVERVIEW_HEIGHT + 5, 3);
        if (context.menu().supportsEquipmentDismantling()) {
            drawControl(context, graphics, firstX + 94, overviewY + OVERVIEW_HEIGHT + 5, 4);
        }
    }

    static void renderControls(
            MythicMinerScreenContext c, GuiGraphics g, int logicalMouseX, int logicalMouseY) {
        int x = c.imageWidth() - 26;
        int[] ys = {48, 77, 106};
        for (int index = 0; index < ys.length; index++) {
            boolean hovered =
                    logicalMouseX >= c.leftPos() + x
                            && logicalMouseX < c.leftPos() + x + 24
                            && logicalMouseY >= c.topPos() + ys[index]
                            && logicalMouseY < c.topPos() + ys[index] + 24;
            MythicMinerTheme.button(
                    g,
                    c.font(),
                    c.leftPos() + x,
                    c.topPos() + ys[index],
                    24,
                    24,
                    Component.empty(),
                    hovered,
                    true,
                    index == 2 ? MythicMinerTheme.AMBER : MythicMinerTheme.FLUIX);
            ItemStack icon =
                    switch (index) {
                        case 0 -> new ItemStack(Items.CHEST);
                        case 1 -> outputIcon(c);
                        default -> redstoneIcon(c);
                    };
            g.renderItem(icon, c.leftPos() + x + 4, c.topPos() + ys[index] + 4);
            if (index == 2)
                drawRedstoneOverlay(c, g, c.leftPos() + x + 4, c.topPos() + ys[index] + 4);
        }
    }

    static void renderTooltip(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int logicalX,
            int logicalY,
            int screenX,
            int screenY) {
        int control = controlAt(c, logicalX, logicalY);
        if (control >= 0) {
            g.renderTooltip(
                    c.font(), controlTooltip(c, control), Optional.empty(), screenX, screenY);
        }
        for (int index = 0; index < c.menu().getContainerSlotCount(); index++) {
            Slot slot = c.menu().slots.get(index);
            int barX = c.leftPos() + MythicMinerLayout.progressBarX(slot.x);
            int barY = c.topPos() + MythicMinerLayout.progressBarY(slot.y);
            if (!MythicMinerScreen.inside(
                    logicalX,
                    logicalY,
                    barX,
                    barY,
                    MythicMinerLayout.PROGRESS_WIDTH,
                    MythicMinerLayout.PROGRESS_HEIGHT)) continue;
            boolean configured =
                    com.suntide_20210418.dimensiontech.item.StructMarkerItem.getMarkerInfo(
                                    slot.getItem())
                            .isPresent();
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_progress",
                            configured ? c.menu().getMarkerProgress(index) : 0,
                            configured ? c.menu().getMarkerProcessingTime(index) : 0));
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_progress_toggle",
                            Component.translatable(
                                    c.menu().isMarkerSlotEnabled(index)
                                            ? "screen.dimension_tech.mythic_miner.slot.disable"
                                            : "screen.dimension_tech.mythic_miner.slot.enable")));
            if (configured && c.menu().isMarkerWaitingForNaturalWindow(index)) {
                tooltip.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.waiting_for_natural_window"));
            }
            tooltip.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                            configured ? c.menu().getMarkerCurrentNaturalTicks(index) : 0));
            if (configured && c.menu().isMarkerExternalAccelerationActive(index)) {
                tooltip.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                                c.menu().getMarkerCurrentExternalAccelerationMachineTicks(index)));
            }
            g.renderTooltip(c.font(), tooltip, Optional.empty(), screenX, screenY);
            break;
        }
    }

    static boolean mouseClicked(MythicMinerScreenContext c, double x, double y, int button) {
        if (button != 0) return false;
        for (int index = 0; index < c.menu().getContainerSlotCount(); index++) {
            Slot slot = c.menu().slots.get(index);
            if (MythicMinerScreen.inside(x, y, slot.x - 2, slot.y - 2, 20, 20)) {
                // Keep the visual selection in sync while vanilla still handles item pickup.
                c.selectMarkerSlot(index);
                break;
            }
        }
        for (int index = 0; index < c.menu().getContainerSlotCount(); index++) {
            Slot slot = c.menu().slots.get(index);
            if (MythicMinerScreen.inside(
                    x,
                    y,
                    MythicMinerLayout.progressBarX(slot.x),
                    MythicMinerLayout.progressBarY(slot.y),
                    MythicMinerLayout.PROGRESS_WIDTH,
                    MythicMinerLayout.PROGRESS_HEIGHT)) {
                com.suntide_20210418.dimensiontech.network.ModNetwork.toggleMythicMinerSlot(
                        c.menu().containerId, index);
                return true;
            }
        }
        int controlX = c.imageWidth() - 26;
        if (MythicMinerScreen.inside(x, y, controlX, 48, 24, 24)) {
            c.openOutputFaceScreen();
            return true;
        }
        if (MythicMinerScreen.inside(x, y, controlX, 77, 24, 24)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(c.menu().containerId, 1);
            return true;
        }
        if (MythicMinerScreen.inside(x, y, controlX, 106, 24, 24)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(c.menu().containerId, 2);
            return true;
        }
        int centerX = c.menu().getWorkContentCenter() - 88;
        int centerY = OVERVIEW_Y + OVERVIEW_HEIGHT + 5;
        if (MythicMinerScreen.inside(x, y, centerX, centerY, 88, 24)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(c.menu().containerId, 3);
            return true;
        }
        if (c.menu().supportsEquipmentDismantling()
                && MythicMinerScreen.inside(x, y, centerX + 94, centerY, 88, 24)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(c.menu().containerId, 4);
            return true;
        }
        return false;
    }

    static void drawInventoryChrome(MythicMinerScreenContext c, GuiGraphics g) {
        int dividerY = c.playerInventoryY() - 22;
        g.fill(24, dividerY, c.imageWidth() - 24, dividerY + 1, MythicMinerScreen.AMBER);
        for (int index = c.menu().getContainerSlotCount(); index < c.menu().slots.size(); index++) {
            Slot slot = c.menu().slots.get(index);
            MythicMinerTheme.slot(g, slot.x, slot.y, false, false);
        }
    }

    private static void drawMarkerBay(MythicMinerScreenContext c, GuiGraphics g) {
        int x = MythicMinerLayout.MARKER_BAY_X;
        int y = MythicMinerLayout.MARKER_BAY_Y;
        int width = MythicMinerLayout.markerBayWidth(c.menu().getContainerSlotCount());
        int height = MythicMinerLayout.markerBayHeight(c.menu().getContainerSlotCount());
        MythicMinerTheme.panel(g, x, y, width, height, MythicMinerTheme.AMBER);
        for (int index = 0; index < c.menu().getContainerSlotCount(); index++) {
            Slot slot = c.menu().slots.get(index);
            long progress = c.menu().getMarkerProgress(index);
            int processingTime = c.menu().getMarkerProcessingTime(index);
            int barX = MythicMinerLayout.progressBarX(slot.x);
            int barY = MythicMinerLayout.progressBarY(slot.y);
            boolean configured =
                    com.suntide_20210418.dimensiontech.item.StructMarkerItem.getMarkerInfo(
                                    slot.getItem())
                            .isPresent();
            MythicMinerTheme.slot(
                    g,
                    slot.x,
                    slot.y,
                    index == c.selectedMarkerSlot(),
                    configured && !c.menu().isMarkerSlotEnabled(index));
            g.fill(
                    barX,
                    barY,
                    barX + MythicMinerLayout.PROGRESS_WIDTH,
                    barY + MythicMinerLayout.PROGRESS_HEIGHT,
                    MythicMinerScreen.PANEL_INSET);
            if (configured && processingTime > 0 && progress > 0) {
                MythicMinerTheme.progress(
                        g,
                        barX,
                        barY,
                        MythicMinerLayout.PROGRESS_WIDTH,
                        progress,
                        processingTime,
                        MythicMinerTheme.FLUIX);
            }
            if (configured && !c.menu().isMarkerSlotEnabled(index)) {
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, MythicMinerTheme.DISABLED_OVERLAY);
                g.fill(
                        barX,
                        barY,
                        barX + MythicMinerLayout.PROGRESS_WIDTH,
                        barY + MythicMinerLayout.PROGRESS_HEIGHT,
                        MythicMinerScreen.RED);
            }
        }
    }

    private static int controlAt(MythicMinerScreenContext c, int mouseX, int mouseY) {
        int controlX = c.leftPos() + c.imageWidth() - 26;
        for (int index = 0; index < 3; index++) {
            if (MythicMinerScreen.inside(
                    mouseX, mouseY, controlX, c.topPos() + 48 + index * 29, 24, 24)) return index;
        }
        int centerX = c.leftPos() + c.menu().getWorkContentCenter() - 88;
        int centerY = c.topPos() + OVERVIEW_Y + OVERVIEW_HEIGHT + 5;
        if (MythicMinerScreen.inside(mouseX, mouseY, centerX, centerY, 88, 24)) return 3;
        return c.menu().supportsEquipmentDismantling()
                        && MythicMinerScreen.inside(mouseX, mouseY, centerX + 94, centerY, 88, 24)
                ? 4
                : -1;
    }

    private static List<Component> controlTooltip(MythicMinerScreenContext c, int control) {
        if (control == 0)
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.output_face"),
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output_faces_enabled",
                            Integer.bitCount(c.menu().telemetrySnapshot().outputFaceMask())));
        if (control == 1) {
            BaseMinerBlockEntity.OutputState state = c.menu().telemetrySnapshot().outputState();
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.output_mode"),
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output."
                                    + state.name().toLowerCase(Locale.ROOT)));
        }
        if (control == 3)
            return List.of(
                    Component.translatable("screen.dimension_tech.mythic_miner.place_structure"));
        if (control == 4)
            return List.of(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.equipment_dismantling"),
                    Component.translatable(
                            c.menu().isEquipmentDismantlingEnabled()
                                    ? "screen.dimension_tech.mythic_miner.enabled"
                                    : "screen.dimension_tech.mythic_miner.disabled"));
        BaseMinerBlockEntity.RedstoneMode mode = c.menu().telemetrySnapshot().redstoneMode();
        return List.of(
                Component.translatable("screen.dimension_tech.mythic_miner.redstone"),
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.redstone."
                                + mode.name().toLowerCase(Locale.ROOT)));
    }

    private static ItemStack outputIcon(MythicMinerScreenContext c) {
        return c.menu().telemetrySnapshot().outputState()
                        == BaseMinerBlockEntity.OutputState.ME_NETWORK
                ? new ItemStack(Items.ENDER_CHEST)
                : new ItemStack(Items.CHEST);
    }

    private static ItemStack redstoneIcon(MythicMinerScreenContext c) {
        return switch (c.menu().telemetrySnapshot().redstoneMode().ordinal()) {
            case 0, 1 -> new ItemStack(Items.REDSTONE_TORCH);
            case 2 -> new ItemStack(Items.TORCH);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    private static void drawRedstoneOverlay(
            MythicMinerScreenContext c, GuiGraphics g, int x, int y) {
        int mode = c.menu().telemetrySnapshot().redstoneMode().ordinal();
        if (mode == 0) {
            for (int offset = 0; offset < 7; offset++)
                g.fill(
                        x + 1 + offset,
                        y + 1 + offset,
                        x + 3 + offset,
                        y + 3 + offset,
                        MythicMinerScreen.RED);
        } else if (mode == 3) {
            g.fill(x + 11, y + 2, x + 14, y + 5, MythicMinerScreen.AMBER);
            g.fill(x + 12, y + 5, x + 13, y + 11, MythicMinerScreen.AMBER);
            g.fill(x + 12, y + 13, x + 13, y + 14, MythicMinerScreen.AMBER);
        }
    }

    private static void drawMetric(
            MythicMinerScreenContext context,
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            Item icon,
            String key,
            String value,
            int color) {
        graphics.renderItem(new ItemStack(icon), x, y - 3);
        String label = Component.translatable(key).getString();
        graphics.drawString(
                context.font(),
                context.font().plainSubstrByWidth(label, width - 21),
                x + 20,
                y - 2,
                MythicMinerScreen.MUTED,
                false);
        graphics.drawString(
                context.font(),
                context.font().plainSubstrByWidth(value, width - 21),
                x + 20,
                y + 7,
                color,
                false);
    }

    private static void drawControl(
            MythicMinerScreenContext context, GuiGraphics graphics, int x, int y, int control) {
        int width = 88;
        MythicMinerTheme.button(
                graphics,
                context.font(),
                x,
                y,
                width,
                24,
                Component.empty(),
                false,
                true,
                MythicMinerTheme.AMBER);
        graphics.renderItem(new ItemStack(control == 3 ? Items.BRICKS : Items.ANVIL), x + 4, y + 4);
        graphics.drawString(
                context.font(),
                Component.translatable(
                        control == 3
                                ? "screen.dimension_tech.mythic_miner.place_structure_short"
                                : "screen.dimension_tech.mythic_miner.equipment_dismantling"),
                x + 24,
                y + 8,
                MythicMinerTheme.TEXT,
                false);
        if (control == 4)
            graphics.fill(
                    x + 3,
                    y + 21,
                    x + width - 3,
                    y + 23,
                    context.menu().isEquipmentDismantlingEnabled()
                            ? MythicMinerTheme.FLUIX
                            : MythicMinerTheme.EDGE);
    }

    static boolean isWorkPage(MythicMinerScreen.Page page) {
        return page == MythicMinerScreen.Page.WORK;
    }
}
