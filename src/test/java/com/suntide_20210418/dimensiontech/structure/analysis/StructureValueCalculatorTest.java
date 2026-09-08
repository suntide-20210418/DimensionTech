package com.suntide_20210418.dimensiontech.structure.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.VanillaStructureLootResolver;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

class StructureValueCalculatorTest {
    @Test
    void discoveryCopiesAllNestedCollections() {
        ResourceLocation id = ResourceLocation.parse("test:table");
        var roots = new java.util.ArrayList<>(List.of(id));
        var items = new java.util.ArrayList<>(List.of(id));
        var resolved = new java.util.ArrayList<>(List.of(id));
        var occurrences = new java.util.HashMap<ResourceLocation, Integer>();
        occurrences.put(id, 2);
        StructureLoot loot = new StructureLoot(id, roots, items, resolved, occurrences);
        var structures = new java.util.ArrayList<>(List.of(loot));
        DiscoveryResult discovery = new DiscoveryResult(AnalysisStatus.EXACT, structures, List.of());
        roots.clear();
        items.clear();
        resolved.clear();
        occurrences.clear();
        structures.clear();
        assertEquals(List.of(loot), discovery.structures());
        assertEquals(List.of(id), loot.lootTables());
        assertEquals(List.of(id), loot.items());
        assertEquals(List.of(id), loot.resolvedTables());
        assertEquals(2, loot.occurrences().get(id));
        assertThrows(UnsupportedOperationException.class, () -> loot.lootTables().clear());
        assertThrows(UnsupportedOperationException.class, () -> loot.items().clear());
        assertThrows(UnsupportedOperationException.class, () -> loot.resolvedTables().clear());
        assertThrows(UnsupportedOperationException.class, () -> loot.occurrences().clear());
        assertThrows(UnsupportedOperationException.class, () -> discovery.structures().clear());
    }

    @Test
    void rootTablesRetainEveryContainerOccurrence() {
        ResourceLocation shared = new ResourceLocation("test", "shared");
        ResourceLocation firstOnly = new ResourceLocation("test", "first_only");
        DiscoveryResult discovery =
                new DiscoveryResult(
                        AnalysisStatus.EXACT,
                        List.of(
                                structure("first", List.of(shared, firstOnly, shared, firstOnly)),
                                structure("second", List.of(shared, shared))),
                        List.of());

        assertEquals(
                List.of(firstOnly, firstOnly, shared, shared, shared, shared),
                StructureValueCalculator.rootTablesForValue(discovery));

        DiscoveryResult unsupported =
                new DiscoveryResult(AnalysisStatus.UNSUPPORTED, discovery.structures(), List.of());
        assertEquals(List.of(), StructureValueCalculator.rootTablesForValue(unsupported));
    }

    @Test
    void exactStructureValueRejectsNonFiniteTerminalValue() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StructureValue(
                                AnalysisStatus.EXACT,
                                1.0D,
                                Double.POSITIVE_INFINITY,
                                0.0F,
                                new StackMeasure(),
                                List.of()));
    }

    @Test
    void virtualSampleOriginsAreDeterministicAndStructureSpecific() {
        ResourceLocation dimension = new ResourceLocation("example", "void");
        ResourceLocation first = new ResourceLocation("example", "first");
        ResourceLocation second = new ResourceLocation("example", "second");
        assertEquals(
                StructureAnalysisService.sampleOrigin(42L, dimension, first, 3),
                StructureAnalysisService.sampleOrigin(42L, dimension, first, 3));
        assertFalse(
                StructureAnalysisService.sampleOrigin(42L, dimension, first, 3)
                        .equals(StructureAnalysisService.sampleOrigin(42L, dimension, second, 3)));
    }

    @Test
    void virtualOccurrencesAreAveragedAcrossSamples() {
        ResourceLocation table = ResourceLocation.fromNamespaceAndPath("test", "dynamic");
        DiscoveryResult discovery =
                new DiscoveryResult(
                        AnalysisStatus.APPROXIMATE,
                        List.of(
                                new StructureLoot(
                                        ResourceLocation.fromNamespaceAndPath("test", "structure"),
                                        List.of(table),
                                        List.of(),
                                        List.of(),
                                        java.util.Map.of(table, 12))),
                        List.of(),
                        ExactProbability.of(1, 8));
        assertEquals(
                ExactProbability.of(3, 2),
                StructureValueCalculator.rootTableWeightsForValue(discovery).get(table));
    }

    @Test
    void dynamicContainerLootTablesAreReadFromContainerNbt() {
        net.minecraft.nbt.CompoundTag container = new net.minecraft.nbt.CompoundTag();
        container.putString("LootTable", "example_mod:chests/generated_cache");

        assertEquals(
                java.util.Optional.of(
                        ResourceLocation.fromNamespaceAndPath(
                                "example_mod", "chests/generated_cache")),
                StructureLootAnalyzer.containerLootTable(container));
        assertTrue(StructureLootAnalyzer.containerLootTable(new net.minecraft.nbt.CompoundTag()).isEmpty());
    }

    @Test
    void chestMinecartLootTableUsesTheSameRuntimeNbtField() {
        net.minecraft.nbt.CompoundTag minecartEntity = new net.minecraft.nbt.CompoundTag();
        minecartEntity.putString("id", "minecraft:chest_minecart");
        minecartEntity.putString("LootTable", "minecraft:chests/abandoned_mineshaft");

        assertEquals(
                java.util.Optional.of(
                        ResourceLocation.fromNamespaceAndPath(
                                "minecraft", "chests/abandoned_mineshaft")),
                StructureLootAnalyzer.containerLootTable(minecartEntity));
    }

    @Test
    void vanillaFixedResolverUsesOnlyKnownVanillaStructureLocations() {
        assertEquals(
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/end_city_treasure")),
                VanillaStructureLootResolver
                        .resolve(ResourceLocation.fromNamespaceAndPath("minecraft", "end_city"))
                        .orElseThrow());
        assertEquals(
                List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft")),
                VanillaStructureLootResolver
                        .resolve(ResourceLocation.fromNamespaceAndPath("minecraft", "mineshaft"))
                        .orElseThrow());
        assertTrue(
                VanillaStructureLootResolver
                        .resolve(ResourceLocation.fromNamespaceAndPath("example_mod", "end_city"))
                        .isEmpty());
        assertTrue(
                VanillaStructureLootResolver
                        .resolve(ResourceLocation.fromNamespaceAndPath("minecraft", "unknown"))
                        .isEmpty());
    }

    @Test
    void virtualAnalysisIsNeverAnOriginalStructureFallback() {
        assertFalse(
                StructureAnalysisService.mayUseVirtualAnalysis(
                        ResourceLocation.fromNamespaceAndPath("minecraft", "unknown")));
        assertTrue(
                StructureAnalysisService.mayUseVirtualAnalysis(
                        ResourceLocation.fromNamespaceAndPath("example_mod", "unknown")));
    }

    private static StructureLoot structure(String id, List<ResourceLocation> roots) {
        return new StructureLoot(new ResourceLocation("test", id), roots, List.of(), List.of());
    }
}
