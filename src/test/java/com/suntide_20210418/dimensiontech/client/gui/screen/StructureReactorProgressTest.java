package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StructureReactorProgressTest {
    @Test
    void progressPixelsClampsAndUsesTopDownHeight() {
        assertEquals(0, StructureReactorScreen.progressPixels(0, 100, 11));
        assertEquals(5, StructureReactorScreen.progressPixels(50, 100, 11));
        assertEquals(11, StructureReactorScreen.progressPixels(100, 100, 11));
        assertEquals(0, StructureReactorScreen.progressPixels(-1, 100, 11));
        assertEquals(11, StructureReactorScreen.progressPixels(101, 100, 11));
        assertEquals(0, StructureReactorScreen.progressPixels(1, 0, 11));
    }
}
