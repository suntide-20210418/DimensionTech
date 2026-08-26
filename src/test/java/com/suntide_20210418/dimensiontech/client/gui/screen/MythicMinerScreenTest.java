package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MythicMinerScreenTest {
    @Test
    void energyFillUsesLongArithmeticForLargeCapacities() {
        assertEquals(82, MythicMinerScreen.fillPixels(1_000_000, 2_000_000, 164));
        assertEquals(164, MythicMinerScreen.fillPixels(Integer.MAX_VALUE, 2_000_000, 164));
        assertEquals(0, MythicMinerScreen.fillPixels(0, 2_000_000, 164));
    }
}
