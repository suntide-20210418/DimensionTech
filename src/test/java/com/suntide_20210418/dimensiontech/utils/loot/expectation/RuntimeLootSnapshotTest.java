package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RuntimeLootSnapshotTest {
    @Test
    void sourceIsIsolatedAndReferenceIdentityIsStable() {
        var id = ResourceLocation.parse("test:root");
        var json = JsonParser.parseString("{\"pools\":[]}").getAsJsonObject();
        var tables = new LinkedHashMap<ResourceLocation, com.google.gson.JsonElement>();
        tables.put(id, json);
        RuntimeLootAstSource source = RuntimeLootAstSource.snapshot(tables, Map.of(), Map.of());
        String fingerprint = source.inputFingerprint();
        Object identity = source.table(id).orElseThrow().identity();
        tables.clear();
        json.addProperty("changed", true);
        source.table(id).orElseThrow().json().getAsJsonObject().addProperty("changed", true);
        assertSame(identity, source.table(id).orElseThrow().identity());
        assertEquals(fingerprint, source.inputFingerprint());
        assertFalse(source.table(id).orElseThrow().json().getAsJsonObject().has("changed"));
        assertNull(source.resources());
    }

    @Test
    void predicateAndFunctionContentsParticipateInFingerprint() {
        var id = ResourceLocation.parse("test:reference");
        var first = JsonParser.parseString("{\"chance\":0.5}");
        var second = JsonParser.parseString("{\"chance\":0.6}");
        var baseline = RuntimeLootAstSource.snapshot(Map.of(), Map.of(id, first), Map.of());
        var changed = RuntimeLootAstSource.snapshot(Map.of(), Map.of(id, second), Map.of());
        var modifier = RuntimeLootAstSource.snapshot(Map.of(), Map.of(), Map.of(id, first));
        assertNotEquals(baseline.inputFingerprint(), changed.inputFingerprint());
        assertNotEquals(baseline.inputFingerprint(), modifier.inputFingerprint());
    }
}
