package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * Immutable expected-occurrence measure over terminal valuation equivalence classes.
 *
 * <p>Unlike a {@link FiniteDistribution}, this measure is not normalized. Its coefficients are
 * expected stack occurrences, while stack count remains solely in {@link TerminalStackKey}.
 */
public final class TerminalStackMeasure {
    private static final TerminalStackMeasure EMPTY = new TerminalStackMeasure(Map.of());

    private final Map<TerminalStackKey, ExactProbability> values;

    private TerminalStackMeasure(Map<TerminalStackKey, ExactProbability> source) {
        LinkedHashMap<TerminalStackKey, ExactProbability> copied = new LinkedHashMap<>();
        source.forEach(
                (key, mass) -> {
                    Objects.requireNonNull(key, "terminal stack key");
                    Objects.requireNonNull(mass, "expected occurrences");
                    if (!mass.isZero()) {
                        copied.merge(key, mass, ExactProbability::add);
                    }
                });
        values = Collections.unmodifiableMap(copied);
    }

    public static TerminalStackMeasure empty() {
        return EMPTY;
    }

    public static TerminalStackMeasure of(
            Map<TerminalStackKey, ExactProbability> expectedOccurrences) {
        Objects.requireNonNull(expectedOccurrences, "expectedOccurrences");
        if (expectedOccurrences.isEmpty()) {
            return EMPTY;
        }
        return new TerminalStackMeasure(expectedOccurrences);
    }

    /**
     * Exactly projects a completed full-stack measure onto terminal valuation equivalence classes.
     * No probability is rounded or normalized.
     */
    public static TerminalStackMeasure from(StackMeasure completedMeasure) {
        Objects.requireNonNull(completedMeasure, "completedMeasure");
        LinkedHashMap<TerminalStackKey, ExactProbability> projected = new LinkedHashMap<>();
        completedMeasure
                .values()
                .forEach(
                        (state, mass) ->
                                projected.merge(
                                        TerminalStackKey.from(state), mass, ExactProbability::add));
        return of(projected);
    }

    public Map<TerminalStackKey, ExactProbability> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Returns a new measure containing the exact sum of both occurrence measures. */
    public TerminalStackMeasure plus(TerminalStackMeasure other) {
        Objects.requireNonNull(other, "other");
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        LinkedHashMap<TerminalStackKey, ExactProbability> combined = new LinkedHashMap<>(values);
        other.values.forEach((key, mass) -> combined.merge(key, mass, ExactProbability::add));
        return of(combined);
    }

    /** Returns a new measure with every expected-occurrence coefficient scaled exactly. */
    public TerminalStackMeasure scale(ExactProbability multiplier) {
        Objects.requireNonNull(multiplier, "multiplier");
        if (multiplier.isZero() || isEmpty()) return EMPTY;
        if (ExactProbability.ONE.equals(multiplier)) return this;
        LinkedHashMap<TerminalStackKey, ExactProbability> scaled = new LinkedHashMap<>();
        values.forEach((key, mass) -> scaled.put(key, mass.multiply(multiplier)));
        return of(scaled);
    }

    /**
     * Exact expected item quantity. Negative terminal counts cannot be represented by the
     * non-negative {@link ExactProbability} type and are rejected by this exact convenience API.
     */
    public ExactProbability exactItemCount(Item item) {
        Objects.requireNonNull(item, "item");
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<TerminalStackKey, ExactProbability> entry : values.entrySet()) {
            TerminalStackKey key = entry.getKey();
            if (key.item() == item) {
                total = total.add(weightedMass(entry.getValue(), key.count(), "stack count"));
            }
        }
        return total;
    }

    /** Expected item quantity converted to double only at the terminal aggregation boundary. */
    public double itemCountAsDouble(Item item) {
        Objects.requireNonNull(item, "item");
        return exactItemCount(item).finiteDoubleValue();
    }

    /**
     * Exact rarity-weighted value for non-negative integral rarity multipliers, including the
     * configured vanilla-style 1/10/50/100 multipliers.
     */
    public ExactProbability exactRarityWeightedValue(ToLongFunction<Rarity> rarityMultiplier) {
        Objects.requireNonNull(rarityMultiplier, "rarityMultiplier");
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<TerminalStackKey, ExactProbability> entry : values.entrySet()) {
            TerminalStackKey key = entry.getKey();
            ExactProbability counted = weightedMass(entry.getValue(), key.count(), "stack count");
            long multiplier = rarityMultiplier.applyAsLong(key.rarity());
            total = total.add(weightedMass(counted, multiplier, "rarity multiplier"));
        }
        return total;
    }

    /** Rarity-weighted value converted to double for existing configuration APIs. */
    public double rarityWeightedValueAsDouble(ToDoubleFunction<Rarity> rarityMultiplier) {
        return exactRarityWeightedValueFromDouble(rarityMultiplier).finiteDoubleValue();
    }

    /**
     * Computes rarity-weighted value exactly when the configured multiplier is exposed as a finite,
     * non-negative Java double. Each multiplier is converted to its exact IEEE-754 rational value
     * before any occurrence masses are added.
     */
    public ExactProbability exactRarityWeightedValueFromDouble(
            ToDoubleFunction<Rarity> rarityMultiplier) {
        Objects.requireNonNull(rarityMultiplier, "rarityMultiplier");
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<TerminalStackKey, ExactProbability> entry : values.entrySet()) {
            TerminalStackKey key = entry.getKey();
            double multiplier = rarityMultiplier.applyAsDouble(key.rarity());
            if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
                throw new IllegalArgumentException(
                        "rarity multiplier must be finite and non-negative: " + multiplier);
            }
            ExactProbability counted = weightedMass(entry.getValue(), key.count(), "stack count");
            total = total.add(counted.multiply(ExactProbability.fromDouble(multiplier)));
        }
        return total;
    }

    private static ExactProbability weightedMass(
            ExactProbability mass, long factor, String factorName) {
        if (factor < 0L) {
            throw new IllegalStateException(factorName + " must be non-negative: " + factor);
        }
        return mass.multiply(ExactProbability.of(BigInteger.valueOf(factor), BigInteger.ONE));
    }
}
