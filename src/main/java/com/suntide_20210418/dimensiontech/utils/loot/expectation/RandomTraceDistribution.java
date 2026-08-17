package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactRandomSemantics1201.RandomCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A finite PMF whose outcomes retain their complete ordered RandomSource call trace.
 *
 * <p>{@link #flatMap} is the joint-state transition for the ideal finite probability space: the
 * next kernel may depend on the prior value, and its fresh RandomSource draw is independent only
 * after that dependency has been applied.
 */
public final class RandomTraceDistribution<T> {
    private final FiniteDistribution<Outcome<T>> distribution;

    private RandomTraceDistribution(FiniteDistribution<Outcome<T>> distribution) {
        this.distribution = distribution;
    }

    public static <T> RandomTraceDistribution<T> singleton(T value) {
        return of(Map.of(new Outcome<>(value, List.of()), ExactProbability.ONE));
    }

    public static <T> RandomTraceDistribution<T> of(Map<Outcome<T>, ExactProbability> masses) {
        return new RandomTraceDistribution<>(FiniteDistribution.of(masses));
    }

    public static <T> RandomTraceDistribution<T> fromRandomResult(
            ExactRandomSemantics1201.RandomResult<T> result) {
        LinkedHashMap<Outcome<T>, ExactProbability> masses = new LinkedHashMap<>();
        result.distribution()
                .masses()
                .forEach((value, mass) -> masses.put(new Outcome<>(value, result.calls()), mass));
        return of(masses);
    }

    public Map<Outcome<T>, ExactProbability> masses() {
        return distribution.masses();
    }

    public <R> RandomTraceDistribution<R> flatMap(
            Function<? super T, RandomTraceDistribution<R>> transition, int maxStates) {
        Objects.requireNonNull(transition, "transition");
        LinkedHashMap<Outcome<R>, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<Outcome<T>, ExactProbability> prior : masses().entrySet()) {
            RandomTraceDistribution<R> next = transition.apply(prior.getKey().value());
            for (Map.Entry<Outcome<R>, ExactProbability> branch : next.masses().entrySet()) {
                ArrayList<RandomCall> calls = new ArrayList<>(prior.getKey().calls());
                calls.addAll(branch.getKey().calls());
                Outcome<R> outcome = new Outcome<>(branch.getKey().value(), calls);
                result.merge(
                        outcome,
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
        masses().forEach(
                        (outcome, mass) ->
                                result.merge(outcome.value(), mass, ExactProbability::add));
        return FiniteDistribution.of(result);
    }

    public record Outcome<T>(T value, List<RandomCall> calls) {
        public Outcome {
            calls = List.copyOf(calls);
        }
    }
}
