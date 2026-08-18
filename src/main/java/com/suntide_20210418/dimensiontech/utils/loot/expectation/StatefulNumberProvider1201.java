package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Ordered NumberProvider evaluation over an exact Minecraft 1.20.1 Xoroshiro state. */
public final class StatefulNumberProvider1201 {
    private StatefulNumberProvider1201() {}

    public static IntResult getInt(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        try {
            return getIntUnchecked(element, context, state);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static IntResult getIntUnchecked(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        if (element == null) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            Float value = numberValue(element);
            return value == null || !Float.isFinite(value)
                    ? null
                    : new IntResult(Math.round(value), state);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = type(object);
        if (type.equals("minecraft:constant")) {
            Float value = numberValue(object.get("value"));
            return value == null || !Float.isFinite(value)
                    ? null
                    : new IntResult(Math.round(value), state);
        }
        if (type.equals("minecraft:uniform")) {
            IntResult min = getInt(object.get("min"), context, state);
            if (min == null) return null;
            IntResult max = getInt(object.get("max"), context, min.randomState());
            if (max == null) return null;
            if (min.value() >= max.value()) return new IntResult(min.value(), max.randomState());
            try {
                var draw = max.randomState().nextIntInclusive(min.value(), max.value());
                return new IntResult(draw.value(), draw.state());
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        if (type.equals("minecraft:binomial")) {
            IntResult trials = getInt(object.get("n"), context, state);
            if (trials == null) return null;
            FloatResult probability = getFloat(object.get("p"), context, trials.randomState());
            if (probability == null) return null;
            if (!Float.isFinite(probability.value())) return null;
            int successes = 0;
            XoroshiroState1201 current = probability.randomState();
            for (int index = 0; index < trials.value(); index++) {
                var draw = current.nextFloat();
                current = draw.state();
                if (draw.value() < probability.value()) successes++;
            }
            return new IntResult(successes, current);
        }
        if (type.equals("minecraft:score")) {
            Float score = scoreValue(object, context);
            return score == null ? null : new IntResult(Math.round(score), state);
        }
        return null;
    }

    public static FloatResult getFloat(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        try {
            return getFloatUnchecked(element, context, state);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static FloatResult getFloatUnchecked(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        if (element == null) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            Float value = numberValue(element);
            return value == null || !Float.isFinite(value) ? null : new FloatResult(value, state);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = type(object);
        if (type.equals("minecraft:constant")) {
            Float value = numberValue(object.get("value"));
            return value == null || !Float.isFinite(value) ? null : new FloatResult(value, state);
        }
        if (type.equals("minecraft:uniform")) {
            FloatResult min = getFloat(object.get("min"), context, state);
            if (min == null) return null;
            FloatResult max = getFloat(object.get("max"), context, min.randomState());
            if (max == null) return null;
            if (min.value() >= max.value()) return new FloatResult(min.value(), max.randomState());
            var draw = max.randomState().nextFloat();
            float value = draw.value() * (max.value() - min.value()) + min.value();
            return Float.isFinite(value) ? new FloatResult(value, draw.state()) : null;
        }
        if (type.equals("minecraft:score")) {
            Float score = scoreValue(object, context);
            return score == null ? null : new FloatResult(score, state);
        }
        IntResult integer = getInt(element, context, state);
        return integer == null ? null : new FloatResult(integer.value(), integer.randomState());
    }

    private static Float scoreValue(JsonObject provider, LootAnalysisContext context) {
        if (!provider.has("score")
                || !provider.get("score").isJsonPrimitive()
                || !provider.has("target")
                || !provider.get("target").isJsonObject()) {
            return null;
        }
        JsonObject target = provider.getAsJsonObject("target");
        String type = type(target);
        String scoreboardName;
        if (type.equals("minecraft:fixed")) {
            if (!target.has("name") || !target.get("name").isJsonPrimitive()) return null;
            scoreboardName = safeString(target.get("name"));
            if (scoreboardName == null) return null;
        } else if (type.equals("minecraft:context")) {
            if (!target.has("target") || !target.get("target").isJsonPrimitive()) return null;
            LootContext.EntityTarget entityTarget;
            try {
                String targetName = safeString(target.get("target"));
                if (targetName == null) return null;
                entityTarget = LootContext.EntityTarget.getByName(targetName);
            } catch (IllegalArgumentException exception) {
                return null;
            }
            Entity entity = context.entity(entityTarget);
            if (entity == null) return 0.0F;
            scoreboardName = entity.getScoreboardName();
        } else {
            return null;
        }
        if (context.level() == null) return null;
        Scoreboard scoreboard = context.level().getScoreboard();
        String objectiveName = safeString(provider.get("score"));
        if (objectiveName == null) return null;
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null || !scoreboard.hasPlayerScore(scoreboardName, objective)) return 0.0F;
        int score = scoreboard.getOrCreatePlayerScore(scoreboardName, objective).getScore();
        Float scaleValue = provider.has("scale") ? numberValue(provider.get("scale")) : 1.0F;
        if (scaleValue == null) return null;
        float scale = scaleValue;
        float result = (float) score * scale;
        return Float.isFinite(result) ? result : null;
    }

    private static Float numberValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            return null;
        try {
            return value.getAsFloat();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String type(JsonObject object) {
        if (!object.has("type")) return "";
        JsonElement value = object.get("type");
        String result =
                value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        ? safeString(value)
                        : "";
        return result == null ? "" : result;
    }

    private static String safeString(JsonElement value) {
        try {
            return value == null ? null : value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public record IntResult(int value, XoroshiroState1201 randomState) {}

    public record FloatResult(float value, XoroshiroState1201 randomState) {}
}
