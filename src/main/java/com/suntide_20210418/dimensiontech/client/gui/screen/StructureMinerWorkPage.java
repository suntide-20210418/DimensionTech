package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerTelemetrySnapshot;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * WORK page: the marker lane, one progress strip per cell, and a two-column meter grid.
 *
 * <p>The meter grid carries eight readings in four lines of two. It deliberately shows both the base
 * parallel and the total parallel: the total is what the machine actually produces, while the base is
 * only one of its three addends, so showing the base alone would answer the wrong question.
 *
 * <p>State and interaction stay with the parent screen; this class only draws. The lane is drawn here
 * rather than by the vanilla slot pass because the menu parks its slots off-screen — see
 * {@code StructureMinerMenu#addContainerSlots}.
 */
final class StructureMinerWorkPage {
    private StructureMinerWorkPage() {}

    static void render(StructureMinerScreenContext context, GuiGraphics g) {
        StructureMinerTelemetrySnapshot telemetry = context.menu().telemetrySnapshot();
        drawLane(context, g, telemetry);
        drawMeters(context, g, telemetry);
        drawToggleHint(context, g);
        drawStatusChips(context, g, telemetry);
    }

    // --- marker lane -------------------------------------------------------

    private static void drawLane(
            StructureMinerScreenContext c, GuiGraphics g, StructureMinerTelemetrySnapshot t) {
        int count = c.menu().getContainerSlotCount();
        for (int slot = 0; slot < count; slot++) {
            int x = StructureMinerInfoLayout.laneX(slot, count);
            int y = StructureMinerInfoLayout.MARKER_Y;
            ItemStack stack = c.menu().slots.get(slot).getItem();
            boolean configured = StructMarkerItem.getMarkerInfo(stack).isPresent();
            boolean enabled = t.markers().get(slot).enabled();

            StructureMinerSpriteRenderer.marker(
                    g,
                    x + StructureMinerInfoLayout.MARKER_FACE_INSET,
                    y + StructureMinerInfoLayout.MARKER_FACE_INSET,
                    slot == c.selectedMarkerSlot());

            if (!stack.isEmpty()) {
                g.renderItem(
                        stack,
                        x + StructureMinerInfoLayout.MARKER_ICON_INSET,
                        y + StructureMinerInfoLayout.MARKER_ICON_INSET);
            }

            if (slot == c.hoveredMarkerSlot()) {
                g.fill(
                        x,
                        y,
                        x + StructureMinerInfoLayout.MARKER_SIZE,
                        y + StructureMinerInfoLayout.MARKER_SIZE,
                        GuiPalette.withAlpha(GuiPalette.HILIGHT, 0x40));
            }
            if (configured && !enabled) {
                g.fill(
                        x,
                        y,
                        x + StructureMinerInfoLayout.MARKER_SIZE,
                        y + StructureMinerInfoLayout.MARKER_SIZE,
                        StructureMinerTheme.DISABLED_OVERLAY);
            }

            drawStatusLight(g, t, slot, x, y, configured);
            drawProgressStrip(g, t, slot, x, configured, enabled);
        }
    }

    /**
     * A 3px lamp in the cell's top-right corner. Kept out of the centre so it never covers the marker
     * icon, whose visual weight sits in the middle 10px.
     *
     * <p>Amber covers both "not advancing" states — no processing plan (analysis still running or
     * failed) and a slot throttled by the natural observation window — because the client cannot
     * tell those apart; the tooltips spell out which one applies. The distinction that matters on
     * the lamp itself is between those and a slot that is actually running.
     */
    private static void drawStatusLight(
            GuiGraphics g,
            StructureMinerTelemetrySnapshot t,
            int slot,
            int x,
            int y,
            boolean configured) {
        if (!configured) return;
        StructureMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int color =
                !marker.enabled()
                        ? StructureMinerTheme.ERROR
                        : marker.processingTime() <= 0 || marker.waitingForNaturalWindow()
                                ? StructureMinerTheme.AMBER
                                : StructureMinerTheme.FLUIX;
        int lampRight = x + StructureMinerInfoLayout.MARKER_SIZE - StructureMinerInfoLayout.MARKER_LAMP_RIGHT_INSET;
        int lampTop = y + StructureMinerInfoLayout.MARKER_LAMP_TOP_INSET;
        g.fill(
                lampRight - StructureMinerInfoLayout.MARKER_LAMP_SIZE,
                lampTop,
                lampRight,
                lampTop + StructureMinerInfoLayout.MARKER_LAMP_SIZE,
                color);
    }

    private static void drawProgressStrip(
            GuiGraphics g,
            StructureMinerTelemetrySnapshot t,
            int slot,
            int x,
            boolean configured,
            boolean enabled) {
        int barX = x + StructureMinerInfoLayout.MARKER_PROGRESS_INSET;
        int barY = StructureMinerInfoLayout.MARKER_PROGRESS_Y;
        int width = StructureMinerInfoLayout.MARKER_PROGRESS_W;
        int height = StructureMinerInfoLayout.MARKER_PROGRESS_H;

        if (!configured) {
            // An unconfigured cell reads as "no thread", which is not "a thread sitting at 0%".
            g.fill(barX, barY + 1, barX + width, barY + 2, StructureMinerTheme.PROGRESS_TRACK);
            return;
        }

        StructureMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int filled =
                StructureMinerProgressStrip.pixels(marker.progress(), marker.processingTime(), width);
        StructureMinerSpriteRenderer.progressStrip(g, barX, barY, width, enabled ? filled : 0);

        if (!enabled) {
            g.fill(barX, barY, barX + width, barY + 1, StructureMinerTheme.ERROR);
            g.fill(barX, barY + height - 1, barX + width, barY + height, StructureMinerTheme.ERROR);
        } else if (marker.waitingForNaturalWindow()) {
            // The strip is colour-locked to its sprite, so a side mark carries the throttled state.
            g.fill(
                    barX + width - StructureMinerInfoLayout.MARKER_PROGRESS_END_MARK_W,
                    barY,
                    barX + width,
                    barY + height,
                    StructureMinerTheme.AMBER);
        }
    }

    // --- meter grid --------------------------------------------------------

    private static void drawMeters(
            StructureMinerScreenContext c, GuiGraphics g, StructureMinerTelemetrySnapshot t) {
        drawMeter(
                c,
                g,
                0,
                0,
                "screen.dimension_tech.structure_miner.attribute.efficiency",
                StructureMinerScreen.formatDecimal(t.efficiencyHundredths()),
                StructureMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                1,
                0,
                "screen.dimension_tech.structure_miner.attribute.luck",
                StructureMinerScreen.formatDecimal(t.luckHundredths()),
                StructureMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                1,
                "screen.dimension_tech.structure_miner.work.base_parallel",
                Integer.toString(t.baseParallel()),
                StructureMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                1,
                1,
                "screen.dimension_tech.structure_miner.overview.total_parallel",
                Integer.toString(t.totalParallel()),
                StructureMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                2,
                "screen.dimension_tech.structure_miner.overview.equivalent_acceleration",
                Long.toString(c.menu().getExternalEquivalentAccelerationTicks()),
                StructureMinerTheme.AMBER);
        drawMeter(
                c,
                g,
                1,
                2,
                "screen.dimension_tech.structure_miner.attribute.upgrades",
                Integer.toString(c.menu().getTotalUpgradeCount()),
                StructureMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                3,
                "screen.dimension_tech.structure_miner.overview.active",
                t.workingThreads() + " / " + c.menu().getContainerSlotCount(),
                StructureMinerTheme.SUCCESS);
        drawMeter(
                c,
                g,
                1,
                3,
                "screen.dimension_tech.structure_miner.attribute.consumption",
                StructureMinerScreen.formatCompact(t.energyConsumption()) + " FE/t",
                StructureMinerTheme.AMBER);
    }

    private static void drawMeter(
            StructureMinerScreenContext c,
            GuiGraphics g,
            int column,
            int row,
            String labelKey,
            String value,
            int accent) {
        Font font = c.font();
        int x = StructureMinerInfoLayout.workMeterX(column);
        int y = StructureMinerInfoLayout.workMeterY(row);
        int width = StructureMinerInfoLayout.WORK_METER_COL_W;

        g.fill(
                x,
                y + 2,
                x + StructureMinerInfoLayout.WORK_METER_RAIL_W,
                y + StructureMinerInfoLayout.WORK_METER_ROW_H - 2,
                accent);

        int valueWidth = font.width(value);
        String label =
                font.plainSubstrByWidth(
                        Component.translatable(labelKey).getString(),
                        Math.max(1, width - 10 - valueWidth));
        g.drawString(font, label, x + 6, y + 4, StructureMinerTheme.INK, false);
        g.drawString(font, value, x + width - valueWidth, y + 4, accent, false);
    }

    /**
     * Tells the player how thread toggling works, drawn rather than hover-only.
     *
     * <p>Toggling used to live on the progress strip, so nothing on screen ever said so; after it
     * moved to the right mouse button the gesture is invisible without this line. Hover help alone
     * would only speak to players who already guessed there was something to hover.
     */
    private static void drawToggleHint(StructureMinerScreenContext c, GuiGraphics g) {
        Font font = c.font();
        Component hint =
                Component.translatable("screen.dimension_tech.structure_miner.hint.slot_toggle");
        g.drawString(
                font,
                font.plainSubstrByWidth(hint.getString(), StructureMinerInfoLayout.CONTENT_W),
                StructureMinerInfoLayout.CONTENT_X,
                StructureMinerInfoLayout.WORK_HINT_Y,
                StructureMinerTheme.DIM,
                false);
    }

    // --- status chips ------------------------------------------------------

    private static void drawStatusChips(
            StructureMinerScreenContext c, GuiGraphics g, StructureMinerTelemetrySnapshot t) {
        int x = StructureMinerInfoLayout.CONTENT_X;
        int y = StructureMinerInfoLayout.WORK_CHIP_Y;
        int width = StructureMinerInfoLayout.WORK_CHIP_W;
        int stride = width + StructureMinerInfoLayout.WORK_CHIP_GAP;

        BaseMinerBlockEntity.OutputState output = t.outputState();
        // A non-empty pending list is the machine's hard stop: canRunThisTick refuses every cycle
        // until the router drains it, so this state outranks the configured output mode.
        if (t.pendingCount() > 0) {
            StructureMinerTheme.statusChip(
                    g,
                    c.font(),
                    x,
                    y,
                    width,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.output.blocked", t.pendingCount()),
                    StructureMinerTheme.ERROR);
        } else {
            StructureMinerTheme.statusChip(
                    g,
                    c.font(),
                    x,
                    y,
                    width,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.output."
                                    + output.name().toLowerCase(Locale.ROOT)),
                    switch (output) {
                        case ME_NETWORK -> StructureMinerTheme.SUCCESS;
                        case ITEM_HANDLER -> StructureMinerTheme.FLUIX;
                        case NONE -> StructureMinerTheme.ERROR;
                    });
        }

        BaseMinerBlockEntity.RedstoneMode redstone = t.redstoneMode();
        StructureMinerTheme.statusChip(
                g,
                c.font(),
                x + stride,
                y,
                width,
                Component.translatable(
                        "screen.dimension_tech.structure_miner.redstone."
                                + redstone.name().toLowerCase(Locale.ROOT)),
                switch (redstone) {
                    case ALWAYS -> StructureMinerTheme.SUCCESS;
                    case SIGNAL, NO_SIGNAL -> StructureMinerTheme.AMBER;
                    case NEVER -> StructureMinerTheme.ERROR;
                });

        boolean complete = t.structureComplete();
        StructureMinerTheme.statusChip(
                g,
                c.font(),
                x + stride * 2,
                y,
                width,
                Component.translatable(
                        complete
                                ? "screen.dimension_tech.structure_miner.structure.complete"
                                : "screen.dimension_tech.structure_miner.structure_incomplete"),
                complete ? StructureMinerTheme.SUCCESS : StructureMinerTheme.ERROR);
    }

    // --- hover -------------------------------------------------------------

    /**
     * Tooltip pass for the lane.
     *
     * <p>Cells and strips are hit-tested by the shared layout arithmetic rather than by anything
     * recomputed here, so a tooltip can only appear over something that was actually drawn, and the
     * two targets stay disjoint because the strip band starts exactly where the cells end.
     */
    static void renderTooltip(
            StructureMinerScreenContext c,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        int count = c.menu().getContainerSlotCount();
        int slot = StructureMinerInfoLayout.markerSlotAt(x, y, count);
        if (slot >= 0) {
            StructureMinerTooltips.markerSlot(c, g, slot, screenX, screenY, true);
            return;
        }
        int strip = StructureMinerInfoLayout.markerProgressAt(x, y, count);
        if (strip >= 0) {
            StructureMinerTooltips.markerProgress(c, g, strip, screenX, screenY);
        }
    }
}
