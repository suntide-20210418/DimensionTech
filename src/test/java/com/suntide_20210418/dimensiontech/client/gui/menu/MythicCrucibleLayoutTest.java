package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class MythicCrucibleLayoutTest {
    @Test
    void itemSlotsAreSixteenPixelsWithTwoPixelGaps() {
        assertEquals(16, MythicCrucibleLayout.ITEM_SLOT_SIZE);
        assertEquals(2, MythicCrucibleLayout.ITEM_SLOT_GAP);
        assertEquals(18, MythicCrucibleLayout.ITEM_SLOT_STRIDE);
        assertEquals(16, MythicCrucibleLayout.FRAGMENT_SLOT.width());
        assertEquals(16, MythicCrucibleLayout.FRAGMENT_SLOT.height());
        assertEquals(
                2,
                MythicCrucibleLayout.OPERATION_SLOT.y()
                        - MythicCrucibleLayout.FRAGMENT_SLOT.bottom());

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                var slot = MythicCrucibleLayout.playerSlot(column, row);
                assertEquals(16, slot.width());
                assertEquals(16, slot.height());
                if (column > 0) {
                    assertEquals(
                            2,
                            slot.x()
                                    - MythicCrucibleLayout.playerSlot(column - 1, row).right());
                }
                if (row > 0) {
                    assertEquals(
                            2,
                            slot.y()
                                    - MythicCrucibleLayout.playerSlot(column, row - 1).bottom());
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            var slot = MythicCrucibleLayout.hotbarSlot(column);
            assertEquals(16, slot.width());
            assertEquals(16, slot.height());
            if (column > 0) {
                assertEquals(
                        2,
                        slot.x() - MythicCrucibleLayout.hotbarSlot(column - 1).right());
            }
        }
    }

    @Test
    void exposesRequestedDimensionsAndRegions() {
        assertEquals(175, MythicCrucibleLayout.WIDTH);
        assertEquals(165, MythicCrucibleLayout.HEIGHT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(8, 17, 16, 50), MythicCrucibleLayout.INPUT_TANK);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(30, 17, 16, 16), MythicCrucibleLayout.FRAGMENT_SLOT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(30, 35, 16, 16), MythicCrucibleLayout.OPERATION_SLOT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(34, 54, 12, 11), MythicCrucibleLayout.PROGRESS_BAR);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(34, 54, 12, 11), MythicCrucibleLayout.RECIPE_DISPLAY);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(52, 18, 93, 47), MythicCrucibleLayout.STATUS_DISPLAY);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(152, 17, 16, 50), MythicCrucibleLayout.OUTPUT_TANK);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(7, 83, 162, 54), MythicCrucibleLayout.PLAYER_INVENTORY);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(7, 141, 162, 18), MythicCrucibleLayout.HOTBAR);
    }

    @Test
    void slotGridStaysInsideInventoryRegions() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                assertFalse(MythicCrucibleLayout.playerSlotX(column) < 7);
                assertFalse(MythicCrucibleLayout.playerSlotY(row) < 83);
            }
        }
        assertEquals(8, MythicCrucibleLayout.playerSlotX(0));
        assertEquals(84, MythicCrucibleLayout.playerSlotY(0));
        assertEquals(152, MythicCrucibleLayout.playerSlotX(8));
        assertEquals(120, MythicCrucibleLayout.playerSlotY(2));
        assertEquals(8, MythicCrucibleLayout.hotbarSlotX(0));
        assertEquals(152, MythicCrucibleLayout.hotbarSlotX(8));
        assertEquals(142, MythicCrucibleLayout.hotbarSlotY());
    }

    @Test
    void regionsUseHalfOpenBounds() {
        assertEquals(24, MythicCrucibleLayout.INPUT_TANK.right());
        assertEquals(67, MythicCrucibleLayout.INPUT_TANK.bottom());
        assertFalse(MythicCrucibleLayout.INPUT_TANK.contains(24, 17));
        assertEquals(MythicCrucibleLayout.PROGRESS_BAR, MythicCrucibleLayout.RECIPE_DISPLAY);
        assertFalse(MythicCrucibleLayout.OUTPUT_TANK.intersects(MythicCrucibleLayout.RECIPE_DISPLAY));
        assertFalse(MythicCrucibleLayout.PLAYER_INVENTORY.intersects(MythicCrucibleLayout.HOTBAR));
    }
}
