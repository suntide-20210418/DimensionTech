package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerTelemetrySnapshot;
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
 * {@code MythicMinerMenu#addContainerSlots}.
 */
final class MythicMinerWorkPage {
    private MythicMinerWorkPage() {}

    static void render(MythicMinerScreenContext context, GuiGraphics g) {
        MythicMinerTelemetrySnapshot telemetry = context.menu().telemetrySnapshot();
        drawLane(context, g, telemetry);
        drawMeters(context, g, telemetry);
        drawToggleHint(context, g);
        drawStatusChips(context, g, telemetry);
    }

    // --- marker lane -------------------------------------------------------

    private static void drawLane(
            MythicMinerScreenContext c, GuiGraphics g, MythicMinerTelemetrySnapshot t) {
        int count = c.menu().getContainerSlotCount();
        for (int slot = 0; slot < count; slot++) {
            int x = MythicMinerInfoLayout.laneX(slot, count);
            int y = MythicMinerInfoLayout.MARKER_Y;
            ItemStack stack = c.menu().slots.get(slot).getItem();
            boolean configured = StructMarkerItem.getMarkerInfo(stack).isPresent();
            boolean enabled = t.markers().get(slot).enabled();

            MythicMinerSpriteRenderer.marker(
                    g,
                    x + MythicMinerInfoLayout.MARKER_FACE_INSET,
                    y + MythicMinerInfoLayout.MARKER_FACE_INSET,
                    slot == c.selectedMarkerSlot());

            if (!stack.isEmpty()) {
                g.renderItem(
                        stack,
                        x + MythicMinerInfoLayout.MARKER_ICON_INSET,
                        y + MythicMinerInfoLayout.MARKER_ICON_INSET);
            }

            if (slot == c.hoveredMarkerSlot()) {
                g.fill(
                        x,
                        y,
                        x + MythicMinerInfoLayout.MARKER_SIZE,
                        y + MythicMinerInfoLayout.MARKER_SIZE,
                        GuiPalette.withAlpha(GuiPalette.HILIGHT, 0x40));
            }
            if (configured && !enabled) {
                g.fill(
                        x,
                        y,
                        x + MythicMinerInfoLayout.MARKER_SIZE,
                        y + MythicMinerInfoLayout.MARKER_SIZE,
                        MythicMinerTheme.DISABLED_OVERLAY);
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
            MythicMinerTelemetrySnapshot t,
            int slot,
            int x,
            int y,
            boolean configured) {
        if (!configured) return;
        MythicMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int color =
                !marker.enabled()
                        ? MythicMinerTheme.ERROR
                        : marker.processingTime() <= 0 || marker.waitingForNaturalWindow()
                                ? MythicMinerTheme.AMBER
                                : MythicMinerTheme.FLUIX;
        int lampRight = x + MythicMinerInfoLayout.MARKER_SIZE - MythicMinerInfoLayout.MARKER_LAMP_RIGHT_INSET;
        int lampTop = y + MythicMinerInfoLayout.MARKER_LAMP_TOP_INSET;
        g.fill(
                lampRight - MythicMinerInfoLayout.MARKER_LAMP_SIZE,
                lampTop,
                lampRight,
                lampTop + MythicMinerInfoLayout.MARKER_LAMP_SIZE,
                color);
    }

    private static void drawProgressStrip(
            GuiGraphics g,
            MythicMinerTelemetrySnapshot t,
            int slot,
            int x,
            boolean configured,
            boolean enabled) {
        int barX = x + MythicMinerInfoLayout.MARKER_PROGRESS_INSET;
        int barY = MythicMinerInfoLayout.MARKER_PROGRESS_Y;
        int width = MythicMinerInfoLayout.MARKER_PROGRESS_W;
        int height = MythicMinerInfoLayout.MARKER_PROGRESS_H;

        if (!configured) {
            // An unconfigured cell reads as "no thread", which is not "a thread sitting at 0%".
            g.fill(barX, barY + 1, barX + width, barY + 2, MythicMinerTheme.PROGRESS_TRACK);
            return;
        }

        MythicMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int filled =
                MythicMinerProgressStrip.pixels(marker.progress(), marker.processingTime(), width);
        MythicMinerSpriteRenderer.progressStrip(g, barX, barY, width, enabled ? filled : 0);

        if (!enabled) {
            g.fill(barX, barY, barX + width, barY + 1, MythicMinerTheme.ERROR);
            g.fill(barX, barY + height - 1, barX + width, barY + height, MythicMinerTheme.ERROR);
        } else if (marker.waitingForNaturalWindow()) {
            // The strip is colour-locked to its sprite, so a side mark carries the throttled state.
            g.fill(
                    barX + width - MythicMinerInfoLayout.MARKER_PROGRESS_END_MARK_W,
                    barY,
                    barX + width,
                    barY + height,
                    MythicMinerTheme.AMBER);
        }
    }

    // --- meter grid --------------------------------------------------------

    private static void drawMeters(
            MythicMinerScreenContext c, GuiGraphics g, MythicMinerTelemetrySnapshot t) {
        drawMeter(
                c,
                g,
                0,
                0,
                "screen.dimension_tech.mythic_miner.attribute.efficiency",
                MythicMinerScreen.formatDecimal(t.efficiencyHundredths()),
                MythicMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                1,
                0,
                "screen.dimension_tech.mythic_miner.attribute.luck",
                MythicMinerScreen.formatDecimal(t.luckHundredths()),
                MythicMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                1,
                "screen.dimension_tech.mythic_miner.work.base_parallel",
                Integer.toString(t.baseParallel()),
                MythicMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                1,
                1,
                "screen.dimension_tech.mythic_miner.overview.total_parallel",
                Integer.toString(t.totalParallel()),
                MythicMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                2,
                "screen.dimension_tech.mythic_miner.overview.equivalent_acceleration",
                Long.toString(c.menu().getExternalEquivalentAccelerationTicks()),
                MythicMinerTheme.AMBER);
        drawMeter(
                c,
                g,
                1,
                2,
                "screen.dimension_tech.mythic_miner.attribute.upgrades",
                Integer.toString(c.menu().getTotalUpgradeCount()),
                MythicMinerTheme.FLUIX);
        drawMeter(
                c,
                g,
                0,
                3,
                "screen.dimension_tech.mythic_miner.overview.active",
                t.workingThreads() + " / " + c.menu().getContainerSlotCount(),
                MythicMinerTheme.SUCCESS);
        drawMeter(
                c,
                g,
                1,
                3,
                "screen.dimension_tech.mythic_miner.attribute.consumption",
                MythicMinerScreen.formatCompact(t.energyConsumption()) + " FE/t",
                MythicMinerTheme.AMBER);
    }

    private static void drawMeter(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int column,
            int row,
            String labelKey,
            String value,
            int accent) {
        Font font = c.font();
        int x = MythicMinerInfoLayout.workMeterX(column);
        int y = MythicMinerInfoLayout.workMeterY(row);
        int width = MythicMinerInfoLayout.WORK_METER_COL_W;

        g.fill(
                x,
                y + 2,
                x + MythicMinerInfoLayout.WORK_METER_RAIL_W,
                y + MythicMinerInfoLayout.WORK_METER_ROW_H - 2,
                accent);

        int valueWidth = font.width(value);
        String label =
                font.plainSubstrByWidth(
                        Component.translatable(labelKey).getString(),
                        Math.max(1, width - 10 - valueWidth));
        g.drawString(font, label, x + 6, y + 4, MythicMinerTheme.INK, false);
        g.drawString(font, value, x + width - valueWidth, y + 4, accent, false);
    }

    /**
     * Tells the player how thread toggling works, drawn rather than hover-only.
     *
     * <p>Toggling used to live on the progress strip, so nothing on screen ever said so; after it
     * moved to the right mouse button the gesture is invisible without this line. Hover help alone
     * would only speak to players who already guessed there was something to hover.
     */
    private static void drawToggleHint(MythicMinerScreenContext c, GuiGraphics g) {
        Font font = c.font();
        Component hint =
                Component.translatable("screen.dimension_tech.mythic_miner.hint.slot_toggle");
        g.drawString(
                font,
                font.plainSubstrByWidth(hint.getString(), MythicMinerInfoLayout.CONTENT_W),
                MythicMinerInfoLayout.CONTENT_X,
                MythicMinerInfoLayout.WORK_HINT_Y,
                MythicMinerTheme.DIM,
                false);
    }

    // --- status chips ------------------------------------------------------

    private static void drawStatusChips(
            MythicMinerScreenContext c, GuiGraphics g, MythicMinerTelemetrySnapshot t) {
        int x = MythicMinerInfoLayout.CONTENT_X;
        int y = MythicMinerInfoLayout.WORK_CHIP_Y;
        int width = MythicMinerInfoLayout.WORK_CHIP_W;
        int stride = width + MythicMinerInfoLayout.WORK_CHIP_GAP;

        BaseMinerBlockEntity.OutputState output = t.outputState();
        // A non-empty pending list is the machine's hard stop: canRunThisTick refuses every cycle
        // until the router drains it, so this state outranks the configured output mode.
        if (t.pendingCount() > 0) {
            MythicMinerTheme.statusChip(
                    g,
                    c.font(),
                    x,
                    y,
                    width,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output.blocked", t.pendingCount()),
                    MythicMinerTheme.ERROR);
        } else {
            MythicMinerTheme.statusChip(
                    g,
                    c.font(),
                    x,
                    y,
                    width,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.output."
                                    + output.name().toLowerCase(Locale.ROOT)),
                    switch (output) {
                        case ME_NETWORK -> MythicMinerTheme.SUCCESS;
                        case ITEM_HANDLER -> MythicMinerTheme.FLUIX;
                        case NONE -> MythicMinerTheme.ERROR;
                    });
        }

        BaseMinerBlockEntity.RedstoneMode redstone = t.redstoneMode();
        MythicMinerTheme.statusChip(
                g,
                c.font(),
                x + stride,
                y,
                width,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.redstone."
                                + redstone.name().toLowerCase(Locale.ROOT)),
                switch (redstone) {
                    case ALWAYS -> MythicMinerTheme.SUCCESS;
                    case SIGNAL, NO_SIGNAL -> MythicMinerTheme.AMBER;
                    case NEVER -> MythicMinerTheme.ERROR;
                });

        boolean complete = t.structureComplete();
        MythicMinerTheme.statusChip(
                g,
                c.font(),
                x + stride * 2,
                y,
                width,
                Component.translatable(
                        complete
                                ? "screen.dimension_tech.mythic_miner.structure.complete"
                                : "screen.dimension_tech.mythic_miner.structure_incomplete"),
                complete ? MythicMinerTheme.SUCCESS : MythicMinerTheme.ERROR);
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
            MythicMinerScreenContext c,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        int count = c.menu().getContainerSlotCount();
        int slot = MythicMinerInfoLayout.markerSlotAt(x, y, count);
        if (slot >= 0) {
            MythicMinerTooltips.markerSlot(c, g, slot, screenX, screenY);
            return;
        }
        int strip = MythicMinerInfoLayout.markerProgressAt(x, y, count);
        if (strip >= 0) {
            MythicMinerTooltips.markerProgress(c, g, strip, screenX, screenY);
        }
    }
}
