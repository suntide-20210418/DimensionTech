package com.suntide_20210418.dimensiontech.client.gui.screen;

/**
 * Pixel-locked layout for the three miner pages.
 *
 * <p><b>Coordinate space.</b> Every value here is page-local. {@code MythicMinerScreen#drawPage}
 * translates the pose by {@code (leftPos, topPos)} and clips to {@link #INFO_X}/{@link #INFO_Y} plus
 * {@link #INFO_W}/{@link #INFO_H}, so a page draws in exactly these numbers. Only
 * {@code GuiGraphics.enableScissor} needs the absolute projection — use
 * {@code MythicMinerLayout.scaleToScreen} for that.
 *
 * <p><b>Grid.</b> The canvas is 208x123 and content runs from {@link #CONTENT_X} to
 * {@link #CONTENT_RIGHT}. The marker lane holds up to nine {@link #MARKER_SIZE} cells at
 * {@link #MARKER_STRIDE}, centred. The vanilla slot pass is *not* responsible for that lane: the menu
 * keeps its slots off-screen and the pages draw them.
 *
 * <p><b>Page shapes.</b> Work uses a two-column meter grid (8 readings + 3 status chips); info uses
 * one flat scrolling viewport with no collapsing sections; attributes uses two columns sharing a
 * single scrollbar. The three share one principle: values that change stay above the fold, static
 * lists scroll.
 */
final class MythicMinerInfoLayout {
    // --- canvas ------------------------------------------------------------
    static final int INFO_X = 32, INFO_Y = 33, INFO_W = 208, INFO_H = 123;
    static final int CONTENT_X = 36, CONTENT_RIGHT = 236;
    static final int CONTENT_W = CONTENT_RIGHT - CONTENT_X;

    // --- page tabs ---------------------------------------------------------
    static final int WORK_BUTTON_X = 32, INFO_BUTTON_X = 66, ATTR_BUTTON_X = 100, BUTTON_Y = 16;

    // --- marker lane / thread selector -------------------------------------
    /** Edge length of one marker cell; also the thread selector's button size. */
    static final int MARKER_SIZE = 20;

    /** Horizontal pitch: a 20px cell plus a 2px gutter. */
    static final int MARKER_STRIDE = 22;

    /** Left edge of a full nine-cell lane, already centred inside the content width. */
    static final int MARKER_X = CONTENT_X + (CONTENT_W - laneWidth(9)) / 2;

    /** Top edge of the lane, one gutter below the canvas top. */
    static final int MARKER_Y = INFO_Y + 4;

    /** The 18x18 spritesheet slot face sits this far inside the 20px cell. */
    static final int MARKER_FACE_INSET = 1;

    /** The 16x16 item icon sits this far inside the same cell. */
    static final int MARKER_ICON_INSET = 2;

    // --- progress strip ----------------------------------------------------
    /** Flush under the cell: the gap is required to be zero, so this is derived, not typed. */
    static final int MARKER_PROGRESS_Y = MARKER_Y + MARKER_SIZE;

    static final int MARKER_PROGRESS_W = 16, MARKER_PROGRESS_H = 4;

    /** A 16px strip centred under a 20px cell. */
    static final int MARKER_PROGRESS_INSET = (MARKER_SIZE - MARKER_PROGRESS_W) / 2;

    // --- work page: two-column meter grid ----------------------------------
    static final int WORK_METER_ROW_1_Y = 65, WORK_METER_ROW_H = 15, WORK_METER_ROWS = 4;
    static final int WORK_METER_COL_1_X = CONTENT_X, WORK_METER_COL_2_X = 138;
    static final int WORK_METER_COL_W = 98, WORK_METER_COL_GAP = 4;

    /** Width of the accent rail that carries a row's data axis. */
    static final int WORK_METER_RAIL_W = 2;

    static final int WORK_CHIP_Y = 136, WORK_CHIP_W = 64, WORK_CHIP_H = 12, WORK_CHIP_GAP = 4;

    // --- info page: one flat scrolling viewport -----------------------------
    static final int INFO_LIST_X = 36, INFO_LIST_Y = 62, INFO_LIST_W = 190, INFO_LIST_H = 94;

    // --- attributes page: one column in a single scrollable list (A1) ---------
    static final int ATTR_HEADER_Y = INFO_Y + 4, ATTR_HEADER_H = 12;
    static final int ATTR_LIST_X = 36, ATTR_LIST_Y = 55, ATTR_LIST_W = 190, ATTR_LIST_H = 101;

    /**
     * The attributes page's single content column, clear of the scrollbar grab zone.
     *
     * <p>It replaced the two-column layout, whose 88px readout column truncated Chinese labels and
     * left the composition breakdown entirely below the fold.
     */
    static final int ATTR_COL_X = 40, ATTR_COL_W = 182;

    /** Inner padding between a viewport edge and its content. */
    static final int VIEWPORT_PAD = 4;

    // --- shared list metrics -----------------------------------------------
    static final int ROW_H_INTERACTIVE = 20, ROW_H_DATA = 13, SECTION_HEADER_H = 18;

