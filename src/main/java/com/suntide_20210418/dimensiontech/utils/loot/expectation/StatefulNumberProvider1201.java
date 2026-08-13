package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Ordered NumberProvider evaluation over an exact Minecraft 1.20.1 Xoroshiro state. */
public final class StatefulNumberProvider1201 {
    private StatefulNumberProvider1201() {}

    public static IntResult getInt(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        if (element == null) return new IntResult(1, state);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new IntResult(net.minecraft.util.Mth.floor(element.getAsFloat()), state);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) {
            return new IntResult(net.minecraft.util.Mth.floor(object.get("value").getAsFloat()), state);
        }
        if (type.endsWith(":uniform")) {
            IntResult min = getInt(object.get("min"), context, state);
            if (min == null) return null;
            IntResult max = getInt(object.get("max"), context, min.randomState());
            if (max == null) return null;
            if (min.value() >= max.value()) return new IntResult(min.value(), max.randomState());
            var draw = max.randomState().nextIntInclusive(min.value(), max.value());
            return new IntResult(draw.value(), draw.state());
        }
        if (type.endsWith(":binomial")) {
            IntResult trials = getInt(object.get("n"), context, state);
            if (trials == null) return null;
            FloatResult probability = getFloat(object.get("p"), context, trials.randomState());
            if (probability == null) return null;
            int successes = 0;
            XoroshiroState1201 current = probability.randomState();
            for (int index = 0; index < trials.value(); index++) {
                var draw = current.nextFloat();
                current = draw.state();
                if (draw.value() < probability.value()) successes++;
            }
            return new IntResult(successes, current);
        }
        return null;
    }

    public static FloatResult getFloat(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        if (element == null) return new FloatResult(0.0F, state);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new FloatResult(element.getAsFloat(), state);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) {
            return new FloatResult(object.get("value").getAsFloat(), state);
        }
        if (type.endsWith(":uniform")) {
            FloatResult min = getFloat(object.get("min"), context, state);
            if (min == null) return null;
            FloatResult max = getFloat(object.get("max"), context, min.randomState());
            if (max == null) return null;
            if (min.value() >= max.value()) return new FloatResult(min.value(), max.randomState());
            var draw = max.randomState().nextFloat();
            float value = draw.value() * (max.value() - min.value()) + min.value();
            return new FloatResult(value, draw.state());
        }
        IntResult integer = getInt(element, context, state);
        return integer == null ? null : new FloatResult(integer.value(), integer.randomState());
    }

    public record IntResult(int value, XoroshiroState1201 randomState) {}

    public record FloatResult(float value, XoroshiroState1201 randomState) {}
}
