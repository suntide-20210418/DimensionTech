package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StructureReactorLayoutTest {
    @Test
    void itemSlotsAreSixteenPixelsWithGaps() {
        assertEquals(16, StructureReactorLayout.ITEM_SLOT_SIZE);
        assertEquals(2, StructureReactorLayout.ITEM_SLOT_GAP);
        assertEquals(18, StructureReactorLayout.ITEM_SLOT_STRIDE);
        assertEquals(16, StructureReactorLayout.FRAGMENT_SLOT.width());
        assertEquals(16, StructureReactorLayout.FRAGMENT_SLOT.height());
        assertEquals(
                10,
                StructureReactorLayout.OPERATION_SLOT.y()
                        - StructureReactorLayout.FRAGMENT_SLOT.bottom());

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                var slot = StructureReactorLayout.playerSlot(column, row);
                assertEquals(16, slot.width());
                assertEquals(16, slot.height());
                if (column > 0) {
                    assertEquals(
                            2,
                            slot.x()
                                    - StructureReactorLayout.playerSlot(column - 1, row).right());
                }
                if (row > 0) {
                    assertEquals(
                            2,
                            slot.y()
                                    - StructureReactorLayout.playerSlot(column, row - 1).bottom());
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            var slot = StructureReactorLayout.hotbarSlot(column);
            assertEquals(16, slot.width());
            assertEquals(16, slot.height());
            if (column > 0) {
                assertEquals(
                        2,
                        slot.x() - StructureReactorLayout.hotbarSlot(column - 1).right());
            }
        }
    }

    @Test
    void exposesRequestedDimensionsAndRegions() {
        assertEquals(292, StructureReactorLayout.WIDTH);
        assertEquals(165, StructureReactorLayout.HEIGHT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(8, 12, 16, 50), StructureReactorLayout.INPUT_TANK);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(32, 16, 16, 16), StructureReactorLayout.FRAGMENT_SLOT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(32, 42, 16, 16), StructureReactorLayout.OPERATION_SLOT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(56, 18, 88, 5), StructureReactorLayout.PROGRESS_BAR);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(56, 18, 88, 5), StructureReactorLayout.RECIPE_DISPLAY);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(56, 27, 88, 29), StructureReactorLayout.STATUS_REWARD_AREA);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(182, 42, 100, 112), StructureReactorLayout.VIEWPORT);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(152, 12, 16, 50), StructureReactorLayout.OUTPUT_TANK);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(7, 83, 162, 54), StructureReactorLayout.PLAYER_INVENTORY);
        assertEquals(new com.suntide_20210418.dimensiontech.client.gui.screen.GuiRect(7, 141, 162, 18), StructureReactorLayout.HOTBAR);
    }

    @Test
    void slotGridStaysInsideInventoryRegions() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                assertFalse(StructureReactorLayout.playerSlotX(column) < 7);
                assertFalse(StructureReactorLayout.playerSlotY(row) < 83);
            }
        }
        assertEquals(8, StructureReactorLayout.playerSlotX(0));
        assertEquals(84, StructureReactorLayout.playerSlotY(0));
        assertEquals(152, StructureReactorLayout.playerSlotX(8));
        assertEquals(120, StructureReactorLayout.playerSlotY(2));
        assertEquals(8, StructureReactorLayout.hotbarSlotX(0));
        assertEquals(152, StructureReactorLayout.hotbarSlotX(8));
        assertEquals(142, StructureReactorLayout.hotbarSlotY());
    }

    @Test
    void regionsUseHalfOpenBounds() {
        assertEquals(24, StructureReactorLayout.INPUT_TANK.right());
        assertEquals(62, StructureReactorLayout.INPUT_TANK.bottom());
        assertFalse(StructureReactorLayout.INPUT_TANK.contains(24, 17));
        assertEquals(StructureReactorLayout.PROGRESS_BAR, StructureReactorLayout.RECIPE_DISPLAY);
        assertFalse(StructureReactorLayout.OUTPUT_TANK.intersects(StructureReactorLayout.RECIPE_DISPLAY));
        assertFalse(StructureReactorLayout.PLAYER_INVENTORY.intersects(StructureReactorLayout.HOTBAR));
        assertFalse(StructureReactorLayout.STATUS_REWARD_AREA.intersects(StructureReactorLayout.PROGRESS_BAR));
        assertTrue(StructureReactorLayout.VIEWPORT.x() >= 0);
        assertTrue(StructureReactorLayout.VIEWPORT.y() >= 0);
        assertTrue(StructureReactorLayout.VIEWPORT.x() + StructureReactorLayout.VIEWPORT.width() <= StructureReactorLayout.WIDTH);
        assertTrue(StructureReactorLayout.VIEWPORT.y() + StructureReactorLayout.VIEWPORT.height() <= StructureReactorLayout.HEIGHT);
    }
}
