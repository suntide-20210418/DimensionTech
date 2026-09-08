package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomCall;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomMethod;
import com.suntide_20210418.dimensiontech.utils.FullDurabilityLoot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Linear StackState function kernels with lazy finite branching. */
public final class DistributionalFunction1201 {
    private DistributionalFunction1201() {}

    public static Evaluation applyAll(
            StackState input,
            JsonElement functions,
            LootAnalysisContext context,
            int maxStates,
            String functionsPointer) {
        return applyAll(input, functions, context, maxStates, functionsPointer, null, null);
    }

    public static Evaluation applyAll(
            StackState input,
            JsonElement functions,
            LootAnalysisContext context,
            int maxStates,
            String functionsPointer,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences) {
        try {
            return applyAllUnchecked(
                    input,
                    functions,
                    context,
                    maxStates,
                    functionsPointer,
                    conditionReferences,
                    functionReferences);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return Evaluation.randomSemantics(
                    functionsPointer == null ? "" : functionsPointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return Evaluation.unsupported(
                    functionsPointer == null ? "" : functionsPointer,
                    malformedJsonMessage(exception));
        }
    }

    private static Evaluation applyAllUnchecked(
            StackState input,
            JsonElement functions,
            LootAnalysisContext context,
            int maxStates,
            String functionsPointer,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences) {
        if (functions == null) {
            return Evaluation.exact(RandomTraceDistribution.singleton(input));
        }
        if (!functions.isJsonArray()) {
            return Evaluation.unsupported(functionsPointer, "Function list is not an array");
        }
        RandomTraceDistribution<StackState> current = RandomTraceDistribution.singleton(input);
        JsonArray array = functions.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            String pointer = functionsPointer + "/" + index;
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                return Evaluation.unsupported(pointer, "Function is not an object");
            }
            JsonObject function = element.getAsJsonObject();
            if (!function.has("function")) {
                return Evaluation.unsupported(pointer + "/function", "Function type is missing");
            }
            if (stringField(function, "function") == null) {
                return Evaluation.unsupported(
                        pointer + "/function", "Function type is not a string");
            }
            DistributionalCondition1201.Evaluation conditions;
            try {
                conditions =
                        DistributionalCondition1201.testAll(
                                function.get("conditions"),
                                context,
                                maxStates,
                                pointer + "/conditions",
                                conditionReferences);
            } catch (RuntimeException exception) {
                return Evaluation.unsupported(
                        pointer + "/conditions", malformedJsonMessage(exception));
            }
            if (!conditions.supported()) {
                return Evaluation.unsupported(
                        conditions.pointer(), conditions.message(), conditions.failureKind());
            }
            if (!canReachTrue(conditions.distribution())) {
                try {
                    current =
                            current.flatMap(
                                    stack ->
                                            conditions
                                                    .distribution()
                                                    .flatMap(
                                                            ignored ->
                                                                    RandomTraceDistribution
                                                                            .singleton(stack),
                                                            maxStates),
                                    maxStates);
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    return Evaluation.randomSemantics(pointer, exception.getMessage());
                }
                continue;
            }

            Map<StackState, RandomTraceDistribution<StackState>> kernels = new LinkedHashMap<>();
            for (StackState stack : current.marginal().masses().keySet()) {
                Evaluation kernel =
                        applyOne(stack, function, context, maxStates, pointer, functionReferences);
                if (!kernel.supported()) return kernel;
                kernels.put(stack, kernel.distribution());
            }
            try {
                current =
                        current.flatMap(
                                stack ->
                                        conditions
                                                .distribution()
                                                .flatMap(
                                                        pass ->
                                                                pass
                                                                        ? kernels.get(stack)
                                                                        : RandomTraceDistribution
                                                                                .singleton(stack),
                                                        maxStates),
                                maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        return Evaluation.exact(current, allocatesMap(input, current));
    }

    /**
     * Exact first-moment function execution. Fresh ideal RandomSource calls do not depend on a
     * discarded call trace, so each completed function can be marginalized before the next one.
     */
    public static ExpectedEvaluation applyAllExpected(
            StackState input,
            JsonElement functions,
            LootAnalysisContext context,
            int maxStates,
            String functionsPointer,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences) {
        try {
            return applyAllExpectedUnchecked(
                    input,
                    functions,
                    context,
                    maxStates,
                    functionsPointer,
                    conditionReferences,
                    functionReferences);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExpectedEvaluation.randomSemantics(
                    functionsPointer == null ? "" : functionsPointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return ExpectedEvaluation.unsupported(
                    functionsPointer == null ? "" : functionsPointer,
                    malformedJsonMessage(exception));
        }
    }

    private static ExpectedEvaluation applyAllExpectedUnchecked(
            StackState input,
            JsonElement functions,
            LootAnalysisContext context,
            int maxStates,
            String functionsPointer,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences) {
        if (functions == null) {
            return ExpectedEvaluation.exact(FiniteDistribution.singleton(input), false, false);
        }
        if (!functions.isJsonArray()) {
            return ExpectedEvaluation.unsupported(
                    functionsPointer, "Function list is not an array");
        }
        FiniteDistribution<StackState> current = FiniteDistribution.singleton(input);
        boolean hasRandomCalls = false;
        JsonArray array = functions.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            String pointer = functionsPointer + "/" + index;
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                return ExpectedEvaluation.unsupported(pointer, "Function is not an object");
            }
            JsonObject function = element.getAsJsonObject();
            if (!function.has("function")) {
                return ExpectedEvaluation.unsupported(
                        pointer + "/function", "Function type is missing");
            }
            if (stringField(function, "function") == null) {
                return ExpectedEvaluation.unsupported(
                        pointer + "/function", "Function type is not a string");
            }
            DistributionalCondition1201.Evaluation conditions;
            try {
                conditions =
                        DistributionalCondition1201.testAll(
                                function.get("conditions"),
                                context,
                                maxStates,
                                pointer + "/conditions",
                                conditionReferences);
            } catch (RuntimeException exception) {
                return ExpectedEvaluation.unsupported(
                        pointer + "/conditions", malformedJsonMessage(exception));
            }
            if (!conditions.supported()) {
                return ExpectedEvaluation.unsupported(
                        conditions.pointer(), conditions.message(), conditions.failureKind());
            }
            hasRandomCalls |= hasRandomCalls(conditions.distribution());
            FiniteDistribution<Boolean> conditionMasses = conditions.distribution().marginal();
            ExactProbability passMass =
                    conditionMasses.masses().getOrDefault(true, ExactProbability.ZERO);
            ExactProbability failMass =
                    conditionMasses.masses().getOrDefault(false, ExactProbability.ZERO);
            if (passMass.isZero()) continue;

            if (functionType(function).equals("minecraft:enchant_with_levels")) {
                DistributionalNumberProvider1201.Evaluation<Integer> levels =
                        DistributionalNumberProvider1201.getInt(
                                function.get("levels"), context, maxStates, pointer + "/levels");
                if (!levels.supported()) {
                    return ExpectedEvaluation.unsupported(
                            levels.pointer(), levels.message(), levels.failureKind());
                }
                Boolean treasureValue = booleanField(function, "treasure", false);
                if (treasureValue == null) {
                    return ExpectedEvaluation.unsupported(
                            pointer + "/treasure", "Invalid treasure boolean");
                }
                boolean treasure = treasureValue;
                FiniteDistribution<StackState> enchanted;
                try {
                    enchanted =
                            ExactEnchantmentSemantics1201.enchantItemsMarginal(
                                    current, levels.distribution().marginal(), treasure, maxStates);
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    return ExpectedEvaluation.randomSemantics(pointer, exception.getMessage());
                } catch (IllegalArgumentException exception) {
                    return ExpectedEvaluation.unsupported(pointer, exception.getMessage());
                }
                hasRandomCalls |=
                        hasRandomCalls(levels.distribution())
                                || current.masses().keySet().stream()
                                        .anyMatch(state -> state.stack().getEnchantmentValue() > 0);
                LinkedHashMap<StackState, ExactProbability> next = new LinkedHashMap<>();
                if (!failMass.isZero()) {
                    current.masses()
                            .forEach(
                                    (state, mass) ->
                                            next.merge(
                                                    state,
                                                    mass.multiply(failMass),
                                                    ExactProbability::add));
                }
                enchanted
                        .masses()
                        .forEach(
                                (state, mass) ->
                                        next.merge(
                                                state,
                                                mass.multiply(passMass),
                                                ExactProbability::add));
                if (next.size() > maxStates) {
                    return ExpectedEvaluation.randomSemantics(
                            pointer,
                            "Function state space " + next.size() + " exceeds limit " + maxStates);
                }
                current = FiniteDistribution.of(next);
                continue;
            }

            LinkedHashMap<StackState, ExactProbability> next = new LinkedHashMap<>();
            for (Map.Entry<StackState, ExactProbability> prior : current.masses().entrySet()) {
                if (!failMass.isZero()) {
                    next.merge(
                            prior.getKey(),
                            prior.getValue().multiply(failMass),
                            ExactProbability::add);
                }
                ExpectedKernel kernel =
                        applyOneExpected(
                                prior.getKey(),
                                function,
                                context,
                                maxStates,
                                pointer,
                                functionReferences);
                if (!kernel.supported()) {
                    return ExpectedEvaluation.unsupported(
                            kernel.pointer(), kernel.message(), kernel.failureKind());
                }
                hasRandomCalls |= kernel.hasRandomCalls();
                for (Map.Entry<StackState, ExactProbability> branch :
                        kernel.distribution().masses().entrySet()) {
                    next.merge(
                            branch.getKey(),
                            prior.getValue().multiply(passMass).multiply(branch.getValue()),
                            ExactProbability::add);
                    if (next.size() > maxStates) {
                        return ExpectedEvaluation.randomSemantics(
                                pointer,
                                "Function state space "
                                        + next.size()
                                        + " exceeds limit "
                                        + maxStates);
                    }
                }
            }
            current = FiniteDistribution.of(next);
        }
        boolean allocatesMap =
                input.stack().is(Items.MAP)
                        && current.masses().keySet().stream()
                                .anyMatch(state -> state.stack().is(Items.FILLED_MAP));
        return ExpectedEvaluation.exact(current, hasRandomCalls, allocatesMap);
    }

    private static ExpectedKernel applyOneExpected(
            StackState input,
            JsonObject function,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            FunctionReferenceResolver functionReferences) {
        if (!functionType(function).equals("minecraft:enchant_with_levels")) {
            Evaluation traced =
                    applyOne(input, function, context, maxStates, pointer, functionReferences);
            return traced.supported()
                    ? ExpectedKernel.exact(
                            traced.distribution().marginal(), hasRandomCalls(traced.distribution()))
                    : ExpectedKernel.unsupported(
                            traced.pointer(), traced.message(), traced.failureKind());
        }

        DistributionalNumberProvider1201.Evaluation<Integer> levels =
                DistributionalNumberProvider1201.getInt(
                        function.get("levels"), context, maxStates, pointer + "/levels");
        if (!levels.supported()) {
            return ExpectedKernel.unsupported(
                    levels.pointer(), levels.message(), levels.failureKind());
        }
        Boolean treasureValue = booleanField(function, "treasure", false);
        if (treasureValue == null) {
            return ExpectedKernel.unsupported(pointer + "/treasure", "Invalid treasure boolean");
        }
        boolean treasure = treasureValue;
        FiniteDistribution<StackState> result;
        try {
            result =
                    ExactEnchantmentSemantics1201.enchantItemMarginal(
                            input.stack(), levels.distribution().marginal(), treasure, maxStates);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExpectedKernel.randomSemantics(pointer, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ExpectedKernel.unsupported(pointer, exception.getMessage());
        }
        boolean random =
                hasRandomCalls(levels.distribution()) || input.stack().getEnchantmentValue() > 0;
        return ExpectedKernel.exact(result, random);
    }

    private static boolean allocatesMap(
            StackState input, RandomTraceDistribution<StackState> distribution) {
        return input.stack().is(Items.MAP)
                && distribution.masses().keySet().stream()
                        .anyMatch(outcome -> outcome.value().stack().is(Items.FILLED_MAP));
    }

    private static Evaluation applyOne(
            StackState input,
            JsonObject function,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            FunctionReferenceResolver functionReferences) {
        String type = functionType(function);
        if (type.equals("minecraft:reference")) {
            ResourceLocation id = resourceLocationField(function, "name");
            return id == null || functionReferences == null
                    ? Evaluation.unsupported(pointer + "/name", "Invalid function reference")
                    : functionReferences.resolve(id, input, maxStates, pointer);
        }
        if (type.equals("minecraft:set_count")) {
            DistributionalNumberProvider1201.Evaluation<Integer> counts =
                    DistributionalNumberProvider1201.getInt(
                            function.get("count"), context, maxStates, pointer + "/count");
            if (!counts.supported()) {
                return Evaluation.unsupported(
                        counts.pointer(), counts.message(), counts.failureKind());
            }
            Boolean addValue = booleanField(function, "add", false);
            if (addValue == null) {
                return Evaluation.unsupported(pointer + "/add", "Invalid add boolean");
            }
            boolean add = addValue;
            try {
                return Evaluation.exact(
                        counts.distribution()
                                .flatMap(
                                        value -> {
                                            int count = add ? input.count() + value : value;
                                            return RandomTraceDistribution.singleton(
                                                    input.withCount(
                                                            Mth.clamp(
                                                                    count,
                                                                    0,
                                                                    input.stack()
                                                                            .getMaxStackSize())));
                                        },
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:set_damage")) {
            ItemStack stack = input.stack();
            if (!stack.isDamageableItem()) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            Boolean addValue = booleanField(function, "add", false);
            if (addValue == null) {
                return Evaluation.unsupported(pointer + "/add", "Invalid add boolean");
            }
            boolean add = addValue;
            JsonElement damage = function.get("damage");
            Float constant = constantFloat(damage);
            ExactRandomSemantics1201.RandomResult<Integer> damages;
            try {
                if (constant != null) {
                    damages =
                            new ExactRandomSemantics1201.RandomResult<>(
                                    FiniteDistribution.singleton(
                                            ExactRandomSemantics1201.setDamageValue(
                                                    constant,
                                                    add,
                                                    stack.getDamageValue(),
                                                    stack.getMaxDamage())),
                                    java.util.List.of());
                } else if (damage != null
                        && damage.isJsonObject()
                        && type(damage.getAsJsonObject()).equals("minecraft:uniform")) {
                    Float min = constantFloat(damage.getAsJsonObject().get("min"));
                    Float max = constantFloat(damage.getAsJsonObject().get("max"));
                    if (min == null || max == null) {
                        return Evaluation.unsupported(
                                pointer + "/damage", "Uniform damage bounds are not constant");
                    }
                    damages =
                            context.fullDurability()
                                    ? new ExactRandomSemantics1201.RandomResult<>(
                                            FiniteDistribution.singleton(0),
                                            min >= max
                                                    ? List.of()
                                                    : List.of(
                                                            new RandomCall(
                                                                    RandomMethod.NEXT_FLOAT, 0)))
                                    : ExactRandomSemantics1201.uniformFloatSetDamage(
                                            min,
                                            max,
                                            add,
                                            stack.getDamageValue(),
                                            stack.getMaxDamage(),
                                            maxStates);
                } else {
                    return Evaluation.unsupported(
                            pointer + "/damage", "Unsupported damage provider");
                }
                if (context.fullDurability()) {
                    ItemStack output = FullDurabilityLoot.normalize(input.stack());
                    StackState fullDurability = new StackState(output);
                    return Evaluation.exact(
                            RandomTraceDistribution.fromRandomResult(damages)
                                    .flatMap(
                                            ignored ->
                                                    RandomTraceDistribution.singleton(
                                                            fullDurability),
                                            maxStates));
                }
                return Evaluation.exact(
                        RandomTraceDistribution.fromRandomResult(damages)
                                .flatMap(
                                        value -> {
                                            ItemStack output = input.stack();
                                            output.setDamageValue(value);
                                            return RandomTraceDistribution.singleton(
                                                    new StackState(output));
                                        },
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer + "/damage", exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer + "/damage", exception.getMessage());
            }
        }
        if (type.equals("minecraft:set_name")) {
            if (function.has("entity")) {
                return Evaluation.unsupported(
                        pointer + "/entity", "Context-resolved set_name is not yet supported");
            }
            Component name;
            try {
                JsonElement value = function.get("name");
                name =
                        value != null && value.isJsonPrimitive()
                                ? Component.literal(value.getAsString())
                                : Component.Serializer.fromJson(value);
            } catch (RuntimeException exception) {
                return Evaluation.unsupported(pointer + "/name", "Invalid name component");
            }
            if (name == null) {
                return Evaluation.unsupported(pointer + "/name", "Null name component");
            }
            ItemStack output = input.stack();
            output.setHoverName(name);
            return Evaluation.exact(RandomTraceDistribution.singleton(new StackState(output)));
        }
        if (type.equals("minecraft:set_potion")) {
            ResourceLocation id = resourceLocationField(function, "id");
            Potion potion = id == null ? null : ForgeRegistries.POTIONS.getValue(id);
            if (potion == null) {
                return Evaluation.unsupported(pointer + "/id", "Missing potion reference");
            }
            ItemStack output = input.stack();
            PotionUtils.setPotion(output, potion);
            return Evaluation.exact(RandomTraceDistribution.singleton(new StackState(output)));
        }
        if (type.equals("minecraft:enchant_randomly")) {
            List<Enchantment> candidates = enchantmentCandidates(function, input.stack());
            if (candidates == null) {
                return Evaluation.unsupported(
                        pointer + "/enchantments", "Invalid enchantment reference");
            }
            if (candidates.isEmpty()) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            try {
                return Evaluation.exact(
                        RandomTraceDistribution.fromRandomResult(
                                        ExactRandomSemantics1201.nextInt(candidates.size()))
                                .flatMap(
                                        index -> {
                                            Enchantment enchantment = candidates.get(index);
                                            RandomTraceDistribution<Integer> levels =
                                                    enchantment.getMinLevel()
                                                                    >= enchantment.getMaxLevel()
                                                            ? RandomTraceDistribution.singleton(
                                                                    enchantment.getMinLevel())
                                                            : RandomTraceDistribution
                                                                    .fromRandomResult(
                                                                            ExactRandomSemantics1201
                                                                                    .uniformIntInclusive(
                                                                                            enchantment
                                                                                                    .getMinLevel(),
                                                                                            enchantment
                                                                                                    .getMaxLevel()));
                                            return levels.flatMap(
                                                    level -> {
                                                        ItemStack output;
                                                        if (input.stack().is(Items.BOOK)) {
                                                            output =
                                                                    new ItemStack(
                                                                            Items.ENCHANTED_BOOK);
                                                            EnchantedBookItem.addEnchantment(
                                                                    output,
                                                                    new EnchantmentInstance(
                                                                            enchantment, level));
                                                        } else {
                                                            output = input.stack();
                                                            output.enchant(enchantment, level);
                                                        }
                                                        return RandomTraceDistribution.singleton(
                                                                new StackState(output));
                                                    },
                                                    maxStates);
                                        },
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:enchant_with_levels")) {
            DistributionalNumberProvider1201.Evaluation<Integer> levels =
                    DistributionalNumberProvider1201.getInt(
                            function.get("levels"), context, maxStates, pointer + "/levels");
            if (!levels.supported()) {
                return Evaluation.unsupported(
                        levels.pointer(), levels.message(), levels.failureKind());
            }
            Boolean treasureValue = booleanField(function, "treasure", false);
            if (treasureValue == null) {
                return Evaluation.unsupported(pointer + "/treasure", "Invalid treasure boolean");
            }
            boolean treasure = treasureValue;
            try {
                return Evaluation.exact(
                        levels.distribution()
                                .flatMap(
                                        level ->
                                                ExactEnchantmentSemantics1201.enchantItem(
                                                        input.stack(), level, treasure, maxStates),
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:set_stew_effect")) {
            if (!input.stack().is(Items.SUSPICIOUS_STEW)) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            JsonArray effects =
                    function.has("effects") && function.get("effects").isJsonArray()
                            ? function.getAsJsonArray("effects")
                            : null;
            if (effects == null) {
                return Evaluation.unsupported(pointer + "/effects", "Invalid stew effect list");
            }
            if (effects.isEmpty()) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            Map<Integer, RandomTraceDistribution<StackState>> effectKernels = new LinkedHashMap<>();
            for (int index = 0; index < effects.size(); index++) {
                JsonElement effectElement = effects.get(index);
                if (!effectElement.isJsonObject()) {
                    return Evaluation.unsupported(
                            pointer + "/effects/" + index, "Invalid stew effect entry");
                }
                JsonObject effectObject = effectElement.getAsJsonObject();
                ResourceLocation effectId = resourceLocationField(effectObject, "type");
                MobEffect effect =
                        effectId == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                if (effect == null) {
                    return Evaluation.unsupported(
                            pointer + "/effects/" + index + "/type",
                            "Missing stew effect reference");
                }
                DistributionalNumberProvider1201.Evaluation<Integer> duration =
                        DistributionalNumberProvider1201.getInt(
                                effectObject.get("duration"),
                                context,
                                maxStates,
                                pointer + "/effects/" + index + "/duration");
                if (!duration.supported()) {
                    return Evaluation.unsupported(
                            duration.pointer(), duration.message(), duration.failureKind());
                }
                try {
                    effectKernels.put(
                            index,
                            duration.distribution()
                                    .flatMap(
                                            value -> {
                                                ItemStack output = input.stack();
                                                int ticks =
                                                        effect.isInstantenous()
                                                                ? value
                                                                : value * 20;
                                                SuspiciousStewItem.saveMobEffect(
                                                        output, effect, ticks);
                                                return RandomTraceDistribution.singleton(
                                                        new StackState(output));
                                            },
                                            maxStates));
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    return Evaluation.randomSemantics(pointer, exception.getMessage());
                }
            }
            try {
                return Evaluation.exact(
                        RandomTraceDistribution.fromRandomResult(
                                        ExactRandomSemantics1201.nextInt(effects.size()))
                                .flatMap(effectKernels::get, maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:set_instrument")) {
            String option = stringField(function, "options");
            if (option == null) {
                return Evaluation.unsupported(
                        pointer + "/options", "Instrument options is not a string");
            }
            if (!option.startsWith("#")) {
                return Evaluation.unsupported(
                        pointer + "/options", "Instrument options is not a tag");
            }
            ResourceLocation tagId = ResourceLocation.tryParse(option.substring(1));
            if (tagId == null) {
                return Evaluation.unsupported(pointer + "/options", "Invalid instrument tag");
            }
            TagKey<Instrument> tag = TagKey.create(Registries.INSTRUMENT, tagId);
            java.util.Optional<? extends HolderSet<Instrument>> set =
                    BuiltInRegistries.INSTRUMENT.getTag(tag);
            if (set.isEmpty() || set.get().size() == 0) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            List<Holder<Instrument>> holders = new ArrayList<>();
            set.get().forEach(holders::add);
            for (int index = 0; index < holders.size(); index++) {
                if (holders.get(index).unwrapKey().isEmpty()) {
                    return Evaluation.unsupported(
                            pointer + "/options", "Unkeyed instrument holder");
                }
            }
            try {
                return Evaluation.exact(
                        RandomTraceDistribution.fromRandomResult(
                                        ExactRandomSemantics1201.nextInt(holders.size()))
                                .flatMap(
                                        index -> {
                                            ItemStack output = input.stack();
                                            output.getOrCreateTag()
                                                    .putString(
                                                            "instrument",
                                                            holders.get(index)
                                                                    .unwrapKey()
                                                                    .orElseThrow()
                                                                    .location()
                                                                    .toString());
                                            return RandomTraceDistribution.singleton(
                                                    new StackState(output));
                                        },
                                        maxStates));
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return Evaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        if (type.equals("minecraft:exploration_map")) {
            if (!input.stack().is(Items.MAP)) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            if (!(context.level() instanceof ServerLevel level) || context.origin() == null) {
                return Evaluation.unsupported(
                        pointer, "Exploration map requires ServerLevel and origin context");
            }
            String destinationName =
                    function.has("destination")
                            ? stringField(function, "destination")
                            : "minecraft:on_treasure_maps";
            if (destinationName == null) {
                return Evaluation.unsupported(pointer + "/destination", "Invalid structure tag");
            }
            if (destinationName.startsWith("#")) {
                destinationName = destinationName.substring(1);
            }
            ResourceLocation destinationId = ResourceLocation.tryParse(destinationName);
            if (destinationId == null) {
                return Evaluation.unsupported(pointer + "/destination", "Invalid structure tag");
            }
            MapDecoration.Type decoration;
            try {
                decoration =
                        function.has("decoration")
                                ? MapDecoration.Type.valueOf(
                                        function.get("decoration")
                                                .getAsString()
                                                .toUpperCase(Locale.ROOT))
                                : MapDecoration.Type.MANSION;
            } catch (IllegalArgumentException exception) {
                return Evaluation.unsupported(pointer + "/decoration", "Invalid map decoration");
            }
            Integer zoomValue = integerField(function, "zoom", 2);
            Integer radiusValue = integerField(function, "search_radius", 50);
            Boolean skipKnownValue = booleanField(function, "skip_existing_chunks", true);
            if (zoomValue == null) {
                return Evaluation.unsupported(pointer + "/zoom", "Invalid map zoom");
            }
            if (radiusValue == null) {
                return Evaluation.unsupported(pointer + "/search_radius", "Invalid search radius");
            }
            if (skipKnownValue == null) {
                return Evaluation.unsupported(
                        pointer + "/skip_existing_chunks", "Invalid skip_existing_chunks boolean");
            }
            int zoom = zoomValue;
            int radius = radiusValue;
            boolean skipKnown = skipKnownValue;
            TagKey<Structure> destination = TagKey.create(Registries.STRUCTURE, destinationId);
            BlockPos target =
                    level.findNearestMapStructure(
                            destination, BlockPos.containing(context.origin()), radius, skipKnown);
            if (target == null) {
                return Evaluation.exact(RandomTraceDistribution.singleton(input));
            }
            try {
                ItemStack output =
                        SavedDataTransaction1201.run(
                                level,
                                () -> {
                                    ItemStack map =
                                            MapItem.create(
                                                    level,
                                                    target.getX(),
                                                    target.getZ(),
                                                    (byte) zoom,
                                                    true,
                                                    true);
                                    MapItem.renderBiomePreviewMap(level, map);
                                    MapItemSavedData.addTargetDecoration(
                                            map, target, "+", decoration);
                                    return map.copy();
                                });
                return Evaluation.exact(RandomTraceDistribution.singleton(new StackState(output)));
            } catch (RuntimeException exception) {
                return Evaluation.unsupported(
                        pointer,
                        "Exploration map SavedData transaction failed: "
                                + exception.getClass().getSimpleName());
            }
        }
        return Evaluation.unsupported(pointer, "Unsupported reachable function " + type);
    }

    private static List<Enchantment> enchantmentCandidates(JsonObject function, ItemStack stack) {
        if (function.has("enchantments")) {
            JsonElement values = function.get("enchantments");
            if (values == null || !values.isJsonArray()) return null;
            // EnchantRandomlyFunction uses an empty collection as the sentinel for its
            // randomApplicableEnchantment builder.  The runtime then discovers all currently
            // registered, discoverable enchantments applicable to this stack; an empty JSON array
            // is therefore not a deterministic no-op.
            if (values.getAsJsonArray().isEmpty()) {
                return dynamicEnchantmentCandidates(stack);
            }
            ArrayList<Enchantment> result = new ArrayList<>();
            for (JsonElement value : values.getAsJsonArray()) {
                String text =
                        value != null
                                        && value.isJsonPrimitive()
                                        && value.getAsJsonPrimitive().isString()
                                ? stringValue(value)
                                : null;
                ResourceLocation id = text == null ? null : ResourceLocation.tryParse(text);
                Enchantment enchantment =
                        id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment == null) return null;
                result.add(enchantment);
            }
            return List.copyOf(result);
        }
        return dynamicEnchantmentCandidates(stack);
    }

    private static List<Enchantment> dynamicEnchantmentCandidates(ItemStack stack) {
        boolean book = stack.is(Items.BOOK);
        return BuiltInRegistries.ENCHANTMENT.stream()
                .filter(Enchantment::isDiscoverable)
                .filter(enchantment -> book || enchantment.canEnchant(stack))
                .toList();
    }

    private static Float constantFloat(JsonElement element) {
        if (element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isNumber()) {
            try {
                return element.getAsFloat();
            } catch (RuntimeException exception) {
                return null;
            }
        }
        if (element == null || !element.isJsonObject()) return null;
        JsonObject provider = element.getAsJsonObject();
        if (!type(provider).equals("minecraft:constant")
                || !provider.has("value")
                || !provider.get("value").isJsonPrimitive()
                || !provider.get("value").getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return provider.get("value").getAsFloat();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String type(JsonObject object) {
        if (!object.has("type")) return "";
        JsonElement value = object.get("type");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? stringValue(value)
                : "";
    }

    private static String functionType(JsonObject object) {
        if (!object.has("function")) return "";
        JsonElement value = object.get("function");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? stringValue(value)
                : "";
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String stringValue(JsonElement value) {
        try {
            return value == null ? null : value.getAsString();
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

    private static Boolean booleanField(JsonObject object, String name, boolean defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean()
                : null;
    }

    private static Integer integerField(JsonObject object, String name, int defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            return null;
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable function AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static boolean canReachTrue(RandomTraceDistribution<Boolean> distribution) {
        return !distribution.marginal().masses().getOrDefault(true, ExactProbability.ZERO).isZero();
    }

    private static boolean hasRandomCalls(RandomTraceDistribution<?> distribution) {
        return distribution.masses().keySet().stream()
                .anyMatch(outcome -> !outcome.calls().isEmpty());
    }

    @FunctionalInterface
    public interface FunctionReferenceResolver {
        Evaluation resolve(ResourceLocation id, StackState input, int maxStates, String pointer);
    }

    public record Evaluation(
            boolean supported,
            RandomTraceDistribution<StackState> distribution,
            boolean mayAllocateMap,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public static Evaluation exact(RandomTraceDistribution<StackState> distribution) {
            return exact(distribution, false);
        }

        public static Evaluation exact(
                RandomTraceDistribution<StackState> distribution, boolean mayAllocateMap) {
            return new Evaluation(true, distribution, mayAllocateMap, "", "", null);
        }

        public static Evaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static Evaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new Evaluation(false, null, false, pointer, message, failureKind);
        }

        public static Evaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public record ExpectedEvaluation(
            boolean supported,
            FiniteDistribution<StackState> distribution,
            boolean hasRandomCalls,
            boolean mayAllocateMap,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public static ExpectedEvaluation exact(
                FiniteDistribution<StackState> distribution,
                boolean hasRandomCalls,
                boolean mayAllocateMap) {
            return new ExpectedEvaluation(
                    true, distribution, hasRandomCalls, mayAllocateMap, "", "", null);
        }

        public static ExpectedEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static ExpectedEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedEvaluation(false, null, false, false, pointer, message, failureKind);
        }

        public static ExpectedEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record ExpectedKernel(
            boolean supported,
            FiniteDistribution<StackState> distribution,
            boolean hasRandomCalls,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static ExpectedKernel exact(
                FiniteDistribution<StackState> distribution, boolean hasRandomCalls) {
            return new ExpectedKernel(true, distribution, hasRandomCalls, "", "", null);
        }

        private static ExpectedKernel unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static ExpectedKernel unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedKernel(false, null, false, pointer, message, failureKind);
        }

        private static ExpectedKernel randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }
}
