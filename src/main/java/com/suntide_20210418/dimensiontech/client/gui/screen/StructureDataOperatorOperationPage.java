package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import com.suntide_20210418.dimensiontech.item.ItemValueFacade;
import com.suntide_20210418.dimensiontech.item.ModDataComponents;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The operate page: the read marker's six readings, and the one-to-many copy workflow.
 *
 * <p>Readings are derived entirely client-side — a marker carries its snapshot in a data component
 * and the vanilla slot pass synchronises it — so this page needs no round trip. The dimension and
 * structure are text; dimension value, structure value and the per-item multiplier are numbers; the
 * item expectation list carries the remaining two readings as its cell count and its tooltip.
 *
 * <p><b>Table state is static</b> because the screen owns exactly one operate page and a page
 * renderer has no instance to hang it on. Rows are re-derived only when the marker's payload
 * component changes.
 *
 * <p><b>The table became a grid under a sort bar.</b> It used to be three columns of text — item,
 * expected, multiplier — which is a lot of horizontal room for a name and two numbers that fit in a
 * tooltip, and it capped the list at the handful of rows a 180px-tall canvas could stack. The rows
 * are now 18px cells nine across, so four times as many items fit in the same area, and the sort
 * interaction survives as what the header always really was: a bar of three clickable labels.
 */
final class StructureDataOperatorOperationPage {
    private static final int PAD = 4;

    /**
     * Width of the readings column. The item grid takes whatever is left, so this has to hold the
     * label column the readings block measures — in English the labels are about twice as wide as
     * in Chinese, and a narrower column would truncate the dimension and structure names.
     */
    private static final int READINGS_W = 152;

    private static final int TABLE_X = PAD + READINGS_W + PAD;

    /**
     * Height of the sort bar.
     *
     * <p>Twelve rather than the eleven the old header used: the bar now carries a hover wash and a
     * sort arrow, and the extra row stops the arrow from sitting on the cell above it.
     */
    private static final int HEADER_H = 12;

    /**
     * Cells across the item grid.
     *
     * <p>Nine, the same inventory width the marker screens use. The canvas is wider than theirs, so
     * the grid could take more, but nine keeps the cell size and pitch identical to every other
     * expectation grid in the mod and leaves the leftover width as margin rather than stretching
     * the pitch.
     */
    private static final int GRID_COLUMNS = 9;

    /**
     * The one grid instance for this page.
     *
     * <p>Static alongside the row cache, for the same reason: there is one operate page and no
     * instance to own it. It stays valid across tabs because it is rebuilt whenever {@link #rows}
     * changes.
     */
    private static final ItemExpectationGrid GRID =
            ItemExpectationGrid.ofSpriteSlots(GRID_COLUMNS, true);

    private enum Column {
        ITEM,
        EXPECTED,
        MULTIPLIER
    }

    private static Column sortColumn = Column.EXPECTED;
    private static boolean ascending = false;
    private static List<Row> rows = List.of();
    private static int rowsHash;
    private static boolean hasRows;
    private static boolean dragging;

    private StructureDataOperatorOperationPage() {}

    /** True when the read slot holds a marked structure and at least one write marker. */
    static boolean canCopy(StructureDataOperatorScreen s) {
        return s.hasWriteMarker()
                && s.hasOperands()
                && StructMarkerItem.getMarkerInfo(s.readMarker()).isPresent();
    }

    static void scroll(StructureDataOperatorScreen s, int step) {
        refresh(s);
        GRID.scrollBy(-step, rows.size(), gridHeight());
    }

    /**
     * Forwards a scrollbar drag.
     *
     * <p>Static like the rest of this page's state: the drag has to survive across the frames
     * between press and release, and the page renderer has no object to hold it.
     */
    static void mouseDragged(int canvasY) {
        if (!dragging) return;
        GRID.continueDrag(canvasY - gridTop(), rows.size(), gridHeight());
    }

    static void mouseReleased() {
        dragging = false;
        GRID.endDrag();
    }

    static boolean isDragging() {
        return dragging;
    }

