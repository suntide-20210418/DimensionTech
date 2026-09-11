package com.suntide_20210418.dimensiontech.mythiccrucible;

import java.util.Objects;

/**
 * A state and the one operation item that settles it.
 *
 * @param <S> the supplied stack type, an {@code ItemStack} in production
 */
public record StateStep<S>(StateId state, OperationMatcher<S> operation) {
    public StateStep {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(operation, "operation");
        if (operation.isEmpty()) throw new IllegalArgumentException("operation must not be empty");
    }
}
