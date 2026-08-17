package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Executes an ordered sequence of raw loot-table calls over one concrete 1.20.1 RNG stream.
 *
 * <p>A loot table call is a state transition, rather than an isolated expectation.  In
 * particular, the state returned by one root call is the state supplied to the next root call.
 * This class is intentionally separate from {@link StatefulLootTableExecutor1201}: the latter's
 * single-table API remains useful to callers that need one transition and its source-compatible
 * return type does not have to grow a sequence-level state distribution.
 */
public final class StatefulLootSequenceExecutor1201 {
    private StatefulLootSequenceExecutor1201() {}

    /**
     * Executes each root in list order and carries the exact continuation state between roots.
     *
     * <p>The whole sequence is enclosed in one SavedData transaction when it runs on a server
     * level.  This matters for functions such as {@code exploration_map}: state created by an
     * earlier table is visible to later tables during this hypothetical sequence, while all
     * predicted writes are restored when the sequence finishes.
     */
    public static Result execute(
            MinecraftServer server,
            List<ResourceLocation> tableIds,
            LootAnalysisContext context,
            XoroshiroState1201 initialState) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(tableIds, "tableIds");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(initialState, "initialState");
        List<ResourceLocation> roots = copyRoots(tableIds);
        Supplier<Result> action =
                () -> executeWithinTransaction(server, roots, context, initialState);
        return context.level() instanceof ServerLevel level
                ? SavedDataTransaction1201.run(level, action)
                : action.get();
    }

    /** Convenience overload for callers that naturally hold an iterable of roots. */
    public static Result execute(
            MinecraftServer server,
            Iterable<ResourceLocation> tableIds,
            LootAnalysisContext context,
            XoroshiroState1201 initialState) {
        Objects.requireNonNull(tableIds, "tableIds");
        ArrayList<ResourceLocation> roots = new ArrayList<>();
        tableIds.forEach(roots::add);
        return execute(server, roots, context, initialState);
    }

    /**
     * Computes the first-moment output measure for an initial finite state PMF while retaining
     * the exact PMF of the state after the final root.
     *
     * <p>Each initial state is a deterministic branch.  Branches are executed independently so
     * a failed branch cannot contaminate another branch's mutable SavedData snapshot.  Final
     * states are merged only after the table sequence has completed; this is valid because all
     * future random calls depend on the continuation state, not on the history by which that
     * state was reached.
     */
    public static ExpectationResult expectation(
            MinecraftServer server,
            List<ResourceLocation> tableIds,
            LootAnalysisContext context,
            FiniteDistribution<XoroshiroState1201> initialStates,
            int maxStates) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(tableIds, "tableIds");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(initialStates, "initialStates");
        if (maxStates <= 0) {
            return ExpectationResult.unsupported(
                    new Diagnostic(
                            "STATE_SPACE_LIMIT",
                            "State-space limit must be positive: " + maxStates));
        }
        if (initialStates.masses().size() > maxStates) {
            return ExpectationResult.unsupported(
                    new Diagnostic(
                            "STATE_SPACE_LIMIT",
                            "Initial RNG distribution exceeds state limit " + maxStates));
        }

        List<ResourceLocation> roots = copyRoots(tableIds);
        StackMeasure measure = new StackMeasure();
        LinkedHashMap<XoroshiroState1201, ExactProbability> finalMasses = new LinkedHashMap<>();
        LinkedHashSet<Diagnostic> diagnostics = new LinkedHashSet<>();
        for (Map.Entry<XoroshiroState1201, ExactProbability> branch :
                initialStates.masses().entrySet()) {
            Result result = execute(server, roots, context, branch.getKey());
            diagnostics.addAll(result.diagnostics());
            if (!result.supported()) {
                return ExpectationResult.unsupported(List.copyOf(diagnostics));
            }
            ExactProbability mass = branch.getValue();
            for (StackState output : result.outputs()) {
                measure.add(output, mass);
            }
            finalMasses.merge(result.randomState(), mass, ExactProbability::add);
            if (finalMasses.size() > maxStates) {
                diagnostics.add(
                        new Diagnostic(
                                "STATE_SPACE_LIMIT",
                                "Execution exceeded state limit " + maxStates));
                return ExpectationResult.unsupported(List.copyOf(diagnostics));
            }
        }

        // A FiniteDistribution is deliberately constructed at the boundary so its exact
        // normalization invariant is checked even when several branches coalesce.
        FiniteDistribution<XoroshiroState1201> finalStates = FiniteDistribution.of(finalMasses);
        return ExpectationResult.exact(measure, finalStates, List.copyOf(diagnostics));
    }

    /** Alias whose name makes the first-moment nature explicit at call sites. */
    public static ExpectationResult expectationSequence(
            MinecraftServer server,
            List<ResourceLocation> tableIds,
            LootAnalysisContext context,
            FiniteDistribution<XoroshiroState1201> initialStates,
            int maxStates) {
        return expectation(server, tableIds, context, initialStates, maxStates);
    }

    private static Result executeWithinTransaction(
            MinecraftServer server,
            List<ResourceLocation> tableIds,
            LootAnalysisContext context,
            XoroshiroState1201 initialState) {
        ArrayList<StackState> outputs = new ArrayList<>();
        LinkedHashSet<Diagnostic> diagnostics = new LinkedHashSet<>();
        XoroshiroState1201 state = initialState;
        for (ResourceLocation tableId : tableIds) {
            StatefulLootTableExecutor1201.Result result =
                    StatefulLootTableExecutor1201.execute(server, tableId, context, state);
            diagnostics.addAll(result.diagnostics());
            if (!result.supported()) {
                return Result.unsupported(result.randomState(), List.copyOf(diagnostics));
            }
            outputs.addAll(result.outputs());
            state = result.randomState();
        }
        return Result.exact(outputs, state, List.copyOf(diagnostics));
    }

    private static List<ResourceLocation> copyRoots(Iterable<ResourceLocation> tableIds) {
        ArrayList<ResourceLocation> roots = new ArrayList<>();
        for (ResourceLocation tableId : tableIds) {
            roots.add(Objects.requireNonNull(tableId, "table id"));
        }
        return Collections.unmodifiableList(roots);
    }

    /** One deterministic sequence transition. */
    public record Result(
            boolean supported,
            List<StackState> outputs,
            XoroshiroState1201 randomState,
            List<Diagnostic> diagnostics) {
        public Result {
            outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
            randomState = Objects.requireNonNull(randomState, "randomState");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private static Result exact(
                List<StackState> outputs,
                XoroshiroState1201 randomState,
                List<Diagnostic> diagnostics) {
            return new Result(true, outputs, randomState, diagnostics);
        }

        private static Result unsupported(
                XoroshiroState1201 randomState, List<Diagnostic> diagnostics) {
            return new Result(false, List.of(), randomState, diagnostics);
        }
    }

    /** First-moment output plus the exact continuation PMF after the final root. */
    public record ExpectationResult(
            AnalysisStatus status,
            StackMeasure measure,
            Optional<FiniteDistribution<XoroshiroState1201>> finalStates,
            List<Diagnostic> diagnostics) {
        public ExpectationResult {
            status = Objects.requireNonNull(status, "status");
            measure = Objects.requireNonNull(measure, "measure");
            finalStates = Objects.requireNonNull(finalStates, "finalStates");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
            if (status != AnalysisStatus.EXACT) {
                measure = new StackMeasure();
                finalStates = Optional.empty();
            } else if (finalStates.isEmpty()) {
                throw new IllegalArgumentException("Exact sequence must retain final states");
            }
        }

        private static ExpectationResult exact(
                StackMeasure measure,
                FiniteDistribution<XoroshiroState1201> finalStates,
                List<Diagnostic> diagnostics) {
            return new ExpectationResult(
                    AnalysisStatus.EXACT, measure, Optional.of(finalStates), diagnostics);
        }

        private static ExpectationResult unsupported(Diagnostic diagnostic) {
            return new ExpectationResult(
                    AnalysisStatus.UNSUPPORTED,
                    new StackMeasure(),
                    Optional.empty(),
                    List.of(diagnostic));
        }

        private static ExpectationResult unsupported(List<Diagnostic> diagnostics) {
            return new ExpectationResult(
                    AnalysisStatus.UNSUPPORTED,
                    new StackMeasure(),
                    Optional.empty(),
                    diagnostics);
        }
    }
}
