package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The shared expectation grid: a scrollable matrix of item cells whose hover tooltip appends the
 * cell's expected count and value multiplier under the vanilla item tooltip.
 *
 * <p><b>Stateful on purpose.</b> Column count and cell size are construction parameters and the
 * scroll offset is live state, so this is an object rather than a bag of static painters. The four
 * call sites disagree about where the scroll offset lives — two hang it on a page context, two on
 * the screen — and an object is the only shape that can own it in all four.
 *
 * <p><b>Geometry only, no data policy.</b> Ordering and the equipment-dismantling merge stay with
 * the caller: one site sorts by a user-selected column and another merges equipment into materials,
 * neither of which is a rendering concern. The multiplier arrives pre-computed on the row for the
 * same reason — {@code itemMultiplier} scans and compiles regexes, so calling it per cell per frame
 * would be a real cost, not a theoretical one.
 *
 * <p><b>Two coordinate spaces.</b> Every {@code x}/{@code y}/{@code viewport*} argument is in the
 * caller's current space — page-local for a page renderer, panel-local for a screen that has
 * already translated its pose. The scissor is the exception and takes a {@link
 * StructureMinerLayout.ScissorBounds} the caller builds itself, because {@code
 * GuiGraphics#enableScissor} applies the window GUI scale but ignores the pose, and one caller
 * additionally scales by its own UI scale factor. Keeping that projection with the caller is what
 * stops this class from guessing a scale it cannot know.
 */
final class ItemExpectationGrid {

    /**
     * One cell.
     *
     * <p>A stack rather than a bare item: the icon pass needs one anyway, rarity — which the
     * multiplier was derived from — is a stack property, and carrying it lets the caller build it
     * once per list rebuild instead of once per frame per cell.
     */
    record Entry(ItemStack stack, double expected, double multiplier, boolean enabled) {
        Entry(ItemStack stack, double expected, double multiplier) {
            this(stack, expected, multiplier, true);
        }
    }

    /** Drawn width of the scrollbar, matching {@link GuiChrome#scrollbar}. */
    private static final int BAR_W = 3;

    /**
     * Width of the scrollbar's grab zone, wider than the bar itself.
     *
     * <p>Three pixels is a drawn width, not a target a hand can hit. The extra margin is invisible
     * and overlaps the grid's right edge by a few pixels, which is harmless because the bar is
     * tested before the cell.
     */
    private static final int GRAB_W = 9;

    // --- construction-time geometry ----------------------------------------
    private final int columns;
    private final int cellW;
    private final int cellH;
    private final int cellGap;
    private final int iconInset;

    /**
     * When false the grid never scrolls and never draws a bar.
     *
     * <p>For the page that embeds the grid inside a larger scrolling document: two nested scroll
     * regions on one pointer would have to negotiate the wheel, the keyboard and the drag bar, and
     * the inner one buys nothing when the outer page already scrolls.
     */
    private final boolean scrollable;

    // --- live state --------------------------------------------------------
    private int scroll;
    private int selected = -1;

    /**
     * Index the pointer grabbed the scrollbar at, or {@code -1}.
     *
     * <p>Unlike {@link #scroll} this is transient pointer state, not layout state: it exists only
     * between {@code mouseClicked} and {@code mouseReleased} and must survive across frames,
     * because the drag is delivered as a stream of {@code mouseDragged} calls.
     */
    private int dragIndex = -1;

    /** Scroll offset captured when the drag started, so the move is relative to the grab. */
    private int dragOriginScroll;

    /** Pointer y captured when the drag started. */
    private int dragOriginY;

    /** True while the pointer is being held on the bar. */
    private boolean dragging;

    ItemExpectationGrid(int columns, int cellW, int cellH, int cellGap, boolean scrollable) {
        this.columns = Math.max(1, columns);
        this.cellW = Math.max(1, cellW);
        this.cellH = Math.max(1, cellH);
        this.cellGap = Math.max(0, cellGap);
        this.iconInset = Math.max(0, (this.cellW - 16) / 2);
        this.scrollable = scrollable;
    }

    /**
     * The standard 18px slot sprite on a 2px pitch.
     *
     * <p>18 is not a preference: {@link StructureMinerSpriteRenderer#marker} draws an 18x18 sprite,
     * so a smaller cell would let neighbours overlap it.
     */
    static ItemExpectationGrid ofSpriteSlots(int columns, boolean scrollable) {
        return new ItemExpectationGrid(columns, 18, 18, 2, scrollable);
    }

    // --- geometry ----------------------------------------------------------

    int cellW() {
        return cellW;
    }

    int cellH() {
        return cellH;
    }

    int iconInset() {
        return iconInset;
    }

    int columns() {
        return columns;
    }

    int strideX() {
        return cellW + cellGap;
    }

    int strideY() {
        return cellH + cellGap;
    }

    /** Width of the whole grid at the configured column count. */
    int gridWidth() {
        return columns * cellW + (columns - 1) * cellGap;
    }

    int rowCount(int itemCount) {
        return itemCount <= 0 ? 0 : (itemCount + columns - 1) / columns;
    }

    /**
     * Full content height for {@code itemCount} cells.
     *
     * <p>The trailing gutter is subtracted so the last row does not reserve a gap that is never
     * drawn — without it a full list scrolls by two pixels it does not have.
     */
    int contentHeight(int itemCount) {
        int rows = rowCount(itemCount);
        return rows <= 0 ? 0 : rows * cellH + (rows - 1) * cellGap;
    }

    int visibleRows(int viewportH) {
        return Math.max(1, viewportH / Math.max(1, strideY()));
    }

    int maxScroll(int itemCount, int viewportH) {
        if (!scrollable) return 0;
        return Math.max(0, contentHeight(itemCount) - viewportH);
    }

    /** X of the left edge of column {@code col}, in viewport-local pixels. */
    int cellX(int col) {
        return col * strideX();
    }

    /** Y of the top edge of row {@code row}, in viewport-local pixels, after the scroll offset. */
    int cellY(int row) {
        return row * strideY() - scroll;
    }

    /** X of the drawn scrollbar, in viewport-local pixels. */
    int barX(int viewportW) {
        return viewportW - cellGap - BAR_W;
    }

    int scroll() {
        return scroll;
    }

    int selected() {
        return selected;
    }

    void select(int index) {
        selected = index;
    }

    // --- render ------------------------------------------------------------

    /**
     * Draws the grid: slot faces, item icons, disabled washes, the hover wash, the scrollbar and
     * the empty state.
     *
     * <p>The scissor opens before the pose is translated and closes after it is restored, so the
     * clip is applied in absolute pixels while everything inside is drawn in local ones. The
     * tooltip pass is deliberately not part of this method — see {@link #renderTooltip}.
     */
    void render(
            GuiGraphics g,
            Font font,
            List<Entry> items,
            int x,
            int y,
            int viewportW,
            int viewportH,
            StructureMinerLayout.ScissorBounds clip,
            int mouseLocalX,
            int mouseLocalY,
            Component emptyLabel) {
        if (items.isEmpty()) {
            if (emptyLabel != null) {
                GuiChrome.emptyState(g, font, x, y, viewportW, viewportH, emptyLabel);
            }
            return;
        }

        clampScroll(items.size(), viewportH);
        int hovered = cellAt(items, mouseLocalX, mouseLocalY, viewportW, viewportH);
        int right = Math.min(viewportW, gridWidth());

        g.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
        g.pose().pushPose();
        g.pose().translate(x, y, 0);

        int rows = rowCount(items.size());
        for (int row = 0; row < rows; row++) {
            int cy = cellY(row);
            if (cy >= viewportH) break;
            if (cy + cellH <= 0) continue;
            for (int col = 0; col < columns; col++) {
                int index = row * columns + col;
                if (index >= items.size()) break;
                int cx = cellX(col);
                if (cx + cellW > right) break;
                drawCell(g, items.get(index), index, cx, cy, index == hovered);
            }
        }

        g.pose().popPose();
        g.disableScissor();

        if (scrollable) {
            // Outside the scissor, so the bar is never clipped by the grid it belongs to.
            GuiChrome.scrollbar(
                    g,
                    x + barX(viewportW),
                    y,
                    viewportH,
                    contentHeight(items.size()),
                    viewportH,
                    scroll);
        }
    }

    /**
     * One cell, in the order the layers have to land.
     *
     * <p>The slot face is under the icon because the sprite is opaque; the disabled wash and the
     * hover wash are over it because both are translucent and would be hidden by an opaque icon
     * below them. Selection is a sprite swap rather than a wash, so a selected cell that is also
     * hovered still reads as selected.
     */
    private void drawCell(GuiGraphics g, Entry entry, int index, int cx, int cy, boolean hovered) {
        if (scrollable) {
            StructureMinerSpriteRenderer.marker(g, cx, cy, index == selected);
        } else {
            StructureMinerSpriteRenderer.marker(g, cx, cy, false);
        }

        if (!entry.stack().isEmpty()) {
            // ItemStack#render now routes through the item model system, which lives on the
            // client level rather than the stack, so no level is needed here.
            g.renderItem(entry.stack(), cx + iconInset, cy + iconInset);
        }

        if (!entry.enabled()) {
            g.fill(cx, cy, cx + cellW, cy + cellH, StructureMinerTheme.DISABLED_OVERLAY);
        }
        if (hovered && index != selected) {
            g.fill(cx, cy, cx + cellW, cy + cellH, StructureMinerTheme.HOVER_WASH);
        }
    }

    /**
     * Tooltip pass: the vanilla item tooltip with the cell's expectation and multiplier appended.
     *
     * <p>Called after the pose is restored and the scissor closed — {@code renderTooltip} draws
     * under the current pose and leaves the scissor alone, so a tooltip issued inside a clip region
     * would be cut off by it and would land at the wrong offset.
     */
    void renderTooltip(
            GuiGraphics g,
            Font font,
            List<Entry> items,
            int localX,
            int localY,
            int viewportW,
            int viewportH,
            int screenX,
            int screenY) {
        int index = cellAt(items, localX, localY, viewportW, viewportH);
        if (index < 0) return;
        Entry entry = items.get(index);

        Minecraft minecraft = Minecraft.getInstance();
        // Vanilla's own line list, so enchantments, durability, food values and the item's tooltip
        // image all survive. Hand-assembling the box and drawing two extra lines under it instead
        // would have to clone vanilla's padding and border constants and would drift on any update.
        List<Component> lines =
                new ArrayList<>(Screen.getTooltipFromItem(minecraft, entry.stack()));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.struct_marker.tooltip.expected",
                        ReadingFormat.reading(entry.expected())));
        /*
         * The short amount is a second line rather than a replacement for the reading above. The
         * reading is the number the machine will actually average over many cycles, decimals and
         * all, and the short form is what a player compares between cells at a glance — dropping
         * either would lose something the other carries.
         */
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.struct_marker.tooltip.quantity",
                        ReadingFormat.quantity(entry.expected())));
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.struct_marker.multiplier",
                        String.format(Locale.ROOT, "%.2f", entry.multiplier())));
        if (!entry.enabled()) {
            lines.add(Component.translatable("screen.dimension_tech.struct_marker.item.disabled"));
        }

        g.renderTooltip(font, lines, entry.stack().getTooltipImage(), screenX, screenY);
    }

    // --- hit testing -------------------------------------------------------

    /**
     * Cell index under a local point, or {@code -1}.
     *
     * <p>Negative coordinates are rejected outright rather than left to {@code /} and {@code %},
     * whose Java semantics would turn {@code x = -1} into column 0 with a negative remainder — a
     * point one pixel left of the grid would select the first cell. The two gutter tests keep a
     * pointer in the gap between cells from selecting a neighbour.
     */
    int cellAt(List<Entry> items, int localX, int localY, int viewportW, int viewportH) {
        if (localX < 0 || localY < 0) return -1;
        if (localX >= Math.min(viewportW, gridWidth())) return -1;
        if (localY >= viewportH) return -1;

        int col = localX / strideX();
        if (col >= columns || localX % strideX() >= cellW) return -1;

        int contentY = localY + scroll;
        if (contentY % strideY() >= cellH) return -1;

        int index = (contentY / strideY()) * columns + col;
        return index < items.size() ? index : -1;
    }

    /**
     * True when a local point is inside the scrollbar's grab zone.
     *
     * <p>False when there is nothing to scroll, so an empty bar cannot swallow a click. The zone is
     * {@link #GRAB_W} wide and centred on the {@link #BAR_W} drawn bar.
     */
    boolean scrollbarContains(int localX, int localY, int viewportW, int viewportH, int itemCount) {
        if (!scrollable) return false;
        if (maxScroll(itemCount, viewportH) == 0) return false;
        int pad = (GRAB_W - BAR_W) / 2;
        int bar = barX(viewportW);
        return localX >= bar - pad
                && localX < bar + BAR_W + pad
                && localY >= 0
                && localY < viewportH;
    }

    // --- scroll ------------------------------------------------------------

    /** Moves the offset by whole rows; the caller passes -1 or 1 for one wheel notch. */
    void scrollBy(int stepRows, int itemCount, int viewportH) {
        scroll = clamp(scroll + stepRows * strideY(), itemCount, viewportH);
    }

    void clampScroll(int itemCount, int viewportH) {
        scroll = clamp(scroll, itemCount, viewportH);
    }

    /**
     * Applies a drag, from the offset the pointer took hold at.
     *
     * <p>Grab-relative rather than proportional: the thumb keeps the pixel the player grabbed
     * instead of snapping under the cursor on the first movement.
     */
    void dragTo(int originScroll, int originY, int y, int itemCount, int viewportH) {
        int range = maxScroll(itemCount, viewportH);
        if (range == 0 || viewportH <= 0) {
            scroll = 0;
            return;
        }
        int moved = (int) Math.round((y - originY) * range / (double) viewportH);
        scroll = Math.max(0, Math.min(range, originScroll + moved));
    }

    private int clamp(int value, int itemCount, int viewportH) {
        return Math.max(0, Math.min(maxScroll(itemCount, viewportH), value));
    }

    // --- scrollbar drag ----------------------------------------------------
    /*
     * The three calls below are the whole drag protocol. It is a protocol rather than one call
     * because the pointer events arrive as three separate ones: a press, then any number of moves,
     * then a release, and the offset captured at the press has to survive until the release or the
     * thumb would snap under the cursor on the first pixel of movement.
     */

    /** Begins a drag, recording where the pointer took hold of the bar. */
    void beginDrag(int localY) {
        dragging = true;
        dragOriginY = localY;
        dragOriginScroll = scroll;
    }

    /** Applies a move while dragging; a no-op if no drag is in flight. */
    void continueDrag(int localY, int itemCount, int viewportH) {
        if (!dragging) return;
        dragTo(dragOriginScroll, dragOriginY, localY, itemCount, viewportH);
    }

    /** Ends a drag, whether or not the pointer is still over the bar. */
    void endDrag() {
        dragging = false;
    }

    boolean dragging() {
        return dragging;
    }
}
