package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StructureMinerTelemetrySnapshotTest {
    @Test
    void combinesUnsignedSixteenBitWordsIntoInt() {
        assertEquals(0x1234ABCD,
                StructureMinerTelemetrySnapshot.combineUnsignedWords(0xABCD, 0x1234));
        assertEquals(-1, StructureMinerTelemetrySnapshot.combineUnsignedWords(0xFFFF, 0xFFFF));
    }

    @Test
    void combinesUnsignedSixteenBitWordsIntoLong() {
        assertEquals(0x0123456789ABCDEFL,
                StructureMinerTelemetrySnapshot.combineUnsignedWords(
                        0xCDEF, 0x89AB, 0x4567, 0x0123));
        assertEquals(-1L,
                StructureMinerTelemetrySnapshot.combineUnsignedWords(
                        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF));
    }
}
