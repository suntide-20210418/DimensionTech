package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Ordered deterministic execution helpers over an immutable 1.20.1 random state. */
public final class RandomStateExecutor1211 {
    private RandomStateExecutor1211() {}

    public static <T> SequenceResult<T> repeat(
            int count,
            XoroshiroState1211 initialState,
            Function<XoroshiroState1211, StepResult<T>> step) {
        if (count < 0) throw new IllegalArgumentException("negative repeat count");
        XoroshiroState1211 state = initialState;
        ArrayList<T> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            StepResult<T> result = step.apply(state);
            values.add(result.value());
            state = result.randomState();
        }
        return new SequenceResult<>(List.copyOf(values), state);
    }

    public record StepResult<T>(T value, XoroshiroState1211 randomState) {}

    public record SequenceResult<T>(List<T> values, XoroshiroState1211 randomState) {}
}
