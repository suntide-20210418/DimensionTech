package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Information-page renderer entry point. */
final class MythicMinerInfoPage {
    static final int SECTION_HEADER_HEIGHT = 22;

    private MythicMinerInfoPage() {}

    static void render(
            MythicMinerScreenContext context, GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 28;
        int y = MythicMinerScreen.INFO_PANEL_Y;
        int width = context.imageWidth() - 56;
        int height = context.playerInventoryY() + 70 - y;
        MythicMinerTheme.panel(graphics, x, y, width, height, MythicMinerTheme.AMBER);
        graphics.drawString(
                context.font(),
                Component.translatable("screen.dimension_tech.mythic_miner.info.slots"),
                x + 8,
                y + 8,
                MythicMinerScreen.MUTED,
                false);
        for (int index = 0; index < context.menu().getContainerSlotCount(); index++) {
            int buttonX = x + 8 + index * 24;
            boolean selected = index == context.selectedMarkerSlot();
            MythicMinerTheme.button(
                    graphics,
                    context.font(),
                    buttonX,
                    MythicMinerScreen.INFO_SLOT_Y,
                    20,
                    20,
                    Component.literal(Integer.toString(index + 1)),
                    false,
                    true,
                    selected ? MythicMinerTheme.FLUIX : MythicMinerTheme.EDGE);
        }
        if (context.selectedMarkerSlot() < 0) {
            graphics.drawCenteredString(
                    context.font(),
                    Component.translatable("screen.dimension_tech.mythic_miner.marker_info.select"),
                    x + width / 2,
                    y + 82,
                    MythicMinerScreen.MUTED);
            return;
        }
        int viewportX = x + 8;
        int viewportY = MythicMinerScreen.INFO_VIEWPORT_Y;
        int viewportWidth = width - 16;
        int viewportHeight = viewportHeight(context);
        context.markerInfoContentHeight(contentHeight(context));
        int maxScroll = Math.max(0, context.markerInfoContentHeight() - viewportHeight);
        context.markerInfoScroll(Math.min(context.markerInfoScroll(), maxScroll));
        graphics.fill(
                viewportX,
                viewportY,
                viewportX + viewportWidth,
                viewportY + viewportHeight,
                MythicMinerScreen.PANEL_INSET);
        MythicMinerLayout.ScissorBounds scissor =
                MythicMinerLayout.scaleToScreen(
                        context.leftPos() + viewportX,
                        context.topPos() + viewportY,
                        viewportWidth,
                        viewportHeight,
                        context.uiScale());
        graphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        int cursorY = viewportY - context.markerInfoScroll();
        cursorY =
                drawSectionHeader(
                        context,
                        graphics,
                        viewportX,
                        cursorY,
                        viewportWidth,
                        "screen.dimension_tech.mythic_miner.info.section.marker",
                        context.markerPropertiesExpanded(),
                        MythicMinerScreen.CYAN);
        if (context.markerPropertiesExpanded()) {
            StructMarkerItem.MarkerInfo info =
                    StructMarkerItem.getMarkerInfo(
                                    context.menu()
                                            .slots
                                            .get(context.selectedMarkerSlot())
                                            .getItem())
                            .orElse(null);
            if (info != null) {
                drawClipped(
                        context,
                        graphics,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.structure",
                                context.selectedMarkerSlot() + 1,
                                TranslateHelper.structureName(info.structure().id())),
                        viewportX + 4,
                        cursorY,
                        viewportWidth - 8,
                        MythicMinerScreen.TEXT);
                drawClipped(
                        context,
                        graphics,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.dimension",
                                TranslateHelper.dimensionName(info.dimension())),
                        viewportX + 4,
                        cursorY + 13,
                        viewportWidth - 8,
                        MythicMinerScreen.MUTED);
            }
            drawClipped(
                    context,
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.dimension_value",
                            MythicMinerScreen.formatAnalysisValue(
                                    context.effectiveDimensionValue())),
                    viewportX + 4,
                    cursorY + 27,
                    viewportWidth - 8,
                    MythicMinerScreen.CYAN);
            drawClipped(
                    context,
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.structure_value",
                            MythicMinerScreen.formatAnalysisValue(
                                    context.effectiveStructureValue())),
                    viewportX + 4,
                    cursorY + 40,
                    viewportWidth - 8,
                    MythicMinerScreen.AMBER);
            cursorY += 57;
        }
        cursorY =
                drawSectionHeader(
                        context,
                        graphics,
                        viewportX,
                        cursorY,
                        viewportWidth,
                        "screen.dimension_tech.mythic_miner.info.section.work",
                        context.workStatusExpanded(),
                        MythicMinerScreen.CYAN);
        if (context.workStatusExpanded())
            cursorY = drawWorkStatus(context, graphics, viewportX, viewportWidth, cursorY);
        cursorY =
                drawSectionHeader(
                        context,
                        graphics,
                        viewportX,
                        cursorY,
                        viewportWidth,
                        "screen.dimension_tech.mythic_miner.info.section.products",
                        context.productInfoExpanded(),
                        MythicMinerScreen.AMBER);
        if (context.productInfoExpanded()) {
            drawClipped(
                    context,
                    graphics,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.expected_items"),
                    viewportX + 4,
                    cursorY,
                    viewportWidth - 8,
                    MythicMinerScreen.AMBER);
            context.resetExpectedHover();
            drawExpectedItems(
                    context,
                    graphics,
                    viewportX + 4,
                    cursorY + 14,
                    viewportWidth - 8,
                    viewportY,
                    viewportHeight,
                    mouseX,
                    mouseY);
        }
        graphics.disableScissor();
        if (context.markerInfoContentHeight() > viewportHeight) {
            int thumbHeight =
                    Math.max(
                            12,
                            viewportHeight * viewportHeight / context.markerInfoContentHeight());
            int thumbY =
                    viewportY
                            + (viewportHeight - thumbHeight)
                                    * context.markerInfoScroll()
                                    / Math.max(1, maxScroll);
            graphics.fill(
                    viewportX + viewportWidth - 3,
                    viewportY,
                    viewportX + viewportWidth - 2,
                    viewportY + viewportHeight,
                    MythicMinerScreen.RULE);
            graphics.fill(
                    viewportX + viewportWidth - 4,
                    thumbY,
                    viewportX + viewportWidth - 1,
                    thumbY + thumbHeight,
                    MythicMinerScreen.CYAN);
        }
    }

    static boolean mouseClicked(MythicMinerScreenContext c, double x, double y, int button) {
        if (button != 0) return true;
        int slot = slotAt(c, x, y);
        if (slot >= 0) {
            c.selectMarkerSlot(slot);
            return true;
        }
        if (c.selectedMarkerSlot() < 0) return true;
        Bounds parallel = parallelBounds(c);
        if (c.workStatusExpanded() && parallel.contains(x, y)) {
            c.toggleParallelBreakdown();
            c.markerInfoScroll(0);
            return true;
        }
        Bounds products = productsHeaderBounds(c);
        if (products.contains(x, y)) {
            c.toggleProductInfo();
            c.markerInfoScroll(0);
            return true;
        }
        int section = sectionAt(c, x, y);
        if (section >= 0) {
            if (section == 0) c.toggleMarkerProperties();
            else if (section == 1) c.toggleWorkStatus();
            else c.toggleProductInfo();
            c.markerInfoScroll(0);
            return true;
        }
        int expected = expectedRowAt(c, x, y);
        if (expected >= 0) {
            ResourceLocation itemId =
                    BuiltInRegistries.ITEM.getKey(c.expectedItemRows().get(expected).item());
            if (itemId != null) {
                ModNetwork.toggleMythicMinerExpectedItem(
                        c.menu().containerId, c.selectedMarkerSlot(), itemId);
            }
        }
        return true;
    }

    static boolean mouseScrolled(
            MythicMinerScreenContext c, double logicalX, double logicalY, double delta) {
        int height = viewportHeight(c);
        if (MythicMinerScreen.inside(
                logicalX,
                logicalY,
                c.leftPos() + 36,
                c.topPos() + MythicMinerScreen.INFO_VIEWPORT_Y,
                c.imageWidth() - 72,
                height)) {
            int maxScroll = Math.max(0, c.markerInfoContentHeight() - height);
            c.markerInfoScroll(
                    Math.max(
                            0,
                            Math.min(
                                    maxScroll,
                                    c.markerInfoScroll()
                                            - (int) Math.signum(delta)
                                                    * MythicMinerScreen.MARKER_INFO_ROW_HEIGHT)));
        }
        return true;
    }

    private static int drawSectionHeader(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            String key,
            boolean expanded,
            int accent) {
        g.fill(x + 1, y, x + width - 1, y + 18, MythicMinerTheme.PANEL);
        g.fill(x + 1, y, x + 3, y + 18, accent);
        g.drawString(
                c.font(), Component.translatable(key), x + 8, y + 5, MythicMinerScreen.TEXT, false);
        g.drawString(
                c.font(),
                Component.literal(expanded ? "-" : "+"),
                x + width - 12,
                y + 5,
                expanded ? accent : MythicMinerScreen.MUTED,
                false);
        return y + 22;
    }

    private static void drawExpectedItems(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            int viewportY,
            int viewportHeight,
            int mouseX,
            int mouseY) {
        if (c.expectedItemRows().isEmpty()) {
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.no_items"),
                    x,
                    y + 3,
                    width,
                    MythicMinerScreen.MUTED);
            return;
        }
        for (int index = 0; index < c.expectedItemRows().size(); index++) {
            int rowY = y + index * MythicMinerScreen.MARKER_INFO_ROW_HEIGHT;
            MythicMinerScreen.ExpectedItemRow row = c.expectedItemRows().get(index);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(row.item());
            boolean disabled = itemId != null && c.disabledExpectedItems().contains(itemId);
            ItemStack stack = new ItemStack(row.item());
            String expected = MythicMinerScreen.formatExpectedValue(row.expected());
            int expectedWidth = c.font().width(expected);
            int nameWidth = Math.max(1, width - expectedWidth - 5);
            if (index % 2 != 0)
                g.fill(x - 1, rowY - 1, x + width, rowY + 18, MythicMinerScreen.PANEL_RAISED);
            if (disabled) {
                g.fill(x - 2, rowY - 2, x + width, rowY + 18, MythicMinerTheme.DISABLED_OVERLAY);
                g.fill(x - 2, rowY - 2, x, rowY + 18, MythicMinerScreen.RED);
            }
            g.renderItem(stack, x, rowY);
            g.drawString(
                    c.font(),
                    c.font().plainSubstrByWidth(stack.getHoverName().getString(), nameWidth - 22),
                    x + 22,
                    rowY + 4,
                    disabled ? MythicMinerScreen.RED : MythicMinerScreen.TEXT,
                    false);
            g.drawString(
                    c.font(),
                    expected,
                    x + width - expectedWidth,
                    rowY + 4,
                    disabled ? MythicMinerScreen.RED : MythicMinerScreen.CYAN,
                    false);
            if (disabled)
                g.fill(
                        x,
                        rowY + 9,
                        x + width - expectedWidth - 3,
                        rowY + 10,
                        MythicMinerScreen.RED);
            if (rowY + 18 > viewportY
                    && rowY < viewportY + viewportHeight
                    && MythicMinerScreen.inside(mouseX, mouseY, x, rowY, width, 18))
                c.setExpectedHover(stack, disabled);
        }
    }

    private static int drawWorkStatus(
            MythicMinerScreenContext c, GuiGraphics g, int x, int width, int y) {
        boolean external = c.menu().isMarkerExternalAccelerationActive(c.selectedMarkerSlot());
        boolean waiting =
                external && c.menu().isMarkerWaitingForNaturalWindow(c.selectedMarkerSlot());
        drawClipped(
                c,
                g,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_progress",
                        c.menu().getMarkerProgress(c.selectedMarkerSlot()),
                        c.menu().getMarkerProcessingTime(c.selectedMarkerSlot())),
                x + 4,
                y,
                width - 8,
                MythicMinerScreen.TEXT);
        int line = y + 14;
        int bar = width - 8;
        drawProgressBar(
                g,
                x + 4,
                line,
                bar,
                c.menu().getMarkerCurrentNaturalTicks(c.selectedMarkerSlot()),
                c.menu().getMarkerProcessingTime(c.selectedMarkerSlot()),
                MythicMinerScreen.CYAN);
        line += 8;
        if (waiting) {
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.waiting_for_natural_window"),
                    x + 4,
                    line,
                    width - 8,
                    MythicMinerScreen.AMBER);
            line += 14;
        }
        drawClipped(
                c,
                g,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.marker_info.natural_ticks",
                        c.menu().getMarkerCurrentNaturalTicks(c.selectedMarkerSlot())),
                x + 4,
                line,
                width - 8,
                MythicMinerScreen.CYAN);
        line += 13;
        if (external) {
            long actual = c.menu().getMarkerActualProgress(c.selectedMarkerSlot());
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.actual_progress",
                            actual,
                            c.menu().getMarkerProcessingTime(c.selectedMarkerSlot()),
                            c.menu().getMarkerActualCycleCount(c.selectedMarkerSlot())),
                    x + 4,
                    line,
                    width - 8,
                    MythicMinerScreen.GREEN);
            line += 13;
            long cycle = c.menu().getMarkerProcessingTime(c.selectedMarkerSlot());
            drawProgressBar(
                    g,
                    x + 4,
                    line,
                    bar,
                    cycle > 0 ? Math.floorMod(actual, cycle) : 0,
                    cycle,
                    MythicMinerScreen.GREEN);
            line += 8;
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.info.external",
                            MythicMinerScreen.formatRatio(
                                    c.menu()
                                            .getMarkerCurrentCycleExternalEquivalentAcceleration(
                                                    c.selectedMarkerSlot()))),
                    x + 4,
                    line,
                    width - 8,
                    MythicMinerScreen.GREEN);
            line += 13;
        }
        drawClipped(
                c,
                g,
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.info.total_parallel",
                        c.menu().getMarkerTotalParallel(c.selectedMarkerSlot())),
                x + 4,
                line,
                width - 8,
                MythicMinerScreen.CYAN);
        drawClipped(
                c,
                g,
                Component.translatable(
                        c.showParallelBreakdown()
                                ? "screen.dimension_tech.mythic_miner.parallel.collapse_hint"
                                : "screen.dimension_tech.mythic_miner.parallel.expand_hint"),
                x + width - 132,
                line,
                128,
                MythicMinerScreen.MUTED);
        line += 13;
        if (c.showParallelBreakdown()) {
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.parallel.base",
                            c.menu().getBaseParallel()),
                    x + 12,
                    line,
                    width - 16,
                    MythicMinerScreen.MUTED);
            line += 12;
            drawClipped(
                    c,
                    g,
                    Component.translatable(
                            "screen.dimension_tech.mythic_miner.marker_info.parallel.efficiency",
                            c.menu().getMarkerExtraEfficiencyParallel(c.selectedMarkerSlot())),
                    x + 12,
                    line,
                    width - 16,
                    MythicMinerScreen.AMBER);
            line += 12;
            if (external) {
                drawClipped(
                        c,
                        g,
                        Component.translatable(
                                "screen.dimension_tech.mythic_miner.marker_info.parallel.external",
                                MythicMinerScreen.formatDecimal(
                                        c.menu()
                                                .getMarkerExternalAccelerationParallelHundredths(
                                                        c.selectedMarkerSlot()))),
                        x + 12,
                        line,
                        width - 16,
                        MythicMinerScreen.GREEN);
                line += 12;
            }
        }
        return line + 5;
    }

    private static int slotAt(MythicMinerScreenContext c, double x, double y) {
        if (y < MythicMinerScreen.INFO_SLOT_Y || y >= MythicMinerScreen.INFO_SLOT_Y + 20) return -1;
        int index = (int) ((x - 36) / 24);
        return x >= 36 && index >= 0 && index < c.menu().getContainerSlotCount() ? index : -1;
    }

    private static int sectionAt(MythicMinerScreenContext c, double x, double y) {
        if (x < 36 || x >= c.imageWidth() - 36) return -1;
        int rowY = MythicMinerScreen.INFO_VIEWPORT_Y - c.markerInfoScroll();
        if (MythicMinerScreen.inside(x, y, 36, rowY, c.imageWidth() - 72, 18)) return 0;
        rowY += 22 + (c.markerPropertiesExpanded() ? 57 : 0);
        if (MythicMinerScreen.inside(x, y, 36, rowY, c.imageWidth() - 72, 18)) return 1;
        rowY += 22 + workStatusHeight(c);
        return MythicMinerScreen.inside(x, y, 36, rowY, c.imageWidth() - 72, 18) ? 2 : -1;
    }

    private static int expectedRowAt(MythicMinerScreenContext c, double x, double y) {
        if (!c.productInfoExpanded() || c.expectedItemRows().isEmpty()) return -1;
        int rowY = expectedItemsY(c);
        int index = (int) Math.floor((y - rowY) / MythicMinerScreen.MARKER_INFO_ROW_HEIGHT);
        return index >= 0
                        && index < c.expectedItemRows().size()
                        && MythicMinerScreen.inside(
                                x,
                                y,
                                40,
                                rowY + index * MythicMinerScreen.MARKER_INFO_ROW_HEIGHT,
                                c.imageWidth() - 72,
                                18)
                ? index
                : -1;
    }

    private static Bounds parallelBounds(MythicMinerScreenContext c) {
        int y =
                MythicMinerScreen.INFO_VIEWPORT_Y
                        - c.markerInfoScroll()
                        + 22
                        + (c.markerPropertiesExpanded() ? 57 : 0)
                        + 22
                        + 22;
        if (c.menu().isMarkerExternalAccelerationActive(c.selectedMarkerSlot())
                && c.menu().isMarkerWaitingForNaturalWindow(c.selectedMarkerSlot())) y += 14;
        y += 13;
        if (c.menu().isMarkerExternalAccelerationActive(c.selectedMarkerSlot())) y += 34;
        return new Bounds(40, y, c.imageWidth() - 80, 13);
    }

    private static Bounds productsHeaderBounds(MythicMinerScreenContext c) {
        int y =
                MythicMinerScreen.INFO_VIEWPORT_Y
                        - c.markerInfoScroll()
                        + 22
                        + (c.markerPropertiesExpanded() ? 57 : 0)
                        + 22
                        + workStatusHeight(c);
        return new Bounds(36, y, c.imageWidth() - 72, 18);
    }

    private static int expectedItemsY(MythicMinerScreenContext c) {
        return MythicMinerScreen.INFO_VIEWPORT_Y
                - c.markerInfoScroll()
                + 22
                + (c.markerPropertiesExpanded() ? 57 : 0)
                + 22
                + workStatusHeight(c)
                + 22
                + 14;
    }

    private static int contentHeight(MythicMinerScreenContext c) {
        if (c.selectedMarkerSlot() < 0) return 0;
        int height = 22 + (c.markerPropertiesExpanded() ? 57 : 0);
        height += 22 + workStatusHeight(c) + 22;
        if (c.productInfoExpanded()) {
            height +=
                    14
                            + Math.max(
                                    18,
                                    c.expectedItemRows().size()
                                            * MythicMinerScreen.MARKER_INFO_ROW_HEIGHT);
        }
        return height;
    }

    private static int workStatusHeight(MythicMinerScreenContext c) {
        if (!c.workStatusExpanded() || c.selectedMarkerSlot() < 0) return 0;
        boolean external = c.menu().isMarkerExternalAccelerationActive(c.selectedMarkerSlot());
        // The total-parallel row and its bottom padding follow the progress and tick rows.
        int height = 53;
        if (external && c.menu().isMarkerWaitingForNaturalWindow(c.selectedMarkerSlot()))
            height += 14;
        if (external) height += 34;
        if (c.showParallelBreakdown()) height += external ? 36 : 24;
        return height;
    }

    private static int viewportHeight(MythicMinerScreenContext c) {
        return c.playerInventoryY() + 70 - MythicMinerScreen.INFO_PANEL_Y - 66;
    }

    private static void drawClipped(
            MythicMinerScreenContext c,
            GuiGraphics g,
            Component text,
            int x,
            int y,
            int width,
            int color) {
        g.drawString(
                c.font(), c.font().plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private static void drawProgressBar(
            GuiGraphics g, int x, int y, int width, long value, long total, int color) {
        g.fill(x, y, x + width, y + 4, MythicMinerScreen.PANEL_RAISED);
        if (total > 0 && value > 0) {
            g.fill(x, y, x + (int) Math.min(width, value * width / total), y + 4, color);
        }
    }

    private record Bounds(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return MythicMinerScreen.inside(mouseX, mouseY, x, y, width, height);
        }
    }

    static boolean isInfoPage(MythicMinerScreen.Page page) {
        return page == MythicMinerScreen.Page.INFO;
    }
}
