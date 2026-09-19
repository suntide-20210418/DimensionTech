package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the three-page grid: one canvas, one centred marker lane, nothing outside the canvas.
 *
 * <p>These are the numbers every page renderer draws with, so a change here changes all three pages
 * at once — which is exactly why they are asserted instead of eyeballed.
 */
class StructureMinerInfoLayoutTest {
    /** Canvas bottom row, exclusive. Every page must end at or above it. */
    private static final int CANVAS_BOTTOM = 156;

    @Test
    void pageButtonsUsePixelLockedPositions() {
        assertEquals(32, StructureMinerInfoLayout.WORK_BUTTON_X);
        assertEquals(66, StructureMinerInfoLayout.INFO_BUTTON_X);
        assertEquals(100, StructureMinerInfoLayout.ATTR_BUTTON_X);
        assertEquals(16, StructureMinerInfoLayout.BUTTON_Y);
    }

    @Test
    void canvasAndGuttersArePixelLocked() {
        assertEquals(208, StructureMinerInfoLayout.INFO_W);
        assertEquals(123, StructureMinerInfoLayout.INFO_H);
        assertEquals(200, StructureMinerInfoLayout.CONTENT_W);
        assertEquals(
                StructureMinerInfoLayout.CONTENT_RIGHT - StructureMinerInfoLayout.CONTENT_X,
                StructureMinerInfoLayout.CONTENT_W);
    }

    @Test
    void nineMarkerCellsFitInsideTheContentWidth() {
        assertEquals(20, StructureMinerInfoLayout.MARKER_SIZE);
        assertEquals(22, StructureMinerInfoLayout.MARKER_STRIDE);
        assertEquals(38, StructureMinerInfoLayout.MARKER_X);

        int laneRight =
                StructureMinerInfoLayout.MARKER_X
                        + 8 * StructureMinerInfoLayout.MARKER_STRIDE
                        + StructureMinerInfoLayout.MARKER_SIZE;
        assertTrue(
                laneRight <= StructureMinerInfoLayout.CONTENT_RIGHT,
                "nine cells end at "
                        + laneRight
                        + ", content ends at "
                        + StructureMinerInfoLayout.CONTENT_RIGHT);
    }

    @Test
    void markerLaneIsCentredAtEverySlotCount() {
        for (int slotCount = 1; slotCount <= 9; slotCount++) {
            int left = StructureMinerInfoLayout.laneX(0, slotCount);
            int right =
                    StructureMinerInfoLayout.laneX(slotCount - 1, slotCount)
                            + StructureMinerInfoLayout.MARKER_SIZE;
            int leftGutter = left - StructureMinerInfoLayout.CONTENT_X;
            int rightGutter = StructureMinerInfoLayout.CONTENT_RIGHT - right;

            assertEquals(leftGutter, rightGutter, "slotCount=" + slotCount);
        }
    }

    @Test
    void laneWidthGrowsByExactlyOneStridePerCell() {
        for (int slotCount = 1; slotCount < 9; slotCount++) {
            assertEquals(
                    StructureMinerInfoLayout.MARKER_STRIDE,
                    StructureMinerInfoLayout.laneWidth(slotCount + 1)
                            - StructureMinerInfoLayout.laneWidth(slotCount));
        }
    }

    @Test
    void progressStripSitsFlushUnderTheCell() {
        assertEquals(
                StructureMinerInfoLayout.MARKER_Y + StructureMinerInfoLayout.MARKER_SIZE,
                StructureMinerInfoLayout.MARKER_PROGRESS_Y);
        assertEquals(16, StructureMinerInfoLayout.MARKER_PROGRESS_W);
        assertEquals(4, StructureMinerInfoLayout.MARKER_PROGRESS_H);
        assertEquals(
                StructureMinerInfoLayout.MARKER_SIZE,
                StructureMinerInfoLayout.MARKER_PROGRESS_W
                        + 2 * StructureMinerInfoLayout.MARKER_PROGRESS_INSET);
    }

    @Test
    void workPageMeterGridFillsTheContentWidthExactly() {
        assertEquals(
                StructureMinerInfoLayout.WORK_METER_COL_2_X,
                StructureMinerInfoLayout.WORK_METER_COL_1_X
                        + StructureMinerInfoLayout.WORK_METER_COL_W
                        + StructureMinerInfoLayout.WORK_METER_COL_GAP);
        assertEquals(
                StructureMinerInfoLayout.CONTENT_RIGHT,
                StructureMinerInfoLayout.WORK_METER_COL_2_X + StructureMinerInfoLayout.WORK_METER_COL_W);
        assertEquals(
                StructureMinerInfoLayout.CONTENT_W,
                3 * StructureMinerInfoLayout.WORK_CHIP_W + 2 * StructureMinerInfoLayout.WORK_CHIP_GAP);
    }

