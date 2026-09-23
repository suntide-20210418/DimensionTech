package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructMarkerLayout;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem.ChestInfo;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
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
 * readings all come from the same {@code StructureMarkerData} NBT the structure marker writes.
 */
public final class ChestMarkerScreen extends Screen {
    private static final int ROW_HOVER = 0xFF9BB49A;

    private final InteractionHand hand;

    private ItemStack marker;
    private final List<Row> rows = new ArrayList<>();

    private int left;
    private int top;
    private int scroll;
    private int hoveredRow = -1;

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
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    protected void init() {
        left = (width - StructMarkerLayout.WIDTH) / 2;
        top = (height - StructMarkerLayout.HEIGHT) / 2;
        rebuildRows();
    }

    /** The expectation rows, heaviest first — the same order the structure marker ships. */
    private void rebuildRows() {
        rows.clear();
        for (Map.Entry<ResourceLocation, ExactProbability> entry :
                StructMarkerItem.getExpectedItemCounts(marker).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item != null) rows.add(new Row(item, entry.getValue()));
        }
        rows.sort(
                Comparator.comparingDouble((Row row) -> row.expected.finiteDoubleValue())
                        .reversed()
                        .thenComparing(row -> BuiltInRegistries.ITEM.getKey(row.item).toString()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int windowX = mouseX - left;
        int windowY = mouseY - top;
        hoveredRow = rowAt(mouseX, mouseY);

        g.pose().pushPose();
        g.pose().translate(left, top, 0.0D);
        drawFace(g);
        drawCaption(g);
        drawLeftColumn(g);
        drawExpectationSection(g);
        drawActions(g, windowX, windowY);
        g.pose().popPose();

        if (hoveredRow >= 0) {
            g.renderTooltip(font, new ItemStack(rows.get(hoveredRow).item), mouseX, mouseY);
        }
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
                StructMarkerLayout.WIDTH
                        - 2
                        - StructMarkerLayout.FACE_MARGIN
                        - chipWidth,
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
        net.minecraft.nbt.CompoundTag data = marker.getTagElement("StructureMarkerData");
        if (data == null) return null;
        return ResourceLocation.tryParse(data.getString("Dimension"));
    }

    /** The section caption, its rule, and the scrolling expectation viewport. */
    private void drawExpectationSection(GuiGraphics g) {
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

        drawViewport(g);
    }

    private void drawViewport(GuiGraphics g) {
        g.enableScissor(
                left + StructMarkerLayout.VIEWPORT.x(),
                top + StructMarkerLayout.VIEWPORT.y(),
                left + StructMarkerLayout.VIEWPORT.right(),
                top + StructMarkerLayout.VIEWPORT.bottom());
        g.pose().pushPose();
        g.pose()
                .translate(
                        StructMarkerLayout.VIEWPORT.x(), StructMarkerLayout.VIEWPORT.y(), 0.0D);

        if (rows.isEmpty()) {
            StructureMinerTheme.emptyState(
                    g,
                    font,
                    0,
                    0,
                    StructMarkerLayout.VIEWPORT_W,
                    StructMarkerLayout.VIEWPORT_H,
                    Component.translatable("screen.dimension_tech.struct_marker.no_items"));
        } else {
            int visible = Math.min(scroll, Math.max(0, rows.size() - StructMarkerLayout.visibleRows()));
            for (int index = visible; index < rows.size(); index++) {
                int y = StructMarkerLayout.rowY(index - visible);
                if (y >= StructMarkerLayout.VIEWPORT_H) break;
                drawRow(g, index, y);
            }
        }

        g.pose().popPose();
        g.disableScissor();

        StructureMinerTheme.scrollbar(
                g,
                StructMarkerLayout.VIEWPORT.x() + StructMarkerLayout.SCROLLBAR_X,
                StructMarkerLayout.VIEWPORT.y(),
                StructMarkerLayout.VIEWPORT_H,
                rows.size() * StructMarkerLayout.ROW_H,
                StructMarkerLayout.VIEWPORT_H,
                scroll * StructMarkerLayout.ROW_H);
    }

    /** One expectation row: item icon, name, and the expected count right-aligned against it. */
    private void drawRow(GuiGraphics g, int index, int y) {
        Row row = rows.get(index);
        if (index == hoveredRow) {
            g.fill(0, y, StructMarkerLayout.VIEWPORT_W, y + StructMarkerLayout.ROW_H, ROW_HOVER);
        }

        ItemStack stack = new ItemStack(row.item);
        g.renderItem(stack, StructMarkerLayout.ROW_PAD, y + 1);

        String expected = ReadingFormat.reading(row.expected.finiteDoubleValue());
        int expectedWidth = font.width(expected);
        String name =
                font.plainSubstrByWidth(
                        stack.getHoverName().getString(),
                        Math.max(
                                1,
                                StructMarkerLayout.CONTENT_RIGHT
                                        - StructMarkerLayout.ROW_NAME_X
                                        - expectedWidth
                                        - 6));
        g.drawString(
                font,
                name,
                StructMarkerLayout.ROW_NAME_X,
                y + 4,
                StructureMinerTheme.INK,
                false);
        g.drawString(
                font,
                expected,
                StructMarkerLayout.CONTENT_RIGHT - expectedWidth,
                y + 4,
                StructureMinerTheme.FLUIX,
                false);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (int) Math.signum(delta);
        scroll = Math.max(0, Math.min(maxScroll(), scroll - step));
        return true;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - StructMarkerLayout.visibleRows());
    }

    /** Expectation-row index under a screen point, or {@code -1}. */
    private int rowAt(double mouseX, double mouseY) {
        int viewportX = (int) mouseX - left - StructMarkerLayout.VIEWPORT.x();
        int viewportY = (int) mouseY - top - StructMarkerLayout.VIEWPORT.y();
        if (viewportX < 0 || viewportX >= StructMarkerLayout.VIEWPORT_W) return -1;
        if (viewportY < 0 || viewportY >= StructMarkerLayout.VIEWPORT_H) return -1;
        int index = scroll + viewportY / StructMarkerLayout.ROW_H;
        return index >= 0 && index < rows.size() ? index : -1;
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

    private static boolean inside(
            double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private record Row(Item item, ExactProbability expected) {}
}
