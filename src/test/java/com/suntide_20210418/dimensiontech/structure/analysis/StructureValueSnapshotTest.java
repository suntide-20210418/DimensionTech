package com.suntide_20210418.dimensiontech.structure.analysis;

import static org.junit.jupiter.api.Assertions.*;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueSnapshot.Config;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueSnapshot.Expectation;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueSnapshot.TerminalItem;
import com.suntide_20210418.dimensiontech.utils.AnalysisTaskCache;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructureValueSnapshotTest {
    private static final TerminalItem DIAMOND = new TerminalItem("minecraft:diamond", 2, "RARE");

    @Test
    void valuesExactOccurrencesAndFiltersBeforeAggregating() {
        var input = new Expectation(Map.of(DIAMOND, ExactProbability.of(1, 3),
                new TerminalItem("minecraft:dirt", 64, "COMMON"), ExactProbability.ONE));
        var result = StructureValueSnapshot.calculate(input,
                new Config(3, Map.of("minecraft:diamond", Map.of("RARE", 50.0))));
        assertTrue(result.supported());
        assertEquals(500.0, result.structureValue());
        assertEquals(Map.of("minecraft:diamond", ExactProbability.of(2, 3)), result.itemCounts());
        assertEquals(Map.of(DIAMOND, ExactProbability.of(1, 3)), result.expectation().occurrences());
    }

    @Test
    void snapshotsCopyNestedDataAndFingerprintsDescribeContent() {
        var occurrences = new LinkedHashMap<TerminalItem, ExactProbability>();
        occurrences.put(DIAMOND, ExactProbability.ONE);
        var rarities = new LinkedHashMap<String, Double>();
        rarities.put("RARE", 50.0);
        var items = new LinkedHashMap<String, Map<String, Double>>();
        items.put("minecraft:diamond", rarities);
        var input = new Expectation(occurrences);
        var config = new Config(3, items);
        String inputFingerprint = input.inputFingerprint();
        String configFingerprint = config.configFingerprint();
        occurrences.clear();
        rarities.clear();
        items.clear();
        assertEquals(inputFingerprint, input.inputFingerprint());
        assertEquals(configFingerprint, config.configFingerprint());
        assertThrows(UnsupportedOperationException.class, () -> input.occurrences().clear());
        assertThrows(UnsupportedOperationException.class, () -> config.itemValues().get("minecraft:diamond").clear());
        assertEquals(inputFingerprint, new Expectation(Map.of(DIAMOND, ExactProbability.ONE)).inputFingerprint());
        assertNotEquals(configFingerprint, new Config(4, config.itemValues()).configFingerprint());
    }

    @Test
    void dimensionChangesCanReuseTheSameTerminalExpectation() throws Exception {
        var input = new Expectation(Map.of(DIAMOND, ExactProbability.ONE));
        var initial = new Config(1, Map.of("minecraft:diamond", Map.of("RARE", 50.0)));
        var changed = new Config(4, initial.itemValues());
        try (var cache = new AnalysisTaskCache(1, 32)) {
            var first = cache.submit(new AnalysisTaskCache.Key("value", input.inputFingerprint(),
                    initial.configFingerprint(), 1), () -> StructureValueSnapshot.calculate(input, initial));
            var second = cache.submit(new AnalysisTaskCache.Key("value", input.inputFingerprint(),
                    changed.configFingerprint(), 1), () -> StructureValueSnapshot.calculate(input, changed));
            assertEquals(first.get().structureValue() * 2, second.get().structureValue());
            assertEquals(first.get().itemCounts(), second.get().itemCounts());
        }
    }

    @Test
    void failureDoesNotMasqueradeAsAnExactZero() {
        var input = new Expectation(Map.of(DIAMOND, ExactProbability.ONE));
        assertFalse(StructureValueSnapshot.calculate(input,
                new Config(Double.POSITIVE_INFINITY, Map.of())).supported());
        assertFalse(StructureValueSnapshot.calculate(input,
                new Config(1, Map.of("minecraft:diamond", Map.of("RARE", Double.MAX_VALUE)))).supported());
        var zero = StructureValueSnapshot.calculate(input, new Config(1, Map.of()));
        assertTrue(zero.supported());
        assertEquals(0, zero.structureValue());
    }
}
