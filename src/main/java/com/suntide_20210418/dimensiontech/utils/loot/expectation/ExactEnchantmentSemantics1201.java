package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactRandomSemantics1201.RandomCall;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactRandomSemantics1201.RandomMethod;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.RandomTraceDistribution.Outcome;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** Exact finite branching of EnchantmentHelper.enchantItem for Minecraft 1.20.1. */
public final class ExactEnchantmentSemantics1201 {
    private ExactEnchantmentSemantics1201() {}

    /**
     * Result of the terminal-only enchantment transition.
     *
     * <p>The transition deliberately does not expose the selected enchantment list. Once all stack
     * functions have run, only the terminal item/count/rarity key is observable by the valuation
     * layer. A failed result never contains a partial measure.
     */
    public record TerminalEvaluation(
            boolean supported,
            TerminalStackMeasure measure,
            boolean hasRandomCalls,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        public TerminalEvaluation {
            measure = Objects.requireNonNull(measure, "measure");
            pointer = pointer == null ? "" : pointer;
            message = message == null ? "" : message;
        }

        public static TerminalEvaluation exact(
                TerminalStackMeasure measure, boolean hasRandomCalls) {
            return new TerminalEvaluation(true, measure, hasRandomCalls, "", "", null);
        }

        public static TerminalEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        public static TerminalEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new TerminalEvaluation(
                    false,
                    TerminalStackMeasure.empty(),
                    false,
                    pointer,
                    message,
                    Objects.requireNonNull(failureKind, "failureKind"));
        }

