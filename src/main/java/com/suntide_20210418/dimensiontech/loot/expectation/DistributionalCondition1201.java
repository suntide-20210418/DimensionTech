package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/** Lazy finite branching for ordered Minecraft 1.20.1 loot conditions. */
public final class DistributionalCondition1201 {
    private DistributionalCondition1201() {}

    public static Evaluation testAll(
            JsonElement conditions, LootAnalysisContext context, int maxStates, String pointer) {
        return testAll(conditions, context, maxStates, pointer, null);
    }

    public static Evaluation testAll(
            JsonElement conditions,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            ReferenceResolver references) {
        String safePointer = pointer == null ? "" : pointer;
        try {
            return testAllUnchecked(conditions, context, maxStates, safePointer, references);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(safePointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return Evaluation.unsupported(safePointer, malformedJsonMessage(exception));
        }
    }

    private static Evaluation testAllUnchecked(
            JsonElement conditions,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            ReferenceResolver references) {
        if (conditions == null) return Evaluation.exact(RandomTraceDistribution.singleton(true));
        if (!conditions.isJsonArray()) {
            return test(conditions, context, maxStates, pointer, references);
        }
        RandomTraceDistribution<Boolean> current = RandomTraceDistribution.singleton(true);
        JsonArray array = conditions.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            if (!canReach(current, true)) break;
            Evaluation next =
                    test(array.get(index), context, maxStates, pointer + "/" + index, references);
            if (!next.supported()) return next;
            RandomTraceDistribution<Boolean> term = next.distribution();
            try {
                current =
                        current.flatMap(
                                prior -> prior ? term : RandomTraceDistribution.singleton(false),
                                maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer + "/" + index, exception.getMessage());
            }
        }
        return Evaluation.exact(current);
    }

