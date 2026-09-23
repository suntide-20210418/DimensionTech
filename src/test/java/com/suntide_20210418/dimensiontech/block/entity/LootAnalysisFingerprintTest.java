package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LootAnalysisFingerprintTest {
    @Test
    void equalInputsProveCachedAnalysisCanBeReused() {
        assertEquals(
                new LootAnalysisFingerprint(1, java.util.List.of("0:stone"), 1, "config"),
                new LootAnalysisFingerprint(1, java.util.List.of("0:stone"), 1, "config"));
    }

    @Test
    void analysisInputsInvalidateTheFingerprint() {
        LootAnalysisFingerprint baseline =
                new LootAnalysisFingerprint(1, java.util.List.of("0:empty"), 1, "config");

        assertNotEquals(baseline, new LootAnalysisFingerprint(1, java.util.List.of("0:empty"), 2, "config"));
        assertNotEquals(baseline, new LootAnalysisFingerprint(1, java.util.List.of("0:empty"), 1, "changed"));
        assertNotEquals(baseline, new LootAnalysisFingerprint(1, java.util.List.of("0:stone"), 1, "config"));
        assertNotEquals(baseline, new LootAnalysisFingerprint(2, java.util.List.of("0:empty"), 1, "config"));
    }
}
