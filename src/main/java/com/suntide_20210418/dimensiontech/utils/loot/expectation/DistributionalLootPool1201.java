package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact finite pool branching with lazy reachability and ordered logical random calls. */
public final class DistributionalLootPool1201 {
    private DistributionalLootPool1201() {}

    public static Evaluation execute(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            String poolPointer) {
        return execute(pool, context, maxStates, tagExpander, null, poolPointer);
    }

    public static Evaluation execute(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            String poolPointer) {
        ExecutionEvaluation<SelectedEntry> result =
                execute(
                        pool,
                        context,
                        maxStates,
                        tagExpander,
                        conditionReferences,
                        (selected, ignored) ->
                                SelectionEvaluation.exact(
                                        RandomTraceDistribution.singleton(List.of(selected))),
                        poolPointer);
        return result.supported()
                ? Evaluation.exact(result.distribution())
                : Evaluation.unsupported(
                        result.pointer(), result.message(), result.failureKind());
    }

    public static <T> ExecutionEvaluation<T> execute(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            SelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        String pointer = poolPointer == null ? "" : poolPointer;
        try {
            return executeUnchecked(
                    pool,
                    context,
                    maxStates,
                    tagExpander,
                    conditionReferences,
                    selectionExecutor,
                    pointer);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExecutionEvaluation.randomSemantics(pointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return ExecutionEvaluation.unsupported(pointer, malformedJsonMessage(exception));
        }
    }

    private static <T> ExecutionEvaluation<T> executeUnchecked(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            SelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(tagExpander, "tagExpander");
        Objects.requireNonNull(selectionExecutor, "selectionExecutor");
        String pointer = poolPointer == null ? "" : poolPointer;

        DistributionalCondition1201.Evaluation conditions =
                DistributionalCondition1201.testAll(
                        pool.get("conditions"),
                        context,
                        maxStates,
                        pointer + "/conditions",
                        conditionReferences);
        if (!conditions.supported())
            return ExecutionEvaluation.unsupported(
                    conditions.pointer(), conditions.message(), conditions.failureKind());
        if (!canReach(conditions.distribution(), true)) {
            return ExecutionEvaluation.exact(
                    conditions
                            .distribution()
                            .flatMap(
                                    ignored -> RandomTraceDistribution.singleton(List.of()),
                                    maxStates));
        }

        DistributionalNumberProvider1201.Evaluation<Integer> rolls =
                DistributionalNumberProvider1201.getInt(
                        pool.get("rolls"), context, maxStates, pointer + "/rolls");
        if (!rolls.supported())
            return ExecutionEvaluation.unsupported(
                    rolls.pointer(), rolls.message(), rolls.failureKind());
        DistributionalNumberProvider1201.Evaluation<Integer> bonus =
                DistributionalNumberProvider1201.getBonusFloor(
                        pool.get("bonus_rolls"), context, maxStates, pointer + "/bonus_rolls");
        if (!bonus.supported())
            return ExecutionEvaluation.unsupported(
                    bonus.pointer(), bonus.message(), bonus.failureKind());

        RandomTraceDistribution<Integer> rollCounts;
        try {
            rollCounts =
                    rolls.distribution()
                            .flatMap(
                                    base ->
                                            bonus.distribution()
                                                    .flatMap(
                                                            extra ->
                                                                    RandomTraceDistribution
                                                                            .singleton(
                                                                                    Math.max(
                                                                                            0,
                                                                                            base
                                                                                                    + extra)),
                                                            maxStates),
                                    maxStates);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExecutionEvaluation.randomSemantics(pointer, exception.getMessage());
        }

        RollEvaluation singleRoll;
        if (!canReachPositive(rollCounts)) {
            singleRoll = RollEvaluation.exact(RandomTraceDistribution.singleton(null));
        } else {
            JsonElement entriesElement = pool.get("entries");
            if (entriesElement != null && !entriesElement.isJsonArray()) {
                return ExecutionEvaluation.unsupported(
                        pointer + "/entries", "Entry list is not an array");
            }
            singleRoll =
                    singleRoll(
                            entriesElement == null
                                    ? new JsonArray()
                                    : entriesElement.getAsJsonArray(),
                            context,
                            maxStates,
                            tagExpander,
                            conditionReferences,
                            pointer + "/entries");
        }
        if (!singleRoll.supported()) {
            return ExecutionEvaluation.unsupported(
                    singleRoll.pointer(), singleRoll.message(), singleRoll.failureKind());
        }

        ProcessedRollEvaluation<T> processed =
                processRoll(
                        singleRoll.distribution(),
                        selectionExecutor,
                        maxStates,
                        pointer + "/entries");
        if (!processed.supported()) {
            return ExecutionEvaluation.unsupported(
                    processed.pointer(), processed.message(), processed.failureKind());
        }

        try {
            RandomTraceDistribution<List<T>> passed =
                    rollCounts.flatMap(
                            count -> repeat(processed.distribution(), count, maxStates), maxStates);
            RandomTraceDistribution<List<T>> result =
                    conditions
                            .distribution()
                            .flatMap(
                                    pass ->
                                            pass
                                                    ? passed
                                                    : RandomTraceDistribution.singleton(List.of()),
                                    maxStates);
            return ExecutionEvaluation.exact(result);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExecutionEvaluation.randomSemantics(pointer, exception.getMessage());
        }
    }

    /**
     * Computes first moments without materializing the Cartesian product of every roll's output
     * list. In {@link IdealRandomProbabilitySpace1201}, roll bodies are independent and identically
     * distributed after the roll-count branch has been evaluated, so linearity makes this an exact
     * reduction rather than an approximation.
     */
    public static <T> ExpectedExecutionEvaluation<T> expectation(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            ExpectedSelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        String pointer = poolPointer == null ? "" : poolPointer;
        try {
            return expectationUnchecked(
                    pool,
                    context,
                    maxStates,
                    tagExpander,
                    conditionReferences,
                    selectionExecutor,
                    pointer);
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExpectedExecutionEvaluation.randomSemantics(pointer, exception.getMessage());
        } catch (RuntimeException exception) {
            return ExpectedExecutionEvaluation.unsupported(pointer, malformedJsonMessage(exception));
        }
    }

    private static <T> ExpectedExecutionEvaluation<T> expectationUnchecked(
            JsonObject pool,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            ExpectedSelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(tagExpander, "tagExpander");
        Objects.requireNonNull(selectionExecutor, "selectionExecutor");
        String pointer = poolPointer == null ? "" : poolPointer;

        DistributionalCondition1201.Evaluation conditions =
                DistributionalCondition1201.testAll(
                        pool.get("conditions"),
                        context,
                        maxStates,
                        pointer + "/conditions",
                        conditionReferences);
        if (!conditions.supported()) {
            return ExpectedExecutionEvaluation.unsupported(
                    conditions.pointer(), conditions.message(), conditions.failureKind());
        }
        boolean hasRandomCalls = hasRandomCalls(conditions.distribution());
        ExactProbability passMass =
                conditions
                        .distribution()
                        .marginal()
                        .masses()
                        .getOrDefault(true, ExactProbability.ZERO);
        if (passMass.isZero()) {
            return ExpectedExecutionEvaluation.exact(Map.of(), hasRandomCalls, 0, 0);
        }

        DistributionalNumberProvider1201.Evaluation<Integer> rolls =
                DistributionalNumberProvider1201.getInt(
                        pool.get("rolls"), context, maxStates, pointer + "/rolls");
        if (!rolls.supported()) {
            return ExpectedExecutionEvaluation.unsupported(
                    rolls.pointer(), rolls.message(), rolls.failureKind());
        }
        DistributionalNumberProvider1201.Evaluation<Integer> bonus =
                DistributionalNumberProvider1201.getBonusFloor(
                        pool.get("bonus_rolls"), context, maxStates, pointer + "/bonus_rolls");
        if (!bonus.supported()) {
            return ExpectedExecutionEvaluation.unsupported(
                    bonus.pointer(), bonus.message(), bonus.failureKind());
        }
        hasRandomCalls |=
                hasRandomCalls(rolls.distribution()) || hasRandomCalls(bonus.distribution());

        LinkedHashMap<Integer, ExactProbability> rollCountMasses = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> base :
                rolls.distribution().marginal().masses().entrySet()) {
            for (Map.Entry<Integer, ExactProbability> extra :
                    bonus.distribution().marginal().masses().entrySet()) {
                rollCountMasses.merge(
                        Math.max(0, base.getKey() + extra.getKey()),
                        base.getValue().multiply(extra.getValue()),
                        ExactProbability::add);
                if (rollCountMasses.size() > maxStates) {
                    return ExpectedExecutionEvaluation.randomSemantics(
                            pointer,
                            "Roll-count state space "
                                    + rollCountMasses.size()
                                    + " exceeds limit "
                                    + maxStates);
                }
            }
        }
        FiniteDistribution<Integer> rollCounts = FiniteDistribution.of(rollCountMasses);

        ExactProbability expectedRolls = ExactProbability.ZERO;
        for (Map.Entry<Integer, ExactProbability> branch :
                rollCounts.masses().entrySet()) {
            expectedRolls =
                    expectedRolls.add(
                            branch.getValue().multiply(ExactProbability.of(branch.getKey(), 1)));
        }
        if (expectedRolls.isZero()) {
            return ExpectedExecutionEvaluation.exact(Map.of(), hasRandomCalls, 0, 0);
        }
        int maxRollCount =
                rollCounts.masses().keySet().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);

