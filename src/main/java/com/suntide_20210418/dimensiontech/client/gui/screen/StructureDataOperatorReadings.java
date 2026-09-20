package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The four readings that describe a structure, drawn identically on every operator page.
 *
 * <p>Both console pages used to draw their readings as framed value cells — the operate page stacked
 * four of them down its left column, the catalogue pages showed two side by side under a structure
 * name — so the same data read as two different things. They share this block now: one reading per
 * line, label then value, no frame around either.
 *
 * <p>The label column is measured from the widest label so the values line up, and because it is
 * measured at draw time it stays aligned in every language.
 */
final class StructureDataOperatorReadings {
    /** One reading per row: a nine pixel line plus four pixels of leading. */
    static final int LINE_HEIGHT = 13;

    /** Gap between the label column and the value. */
    private static final int COLUMN_GAP = 6;

    /** Stands in for a reading that has no value yet. */
    private static final String NONE = "—";

    private static final Component DIMENSION =
            Component.translatable("screen.dimension_tech.structure_operator.dimension");
    private static final Component STRUCTURE =
            Component.translatable("screen.dimension_tech.structure_operator.structure");
    private static final Component DIMENSION_VALUE =
            Component.translatable("screen.dimension_tech.struct_marker.dimension_value");
    private static final Component STRUCTURE_VALUE =
            Component.translatable("screen.dimension_tech.struct_marker.structure_value");

    private StructureDataOperatorReadings() {}

    /** Height of the whole block, so a page can place its item table underneath. */
    static int height() {
        return LINE_HEIGHT * 4;
    }

    /**
     * Draws the readings, one per line: which dimension and structure are being described, then the
     * two values the marker carries.
     *
     * <p>A marker without a snapshot leaves the value column showing a dash rather than collapsing the
     * block, so the page keeps its shape while a catalogue entry is still being analysed. The two
     * identifiers are passed in because the operate page reads them out of its marker while the
     * catalogue pages already know which entry is selected — and an entry can be selected before its
     * analysis lands, so the two cannot both be taken from the marker.
     */
    static void draw(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            ItemStack marker,
            ResourceLocation dimension,
            ResourceLocation structure) {
        boolean hasValues = !marker.isEmpty() && StructMarkerItem.getMarkerInfo(marker).isPresent();
        int column = labelColumn(font);
        int budget = Math.max(0, width - column);
        row(
                g,
                font,
                x,
                y,
                column,
                budget,
                DIMENSION,
                dimension == null ? NONE : TranslateHelper.dimensionName(dimension).getString(),
                StructureMinerTheme.FLUIX);
        row(
                g,
                font,
                x,
                y + LINE_HEIGHT,
                column,
                budget,
                STRUCTURE,
                structure == null ? NONE : TranslateHelper.structureName(structure).getString(),
                StructureMinerTheme.FLUIX);
        row(
                g,
                font,
                x,
                y + LINE_HEIGHT * 2,
                column,
                budget,
                DIMENSION_VALUE,
                hasValues
                        ? ReadingFormat.reading(StructMarkerItem.getDimensionValue(marker))
                        : NONE,
                StructureMinerTheme.FLUIX);
        row(
                g,
                font,
                x,
                y + LINE_HEIGHT * 3,
                column,
                budget,
                STRUCTURE_VALUE,
                hasValues
                        ? ReadingFormat.reading(StructMarkerItem.getStructureValue(marker))
                        : NONE,
                StructureMinerTheme.AMBER);
    }

    /** Width reserved for the label, measured so the value column lines up in the current language. */
    private static int labelColumn(Font font) {
        int widest = 0;
        for (Component label : List.of(DIMENSION, STRUCTURE, DIMENSION_VALUE, STRUCTURE_VALUE)) {
            widest = Math.max(widest, font.width(label.getString()));
        }
        return widest + COLUMN_GAP;
    }

    /** One reading: an unremarkable label, then the value in its accent colour, on the same line. */
    private static void row(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int column,
            int budget,
            Component label,
            String value,
            int accent) {
        g.drawString(font, label, x, y, StructureMinerTheme.DIM, false);
        g.drawString(font, font.plainSubstrByWidth(value, budget), x + column, y, accent, false);
    }
}
