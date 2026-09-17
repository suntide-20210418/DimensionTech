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
class MythicMinerInfoLayoutTest {
    /** Canvas bottom row, exclusive. Every page must end at or above it. */
    private static final int CANVAS_BOTTOM = 156;

    @Test
    void pageButtonsUsePixelLockedPositions() {
        assertEquals(32, MythicMinerInfoLayout.WORK_BUTTON_X);
        assertEquals(66, MythicMinerInfoLayout.INFO_BUTTON_X);
        assertEquals(100, MythicMinerInfoLayout.ATTR_BUTTON_X);
        assertEquals(16, MythicMinerInfoLayout.BUTTON_Y);
    }

    @Test
    void canvasAndGuttersArePixelLocked() {
        assertEquals(208, MythicMinerInfoLayout.INFO_W);
        assertEquals(123, MythicMinerInfoLayout.INFO_H);
        assertEquals(200, MythicMinerInfoLayout.CONTENT_W);
        assertEquals(
                MythicMinerInfoLayout.CONTENT_RIGHT - MythicMinerInfoLayout.CONTENT_X,
                MythicMinerInfoLayout.CONTENT_W);
    }

    @Test
    void nineMarkerCellsFitInsideTheContentWidth() {
        assertEquals(20, MythicMinerInfoLayout.MARKER_SIZE);
        assertEquals(22, MythicMinerInfoLayout.MARKER_STRIDE);
        assertEquals(38, MythicMinerInfoLayout.MARKER_X);

        int laneRight =
                MythicMinerInfoLayout.MARKER_X
                        + 8 * MythicMinerInfoLayout.MARKER_STRIDE
                        + MythicMinerInfoLayout.MARKER_SIZE;
        assertTrue(
                laneRight <= MythicMinerInfoLayout.CONTENT_RIGHT,
                "nine cells end at "
                        + laneRight
                        + ", content ends at "
                        + MythicMinerInfoLayout.CONTENT_RIGHT);
    }

    @Test
    void markerLaneIsCentredAtEverySlotCount() {
        for (int slotCount = 1; slotCount <= 9; slotCount++) {
            int left = MythicMinerInfoLayout.laneX(0, slotCount);
            int right =
                    MythicMinerInfoLayout.laneX(slotCount - 1, slotCount)
                            + MythicMinerInfoLayout.MARKER_SIZE;
            int leftGutter = left - MythicMinerInfoLayout.CONTENT_X;
            int rightGutter = MythicMinerInfoLayout.CONTENT_RIGHT - right;

            assertEquals(leftGutter, rightGutter, "slotCount=" + slotCount);
        }
    }

    @Test
    void laneWidthGrowsByExactlyOneStridePerCell() {
        for (int slotCount = 1; slotCount < 9; slotCount++) {
            assertEquals(
                    MythicMinerInfoLayout.MARKER_STRIDE,
                    MythicMinerInfoLayout.laneWidth(slotCount + 1)
                            - MythicMinerInfoLayout.laneWidth(slotCount));
        }
    }

    @Test
    void progressStripSitsFlushUnderTheCell() {
        assertEquals(
                MythicMinerInfoLayout.MARKER_Y + MythicMinerInfoLayout.MARKER_SIZE,
                MythicMinerInfoLayout.MARKER_PROGRESS_Y);
        assertEquals(16, MythicMinerInfoLayout.MARKER_PROGRESS_W);
        assertEquals(4, MythicMinerInfoLayout.MARKER_PROGRESS_H);
        assertEquals(
                MythicMinerInfoLayout.MARKER_SIZE,
                MythicMinerInfoLayout.MARKER_PROGRESS_W
                        + 2 * MythicMinerInfoLayout.MARKER_PROGRESS_INSET);
    }

    @Test
    void workPageMeterGridFillsTheContentWidthExactly() {
        assertEquals(
                MythicMinerInfoLayout.WORK_METER_COL_2_X,
                MythicMinerInfoLayout.WORK_METER_COL_1_X
                        + MythicMinerInfoLayout.WORK_METER_COL_W
                        + MythicMinerInfoLayout.WORK_METER_COL_GAP);
        assertEquals(
                MythicMinerInfoLayout.CONTENT_RIGHT,
                MythicMinerInfoLayout.WORK_METER_COL_2_X + MythicMinerInfoLayout.WORK_METER_COL_W);
        assertEquals(
                MythicMinerInfoLayout.CONTENT_W,
                3 * MythicMinerInfoLayout.WORK_CHIP_W + 2 * MythicMinerInfoLayout.WORK_CHIP_GAP);
    }