    /** Scrollbar x, shared by both scrolling pages. */
    static final int SCROLLBAR_X = INFO_LIST_X + INFO_LIST_W;

    static final int SCROLLBAR_W = 3;

    /**
     * Width of the scrollbar's grab zone, wider than the bar itself.
     *
     * <p>Three pixels is a drawn width, not a target a hand can hit. The extra margin is invisible
     * and overlaps the content column's right edge by a few pixels, which is harmless because the
     * bar is tested first.
     */
    static final int SCROLLBAR_GRAB_W = 9;

    // --- external control rail (drawn outside the page canvas) --------------
    static final int EXTERNAL_BUTTON_X = -21, EXTERNAL_BUTTON_Y = 0, EXTERNAL_BUTTON_STRIDE = 20;
    static final int EXTERNAL_ICON_INSET = 2;

    /** The redstone icon is drawn this many pixels above the ordinary icon slots. */
    static final int REDSTONE_ICON_RISE = 2;

    private MythicMinerInfoLayout() {}

    /** Width of a lane holding {@code slotCount} cells. */
    static int laneWidth(int slotCount) {
        return MARKER_SIZE + MARKER_STRIDE * (Math.max(1, slotCount) - 1);
    }

    /**
     * Centred left edge of the {@code index}-th cell in a lane of {@code slotCount} cells.
     *
     * <p>{@code CONTENT_W - laneWidth} is always even ({@code 202 - 22n}), so the lane centres
     * exactly at every slot count and never lands on a half pixel.
     */
    static int laneX(int index, int slotCount) {
        return CONTENT_X + (CONTENT_W - laneWidth(slotCount)) / 2 + index * MARKER_STRIDE;
    }

    /** Y of the {@code row}-th meter line on the work page. */
    static int workMeterY(int row) {
        return WORK_METER_ROW_1_Y + row * WORK_METER_ROW_H;
    }

    /** X of the {@code column}-th meter column on the work page, {@code 0} or {@code 1}. */
    static int workMeterX(int column) {
        return column == 0 ? WORK_METER_COL_1_X : WORK_METER_COL_2_X;
    }

    /**
     * Marker cell index under a panel-local point, or {@code -1}.
     *
     * <p>Lives here so the work page's lane and the info page's thread selector hit-test against the
     * same arithmetic that draws them — the menu cannot answer this, because it parks its slots
     * off-screen.
     */
    static int markerSlotAt(double localX, double localY, int slotCount) {
        if (localY < MARKER_Y || localY >= MARKER_Y + MARKER_SIZE) return -1;
        for (int slot = 0; slot < slotCount; slot++) {
            int x = laneX(slot, slotCount);
            if (localX >= x && localX < x + MARKER_SIZE) return slot;
        }
        return -1;
    }

    /** True when a panel-local point falls inside the given rectangle. */
    static boolean inside(double localX, double localY, int x, int y, int width, int height) {
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    /**
     * Marker cell whose progress strip is under a panel-local point, or {@code -1}.
     *
     * <p>The strip band starts exactly where the cells end, so this and {@link
     * #markerSlotAt} partition the lane: every y in it belongs to exactly one of them.
     */
    static int markerProgressAt(double localX, double localY, int slotCount) {
        if (localY < MARKER_PROGRESS_Y || localY >= MARKER_PROGRESS_Y + MARKER_PROGRESS_H) return -1;
        for (int slot = 0; slot < slotCount; slot++) {
            int x = laneX(slot, slotCount) + MARKER_PROGRESS_INSET;
            if (localX >= x && localX < x + MARKER_PROGRESS_W) return slot;
        }
        return -1;
    }

    /**
     * True when a point is inside the scrollbar's grab zone, {@link #SCROLLBAR_GRAB_W} wide and
     * centred on the {@link #SCROLLBAR_W}-wide drawn bar.
     *
     * <p>Used for grabbing the bar, not for deciding whether the wheel applies — see
     * {@code MythicMinerPageRenderer#mouseScrolled} for why the wheel deliberately has no
     * position gate.
     */
    static boolean scrollColumnContains(double localX, double localY, int viewportY, int viewportH) {
        int pad = (SCROLLBAR_GRAB_W - SCROLLBAR_W) / 2;
        return localX >= SCROLLBAR_X - pad
                && localX < SCROLLBAR_X + SCROLLBAR_W + pad
                && localY >= viewportY
                && localY < viewportY + viewportH;
    }

    /**
     * Scroll offset for a pointer at {@code y}, grabbed at {@code originY} when the offset was
     * {@code originScroll}, clamped to the scrollable range.
     *
     * <p>Grab-relative rather than proportional: the thumb keeps the pointer where the player took
     * hold of it instead of snapping under the cursor on the first pixel of movement.
     */
    static int scrollFromDrag(
            int originScroll, double originY, double y, int contentHeight, int viewportH) {
        int range = Math.max(0, contentHeight - viewportH);
        if (range == 0 || viewportH <= 0) return 0;
        int moved = (int) Math.round((y - originY) * range / (double) viewportH);
        return Math.max(0, Math.min(range, originScroll + moved));
    }
}
