package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Opaque structure dossier showing the persisted loot expectation snapshot. */
public final class StructMarkerScreen extends Screen {
    /** Maximum panel dimensions in GUI pixels. */
    private static final int WIDTH = 320;
    private static final int HEIGHT = 320;
    private static final int PANEL = 0xFF171B20;
    private static final int PANEL_RAISED = 0xFF20262D;
    private static final int PANEL_INSET = 0xFF101419;
    private static final int TEXT = 0xFFE6E9EC;
    private static final int MUTED = 0xFF98A2AD;
    private static final int CYAN = 0xFF48C6D1;
    private static final int AMBER = 0xFFF1B24A;
    private static final int SORT_MARKER_GAP = 5;

    private final ItemStack marker;
    private final List<Row> rows = new ArrayList<>();
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int scroll;
    private SortColumn sortColumn = SortColumn.ITEM;
    private boolean ascending = true;

    public StructMarkerScreen(ItemStack marker) {
        super(Component.translatable("screen.dimension_tech.struct_marker.title"));
        this.marker = marker;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(WIDTH, Math.max(1, width - 24));
        panelHeight = Math.min(HEIGHT, Math.max(1, height - 24));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rows.clear();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) rows.add(new Row(item, entry.getValue()));
        }
        sortRows();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF0B0E12);
        drawPanel(graphics, left, top, panelWidth, panelHeight);
        drawHeader(graphics, left);
        drawSummary(graphics, left);
        drawRows(graphics, left, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, int left, int y, int width, int height) {
        graphics.fill(left + 3, y + 3, left + width + 3, y + height + 3, 0xFF080A0C);
        graphics.fill(left, y, left + width, y + height, PANEL);
        graphics.fill(left + 1, y + 1, left + width - 1, y + 2, CYAN);
        graphics.fill(left + 1, y + height - 2, left + width - 1, y + height - 1, 0xFF343D46);
        graphics.fill(left + 1, y + 1, left + 2, y + height - 1, 0xFF343D46);
    }

    private void drawHeader(GuiGraphics graphics, int left) {
        graphics.drawString(font, title, left + 20, top + 17, TEXT, false);
        graphics.drawString(
                font,
                Component.translatable("screen.dimension_tech.struct_marker.subtitle"),
                left + 20,
                top + 33,
                MUTED,
                false);
        StructMarkerItem.getMarkerInfo(marker)
                .ifPresent(
                        info -> {
                            int metadataX = left + panelWidth / 2;
                            String dimension = info.dimension().toString();
                            graphics.drawString(
                                    font,
                                    Component.translatable(
                                            "screen.dimension_tech.struct_marker.dimension", dimension),
                                    metadataX,
                                    top + 17,
                                    CYAN,
                                    false);
                            String structures =
                                    info.structures().stream()
                                            .map(structure -> structure.id().toString())
                                            .distinct()
                                            .sorted()
                                            .reduce((a, b) -> a + ", " + b)
                                            .orElse("-");
                            String clipped = font.plainSubstrByWidth(structures, Math.max(1, panelWidth / 2 - 25));
                            graphics.drawString(
                                    font,
                                    Component.translatable(
                                            "screen.dimension_tech.struct_marker.structure", clipped),
                                    metadataX,
                                    top + 33,
                                    MUTED,
                                    false);
                        });
        graphics.fill(left + 18, top + 51, left + panelWidth - 18, top + 52, 0xFF303942);
    }

    private void drawSummary(GuiGraphics graphics, int left) {
        int y = top + 62;
        int metricWidth = Math.max(1, (panelWidth - 60) / 2);
        drawMetric(graphics, left + 20, y, metricWidth, "screen.dimension_tech.struct_marker.dimension_value", StructMarkerItem.getDimensionValue(marker), CYAN);
        drawMetric(graphics, left + 30 + metricWidth, y, metricWidth, "screen.dimension_tech.struct_marker.structure_value", StructMarkerItem.getStructureValue(marker), AMBER);
        int structureCount = StructMarkerItem.getMarkerInfo(marker).map(info -> info.structures().size()).orElse(0);
        AnalysisStatus status = StructMarkerItem.getAnalysisStatus(marker);
        Component structures =
                Component.translatable("screen.dimension_tech.struct_marker.structures", structureCount);
        Component calculationMethod =
                Component.translatable(
                        "screen.dimension_tech.struct_marker.calculation_method",
                        Component.translatable(
                                "screen.dimension_tech.struct_marker.analysis_status."
                                        + status.name().toLowerCase(java.util.Locale.ROOT)));
        graphics.drawString(
                font,
                structures,
                left + 20,
                y + 49,
                MUTED,
                false);
        graphics.drawString(
                font,
                calculationMethod,
                left + panelWidth - 20 - font.width(calculationMethod),
                y + 49,
                status == AnalysisStatus.EXACT ? CYAN : AMBER,
                false);
    }

    private void drawMetric(GuiGraphics graphics, int x, int y, int width, String key, double value, int accent) {
        graphics.fill(x, y, x + width, y + 40, PANEL_RAISED);
        graphics.fill(x, y, x + 3, y + 40, accent);
        graphics.drawString(font, Component.translatable(key), x + 12, y + 7, MUTED, false);
        String formatted = String.format(java.util.Locale.ROOT, "%.3f", value);
        graphics.drawString(font, formatted, x + 12, y + 21, TEXT, false);
    }

    private void drawRows(GuiGraphics graphics, int left, int mouseX, int mouseY) {
        int listX = left + 20;
        int listY = top + 160;
        int listW = Math.max(1, panelWidth - 40);
        int listH = Math.max(40, panelHeight - 176);
        graphics.fill(listX, listY, listX + listW, listY + listH, PANEL_INSET);
        graphics.fill(listX, listY, listX + listW, listY + 1, 0xFF303942);
        drawTableHeader(graphics, listX, listY - 25, listW);
        if (rows.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.dimension_tech.struct_marker.no_items"),
                    left + panelWidth / 2,
                    listY + 42,
                    MUTED);
            return;
        }
        int rowHeight = 30;
        int visible = listH / rowHeight;
        int first = Math.min(scroll, Math.max(0, rows.size() - visible));
        for (int index = first; index < Math.min(rows.size(), first + visible + 1); index++) {
            int y = listY + 6 + (index - first) * rowHeight;
            Row row = rows.get(index);
            ItemStack stack = new ItemStack(row.item);
            graphics.renderItem(stack, listX + 8, y);
            graphics.drawString(font, stack.getHoverName(), listX + 34, y + 2, TEXT, false);
            String amount = String.format(java.util.Locale.ROOT, "%.4f", row.expected.finiteDoubleValue());
            graphics.drawString(font, amount, expectedColumnX(listX, listW), y + 2, CYAN, false);
            Rarity rarity = stack.getRarity();
            double multiplier = ModConfigs.STRUCTURE_VALUE.itemMultiplier(stack.getItem(), rarity);
            graphics.drawString(
                    font,
                    Component.translatable("screen.dimension_tech.struct_marker.multiplier", multiplier),
                    multiplierColumnX(listX, listW),
                    y + 2,
                    rarityColor(rarity),
                    false);
            graphics.fill(listX + 34, y + 19, listX + listW - 10, y + 20, 0xFF252D35);
            if (mouseX >= listX + 4 && mouseX < listX + listW - 4 && mouseY >= y && mouseY < y + 24) {
                graphics.fill(listX + 4, y - 1, listX + listW - 4, y + 25, 0x223FC2CE);
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
        if (rows.size() > visible) {
            int barH = Math.max(12, listH * visible / rows.size());
            int barY = listY + (listH - barH) * first / Math.max(1, rows.size() - visible);
            graphics.fill(listX + listW - 5, listY + barY - listY, listX + listW - 2, listY + barY - listY + barH, AMBER);
        }
    }

    private void drawTableHeader(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 20, PANEL_RAISED);
        graphics.fill(x, y + 19, x + width, y + 20, AMBER);
        Component itemLabel = Component.translatable("screen.dimension_tech.struct_marker.item");
        Component expectedLabel = Component.translatable("screen.dimension_tech.struct_marker.expected");
        Component multiplierLabel =
                Component.translatable("screen.dimension_tech.struct_marker.multiplier_header");
        int itemLabelX = x + 12;
        int expectedLabelX = expectedColumnX(x, width);
        int multiplierLabelX = multiplierColumnX(x, width);
        graphics.drawString(
                font,
                itemLabel,
                itemLabelX,
                y + 6,
                TEXT,
                false);
        graphics.drawString(
                font,
                expectedLabel,
                expectedLabelX,
                y + 6,
                TEXT,
                false);
        graphics.drawString(
                font,
                multiplierLabel,
                multiplierLabelX,
                y + 6,
                TEXT,
                false);
        drawSortMarker(graphics, y, SortColumn.ITEM, itemLabelX, itemLabel);
        drawSortMarker(graphics, y, SortColumn.EXPECTED, expectedLabelX, expectedLabel);
        drawSortMarker(graphics, y, SortColumn.MULTIPLIER, multiplierLabelX, multiplierLabel);
    }

    private void drawSortMarker(
            GuiGraphics graphics, int y, SortColumn column, int labelX, Component label) {
        if (sortColumn != column) return;
        int markerX = labelX + font.width(label) + SORT_MARKER_GAP;
        graphics.drawString(font, ascending ? "↑" : "↓", markerX, y + 6, AMBER, false);
    }

    private int expectedColumnX(int x, int width) {
        return x + width * 64 / 100;
    }

    private int multiplierColumnX(int x, int width) {
        return x + width * 79 / 100;
    }

    private void sortRows() {
        Comparator<Row> comparator =
                switch (sortColumn) {
                    case ITEM -> Comparator.comparing(
                            row -> BuiltInRegistries.ITEM.getKey(row.item).toString());
                    case EXPECTED -> Comparator.comparing(
                            row -> row.expected.finiteDoubleValue());
                    case MULTIPLIER -> Comparator.comparing(row -> multiplier(row.item));
                };
        if (!ascending) comparator = comparator.reversed();
        rows.sort(
                comparator.thenComparing(
                        row -> BuiltInRegistries.ITEM.getKey(row.item).toString()));
    }

    private double multiplier(Item item) {
        return ModConfigs.STRUCTURE_VALUE.itemMultiplier(item, new ItemStack(item).getRarity());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int headerY = top + 135;
        if (button == 0
                && mouseX >= left + 20
                && mouseX < left + panelWidth - 20
                && mouseY >= headerY
                && mouseY < headerY + 20) {
            int relative = (int) mouseX - (left + 20);
            int listW = Math.max(1, panelWidth - 40);
            SortColumn selected =
                    relative >= listW * 79 / 100
                            ? SortColumn.MULTIPLIER
                            : relative >= listW * 64 / 100
                                    ? SortColumn.EXPECTED
                                    : SortColumn.ITEM;
            if (selected == sortColumn) {
                ascending = !ascending;
            } else {
                sortColumn = selected;
                ascending = true;
            }
            sortRows();
            scroll = 0;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int rarityColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> TEXT;
            case UNCOMMON -> 0xFF55FF55;
            case RARE -> 0xFF55AAFF;
            case EPIC -> 0xFFFF55FF;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (panelHeight - 176) / 30);
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - visible), scroll - (int) Math.signum(delta)));
        return true;
    }

    private enum SortColumn {
        ITEM,
        EXPECTED,
        MULTIPLIER
    }

    private record Row(Item item, ExactProbability expected) {}
}
