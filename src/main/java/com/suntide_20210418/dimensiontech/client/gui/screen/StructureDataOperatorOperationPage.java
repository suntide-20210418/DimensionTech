package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorLayout;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The operate page: the read marker's six readings, and the one-to-many copy workflow.
 *
 * <p>Readings are derived entirely client-side — a marker carries its snapshot in item NBT and the
 * vanilla slot pass synchronises it — so this page needs no round trip. The dimension and structure
 * are text; dimension value, structure value and the per-item multiplier are numbers; the item
 * expectation list carries the remaining two readings as its row count and its multiplier column.
 *
 * <p>Table state is static because the screen owns exactly one operate page and a page renderer has
 * no instance to hang it on. Rows are re-derived only when the marker's compound tag changes.
 */
final class StructureDataOperatorOperationPage {
    private static final int PAD = 4;

    /**
     * Width of the readings column. The item table takes whatever is left, so this has to hold the
     * label column the readings block measures — in English the labels are about twice as wide as in
     * Chinese, and a narrower column would truncate the dimension and structure names.
     */
    private static final int READINGS_W = 152;

    private static final int TABLE_X = PAD + READINGS_W + PAD;

    private static final int HEADER_H = 11;

    private enum Column {
        ITEM,
        EXPECTED,
        MULTIPLIER
    }

    private static Column sortColumn = Column.EXPECTED;
    private static boolean ascending = false;
    private static int scroll;
    private static List<Row> rows = List.of();
    private static int rowsHash;
    private static boolean hasRows;

    private StructureDataOperatorOperationPage() {}

    /** True when the read slot holds a marked structure and at least one write marker. */
    static boolean canCopy(StructureDataOperatorScreen s) {
        return s.hasWriteMarker()
                && s.hasOperands()
                && StructMarkerItem.getMarkerInfo(s.readMarker()).isPresent();
    }

