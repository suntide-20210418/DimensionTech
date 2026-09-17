package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerTelemetrySnapshot;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip bodies for the marker lane, which the work page's slot row and the info page's thread
 * selector both draw from the same grid.
 *
 * <p>Every number comes from the getter the pages themselves draw with, so a hovered value cannot
 * disagree with a printed one. The two bodies answer different questions on purpose: a cell answers
 * "what is in here", the strip under it answers "how far along is it, and where does its parallelism
 * come from". The second would not fit anywhere on a 20px cell, so it is hover-only rather than a
 * third collapsible control.
 *
 * <p>Units are load-bearing. Total and efficiency parallelism are whole units; external acceleration
 * parallel is stored in hundredths and must go through {@link MythicMinerScreen#formatDecimal}.
 */
final class MythicMinerTooltips {
    private MythicMinerTooltips() {}

    /** Slot contents, where the marker points, and what a click will do. */
    static void markerSlot(
            MythicMinerScreenContext c, GuiGraphics g, int slot, int screenX, int screenY) {
        markerSlot(c, g, slot, screenX, screenY, false);
    }

    /**
     * Slot contents, where the marker points, and what a click will do.
     *
     * <p>{@code rightClickToggle} is true on the work page, where right-click is the gesture that
     * flips a thread's enabled flag; the info-page selector reuses the same grid but only selects, so
     * it keeps the plain enable/disable state without the misleading right-click instruction.
     */
    static void markerSlot(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int slot,
            int screenX,
            int screenY,
            boolean rightClickToggle) {
        MythicMinerMenu menu = c.menu();
        ItemStack stack = menu.slots.get(slot).getItem();
        Optional<StructMarkerItem.MarkerInfo> info = StructMarkerItem.getMarkerInfo(stack);
        List<Component> lines = new ArrayList<>(4);

        Component name =
                info.map(
                                markerInfo ->
                                        TranslateHelper.structureName(markerInfo.structure().id()))
                        .orElse(
                                Component.translatable(
                                        "screen.dimension_tech.mythic_miner.marker_info.unconfigured"));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.structure", slot + 1, name));

        info.ifPresent(
                markerInfo -> lines.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.dimension",
                                TranslateHelper.dimensionName(markerInfo.dimension()))));

        // A configured, enabled slot with no plan is the "looks ready but never advances" state:
        // the analysis either has not landed yet or keeps failing, and nothing else says so. Stated
        // as its own line rather than inferred from the zero in a progress readout, because zero is
        // a legitimate value elsewhere on this screen.
        MythicMinerTelemetrySnapshot telemetry = menu.telemetrySnapshot();
        if (info.isPresent() && slot >= 0 && slot < telemetry.markers().size()) {
            MythicMinerTelemetrySnapshot.Marker marker = telemetry.markers().get(slot);
            if (marker.enabled() && marker.processingTime() <= 0) {
                lines.add(
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker.no_plan"));
            }
        }

        lines.add(
                Component.translatable(
                        rightClickToggle
                                ? (menu.isMarkerSlotEnabled(slot)
                                        ? "screen.dimension_tech.mythic_miner.slot.disable_right"
                                        : "screen.dimension_tech.mythic_miner.slot.enable_right")
                                : (menu.isMarkerSlotEnabled(slot)
                                        ? "screen.dimension_tech.mythic_miner.slot.disable"
                                        : "screen.dimension_tech.mythic_miner.slot.enable")));
        g.renderTooltip(c.font(), lines, Optional.empty(), screenX, screenY);
    }

    /**
     * Progress, both tick counts, and the three-way parallelism breakdown that explains the total.
     *
     * <p>The parallelism total is spelled out by hand rather than through {@code
     * marker_info.parallel}, whose second placeholder carries a state label this screen has no
     * message for — filling it with a guess would print nonsense.
     */
    static void markerProgress(
            MythicMinerScreenContext c, GuiGraphics g, int slot, int screenX, int screenY) {
        MythicMinerMenu menu = c.menu();
        List<MythicMinerTelemetrySnapshot.Marker> markers = menu.telemetrySnapshot().markers();
        if (slot < 0 || slot >= markers.size()) return;
        MythicMinerTelemetrySnapshot.Marker marker = markers.get(slot);

        List<Component> lines = new ArrayList<>(8);
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_progress",
                        marker.progress(),
                        marker.processingTime()));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                        marker.naturalTicks()));
        // The actual count only differs under external acceleration. Printing it unconditionally
        // would suggest the machine is being accelerated when nothing is touching it.
        if (marker.actualTicks() != marker.naturalTicks()) {
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.actual_ticks",
                            marker.actualTicks()));
        }

        lines.add(
                Component.literal(
                        Component.translatable(
                                                "screen.dimension_tech.mythic_miner.overview.total_parallel")
                                        .getString()
                                + " "
                                + menu.getMarkerTotalParallel(slot)));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.base",
                        menu.getBaseParallel()));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                        menu.getMarkerExtraEfficiencyParallel(slot)));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                        MythicMinerScreen.formatDecimal(
                                menu.getMarkerExternalAccelerationParallelHundredths(slot))));

        if (marker.processingTime() <= 0) {
            lines.add(
                    Component.translatable("screen.dimension_tech.mythic_miner.marker.no_plan"));
        }
        if (marker.waitingForNaturalWindow()) {
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.waiting_for_natural_window"));
        }
        lines.add(toggleHint(menu.isMarkerSlotEnabled(slot)));
        g.renderTooltip(c.font(), lines, Optional.empty(), screenX, screenY);
    }

    private static Component toggleHint(boolean enabled) {
        return Component.translatable(
                enabled
                        ? "screen.dimension_tech.mythic_miner.slot.disable"
                        : "screen.dimension_tech.mythic_miner.slot.enable");
    }
}
