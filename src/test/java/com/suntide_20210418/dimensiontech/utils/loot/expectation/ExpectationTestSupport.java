package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ExpectationTestSupport {
    private ExpectationTestSupport() {}

    static int constantIntValue(JsonElement element) {
        return Math.round(element.getAsFloat());
    }

    static <T> Map<T, ExactProbability> scaleStates(
            Map<T, ExactProbability> states, ExactProbability multiplier) {
        Map<T, ExactProbability> result = new LinkedHashMap<>();
        if (multiplier.isZero()) return result;
        states.forEach((state, mass) -> result.put(state, mass.multiply(multiplier)));
        return result;
    }

    static <T> void mergeStates(
            Map<T, ExactProbability> destination, Map<T, ExactProbability> source) {
        source.forEach((state, mass) -> destination.merge(state, mass, ExactProbability::add));
    }

    static List<ExactProbability> positiveRollInboundMasses(
            Map<Integer, ExactProbability> distribution) {
        int maximum = distribution.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maximum <= 0) return List.of();
        List<ExactProbability> result = new ArrayList<>(maximum);
        for (int rollIndex = 0; rollIndex < maximum; rollIndex++) {
            ExactProbability inbound = ExactProbability.ZERO;
            for (Map.Entry<Integer, ExactProbability> outcome : distribution.entrySet()) {
                if (outcome.getKey() > rollIndex) inbound = inbound.add(outcome.getValue());
            }
            if (!inbound.isZero()) result.add(inbound);
        }
        return List.copyOf(result);
    }

    static <T> FiniteDistribution<List<T>> expandCandidateLists(
            List<T> entries, List<ExactProbability> inclusionProbabilities, int maxStates) {
        return expandCandidateGroups(
                entries.stream().map(List::of).toList(), inclusionProbabilities, maxStates);
    }

    static <T> FiniteDistribution<List<T>> expandCandidateGroups(
            List<List<T>> groups, List<ExactProbability> inclusionProbabilities, int maxStates) {
        if (groups.size() != inclusionProbabilities.size()) {
            throw new IllegalArgumentException("candidate group/probability size mismatch");
        }
        Map<List<T>, ExactProbability> states = new LinkedHashMap<>();
        states.put(List.of(), ExactProbability.ONE);
        for (int entryIndex = 0; entryIndex < groups.size(); entryIndex++) {
            List<T> group = List.copyOf(groups.get(entryIndex));
            ExactProbability included = inclusionProbabilities.get(entryIndex);
            ExactProbability excluded = ExactProbability.ONE.subtract(included);
            Map<List<T>, ExactProbability> next = new LinkedHashMap<>();
            for (Map.Entry<List<T>, ExactProbability> state : states.entrySet()) {
                if (!excluded.isZero()) {
                    next.merge(
                            state.getKey(),
                            state.getValue().multiply(excluded),
                            ExactProbability::add);
                }
                if (!included.isZero()) {
                    List<T> candidates = new ArrayList<>(state.getKey());
                    candidates.addAll(group);
                    next.merge(
                            List.copyOf(candidates),
                            state.getValue().multiply(included),
                            ExactProbability::add);
                }
            }
            if (next.size() > maxStates) {
                throw new ExactRandomSemantics1201.StateSpaceLimitException(next.size(), maxStates);
            }
            states = next;
        }
        return FiniteDistribution.of(states);
    }
}