    static void scroll(StructureDataOperatorScreen s, int step) {
        refresh(s);
        scroll = clamp(scroll - step, rows.size(), visibleRows());
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
            empty(g, s, TABLE_X, PAD, tableW, "screen.dimension_tech.struct_marker.selection.empty");
            return;
        }
        refresh(s);
        drawTable(g, s, TABLE_X, PAD, tableW, mouseX, mouseY);
    }

    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) {
        int tableW = StructureDataOperatorLayout.CANVAS.width() - TABLE_X - PAD;
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

    private static void drawTable(
            GuiGraphics g, StructureDataOperatorScreen s, int x, int y, int width, int mouseX, int mouseY) {
        int expectedX = x + width * 58 / 100;
        int multiplierX = x + width * 78 / 100;
        g.fill(x, y, x + width, y + HEADER_H, StructureMinerTheme.STRIPE_WELL);

        String itemLabel = Component.translatable("screen.dimension_tech.struct_marker.item").getString();
        String expectedLabel =
                Component.translatable("screen.dimension_tech.struct_marker.expected").getString();
        String multiplierLabel =
                Component.translatable("screen.dimension_tech.struct_marker.multiplier_header")
                        .getString();
        g.drawString(s.getMinecraft().font, itemLabel, x + 3, y + 2, StructureMinerTheme.INK, false);
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

        int listY = y + HEADER_H;
        int listH = StructureDataOperatorLayout.CANVAS.height() - listY - PAD;
        if (rows.isEmpty()) {
            g.drawString(
                    s.getMinecraft().font,
                    Component.translatable("screen.dimension_tech.struct_marker.no_items").getString(),
                    x + 3,
                    listY + 4,
                    StructureMinerTheme.DIM,
                    false);
            return;
        }
        int visible = visibleRows();
        int first = clamp(scroll, rows.size(), visible);
        int nameBudget = Math.max(20, expectedX - x - 22);
        for (int index = 0; index < visible; index++) {
            int rowIndex = first + index;
            if (rowIndex >= rows.size()) break;
            int rowY = listY + index * StructureDataOperatorScreen.TABLE_ROW_H;
            Row row = rows.get(rowIndex);
            ItemStack stack = new ItemStack(row.item());
            g.renderItem(stack, x + 2, rowY + 1);
            g.drawString(
                    s.getMinecraft().font,
                    s.getMinecraft()
                            .font
                            .plainSubstrByWidth(stack.getHoverName().getString(), nameBudget),
                    x + 20,
                    rowY + 5,
                    StructureMinerTheme.INK,
                    false);
            g.drawString(
                    s.getMinecraft().font,
                    ReadingFormat.reading(row.expected()),
                    expectedX,
                    rowY + 5,
                    StructureMinerTheme.FLUIX,
                    false);
            g.drawString(
                    s.getMinecraft().font,
                    String.format(Locale.ROOT, "%.2f", multiplier(row.item())),
                    multiplierX,
                    rowY + 5,
                    rarityColor(stack.getRarity()),
                    false);
            if (StructureDataOperatorScreen.inside(
                    mouseX, mouseY, x, rowY, width, StructureDataOperatorScreen.TABLE_ROW_H - 1)) {
                g.fill(
                        x,
                        rowY,
                        x + width,
                        rowY + StructureDataOperatorScreen.TABLE_ROW_H - 1,
                        0x223C5647);
            }
        }
        if (rows.size() > visible) {
            StructureMinerTheme.scrollbar(
                    g,
                    x + width - 3,
                    listY,
                    listH,
                    rows.size() * StructureDataOperatorScreen.TABLE_ROW_H,
                    visible * StructureDataOperatorScreen.TABLE_ROW_H,
                    first * StructureDataOperatorScreen.TABLE_ROW_H);
        }
    }

    /**
     * Draws the hovered row's item tooltip.
     *
     * <p>Called from the screen's hover pass rather than from {@link #drawTable}, which runs with the
     * pose translated onto the canvas and the scissor clipped to it. {@code renderTooltip} draws under
     * the current pose and leaves the scissor alone, so calling it in there placed the box one canvas
     * origin away from the cursor and cropped it at the canvas edge.
     */
    static void renderTooltip(
            StructureDataOperatorScreen s,
            GuiGraphics g,
            int localMouseX,
            int localMouseY,
            int screenMouseX,
            int screenMouseY) {
        int tableW = StructureDataOperatorLayout.CANVAS.width() - TABLE_X - PAD;
        int listY = PAD + HEADER_H;
        int listH = StructureDataOperatorLayout.CANVAS.height() - listY - PAD;
        if (!StructureDataOperatorScreen.inside(
                localMouseX, localMouseY, TABLE_X, listY, tableW, listH)) {
            return;
        }
        int visible = visibleRows();
        int first = clamp(scroll, rows.size(), visible);
        int row = (localMouseY - listY) / StructureDataOperatorScreen.TABLE_ROW_H;
        int index = first + row;
        if (row < 0 || row >= visible || index >= rows.size()) return;
        g.renderTooltip(
                s.getMinecraft().font,
                new ItemStack(rows.get(index).item()),
                screenMouseX,
                screenMouseY);
    }

    private static void sortMarker(
            GuiGraphics g, StructureDataOperatorScreen s, int headerY, Column column, int labelX, String label) {
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
        int hash = marker.isEmpty() ? 0 : Objects.hashCode(marker.getTag());
        if (hasRows && hash == rowsHash) return;
        hasRows = true;
        rowsHash = hash;
        rows = marker.isEmpty() ? List.of() : rowsOf(marker);
        sortRows();
        scroll = 0;
    }

    private static List<Row> rowsOf(ItemStack marker) {
        List<Row> built = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) built.add(new Row(item, entry.getValue().finiteDoubleValue()));
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
                    case MULTIPLIER -> Comparator.comparingDouble(row -> multiplier(row.item()));
                };
        if (!ascending) comparator = comparator.reversed();
        sorted.sort(
                comparator.thenComparing(
                        row -> BuiltInRegistries.ITEM.getKey(row.item()).toString()));
        rows = List.copyOf(sorted);
    }

    static double multiplier(Item item) {
        return ModConfigs.STRUCTURE_VALUE.itemMultiplier(item, new ItemStack(item).getRarity());
    }

    private static int visibleRows() {
        int listH = StructureDataOperatorLayout.CANVAS.height() - PAD - HEADER_H - PAD;
        return Math.max(1, listH / StructureDataOperatorScreen.TABLE_ROW_H);
    }

    private static int clamp(int value, int total, int visible) {
        return Math.max(0, Math.min(Math.max(0, total - visible), value));
    }

    private static int rarityColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> StructureMinerTheme.INK;
            case UNCOMMON -> 0xFF2E6B3E;
            case RARE -> 0xFF2A6180;
            case EPIC -> 0xFF7A2A70;
        };
    }

    private record Row(Item item, double expected) {}
}
