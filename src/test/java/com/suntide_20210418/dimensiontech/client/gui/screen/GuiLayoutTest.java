package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuiLayoutTest {
    @Test
    void rectanglesUseHalfOpenBoundsForPaintingAndInput() {
        GuiRect rect = new GuiRect(8, 12, 16, 18);
        assertTrue(rect.contains(8, 12));
        assertTrue(rect.contains(23, 29));
        assertFalse(rect.contains(24, 29));
        assertFalse(rect.contains(23, 30));
        assertEquals(24, rect.right());
        assertEquals(30, rect.bottom());
    }

    @Test
    void layoutCopiesSlotListAndProvidesStableSharedChromeRects() {
        GuiLayout layout = GuiLayout.empty(176, 282);
        assertEquals(176, layout.width());
        assertEquals(282, layout.height());
        assertEquals(14, layout.titleBand().height());
        assertEquals(13, layout.tabs().height());
        assertTrue(layout.body().y() >= layout.tabs().bottom());
        assertTrue(layout.playerInventory().bottom() <= layout.height());
        assertEquals(List.of(), layout.machineSlots());
    }

    @Test
    void invalidGeometryIsRejectedEarly() {
        assertThrows(IllegalArgumentException.class, () -> new GuiLayout(0, 1, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new GuiRect(0, 0, -1, 4));
    }
}
