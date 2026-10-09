package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructMarkerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem.ChestInfo;
import com.suntide_20210418.dimensiontech.item.ItemValueFacade;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructureMarkerData;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The chest marker terminal: the handheld console that reads one chest marker's dossier.
 *
 * <p>It shares the structure marker's frame, layout coordinates and rendering primitives — the only
 * difference is the interaction contract: a chest is marked by clicking it in the world with the
 * item, so this screen has no "mark" button and no choice overlay, just the readings, the
 * expectation viewport and a clear button. Expectation rows, the analysis-status chip and the four
 * readings all come from the same {@code StructureMarkerData} component the structure marker
 * writes.
 *
 * <p><b>The list is a grid.</b> Like its structure counterpart, the viewport is drawn by {@link
 * ItemExpectationGrid}: nine 18px cells across, names moved into the tooltip. The two screens were
 * line-for-line duplicates before; the grid is the one place their list rendering converges.
 */
public final class ChestMarkerScreen extends Screen {
    /** Cells across the expectation viewport — the same nine the structure marker uses. */
    private static final int GRID_COLUMNS = 9;

    private final InteractionHand hand;

    private ItemStack marker;
    private final List<ItemExpectationGrid.Entry> rows = new ArrayList<>();
    private final ItemExpectationGrid grid = ItemExpectationGrid.ofSpriteSlots(GRID_COLUMNS, true);

    private int left;
    private int top;

    public ChestMarkerScreen(ItemStack marker, InteractionHand hand) {
        super(Component.translatable("screen.dimension_tech.chest_marker.title"));
        this.marker = marker;
        this.hand = hand;
    }

    /** True when this screen is already showing the given hand, i.e. it can be reused in place. */
    public boolean holds(InteractionHand requestedHand) {
        return requestedHand == hand;
    }

    /** Re-reads a marker the server has just pushed, preserving the scroll position. */
    public void refresh(ItemStack updated) {
        marker = updated;
        rebuildRows();
        grid.clampScroll(rows.size(), StructMarkerLayout.VIEWPORT_H);
    }

    @Override
    protected void init() {
        left = (width - StructMarkerLayout.WIDTH) / 2;
        top = (height - StructMarkerLayout.HEIGHT) / 2;
        rebuildRows();
    }

