package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.junit.jupiter.api.Test;

class StatefulNumberProvider1201Test {
    private static final LootAnalysisContext CONTEXT = new LootAnalysisContext(
            null, null, null, 0.0F, Map.of("points", 7), null, null, null);

    @Test
    void uniformIntegerMatchesRuntimeAndContinuation() {
        XoroshiroState1201 state = new XoroshiroState1201(31L, 47L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(31L, 47L);
        var provider = JsonParser.parseString(
                "{\"type\":\"minecraft:uniform\",\"min\":-2.1,\"max\":4.9}");

        var result = StatefulNumberProvider1201.getInt(provider, CONTEXT, state);
        assertEquals(net.minecraft.util.Mth.nextInt(runtime, -3, 4), result.value());
        assertEquals(runtime.nextLong(), result.randomState().nextLong().value());
    }

    @Test
    void binomialEvaluatesNThenPThenOneFloatPerTrial() {
        XoroshiroState1201 state = new XoroshiroState1201(101L, 303L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(101L, 303L);
        var provider = JsonParser.parseString(
                "{\"type\":\"minecraft:binomial\",\"n\":3,\"p\":0.25}");

        var result = StatefulNumberProvider1201.getInt(provider, CONTEXT, state);
        int expected = 0;
        for (int index = 0; index < 3; index++) if (runtime.nextFloat() < 0.25F) expected++;
        assertEquals(expected, result.value());
        assertEquals(runtime.nextLong(), result.randomState().nextLong().value());
    }

}
