package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Ordered core LootItemCondition execution over a Minecraft 1.20.1 Xoroshiro state. */
public final class StatefulCondition1201 {
    private StatefulCondition1201() {}

    public static Result testAll(
            JsonElement conditions, LootAnalysisContext context, XoroshiroState1201 state) {
        if (conditions == null) return new Result(true, state);
        if (!conditions.isJsonArray()) return test(conditions, context, state);
        XoroshiroState1201 current = state;
        for (JsonElement condition : conditions.getAsJsonArray()) {
            Result result = test(condition, context, current);
            if (result == null) return null;
            current = result.randomState();
            if (!result.value()) return new Result(false, current);
        }
        return new Result(true, current);
    }

    public static Result test(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject condition = element.getAsJsonObject();
        String type = condition.has("condition")
                ? condition.get("condition").getAsString()
                : "";
        if (type.endsWith(":always_true")) return new Result(true, state);
        if (type.endsWith(":always_false")) return new Result(false, state);
        if (type.endsWith(":random_chance")) {
            if (!condition.has("chance")) return null;
            float threshold = condition.get("chance").getAsFloat();
            if (!Float.isFinite(threshold)) return null;
            var draw = state.nextFloat();
            return new Result(draw.value() < threshold, draw.state());
        }
        if (type.endsWith(":random_chance_with_luck")) {
            if (!condition.has("chance")) return null;
            float chance = condition.get("chance").getAsFloat();
            float multiplier = condition.has("luck_multiplier")
                    ? condition.get("luck_multiplier").getAsFloat()
                    : 1.0F;
            float threshold = chance + context.luck() * multiplier;
            if (!Float.isFinite(threshold)) return null;
            var draw = state.nextFloat();
            return new Result(draw.value() < threshold, draw.state());
        }
        if (type.endsWith(":inverted")) {
            Result nested = test(condition.get("term"), context, state);
            return nested == null ? null : new Result(!nested.value(), nested.randomState());
        }
        if (type.endsWith(":all_of")) {
            JsonArray terms = condition.has("terms") && condition.get("terms").isJsonArray()
                    ? condition.getAsJsonArray("terms")
                    : null;
            return terms == null ? null : testAll(terms, context, state);
        }
        if (type.endsWith(":any_of")) {
            JsonArray terms = condition.has("terms") && condition.get("terms").isJsonArray()
                    ? condition.getAsJsonArray("terms")
                    : null;
            if (terms == null) return null;
            XoroshiroState1201 current = state;
            for (JsonElement term : terms) {
                Result result = test(term, context, current);
                if (result == null) return null;
                current = result.randomState();
                if (result.value()) return new Result(true, current);
            }
            return new Result(false, current);
        }
        return null;
    }

    public record Result(boolean value, XoroshiroState1201 randomState) {}
}
