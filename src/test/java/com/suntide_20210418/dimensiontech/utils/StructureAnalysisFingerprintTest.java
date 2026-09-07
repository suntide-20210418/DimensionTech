package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.*;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StructureAnalysisFingerprintTest {
    @BeforeAll
    static void loadConfig() {
        ModConfigs.COMMON_SPEC.setConfig(CommentedConfig.inMemory());
    }

    @AfterEach
    void clearOverrides() { StructureScriptConfigService.clear(); }

    @Test
    void valueAndLootSettingsDoNotInvalidateDiscovery() {
        String discovery = ModConfigs.STRUCTURE_VALUE.discoveryFingerprint();
        String generation = ModConfigs.STRUCTURE_VALUE.generationFingerprint();
        StructureScriptConfigService.dimensionValue(ResourceLocation.parse("test:dimension"), 9);
        StructureScriptConfigService.rarity("rare", 90);
        StructureScriptConfigService.itemMultiplier("test:.*", 12);
        StructureScriptConfigService.blacklist("item", List.of("test:blocked"));
        StructureScriptConfigService.expectationMethod("SAMPLING");
        StructureScriptConfigService.samplingCount(123);
        StructureScriptConfigService.stepsPerTick(7);
        assertEquals(discovery, ModConfigs.STRUCTURE_VALUE.discoveryFingerprint());
        assertEquals(generation, ModConfigs.STRUCTURE_VALUE.generationFingerprint());
    }

    @Test
    void virtualSampleCountAffectsOnlyGenerationConfiguration() {
        String discovery = ModConfigs.STRUCTURE_VALUE.discoveryFingerprint();
        String generation = ModConfigs.STRUCTURE_VALUE.generationFingerprint();
        String expectation = ModConfigs.STRUCTURE_VALUE.expectationFingerprint();
        StructureScriptConfigService.virtualSamples(ModConfigs.STRUCTURE_VALUE.virtualStructureSamples() + 1);
        assertEquals(discovery, ModConfigs.STRUCTURE_VALUE.discoveryFingerprint());
        assertNotEquals(generation, ModConfigs.STRUCTURE_VALUE.generationFingerprint());
        assertEquals(expectation, ModConfigs.STRUCTURE_VALUE.expectationFingerprint());
    }

    @Test
    void structureFiltersInvalidateDiscoveryAndLootSamplingInvalidatesExpectation() {
        String discovery = ModConfigs.STRUCTURE_VALUE.discoveryFingerprint();
        String expectation = ModConfigs.STRUCTURE_VALUE.expectationFingerprint();
        StructureScriptConfigService.blacklist("structure", List.of("test:blocked"));
        assertNotEquals(discovery, ModConfigs.STRUCTURE_VALUE.discoveryFingerprint());
        assertEquals(expectation, ModConfigs.STRUCTURE_VALUE.expectationFingerprint());
        StructureScriptConfigService.samplingCount(ModConfigs.STRUCTURE_VALUE.samplingCount() + 1);
        assertNotEquals(expectation, ModConfigs.STRUCTURE_VALUE.expectationFingerprint());
    }
}
