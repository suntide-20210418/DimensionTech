package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MythicCrucibleProgressTest {
    @Test
    void progressPixelsClampsAndUsesTopDownHeight() {
        assertEquals(0, MythicCrucibleScreen.progressPixels(0, 100, 11));
        assertEquals(5, MythicCrucibleScreen.progressPixels(50, 100, 11));
        assertEquals(11, MythicCrucibleScreen.progressPixels(100, 100, 11));
        assertEquals(0, MythicCrucibleScreen.progressPixels(-1, 100, 11));
        assertEquals(11, MythicCrucibleScreen.progressPixels(101, 100, 11));
        assertEquals(0, MythicCrucibleScreen.progressPixels(1, 0, 11));
    }
}
