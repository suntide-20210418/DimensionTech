package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.suntide_20210418.dimensiontech.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulNumberProvider1201;
import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import org.junit.jupiter.api.Test;

import java.util.Map;

class StatefulNumberProvider1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(
                    null, null, null, 0.0F, Map.of("points", 7), null, null, null, Map.of());

    @Test
    void vanillaPathInAnotherNamespaceRemainsUnknown() {
        var result =
                StatefulNumberProvider1201.getInt(
                        JsonParser.parseString(
                                "{\"type\":\"example:uniform\",\"min\":1,\"max\":2}"),
                        CONTEXT,
                        new XoroshiroState1201(8L, 13L));

        assertNull(result);
    }

    @Test
    void uniformIntegerMatchesRuntimeAndContinuation() {
        XoroshiroState1201 state = new XoroshiroState1201(31L, 47L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(31L, 47L);
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:uniform\",\"min\":-2.1,\"max\":4.9}");

        var result = StatefulNumberProvider1201.getInt(provider, CONTEXT, state);
        assertEquals(net.minecraft.util.Mth.nextInt(runtime, -2, 5), result.value());
        assertEquals(runtime.nextLong(), result.randomState().nextLong().value());
    }

    @Test
    void binomialEvaluatesNThenPThenOneFloatPerTrial() {
        XoroshiroState1201 state = new XoroshiroState1201(101L, 303L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(101L, 303L);
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:binomial\",\"n\":3,\"p\":0.25}");

        var result = StatefulNumberProvider1201.getInt(provider, CONTEXT, state);
        int expected = 0;
        for (int index = 0; index < 3; index++) if (runtime.nextFloat() < 0.25F) expected++;
        assertEquals(expected, result.value());
        assertEquals(runtime.nextLong(), result.randomState().nextLong().value());
    }

    @Test
    void missingOrUntypedProviderIsNotGuessed() {
        XoroshiroState1201 state = new XoroshiroState1201(5L, 8L);

        assertNull(StatefulNumberProvider1201.getInt(null, CONTEXT, state));
        assertNull(StatefulNumberProvider1201.getFloat(null, CONTEXT, state));
        assertNull(
                StatefulNumberProvider1201.getInt(
                        JsonParser.parseString("{\"min\":1,\"max\":2}"), CONTEXT, state));
    }

    @Test
    void malformedNumericPrimitiveDoesNotEscapeStatefulBoundary() {
        XoroshiroState1201 state = new XoroshiroState1201(13L, 21L);
        JsonPrimitive malformed = new JsonPrimitive(new ThrowingNumber());

        assertNull(StatefulNumberProvider1201.getInt(malformed, CONTEXT, state));
        assertNull(StatefulNumberProvider1201.getFloat(malformed, CONTEXT, state));
    }

    private static final class ThrowingNumber extends Number {
        @Override
        public int intValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public long longValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public float floatValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public double doubleValue() {
            throw new NumberFormatException("malformed number");
        }
    }
}