    public static Evaluation test(
            JsonElement element,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            ReferenceResolver references) {
        String safePointer = pointer == null ? "" : pointer;
        try {
            return testUnchecked(element, context, maxStates, safePointer, references);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(safePointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return Evaluation.unsupported(safePointer, malformedJsonMessage(exception));
        }
    }

    private static Evaluation testUnchecked(
            JsonElement element,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            ReferenceResolver references) {
        if (element == null || !element.isJsonObject()) {
            return Evaluation.unsupported(pointer, "Condition is not an object");
        }
        JsonObject condition = element.getAsJsonObject();
        String type = stringField(condition, "condition");
        if (type == null) {
            return Evaluation.unsupported(pointer + "/condition", "Invalid condition type");
        }
        if (type.equals("minecraft:always_true")) return constant(true);
        if (type.equals("minecraft:always_false")) return constant(false);
        if (type.equals("minecraft:random_chance")) {
            if (!condition.has("chance")) {
                return Evaluation.unsupported(pointer + "/chance", "Missing random chance");
            }
            Float chanceValue = floatField(condition, "chance");
            if (chanceValue == null) {
                return Evaluation.unsupported(pointer + "/chance", "Invalid random chance");
            }
            float chance = chanceValue;
            if (!Float.isFinite(chance)) {
                return Evaluation.unsupported(pointer + "/chance", "Non-finite random chance");
            }
            return random(ExactRandomSemantics1201.nextFloatLessThan(chance));
        }
        if (type.equals("minecraft:random_chance_with_looting")) {
            if (!condition.has("chance")) {
                return Evaluation.unsupported(pointer + "/chance", "Missing base chance");
            }
            if (!condition.has("looting_multiplier")) {
                return Evaluation.unsupported(
                        pointer + "/looting_multiplier", "Missing looting multiplier");
            }
            Float chanceValue = floatField(condition, "chance");
            Float multiplierValue = floatField(condition, "looting_multiplier");
            if (chanceValue == null) {
                return Evaluation.unsupported(pointer + "/chance", "Invalid base chance");
            }
            if (multiplierValue == null) {
                return Evaluation.unsupported(
                        pointer + "/looting_multiplier", "Invalid looting multiplier");
            }
            float chance = chanceValue;
            float multiplier = multiplierValue;
            float threshold = chance + (float) context.lootingModifier() * multiplier;
            if (!Float.isFinite(threshold)) {
                return Evaluation.unsupported(pointer, "Non-finite looting-adjusted chance");
            }
            return random(ExactRandomSemantics1201.nextFloatLessThan(threshold));
        }
        if (type.equals("minecraft:killed_by_player")) {
            return constant(Boolean.TRUE.equals(context.killedByPlayer()));
        }
        if (type.equals("minecraft:survives_explosion")) {
            if (context.explosionRadius() == null) return constant(true);
            return random(
                    ExactRandomSemantics1201.nextFloatAtMost(1.0F / context.explosionRadius()));
        }
        if (type.equals("minecraft:table_bonus")) {
            ResourceLocation enchantmentId = resourceLocationField(condition, "enchantment");
            Enchantment enchantment =
                    enchantmentId == null
                            ? null
                            : ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchantment == null) {
                return Evaluation.unsupported(
                        pointer + "/enchantment", "Invalid table bonus enchantment");
            }
            JsonArray chances =
                    condition.has("chances") && condition.get("chances").isJsonArray()
                            ? condition.getAsJsonArray("chances")
                            : null;
            if (chances == null || chances.isEmpty()) {
                return Evaluation.unsupported(pointer + "/chances", "Invalid table bonus chances");
            }
            int level =
                    context.tool() == null ? 0 : context.tool().getEnchantmentLevel(enchantment);
            int chanceIndex = Math.min(level, chances.size() - 1);
            Float chanceValue = floatValue(chances.get(chanceIndex));
            if (chanceValue == null) {
                return Evaluation.unsupported(
                        pointer + "/chances/" + chanceIndex, "Invalid table bonus chance");
            }
            float chance = chanceValue;
            if (!Float.isFinite(chance)) {
                return Evaluation.unsupported(
                        pointer + "/chances/" + chanceIndex, "Non-finite table bonus chance");
            }
            return random(ExactRandomSemantics1201.nextFloatLessThan(chance));
        }
        if (type.equals("minecraft:inverted")) {
            Evaluation nested =
                    test(condition.get("term"), context, maxStates, pointer + "/term", references);
            if (!nested.supported()) return nested;
            try {
                return Evaluation.exact(
                        nested.distribution()
                                .flatMap(
                                        value -> RandomTraceDistribution.singleton(!value),
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:all_of")) {
            JsonElement terms = condition.get("terms");
            return terms == null || !terms.isJsonArray()
                    ? Evaluation.unsupported(pointer + "/terms", "Invalid all_of terms")
                    : testAll(terms, context, maxStates, pointer + "/terms", references);
        }
        if (type.equals("minecraft:any_of")) {
            JsonArray terms =
                    condition.has("terms") && condition.get("terms").isJsonArray()
                            ? condition.getAsJsonArray("terms")
                            : null;
            if (terms == null) {
                return Evaluation.unsupported(pointer + "/terms", "Invalid any_of terms");
            }
            RandomTraceDistribution<Boolean> current = RandomTraceDistribution.singleton(false);
            for (int index = 0; index < terms.size(); index++) {
                if (!canReach(current, false)) break;
                Evaluation next =
                        test(
                                terms.get(index),
                                context,
                                maxStates,
                                pointer + "/terms/" + index,
                                references);
                if (!next.supported()) return next;
                RandomTraceDistribution<Boolean> term = next.distribution();
                try {
                    current =
                            current.flatMap(
                                    prior -> prior ? RandomTraceDistribution.singleton(true) : term,
                                    maxStates);
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    return Evaluation.randomSemantics(
                            pointer + "/terms/" + index, exception.getMessage());
                }
            }
            return Evaluation.exact(current);
        }
        if (type.equals("minecraft:reference")) {
            ResourceLocation id = resourceLocationField(condition, "name");
            return id == null || references == null
                    ? Evaluation.unsupported(pointer + "/name", "Invalid predicate reference")
                    : references.resolve(id, maxStates, pointer);
        }
        return Evaluation.unsupported(pointer, "Unsupported reachable condition " + type);
    }

    private static boolean canReach(RandomTraceDistribution<Boolean> distribution, boolean value) {
        return !distribution
                .marginal()
                .masses()
                .getOrDefault(value, ExactProbability.ZERO)
                .isZero();
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? safeString(value)
                : null;
    }

    private static Float floatField(JsonObject object, String name) {
        return object.has(name) ? floatValue(object.get(name)) : null;
    }

    private static Float floatValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            return null;
        try {
            return value.getAsFloat();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ResourceLocation resourceLocationField(JsonObject object, String name) {
        String value = stringField(object, name);
        if (value == null || value.isEmpty()) return null;
        try {
            return ResourceLocation.tryParse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String safeString(JsonElement value) {
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable condition AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static Evaluation constant(boolean value) {
        return Evaluation.exact(RandomTraceDistribution.singleton(value));
    }

    private static Evaluation random(ExactRandomSemantics1201.RandomResult<Boolean> result) {
        return Evaluation.exact(RandomTraceDistribution.fromRandomResult(result));
    }

    @FunctionalInterface
    public interface ReferenceResolver {
        Evaluation resolve(ResourceLocation id, int maxStates, String pointer);
    }

    public record Evaluation(
            boolean supported,
            RandomTraceDistribution<Boolean> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public static Evaluation exact(RandomTraceDistribution<Boolean> distribution) {
            return new Evaluation(true, distribution, "", "", null);
        }

        public static Evaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static Evaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new Evaluation(false, null, pointer, message, failureKind);
        }

        public static Evaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }
}
