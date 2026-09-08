package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A normalized finite probability mass function. */
public final class FiniteDistribution<T> {
    private final Map<T, ExactProbability> masses;

    private FiniteDistribution(Map<T, ExactProbability> masses) {
        if (masses.isEmpty()) {
            throw new IllegalArgumentException("empty PMF");
        }
        LinkedHashMap<T, ExactProbability> normalized = new LinkedHashMap<>();
        ExactProbability total = ExactProbability.ZERO;
        for (Map.Entry<T, ExactProbability> entry : masses.entrySet()) {
            ExactProbability mass = java.util.Objects.requireNonNull(entry.getValue(), "PMF mass");
            if (!mass.isZero()) {
                normalized.merge(entry.getKey(), mass, ExactProbability::add);
                total = total.add(mass);
            }
        }
        if (!total.equals(ExactProbability.ONE)) {
            throw new IllegalArgumentException("PMF mass must sum exactly to 1, got " + total);
        }
        this.masses = Collections.unmodifiableMap(normalized);
    }

    public static <T> FiniteDistribution<T> singleton(T value) {
        return of(Map.of(value, ExactProbability.ONE));
    }

    public static <T> FiniteDistribution<T> of(Map<T, ExactProbability> masses) {
        return new FiniteDistribution<>(masses);
    }

    public Map<T, ExactProbability> masses() {
        return masses;
    }
}
