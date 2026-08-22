package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MythicMinerSlotProgressTest {
    @Test
    void markerSlotsAdvanceAndCompleteIndependently() {
        MythicMinerSlotProgress progress = new MythicMinerSlotProgress(2);

        assertFalse(progress.advance(0, 2));
        assertFalse(progress.advance(1, 3));
        assertTrue(progress.advance(0, 2));
        assertFalse(progress.advance(1, 3));

        assertEquals(0, progress.get(0));
        assertEquals(2, progress.get(1));
    }

    @Test
    void resettingOneMarkerDoesNotResetAnother() {
        MythicMinerSlotProgress progress = new MythicMinerSlotProgress(2);
        progress.advance(0, 4);
        progress.advance(1, 4);

        progress.reset(0);

        assertEquals(0, progress.get(0));
        assertEquals(1, progress.get(1));
    }
}
