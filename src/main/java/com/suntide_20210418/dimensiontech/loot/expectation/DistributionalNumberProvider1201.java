package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Finite ordered branching for NumberProvider calls used by Minecraft 1.20.1 loot pools. */
public final class DistributionalNumberProvider1201 {
    private DistributionalNumberProvider1201() {}

    public static Evaluation<Integer> getInt(
            JsonElement element, LootAnalysisContext context, int maxStates, String pointer) {
        String safePointer = pointer == null ? "" : pointer;
        try {
            return getIntUnchecked(element, context, maxStates, safePointer);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(safePointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return Evaluation.unsupported(safePointer, malformedJsonMessage(exception));
        }
    }

    private static Evaluation<Integer> getIntUnchecked(
            JsonElement element, LootAnalysisContext context, int maxStates, String pointer) {
        if (element == null) return Evaluation.unsupported(pointer, "Missing number provider");
        if (isNumber(element)) {
            Float value = safeFloat(element);
            if (value == null || !Float.isFinite(value)) {
                return Evaluation.unsupported(pointer, "Invalid numeric provider");
            }
            return Evaluation.exact(RandomTraceDistribution.singleton(Math.round(value)));
        }
        if (!element.isJsonObject()) {
            return Evaluation.unsupported(pointer, "Number provider is not an object");
        }
        JsonObject provider = element.getAsJsonObject();
        String type = type(provider);
        if (type == null || type.isEmpty()) {
            return Evaluation.unsupported(pointer + "/type", "Invalid number provider type");
        }
        if (type.equals("minecraft:constant")) {
            Float value = provider.has("value") ? safeFloat(provider.get("value")) : null;
            return provider.has("value")
                            && isNumber(provider.get("value"))
                            && value != null
                            && Float.isFinite(value)
                    ? Evaluation.exact(RandomTraceDistribution.singleton(Math.round(value)))
                    : Evaluation.unsupported(pointer + "/value", "Invalid constant provider");
        }
        if (type.equals("minecraft:uniform")) {
            Evaluation<Integer> min =
                    getInt(provider.get("min"), context, maxStates, pointer + "/min");
            if (!min.supported()) return min;
            Evaluation<Integer> max =
                    getInt(provider.get("max"), context, maxStates, pointer + "/max");
            if (!max.supported()) return max;
            try {
                return Evaluation.exact(
                        min.distribution()
                                .flatMap(
                                        minimum ->
                                                max.distribution()
                                                        .flatMap(
                                                                maximum ->
                                                                        uniformInt(
                                                                                minimum, maximum),
                                                                maxStates),
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:binomial")) {
            Evaluation<Integer> trials =
                    getInt(provider.get("n"), context, maxStates, pointer + "/n");
            if (!trials.supported()) return trials;
            Float constantProbability = constantFloat(provider.get("p"));
            if (constantProbability == null || !Float.isFinite(constantProbability)) {
                return Evaluation.unsupported(
                        pointer + "/p", "Non-constant binomial probability is not yet supported");
            }
            try {
                return Evaluation.exact(
                        trials.distribution()
                                .flatMap(
                                        value -> {
                                            if (value < 0) {
                                                throw new IllegalArgumentException(
                                                        "Negative binomial trial count");
                                            }
                                            return RandomTraceDistribution.fromRandomResult(
                                                    ExactRandomSemantics1201.binomial(
                                                            value, constantProbability));
                                        },
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:score")) {
            String shapeFailure = scoreShapeFailure(provider);
            if (shapeFailure != null) {
                return Evaluation.unsupported(
                        pointer + shapeFailure, "Invalid score provider field");
            }
            Float score = scoreValue(provider, context);
            return score == null
                    ? Evaluation.unsupported(pointer, "Invalid score provider or context")
                    : Evaluation.exact(RandomTraceDistribution.singleton(Math.round(score)));
        }
        return Evaluation.unsupported(pointer, "Unsupported reachable number provider " + type);
    }

    public static Evaluation<Integer> getBonusFloor(
            JsonElement element, LootAnalysisContext context, int maxStates, String pointer) {
        String safePointer = pointer == null ? "" : pointer;
        try {
            return getBonusFloorUnchecked(element, context, maxStates, safePointer);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(safePointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return Evaluation.unsupported(safePointer, malformedJsonMessage(exception));
        }
    }

    private static Evaluation<Integer> getBonusFloorUnchecked(
            JsonElement element, LootAnalysisContext context, int maxStates, String pointer) {
        if (element == null) {
            return Evaluation.exact(RandomTraceDistribution.singleton(0));
        }
        Float constant = constantFloat(element);
        if (constant != null) {
            if (!Float.isFinite(constant)) {
                return Evaluation.unsupported(pointer, "Invalid bonus provider value");
            }
            return Evaluation.exact(
                    RandomTraceDistribution.singleton(Mth.floor(constant * context.luck())));
        }
        if (!element.isJsonObject()) {
            return Evaluation.unsupported(pointer, "Bonus provider is not an object");
        }
        JsonObject provider = element.getAsJsonObject();
        String type = type(provider);
        if (type == null || type.isEmpty()) {
            return Evaluation.unsupported(pointer + "/type", "Invalid bonus provider type");
        }
        if (type.equals("minecraft:score")) {
            String shapeFailure = scoreShapeFailure(provider);
            if (shapeFailure != null) {
                return Evaluation.unsupported(
                        pointer + shapeFailure, "Invalid score provider field");
            }
            Float score = scoreValue(provider, context);
            return score == null
                    ? Evaluation.unsupported(pointer, "Invalid score provider or context")
                    : Evaluation.exact(
                            RandomTraceDistribution.singleton(Mth.floor(score * context.luck())));
        }
        if (!type.equals("minecraft:uniform")) {
            return Evaluation.unsupported(pointer, "Unsupported reachable bonus provider " + type);
        }
        Float min = constantFloat(provider.get("min"));
        Float max = constantFloat(provider.get("max"));
        if (min == null || !Float.isFinite(min)) {
            return Evaluation.unsupported(pointer + "/min", "Invalid uniform bonus minimum");
        }
        if (max == null || !Float.isFinite(max)) {
            return Evaluation.unsupported(pointer + "/max", "Invalid uniform bonus maximum");
        }
        try {
            return Evaluation.exact(
                    RandomTraceDistribution.fromRandomResult(
                            ExactRandomSemantics1201.uniformFloatTimesLuckFloor(
                                    min, max, context.luck(), maxStates)));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(pointer, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Evaluation.unsupported(pointer, exception.getMessage());
        }
    }

    private static RandomTraceDistribution<Integer> uniformInt(int min, int max) {
        if (min >= max) return RandomTraceDistribution.singleton(min);
        return RandomTraceDistribution.fromRandomResult(
                ExactRandomSemantics1201.uniformIntInclusive(min, max));
    }

    private static String scoreShapeFailure(JsonObject provider) {
        if (!isString(provider.get("score"))) return "/score";
        JsonElement targetElement = provider.get("target");
        if (targetElement == null || !targetElement.isJsonObject()) return "/target";
        JsonObject target = targetElement.getAsJsonObject();
        String targetType = type(target);
        if (targetType == null || targetType.isEmpty()) return "/target/type";
        if (targetType.equals("minecraft:fixed")) {
            if (!isString(target.get("name"))) return "/target/name";
        } else if (targetType.equals("minecraft:context")) {
            if (!isString(target.get("target"))) return "/target/target";
            try {
                if (LootContext.EntityTarget.getByName(safeString(target.get("target"))) == null) {
                    return "/target/target";
                }
            } catch (RuntimeException exception) {
                return "/target/target";
            }
        } else {
            return "/target/type";
        }
        if (provider.has("scale")) {
            Float scale = isNumber(provider.get("scale")) ? safeFloat(provider.get("scale")) : null;
            if (scale == null || !Float.isFinite(scale)) return "/scale";
        }
        return null;
    }

    private static Float scoreValue(JsonObject provider, LootAnalysisContext context) {
        if (!provider.has("score")
                || !provider.get("score").isJsonPrimitive()
                || !provider.has("target")
                || !provider.get("target").isJsonObject()) {
            return null;
        }
        JsonObject target = provider.getAsJsonObject("target");
        String targetType = type(target);
        String scoreboardName;
        if (targetType.equals("minecraft:fixed")) {
            if (!target.has("name") || !target.get("name").isJsonPrimitive()) return null;
            scoreboardName = safeString(target.get("name"));
            if (scoreboardName == null) return null;
        } else if (targetType.equals("minecraft:context")) {
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
        if (objective == null || !scoreboard.hasPlayerScore(scoreboardName, objective)) {
            return 0.0F;
        }
        int value = scoreboard.getOrCreatePlayerScore(scoreboardName, objective).getScore();
        if (provider.has("scale") && !isNumber(provider.get("scale"))) return null;
        Float scaleValue = provider.has("scale") ? safeFloat(provider.get("scale")) : 1.0F;
        if (scaleValue == null || !Float.isFinite(scaleValue)) return null;
        float scale = scaleValue;
        float result = (float) value * scale;
        return Float.isFinite(result) ? result : null;
    }

    private static Float constantFloat(JsonElement element) {
        if (isNumber(element)) return safeFloat(element);
        if (element == null || !element.isJsonObject()) return null;
        JsonObject provider = element.getAsJsonObject();
        return "minecraft:constant".equals(type(provider))
                        && provider.has("value")
                        && isNumber(provider.get("value"))
                ? safeFloat(provider.get("value"))
                : null;
    }

    private static boolean isNumber(JsonElement element) {
        return element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isNumber();
    }

    private static boolean isString(JsonElement element) {
        return element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && safeString(element) != null;
    }

    private static String type(JsonObject provider) {
        if (!provider.has("type")) return "";
        JsonElement value = provider.get("type");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? safeString(value)
                : null;
    }

    private static Float safeFloat(JsonElement value) {
        try {
            return value == null ? null : value.getAsFloat();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String safeString(JsonElement value) {
        try {
            return value == null ? null : value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable number provider AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    public record Evaluation<T>(
            boolean supported,
            RandomTraceDistribution<T> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static <T> Evaluation<T> exact(RandomTraceDistribution<T> distribution) {
            return new Evaluation<>(true, distribution, "", "", null);
        }

        private static <T> Evaluation<T> unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static <T> Evaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new Evaluation<>(false, null, pointer, message, failureKind);
        }

        private static <T> Evaluation<T> randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }
}
