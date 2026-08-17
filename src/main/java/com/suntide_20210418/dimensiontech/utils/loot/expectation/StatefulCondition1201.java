package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/** Ordered core LootItemCondition execution over a Minecraft 1.20.1 Xoroshiro state. */
public final class StatefulCondition1201 {
    private StatefulCondition1201() {}

    public static Result testAll(
            JsonElement conditions, LootAnalysisContext context, XoroshiroState1201 state) {
        return testAll(conditions, context, state, null);
    }

    public static Result testAll(
            JsonElement conditions,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references) {
        return testAll(conditions, context, state, references, "");
    }

    public static Result testAll(
            JsonElement conditions,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references,
            String pointer) {
        try {
            return testAllUnchecked(conditions, context, state, references, safePointer(pointer));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Result testAllUnchecked(
            JsonElement conditions,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references,
            String pointer) {
        if (conditions == null) return new Result(true, state);
        if (!conditions.isJsonArray()) return test(conditions, context, state, references, pointer);
        XoroshiroState1201 current = state;
        for (int index = 0; index < conditions.getAsJsonArray().size(); index++) {
            JsonElement condition = conditions.getAsJsonArray().get(index);
            Result result =
                    test(
                            condition,
                            context,
                            current,
                            references,
                            pointer + "/" + index);
            if (result == null) return null;
            current = result.randomState();
            if (!result.value()) return new Result(false, current);
        }
        return new Result(true, current);
    }

    public static Result test(
            JsonElement element, LootAnalysisContext context, XoroshiroState1201 state) {
        return test(element, context, state, null);
    }

    public static Result test(
            JsonElement element,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references) {
        return test(element, context, state, references, "");
    }

    public static Result test(
            JsonElement element,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references,
            String pointer) {
        try {
            return testUnchecked(element, context, state, references, safePointer(pointer));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Result testUnchecked(
            JsonElement element,
            LootAnalysisContext context,
            XoroshiroState1201 state,
            ReferenceResolver references,
            String pointer) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject condition = element.getAsJsonObject();
        String type = stringField(condition, "condition");
        if (type == null) return null;
        if (type.equals("minecraft:always_true")) return new Result(true, state);
        if (type.equals("minecraft:always_false")) return new Result(false, state);
        if (type.equals("minecraft:random_chance")) {
            if (!condition.has("chance")) return null;
            Float thresholdValue = floatField(condition, "chance");
            if (thresholdValue == null) return null;
            float threshold = thresholdValue;
            if (!Float.isFinite(threshold)) return null;
            var draw = state.nextFloat();
            return new Result(draw.value() < threshold, draw.state());
        }
        if (type.equals("minecraft:random_chance_with_looting")) {
            if (!condition.has("chance") || !condition.has("looting_multiplier")) return null;
            Float chanceValue = floatField(condition, "chance");
            Float multiplierValue = floatField(condition, "looting_multiplier");
            if (chanceValue == null || multiplierValue == null) return null;
            float chance = chanceValue;
            float multiplier = multiplierValue;
            float threshold = chance + (float) context.lootingModifier() * multiplier;
            if (!Float.isFinite(threshold)) return null;
            var draw = state.nextFloat();
            return new Result(draw.value() < threshold, draw.state());
        }
        if (type.equals("minecraft:killed_by_player")) {
            return new Result(Boolean.TRUE.equals(context.killedByPlayer()), state);
        }
        if (type.equals("minecraft:survives_explosion")) {
            if (context.explosionRadius() == null) return new Result(true, state);
            var draw = state.nextFloat();
            float survivalChance = 1.0F / context.explosionRadius();
            return new Result(draw.value() <= survivalChance, draw.state());
        }
        if (type.equals("minecraft:table_bonus")) {
            ResourceLocation enchantmentId = resourceLocationField(condition, "enchantment");
            Enchantment enchantment =
                    enchantmentId == null
                            ? null
                            : ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            JsonArray chances =
                    condition.has("chances") && condition.get("chances").isJsonArray()
                            ? condition.getAsJsonArray("chances")
                            : null;
            if (enchantment == null || chances == null || chances.isEmpty()) {
                return null;
            }
            int level =
                    context.tool() == null ? 0 : context.tool().getEnchantmentLevel(enchantment);
            Float chanceValue = floatValue(chances.get(Math.min(level, chances.size() - 1)));
            if (chanceValue == null) return null;
            float chance = chanceValue;
            if (!Float.isFinite(chance)) return null;
            var draw = state.nextFloat();
            return new Result(draw.value() < chance, draw.state());
        }
        if (type.equals("minecraft:inverted")) {
            Result nested =
                    test(condition.get("term"), context, state, references, pointer + "/term");
            return nested == null ? null : new Result(!nested.value(), nested.randomState());
        }
        if (type.equals("minecraft:all_of")) {
            JsonArray terms =
                    condition.has("terms") && condition.get("terms").isJsonArray()
                            ? condition.getAsJsonArray("terms")
                            : null;
            return terms == null
                    ? null
                    : testAll(terms, context, state, references, pointer + "/terms");
        }
        if (type.equals("minecraft:any_of")) {
            JsonArray terms =
                    condition.has("terms") && condition.get("terms").isJsonArray()
                            ? condition.getAsJsonArray("terms")
                            : null;
            if (terms == null) return null;
            XoroshiroState1201 current = state;
            for (int index = 0; index < terms.size(); index++) {
                Result result =
                        test(
                                terms.get(index),
                                context,
                                current,
                                references,
                                pointer + "/terms/" + index);
                if (result == null) return null;
                current = result.randomState();
                if (result.value()) return new Result(true, current);
            }
            return new Result(false, current);
        }
        if (type.equals("minecraft:reference")) {
            ResourceLocation id = resourceLocationField(condition, "name");
            return id == null || references == null
                    ? null
                    : references.resolve(id, state, pointer + "/name");
        }
        return null;
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return "";
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
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

    private static String safePointer(String pointer) {
        return pointer == null ? "" : pointer;
    }

    @FunctionalInterface
    public interface ReferenceResolver {
        Result resolve(ResourceLocation id, XoroshiroState1201 randomState);

        default Result resolve(
                ResourceLocation id, XoroshiroState1201 randomState, String pointer) {
            return resolve(id, randomState);
        }
    }

    public record Result(boolean value, XoroshiroState1201 randomState) {}
}
