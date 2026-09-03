package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
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

/** Shared dimension catalogue and dossier viewport for both data-source plugins. */
final class StructureDataIntegratorPage {
    private static final int LIST_Y = 104;
    private static final int ROW_H = 14;

    private StructureDataIntegratorPage() {}

    static void renderSource(StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY, int accent, String titleKey) {
        MythicMinerTheme.panel(g, StructureDataOperatorScreen.LEFT_X, 48, StructureDataOperatorScreen.LEFT_W, 186, accent);
        MythicMinerTheme.panel(g, StructureDataOperatorScreen.RIGHT_X, 48, StructureDataOperatorScreen.RIGHT_W, 186, MythicMinerTheme.AMBER);
        g.drawString(s.getMinecraft().font, Component.translatable(titleKey), 52, 55, MythicMinerTheme.TEXT, false);
        g.drawString(s.getMinecraft().font, Component.translatable("screen.dimension_tech.structure_operator.catalogue_marker"), 52, 68, MythicMinerTheme.MUTED, false);
        g.fill(42, 82, 122, 98, MythicMinerTheme.PANEL);
        g.drawString(s.getMinecraft().font, Component.literal("/"), 48, 86, accent, false);
        button(s, g, 16, 82, 22, 16, "screen.dimension_tech.structure_operator.refresh_short", true, accent, mouseX, mouseY);
        button(s, g, 126, 82, 22, 16, "screen.dimension_tech.structure_operator.write_short", s.selected() != null && !s.detailMarker().isEmpty() && s.hasWriteMarker(), accent, mouseX, mouseY);
        drawCatalogue(s, g, mouseX, mouseY, accent);
        drawDetail(s, g, accent, mouseX, mouseY);
    }

    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) {
        if (StructureDataOperatorScreen.inside(x, y, 16, 82, 22, 16)) { if (s.selected() == null) s.refreshPageData(); else s.refreshSelectedAnalysis(); return true; }
        if (StructureDataOperatorScreen.inside(x, y, 126, 82, 22, 16) && s.selected() != null && !s.detailMarker().isEmpty() && s.hasWriteMarker()) { s.showWriteConfirmation(); return true; }
        if (StructureDataOperatorScreen.inside(x, y, StructureDataOperatorScreen.LEFT_X, LIST_Y, StructureDataOperatorScreen.LEFT_W, 126)) {
            int row = (y - LIST_Y) / ROW_H;
            int index = s.listScroll() + row;
            List<StructureDataOperatorScreen.CatalogueRow> rows = s.catalogueRows();
            if (index >= 0 && index < rows.size()) {
                StructureDataOperatorScreen.CatalogueRow selected = rows.get(index);
                if (selected.isDimension()) s.toggleDimension(selected.dimension());
                else s.select(selected.entry());
            }
            return true;
        }
        return false;
    }

    static void renderTooltip(
            StructureDataOperatorScreen s,
            GuiGraphics graphics,
            int localMouseX,
            int localMouseY,
            int screenMouseX,
            int screenMouseY) {
        if (s.detailMarker().isEmpty()) return;
        List<Map.Entry<ResourceLocation, ExactProbability>> rows = expectedItemRows(s.detailMarker());
        for (int row = 0; row < 5 && s.detailScroll() + row < rows.size(); row++) {
            int y = 144 + row * 16;
            if (!StructureDataOperatorScreen.inside(localMouseX, localMouseY, StructureDataOperatorScreen.RIGHT_X + 8, y - 2, 20, 20)) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(rows.get(s.detailScroll() + row).getKey()).orElse(null);
            if (item != null) graphics.renderTooltip(s.getMinecraft().font, new ItemStack(item), screenMouseX, screenMouseY);
            return;
        }
    }

    private static void drawCatalogue(StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY, int accent) {
        int x = StructureDataOperatorScreen.LEFT_X;
        g.fill(x, LIST_Y, x + StructureDataOperatorScreen.LEFT_W, 230, MythicMinerTheme.INSET);
        List<StructureDataOperatorScreen.CatalogueRow> rows = s.catalogueRows();
        if (rows.isEmpty()) { g.drawCenteredString(s.getMinecraft().font, Component.translatable("screen.dimension_tech.structure_operator.empty"), x + 68, 160, MythicMinerTheme.MUTED); return; }
        for (int row = 0; row < 9 && s.listScroll() + row < rows.size(); row++) {
            int index = s.listScroll() + row;
            int y = LIST_Y + row * ROW_H;
            StructureDataOperatorScreen.CatalogueRow rowData = rows.get(index);
            boolean selected = rowData.entry() != null && rowData.entry().equals(s.selected());
            boolean hovered = StructureDataOperatorScreen.inside(mouseX, mouseY, x, y, StructureDataOperatorScreen.LEFT_W, ROW_H);
            if (selected || hovered) g.fill(x + 1, y, x + StructureDataOperatorScreen.LEFT_W - 1, y + ROW_H - 1, selected ? 0xFF536779 : MythicMinerTheme.PANEL);
            if (rowData.isDimension()) {
                g.fill(x + 1, y, x + 3, y + ROW_H - 1, accent);
                String label =
                        (expanded(s, rowData.dimension()) ? "v " : "> ")
                                + Component.translatable(
                                                "screen.dimension_tech.structure_operator.dimension",
                                                TranslateHelper.dimensionName(rowData.dimension()))
                                        .getString();
                g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(label, 126), x + 7, y + 3, MythicMinerTheme.TEXT, false);
            } else {
                String label =
                        Component.translatable(
                                        "screen.dimension_tech.structure_operator.structure",
                                        TranslateHelper.structureName(rowData.entry().structure()))
                                .getString();
                g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(label, 112), x + 20, y + 3, selected ? MythicMinerTheme.TEXT : MythicMinerTheme.MUTED, false);
            }
        }
    }

    private static boolean expanded(StructureDataOperatorScreen s, ResourceLocation dimension) {
        return s.isDimensionExpanded(dimension);
    }

    private static void drawDetail(StructureDataOperatorScreen s, GuiGraphics g, int accent, int mouseX, int mouseY) {
        int x = StructureDataOperatorScreen.RIGHT_X;
        ItemStack marker = s.detailMarker();
        if (s.selected() == null) { g.drawCenteredString(s.getMinecraft().font, Component.translatable("screen.dimension_tech.structure_operator.select_entry"), x + 72, 136, MythicMinerTheme.MUTED); return; }
        g.drawString(s.getMinecraft().font, Component.translatable("screen.dimension_tech.structure_operator.catalogue_analysis"), x + 8, 55, MythicMinerTheme.TEXT, false);
        String dimension =
                Component.translatable(
                                "screen.dimension_tech.structure_operator.dimension",
                                TranslateHelper.dimensionName(s.selected().dimension()))
                        .getString();
        String structure =
                Component.translatable(
                                "screen.dimension_tech.structure_operator.structure",
                                TranslateHelper.structureName(s.selected().structure()))
                        .getString();
        g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(dimension, 128), x + 8, 68, accent, false);
        g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(structure, 128), x + 8, 80, MythicMinerTheme.MUTED, false);
        if (marker.isEmpty()) {
            Component progress =
                    s.totalAnalysisSamples() > 0
                            ? Component.translatable(
                                    "screen.dimension_tech.structure_operator.virtual_progress",
                                    s.completedAnalysisSamples(),
                                    s.totalAnalysisSamples())
                            : Component.translatable("screen.dimension_tech.structure_operator.loading");
            g.drawCenteredString(s.getMinecraft().font, progress, x + 72, 130, MythicMinerTheme.MUTED);
            return;
        }
        if (s.totalAnalysisSamples() > 0
                && StructMarkerItem.getAnalysisStatus(marker)
                        == com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus.APPROXIMATE) {
            g.drawString(
                    s.getMinecraft().font,
                    Component.translatable(
                            "screen.dimension_tech.structure_operator.virtual_approximate",
                            s.totalAnalysisSamples()),
                    x + 8,
                    128,
                    MythicMinerTheme.MUTED,
                    false);
        }
        metric(s, g, x + 8, 96, 60, "screen.dimension_tech.struct_marker.dimension_value", StructMarkerItem.getDimensionValue(marker), accent);
        metric(s, g, x + 76, 96, 60, "screen.dimension_tech.struct_marker.structure_value", StructMarkerItem.getStructureValue(marker), MythicMinerTheme.AMBER);
        g.fill(x + 8, 138, x + 136, 230, MythicMinerTheme.INSET);
        List<Map.Entry<ResourceLocation, ExactProbability>> rows = expectedItemRows(marker);
        for (int row = 0; row < 5 && s.detailScroll() + row < rows.size(); row++) {
            int y = 144 + row * 16;
            Map.Entry<ResourceLocation, ExactProbability> entry = rows.get(s.detailScroll() + row);
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) g.renderItem(new ItemStack(item), x + 12, y - 2);
            String name = item == null ? entry.getKey().toString() : new ItemStack(item).getHoverName().getString();
            g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(name, 78), x + 31, y + 1, MythicMinerTheme.TEXT, false);
            g.drawString(s.getMinecraft().font, String.format(java.util.Locale.ROOT, "%.2f", entry.getValue().finiteDoubleValue()), x + 108, y + 1, accent, false);
        }
    }

    private static void metric(StructureDataOperatorScreen s, GuiGraphics g, int x, int y, int width, String key, double value, int accent) {
        g.fill(x, y, x + width, y + 34, MythicMinerTheme.PANEL);
        g.fill(x, y, x + 2, y + 34, accent);
        g.drawString(s.getMinecraft().font, s.getMinecraft().font.plainSubstrByWidth(Component.translatable(key).getString(), width - 10), x + 6, y + 5, MythicMinerTheme.MUTED, false);
        g.drawString(s.getMinecraft().font, String.format(java.util.Locale.ROOT, "%.2f", value), x + 6, y + 18, MythicMinerTheme.TEXT, false);
    }

    private static List<Map.Entry<ResourceLocation, ExactProbability>> expectedItemRows(
            ItemStack marker) {
        List<Map.Entry<ResourceLocation, ExactProbability>> rows =
                new ArrayList<>(StructMarkerItem.getExpectedItemCounts(marker).entrySet());
        rows.sort(
                Comparator.<Map.Entry<ResourceLocation, ExactProbability>, ExactProbability>
                                comparing(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(entry -> entry.getKey().toString()));
        return rows;
    }

    private static void button(StructureDataOperatorScreen s, GuiGraphics g, int x, int y, int width, int height, String key, boolean enabled, int accent, int mouseX, int mouseY) {
        MythicMinerTheme.button(g, s.getMinecraft().font, x, y, width, height, Component.translatable(key), StructureDataOperatorScreen.inside(mouseX, mouseY, x, y, width, height), enabled, accent);
    }
}
