package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerTelemetrySnapshot;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * INFO page: one flat scrolling viewport describing the selected thread.
 *
 * <p>There is no collapsing here on purpose. Section heights come from a single {@link #contentHeight}
 * function rather than from per-section offset helpers, which is what made the previous incarnation
 * drift out of sync with its own hit-testing: moving one section silently shifted every section below
 * it and nothing noticed.
 *
 * <p>The dual bars are drawn from one telemetry snapshot. Two bars exist only so they can be compared,
 * so sampling them at different moments would defeat the point.
 */
final class MythicMinerInfoPage {
    /** Height of the thread summary line that opens the viewport. */
    private static final int SUMMARY_H = 12;

    /** Rows the marker section carries: dimension, then the two analysis values. */
    private static final int MARKER_SECTION_ROWS = 3;

    /** Breathing room between a progress strip and whatever follows it. */
    private static final int STRIP_GAP = 2;

    /**
     * A strip plus its gap. The sprite owns its own height, so only the rhythm is typed here.
     */
    private static final int STRIP_H = MythicMinerSpriteRenderer.PROGRESS_H + STRIP_GAP;

    /** A tick line plus the strip under it. */
    private static final int TICK_BLOCK_H = MythicMinerInfoLayout.ROW_H_DATA + STRIP_H;

    private static final int DETAIL_H = 11;

    /** One labelled detail row: its own text box plus a pixel of separation. */
    private static final int DETAIL_ROW_STRIDE = DETAIL_H + 1;

    private MythicMinerInfoPage() {}

    static void render(MythicMinerScreenContext c, GuiGraphics g) {
        MythicMinerTelemetrySnapshot telemetry = c.menu().telemetrySnapshot();
        drawSelector(c, g);

        int viewportX = MythicMinerInfoLayout.INFO_LIST_X;
        int viewportY = MythicMinerInfoLayout.INFO_LIST_Y;
        int viewportW = MythicMinerInfoLayout.INFO_LIST_W;
        int viewportH = MythicMinerInfoLayout.INFO_LIST_H;

        int selected = c.selectedMarkerSlot();
        if (selected < 0) {
            MythicMinerTheme.emptyState(
                    g,
                    c.font(),
                    viewportX,
                    viewportY,
                    viewportW,
                    viewportH,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.select"));
            return;
        }

        int contentHeight = contentHeight(c, telemetry, selected);
        c.markerInfoContentHeight(contentHeight);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        int scroll = Math.max(0, Math.min(c.markerInfoScroll(), maxScroll));
        c.markerInfoScroll(scroll);

        MythicMinerLayout.ScissorBounds scissor =
                MythicMinerLayout.scaleToScreen(
                        c.leftPos() + viewportX,
                        c.topPos() + viewportY,
                        viewportW,
                        viewportH,
                        c.uiScale());
        g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());

        int x = viewportX + MythicMinerInfoLayout.VIEWPORT_PAD;
        int width = viewportW - 2 * MythicMinerInfoLayout.VIEWPORT_PAD;
        int y = viewportY - scroll;

        y = drawSummary(c, g, telemetry, selected, x, y, width);
        y = drawMarkerSection(c, g, selected, x, y, width);
        y = drawWorkSection(c, g, telemetry, selected, x, y, width);
        drawProductSection(c, g, x, y, width);

        g.disableScissor();

        MythicMinerTheme.scrollbar(
                g,
                MythicMinerInfoLayout.SCROLLBAR_X,
                viewportY,
                viewportH,
                contentHeight,
                viewportH,
                scroll);
    }

    // --- thread selector ---------------------------------------------------

    /**
     * One 20x20 sprite button per thread, drawn from the sheet rather than assembled from fills so the
     * control keeps the same lighting as every other button in the console.
     *
     * <p>The button is dimmed when its slot holds no marker: the number alone cannot say whether a
     * thread exists, and that is the first thing the player needs from this row.
     */
    private static void drawSelector(MythicMinerScreenContext c, GuiGraphics g) {
        int count = c.menu().getContainerSlotCount();
        for (int slot = 0; slot < count; slot++) {
            int x = MythicMinerInfoLayout.laneX(slot, count);
            int y = MythicMinerInfoLayout.MARKER_Y;
            int size = MythicMinerInfoLayout.MARKER_SIZE;
            boolean configured =
                    StructMarkerItem.getMarkerInfo(c.menu().slots.get(slot).getItem()).isPresent();
            boolean selected = slot == c.selectedMarkerSlot();

            MythicMinerSpriteRenderer.smallButton(
                    g, x, y, slot == c.hoveredMarkerSlot(), selected);
            if (!configured) {
                g.fill(x, y, x + size, y + size, MythicMinerTheme.DISABLED_OVERLAY);
            }

            String label = Integer.toString(slot + 1);
            g.drawString(
                    c.font(),
                    label,
                    x + (size - c.font().width(label)) / 2,
                    y + (size - 8) / 2,
                    MythicMinerTheme.INK,
                    false);
        }

        int selected = c.selectedMarkerSlot();
        if (selected >= 0 && selected < count) {
            int x = MythicMinerInfoLayout.laneX(selected, count);
            g.fill(
                    x,
                    MythicMinerInfoLayout.MARKER_PROGRESS_Y,
                    x + MythicMinerInfoLayout.MARKER_SIZE,
                    MythicMinerInfoLayout.MARKER_PROGRESS_Y + 1,
                    MythicMinerTheme.FLUIX);
        }
    }

    // --- sections ----------------------------------------------------------

    private static int drawSummary(
            MythicMinerScreenContext c,
            GuiGraphics g,
            MythicMinerTelemetrySnapshot t,
            int slot,
            int x,
            int y,
            int width) {
        Font font = c.font();
        ItemStack stack = c.menu().slots.get(slot).getItem();
        Component name =
                StructMarkerItem.getMarkerInfo(stack)
                        .map(info -> TranslateHelper.structureName(info.structure().id()))
                        .orElse(
                                Component.translatable(
                                        "screen.dimension_tech.mythic_miner.marker_info.unconfigured"));
        Component header =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.structure", slot + 1, name);

        String total =
                Component.translatable(
                                                "screen.dimension_tech.mythic_miner.overview.total_parallel")
                                        .getString()
                        + " "
                        + t.totalParallel();
        int totalWidth = font.width(total);
        g.drawString(
                font,
                font.plainSubstrByWidth(header.getString(), Math.max(1, width - totalWidth - 6)),
                x,
                y,
                MythicMinerTheme.INK,
                false);
        g.drawString(font, total, x + width - totalWidth, y, MythicMinerTheme.FLUIX, false);
        return y + SUMMARY_H;
    }

    private static int drawMarkerSection(
            MythicMinerScreenContext c, GuiGraphics g, int slot, int x, int y, int width) {
        Font font = c.font();
        y = sectionHeader(c, g, x, y, width, "screen.dimension_tech.mythic_miner.info.section.marker");

        ItemStack stack = c.menu().slots.get(slot).getItem();
        StructMarkerItem.MarkerInfo info = StructMarkerItem.getMarkerInfo(stack).orElse(null);
        if (info == null) {
            g.drawString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.unconfigured"),
                    x,
                    y,
                    MythicMinerTheme.DIM,
                    false);
            return y + MythicMinerInfoLayout.ROW_H_DATA * MARKER_SECTION_ROWS;
        }

        g.drawString(
                font,
                clip(
                        font,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.dimension",
                                TranslateHelper.dimensionName(info.dimension())),
                        width),
                x,
                y,
                MythicMinerTheme.INK,
                false);
        y += MythicMinerInfoLayout.ROW_H_DATA;

        y =
                drawAnalysisRow(
                        c,
                        g,
                        "screen.dimension_tech.mythic_miner.marker_info.dimension_value",
                        c.effectiveDimensionValue(),
                        x,
                        y,
                        width,
                        MythicMinerTheme.FLUIX);
        y =
                drawAnalysisRow(
                        c,
                        g,
                        "screen.dimension_tech.mythic_miner.marker_info.structure_value",
                        c.effectiveStructureValue(),
                        x,
                        y,
                        width,
                        MythicMinerTheme.AMBER);
        return y;
    }

    /**
     * Analysis arrives asynchronously, so an unloaded value must read as "loading" rather than as
     * zero — zero is a legitimate result the player would otherwise mistake for a real reading.
     */
    private static int drawAnalysisRow(
            MythicMinerScreenContext c,
            GuiGraphics g,
            String key,
            double value,
            int x,
            int y,
            int width,
            int color) {
        Font font = c.font();
        boolean ready = c.markerAnalysisReady();
        Component text =
                ready
                        ? Component.translatable(key, MythicMinerScreen.formatAnalysisValue(value))
                        : Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.loading");
        g.drawString(
                font,
                clip(font, text, width),
                x,
                y,
                ready ? color : MythicMinerTheme.DIM,
                false);
        return y + MythicMinerInfoLayout.ROW_H_DATA;
    }

    private static int drawWorkSection(
            MythicMinerScreenContext c,
            GuiGraphics g,
            MythicMinerTelemetrySnapshot t,
            int slot,
            int x,
            int y,
            int width) {
        Font font = c.font();
        y = sectionHeader(c, g, x, y, width, "screen.dimension_tech.mythic_miner.info.section.work");

        MythicMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int cycle = Math.max(1, marker.processingTime());
        boolean accelerated = marker.actualTicks() != marker.naturalTicks();

        g.drawString(
                font,
                clip(
                        font,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_progress",
                                marker.naturalTicks(),
                                marker.processingTime()),
                        width),
                x,
                y,
                MythicMinerTheme.INK,
                false);
        y += MythicMinerInfoLayout.ROW_H_DATA;
        progressStrip(g, x, y, width, marker.naturalTicks(), cycle);
        y += STRIP_H;

        if (accelerated) {
            g.drawString(
                    font,
                    clip(
                            font,
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.actual_progress",
                                    c.menu().getMarkerActualProgress(slot),
                                    marker.processingTime(),
                                    c.menu().getMarkerActualCycleCount(slot)),
                            width),
                    x,
                    y,
                    MythicMinerTheme.SUCCESS,
                    false);
            y += MythicMinerInfoLayout.ROW_H_DATA;
            progressStrip(g, x, y, width, c.menu().getMarkerActualProgress(slot), cycle);
            y += STRIP_H;
        }

        y = drawParallelBreakdown(c, g, slot, x, y, width, t, accelerated);

        if (marker.waitingForNaturalWindow()) {
            g.drawString(
                    font,
                    clip(
                            font,
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.waiting_for_natural_window"),
                            width),
                    x,
                    y,
                    MythicMinerTheme.AMBER,
                    false);
            y += MythicMinerInfoLayout.ROW_H_DATA;
        }
        return y;
    }

    /**
     * One line of text, then the labelled rows that explain it.
     *
     * <p>The previous stacked bar encoded the same three addends as lengths. Length is the wrong
     * encoding here: the segments were proportional to a total the player cannot otherwise see, so
     * the bar could only be read by measuring it against something. Stating the thread's parallel
     * against the machine's answers "how much of this machine is this thread" directly, and the
     * rows underneath still carry the breakdown.
     */
    private static int drawParallelBreakdown(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int slot,
            int x,
            int y,
            int width,
            MythicMinerTelemetrySnapshot t,
            boolean accelerated) {
        long base = t.baseParallel();
        long efficiency = c.menu().getMarkerExtraEfficiencyParallel(slot);
        double external = c.menu().getMarkerExternalAccelerationParallelHundredths(slot) / 100.0D;
        long total = t.markers().get(slot).parallel();

        Font font = c.font();
        g.drawString(
                font,
                clip(
                        font,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.parallel_status",
                                total,
                                t.totalParallel()),
                        width),
                x,
                y,
                MythicMinerTheme.INK,
                false);
        y += MythicMinerInfoLayout.ROW_H_DATA;

        y =
                detailRow(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.base",
                        base,
                        MythicMinerTheme.FLUIX);
        y =
                detailRow(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                        efficiency,
                        MythicMinerTheme.AMBER);
        if (accelerated) {
            y =
                    detailRow(
                            c,
                            g,
                            x,
                            y,
                            width,
                            "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                            MythicMinerScreen.formatRatio(external),
                            MythicMinerTheme.SUCCESS);
        }
        return detailRow(
                c,
                g,
                x,
                y,
                width,
                "screen.dimension_tech.mythic_miner.marker_info.previous_cycle_parallel",
                MythicMinerScreen.formatDecimal(
                        c.menu().getMarkerPreviousExternalAccelerationParallelHundredths(slot)),
                MythicMinerTheme.MUTED);
    }

    private static int detailRow(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            String key,
            Object value,
            int color) {
        Font font = c.font();
        g.fill(x, y + 1, x + 2, y + DETAIL_H - 3, color);
        g.drawString(
                font,
                clip(font, Component.translatable(key, value), width - 6),
                x + 6,
                y,
                color == MythicMinerTheme.MUTED ? MythicMinerTheme.DIM : MythicMinerTheme.INK,
                false);
        return y + DETAIL_ROW_STRIDE;
    }

    private static int drawProductSection(
            MythicMinerScreenContext c, GuiGraphics g, int x, int y, int width) {
        Font font = c.font();
        y =
                sectionHeader(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.mythic_miner.info.section.products");

        List<MythicMinerScreen.ExpectedItemRow> rows = c.expectedItemRows();
        if (rows.isEmpty()) {
            g.drawString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.no_items"),
                    x,
                    y,
                    MythicMinerTheme.DIM,
                    false);
            return y + MythicMinerInfoLayout.ROW_H_INTERACTIVE;
        }

        for (int index = 0; index < rows.size(); index++) {
            int rowY = y + index * MythicMinerInfoLayout.ROW_H_INTERACTIVE;
            MythicMinerScreen.ExpectedItemRow row = rows.get(index);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(row.item());
            boolean disabled = itemId != null && c.disabledExpectedItems().contains(itemId);
            ItemStack stack = new ItemStack(row.item());
            String expected = MythicMinerScreen.formatExpectedValue(row.expected());
            int expectedWidth = font.width(expected);

            if (index % 2 != 0) {
                g.fill(x - 1, rowY - 1, x + width, rowY + 18, MythicMinerTheme.STRIPE_INK);
            }
            if (disabled) {
                g.fill(x - 2, rowY - 2, x + width, rowY + 18, MythicMinerTheme.DISABLED_OVERLAY);
                g.fill(x - 2, rowY - 2, x, rowY + 18, MythicMinerTheme.ERROR);
            }
            g.renderItem(stack, x, rowY);
            g.drawString(
                    font,
                    font.plainSubstrByWidth(
                            stack.getHoverName().getString(),
                            Math.max(1, width - expectedWidth - 26)),
                    x + 22,
                    rowY + 4,
                    disabled ? MythicMinerTheme.ERROR : MythicMinerTheme.INK,
                    false);
            g.drawString(
                    font,
                    expected,
                    x + width - expectedWidth,
                    rowY + 4,
                    disabled ? MythicMinerTheme.ERROR : MythicMinerTheme.FLUIX,
                    false);
            if (disabled) {
                g.fill(x, rowY + 9, x + width - expectedWidth - 3, rowY + 10, MythicMinerTheme.ERROR);
            }
        }
        return y + rows.size() * MythicMinerInfoLayout.ROW_H_INTERACTIVE;
    }

    // --- shared helpers ----------------------------------------------------

    private static int sectionHeader(
            MythicMinerScreenContext c, GuiGraphics g, int x, int y, int width, String key) {
        MythicMinerTheme.sectionHeader(
                g, c.font(), x, y, width, Component.translatable(key), MythicMinerTheme.FLUIX);
        return y + MythicMinerInfoLayout.SECTION_HEADER_H;
    }

    /**
     * A full-width progress strip assembled from the two spritesheet halves rather than from fills:
     * the drained track stitched out to {@code width}, then the energised cover clipped over it from
     * the left to whatever fraction {@code value / total} comes to.
     *
     * <p>The old two-tone version took a colour, which let the held/actual pair be told apart by
     * hue. The sheet owns the colours now, so the two strips are distinguished by the labels above
     * them instead — see {@link #drawWorkSection}.
     */
    private static void progressStrip(
            GuiGraphics g, int x, int y, int width, long value, long total) {
        MythicMinerSpriteRenderer.stitchTo(
                g,
                MythicMinerSpriteRenderer.PROGRESS_TRACK_FRAGMENT,
                x,
                y,
                MythicMinerSpriteRenderer.StitchDirection.HORIZONTAL,
                width,
                false);
        if (total <= 0 || value <= 0) return;
        int filled = (int) Math.min(width, Math.min(value, total) * width / total);
        if (filled <= 0) return;
        MythicMinerSpriteRenderer.stitchTo(
                g,
                MythicMinerSpriteRenderer.PROGRESS_FILL_FRAGMENT,
                x,
                y,
                MythicMinerSpriteRenderer.StitchDirection.HORIZONTAL,
                filled,
                true);
    }

    private static Component clip(Font font, Component text, int width) {
        return Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(1, width)));
    }

    /**
     * The single source of truth for the page height. Every section offset is derived from the same
     * terms, so the drawn content and the scroll range cannot disagree.
     */
    static int contentHeight(MythicMinerScreenContext c, MythicMinerTelemetrySnapshot t, int slot) {
        int height = SUMMARY_H;
        height +=
                MythicMinerInfoLayout.SECTION_HEADER_H
                        + MARKER_SECTION_ROWS * MythicMinerInfoLayout.ROW_H_DATA;
        height += MythicMinerInfoLayout.SECTION_HEADER_H + workSectionHeight(t, slot);
        height +=
                MythicMinerInfoLayout.SECTION_HEADER_H
                        + Math.max(
                                MythicMinerInfoLayout.ROW_H_INTERACTIVE,
                                c.expectedItemRows().size()
                                        * MythicMinerInfoLayout.ROW_H_INTERACTIVE);
        return height;
    }

    /**
     * Exactly what {@link #drawWorkSection} advances {@code y} by — no padding term, because these
     * two numbers feed {@link #productRowAt}'s hit test as well as the scroll range, and either use
     * breaks by the pad in pixels it thinks the drawing has.
     */
    private static int workSectionHeight(MythicMinerTelemetrySnapshot t, int slot) {
        MythicMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        boolean accelerated = marker.actualTicks() != marker.naturalTicks();
        int height = accelerated ? 2 * TICK_BLOCK_H : TICK_BLOCK_H;
        // One headline line, then a row per contribution. The external row only appears under
        // external acceleration, so it has to be counted here too: leaving it out is what made the
        // old height ten pixels short, which clipped the scroll range and shifted every product row.
        height += MythicMinerInfoLayout.ROW_H_DATA;
        height += (accelerated ? 4 : 3) * DETAIL_ROW_STRIDE;
        if (marker.waitingForNaturalWindow()) height += MythicMinerInfoLayout.ROW_H_DATA;
        return height;
    }

    /**
     * Product-row index under a panel-local point, or {@code -1}.
     *
     * <p>Derived from the same terms {@link #contentHeight} uses, so the clickable strip and the drawn
     * strip cannot drift apart.
     */
    static int productRowAt(MythicMinerScreenContext c, double x, double y) {
        int slot = c.selectedMarkerSlot();
        if (slot < 0) return -1;
        int top = productSectionTop(c, slot);
        int index = (int) Math.floor((y - top) / (double) MythicMinerInfoLayout.ROW_H_INTERACTIVE);
        if (index < 0 || index >= c.expectedItemRows().size()) return -1;
        int left = MythicMinerInfoLayout.INFO_LIST_X + MythicMinerInfoLayout.VIEWPORT_PAD;
        int width = MythicMinerInfoLayout.INFO_LIST_W - 2 * MythicMinerInfoLayout.VIEWPORT_PAD;
        return MythicMinerInfoLayout.inside(
                        x,
                        y,
                        left,
                        top + index * MythicMinerInfoLayout.ROW_H_INTERACTIVE,
                        width,
                        MythicMinerInfoLayout.ROW_H_INTERACTIVE)
                ? index
                : -1;
    }

    private static int productSectionTop(MythicMinerScreenContext c, int slot) {
        return MythicMinerInfoLayout.INFO_LIST_Y
                - c.markerInfoScroll()
                + SUMMARY_H
                + MythicMinerInfoLayout.SECTION_HEADER_H
                + MARKER_SECTION_ROWS * MythicMinerInfoLayout.ROW_H_DATA
                + MythicMinerInfoLayout.SECTION_HEADER_H
                + workSectionHeight(c.menu().telemetrySnapshot(), slot)
                + MythicMinerInfoLayout.SECTION_HEADER_H;
    }

    /** True when a panel-local point falls inside the scrolling viewport. */
    /**
     * Hover pass: the thread selector, which shares the work lane's grid, and the product rows.
     *
     * <p>Product rows reuse {@link #productRowAt}, the same hit test the click path uses, so a
     * tooltip cannot appear over a row that would not respond to a click.
     */
    static void renderTooltip(
            MythicMinerScreenContext c,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        int slot = MythicMinerInfoLayout.markerSlotAt(x, y, c.menu().getContainerSlotCount());
        if (slot >= 0) {
            MythicMinerTooltips.markerSlot(c, g, slot, screenX, screenY);
            return;
        }

        List<MythicMinerScreen.ExpectedItemRow> rows = c.expectedItemRows();
        int row = productRowAt(c, x, y);
        if (row < 0 || row >= rows.size()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(rows.get(row).item());
        boolean disabled = itemId != null && c.disabledExpectedItems().contains(itemId);
        g.renderTooltip(
                c.font(),
                List.of(
                        new ItemStack(rows.get(row).item()).getHoverName(),
                        Component.translatable(
                                disabled
                                        ? "screen.dimension_tech.mythic_miner.expected_item.enable"
                                        : "screen.dimension_tech.mythic_miner.expected_item.disable")),
                Optional.empty(),
                screenX,
                screenY);
    }
}