    /**
     * The expectation cells, heaviest first — the same order the structure marker ships.
     *
     * <p>Sorting goes through {@link ReadingFormat#displayValue} rather than {@code
     * ExactProbability#finiteDoubleValue}: the latter throws on a rational that does not fit a
     * double, and a comparator that throws takes the frame down with it.
     */
    private void rebuildRows() {
        rows.clear();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) {
                rows.add(
                        new ItemExpectationGrid.Entry(
                                new ItemStack(item),
                                ReadingFormat.displayValue(entry.getValue()),
                                ItemValueFacade.multiplier(item)));
            }
        }
        rows.sort(
                Comparator.comparingDouble(ItemExpectationGrid.Entry::expected)
                        .reversed()
                        .thenComparing(
                                row ->
                                        BuiltInRegistries.ITEM
                                                .getKey(row.stack().getItem())
                                                .toString()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        int windowX = mouseX - left;
        int windowY = mouseY - top;

        g.pose().pushPose();
        g.pose().translate(left, top, 0.0D);
        drawFace(g);
        drawCaption(g);
        drawLeftColumn(g);
        drawExpectationSection(g, windowX, windowY);
        drawActions(g, windowX, windowY);
        g.pose().popPose();

        grid.renderTooltip(
                g,
                font,
                rows,
                windowX - StructMarkerLayout.VIEWPORT.x(),
                windowY - StructMarkerLayout.VIEWPORT.y(),
                StructMarkerLayout.VIEWPORT_W,
                StructMarkerLayout.VIEWPORT_H,
                mouseX,
                mouseY);
    }

    private void drawFace(GuiGraphics g) {
        g.blit(
                StructMarkerLayout.TEXTURE,
                0,
                0,
                0,
                0,
                StructMarkerLayout.WIDTH,
                StructMarkerLayout.HEIGHT,
                StructMarkerLayout.TEXTURE_WIDTH,
                StructMarkerLayout.TEXTURE_HEIGHT);
    }

    private void drawCaption(GuiGraphics g) {
        g.drawString(
                font,
                Component.translatable("screen.dimension_tech.chest_marker.title"),
                StructMarkerLayout.TITLE_X,
                StructMarkerLayout.TITLE_Y,
                StructureMinerTheme.INK,
                false);

        AnalysisStatus status = StructMarkerItem.getAnalysisStatus(marker);
        Component label = statusLabel(status);
        int chipWidth =
                Math.max(
                        StructMarkerLayout.CHIP_W,
                        font.width(label) + 2 * StructMarkerLayout.CHIP_PAD);
        StructureMinerTheme.statusChip(
                g,
                font,
                StructMarkerLayout.WIDTH - 2 - StructMarkerLayout.FACE_MARGIN - chipWidth,
                StructMarkerLayout.CHIP_Y,
                chipWidth,
                label,
                stateAccent(status));
    }

    /** The four readings, the rule under them, and the three state lines below it. */
    private void drawLeftColumn(GuiGraphics g) {
        ChestInfo chest = ChestMarkerItem.getChestInfo(marker).orElse(null);
        StructureDataOperatorReadings.draw(
                g,
                font,
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.READING_Y,
                StructMarkerLayout.LEFT_W,
                StructMarkerLayout.READING_ROW_H,
                marker,
                markerDim(),
                ChestMarkerItem.CHEST_MARKER_ID,
                Component.translatable("screen.dimension_tech.chest_marker.chest_value"));

        g.fill(
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_RULE_Y,
                StructMarkerLayout.LEFT_X + StructMarkerLayout.LEFT_W,
                StructMarkerLayout.STATE_RULE_Y + 1,
                StructureMinerTheme.SHADE);

        AnalysisStatus status = StructMarkerItem.getAnalysisStatus(marker);
        g.drawString(
                font,
                Component.translatable(
                        chest == null
                                ? "screen.dimension_tech.chest_marker.selection.empty"
                                : "screen.dimension_tech.chest_marker.selection.selected"),
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_LINE_1_Y,
                chest == null ? StructureMinerTheme.DIM : StructureMinerTheme.SUCCESS,
                false);

        g.drawString(
                font,
                Component.translatable(
                        "screen.dimension_tech.chest_marker.calculation_method",
                        statusLabel(status)),
                StructMarkerLayout.LEFT_X,
                StructMarkerLayout.STATE_LINE_2_Y,
                stateAccent(status),
                false);

        if (chest != null) {
            BlockPos position = chest.position();
            g.drawString(
                    font,
                    position.getX() + ", " + position.getY() + ", " + position.getZ(),
                    StructMarkerLayout.LEFT_X,
                    StructMarkerLayout.STATE_LINE_3_Y,
                    StructureMinerTheme.DIM,
                    false);
        }
    }

    private ResourceLocation markerDim() {
        return StructMarkerItem.getMarkerData(marker)
                .map(StructureMarkerData::dimension)
                .orElse(null);
    }

    /** The section caption, its rule, and the scrolling expectation grid. */
    private void drawExpectationSection(GuiGraphics g, int windowX, int windowY) {
        StructureMinerTheme.sectionHeader(
                g,
                font,
                StructMarkerLayout.SECTION_X,
                StructMarkerLayout.SECTION_Y,
                StructMarkerLayout.SECTION_W,
                Component.translatable("screen.dimension_tech.struct_marker.items_heading"),
                StructureMinerTheme.FLUIX);

        String count =
                Component.translatable(
                                "screen.dimension_tech.struct_marker.item_count", rows.size())
                        .getString();
        g.drawString(
                font,
                count,
                StructMarkerLayout.SECTION_X + StructMarkerLayout.SECTION_W - font.width(count),
                StructMarkerLayout.SECTION_Y + 1,
                StructureMinerTheme.INK,
                false);

        drawGrid(g, windowX, windowY);
    }

    /**
     * Hands the viewport to the shared grid.
     *
     * <p>The scissor is projected here rather than inside the control because {@code enableScissor}
     * applies the window GUI scale but ignores the pose. This screen does not scale its pose — the
     * frame is a fixed 300x176 — so the projection is the panel origin plus the viewport's own
     * offset, at scale 1.
     */
    private void drawGrid(GuiGraphics g, int windowX, int windowY) {
        StructureMinerLayout.ScissorBounds clip =
                StructureMinerLayout.scaleToScreen(
                        left + StructMarkerLayout.VIEWPORT.x(),
                        top + StructMarkerLayout.VIEWPORT.y(),
                        StructMarkerLayout.VIEWPORT_W,
                        StructMarkerLayout.VIEWPORT_H,
                        1.0F);

        grid.render(
                g,
                font,
                rows,
                StructMarkerLayout.VIEWPORT.x(),
                StructMarkerLayout.VIEWPORT.y(),
                StructMarkerLayout.VIEWPORT_W,
                StructMarkerLayout.VIEWPORT_H,
                clip,
                windowX - StructMarkerLayout.VIEWPORT.x(),
                windowY - StructMarkerLayout.VIEWPORT.y(),
                Component.translatable("screen.dimension_tech.struct_marker.no_items"));
    }

    /** A single clear button, centred across the bottom of the face. */
    private void drawActions(GuiGraphics g, int windowX, int windowY) {
        int x = (StructMarkerLayout.WIDTH - StructMarkerLayout.ACTION_W) / 2;
        boolean marked = ChestMarkerItem.getChestInfo(marker).isPresent();
        boolean hovered =
                inside(
                        windowX,
                        windowY,
                        x,
                        StructMarkerLayout.ACTION_Y,
                        StructMarkerLayout.ACTION_W,
                        StructMarkerLayout.ACTION_H);
        StructureMinerSpriteRenderer.longButton(
                g, x, StructMarkerLayout.ACTION_Y, marked && hovered);
        if (!marked) {
            g.fill(
                    x,
                    StructMarkerLayout.ACTION_Y,
                    x + StructMarkerLayout.ACTION_W,
                    StructMarkerLayout.ACTION_Y + StructMarkerLayout.ACTION_H,
                    StructureMinerTheme.DISABLED_OVERLAY);
        }
        GuiText.centered(
                g,
                font,
                Component.translatable("screen.dimension_tech.struct_marker.clear"),
                x + StructMarkerLayout.ACTION_W / 2,
                StructMarkerLayout.ACTION_Y + 4,
                StructureMinerTheme.INK);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int windowX = (int) mouseX - left;
        int windowY = (int) mouseY - top;

        /* The bar is tested first: its grab zone overlaps the grid's right edge by a few pixels. */
        int gridX = windowX - StructMarkerLayout.VIEWPORT.x();
        int gridY = windowY - StructMarkerLayout.VIEWPORT.y();
        if (grid.scrollbarContains(
                gridX,
                gridY,
                StructMarkerLayout.VIEWPORT_W,
                StructMarkerLayout.VIEWPORT_H,
                rows.size())) {
            grid.beginDrag(gridY);
            return true;
        }

        int x = (StructMarkerLayout.WIDTH - StructMarkerLayout.ACTION_W) / 2;
        if (ChestMarkerItem.getChestInfo(marker).isPresent()
                && inside(
                        windowX,
                        windowY,
                        x,
                        StructMarkerLayout.ACTION_Y,
                        StructMarkerLayout.ACTION_W,
                        StructMarkerLayout.ACTION_H)) {
            ModNetwork.clear(hand);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Drags the expectation scrollbar, a path this screen did not have before the grid. */
    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (grid.dragging()) {
            grid.continueDrag(
                    (int) mouseY - top - StructMarkerLayout.VIEWPORT.y(),
                    rows.size(),
                    StructMarkerLayout.VIEWPORT_H);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        grid.endDrag();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 鼠标滚轮只产生纵向滚动量。
        int step = (int) Math.signum(scrollY);
        grid.scrollBy(-step, rows.size(), StructMarkerLayout.VIEWPORT_H);
        return true;
    }

    // ----------------------------------------------------------------- helpers

    private static Component statusLabel(AnalysisStatus status) {
        return Component.translatable(
                "screen.dimension_tech.struct_marker.analysis_status."
                        + status.name().toLowerCase(Locale.ROOT));
    }

    /** The accent a state announces with, on the chip and on the calculation-method line. */
    private static int stateAccent(AnalysisStatus status) {
        return switch (status) {
            case EXACT -> StructureMinerTheme.SUCCESS;
            case APPROXIMATE -> StructureMinerTheme.AMBER;
            case UNSUPPORTED -> StructureMinerTheme.ERROR;
            case LEGACY -> StructureMinerTheme.MUTED;
        };
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
