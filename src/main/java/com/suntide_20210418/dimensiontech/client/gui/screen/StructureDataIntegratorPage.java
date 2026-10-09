package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import com.suntide_20210418.dimensiontech.item.ItemValueFacade;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared dimension catalogue and dossier viewport for both data-source plugins.
 *
 * <p>Everything here is canvas-local: {@code StructureDataOperatorScreen#drawPage} has already
 * clipped to the canvas and translated to its origin, so {@code (0,0)} is the canvas corner. The
 * two columns are 150 and 181 pixels wide inside a 343-wide canvas — the catalogue is a master
 * list, the dossier its detail pane.
 *
 * <p><b>The dossier's expectation table became a grid.</b> It was three columns of text — item,
 * expected, multiplier — inside a 181px pane, which left about ninety pixels for the item name and
 * stacked at most a dozen rows in the space below the readings block. As a grid of 18px cells nine
 * across it fits four times as many items, and the name and the multiplier both moved into the
 * tooltip. The sort bar above it is not here: this pane has no sort interaction to preserve, only
 * one order, so the bar shrank to a caption row.
 */
final class StructureDataIntegratorPage {
    private static final int PAD = 4;

    /**
     * First row of the scrolling list — below the title strip and the search field that filters it,
     * so neither can end up drawn over a row.
     */
    private static final int LIST_Y = StructureDataOperatorScreen.LIST_Y;

    private static final int ROW_H = StructureDataOperatorScreen.LIST_ROW_H;

    /** Caption row above the dossier grid. */
    private static final int TABLE_HEADER_H = 10;

    /**
     * Cells across the dossier grid.
     *
     * <p>Nine, the same as everywhere else. The pane is 181 wide, so nine cells plus eight gutters
     * come to 178 and the bar takes three more — the grid leaves four pixels of margin, which is
     * the tightest fit of the four call sites and the reason the count is not raised.
     */
    private static final int GRID_COLUMNS = 9;

    /**
     * The dossier grid.
     *
     * <p>Not static, unlike the operate page's: this page's scroll offset lives on the screen
     * ({@code detailScroll}), because the screen also owns the wheel gate for the two panes. The
     * grid here is only a painter plus a hit test, so one shared instance is enough and the scroll
     * stays where it already was.
     */
    private static final ItemExpectationGrid GRID =
            ItemExpectationGrid.ofSpriteSlots(GRID_COLUMNS, false);

    private StructureDataIntegratorPage() {}

    static void renderSource(
            StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY, int accent) {
        int detailX = StructureDataOperatorScreen.DETAIL_X;
        int canvasHeight = StructureDataOperatorLayout.CANVAS.height();
        /* The page title used to sit here, above the search field. The tab already names the page,
         * so the strip is gone and the search field takes the canvas top in its place. */
        /* One hairline splits the master list from the detail pane; the canvas is already recessed,
         * so the columns do not need panels of their own. */
        g.fill(detailX - 4, PAD, detailX - 2, canvasHeight - PAD, StructureMinerTheme.HAIRLINE);
        drawCatalogue(s, g, mouseX, mouseY, accent);
        drawDetail(s, g, accent, mouseX, mouseY);
    }

    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) {
        int listX = StructureDataOperatorScreen.LIST_X;
        int listW = StructureDataOperatorScreen.LIST_W;
        int listH = StructureDataOperatorLayout.CANVAS.height() - LIST_Y - PAD;
        if (!StructureDataOperatorScreen.inside(x, y, listX, LIST_Y, listW, listH)) return false;
        int index = s.listScroll() + (y - LIST_Y) / ROW_H;
        List<StructureDataOperatorScreen.CatalogueRow> rows = s.catalogueRows();
        if (index >= 0 && index < rows.size()) {
            StructureDataOperatorScreen.CatalogueRow picked = rows.get(index);
            if (picked.isDimension()) s.toggleDimension(picked.dimension());
            else s.select(picked.entry());
        }
        return true;
    }

    /**
     * Draws the hovered dossier cell's tooltip.
     *
     * <p>The point is rebased twice: once out of the canvas and out of the detail column, and once
     * more by the scroll offset — the cells are drawn at {@code row - scroll}, so a point has to be
     * pushed back by the same amount before the grid can match it to a cell.
     */
    static void renderTooltip(
            StructureDataOperatorScreen s,
            GuiGraphics graphics,
            int localMouseX,
            int localMouseY,
            int screenMouseX,
            int screenMouseY) {
        if (s.detailMarker().isEmpty()) return;
        List<ItemExpectationGrid.Entry> cells = cellsOf(s.detailMarker());
        if (cells.isEmpty()) return;
        GRID.renderTooltip(
                graphics,
                s.getMinecraft().font,
                cells,
                localMouseX - StructureDataOperatorScreen.DETAIL_X,
                localMouseY - tableTop() + s.detailScroll(),
                StructureDataOperatorScreen.DETAIL_W,
                gridHeight(),
                screenMouseX,
                screenMouseY);
    }

    // ------------------------------------------------------------------ list

    private static void drawCatalogue(
            StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY, int accent) {
        int x = StructureDataOperatorScreen.LIST_X;
        int width = StructureDataOperatorScreen.LIST_W;
        int height = StructureDataOperatorLayout.CANVAS.height() - LIST_Y - PAD;
        List<StructureDataOperatorScreen.CatalogueRow> rows = s.catalogueRows();
        if (rows.isEmpty()) {
            GuiText.centered(
                    g,
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.structure_operator.empty"),
                    x + width / 2,
                    LIST_Y + height / 2 - 4,
                    StructureMinerTheme.DIM);
            return;
        }
        int visible = StructureDataOperatorScreen.listRows();
        for (int row = 0; row < visible && s.listScroll() + row < rows.size(); row++) {
            int index = s.listScroll() + row;
            int y = LIST_Y + row * ROW_H;
            StructureDataOperatorScreen.CatalogueRow rowData = rows.get(index);
            boolean selected = rowData.entry() != null && rowData.entry().equals(s.selected());
            boolean hovered =
                    StructureDataOperatorScreen.inside(mouseX, mouseY, x, y, width, ROW_H);
            String label;
            if (rowData.isDimension()) {
                label =
                        (s.isDimensionExpanded(rowData.dimension()) ? "v " : "> ")
                                + TranslateHelper.dimensionName(rowData.dimension()).getString();
            } else {
                label =
                        "  "
                                + TranslateHelper.structureName(rowData.entry().structure())
                                        .getString();
            }
            StructureMinerTheme.listRow(
                    g,
                    s.getMinecraft().font,
                    x,
                    y,
                    width - 4,
                    ROW_H,
                    Component.literal(s.getMinecraft().font.plainSubstrByWidth(label, width - 12)),
                    selected,
                    hovered,
                    accent);
        }
        if (rows.size() > visible) {
            StructureMinerTheme.scrollbar(
                    g,
                    x + width - 3,
                    LIST_Y,
                    height,
                    rows.size() * ROW_H,
                    visible * ROW_H,
                    s.listScroll() * ROW_H);
        }
    }

    // ---------------------------------------------------------------- detail

    /** Y of the detail table's header, below the readings block. */
    private static int tableHeaderY() {
        return PAD + StructureDataOperatorReadings.height() + 2;
    }

    /**
     * Y of the dossier grid's first row — one caption strip below the header origin.
     *
     * <p>The screen sizes its scroll window from this rather than from a fixed canvas inset,
     * because the readings block above the grid is what actually sets the offset.
     */
    static int tableTop() {
        return tableHeaderY() + TABLE_HEADER_H;
    }

    private static void drawDetail(
            StructureDataOperatorScreen s, GuiGraphics g, int accent, int mouseX, int mouseY) {
        int x = StructureDataOperatorScreen.DETAIL_X;
        int width = StructureDataOperatorScreen.DETAIL_W;
        if (s.selected() == null) {
            GuiText.centered(
                    g,
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.structure_operator.select_entry"),
                    x + width / 2,
                    StructureDataOperatorLayout.CANVAS.height() / 2 - 4,
                    StructureMinerTheme.DIM);
            return;
        }
        /* The structure's name is one of the readings now, so it needs no title of its own. */
        ItemStack marker = s.detailMarker();
        StructureDataOperatorReadings.draw(
                g,
                s.getMinecraft().font,
                x,
                PAD,
                width,
                marker,
                s.selected().dimension(),
                s.selected().structure());

        if (marker.isEmpty()) {
            GuiText.centered(
                    g,
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.structure_operator.loading"),
                    x + width / 2,
                    tableHeaderY() + 20,
                    StructureMinerTheme.DIM);
            return;
        }

        /*
         * The bar is a caption, not a sort control. This pane has one order — expected descending —
         * and nothing to toggle it against, so the three column labels collapsed into one line
         * naming what the grid holds. It reads as a header without pretending to be clickable.
         */
        String caption =
                Component.translatable("screen.dimension_tech.struct_marker.items_heading")
                        .getString();
        g.fill(
                x,
                tableHeaderY(),
                x + width,
                tableHeaderY() + TABLE_HEADER_H,
                StructureMinerTheme.STRIPE_WELL);
        g.drawString(
                s.getMinecraft().font,
                s.getMinecraft().font.plainSubstrByWidth(caption, width - 4),
                x + 2,
                tableHeaderY() + 1,
                StructureMinerTheme.INK,
                false);

        List<ItemExpectationGrid.Entry> cells = cellsOf(marker);
        /*
         * The origin carries the screen's scroll offset, not just the table top. This grid is
         * scrollable = false — its offset lives on the screen — so the control positions row 0 at
         * the origin it is handed and subtracts nothing itself. Drawing at tableTop() left the
         * cells frozen while the bar moved and the tooltip hit test, which does subtract the
         * offset, slid to a row that was never under the pointer.
         *
         * The scissor stays anchored at tableTop(): it is the window the cells scroll inside, so it
         * must not move with them.
         */
        int gridY = tableTop() - s.detailScroll();
        GRID.render(
                g,
                s.getMinecraft().font,
                cells,
                x,
                gridY,
                width,
                gridHeight(),
                StructureMinerLayout.scaleToScreen(
                        s.canvasX(x), s.canvasY(tableTop()), width, gridHeight(), 1.0F),
                0,
                0,
                Component.translatable("screen.dimension_tech.struct_marker.no_items"));

        if (cells.isEmpty()
                && StructMarkerItem.getAnalysisStatus(marker) == AnalysisStatus.UNSUPPORTED) {
            GuiText.centered(
                    g,
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.structure_operator.no_loot"),
                    x + width / 2,
                    gridY + GRID.strideY(),
                    StructureMinerTheme.DIM);
        }

        StructMarkerItem.filterDiagnostic(marker)
                .ifPresent(
                        message ->
                                g.drawString(
                                        s.getMinecraft().font,
                                        Component.literal(
                                                s.getMinecraft()
                                                        .font
                                                        .plainSubstrByWidth(message, width)),
                                        x,
                                        StructureDataOperatorLayout.CANVAS.height() - 12,
                                        StructureMinerTheme.ERROR,
                                        false));
        if (StructMarkerItem.getAnalysisStatus(marker) == AnalysisStatus.APPROXIMATE) {
            g.drawString(
                    s.getMinecraft().font,
                    Component.translatable(
                                    "screen.dimension_tech.structure_operator.virtual_approximate")
                            .getString(),
                    x,
                    StructureDataOperatorLayout.CANVAS.height() - 21,
                    StructureMinerTheme.DIM,
                    false);
        }
        /*
         * The bar is drawn by hand rather than by the grid: this call site runs the grid with
         * scrollable = false, because the scroll offset it has to honour already lives on the
         * screen. The range and the thumb position are therefore expressed in the grid's own pixel
         * terms, which is what makes the bar agree with the cells after the switch from rows.
         */
        int contentHeight = gridContentHeight(cells.size());
        if (contentHeight > gridHeight()) {
            StructureMinerTheme.scrollbar(
                    g,
                    x + width - 3,
                    tableTop(),
                    gridHeight(),
                    contentHeight,
                    gridHeight(),
                    s.detailScroll());
        }
    }

    /** Height available to the dossier grid, in canvas-local pixels. */
    static int gridHeight() {
        return StructureDataOperatorLayout.CANVAS.height() - tableTop() - PAD;
    }

    /** Pixel height {@code itemCount} dossier cells need, for the scroll range the screen owns. */
    static int gridContentHeight(int itemCount) {
        return GRID.contentHeight(itemCount);
    }

    /** The dossier grid's cells, rebuilt from the marker's expectation map. */
    private static List<ItemExpectationGrid.Entry> cellsOf(ItemStack marker) {
        List<Map.Entry<ResourceLocation, ExactProbability>> rows = expectedItemRows(marker);
        List<ItemExpectationGrid.Entry> cells = new ArrayList<>(rows.size());
        for (Map.Entry<ResourceLocation, ExactProbability> entry : rows) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            cells.add(
                    new ItemExpectationGrid.Entry(
                            stack,
                            ReadingFormat.displayValue(entry.getValue()),
                            ItemValueFacade.multiplier(item),
                            true));
        }
        return cells;
    }

    private static List<Map.Entry<ResourceLocation, ExactProbability>> expectedItemRows(
            ItemStack marker) {
        List<Map.Entry<ResourceLocation, ExactProbability>> rows =
                new ArrayList<>(StructMarkerItem.getExpectedItemCounts(marker).entrySet());
        rows.sort(
                Comparator
                        .<Map.Entry<ResourceLocation, ExactProbability>, ExactProbability>comparing(
                                Map.Entry::getValue)
                        .reversed()
                        .thenComparing(entry -> entry.getKey().toString()));
        return rows;
    }
}