        JsonElement entriesElement = pool.get("entries");
        if (entriesElement != null && !entriesElement.isJsonArray()) {
            return ExpectedExecutionEvaluation.unsupported(
                    pointer + "/entries", "Entry list is not an array");
        }
        RollEvaluation singleRoll =
                singleRoll(
                        entriesElement == null
                                ? new JsonArray()
                                : entriesElement.getAsJsonArray(),
                        context,
                        maxStates,
                        tagExpander,
                        conditionReferences,
                        pointer + "/entries");
        if (!singleRoll.supported()) {
            return ExpectedExecutionEvaluation.unsupported(
                    singleRoll.pointer(), singleRoll.message(), singleRoll.failureKind());
        }
        hasRandomCalls |= hasRandomCalls(singleRoll.distribution());

        LinkedHashMap<SelectedEntry, ExpectedSelectionEvaluation<T>> kernels =
                new LinkedHashMap<>();
        LinkedHashMap<T, ExactProbability> oneRoll = new LinkedHashMap<>();
        int maxOneRollOutputs = 0;
        int maxOneRollMapAllocations = 0;
        for (Map.Entry<SelectedEntry, ExactProbability> branch :
                singleRoll.distribution().marginal().masses().entrySet()) {
            SelectedEntry selected = branch.getKey();
            if (selected == null) continue;
            ExpectedSelectionEvaluation<T> generated = kernels.get(selected);
            if (generated == null) {
                generated = selectionExecutor.execute(selected, maxStates);
                if (generated == null) {
                    return ExpectedExecutionEvaluation.unsupported(
                            selected.pointer(), "Selection executor returned no result");
                }
                if (!generated.supported()) {
                    return ExpectedExecutionEvaluation.unsupported(
                            generated.pointer(), generated.message(), generated.failureKind());
                }
                kernels.put(selected, generated);
            }
            hasRandomCalls |= generated.hasRandomCalls();
            maxOneRollOutputs = Math.max(maxOneRollOutputs, generated.maxOutputs());
            maxOneRollMapAllocations =
                    Math.max(maxOneRollMapAllocations, generated.maxMapAllocations());
            for (Map.Entry<T, ExactProbability> output : generated.occurrences().entrySet()) {
                oneRoll.merge(
                        output.getKey(),
                        branch.getValue().multiply(output.getValue()),
                        ExactProbability::add);
                if (oneRoll.size() > maxStates) {
                    return ExpectedExecutionEvaluation.randomSemantics(
                            pointer,
                            "Expected output state space "
                                    + oneRoll.size()
                                    + " exceeds limit "
                                    + maxStates);
                }
            }
        }

