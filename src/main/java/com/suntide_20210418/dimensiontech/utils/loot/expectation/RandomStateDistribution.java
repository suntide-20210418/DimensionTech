package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Finite PMF over a value and its exact Minecraft Xoroshiro continuation state. */
public final class RandomStateDistribution<T> {
    private final FiniteDistribution<State<T>> distribution;

    private RandomStateDistribution(FiniteDistribution<State<T>> distribution) {
        this.distribution = distribution;
    }

    public static <T> RandomStateDistribution<T> singleton(
            T value, XoroshiroState1201 randomState) {
        return of(Map.of(new State<>(value, randomState), ExactProbability.ONE));
    }

    public static <T> RandomStateDistribution<T> of(Map<State<T>, ExactProbability> masses) {
        return new RandomStateDistribution<>(FiniteDistribution.of(masses));
    }

    public Map<State<T>, ExactProbability> masses() {
        return distribution.masses();
    }

    public <R> RandomStateDistribution<R> flatMap(
            Function<State<T>, RandomStateDistribution<R>> transition, int maxStates) {
        Objects.requireNonNull(transition, "transition");
        LinkedHashMap<State<R>, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<State<T>, ExactProbability> prior : masses().entrySet()) {
            RandomStateDistribution<R> next = transition.apply(prior.getKey());
            for (Map.Entry<State<R>, ExactProbability> branch : next.masses().entrySet()) {
                result.merge(
                        branch.getKey(),
                        prior.getValue().multiply(branch.getValue()),
                        ExactProbability::add);
                if (result.size() > maxStates) {
                    throw new ExactRandomSemantics1201.StateSpaceLimitException(
                            result.size(), maxStates);
                }
            }
        }
        return of(result);
    }

    public FiniteDistribution<T> marginal() {
        LinkedHashMap<T, ExactProbability> result = new LinkedHashMap<>();
        masses().forEach((state, mass) -> result.merge(state.value(), mass, ExactProbability::add));
        return FiniteDistribution.of(result);
    }

    public record State<T>(T value, XoroshiroState1201 randomState) {}
}