    @Test
    void workPageMeterRowsClearTheStatusChips() {
        for (int row = 0; row < StructureMinerInfoLayout.WORK_METER_ROWS; row++) {
            assertEquals(
                    StructureMinerInfoLayout.WORK_METER_ROW_1_Y
                            + row * StructureMinerInfoLayout.WORK_METER_ROW_H,
                    StructureMinerInfoLayout.workMeterY(row));
        }
        assertTrue(
                StructureMinerInfoLayout.workMeterY(StructureMinerInfoLayout.WORK_METER_ROWS - 1)
                                + StructureMinerInfoLayout.WORK_METER_ROW_H
                        <= StructureMinerInfoLayout.WORK_CHIP_Y);
        assertEquals(
                StructureMinerInfoLayout.WORK_METER_COL_1_X, StructureMinerInfoLayout.workMeterX(0));
        assertEquals(
                StructureMinerInfoLayout.WORK_METER_COL_2_X, StructureMinerInfoLayout.workMeterX(1));
    }

    @Test
    void attributesColumnStaysInsideTheViewport() {
        assertTrue(
                StructureMinerInfoLayout.ATTR_COL_X
                        >= StructureMinerInfoLayout.ATTR_LIST_X + StructureMinerInfoLayout.VIEWPORT_PAD);
        assertTrue(
                StructureMinerInfoLayout.ATTR_COL_X + StructureMinerInfoLayout.ATTR_COL_W
                        < StructureMinerInfoLayout.SCROLLBAR_X
                                - (StructureMinerInfoLayout.SCROLLBAR_GRAB_W
                                                - StructureMinerInfoLayout.SCROLLBAR_W)
                                        / 2);
    }

    @Test
    void markerSlotHitTestMatchesTheLaneGeometry() {
        for (int slotCount = 1; slotCount <= 9; slotCount++) {
            for (int slot = 0; slot < slotCount; slot++) {
                assertEquals(
                        slot,
                        StructureMinerInfoLayout.markerSlotAt(
                                StructureMinerInfoLayout.laneX(slot, slotCount)
                                        + StructureMinerInfoLayout.MARKER_SIZE / 2.0,
                                StructureMinerInfoLayout.MARKER_Y + 1,
                                slotCount),
                        "slotCount=" + slotCount + " slot=" + slot);
            }
            assertTrue(
                    StructureMinerInfoLayout.markerSlotAt(
                                    StructureMinerInfoLayout.MARKER_X - 8,
                                    StructureMinerInfoLayout.MARKER_Y + 1,
                                    slotCount)
                            < 0);
        }
        // Vertically outside the lane is a miss at every slot count.
        assertTrue(
                StructureMinerInfoLayout.markerSlotAt(
                                StructureMinerInfoLayout.MARKER_X, StructureMinerInfoLayout.MARKER_Y - 1, 9)
                        < 0);
        assertTrue(
                StructureMinerInfoLayout.markerSlotAt(
                                StructureMinerInfoLayout.MARKER_X,
                                StructureMinerInfoLayout.MARKER_Y + StructureMinerInfoLayout.MARKER_SIZE,
                                9)
                        < 0);
    }

    @Test
    void everyPageEndsInsideTheCanvas() {
        assertTrue(
                StructureMinerInfoLayout.MARKER_PROGRESS_Y + StructureMinerInfoLayout.MARKER_PROGRESS_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                StructureMinerInfoLayout.WORK_CHIP_Y + StructureMinerInfoLayout.WORK_CHIP_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                StructureMinerInfoLayout.INFO_LIST_Y + StructureMinerInfoLayout.INFO_LIST_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                StructureMinerInfoLayout.ATTR_LIST_Y + StructureMinerInfoLayout.ATTR_LIST_H
                        <= CANVAS_BOTTOM);
    }

    @Test
    void scrollbarSitsJustRightOfTheListColumn() {
        assertEquals(
                StructureMinerInfoLayout.INFO_LIST_X + StructureMinerInfoLayout.INFO_LIST_W,
                StructureMinerInfoLayout.SCROLLBAR_X);
    }
}
