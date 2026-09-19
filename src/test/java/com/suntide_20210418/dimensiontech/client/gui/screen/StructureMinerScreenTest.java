package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StructureMinerScreenTest {
    @Test
    void energyFillUsesLongArithmeticForLargeCapacities() {
        assertEquals(82, StructureMinerScreen.fillPixels(1_000_000, 2_000_000, 164));
        assertEquals(164, StructureMinerScreen.fillPixels(Integer.MAX_VALUE, 2_000_000, 164));
        assertEquals(0, StructureMinerScreen.fillPixels(0, 2_000_000, 164));
    }
}
