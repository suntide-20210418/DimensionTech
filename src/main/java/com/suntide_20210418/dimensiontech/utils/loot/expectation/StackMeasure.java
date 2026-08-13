package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Expected occurrence measure of emitted stacks; intentionally not normalized. */
public final class StackMeasure {
    private final Map<StackState, ExactProbability> values = new LinkedHashMap<>();

    public void add(StackState state, ExactProbability expectedOccurrences) {
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(expectedOccurrences, "expectedOccurrences");
        if (!expectedOccurrences.isZero()) {
            values.merge(state, expectedOccurrences, ExactProbability::add);
        }
    }

    public void addAll(StackMeasure other, ExactProbability multiplier) {
        other.values.forEach((state, value) -> add(state, value.multiply(multiplier)));
    }

    public void replaceWith(StackMeasure other) {
        values.clear();
        values.putAll(other.values);
    }

    public Map<StackState, ExactProbability> values() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() { return values.isEmpty(); }
}
