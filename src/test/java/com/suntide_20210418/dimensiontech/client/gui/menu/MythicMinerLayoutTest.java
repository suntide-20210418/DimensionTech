package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MythicMinerLayoutTest {
    @Test
    void usesRequestedMarkerSlotGrids() {
        assertGrid(1, 1, 1);
        assertGrid(2, 2, 1);
        assertGrid(3, 3, 1);
        assertGrid(4, 2, 2);
        assertGrid(6, 3, 2);
        assertGrid(9, 3, 3);
    }

    @Test
    void markerBayKeepsThreeRowHeightAndContainsEverySlot() {
        assertEquals(80, MythicMinerLayout.markerBayWidth(1));
        assertEquals(80, MythicMinerLayout.markerBayWidth(4));
        assertEquals(80, MythicMinerLayout.markerBayWidth(6));
        assertEquals(94, MythicMinerLayout.markerBayHeight(3));
        assertEquals(94, MythicMinerLayout.markerBayHeight(6));
        assertEquals(94, MythicMinerLayout.markerBayHeight(9));

        for (int slotCount : new int[] {1, 2, 3, 4, 6, 9}) {
            int bayRight =
                    MythicMinerLayout.MARKER_BAY_X + MythicMinerLayout.markerBayWidth(slotCount);
            int bayBottom =
                    MythicMinerLayout.MARKER_BAY_Y + MythicMinerLayout.markerBayHeight(slotCount);
            for (int slot = 0; slot < slotCount; slot++) {
                int row = slot / MythicMinerLayout.columnsForSlotCount(slotCount);
                assertTrue(
                        MythicMinerLayout.markerSlotX(slot, slotCount) - 1
                                >= MythicMinerLayout.MARKER_BAY_X);
                assertTrue(MythicMinerLayout.markerSlotX(slot, slotCount) + 18 <= bayRight);
                assertTrue(
                        MythicMinerLayout.markerSlotY(row) - 1 >= MythicMinerLayout.MARKER_BAY_Y);
                assertTrue(
                        MythicMinerLayout.progressBarY(MythicMinerLayout.markerSlotY(row))
                                        + MythicMinerLayout.PROGRESS_HEIGHT
                                <= bayBottom);
            }
        }
    }

    @Test
    void markerInfoPanelUsesTheSpaceToTheRightOfMarkerSlots() {
        for (int slotCount : new int[] {1, 2, 3, 4, 6, 9}) {
            int markerBayRight =
                    MythicMinerLayout.MARKER_BAY_X
                            + MythicMinerLayout.markerBayWidth(slotCount);
            assertEquals(
                    markerBayRight + MythicMinerLayout.MARKER_INFO_GAP,
                    MythicMinerLayout.markerInfoX(slotCount));
            assertTrue(MythicMinerLayout.markerInfoWidth(slotCount, 320) > 0);
            assertEquals(
                    MythicMinerLayout.markerBayHeight(slotCount),
                    MythicMinerLayout.markerInfoHeight(slotCount));
        }
    }

    @Test
    void progressBarAlignsWithSlotFrameAndDoesNotOverlapNextRow() {
        int slotX = 100;
        int firstSlotY = MythicMinerLayout.markerSlotY(0);
        int secondSlotY = MythicMinerLayout.markerSlotY(1);

        assertEquals(slotX - 1, MythicMinerLayout.progressBarX(slotX));
        assertEquals(18, MythicMinerLayout.PROGRESS_WIDTH);
        assertEquals(3, MythicMinerLayout.PROGRESS_HEIGHT);
        assertTrue(
                MythicMinerLayout.progressBarY(firstSlotY) + MythicMinerLayout.PROGRESS_HEIGHT
                        <= secondSlotY - 1);
    }

    @Test
    void playerInventoryStaysBelowTheFixedMarkerSection() {
        assertEquals(258, MythicMinerLayout.playerInventoryY(1));
        assertEquals(258, MythicMinerLayout.playerInventoryY(2));
        assertEquals(258, MythicMinerLayout.playerInventoryY(3));
    }

    @Test
    void attributesFollowTheFixedMarkerSection() {
        assertEquals(94, MythicMinerLayout.markerBayHeight(3));
        assertEquals(142, MythicMinerLayout.attributeY(3));
        assertEquals(142, MythicMinerLayout.attributeY(6));
        assertEquals(142, MythicMinerLayout.attributeY(9));
    }

    @Test
    void scissorBoundsIncludeGuiOriginAndCustomScale() {
        MythicMinerLayout.ScissorBounds bounds =
                MythicMinerLayout.scaleToScreen(101, 53, 80, 35, 0.75F);

        assertEquals(75, bounds.left());
        assertEquals(39, bounds.top());
        assertEquals(136, bounds.right());
        assertEquals(66, bounds.bottom());
    }

    private static void assertGrid(int slotCount, int columns, int rows) {
        assertEquals(columns, MythicMinerLayout.columnsForSlotCount(slotCount));
        assertEquals(rows, MythicMinerLayout.rowsForSlotCount(slotCount));
    }
}
