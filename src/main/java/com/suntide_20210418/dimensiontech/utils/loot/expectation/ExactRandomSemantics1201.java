package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finite marginal semantics of Minecraft 1.20.1 RandomSource calls. */
public final class ExactRandomSemantics1201 {
    private static final BigInteger FLOAT_OUTCOMES = BigInteger.ONE.shiftLeft(24);
    private static final int FLOAT_OUTCOME_COUNT = 1 << 24;
    private static final BigInteger DOUBLE_OUTCOMES = BigInteger.ONE.shiftLeft(53);

    private ExactRandomSemantics1201() {}

    public static RandomResult<Integer> nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        LinkedHashMap<Integer, ExactProbability> masses = new LinkedHashMap<>();
        ExactProbability mass = ExactProbability.of(1, bound);
        for (int value = 0; value < bound; value++) masses.put(value, mass);
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_INT_BOUND, bound)));
    }

    /** LootPool.addRandomItem selection, including its one-candidate no-call fast path. */
    public static RandomResult<Integer> weightedIndex(List<Integer> weights) {
        if (weights.isEmpty()) throw new IllegalArgumentException("empty candidate list");
        long total = 0L;
        for (int index = 0; index < weights.size(); index++) {
            int weight = weights.get(index);
            if (weight <= 0) {
                throw new IllegalArgumentException(
                        "LootPool candidate weights must be positive after expand filtering");
            }
            total += weight;
            if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("weight overflow");
        }
        if (weights.size() == 1) {
            return new RandomResult<>(FiniteDistribution.singleton(0), List.of());
        }
        LinkedHashMap<Integer, ExactProbability> masses = new LinkedHashMap<>();
        for (int index = 0; index < weights.size(); index++) {
            int weight = weights.get(index);
            masses.put(index, ExactProbability.of(weight, total));
        }
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_INT_BOUND, (int) total)));
    }

    public static RandomResult<Integer> uniformIntInclusive(int min, int max) {
        long size = (long) max - min + 1L;
        if (size <= 0L || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid or unsupported inclusive range");
        }
        RandomResult<Integer> bounded = nextInt((int) size);
        LinkedHashMap<Integer, ExactProbability> shifted = new LinkedHashMap<>();
        bounded.distribution().masses().forEach((value, mass) -> shifted.put(min + value, mass));
        return new RandomResult<>(FiniteDistribution.of(shifted), bounded.calls());
    }

    public static RandomResult<Boolean> nextFloatLessThan(float threshold) {
        BigInteger successes;
        if (Float.isNaN(threshold) || threshold <= 0.0F) {
            successes = BigInteger.ZERO;
        } else if (threshold > 1.0F) {
            successes = FLOAT_OUTCOMES;
        } else {
            ExactProbability exact = ExactProbability.fromFloat(threshold);
            BigInteger scaledNumerator = exact.numerator().multiply(FLOAT_OUTCOMES);
            successes = ceilDivide(scaledNumerator, exact.denominator());
            if (successes.compareTo(FLOAT_OUTCOMES) > 0) successes = FLOAT_OUTCOMES;
        }
        ExactProbability success = ExactProbability.of(successes, FLOAT_OUTCOMES);
        ExactProbability failure = ExactProbability.of(
                FLOAT_OUTCOMES.subtract(successes), FLOAT_OUTCOMES);
        Map<Boolean, ExactProbability> masses = new LinkedHashMap<>();
        if (!failure.isZero()) masses.put(false, failure);
        if (!success.isZero()) masses.put(true, success);
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_FLOAT, 0)));
    }

    public static RandomResult<Boolean> nextDoubleLessThan(double threshold) {
        BigInteger successes;
        if (Double.isNaN(threshold) || threshold <= 0.0D) {
            successes = BigInteger.ZERO;
        } else if (threshold > 1.0D) {
            successes = DOUBLE_OUTCOMES;
        } else {
            ExactProbability exact = ExactProbability.fromDouble(threshold);
            successes = ceilDivide(
                    exact.numerator().multiply(DOUBLE_OUTCOMES), exact.denominator());
            if (successes.compareTo(DOUBLE_OUTCOMES) > 0) successes = DOUBLE_OUTCOMES;
        }
        Map<Boolean, ExactProbability> masses = booleanMasses(successes, DOUBLE_OUTCOMES);
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_DOUBLE, 0)));
    }

    public static RandomResult<Boolean> nextBoolean() {
        return new RandomResult<>(
                FiniteDistribution.of(Map.of(
                        false, ExactProbability.of(1, 2),
                        true, ExactProbability.of(1, 2))),
                List.of(new RandomCall(RandomMethod.NEXT_BOOLEAN, 0)));
    }

    public static <T> RandomResult<List<T>> shuffle(List<T> input, int maxStates) {
        List<State<T>> states = List.of(new State<>(List.copyOf(input), ExactProbability.ONE));
        List<RandomCall> calls = new ArrayList<>();
        for (int index = input.size(); index > 1; index--) {
            int bound = index;
            ExactProbability branch = ExactProbability.of(1, bound);
            List<State<T>> next = new ArrayList<>();
            for (State<T> state : states) {
                for (int selected = 0; selected < bound; selected++) {
                    ArrayList<T> values = new ArrayList<>(state.value());
                    java.util.Collections.swap(values, bound - 1, selected);
                    next.add(new State<>(List.copyOf(values), state.mass().multiply(branch)));
                    if (next.size() > maxStates) {
                        throw new StateSpaceLimitException(next.size(), maxStates);
                    }
                }
            }
            states = next;
            calls.add(new RandomCall(RandomMethod.NEXT_INT_BOUND, bound));
        }
        LinkedHashMap<List<T>, ExactProbability> masses = new LinkedHashMap<>();
        states.forEach(state -> masses.merge(state.value(), state.mass(), ExactProbability::add));
        return new RandomResult<>(FiniteDistribution.of(masses), List.copyOf(calls));
    }

    public static RandomResult<Integer> binomial(int trials, float probability) {
        if (trials < 0) throw new IllegalArgumentException("negative trial count");
        RandomResult<Boolean> trial = nextFloatLessThan(probability);
        ExactProbability success = trial.distribution().masses()
                .getOrDefault(true, ExactProbability.ZERO);
        ExactProbability failure = trial.distribution().masses()
                .getOrDefault(false, ExactProbability.ZERO);
        LinkedHashMap<Integer, ExactProbability> current = new LinkedHashMap<>();
        current.put(0, ExactProbability.ONE);
        for (int index = 0; index < trials; index++) {
            LinkedHashMap<Integer, ExactProbability> next = new LinkedHashMap<>();
            current.forEach((count, mass) -> {
                if (!failure.isZero()) {
                    next.merge(count, mass.multiply(failure), ExactProbability::add);
                }
                if (!success.isZero()) {
                    next.merge(count + 1, mass.multiply(success), ExactProbability::add);
                }
            });
            current = next;
        }
        List<RandomCall> calls = new ArrayList<>(trials);
        for (int index = 0; index < trials; index++) {
            calls.add(new RandomCall(RandomMethod.NEXT_FLOAT, 0));
        }
        return new RandomResult<>(FiniteDistribution.of(current), List.copyOf(calls));
    }

    /**
     * Exact distribution of EnchantmentHelper's two-nextFloat level perturbation.
     * Iterates the first 24-bit output and binary-searches contiguous buckets of the second.
     */
    public static RandomResult<Integer> enchantmentLevelPerturbation(
            int level, int enchantability, int maxDistinctStates) {
        if (enchantability <= 0) {
            return new RandomResult<>(FiniteDistribution.singleton(level), List.of());
        }
        int bound = enchantability / 4 + 1;
        LinkedHashMap<Integer, ExactProbability> adjustedLevels = new LinkedHashMap<>();
        ExactProbability intBranch = ExactProbability.of(1, (long) bound * bound);
        for (int firstInt = 0; firstInt < bound; firstInt++) {
            for (int secondInt = 0; secondInt < bound; secondInt++) {
                int baseLevel = level + 1 + firstInt + secondInt;
                Map<Integer, Long> counts = perturbationFloatPairCounts(
                        baseLevel, maxDistinctStates);
                for (Map.Entry<Integer, Long> outcome : counts.entrySet()) {
                    ExactProbability floatBranch = ExactProbability.of(
                            BigInteger.valueOf(outcome.getValue()), FLOAT_OUTCOMES.multiply(FLOAT_OUTCOMES));
                    adjustedLevels.merge(
                            outcome.getKey(), intBranch.multiply(floatBranch), ExactProbability::add);
                }
            }
        }
        return new RandomResult<>(
                FiniteDistribution.of(adjustedLevels),
                List.of(
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, bound),
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, bound),
                        new RandomCall(RandomMethod.NEXT_FLOAT, 0),
                        new RandomCall(RandomMethod.NEXT_FLOAT, 0)));
    }

    private static Map<Integer, Long> perturbationFloatPairCounts(
            int baseLevel, int maxDistinctStates) {
        LinkedHashMap<Integer, Long> counts = new LinkedHashMap<>();
        int maximumSum = 2 * (FLOAT_OUTCOME_COUNT - 1);
        int start = 0;
        while (start <= maximumSum) {
            int value = perturbedLevelFromFloatBitSum(baseLevel, start);
            int low = start;
            int high = maximumSum;
            while (low < high) {
                int middle = low + ((high - low + 1) >>> 1);
                if (perturbedLevelFromFloatBitSum(baseLevel, middle) == value) low = middle;
                else high = middle - 1;
            }
            long pairCount = cumulativeFloatPairCount(low, maximumSum)
                    - cumulativeFloatPairCount(start - 1, maximumSum);
            counts.merge(value, pairCount, Long::sum);
            if (counts.size() > maxDistinctStates) {
                throw new StateSpaceLimitException(counts.size(), maxDistinctStates);
            }
            start = low + 1;
        }
        return counts;
    }

    private static int perturbedLevelFromFloatBitSum(int baseLevel, int sum) {
        float roundedSum = (float) sum * 0x1.0p-24F;
        float adjustment = (roundedSum - 1.0F) * 0.15F;
        return net.minecraft.util.Mth.clamp(
                Math.round((float) baseLevel + (float) baseLevel * adjustment),
                1,
                Integer.MAX_VALUE);
    }

    private static long cumulativeFloatPairCount(int sum, int maximumSum) {
        if (sum < 0) return 0L;
        int last = Math.min(sum, maximumSum);
        if (last < FLOAT_OUTCOME_COUNT) {
            long count = (long) last + 1L;
            return count * (count + 1L) / 2L;
        }
        long firstHalf = (long) FLOAT_OUTCOME_COUNT * (FLOAT_OUTCOME_COUNT + 1L) / 2L;
        long tailLength = (long) last - FLOAT_OUTCOME_COUNT + 1L;
        long firstTail = FLOAT_OUTCOME_COUNT - 1L;
        long lastTail = (long) maximumSum - last + 1L;
        return firstHalf + tailLength * (firstTail + lastTail) / 2L;
    }

    /**
     * Exact Mth.nextFloat(random,min,max), followed by Java float multiplication and Mth.floor.
     * Every one of the 2^24 nextFloat outputs is evaluated; this is exhaustive, not sampling.
     */
    public static RandomResult<Integer> uniformFloatTimesLuckFloor(
            float min, float max, float luck, int maxDistinctStates) {
        if (!Float.isFinite(min) || !Float.isFinite(max) || !Float.isFinite(luck)) {
            throw new IllegalArgumentException("non-finite float input");
        }
        if (luck == 0.0F || min >= max) {
            int value = net.minecraft.util.Mth.floor(min * luck);
            return new RandomResult<>(FiniteDistribution.singleton(value), List.of());
        }
        LinkedHashMap<Integer, Long> counts = new LinkedHashMap<>();
        float width = max - min;
        for (int bits = 0; bits < FLOAT_OUTCOME_COUNT; bits++) {
            float random = bits * 0x1.0p-24F;
            float sampled = random * width + min;
            int value = net.minecraft.util.Mth.floor(sampled * luck);
            counts.merge(value, 1L, Long::sum);
            if (counts.size() > maxDistinctStates) {
                throw new StateSpaceLimitException(counts.size(), maxDistinctStates);
            }
        }
        LinkedHashMap<Integer, ExactProbability> masses = new LinkedHashMap<>();
        counts.forEach((value, count) -> masses.put(
                value, ExactProbability.of(BigInteger.valueOf(count), FLOAT_OUTCOMES)));
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_FLOAT, 0)));
    }

    /**
     * Exact SetItemDamageFunction result for a UniformGenerator backed by constants.
     * The provider uses Mth.nextFloat and all arithmetic below deliberately remains float.
     */
    public static RandomResult<Integer> uniformFloatSetDamage(
            float min,
            float max,
            boolean add,
            int currentDamage,
            int maxDamage,
            int maxDistinctStates) {
        if (!Float.isFinite(min) || !Float.isFinite(max) || maxDamage <= 0) {
            throw new IllegalArgumentException("invalid set_damage input");
        }
        float base = add ? 1.0F - (float) currentDamage / (float) maxDamage : 0.0F;
        if (min >= max) {
            return new RandomResult<>(
                    FiniteDistribution.singleton(setDamageValue(min, base, maxDamage)), List.of());
        }
        LinkedHashMap<Integer, Long> counts = new LinkedHashMap<>();
        float width = max - min;
        for (int bits = 0; bits < FLOAT_OUTCOME_COUNT; bits++) {
            float random = bits * 0x1.0p-24F;
            float sampled = random * width + min;
            int value = setDamageValue(sampled, base, maxDamage);
            counts.merge(value, 1L, Long::sum);
            if (counts.size() > maxDistinctStates) {
                throw new StateSpaceLimitException(counts.size(), maxDistinctStates);
            }
        }
        LinkedHashMap<Integer, ExactProbability> masses = new LinkedHashMap<>();
        counts.forEach((value, count) -> masses.put(
                value, ExactProbability.of(BigInteger.valueOf(count), FLOAT_OUTCOMES)));
        return new RandomResult<>(
                FiniteDistribution.of(masses),
                List.of(new RandomCall(RandomMethod.NEXT_FLOAT, 0)));
    }

    public static int setDamageValue(
            float providerValue, boolean add, int currentDamage, int maxDamage) {
        if (!Float.isFinite(providerValue) || maxDamage <= 0) {
            throw new IllegalArgumentException("invalid set_damage input");
        }
        float base = add ? 1.0F - (float) currentDamage / (float) maxDamage : 0.0F;
        return setDamageValue(providerValue, base, maxDamage);
    }

    private static int setDamageValue(float providerValue, float base, int maxDamage) {
        float remaining = net.minecraft.util.Mth.clamp(providerValue + base, 0.0F, 1.0F);
        return net.minecraft.util.Mth.floor((1.0F - remaining) * (float) maxDamage);
    }

    private static BigInteger ceilDivide(BigInteger numerator, BigInteger denominator) {
        BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(denominator);
        return quotientAndRemainder[1].signum() == 0
                ? quotientAndRemainder[0]
                : quotientAndRemainder[0].add(BigInteger.ONE);
    }

    private static Map<Boolean, ExactProbability> booleanMasses(
            BigInteger successes, BigInteger outcomes) {
        LinkedHashMap<Boolean, ExactProbability> masses = new LinkedHashMap<>();
        BigInteger failures = outcomes.subtract(successes);
        if (failures.signum() > 0) {
            masses.put(false, ExactProbability.of(failures, outcomes));
        }
        if (successes.signum() > 0) {
            masses.put(true, ExactProbability.of(successes, outcomes));
        }
        return masses;
    }

    public enum RandomMethod {
        NEXT_INT_BOUND,
        NEXT_FLOAT,
        NEXT_DOUBLE,
        NEXT_BOOLEAN,
        SHUFFLE
    }

    public record RandomCall(RandomMethod method, int bound) {}

    public record RandomResult<T>(
            FiniteDistribution<T> distribution, List<RandomCall> calls) {
        public RandomResult {
            calls = List.copyOf(calls);
        }
    }

    private record State<T>(List<T> value, ExactProbability mass) {}

    public static final class StateSpaceLimitException extends RuntimeException {
        public StateSpaceLimitException(int actual, int limit) {
            super("Random state space " + actual + " exceeds limit " + limit);
        }
    }
}
