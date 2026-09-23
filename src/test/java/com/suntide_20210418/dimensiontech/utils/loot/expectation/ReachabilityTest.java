package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201;
import com.suntide_20210418.dimensiontech.loot.expectation.FiniteDistribution;
import com.suntide_20210418.dimensiontech.loot.expectation.Reachability;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ReachabilityTest {
    @Test
    void zeroMassUnknownNodeIsNotVisited() {
        Map<String, ExactProbability> masses = new LinkedHashMap<>();
        masses.put("reachable", ExactProbability.ONE);
        masses.put("unknown", ExactProbability.ZERO);
        AtomicInteger visits = new AtomicInteger();

        Reachability.visitPositiveMass(
                FiniteDistribution.of(masses),
                (state, mass) -> {
                    if (state.equals("unknown")) Reachability.unsupported(state, mass);
                    visits.incrementAndGet();
                });

        assertEquals(1, visits.get());
    }

    @Test
    void everyPositiveUnknownMassFailsRegardlessOfHowSmallItIs() {
        ExactProbability tiny = ExactProbability.of(1, 1_000_000_000L);
        assertEquals(tiny, Reachability.unsupported("unknown", tiny).inboundMass());
        assertThrows(
                IllegalArgumentException.class,
                () -> Reachability.unsupported("unknown", ExactProbability.ZERO));
    }

    @Test
    void conditionalFunctionBranchesPreserveAllInboundMass() {
        String original = "original";
        String transformed = "transformed";
        Map<String, ExactProbability> inbound = Map.of(original, ExactProbability.of(7, 3));
        ExactProbability success = ExactProbability.of(1, 4);

        Map<String, ExactProbability> applied =
                ExpectationTestSupport.scaleStates(inbound, success);
        Map<String, ExactProbability> skipped =
                ExpectationTestSupport.scaleStates(inbound, ExactProbability.ONE.subtract(success));
        Map<String, ExactProbability> output = new LinkedHashMap<>();
        output.put(transformed, applied.get(original));
        ExpectationTestSupport.mergeStates(output, skipped);

        assertEquals(ExactProbability.of(7, 12), output.get(transformed));
        assertEquals(ExactProbability.of(7, 4), output.get(original));
        assertEquals(
                ExactProbability.of(7, 3),
                output.values().stream().reduce(ExactProbability.ZERO, ExactProbability::add));
    }

    @Test
    void rollDistributionProducesOneExactInboundMassPerReachableInvocation() {
        Map<Integer, ExactProbability> rolls = new LinkedHashMap<>();
        rolls.put(0, ExactProbability.of(1, 4));
        rolls.put(1, ExactProbability.of(1, 4));
        rolls.put(3, ExactProbability.of(1, 2));

        assertEquals(
                List.of(
                        ExactProbability.of(3, 4),
                        ExactProbability.of(1, 2),
                        ExactProbability.of(1, 2)),
                ExpectationTestSupport.positiveRollInboundMasses(rolls));
    }

    @Test
    void entryExpandPreservesOrderedCandidateListStatesAndStrictZeroBranches() {
        FiniteDistribution<List<String>> expanded =
                ExpectationTestSupport.expandCandidateLists(
                        List.of("always", "never", "coin"),
                        List.of(
                                ExactProbability.ONE,
                                ExactProbability.ZERO,
                                ExactProbability.of(1, 2)),
                        10);

        assertEquals(2, expanded.masses().size());
        assertEquals(ExactProbability.of(1, 2), expanded.masses().get(List.of("always")));
        assertEquals(ExactProbability.of(1, 2), expanded.masses().get(List.of("always", "coin")));
        assertEquals(null, expanded.masses().get(List.of("always", "never")));
    }

    @Test
    void entryExpandFailsWhenStateSpaceCannotProveBranches() {
        assertThrows(
                ExactRandomSemantics1201.StateSpaceLimitException.class,
                () ->
                        ExpectationTestSupport.expandCandidateLists(
                                List.of("a", "b"),
                                List.of(ExactProbability.of(1, 2), ExactProbability.of(1, 2)),
                                3));
    }

    @Test
    void oneSuccessfulEntryConditionCanExpandAnOrderedCandidateGroup() {
        FiniteDistribution<List<String>> expanded =
                ExpectationTestSupport.expandCandidateGroups(
                        List.of(List.of("tag-a", "tag-b"), List.of("plain")),
                        List.of(ExactProbability.of(1, 2), ExactProbability.ONE),
                        10);

        assertEquals(ExactProbability.of(1, 2), expanded.masses().get(List.of("plain")));
        assertEquals(
                ExactProbability.of(1, 2),
                expanded.masses().get(List.of("tag-a", "tag-b", "plain")));
    }
}