        ExactProbability scale = passMass.multiply(expectedRolls);
        LinkedHashMap<T, ExactProbability> result = new LinkedHashMap<>();
        oneRoll.forEach((value, mass) -> result.put(value, mass.multiply(scale)));
        return ExpectedExecutionEvaluation.exact(
                result,
                hasRandomCalls,
                cappedProduct(maxRollCount, maxOneRollOutputs),
                cappedProduct(maxRollCount, maxOneRollMapAllocations));
    }

    private static int cappedProduct(int left, int right) {
        if (left <= 0 || right <= 0) return 0;
        return left == 1 && right == 1 ? 1 : 2;
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable pool AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static <T> RandomTraceDistribution<List<T>> repeat(
            RandomTraceDistribution<List<T>> kernel, int count, int maxStates) {
        RandomTraceDistribution<List<T>> current = RandomTraceDistribution.singleton(List.of());
        for (int index = 0; index < count; index++) {
            current =
                    current.flatMap(
                            outputs ->
                                    kernel.flatMap(
                                            generated -> {
                                                ArrayList<T> next = new ArrayList<>(outputs);
                                                next.addAll(generated);
                                                return RandomTraceDistribution.singleton(
                                                        List.copyOf(next));
                                            },
                                            maxStates),
                            maxStates);
        }
        return current;
    }

    private static <T> ProcessedRollEvaluation<T> processRoll(
            RandomTraceDistribution<SelectedEntry> selections,
            SelectionExecutor<T> selectionExecutor,
            int maxStates,
            String pointer) {
        Map<SelectedEntry, RandomTraceDistribution<List<T>>> kernels = new LinkedHashMap<>();
        for (SelectedEntry selected : selections.marginal().masses().keySet()) {
            if (selected == null) continue;
            SelectionEvaluation<T> result = selectionExecutor.execute(selected, maxStates);
            if (result == null) {
                return ProcessedRollEvaluation.unsupported(
                        selected.pointer(), "Selection executor returned no result");
            }
            if (!result.supported()) {
                return ProcessedRollEvaluation.unsupported(
                        result.pointer(), result.message(), result.failureKind());
            }
            kernels.put(selected, result.distribution());
        }
        try {
            return ProcessedRollEvaluation.exact(
                    selections.flatMap(
                            selected ->
                                    selected == null
                                            ? RandomTraceDistribution.singleton(List.of())
                                            : kernels.get(selected),
                            maxStates));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ProcessedRollEvaluation.randomSemantics(pointer, exception.getMessage());
        }
    }

    private static RollEvaluation singleRoll(
            JsonArray entries,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        ExpansionEvaluation expansion =
                expandEntries(
                        entries, context, maxStates, tagExpander, conditionReferences, pointer);
        if (!expansion.supported()) {
            return RollEvaluation.unsupported(
                    expansion.pointer(), expansion.message(), expansion.failureKind());
        }
        try {
            return RollEvaluation.exact(
                    expansion
                            .distribution()
                            .flatMap(
                                    value -> {
                                        if (value.candidates().isEmpty()) {
                                            return RandomTraceDistribution.singleton(null);
                                        }
                                        if (value.candidates().size() == 1) {
                                            Candidate only = value.candidates().get(0);
                                            return RandomTraceDistribution.singleton(
                                                    new SelectedEntry(
                                                            only.entry(), only.pointer()));
                                        }
                                        List<Integer> weights =
                                                value.candidates().stream()
                                                        .map(Candidate::weight)
                                                        .toList();
                                        return RandomTraceDistribution.fromRandomResult(
                                                        ExactRandomSemantics1201.weightedIndex(
                                                                weights))
                                                .flatMap(
                                                        index -> {
                                                            Candidate selected =
                                                                    value.candidates().get(index);
                                                            return RandomTraceDistribution
                                                                    .singleton(
                                                                            new SelectedEntry(
                                                                                    selected
                                                                                            .entry(),
                                                                                    selected
                                                                                            .pointer()));
                                                        },
                                                        maxStates);
                                    },
                                    maxStates));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return RollEvaluation.randomSemantics(pointer, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return RollEvaluation.unsupported(pointer, exception.getMessage());
        }
    }

    private static ExpansionEvaluation expandEntries(
            JsonArray entries,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        RandomTraceDistribution<Expansion> current =
                RandomTraceDistribution.singleton(Expansion.of(List.of(), true));
        for (int index = 0; index < entries.size(); index++) {
            ExpansionEvaluation child =
                    expand(
                            entries.get(index),
                            context,
                            maxStates,
                            tagExpander,
                            conditionReferences,
                            pointer + "/" + index);
            if (!child.supported()) return child;
            try {
                current =
                        current.flatMap(
                                prior ->
                                        child.distribution()
                                                .flatMap(
                                                        next -> {
                                                            ArrayList<Candidate> candidates =
                                                                    new ArrayList<>(
                                                                            prior.candidates());
                                                            candidates.addAll(next.candidates());
                                                            return RandomTraceDistribution
                                                                    .singleton(
                                                                            Expansion.of(
                                                                                    candidates,
                                                                                    true));
                                                        },
                                                        maxStates),
                                maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return ExpansionEvaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        return ExpansionEvaluation.exact(current);
    }

    private static ExpansionEvaluation expand(
            JsonElement element,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        if (element == null || !element.isJsonObject()) {
            return ExpansionEvaluation.unsupported(pointer, "Entry is not an object");
        }
        JsonObject entry = element.getAsJsonObject();
        DistributionalCondition1201.Evaluation conditions =
                DistributionalCondition1201.testAll(
                        entry.get("conditions"),
                        context,
                        maxStates,
                        pointer + "/conditions",
                        conditionReferences);
        if (!conditions.supported()) {
            return ExpansionEvaluation.unsupported(
                    conditions.pointer(), conditions.message(), conditions.failureKind());
        }
        if (!canReach(conditions.distribution(), true)) {
            return ExpansionEvaluation.exact(
                    conditions
                            .distribution()
                            .flatMap(
                                    ignored ->
                                            RandomTraceDistribution.singleton(
                                                    Expansion.of(List.of(), false)),
                                    maxStates));
        }

        WeightResult weight = effectiveWeight(entry, context.luck(), pointer);
        if (!weight.supported()) {
            return ExpansionEvaluation.unsupported(
                    weight.pointer(), weight.message());
        }
        // A non-positive effective weight is a runtime-proven unreachable entry.  Do not inspect
        // type-specific fields (which may be malformed) after this proof.
        if (weight.value() <= 0) {
            return ExpansionEvaluation.exact(
                    conditions
                            .distribution()
                            .flatMap(
                                    ignored ->
                                            RandomTraceDistribution.singleton(
                                                    Expansion.of(List.of(), true)),
                                    maxStates));
        }
        String type = stringField(entry, "type");
        if (type == null) {
            return ExpansionEvaluation.unsupported(
                    pointer + "/type", "Entry type is missing or not a string");
        }
        ExpansionEvaluation body;
        if (type.equals("minecraft:item")
                || type.equals("minecraft:empty")
                || type.equals("minecraft:loot_table")) {
            if (!type.equals("minecraft:empty") && !validName(entry)) {
                return ExpansionEvaluation.unsupported(
                        pointer + "/name", "Entry name is missing or not a string");
            }
            body = singleton(entry, context.luck(), pointer);
        } else if (type.equals("minecraft:tag")) {
            if (!validName(entry)) {
                return ExpansionEvaluation.unsupported(
                        pointer + "/name", "Entry name is missing or not a string");
            }
            body = tag(entry, context.luck(), tagExpander, pointer);
        } else if (type.equals("minecraft:group")
                || type.equals("minecraft:sequence")
                || type.equals("minecraft:alternatives")) {
            JsonArray children =
                    entry.has("children") && entry.get("children").isJsonArray()
                            ? entry.getAsJsonArray("children")
                            : null;
            body =
                    children == null
                            ? ExpansionEvaluation.unsupported(
                                    pointer + "/children",
                                    entry.has("children")
                                            ? "Composite entry children is not an array"
                                            : "Composite entry has no children")
                            : composite(
                                    type,
                                    children,
                                    context,
                                    maxStates,
                                    tagExpander,
                                    conditionReferences,
                                    pointer + "/children");
        } else {
            body =
                    ExpansionEvaluation.unsupported(
                            pointer, "Unsupported reachable entry type " + type);
        }
        if (!body.supported()) return body;
        try {
            return ExpansionEvaluation.exact(
                    conditions
                            .distribution()
                            .flatMap(
                                    pass ->
                                            pass
                                                    ? body.distribution()
                                                    : RandomTraceDistribution.singleton(
                                                            Expansion.of(List.of(), false)),
                                    maxStates));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return ExpansionEvaluation.randomSemantics(pointer, exception.getMessage());
        }
    }

    private static ExpansionEvaluation composite(
            String type,
            JsonArray children,
            LootAnalysisContext context,
            int maxStates,
            TagExpander tagExpander,
            DistributionalCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        boolean alternatives = type.equals("minecraft:alternatives");
        boolean sequence = type.equals("minecraft:sequence");
        RandomTraceDistribution<Expansion> current =
                RandomTraceDistribution.singleton(Expansion.of(List.of(), !alternatives));
        for (int index = 0; index < children.size(); index++) {
            boolean hasContinuingBranch =
                    current.marginal().masses().keySet().stream()
                            .anyMatch(
                                    value ->
                                            alternatives
                                                    ? !value.expanded()
                                                    : !sequence || value.expanded());
            if (!hasContinuingBranch) break;
            ExpansionEvaluation child =
                    expand(
                            children.get(index),
                            context,
                            maxStates,
                            tagExpander,
                            conditionReferences,
                            pointer + "/" + index);
            if (!child.supported()) return child;
            try {
                current =
                        current.flatMap(
                                prior -> {
                                    boolean stop =
                                            alternatives
                                                    ? prior.expanded()
                                                    : sequence && !prior.expanded();
                                    if (stop) return RandomTraceDistribution.singleton(prior);
                                    return child.distribution()
                                            .flatMap(
                                                    next -> {
                                                        ArrayList<Candidate> candidates =
                                                                new ArrayList<>(prior.candidates());
                                                        candidates.addAll(next.candidates());
                                                        boolean expanded =
                                                                alternatives
                                                                        ? next.expanded()
                                                                        : !sequence
                                                                                || next.expanded();
                                                        return RandomTraceDistribution.singleton(
                                                                Expansion.of(candidates, expanded));
                                                    },
                                                    maxStates);
                                },
                                maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return ExpansionEvaluation.randomSemantics(pointer, exception.getMessage());
            }
        }
        if (!alternatives && !sequence) {
            current =
                    current.flatMap(
                            value ->
                                    RandomTraceDistribution.singleton(
                                            Expansion.of(value.candidates(), true)),
                            maxStates);
        }
        return ExpansionEvaluation.exact(current);
    }

    private static ExpansionEvaluation singleton(JsonObject entry, float luck, String pointer) {
        WeightResult weight = effectiveWeight(entry, luck, pointer);
        if (!weight.supported()) {
            return ExpansionEvaluation.unsupported(weight.pointer(), weight.message());
        }
        return ExpansionEvaluation.exact(
                RandomTraceDistribution.singleton(
                        Expansion.of(
                                weight.value() > 0
                                        ? List.of(new Candidate(entry, weight.value(), pointer))
                                        : List.of(),
                                true)));
    }

    private static ExpansionEvaluation tag(
            JsonObject entry, float luck, TagExpander tagExpander, String pointer) {
        Boolean expand = booleanField(entry, "expand", false);
        if (expand == null) {
            return ExpansionEvaluation.unsupported(
                    pointer + "/expand", "Entry expand is not a boolean");
        }
        if (!expand) {
            return singleton(entry, luck, pointer);
        }
        List<JsonObject> expanded;
        try {
            expanded = tagExpander.expand(entry);
        } catch (RuntimeException exception) {
            return ExpansionEvaluation.unsupported(
                    pointer + "/name", malformedTagMessage(exception));
        }
        if (expanded == null) {
            return ExpansionEvaluation.unsupported(pointer, "Unable to expand reachable item tag");
        }
        WeightResult weight = effectiveWeight(entry, luck, pointer);
        if (!weight.supported()) {
            return ExpansionEvaluation.unsupported(
                    weight.pointer(), weight.message(), weight.failureKind());
        }
        for (JsonObject value : expanded) {
            if (value == null) {
                return ExpansionEvaluation.unsupported(
                        pointer, "Tag expansion returned a null entry");
            }
        }
        List<Candidate> candidates =
                weight.value() <= 0
                        ? List.of()
                        : expanded.stream()
                                .map(value -> new Candidate(value, weight.value(), pointer))
                                .toList();
        return ExpansionEvaluation.exact(
                RandomTraceDistribution.singleton(Expansion.of(candidates, true)));
    }

    private static WeightResult effectiveWeight(JsonObject entry, float luck, String pointer) {
        Integer weight = integerField(entry, "weight", 1);
        if (weight == null) {
            return WeightResult.unsupported(
                    pointer + "/weight", "Entry weight is not an integer");
        }
        Integer quality = integerField(entry, "quality", 0);
        if (quality == null) {
            return WeightResult.unsupported(
                    pointer + "/quality", "Entry quality is not an integer");
        }
        float adjusted = (float) weight + (float) quality * luck;
        if (!Float.isFinite(adjusted)) {
            return WeightResult.unsupported(
                    pointer + "/quality", "Entry weight adjustment is not finite");
        }
        return WeightResult.exact(Math.max(Mth.floor(adjusted), 0));
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Boolean booleanField(JsonObject object, String name, boolean defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) return null;
        try {
            return value.getAsBoolean();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Integer integerField(JsonObject object, String name, int defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) return null;
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean validName(JsonObject entry) {
        String name = stringField(entry, "name");
        return name != null && !name.isEmpty();
    }

    private static String malformedTagMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable tag expansion ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static boolean canReach(RandomTraceDistribution<Boolean> distribution, boolean value) {
        return !distribution
                .marginal()
                .masses()
                .getOrDefault(value, ExactProbability.ZERO)
                .isZero();
    }

    private static boolean canReachPositive(RandomTraceDistribution<Integer> distribution) {
        return distribution.marginal().masses().entrySet().stream()
                .anyMatch(entry -> entry.getKey() > 0 && !entry.getValue().isZero());
    }

    private static boolean hasRandomCalls(RandomTraceDistribution<?> distribution) {
        return distribution.masses().keySet().stream()
                .anyMatch(outcome -> !outcome.calls().isEmpty());
    }

    @FunctionalInterface
    public interface TagExpander {
        List<JsonObject> expand(JsonObject tagEntry);
    }

    @FunctionalInterface
    public interface SelectionExecutor<T> {
        SelectionEvaluation<T> execute(SelectedEntry selected, int maxStates);
    }

    @FunctionalInterface
    public interface ExpectedSelectionExecutor<T> {
        ExpectedSelectionEvaluation<T> execute(SelectedEntry selected, int maxStates);
    }

    public record SelectedEntry(JsonObject entry, String pointer) {}

    public record SelectionEvaluation<T>(
            boolean supported,
            RandomTraceDistribution<List<T>> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public static <T> SelectionEvaluation<T> exact(
                RandomTraceDistribution<List<T>> distribution) {
            return new SelectionEvaluation<>(true, distribution, "", "", null);
        }

        public static <T> SelectionEvaluation<T> unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static <T> SelectionEvaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new SelectionEvaluation<>(false, null, pointer, message, failureKind);
        }

        public static <T> SelectionEvaluation<T> randomSemantics(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public record ExpectedSelectionEvaluation<T>(
            boolean supported,
            Map<T, ExactProbability> occurrences,
            boolean hasRandomCalls,
            int maxOutputs,
            int maxMapAllocations,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public ExpectedSelectionEvaluation {
            occurrences = Collections.unmodifiableMap(new LinkedHashMap<>(occurrences));
        }

        public static <T> ExpectedSelectionEvaluation<T> exact(
                Map<T, ExactProbability> occurrences, boolean hasRandomCalls) {
            return exact(occurrences, hasRandomCalls, occurrences.isEmpty() ? 0 : 1, 0);
        }

        public static <T> ExpectedSelectionEvaluation<T> exact(
                Map<T, ExactProbability> occurrences,
                boolean hasRandomCalls,
                int maxOutputs,
                int maxMapAllocations) {
            return new ExpectedSelectionEvaluation<>(
                    true,
                    occurrences,
                    hasRandomCalls,
                    Math.min(2, Math.max(0, maxOutputs)),
                    Math.min(2, Math.max(0, maxMapAllocations)),
                    "",
                    "",
                    null);
        }

        public static <T> ExpectedSelectionEvaluation<T> unsupported(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static <T> ExpectedSelectionEvaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedSelectionEvaluation<>(
                    false, Map.of(), false, 0, 0, pointer, message, failureKind);
        }

        public static <T> ExpectedSelectionEvaluation<T> randomSemantics(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public record ExpectedExecutionEvaluation<T>(
            boolean supported,
            Map<T, ExactProbability> occurrences,
            boolean hasRandomCalls,
            int maxOutputs,
            int maxMapAllocations,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public ExpectedExecutionEvaluation {
            occurrences = Collections.unmodifiableMap(new LinkedHashMap<>(occurrences));
        }

        private static <T> ExpectedExecutionEvaluation<T> exact(
                Map<T, ExactProbability> occurrences,
                boolean hasRandomCalls,
                int maxOutputs,
                int maxMapAllocations) {
            return new ExpectedExecutionEvaluation<>(
                    true,
                    occurrences,
                    hasRandomCalls,
                    Math.min(2, Math.max(0, maxOutputs)),
                    Math.min(2, Math.max(0, maxMapAllocations)),
                    "",
                    "",
                    null);
        }

        private static <T> ExpectedExecutionEvaluation<T> unsupported(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static <T> ExpectedExecutionEvaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedExecutionEvaluation<>(
                    false, Map.of(), false, 0, 0, pointer, message, failureKind);
        }

        private static <T> ExpectedExecutionEvaluation<T> randomSemantics(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public record ExecutionEvaluation<T>(
            boolean supported,
            RandomTraceDistribution<List<T>> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static <T> ExecutionEvaluation<T> exact(
                RandomTraceDistribution<List<T>> distribution) {
            return new ExecutionEvaluation<>(true, distribution, "", "", null);
        }

        private static <T> ExecutionEvaluation<T> unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static <T> ExecutionEvaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExecutionEvaluation<>(false, null, pointer, message, failureKind);
        }

        private static <T> ExecutionEvaluation<T> randomSemantics(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public record Evaluation(
            boolean supported,
            RandomTraceDistribution<List<SelectedEntry>> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static Evaluation exact(RandomTraceDistribution<List<SelectedEntry>> distribution) {
            return new Evaluation(true, distribution, "", "", null);
        }

        private static Evaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static Evaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new Evaluation(false, null, pointer, message, failureKind);
        }
    }

    private record Candidate(JsonObject entry, int weight, String pointer) {}

    private record WeightResult(
            boolean supported,
            int value,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static WeightResult exact(int value) {
            return new WeightResult(true, value, "", "", null);
        }

        private static WeightResult unsupported(String pointer, String message) {
            return new WeightResult(
                    false, 0, pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }
    }

    private record Expansion(List<Candidate> candidates, boolean expanded) {
        private static Expansion of(List<Candidate> candidates, boolean expanded) {
            return new Expansion(List.copyOf(candidates), expanded);
        }
    }

    private record ExpansionEvaluation(
            boolean supported,
            RandomTraceDistribution<Expansion> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static ExpansionEvaluation exact(RandomTraceDistribution<Expansion> distribution) {
            return new ExpansionEvaluation(true, distribution, "", "", null);
        }

        private static ExpansionEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static ExpansionEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpansionEvaluation(false, null, pointer, message, failureKind);
        }

        private static ExpansionEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record RollEvaluation(
            boolean supported,
            RandomTraceDistribution<SelectedEntry> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static RollEvaluation exact(RandomTraceDistribution<SelectedEntry> distribution) {
            return new RollEvaluation(true, distribution, "", "", null);
        }

        private static RollEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static RollEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new RollEvaluation(false, null, pointer, message, failureKind);
        }

        private static RollEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record ProcessedRollEvaluation<T>(
            boolean supported,
            RandomTraceDistribution<List<T>> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static <T> ProcessedRollEvaluation<T> exact(
                RandomTraceDistribution<List<T>> distribution) {
            return new ProcessedRollEvaluation<>(true, distribution, "", "", null);
        }

        private static <T> ProcessedRollEvaluation<T> unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static <T> ProcessedRollEvaluation<T> unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ProcessedRollEvaluation<>(false, null, pointer, message, failureKind);
        }

        private static <T> ProcessedRollEvaluation<T> randomSemantics(
                String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }
}