    static void render(StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY) {
        ItemStack marker = s.readMarker();
        boolean hasMarker = !marker.isEmpty();
        boolean hasData = hasMarker && StructMarkerItem.getMarkerInfo(marker).isPresent();

        StructMarkerItem.MarkerInfo info =
                hasData ? StructMarkerItem.getMarkerInfo(marker).orElseThrow() : null;
        StructureDataOperatorReadings.draw(
                g,
                s.getMinecraft().font,
                PAD,
                PAD,
                READINGS_W,
                marker,
                info == null ? null : info.dimension(),
                info == null ? null : info.structure().id());

        int tableW = StructureDataOperatorLayout.CANVAS.width() - TABLE_X - PAD;
        if (!hasMarker) {
            empty(g, s, TABLE_X, PAD, tableW, "screen.dimension_tech.structure_operator.no_data");
            return;
        }
        if (!hasData) {
            empty(
                    g,
                    s,
                    TABLE_X,
                    PAD,
                    tableW,
                    "screen.dimension_tech.struct_marker.selection.empty");
            return;
        }
        refresh(s);
        drawTable(g, s, TABLE_X, PAD, tableW, mouseX, mouseY);
    }

    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) {
        int tableW = StructureDataOperatorLayout.CANVAS.width() - TABLE_X - PAD;

        /* The bar is tested first: its grab zone overlaps the grid's right edge by a few pixels. */
        if (GRID.scrollbarContains(x - TABLE_X, y - gridTop(), tableW, gridHeight(), rows.size())) {
            dragging = true;
            GRID.beginDrag(y - gridTop());
            return true;
        }

        if (!StructureDataOperatorScreen.inside(x, y, TABLE_X, PAD, tableW, HEADER_H)) return false;
        double relative = (x - TABLE_X) / (double) tableW;
        Column picked =
                relative >= 0.78D
                        ? Column.MULTIPLIER
                        : relative >= 0.58D ? Column.EXPECTED : Column.ITEM;
        if (picked == sortColumn) ascending = !ascending;
        else {
            sortColumn = picked;
            ascending = true;
        }
        sortRows();
        return true;
    }

    // ------------------------------------------------------------------ table

    /**
     * The sort bar, then the grid under it.
     *
     * <p>The bar keeps the three labels where the three columns used to be, at the same fractional
     * offsets, so a player who learned where to click to sort by multiplier still clicks in the
     * same place. What changed is that they no longer have to hit an eleven-pixel-tall strip of
     * text: {@link #mouseClicked} still tests the whole bar, and the bar now lights up under the
     * pointer.
     */
    private static void drawTable(
            GuiGraphics g,
            StructureDataOperatorScreen s,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {
        int expectedX = x + width * 58 / 100;
        int multiplierX = x + width * 78 / 100;

        boolean barHovered =
                StructureDataOperatorScreen.inside(mouseX, mouseY, x, y, width, HEADER_H);
        g.fill(
                x,
                y,
                x + width,
                y + HEADER_H,
                barHovered ? StructureMinerTheme.RECESS_LIT : StructureMinerTheme.STRIPE_WELL);

        String itemLabel =
                Component.translatable("screen.dimension_tech.struct_marker.item").getString();
        String expectedLabel =
                Component.translatable("screen.dimension_tech.struct_marker.expected").getString();
        String multiplierLabel =
                Component.translatable("screen.dimension_tech.struct_marker.multiplier_header")
                        .getString();
        g.drawString(
                s.getMinecraft().font, itemLabel, x + 3, y + 2, StructureMinerTheme.INK, false);
        g.drawString(
                s.getMinecraft().font,
                expectedLabel,
                expectedX,
                y + 2,
                StructureMinerTheme.INK,
                false);
        g.drawString(
                s.getMinecraft().font,
                multiplierLabel,
                multiplierX,
                y + 2,
                StructureMinerTheme.INK,
                false);
        sortMarker(g, s, y, Column.ITEM, x + 3, itemLabel);
        sortMarker(g, s, y, Column.EXPECTED, expectedX, expectedLabel);
        sortMarker(g, s, y, Column.MULTIPLIER, multiplierX, multiplierLabel);

        int listY = gridTop();
        if (rows.isEmpty()) {
            g.drawString(
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.struct_marker.no_items")
                            .getString(),
                    x + 3,
                    listY + 4,
                    StructureMinerTheme.DIM,
                    false);
            return;
        }

        GRID.render(
                g,
                s.getMinecraft().font,
                cells(),
                x,
                listY,
                width,
                gridHeight(),
                StructureMinerLayout.scaleToScreen(
                        s.canvasX(x), s.canvasY(listY), width, gridHeight(), 1.0F),
                mouseX - x,
                mouseY - listY,
                Component.translatable("screen.dimension_tech.struct_marker.no_items"));
    }

    /**
     * Y of the grid's top edge, in canvas-local pixels — one sort bar below the table origin.
     *
     * <p>Derived rather than typed so the sort bar's height and the grid's position cannot drift:
     * raising {@link #HEADER_H} moves the grid instead of overlapping it.
     */
    private static int gridTop() {
        return PAD + HEADER_H;
    }

    /** Height available to the grid, in canvas-local pixels. */
    private static int gridHeight() {
        return StructureDataOperatorLayout.CANVAS.height() - gridTop() - PAD;
    }

    /** The grid's cells, rebuilt from the page's rows. */
    private static List<ItemExpectationGrid.Entry> cells() {
        List<ItemExpectationGrid.Entry> cells = new ArrayList<>(rows.size());
        for (Row row : rows) {
            ItemStack stack = new ItemStack(row.item());
            cells.add(
                    new ItemExpectationGrid.Entry(
                            stack, row.expected(), ItemValueFacade.multiplier(row.item()), true));
        }
        return cells;
    }

    /**
     * Draws the hovered cell's tooltip.
     *
     * <p>Called from the screen's hover pass rather than from {@link #drawTable}, which runs with
     * the pose translated onto the canvas and the scissor clipped to it. {@code renderTooltip}
     * draws under the current pose and leaves the scissor alone, so calling it in there placed the
     * box one canvas origin away from the cursor and cropped it at the canvas edge.
     */
    static void renderTooltip(
            StructureDataOperatorScreen s,
            GuiGraphics g,
            int localMouseX,
            int localMouseY,
            int screenMouseX,
            int screenMouseY) {
        int tableW = StructureDataOperatorLayout.CANVAS.width() - TABLE_X - PAD;
        int listY = gridTop();
        if (rows.isEmpty()) return;
        GRID.renderTooltip(
                g,
                s.getMinecraft().font,
                cells(),
                localMouseX - TABLE_X,
                localMouseY - listY,
                tableW,
                gridHeight(),
                screenMouseX,
                screenMouseY);
    }

    private static void sortMarker(
            GuiGraphics g,
            StructureDataOperatorScreen s,
            int headerY,
            Column column,
            int labelX,
            String label) {
        if (sortColumn != column) return;
        g.drawString(
                s.getMinecraft().font,
                ascending ? "^" : "v",
                labelX + s.getMinecraft().font.width(label) + 3,
                headerY + 2,
                StructureMinerTheme.AMBER,
                false);
    }

    private static void empty(
            GuiGraphics g, StructureDataOperatorScreen s, int x, int y, int width, String key) {
        StructureMinerTheme.emptyState(
                g,
                s.getMinecraft().font,
                x,
                y,
                width,
                StructureDataOperatorLayout.CANVAS.height() - PAD * 2,
                Component.translatable(key));
    }

    // ------------------------------------------------------------------- rows

    private static void refresh(StructureDataOperatorScreen s) {
        ItemStack marker = s.readMarker();
        int hash = Objects.hashCode(marker.get(ModDataComponents.STRUCTURE_MARKER));
        if (hasRows && hash == rowsHash) return;
        hasRows = true;
        rowsHash = hash;
        rows = marker.isEmpty() ? List.of() : rowsOf(marker);
        sortRows();
        GRID.clampScroll(rows.size(), gridHeight());
    }

    private static List<Row> rowsOf(ItemStack marker) {
        List<Row> built = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) {
                built.add(new Row(item, ReadingFormat.displayValue(entry.getValue())));
            }
        }
        return List.copyOf(built);
    }

    private static void sortRows() {
        List<Row> sorted = new ArrayList<>(rows);
        Comparator<Row> comparator =
                switch (sortColumn) {
                    case ITEM ->
                            Comparator.comparing(
                                    row -> BuiltInRegistries.ITEM.getKey(row.item()).toString());
                    case EXPECTED -> Comparator.comparingDouble(Row::expected);
                    case MULTIPLIER ->
                            Comparator.comparingDouble(
                                    row -> ItemValueFacade.multiplier(row.item()));
                };
        if (!ascending) comparator = comparator.reversed();
        sorted.sort(
                comparator.thenComparing(
                        row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        rows = List.copyOf(sorted);
    }

    private record Row(Item item, double expected) {}
}