    @Test
    void workPageMeterRowsClearTheStatusChips() {
        for (int row = 0; row < MythicMinerInfoLayout.WORK_METER_ROWS; row++) {
            assertEquals(
                    MythicMinerInfoLayout.WORK_METER_ROW_1_Y
                            + row * MythicMinerInfoLayout.WORK_METER_ROW_H,
                    MythicMinerInfoLayout.workMeterY(row));
        }
        assertTrue(
                MythicMinerInfoLayout.workMeterY(MythicMinerInfoLayout.WORK_METER_ROWS - 1)
                                + MythicMinerInfoLayout.WORK_METER_ROW_H
                        <= MythicMinerInfoLayout.WORK_CHIP_Y);
        assertEquals(
                MythicMinerInfoLayout.WORK_METER_COL_1_X, MythicMinerInfoLayout.workMeterX(0));
        assertEquals(
                MythicMinerInfoLayout.WORK_METER_COL_2_X, MythicMinerInfoLayout.workMeterX(1));
    }

    @Test
    void attributesColumnStaysInsideTheViewport() {
        assertTrue(
                MythicMinerInfoLayout.ATTR_COL_X
                        >= MythicMinerInfoLayout.ATTR_LIST_X + MythicMinerInfoLayout.VIEWPORT_PAD);
        assertTrue(
                MythicMinerInfoLayout.ATTR_COL_X + MythicMinerInfoLayout.ATTR_COL_W
                        < MythicMinerInfoLayout.SCROLLBAR_X
                                - (MythicMinerInfoLayout.SCROLLBAR_GRAB_W
                                                - MythicMinerInfoLayout.SCROLLBAR_W)
                                        / 2);
    }

    @Test
    void markerSlotHitTestMatchesTheLaneGeometry() {
        for (int slotCount = 1; slotCount <= 9; slotCount++) {
            for (int slot = 0; slot < slotCount; slot++) {
                assertEquals(
                        slot,
                        MythicMinerInfoLayout.markerSlotAt(
                                MythicMinerInfoLayout.laneX(slot, slotCount)
                                        + MythicMinerInfoLayout.MARKER_SIZE / 2.0,
                                MythicMinerInfoLayout.MARKER_Y + 1,
                                slotCount),
                        "slotCount=" + slotCount + " slot=" + slot);
            }
            assertTrue(
                    MythicMinerInfoLayout.markerSlotAt(
                                    MythicMinerInfoLayout.MARKER_X - 8,
                                    MythicMinerInfoLayout.MARKER_Y + 1,
                                    slotCount)
                            < 0);
        }
        // Vertically outside the lane is a miss at every slot count.
        assertTrue(
                MythicMinerInfoLayout.markerSlotAt(
                                MythicMinerInfoLayout.MARKER_X, MythicMinerInfoLayout.MARKER_Y - 1, 9)
                        < 0);
        assertTrue(
                MythicMinerInfoLayout.markerSlotAt(
                                MythicMinerInfoLayout.MARKER_X,
                                MythicMinerInfoLayout.MARKER_Y + MythicMinerInfoLayout.MARKER_SIZE,
                                9)
                        < 0);
    }

    @Test
    void everyPageEndsInsideTheCanvas() {
        assertTrue(
                MythicMinerInfoLayout.MARKER_PROGRESS_Y + MythicMinerInfoLayout.MARKER_PROGRESS_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                MythicMinerInfoLayout.WORK_CHIP_Y + MythicMinerInfoLayout.WORK_CHIP_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                MythicMinerInfoLayout.INFO_LIST_Y + MythicMinerInfoLayout.INFO_LIST_H
                        <= CANVAS_BOTTOM);
        assertTrue(
                MythicMinerInfoLayout.ATTR_LIST_Y + MythicMinerInfoLayout.ATTR_LIST_H
                        <= CANVAS_BOTTOM);
    }

    @Test
    void scrollbarSitsJustRightOfTheListColumn() {
        assertEquals(
                MythicMinerInfoLayout.INFO_LIST_X + MythicMinerInfoLayout.INFO_LIST_W,
                MythicMinerInfoLayout.SCROLLBAR_X);
    }
}