        public static TerminalEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }

        /** Alias used by callers that name the projected value a terminal measure. */
        public TerminalStackMeasure terminalMeasure() {
            return measure;
        }

        public AnalysisStatus status() {
            return supported ? AnalysisStatus.EXACT : AnalysisStatus.UNSUPPORTED;
        }
    }

    /**
     * Computes the exact terminal measure for an expected-occurrence input measure.
     *
     * <p>This is intentionally a separate API from {@link #enchantItemsMarginal}: the latter
     * materializes every distinct ordered enchantment list because a later function may inspect
     * that list. Here all functions are complete, so the source branches are reduced to exactly the
     * two relevant events (no available enchantment and at least one available enchantment).
     */
    public static TerminalEvaluation enchantItemsTerminal(
            StackMeasure inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        return enchantItemsTerminal(inputs, levels, treasure, maxStates, false);
    }

    /**
     * Variant that lets the caller carry whether the level provider itself performed a random call.
     * A plain PMF cannot retain that metadata, while the enchantment transition still reports its
     * own calls exactly.
     */
    public static TerminalEvaluation enchantItemsTerminal(
            StackMeasure inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates,
            boolean levelsHaveRandomCalls) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(levels, "levels");
        if (maxStates <= 0) {
            return TerminalEvaluation.randomSemantics(
                    "", "Terminal state limit must be positive: " + maxStates);
        }
        return aggregateTerminal(
                inputs.values().entrySet(), levels, treasure, maxStates, levelsHaveRandomCalls);
    }

    /** Exact terminal transition for a normalized finite input PMF. */
    public static TerminalEvaluation enchantItemsTerminal(
            FiniteDistribution<StackState> inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        return enchantItemsTerminal(inputs, levels, treasure, maxStates, false);
    }

    public static TerminalEvaluation enchantItemsTerminal(
            FiniteDistribution<StackState> inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates,
            boolean levelsHaveRandomCalls) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(levels, "levels");
        if (maxStates <= 0) {
            return TerminalEvaluation.randomSemantics(
                    "", "Terminal state limit must be positive: " + maxStates);
        }
        return aggregateTerminal(
                inputs.masses().entrySet(), levels, treasure, maxStates, levelsHaveRandomCalls);
    }

    /**
     * Preserves random-call metadata when the level provider is already represented by a traced
     * PMF. The trace is only used for its call-presence bit; no call trace is discarded before a
     * caller has had an opportunity to account for it.
     */
    public static TerminalEvaluation enchantItemsTerminal(
            StackMeasure inputs,
            RandomTraceDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        Objects.requireNonNull(levels, "levels");
        boolean levelCalls =
                levels.masses().keySet().stream().anyMatch(outcome -> !outcome.calls().isEmpty());
        return enchantItemsTerminal(inputs, levels.marginal(), treasure, maxStates, levelCalls);
    }

    public static TerminalEvaluation enchantItemsTerminal(
            FiniteDistribution<StackState> inputs,
            RandomTraceDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        Objects.requireNonNull(levels, "levels");
        boolean levelCalls =
                levels.masses().keySet().stream().anyMatch(outcome -> !outcome.calls().isEmpty());
        return enchantItemsTerminal(inputs, levels.marginal(), treasure, maxStates, levelCalls);
    }

    /** Convenience overload for one runtime stack and one deterministic base level. */
    public static TerminalEvaluation enchantItemTerminal(
            ItemStack input, int level, boolean treasure, int maxStates) {
        Objects.requireNonNull(input, "input");
        return enchantItemsTerminal(
                FiniteDistribution.singleton(new StackState(input)),
                FiniteDistribution.singleton(level),
                treasure,
                maxStates);
    }

    /** Convenience overload for one complete stack state and a level PMF. */
    public static TerminalEvaluation enchantItemTerminal(
            StackState input, FiniteDistribution<Integer> levels, boolean treasure, int maxStates) {
        Objects.requireNonNull(input, "input");
        return enchantItemsTerminal(
                FiniteDistribution.singleton(input), levels, treasure, maxStates);
    }

    private static TerminalEvaluation aggregateTerminal(
            Set<Map.Entry<StackState, ExactProbability>> inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates,
            boolean levelsHaveRandomCalls) {
        LinkedHashMap<TerminalStackKey, ExactProbability> result = new LinkedHashMap<>();
        boolean hasRandomCalls = levelsHaveRandomCalls;
        boolean hadInput = false;
        for (Map.Entry<StackState, ExactProbability> input : inputs) {
            if (input.getValue().isZero()) continue;
            hadInput = true;
            TerminalInputEvaluation branch;
            try {
                branch = terminalForInput(input.getKey(), levels, treasure, maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return TerminalEvaluation.randomSemantics("", exception.getMessage());
            } catch (RuntimeException exception) {
                return TerminalEvaluation.unsupported(
                        "",
                        "Runtime enchantment semantics failed: "
                                + exception.getClass().getSimpleName()
                                + (exception.getMessage() == null
                                        ? ""
                                        : ": " + exception.getMessage()));
            }
            if (!branch.supported()) {
                return TerminalEvaluation.unsupported(
                        branch.pointer(), branch.message(), branch.failureKind());
            }
            hasRandomCalls |= branch.hasRandomCalls();
            for (Map.Entry<TerminalStackKey, ExactProbability> value :
                    branch.measure().values().entrySet()) {
                ExactProbability weighted = input.getValue().multiply(value.getValue());
                result.merge(value.getKey(), weighted, ExactProbability::add);
                if (result.size() > maxStates) {
                    return TerminalEvaluation.randomSemantics(
                            "",
                            "Terminal state space "
                                    + result.size()
                                    + " exceeds limit "
                                    + maxStates);
                }
            }
        }
        return TerminalEvaluation.exact(
                TerminalStackMeasure.of(result), hadInput && hasRandomCalls);
    }

    private static TerminalInputEvaluation terminalForInput(
            StackState inputState,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        Objects.requireNonNull(inputState, "inputState");
        ItemStack input = inputState.stack();
        // Vanilla enchantItem replaces a book even when selectEnchantment returns an empty list.
        ItemStack emptyOutput = apply(input, List.of());
        TerminalStackKey emptyKey = TerminalStackKey.from(emptyOutput);
        int enchantability = input.getEnchantmentValue();
        if (enchantability <= 0) {
            return TerminalInputEvaluation.exact(
                    TerminalStackMeasure.of(Map.of(emptyKey, ExactProbability.ONE)), false);
        }

        ExactProbability noAvailable = ExactProbability.ZERO;
        ExactProbability atLeastOne = ExactProbability.ZERO;
        TerminalStackKey nonEmptyKey = null;
        boolean hasRandomCalls = false;

        /*
         * The level perturbation PMF is the only random state retained here.  For every adjusted
         * level we inspect only whether the runtime available list is empty.  We never recurse
         * through the weighted/compatible selection tree, whose ordered leaves are irrelevant at
         * this terminal boundary.
         */
        for (Map.Entry<Integer, ExactProbability> baseLevel : levels.masses().entrySet()) {
            ExactRandomSemantics1201.RandomResult<Integer> perturbed =
                    ExactRandomSemantics1201.enchantmentLevelPerturbation(
                            baseLevel.getKey(), enchantability, Integer.MAX_VALUE);
            hasRandomCalls |= !perturbed.calls().isEmpty();
            for (Map.Entry<Integer, ExactProbability> adjusted :
                    perturbed.distribution().masses().entrySet()) {
                ExactProbability branchMass = baseLevel.getValue().multiply(adjusted.getValue());
                List<EnchantmentInstance> available =
                        EnchantmentHelper.getAvailableEnchantmentResults(
                                adjusted.getKey(), input, treasure);
                if (available.isEmpty()) {
                    noAvailable = noAvailable.add(branchMass);
                    continue;
                }

                // A non-empty available list always produces at least one selected enchantment in
                // 1.20.1: WeightedRandom chooses the first one before the continuation test.
                atLeastOne = atLeastOne.add(branchMass);
                if (nonEmptyKey == null) {
                    EnchantmentInstance first = available.get(0);
                    ItemStack representative = apply(input, List.of(SelectedEnchantment.of(first)));
                    if (!usesVanillaRarityMethod(representative.getItem())) {
                        return TerminalInputEvaluation.unsupported(
                                "",
                                "Cannot terminally compress enchantment choices for item "
                                        + representative.getItem().getClass().getName()
                                        + ": getRarity(ItemStack) is overridden",
                                EvaluationFailureKind.UNSUPPORTED_TYPE);
                    }
                    nonEmptyKey = TerminalStackKey.from(representative);
                } else {
                    // This check is cheap and guards the proof against an unexpected runtime
                    // item implementation while still avoiding ordered-combination enumeration.
                    ItemStack representative =
                            apply(input, List.of(SelectedEnchantment.of(available.get(0))));
                    if (!usesVanillaRarityMethod(representative.getItem())
                            || !nonEmptyKey.equals(TerminalStackKey.from(representative))) {
                        return TerminalInputEvaluation.unsupported(
                                "",
                                "Non-empty enchantment choices do not share one terminal key",
                                EvaluationFailureKind.UNSUPPORTED_TYPE);
                    }
                }
            }
        }

        if (!noAvailable.add(atLeastOne).equals(ExactProbability.ONE)) {
            return TerminalInputEvaluation.unsupported(
                    "",
                    "Enchantment terminal partition did not conserve probability",
                    EvaluationFailureKind.RANDOM_SEMANTICS);
        }
        LinkedHashMap<TerminalStackKey, ExactProbability> values = new LinkedHashMap<>();
        if (!noAvailable.isZero()) values.put(emptyKey, noAvailable);
        if (!atLeastOne.isZero()) {
            if (nonEmptyKey == null) {
                return TerminalInputEvaluation.unsupported(
                        "",
                        "Positive non-empty enchantment mass has no representative terminal key",
                        EvaluationFailureKind.RANDOM_SEMANTICS);
            }
            values.merge(nonEmptyKey, atLeastOne, ExactProbability::add);
        }
        if (values.size() > maxStates) {
            return TerminalInputEvaluation.randomSemantics(
                    "",
                    "Terminal input state space " + values.size() + " exceeds limit " + maxStates);
        }
        return TerminalInputEvaluation.exact(TerminalStackMeasure.of(values), hasRandomCalls);
    }

    private static boolean usesVanillaRarityMethod(net.minecraft.world.item.Item item) {
        try {
            return item.getClass().getMethod("getRarity", ItemStack.class).getDeclaringClass()
                    == net.minecraft.world.item.Item.class;
        } catch (ReflectiveOperationException | SecurityException exception) {
            return false;
        }
    }

    private record TerminalInputEvaluation(
            boolean supported,
            TerminalStackMeasure measure,
            boolean hasRandomCalls,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static TerminalInputEvaluation exact(
                TerminalStackMeasure measure, boolean hasRandomCalls) {
            return new TerminalInputEvaluation(true, measure, hasRandomCalls, "", "", null);
        }

        private static TerminalInputEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new TerminalInputEvaluation(
                    false, TerminalStackMeasure.empty(), false, pointer, message, failureKind);
        }

        private static TerminalInputEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    public static RandomTraceDistribution<StackState> enchantItem(
            ItemStack input, int level, boolean treasure, int maxStates) {
        int enchantability = input.getEnchantmentValue();
        if (enchantability <= 0) return RandomTraceDistribution.singleton(new StackState(input));

        var perturbed =
                ExactRandomSemantics1201.enchantmentLevelPerturbation(
                        level, enchantability, maxStates);
        LinkedHashMap<Outcome<StackState>, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> adjusted :
                perturbed.distribution().masses().entrySet()) {
            RandomTraceDistribution<List<SelectedEnchantment>> selected =
                    selectEnchantments(input, adjusted.getKey(), treasure, maxStates);
            for (Map.Entry<Outcome<List<SelectedEnchantment>>, ExactProbability> branch :
                    selected.masses().entrySet()) {
                ArrayList<RandomCall> calls = new ArrayList<>(perturbed.calls());
                calls.addAll(branch.getKey().calls());
                ItemStack output = apply(input, branch.getKey().value());
                result.merge(
                        new Outcome<>(new StackState(output), calls),
                        adjusted.getValue().multiply(branch.getValue()),
                        ExactProbability::add);
                if (result.size() > maxStates) {
                    throw new ExactRandomSemantics1201.StateSpaceLimitException(
                            result.size(), maxStates);
                }
            }
        }
        return RandomTraceDistribution.of(result);
    }

    /**
     * Exact output marginal for first-moment execution. This follows the same source branches as
     * {@link #enchantItem} but merges each leaf directly by its final runtime stack instead of
     * materializing call-trace outcomes that no later kernel observes.
     */
    public static FiniteDistribution<StackState> enchantItemMarginal(
            ItemStack input, int level, boolean treasure, int maxStates) {
        return enchantItemMarginal(input, FiniteDistribution.singleton(level), treasure, maxStates);
    }

    /**
     * Exact output marginal for a distributed level provider. Overlapping perturbed levels are
     * combined before enchantment selection, so each distinct adjusted level is expanded once.
     */
    public static FiniteDistribution<StackState> enchantItemMarginal(
            ItemStack input, FiniteDistribution<Integer> levels, boolean treasure, int maxStates) {
        return enchantItemsMarginal(
                FiniteDistribution.singleton(new StackState(input)), levels, treasure, maxStates);
    }

    /**
     * Exact marginal transition for several input stacks at once. Inputs with identical
     * enchantability and identical ordered available-enchantment lists share one selection PMF.
     * This is important after {@code set_damage}: damage commonly creates hundreds of input states,
     * while vanilla enchantment applicability is unchanged for every one of them.
     */
    public static FiniteDistribution<StackState> enchantItemsMarginal(
            FiniteDistribution<StackState> inputs,
            FiniteDistribution<Integer> levels,
            boolean treasure,
            int maxStates) {
        Map<Integer, Map<Integer, ExactProbability>> adjustedByEnchantability = new HashMap<>();
        Map<SelectionPlanKey, FiniteDistribution<List<SelectedEnchantment>>> selectionCache =
                new HashMap<>();
        SelectionComputer selectionComputer = new SelectionComputer(maxStates);
        ArrayList<EnchantmentWork> work = new ArrayList<>();
        ArrayList<Map.Entry<StackState, ExactProbability>> unchanged = new ArrayList<>();

        for (Map.Entry<StackState, ExactProbability> input : inputs.masses().entrySet()) {
            ItemStack stack = input.getKey().stack();
            int enchantability = stack.getEnchantmentValue();
            if (enchantability <= 0) {
                unchanged.add(input);
                continue;
            }

            Map<Integer, ExactProbability> adjustedLevels =
                    adjustedByEnchantability.computeIfAbsent(
                            enchantability,
                            ignored -> adjustedLevels(levels, enchantability, maxStates));
            SelectionPlanKey planKey = selectionPlanKey(stack, adjustedLevels, treasure);
            FiniteDistribution<List<SelectedEnchantment>> selections =
                    selectionCache.computeIfAbsent(
                            planKey,
                            ignored -> selectionComputer.select(planKey.availableByLevel()));
            work.add(new EnchantmentWork(input.getKey(), input.getValue(), selections));
        }

        rejectProvenOversizedCartesianProduct(work, maxStates);
        LinkedHashMap<StackState, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<StackState, ExactProbability> input : unchanged) {
            mergeStack(result, input.getKey(), input.getValue(), maxStates);
        }
        for (EnchantmentWork input : work) {
            ItemStack stack = input.input().stack();
            for (Map.Entry<List<SelectedEnchantment>, ExactProbability> selection :
                    input.selections().masses().entrySet()) {
                mergeStack(
                        result,
                        new StackState(apply(stack, selection.getKey())),
                        input.mass().multiply(selection.getValue()),
                        maxStates);
            }
        }
        return FiniteDistribution.of(result);
    }

    /**
     * For a non-book stack without an existing Enchantments tag, enchantItem only appends the
     * ordered selected list and preserves every pre-existing serialized field. Therefore distinct
     * base states and distinct serialized selection lists form a provably injective Cartesian
     * product. This common set_damage -> enchant_with_levels case can enforce the state limit
     * before allocating and serializing up to a million ItemStacks.
     */
    private static void rejectProvenOversizedCartesianProduct(
            List<EnchantmentWork> work, int maxStates) {
        HashSet<CompoundTag> normalizedBases = new HashSet<>();
        for (EnchantmentWork branch : work) {
            ItemStack stack = branch.input().stack();
            if (stack.is(Items.BOOK)
                    || (stack.hasTag() && stack.getTag().contains("Enchantments"))) {
                return;
            }
            stack.getOrCreateTag().remove("Enchantments");
            if (!normalizedBases.add(stack.serializeNBT())) return;
        }

        IdentityHashMap<FiniteDistribution<List<SelectedEnchantment>>, Integer> serializedCounts =
                new IdentityHashMap<>();
        long distinctStates = 0L;
        for (EnchantmentWork branch : work) {
            Integer serializedCount = serializedCounts.get(branch.selections());
            if (serializedCount == null) {
                HashSet<List<SerializedEnchantment>> serializedSelections = new HashSet<>();
                for (List<SelectedEnchantment> selection : branch.selections().masses().keySet()) {
                    serializedSelections.add(
                            selection.stream()
                                    .map(
                                            value ->
                                                    new SerializedEnchantment(
                                                            value.enchantment(),
                                                            (byte) value.level()))
                                    .toList());
                }
                serializedCount = serializedSelections.size();
                serializedCounts.put(branch.selections(), serializedCount);
            }
            distinctStates += serializedCount;
            if (distinctStates > maxStates) {
                int reportedStates =
                        maxStates == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxStates + 1;
                throw new ExactRandomSemantics1201.StateSpaceLimitException(
                        reportedStates, maxStates);
            }
        }
    }

    private static Map<Integer, ExactProbability> adjustedLevels(
            FiniteDistribution<Integer> levels, int enchantability, int maxStates) {
        LinkedHashMap<Integer, ExactProbability> adjustedLevels = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> level : levels.masses().entrySet()) {
            var perturbed =
                    ExactRandomSemantics1201.enchantmentLevelPerturbation(
                            level.getKey(), enchantability, maxStates);
            for (Map.Entry<Integer, ExactProbability> adjusted :
                    perturbed.distribution().masses().entrySet()) {
                adjustedLevels.merge(
                        adjusted.getKey(),
                        level.getValue().multiply(adjusted.getValue()),
                        ExactProbability::add);
                if (adjustedLevels.size() > maxStates) {
                    throw new ExactRandomSemantics1201.StateSpaceLimitException(
                            adjustedLevels.size(), maxStates);
                }
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(adjustedLevels));
    }

    private static SelectionPlanKey selectionPlanKey(
            ItemStack stack, Map<Integer, ExactProbability> adjustedLevels, boolean treasure) {
        ArrayList<AvailableAtLevel> availableByLevel = new ArrayList<>(adjustedLevels.size());
        for (int level : adjustedLevels.keySet()) {
            List<SelectedEnchantment> available =
                    EnchantmentHelper.getAvailableEnchantmentResults(level, stack, treasure)
                            .stream()
                            .map(SelectedEnchantment::of)
                            .toList();
            availableByLevel.add(new AvailableAtLevel(level, adjustedLevels.get(level), available));
        }
        return new SelectionPlanKey(List.copyOf(availableByLevel));
    }

    private static void mergeStack(
            Map<StackState, ExactProbability> result,
            StackState state,
            ExactProbability mass,
            int maxStates) {
        result.merge(state, mass, ExactProbability::add);
        if (result.size() > maxStates) {
            throw new ExactRandomSemantics1201.StateSpaceLimitException(result.size(), maxStates);
        }
    }

    private static final class SelectionComputer {
        private final int maxStates;
        private final Map<TailKey, FiniteDistribution<List<SelectedEnchantment>>> tails =
                new HashMap<>();
        private final Set<TailKey> activeTails = new HashSet<>();

        private SelectionComputer(int maxStates) {
            this.maxStates = maxStates;
        }

        private FiniteDistribution<List<SelectedEnchantment>> select(
                List<AvailableAtLevel> availableByLevel) {
            LinkedHashMap<List<SelectedEnchantment>, ExactProbability> result =
                    new LinkedHashMap<>();
            for (AvailableAtLevel atLevel : availableByLevel) {
                ExactProbability adjustedMass = atLevel.mass();
                List<SelectedEnchantment> available = atLevel.available();
                if (available.isEmpty()) {
                    mergeSelection(result, List.of(), adjustedMass, maxStates);
                    continue;
                }
                for (WeightedSelectedChoice first : weightedSelectedChoices(available)) {
                    List<SelectedEnchantment> compatible = compatibleWith(available, first.value());
                    FiniteDistribution<List<SelectedEnchantment>> suffixes =
                            tail(compatible, atLevel.level());
                    for (Map.Entry<List<SelectedEnchantment>, ExactProbability> suffix :
                            suffixes.masses().entrySet()) {
                        ArrayList<SelectedEnchantment> chosen =
                                new ArrayList<>(suffix.getKey().size() + 1);
                        chosen.add(first.value());
                        chosen.addAll(suffix.getKey());
                        mergeSelection(
                                result,
                                List.copyOf(chosen),
                                adjustedMass.multiply(first.mass()).multiply(suffix.getValue()),
                                maxStates);
                    }
                }
            }
            return FiniteDistribution.of(result);
        }

        private FiniteDistribution<List<SelectedEnchantment>> tail(
                List<SelectedEnchantment> compatible, int level) {
            TailKey key = new TailKey(compatible, level);
            FiniteDistribution<List<SelectedEnchantment>> cached = tails.get(key);
            if (cached != null) return cached;
            if (!activeTails.add(key)) {
                throw new IllegalArgumentException(
                        "Unbounded enchantment selection caused by a compatible cycle");
            }
            try {
                int successfulValues = Math.max(0, Math.min(50, level + 1));
                ExactProbability continues = ExactProbability.of(successfulValues, 50);
                ExactProbability stops = ExactProbability.ONE.subtract(continues);
                LinkedHashMap<List<SelectedEnchantment>, ExactProbability> result =
                        new LinkedHashMap<>();
                if (!stops.isZero()) {
                    mergeSelection(result, List.of(), stops, maxStates);
                }
                if (!continues.isZero()) {
                    if (compatible.isEmpty()) {
                        mergeSelection(result, List.of(), continues, maxStates);
                    } else {
                        for (WeightedSelectedChoice next : weightedSelectedChoices(compatible)) {
                            FiniteDistribution<List<SelectedEnchantment>> suffixes =
                                    tail(compatibleWith(compatible, next.value()), level / 2);
                            for (Map.Entry<List<SelectedEnchantment>, ExactProbability> suffix :
                                    suffixes.masses().entrySet()) {
                                ArrayList<SelectedEnchantment> chosen =
                                        new ArrayList<>(suffix.getKey().size() + 1);
                                chosen.add(next.value());
                                chosen.addAll(suffix.getKey());
                                mergeSelection(
                                        result,
                                        List.copyOf(chosen),
                                        continues.multiply(next.mass()).multiply(suffix.getValue()),
                                        maxStates);
                            }
                        }
                    }
                }
                FiniteDistribution<List<SelectedEnchantment>> computed =
                        FiniteDistribution.of(result);
                tails.put(key, computed);
                return computed;
            } finally {
                activeTails.remove(key);
            }
        }
    }

    private static List<SelectedEnchantment> compatibleWith(
            List<SelectedEnchantment> available, SelectedEnchantment selected) {
        return available.stream()
                .filter(
                        candidate ->
                                selected.enchantment().isCompatibleWith(candidate.enchantment()))
                .toList();
    }

    private static List<WeightedSelectedChoice> weightedSelectedChoices(
            List<SelectedEnchantment> candidates) {
        int total = candidates.stream().mapToInt(value -> value.weight()).sum();
        if (total <= 0) throw new IllegalArgumentException("zero enchantment weight");
        ArrayList<WeightedSelectedChoice> result = new ArrayList<>(candidates.size());
        for (SelectedEnchantment candidate : candidates) {
            int weight = candidate.weight();
            if (weight > 0) {
                result.add(
                        new WeightedSelectedChoice(candidate, ExactProbability.of(weight, total)));
            }
        }
        return List.copyOf(result);
    }

    private static void mergeSelection(
            Map<List<SelectedEnchantment>, ExactProbability> result,
            List<SelectedEnchantment> selection,
            ExactProbability mass,
            int maxStates) {
        result.merge(selection, mass, ExactProbability::add);
        if (result.size() > maxStates) {
            throw new ExactRandomSemantics1201.StateSpaceLimitException(result.size(), maxStates);
        }
    }

    private static RandomTraceDistribution<List<SelectedEnchantment>> selectEnchantments(
            ItemStack stack, int level, boolean treasure, int maxStates) {
        List<EnchantmentInstance> available =
                EnchantmentHelper.getAvailableEnchantmentResults(level, stack, treasure);
        if (available.isEmpty()) return RandomTraceDistribution.singleton(List.of());

        LinkedHashMap<Outcome<List<SelectedEnchantment>>, ExactProbability> result =
                new LinkedHashMap<>();
        for (WeightedChoice first : weightedChoices(available)) {
            List<SelectedEnchantment> chosen = List.of(SelectedEnchantment.of(first.value()));
            continueSelection(
                    available,
                    chosen,
                    level,
                    List.of(new RandomCall(RandomMethod.NEXT_INT_BOUND, first.totalWeight())),
                    first.mass(),
                    result,
                    maxStates);
        }
        return RandomTraceDistribution.of(result);
    }

    private static void continueSelection(
            List<EnchantmentInstance> available,
            List<SelectedEnchantment> chosen,
            int level,
            List<RandomCall> calls,
            ExactProbability mass,
            Map<Outcome<List<SelectedEnchantment>>, ExactProbability> result,
            int maxStates) {
        ArrayList<RandomCall> afterCheck = new ArrayList<>(calls);
        afterCheck.add(new RandomCall(RandomMethod.NEXT_INT_BOUND, 50));
        int successfulValues = Math.max(0, Math.min(50, level + 1));
        ExactProbability continues = ExactProbability.of(successfulValues, 50);
        ExactProbability stops = ExactProbability.ONE.subtract(continues);
        if (!stops.isZero()) {
            merge(
                    result,
                    new Outcome<>(List.copyOf(chosen), afterCheck),
                    mass.multiply(stops),
                    maxStates);
        }
        if (continues.isZero()) return;

        SelectedEnchantment last = chosen.get(chosen.size() - 1);
        List<EnchantmentInstance> compatible =
                available.stream()
                        .filter(
                                candidate ->
                                        last.enchantment().isCompatibleWith(candidate.enchantment))
                        .toList();
        if (compatible.isEmpty()) {
            merge(
                    result,
                    new Outcome<>(List.copyOf(chosen), afterCheck),
                    mass.multiply(continues),
                    maxStates);
            return;
        }
        for (WeightedChoice next : weightedChoices(compatible)) {
            ArrayList<SelectedEnchantment> nextChosen = new ArrayList<>(chosen);
            nextChosen.add(SelectedEnchantment.of(next.value()));
            ArrayList<RandomCall> nextCalls = new ArrayList<>(afterCheck);
            nextCalls.add(new RandomCall(RandomMethod.NEXT_INT_BOUND, next.totalWeight()));
            continueSelection(
                    compatible,
                    List.copyOf(nextChosen),
                    level / 2,
                    nextCalls,
                    mass.multiply(continues).multiply(next.mass()),
                    result,
                    maxStates);
        }
    }

    private static List<WeightedChoice> weightedChoices(List<EnchantmentInstance> candidates) {
        int total = candidates.stream().mapToInt(value -> value.getWeight().asInt()).sum();
        if (total <= 0) throw new IllegalArgumentException("zero enchantment weight");
        ArrayList<WeightedChoice> result = new ArrayList<>(candidates.size());
        for (EnchantmentInstance candidate : candidates) {
            int weight = candidate.getWeight().asInt();
            if (weight > 0) {
                result.add(
                        new WeightedChoice(candidate, ExactProbability.of(weight, total), total));
            }
        }
        return List.copyOf(result);
    }

    private static void merge(
            Map<Outcome<List<SelectedEnchantment>>, ExactProbability> result,
            Outcome<List<SelectedEnchantment>> outcome,
            ExactProbability mass,
            int maxStates) {
        result.merge(outcome, mass, ExactProbability::add);
        if (result.size() > maxStates) {
            throw new ExactRandomSemantics1201.StateSpaceLimitException(result.size(), maxStates);
        }
    }

    private static ItemStack apply(ItemStack input, List<SelectedEnchantment> enchantments) {
        boolean book = input.is(Items.BOOK);
        // ItemStack.copy() returns EMPTY for count <= 0, while vanilla enchantItem mutates the
        // supplied stack in place.  Reconstructing from NBT and restoring count preserves the
        // complete runtime state for zero/negative-count test stacks as well.
        ItemStack output = book ? new ItemStack(Items.ENCHANTED_BOOK) : copyPreservingCount(input);
        for (SelectedEnchantment enchantment : enchantments) {
            EnchantmentInstance instance = enchantment.toInstance();
            if (book) EnchantedBookItem.addEnchantment(output, instance);
            else output.enchant(enchantment.enchantment(), enchantment.level());
        }
        return output;
    }

    private static ItemStack copyPreservingCount(ItemStack source) {
        CompoundTag serialized = source.serializeNBT().copy();
        ItemStack copy = ItemStack.of(serialized);
        copy.setCount(source.getCount());
        return copy;
    }

    private record SelectedEnchantment(
            net.minecraft.world.item.enchantment.Enchantment enchantment, int level) {
        private static SelectedEnchantment of(EnchantmentInstance instance) {
            return new SelectedEnchantment(instance.enchantment, instance.level);
        }

        private int weight() {
            return enchantment.getRarity().getWeight();
        }

        private EnchantmentInstance toInstance() {
            return new EnchantmentInstance(enchantment, level);
        }
    }

    private record AvailableAtLevel(
            int level, ExactProbability mass, List<SelectedEnchantment> available) {
        private AvailableAtLevel {
            available = List.copyOf(available);
        }
    }

    private record SelectionPlanKey(List<AvailableAtLevel> availableByLevel) {
        private SelectionPlanKey {
            availableByLevel = List.copyOf(availableByLevel);
        }
    }

    private record EnchantmentWork(
            StackState input,
            ExactProbability mass,
            FiniteDistribution<List<SelectedEnchantment>> selections) {}

    private record SerializedEnchantment(
            net.minecraft.world.item.enchantment.Enchantment enchantment, byte level) {}

    private record TailKey(List<SelectedEnchantment> available, int level) {
        private TailKey {
            available = List.copyOf(available);
        }
    }

    private record WeightedSelectedChoice(SelectedEnchantment value, ExactProbability mass) {}

    private record WeightedChoice(
            EnchantmentInstance value, ExactProbability mass, int totalWeight) {}
}
