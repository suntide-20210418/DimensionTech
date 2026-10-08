package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerTelemetrySnapshot;
import com.suntide_20210418.dimensiontech.item.ItemValueFacade;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
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
 * <p>There is no collapsing here on purpose. Section heights come from a single {@link
 * #contentHeight} function rather than from per-section offset helpers, which is what made the
 * previous incarnation drift out of sync with its own hit-testing: moving one section silently
 * shifted every section below it and nothing noticed.
 *
 * <p>The dual bars are drawn from one telemetry snapshot. Two bars exist only so they can be
 * compared, so sampling them at different moments would defeat the point.
 */
final class StructureMinerInfoPage {
    /** Height of the thread summary line that opens the viewport. */
    private static final int SUMMARY_H = 12;

    /** Rows the marker section carries: dimension, then the two analysis values. */
    private static final int MARKER_SECTION_ROWS = 3;

    /** Breathing room between a progress strip and whatever follows it. */
    private static final int STRIP_GAP = 2;

    /** A strip plus its gap. The sprite owns its own height, so only the rhythm is typed here. */
    private static final int STRIP_H = StructureMinerSpriteRenderer.PROGRESS_H + STRIP_GAP;

    /** A tick line plus the strip under it. */
    private static final int TICK_BLOCK_H = StructureMinerInfoLayout.ROW_H_DATA + STRIP_H;

    private static final int DETAIL_H = 11;

    /** One labelled detail row: its own text box plus a pixel of separation. */
    private static final int DETAIL_ROW_STRIDE = DETAIL_H + 1;

    /**
     * Cells across the product grid.
     *
     * <p>Seven, not nine: this grid shares its column with the page's own scrollbar, and the
     * content column is 182 wide where the marker viewport is 186. Seven 18px cells and six 2px
     * gutters come to 138, leaving the row's label space beside it. The grid here is not the whole
     * section — the section header and the disabled legend still own the left of the row.
     */
    private static final int PRODUCT_COLUMNS = 7;

    /**
     * One grid instance for the whole page.
     *
     * <p>{@code scrollable = false} on purpose. This grid lives inside the page's own scrolling
     * viewport: the product section is measured into {@link #contentHeight} and moves with the
     * page's scroll offset, so a second scroll region nested inside it would have to negotiate the
     * wheel, the keyboard and a drag bar with the outer one. There is nothing for it to buy — the
     * page already scrolls to whatever the grid is tall enough to need.
     */
    private static final ItemExpectationGrid GRID =
            ItemExpectationGrid.ofSpriteSlots(PRODUCT_COLUMNS, false);

    private StructureMinerInfoPage() {}

    static void render(StructureMinerScreenContext c, GuiGraphics g) {
        StructureMinerTelemetrySnapshot telemetry = c.menu().telemetrySnapshot();
        drawSelector(c, g);

        int viewportX = StructureMinerInfoLayout.INFO_LIST_X;
        int viewportY = StructureMinerInfoLayout.INFO_LIST_Y;
        int viewportW = StructureMinerInfoLayout.INFO_LIST_W;
        int viewportH = StructureMinerInfoLayout.INFO_LIST_H;

        int selected = c.selectedMarkerSlot();
        if (selected < 0) {
            StructureMinerTheme.emptyState(
                    g,
                    c.font(),
                    viewportX,
                    viewportY,
                    viewportW,
                    viewportH,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.marker_info.select"));
            return;
        }

        int contentHeight = contentHeight(c, telemetry, selected);
        c.markerInfoContentHeight(contentHeight);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        int scroll = Math.max(0, Math.min(c.markerInfoScroll(), maxScroll));
        c.markerInfoScroll(scroll);

        StructureMinerLayout.ScissorBounds scissor =
                StructureMinerLayout.scaleToScreen(
                        c.leftPos() + viewportX,
                        c.topPos() + viewportY,
                        viewportW,
                        viewportH,
                        c.uiScale());
        g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());

        int x = viewportX + StructureMinerInfoLayout.VIEWPORT_PAD;
        int width = viewportW - 2 * StructureMinerInfoLayout.VIEWPORT_PAD;
        int y = viewportY - scroll;

        y = drawSummary(c, g, telemetry, selected, x, y, width);
        y = drawMarkerSection(c, g, selected, x, y, width);
        y = drawWorkSection(c, g, telemetry, selected, x, y, width);
        drawProductSection(c, g, x, y, width);

        g.disableScissor();

        StructureMinerTheme.scrollbar(
                g,
                StructureMinerInfoLayout.SCROLLBAR_X,
                viewportY,
                viewportH,
                contentHeight,
                viewportH,
                scroll);
    }

    // --- thread selector ---------------------------------------------------

    /**
     * One 20x20 sprite button per thread, drawn from the sheet rather than assembled from fills so
     * the control keeps the same lighting as every other button in the console.
     *
     * <p>The button is dimmed when its slot holds no marker: the number alone cannot say whether a
     * thread exists, and that is the first thing the player needs from this row.
     */
    private static void drawSelector(StructureMinerScreenContext c, GuiGraphics g) {
        int count = c.menu().getContainerSlotCount();
        for (int slot = 0; slot < count; slot++) {
            int x = StructureMinerInfoLayout.laneX(slot, count);
            int y = StructureMinerInfoLayout.MARKER_Y;
            int size = StructureMinerInfoLayout.MARKER_SIZE;
            boolean configured =
                    StructMarkerItem.getMarkerInfo(c.menu().slots.get(slot).getItem()).isPresent();
            boolean selected = slot == c.selectedMarkerSlot();

            StructureMinerSpriteRenderer.smallButton(
                    g, x, y, slot == c.hoveredMarkerSlot(), selected);
            if (!configured) {
                g.fill(x, y, x + size, y + size, StructureMinerTheme.DISABLED_OVERLAY);
            }

            String label = Integer.toString(slot + 1);
            g.drawString(
                    c.font(),
                    label,
                    x + (size - c.font().width(label)) / 2,
                    y + (size - 8) / 2,
                    StructureMinerTheme.INK,
                    false);
        }

        int selected = c.selectedMarkerSlot();
        if (selected >= 0 && selected < count) {
            int x = StructureMinerInfoLayout.laneX(selected, count);
            g.fill(
                    x,
                    StructureMinerInfoLayout.MARKER_PROGRESS_Y,
                    x + StructureMinerInfoLayout.MARKER_SIZE,
                    StructureMinerInfoLayout.MARKER_PROGRESS_Y + 1,
                    StructureMinerTheme.FLUIX);
        }
    }

    // --- sections ----------------------------------------------------------

    private static int drawSummary(
            StructureMinerScreenContext c,
            GuiGraphics g,
            StructureMinerTelemetrySnapshot t,
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
                                        "screen.dimension_tech.structure_miner.marker_info.unconfigured"));
        Component header =
                Component.translatable(
                        "screen.dimension_tech.structure_miner.marker_info.structure",
                        slot + 1,
                        name);

        String total =
                Component.translatable(
                                        "screen.dimension_tech.structure_miner.overview.total_parallel")
                                .getString()
                        + " "
                        + t.totalParallel();
        int totalWidth = font.width(total);
        g.drawString(
                font,
                font.plainSubstrByWidth(header.getString(), Math.max(1, width - totalWidth - 6)),
                x,
                y,
                StructureMinerTheme.INK,
                false);
        g.drawString(font, total, x + width - totalWidth, y, StructureMinerTheme.FLUIX, false);
        return y + SUMMARY_H;
    }

    private static int drawMarkerSection(
            StructureMinerScreenContext c, GuiGraphics g, int slot, int x, int y, int width) {
        Font font = c.font();
        y =
                sectionHeader(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.structure_miner.info.section.marker");

        ItemStack stack = c.menu().slots.get(slot).getItem();
        StructMarkerItem.MarkerInfo info = StructMarkerItem.getMarkerInfo(stack).orElse(null);
        if (info == null) {
            g.drawString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.marker_info.unconfigured"),
                    x,
                    y,
                    StructureMinerTheme.DIM,
                    false);
            return y + StructureMinerInfoLayout.ROW_H_DATA * MARKER_SECTION_ROWS;
        }

        g.drawString(
                font,
                clip(
                        font,
                        Component.translatable(
                                "screen.dimension_tech.structure_miner.marker_info.dimension",
                                TranslateHelper.dimensionName(info.dimension())),
                        width),
                x,
                y,
                StructureMinerTheme.INK,
                false);
        y += StructureMinerInfoLayout.ROW_H_DATA;

        y =
                drawAnalysisRow(
                        c,
                        g,
                        "screen.dimension_tech.structure_miner.marker_info.dimension_value",
                        c.effectiveDimensionValue(),
                        x,
                        y,
                        width,
                        StructureMinerTheme.FLUIX);
        y =
                drawAnalysisRow(
                        c,
                        g,
                        "screen.dimension_tech.structure_miner.marker_info.structure_value",
                        c.effectiveStructureValue(),
                        x,
                        y,
                        width,
                        StructureMinerTheme.AMBER);
        return y;
    }

    /**
     * Analysis arrives asynchronously, so an unloaded value must read as "loading" rather than as
     * zero — zero is a legitimate result the player would otherwise mistake for a real reading.
     */
    private static int drawAnalysisRow(
            StructureMinerScreenContext c,
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
                        ? Component.translatable(key, ReadingFormat.reading(value))
                        : Component.translatable(
                                "screen.dimension_tech.structure_miner.marker_info.loading");
        g.drawString(
                font,
                clip(font, text, width),
                x,
                y,
                ready ? color : StructureMinerTheme.DIM,
                false);
        return y + StructureMinerInfoLayout.ROW_H_DATA;
    }

    private static int drawWorkSection(
            StructureMinerScreenContext c,
            GuiGraphics g,
            StructureMinerTelemetrySnapshot t,
            int slot,
            int x,
            int y,
            int width) {
        Font font = c.font();
        y =
                sectionHeader(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.structure_miner.info.section.work");

        StructureMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        int cycle = Math.max(1, marker.processingTime());
        boolean accelerated = marker.actualTicks() != marker.naturalTicks();

        g.drawString(
                font,
                clip(
                        font,
                        Component.translatable(
                                "screen.dimension_tech.structure_miner.marker_progress",
                                marker.naturalTicks(),
                                marker.processingTime()),
                        width),
                x,
                y,
                StructureMinerTheme.INK,
                false);
        y += StructureMinerInfoLayout.ROW_H_DATA;
        progressStrip(g, x, y, width, marker.naturalTicks(), cycle);
        y += STRIP_H;

        if (accelerated) {
            g.drawString(
                    font,
                    clip(
                            font,
                            Component.translatable(
                                    "screen.dimension_tech.structure_miner.actual_progress",
                                    c.menu().getMarkerActualProgress(slot),
                                    marker.realProcessingTime(),
                                    c.menu().getMarkerActualCycleCount(slot)),
                            width),
                    x,
                    y,
                    StructureMinerTheme.SUCCESS,
                    false);
            y += StructureMinerInfoLayout.ROW_H_DATA;
            progressStrip(
                    g,
                    x,
                    y,
                    width,
                    c.menu().getMarkerActualProgress(slot),
                    marker.realProcessingTime());
            y += STRIP_H;
        }

        y = drawParallelBreakdown(c, g, slot, x, y, width, t, accelerated);

        if (marker.waitingForNaturalWindow()) {
            g.drawString(
                    font,
                    clip(
                            font,
                            Component.translatable(
                                    "screen.dimension_tech.structure_miner.waiting_for_natural_window"),
                            width),
                    x,
                    y,
                    StructureMinerTheme.AMBER,
                    false);
            y += StructureMinerInfoLayout.ROW_H_DATA;
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
            StructureMinerScreenContext c,
            GuiGraphics g,
            int slot,
            int x,
            int y,
            int width,
            StructureMinerTelemetrySnapshot t,
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
                                "screen.dimension_tech.structure_miner.marker_info.parallel_status",
                                total,
                                t.totalParallel()),
                        width),
                x,
                y,
                StructureMinerTheme.INK,
                false);
        y += StructureMinerInfoLayout.ROW_H_DATA;

        y =
                detailRow(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.structure_miner.marker_info.parallel.base",
                        base,
                        StructureMinerTheme.FLUIX);
        y =
                detailRow(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.structure_miner.marker_info.parallel.efficiency",
                        efficiency,
                        StructureMinerTheme.AMBER);
        if (accelerated) {
            y =
                    detailRow(
                            c,
                            g,
                            x,
                            y,
                            width,
                            "screen.dimension_tech.structure_miner.marker_info.parallel.external",
                            StructureMinerScreen.formatRatio(external),
                            StructureMinerTheme.SUCCESS);
        }
        return detailRow(
                c,
                g,
                x,
                y,
                width,
                "screen.dimension_tech.structure_miner.marker_info.previous_cycle_parallel",
                StructureMinerScreen.formatDecimal(
                        c.menu().getMarkerPreviousExternalAccelerationParallelHundredths(slot)),
                StructureMinerTheme.MUTED);
    }

    private static int detailRow(
            StructureMinerScreenContext c,
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
                color == StructureMinerTheme.MUTED
                        ? StructureMinerTheme.DIM
                        : StructureMinerTheme.INK,
                false);
        return y + DETAIL_ROW_STRIDE;
    }

    /**
     * The product section: a caption, a legend for the disabled state, and a grid of cells.
     *
     * <p><b>What the grid replaced.</b> This section used to be a zebra-striped list of
     * twenty-pixel rows — icon, name, expected count — which meant a structure with forty drops
     * needed eight hundred pixels of page to show them all, and the page scroll grew with the drop
     * count. As a grid of 18px cells it is four times narrower per item, and the item's name, which
     * was the only thing the row had that a cell does not, moved into the tooltip the cell already
     * opens.
     *
     * <p><b>Disabled is drawn twice, on purpose.</b> The cell carries a dim wash, and this section
     * carries a legend line explaining what that wash means. A wash alone says "something is wrong
     * here"; the legend says which way, and a player who has disabled nothing sees an unambiguous
     * empty grid rather than a grid of grey squares they cannot name.
     */
    private static int drawProductSection(
            StructureMinerScreenContext c, GuiGraphics g, int x, int y, int width) {
        Font font = c.font();
        y =
                sectionHeader(
                        c,
                        g,
                        x,
                        y,
                        width,
                        "screen.dimension_tech.structure_miner.info.section.products");

        List<StructureMinerScreen.ExpectedItemRow> rows = c.expectedItemRows();
        if (rows.isEmpty()) {
            g.drawString(
                    font,
                    Component.translatable(
                            "screen.dimension_tech.structure_miner.marker_info.no_items"),
                    x,
                    y,
                    StructureMinerTheme.DIM,
                    false);
            return y + StructureMinerInfoLayout.ROW_H_INTERACTIVE;
        }

        List<ItemExpectationGrid.Entry> cells = cellsOf(c, rows);
        int gridX = StructureMinerInfoLayout.INFO_LIST_X + StructureMinerInfoLayout.VIEWPORT_PAD;
        int gridH = GRID.contentHeight(rows.size());
        GRID.render(
                g,
                font,
                cells,
                gridX,
                y,
                width,
                gridH,
                StructureMinerLayout.scaleToScreen(
                        c.leftPos() + gridX, c.topPos() + y, width, gridH, c.uiScale()),
                0,
                0,
                Component.empty());

        return gridH;
    }

    /**
     * The grid's cells, built from the page's rows.
     *
     * <p>The disabled flag is per-cell rather than a separate overlay pass because the grid draws
     * the wash itself, over the icon pass it owns. The multiplier is read per rebuild, not per
     * frame — {@code ItemValueFacade#multiplier} compiles regexes and would be a real cost in a
     * render loop.
     */
    private static List<ItemExpectationGrid.Entry> cellsOf(
            StructureMinerScreenContext c, List<StructureMinerScreen.ExpectedItemRow> rows) {
        List<ItemExpectationGrid.Entry> cells = new ArrayList<>(rows.size());
        for (StructureMinerScreen.ExpectedItemRow row : rows) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(row.item());
            boolean disabled = itemId != null && c.disabledExpectedItems().contains(itemId);
            cells.add(
                    new ItemExpectationGrid.Entry(
                            new ItemStack(row.item()),
                            row.expected(),
                            ItemValueFacade.multiplier(row.item()),
                            !disabled));
        }
        return cells;
    }

    /** Height the product grid needs, so {@link #contentHeight} and the draw pass agree. */
    static int productSectionHeight(int itemCount) {
        return GRID.contentHeight(itemCount);
    }

    // --- shared helpers ----------------------------------------------------

    private static int sectionHeader(
            StructureMinerScreenContext c, GuiGraphics g, int x, int y, int width, String key) {
        StructureMinerTheme.sectionHeader(
                g, c.font(), x, y, width, Component.translatable(key), StructureMinerTheme.FLUIX);
        return y + StructureMinerInfoLayout.SECTION_HEADER_H;
    }

    /**
     * A full-width progress strip assembled from the two spritesheet halves rather than from fills:
     * the drained track stitched out to {@code width}, then the energised cover clipped over it
     * from the left to whatever fraction {@code value / total} comes to.
     *
     * <p>The old two-tone version took a colour, which let the held/actual pair be told apart by
     * hue. The sheet owns the colours now, so the two strips are distinguished by the labels above
     * them instead — see {@link #drawWorkSection}.
     */
    private static void progressStrip(
            GuiGraphics g, int x, int y, int width, long value, long total) {
        StructureMinerSpriteRenderer.stitchTo(
                g,
                StructureMinerSpriteRenderer.PROGRESS_TRACK_FRAGMENT,
                x,
                y,
                StructureMinerSpriteRenderer.StitchDirection.HORIZONTAL,
                width,
                false);
        if (total <= 0 || value <= 0) return;
        int filled = (int) Math.min(width, Math.min(value, total) * width / total);
        if (filled <= 0) return;
        StructureMinerSpriteRenderer.stitchTo(
                g,
                StructureMinerSpriteRenderer.PROGRESS_FILL_FRAGMENT,
                x,
                y,
                StructureMinerSpriteRenderer.StitchDirection.HORIZONTAL,
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
    static int contentHeight(
            StructureMinerScreenContext c, StructureMinerTelemetrySnapshot t, int slot) {
        int height = SUMMARY_H;
        height +=
                StructureMinerInfoLayout.SECTION_HEADER_H
                        + MARKER_SECTION_ROWS * StructureMinerInfoLayout.ROW_H_DATA;
        height += StructureMinerInfoLayout.SECTION_HEADER_H + workSectionHeight(t, slot);
        height +=
                StructureMinerInfoLayout.SECTION_HEADER_H
                        + Math.max(
                                StructureMinerInfoLayout.ROW_H_INTERACTIVE,
                                productSectionHeight(c.expectedItemRows().size()));
        return height;
    }

    /**
     * Exactly what {@link #drawWorkSection} advances {@code y} by — no padding term, because these
     * two numbers feed {@link #productRowAt}'s hit test as well as the scroll range, and either use
     * breaks by the pad in pixels it thinks the drawing has.
     */
    private static int workSectionHeight(StructureMinerTelemetrySnapshot t, int slot) {
        StructureMinerTelemetrySnapshot.Marker marker = t.markers().get(slot);
        boolean accelerated = marker.actualTicks() != marker.naturalTicks();
        int height = accelerated ? 2 * TICK_BLOCK_H : TICK_BLOCK_H;
        // One headline line, then a row per contribution. The external row only appears under
        // external acceleration, so it has to be counted here too: leaving it out is what made the
        // old height ten pixels short, which clipped the scroll range and shifted every product
        // row.
        height += StructureMinerInfoLayout.ROW_H_DATA;
        height += (accelerated ? 4 : 3) * DETAIL_ROW_STRIDE;
        if (marker.waitingForNaturalWindow()) height += StructureMinerInfoLayout.ROW_H_DATA;
        return height;
    }

    /**
     * Product-cell index under a panel-local point, or {@code -1}.
     *
     * <p>Delegates to the grid's own hit test rather than re-deriving the arithmetic. That matters
     * more here than at the other call sites: this grid is inside a scrolling page, so the point
     * has to be rebased by the current scroll offset before the grid sees it, and getting that
     * rebase wrong would make a click land one row off exactly when the page is scrolled.
     */
    static int productRowAt(StructureMinerScreenContext c, double x, double y) {
        int slot = c.selectedMarkerSlot();
        if (slot < 0) return -1;
        int top = productSectionTop(c, slot);
        int left = StructureMinerInfoLayout.INFO_LIST_X + StructureMinerInfoLayout.VIEWPORT_PAD;
        int width =
                StructureMinerInfoLayout.INFO_LIST_W - 2 * StructureMinerInfoLayout.VIEWPORT_PAD;
        int height = Math.max(1, productSectionHeight(c.expectedItemRows().size()));

        return GRID.cellAt(
                cellsOf(c, c.expectedItemRows()), (int) x - left, (int) y - top, width, height);
    }

    private static int productSectionTop(StructureMinerScreenContext c, int slot) {
        return StructureMinerInfoLayout.INFO_LIST_Y
                - c.markerInfoScroll()
                + SUMMARY_H
                + StructureMinerInfoLayout.SECTION_HEADER_H
                + MARKER_SECTION_ROWS * StructureMinerInfoLayout.ROW_H_DATA
                + StructureMinerInfoLayout.SECTION_HEADER_H
                + workSectionHeight(c.menu().telemetrySnapshot(), slot)
                + StructureMinerInfoLayout.SECTION_HEADER_H;
    }

    /** True when a panel-local point falls inside the scrolling viewport. */
    /**
     * Hover pass: the thread selector, which shares the work lane's grid, and the product cells.
     *
     * <p>Product cells reuse {@link #productRowAt}, the same hit test the click path uses, so a
     * tooltip cannot appear over a cell that would not respond to a click.
     *
     * <p>The product tooltip is assembled here rather than handed to {@link
     * ItemExpectationGrid#renderTooltip} because this page's tooltip has a fourth line the grid
     * knows nothing about — the "click to enable/disable" hint. The two data lines are built from
     * the same keys the grid uses, so a cell's expectation reads identically on every screen.
     */
    static void renderTooltip(
            StructureMinerScreenContext c,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        int slot = StructureMinerInfoLayout.markerSlotAt(x, y, c.menu().getContainerSlotCount());
        if (slot >= 0) {
            StructureMinerTooltips.markerSlot(c, g, slot, screenX, screenY);
            return;
        }

        List<StructureMinerScreen.ExpectedItemRow> rows = c.expectedItemRows();
        int row = productRowAt(c, x, y);
        if (row < 0 || row >= rows.size()) return;

        StructureMinerScreen.ExpectedItemRow entry = rows.get(row);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.item());
        boolean disabled = itemId != null && c.disabledExpectedItems().contains(itemId);

        g.renderTooltip(
                c.font(),
                List.of(
                        new ItemStack(entry.item()).getHoverName(),
                        Component.translatable(
                                "screen.dimension_tech.struct_marker.tooltip.expected",
                                ReadingFormat.reading(entry.expected())),
                        Component.translatable(
                                "screen.dimension_tech.struct_marker.multiplier",
                                String.format(
                                        java.util.Locale.ROOT,
                                        "%.2f",
                                        ItemValueFacade.multiplier(entry.item()))),
                        Component.translatable(
                                disabled
                                        ? "screen.dimension_tech.structure_miner.expected_item.enable"
                                        : "screen.dimension_tech.structure_miner.expected_item.disable")),
                Optional.empty(),
                screenX,
                screenY);
    }
}
