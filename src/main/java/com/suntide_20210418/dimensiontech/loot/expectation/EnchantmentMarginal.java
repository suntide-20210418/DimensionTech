package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Expected occurrence counts per {@link EnchantmentKey}, deliberately not normalized.
 *
 * <p>This is a side channel of the loot-table analysis. It answers "how many times does the table
 * select enchantment E at level L", which the terminal valuation layer drops on purpose because
 * {@link TerminalStackKey} only keeps item, count and rarity. The main analysis never depends on
 * it: a marginal that cannot be computed exactly degrades to {@link #EMPTY} instead of turning an
 * {@link AnalysisStatus#EXACT} table into {@link AnalysisStatus#UNSUPPORTED}.
 */
public record EnchantmentMarginal(Map<EnchantmentKey, ExactProbability> values) {
    public static final EnchantmentMarginal EMPTY = new EnchantmentMarginal(Map.of());

    public EnchantmentMarginal {
        Map<EnchantmentKey, ExactProbability> copy = new LinkedHashMap<>();
        values.forEach(
                (key, mass) -> {
                    Objects.requireNonNull(key, "enchantment key");
                    Objects.requireNonNull(mass, "mass");
                    if (!mass.isZero()) {
                        copy.put(key, mass);
                    }
                });
        values = Collections.unmodifiableMap(copy);
    }

    public static EnchantmentMarginal of(Map<EnchantmentKey, ExactProbability> values) {
        return new EnchantmentMarginal(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Adds another marginal, keeping exact rational arithmetic. */
    public EnchantmentMarginal plus(EnchantmentMarginal other) {
        if (values.isEmpty()) return other;
        if (other.values.isEmpty()) return this;
        Map<EnchantmentKey, ExactProbability> merged = new LinkedHashMap<>(values);
        other.values.forEach((key, mass) -> merged.merge(key, mass, ExactProbability::add));
        return new EnchantmentMarginal(merged);
    }

    /** Scales every expected count by {@code factor}. */
    public EnchantmentMarginal scale(ExactProbability factor) {
        if (values.isEmpty() || factor.isZero()) return EMPTY;
        Map<EnchantmentKey, ExactProbability> scaled = new LinkedHashMap<>();
        values.forEach((key, mass) -> scaled.put(key, mass.multiply(factor)));
        return new EnchantmentMarginal(scaled);
    }

    /**
     * Scales by {@code factor}, saturating at {@link Integer#MAX_VALUE} to protect later merges.
     */
    public EnchantmentMarginal scaleSaturating(ExactProbability factor) {
        if (values.isEmpty() || factor.isZero()) return EMPTY;
        Map<EnchantmentKey, ExactProbability> scaled = new LinkedHashMap<>();
        values.forEach(
                (key, mass) -> {
                    ExactProbability product = mass.multiply(factor);
                    scaled.put(
                            key,
                            product.compareTo(ExactProbability.of(Integer.MAX_VALUE, 1)) > 0
                                    ? ExactProbability.of(Integer.MAX_VALUE, 1)
                                    : product);
                });
        return new EnchantmentMarginal(scaled);
    }
}
