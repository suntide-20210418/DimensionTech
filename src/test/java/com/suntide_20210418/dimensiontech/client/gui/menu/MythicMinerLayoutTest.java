package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers what the menu and the screen still share.
 *
 * <p>The marker-lane geometry moved to {@code MythicMinerInfoLayout} when the lanes became a single
 * drawn row, so the grid assertions that used to live here now sit in
 * {@code MythicMinerInfoLayoutTest}.
 */
class MythicMinerLayoutTest {
    @Test
    void playerInventoryStaysBelowTheFixedMarkerSection() {
        assertEquals(258, MythicMinerLayout.playerInventoryY());
        assertEquals(MythicMinerLayout.BASE_PLAYER_INVENTORY_Y, MythicMinerLayout.playerInventoryY());
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
}
