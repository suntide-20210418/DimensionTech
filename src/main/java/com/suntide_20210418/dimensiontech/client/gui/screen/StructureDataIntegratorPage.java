package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
 */
final class StructureDataIntegratorPage {
    private static final int PAD = 4;

    /**
     * First row of the scrolling list — below the title strip and the search field that filters it,
     * so neither can end up drawn over a row.
     */
    private static final int LIST_Y = StructureDataOperatorScreen.LIST_Y;

    private static final int ROW_H = StructureDataOperatorScreen.LIST_ROW_H;
    private static final int TABLE_HEADER_H = 10;

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

    static void renderTooltip(
            StructureDataOperatorScreen s,
            GuiGraphics graphics,
            int localMouseX,
            int localMouseY,
            int screenMouseX,
            int screenMouseY) {
        if (s.detailMarker().isEmpty()) return;
        List<Map.Entry<ResourceLocation, ExactProbability>> rows =
                expectedItemRows(s.detailMarker());
        int visible = StructureDataOperatorScreen.tableRows();
        for (int row = 0; row < visible && s.detailScroll() + row < rows.size(); row++) {
            int y = tableRowY(row);
            if (!StructureDataOperatorScreen.inside(
                    localMouseX,
                    localMouseY,
                    StructureDataOperatorScreen.DETAIL_X + 2,
                    y,
                    18,
                    StructureDataOperatorScreen.TABLE_ROW_H)) continue;
            Item item =
                    BuiltInRegistries.ITEM
                            .getOptional(rows.get(s.detailScroll() + row).getKey())
                            .orElse(null);
            if (item != null)
                graphics.renderTooltip(
                        s.getMinecraft().font, new ItemStack(item), screenMouseX, screenMouseY);
            return;
        }
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

    private static int tableRowY(int row) {
        return tableHeaderY() + TABLE_HEADER_H + row * StructureDataOperatorScreen.TABLE_ROW_H;
    }

    /**
     * Y of the detail table's first row.
     *
     * <p>The screen sizes its scroll window from this rather than from a fixed canvas inset,
     * because the readings block above the table is what actually sets the offset.
     */
    static int tableTop() {
        return tableRowY(0);
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

        String itemLabel =
                Component.translatable("screen.dimension_tech.struct_marker.item").getString();
        String expectedLabel =
                Component.translatable("screen.dimension_tech.struct_marker.expected").getString();
        String multiplierLabel =
                Component.translatable("screen.dimension_tech.struct_marker.multiplier_header")
                        .getString();
        int expectedX = x + width * 55 / 100;
        int multiplierX = x + width * 78 / 100;
        g.fill(
                x,
                tableHeaderY(),
                x + width,
                tableHeaderY() + TABLE_HEADER_H,
                StructureMinerTheme.STRIPE_WELL);
        g.drawString(
                s.getMinecraft().font,
                itemLabel,
                x + 2,
                tableHeaderY() + 1,
                StructureMinerTheme.INK,
                false);
        g.drawString(
                s.getMinecraft().font,
                expectedLabel,
                expectedX,
                tableHeaderY() + 1,
                StructureMinerTheme.INK,
                false);
        g.drawString(
                s.getMinecraft().font,
                multiplierLabel,
                multiplierX,
                tableHeaderY() + 1,
                StructureMinerTheme.INK,
                false);

        List<Map.Entry<ResourceLocation, ExactProbability>> rows = expectedItemRows(marker);
        int visible = StructureDataOperatorScreen.tableRows();
        int nameBudget = Math.max(18, expectedX - x - 20);
        for (int row = 0; row < visible && s.detailScroll() + row < rows.size(); row++) {
            int y = tableRowY(row);
            Map.Entry<ResourceLocation, ExactProbability> entry = rows.get(s.detailScroll() + row);
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) g.renderItem(new ItemStack(item), x + 1, y + 1);
            String name =
                    item == null
                            ? entry.getKey().toString()
                            : new ItemStack(item).getHoverName().getString();
            g.drawString(
                    s.getMinecraft().font,
                    s.getMinecraft().font.plainSubstrByWidth(name, nameBudget),
                    x + 19,
                    y + 5,
                    StructureMinerTheme.INK,
                    false);
            g.drawString(
                    s.getMinecraft().font,
                    ReadingFormat.reading(entry.getValue().finiteDoubleValue()),
                    expectedX,
                    y + 5,
                    accent,
                    false);
            g.drawString(
                    s.getMinecraft().font,
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            StructureDataOperatorOperationPage.multiplier(
                                    item == null ? net.minecraft.world.item.Items.AIR : item)),
                    multiplierX,
                    y + 5,
                    StructureMinerTheme.DIM,
                    false);
        }

        if (rows.isEmpty()
                && StructMarkerItem.getAnalysisStatus(marker) == AnalysisStatus.UNSUPPORTED) {
            GuiText.centered(
                    g,
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.structure_operator.no_loot"),
                    x + width / 2,
                    tableRowY(0) + StructureDataOperatorScreen.TABLE_ROW_H,
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
        if (rows.size() > visible) {
            StructureMinerTheme.scrollbar(
                    g,
                    x + width - 3,
                    tableRowY(0),
                    visible * StructureDataOperatorScreen.TABLE_ROW_H,
                    rows.size() * StructureDataOperatorScreen.TABLE_ROW_H,
                    visible * StructureDataOperatorScreen.TABLE_ROW_H,
                    s.detailScroll() * StructureDataOperatorScreen.TABLE_ROW_H);
        }
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
