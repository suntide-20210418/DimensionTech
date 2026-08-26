package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MythicMinerTelemetrySnapshotTest {
    @Test
    void combinesUnsignedSixteenBitWordsIntoInt() {
        assertEquals(0x1234ABCD,
                MythicMinerTelemetrySnapshot.combineUnsignedWords(0xABCD, 0x1234));
        assertEquals(-1, MythicMinerTelemetrySnapshot.combineUnsignedWords(0xFFFF, 0xFFFF));
    }

    @Test
    void combinesUnsignedSixteenBitWordsIntoLong() {
        assertEquals(0x0123456789ABCDEFL,
                MythicMinerTelemetrySnapshot.combineUnsignedWords(
                        0xCDEF, 0x89AB, 0x4567, 0x0123));
        assertEquals(-1L,
                MythicMinerTelemetrySnapshot.combineUnsignedWords(
                        0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF));
    }
}
