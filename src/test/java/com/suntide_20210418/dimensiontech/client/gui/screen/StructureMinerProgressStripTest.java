package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Boundary coverage for the only piece of page logic that can be checked without a running client.
 */
class StructureMinerProgressStripTest {
    private static final int WIDTH = 16;

    @Test
    void emptyProgressFillsNothing() {
        assertEquals(0, StructureMinerProgressStrip.pixels(0L, 2000L, WIDTH));
    }

    @Test
    void negativeProgressFillsNothing() {
        assertEquals(0, StructureMinerProgressStrip.pixels(-10L, 2000L, WIDTH));
    }

    @Test
    void zeroProcessingTimeNeverDividesByZero() {
        assertEquals(0, StructureMinerProgressStrip.pixels(500L, 0L, WIDTH));
        assertEquals(0, StructureMinerProgressStrip.pixels(500L, -1L, WIDTH));
    }

    @Test
    void zeroWidthFillsNothing() {
        assertEquals(0, StructureMinerProgressStrip.pixels(500L, 2000L, 0));
        assertEquals(0, StructureMinerProgressStrip.pixels(500L, 2000L, -4));
    }

    @Test
    void halfCycleFillsHalfTheStrip() {
        assertEquals(WIDTH / 2, StructureMinerProgressStrip.pixels(1000L, 2000L, WIDTH));
    }

    @Test
    void fullCycleFillsTheWholeStrip() {
        assertEquals(WIDTH, StructureMinerProgressStrip.pixels(2000L, 2000L, WIDTH));
    }

    @Test
    void progressPastOneCycleIsClampedToTheStrip() {
        assertEquals(WIDTH, StructureMinerProgressStrip.pixels(6000L, 2000L, WIDTH));
        assertEquals(WIDTH, StructureMinerProgressStrip.pixels(Long.MAX_VALUE, 2000L, WIDTH));
    }

    @Test
    void stripNeverExceedsItsWidth() {
        for (long progress = 0L; progress <= 4000L; progress += 137L) {
            int filled = StructureMinerProgressStrip.pixels(progress, 2000L, WIDTH);
            assertTrue(filled >= 0 && filled <= WIDTH, "progress=" + progress + " filled=" + filled);
        }
    }
}
