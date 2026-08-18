package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Expected occurrence measure after all stack-inspecting functions have completed. */
public final class StackObservationMeasure {
    private final Map<StackObservation, ExactProbability> values = new LinkedHashMap<>();

    public void add(StackObservation observation, ExactProbability expectedOccurrences) {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(expectedOccurrences, "expectedOccurrences");
        if (!expectedOccurrences.isZero()) {
            values.merge(observation, expectedOccurrences, ExactProbability::add);
        }
    }

    public void addAll(StackObservationMeasure other, ExactProbability multiplier) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(multiplier, "multiplier");
        other.values.forEach((observation, value) -> add(observation, value.multiply(multiplier)));
    }

    public void addAll(StackMeasure other, ExactProbability multiplier) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(multiplier, "multiplier");
        other.values()
                .forEach(
                        (state, value) ->
                                add(
                                        StackObservation.observe(state.stack()),
                                        value.multiply(multiplier)));
    }

    public Map<StackObservation, ExactProbability> values() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public double itemCount(Item item) {
        Objects.requireNonNull(item, "item");
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<StackObservation, ExactProbability> entry : values.entrySet()) {
            StackObservation observation = entry.getKey();
            if (observation.item() == item) {
                if (observation.count() < 0) {
                    throw new IllegalStateException(
                            "stack count must be non-negative: " + observation.count());
                }
                total =
                        total.add(
                                entry.getValue()
                                        .multiply(ExactProbability.of(observation.count(), 1L)));
            }
        }
        return total.finiteDoubleValue();
    }

    public double weightedValue(ToDoubleFunction<Rarity> rarityMultiplier) {
        Objects.requireNonNull(rarityMultiplier, "rarityMultiplier");
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<StackObservation, ExactProbability> entry : values.entrySet()) {
            StackObservation observation = entry.getKey();
            if (observation.count() < 0) {
                throw new IllegalStateException(
                        "stack count must be non-negative: " + observation.count());
            }
            double multiplier = rarityMultiplier.applyAsDouble(observation.rarity());
            if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
                throw new IllegalArgumentException(
                        "rarity multiplier must be finite and non-negative: " + multiplier);
            }
            ExactProbability counted =
                    entry.getValue().multiply(ExactProbability.of(observation.count(), 1L));
            total = total.add(counted.multiply(ExactProbability.fromDouble(multiplier)));
        }
        return total.finiteDoubleValue();
    }
}
