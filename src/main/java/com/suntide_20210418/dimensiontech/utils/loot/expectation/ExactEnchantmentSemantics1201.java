package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactRandomSemantics1201.RandomCall;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactRandomSemantics1201.RandomMethod;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.RandomTraceDistribution.Outcome;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** Exact finite branching of EnchantmentHelper.enchantItem for Minecraft 1.20.1. */
public final class ExactEnchantmentSemantics1201 {
    private ExactEnchantmentSemantics1201() {}

    public static RandomTraceDistribution<StackState> enchantItem(
            ItemStack input, int level, boolean treasure, int maxStates) {
        int enchantability = input.getEnchantmentValue();
        if (enchantability <= 0) return RandomTraceDistribution.singleton(new StackState(input));

        var perturbed = ExactRandomSemantics1201.enchantmentLevelPerturbation(
                level, enchantability, maxStates);
        LinkedHashMap<Outcome<StackState>, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> adjusted
                : perturbed.distribution().masses().entrySet()) {
            RandomTraceDistribution<List<SelectedEnchantment>> selected = selectEnchantments(
                    input, adjusted.getKey(), treasure, maxStates);
            for (Map.Entry<Outcome<List<SelectedEnchantment>>, ExactProbability> branch
                    : selected.masses().entrySet()) {
                ArrayList<RandomCall> calls = new ArrayList<>(perturbed.calls());
                calls.addAll(branch.getKey().calls());
                ItemStack output = apply(input, branch.getKey().value());
                result.merge(
                        new Outcome<>(new StackState(output), calls),
                        adjusted.getValue().multiply(branch.getValue()),
                        ExactProbability::add);
                if (result.size() > maxStates) {
                    throw new ExactRandomSemantics1201.StateSpaceLimitException(
                            result.size(), maxStates);
                }
            }
        }
        return RandomTraceDistribution.of(result);
    }

    private static RandomTraceDistribution<List<SelectedEnchantment>> selectEnchantments(
            ItemStack stack, int level, boolean treasure, int maxStates) {
        List<EnchantmentInstance> available =
                EnchantmentHelper.getAvailableEnchantmentResults(level, stack, treasure);
        if (available.isEmpty()) return RandomTraceDistribution.singleton(List.of());

        LinkedHashMap<Outcome<List<SelectedEnchantment>>, ExactProbability> result =
                new LinkedHashMap<>();
        for (WeightedChoice first : weightedChoices(available)) {
            List<SelectedEnchantment> chosen = List.of(SelectedEnchantment.of(first.value()));
            continueSelection(
                    available,
                    chosen,
                    level,
                    List.of(new RandomCall(RandomMethod.NEXT_INT_BOUND, first.totalWeight())),
                    first.mass(),
                    result,
                    maxStates);
        }
        return RandomTraceDistribution.of(result);
    }

    private static void continueSelection(
            List<EnchantmentInstance> available,
            List<SelectedEnchantment> chosen,
            int level,
            List<RandomCall> calls,
            ExactProbability mass,
            Map<Outcome<List<SelectedEnchantment>>, ExactProbability> result,
            int maxStates) {
        ArrayList<RandomCall> afterCheck = new ArrayList<>(calls);
        afterCheck.add(new RandomCall(RandomMethod.NEXT_INT_BOUND, 50));
        int successfulValues = Math.max(0, Math.min(50, level + 1));
        ExactProbability continues = ExactProbability.of(successfulValues, 50);
        ExactProbability stops = ExactProbability.ONE.subtract(continues);
        if (!stops.isZero()) {
            merge(result, new Outcome<>(List.copyOf(chosen), afterCheck), mass.multiply(stops), maxStates);
        }
        if (continues.isZero()) return;

        SelectedEnchantment last = chosen.get(chosen.size() - 1);
        List<EnchantmentInstance> compatible = available.stream()
                .filter(candidate -> last.enchantment().isCompatibleWith(candidate.enchantment))
                .toList();
        if (compatible.isEmpty()) {
            merge(result, new Outcome<>(List.copyOf(chosen), afterCheck), mass.multiply(continues), maxStates);
            return;
        }
        for (WeightedChoice next : weightedChoices(compatible)) {
            ArrayList<SelectedEnchantment> nextChosen = new ArrayList<>(chosen);
            nextChosen.add(SelectedEnchantment.of(next.value()));
            ArrayList<RandomCall> nextCalls = new ArrayList<>(afterCheck);
            nextCalls.add(new RandomCall(RandomMethod.NEXT_INT_BOUND, next.totalWeight()));
            continueSelection(
                    compatible,
                    List.copyOf(nextChosen),
                    level / 2,
                    nextCalls,
                    mass.multiply(continues).multiply(next.mass()),
                    result,
                    maxStates);
        }
    }

    private static List<WeightedChoice> weightedChoices(List<EnchantmentInstance> candidates) {
        int total = candidates.stream().mapToInt(value -> value.getWeight().asInt()).sum();
        if (total <= 0) throw new IllegalArgumentException("zero enchantment weight");
        ArrayList<WeightedChoice> result = new ArrayList<>(candidates.size());
        for (EnchantmentInstance candidate : candidates) {
            int weight = candidate.getWeight().asInt();
            if (weight > 0) {
                result.add(new WeightedChoice(
                        candidate, ExactProbability.of(weight, total), total));
            }
        }
        return List.copyOf(result);
    }

    private static void merge(
            Map<Outcome<List<SelectedEnchantment>>, ExactProbability> result,
            Outcome<List<SelectedEnchantment>> outcome,
            ExactProbability mass,
            int maxStates) {
        result.merge(outcome, mass, ExactProbability::add);
        if (result.size() > maxStates) {
            throw new ExactRandomSemantics1201.StateSpaceLimitException(result.size(), maxStates);
        }
    }

    private static ItemStack apply(ItemStack input, List<SelectedEnchantment> enchantments) {
        boolean book = input.is(Items.BOOK);
        ItemStack output = book ? new ItemStack(Items.ENCHANTED_BOOK) : input.copy();
        for (SelectedEnchantment enchantment : enchantments) {
            EnchantmentInstance instance = enchantment.toInstance();
            if (book) EnchantedBookItem.addEnchantment(output, instance);
            else output.enchant(enchantment.enchantment(), enchantment.level());
        }
        return output;
    }

    private record SelectedEnchantment(
            net.minecraft.world.item.enchantment.Enchantment enchantment, int level) {
        private static SelectedEnchantment of(EnchantmentInstance instance) {
            return new SelectedEnchantment(instance.enchantment, instance.level);
        }

        private EnchantmentInstance toInstance() {
            return new EnchantmentInstance(enchantment, level);
        }
    }

    private record WeightedChoice(
            EnchantmentInstance value, ExactProbability mass, int totalWeight) {}
}
