package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.ForgeRegistries;

/** Runtime LootData AST execution over finite logical RandomSource call branches. */
public final class DistributionalLootTableExecutor1201 {
    private DistributionalLootTableExecutor1201() {}

    /**
     * Production analysis in the explicitly versioned ideal finite-call probability space.
     *
     * <p>The ideal space is part of the marker payload ({@link
     * IdealRandomProbabilitySpace1201#ID}). Each primitive call has its own finite exact outcome
     * domain, and the ordered call trace is retained until a first-moment reduction is proven. This
     * is deliberately distinct from a concrete seeded {@code RandomSource}; callers that need the
     * latter must use {@link StatefulLootTableExecutor1201#expectation} with an explicit initial
     * state distribution.
     */
    public static LootExpectationResult evaluate(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates) {
        LogicalResult logical = evaluateMarginalCalls(server, tableId, context, maxStates);
        return idealProductionResult(logical);
    }

    /** Alias that makes the declared ideal probability contract explicit at call sites. */
    public static LootExpectationResult evaluateIdeal(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates) {
        return evaluate(server, tableId, context, maxStates);
    }

    /**
     * Applies the concrete-source policy to a marginal result. This method is intentionally
     * package-visible for policy tests and is not used by the ideal marker calculation.
     */
    static LootExpectationResult concreteProductionResult(
            ResourceLocation tableId, LogicalResult logical) {
        if (!logical.supported()) {
            return new LootExpectationResult(
                    AnalysisStatus.UNSUPPORTED, new StackMeasure(), logical.diagnostics());
        }
        if (logical.hasRandomCalls()) {
            ArrayList<Diagnostic> diagnostics = new ArrayList<>(logical.diagnostics());
            diagnostics.add(
                    Diagnostic.randomSemantics(
                            tableId,
                            "",
                            List.of(tableId.toString()),
                            "Concrete RandomSource expectation requires an explicit initial"
                                + " RNG-state distribution; the fresh-draw marginal result is not a"
                                + " certificate"));
            return new LootExpectationResult(
                    AnalysisStatus.UNSUPPORTED, new StackMeasure(), diagnostics);
        }
        return idealProductionResult(logical);
    }

    private static LootExpectationResult idealProductionResult(LogicalResult logical) {
        if (!logical.supported()) {
            return new LootExpectationResult(
                    AnalysisStatus.UNSUPPORTED, new StackMeasure(), logical.diagnostics());
        }
        return logical.fullStackMeasureAvailable()
                ? new LootExpectationResult(
                        AnalysisStatus.EXACT, logical.measure(), logical.diagnostics())
                : LootExpectationResult.exactTerminal(
                        logical.terminalMeasure(), logical.diagnostics());
    }

    public static LogicalResult evaluateLogicalCalls(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates) {
        LinkedHashSet<Diagnostic> diagnostics = new LinkedHashSet<>();
        try {
            if (maxStates <= 0) {
                diagnostics.add(
                        Diagnostic.randomSemantics(
                                tableId,
                                "",
                                List.of(tableId.toString()),
                                "State-space limit must be positive: " + maxStates));
                return LogicalResult.unsupported(List.copyOf(diagnostics));
            }
            RuntimeLootAstSource source = new RuntimeLootAstSource(server);
            ExpectedTableEvaluation safety =
                    executeExpected(
                            source,
                            tableId,
                            context,
                            maxStates,
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            List.of(tableId.toString()),
                            diagnostics,
                            false);
            if (!safety.supported()) {
                return LogicalResult.unsupported(List.copyOf(diagnostics));
            }
            TableEvaluation evaluation =
                    execute(
                            source,
                            tableId,
                            context,
                            maxStates,
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            List.of(tableId.toString()),
                            diagnostics);
            if (!evaluation.supported()) {
                return LogicalResult.unsupported(List.copyOf(diagnostics));
            }
            StackMeasure measure = new StackMeasure();
            boolean hasRandomCalls = false;
            evaluation
                    .distribution()
                    .masses()
                    .forEach(
                            (outcome, mass) ->
                                    outcome.value().forEach(stack -> measure.add(stack, mass)));
            for (RandomTraceDistribution.Outcome<List<StackState>> outcome :
                    evaluation.distribution().masses().keySet()) {
                if (!outcome.calls().isEmpty()) {
                    hasRandomCalls = true;
                    break;
                }
            }
            return new LogicalResult(
                    true,
                    measure,
                    TerminalStackMeasure.from(measure),
                    true,
                    hasRandomCalls,
                    List.copyOf(diagnostics));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            diagnostics.add(
                    Diagnostic.randomSemantics(
                            tableId, "", List.of(tableId.toString()), exception.getMessage()));
            return LogicalResult.unsupported(List.copyOf(diagnostics));
        } catch (RuntimeException exception) {
            diagnostics.add(
                    Diagnostic.unsupportedType(
                            tableId,
                            "",
                            List.of(tableId.toString()),
                            malformedTableMessage(exception)));
            return LogicalResult.unsupported(List.copyOf(diagnostics));
        }
    }

    /**
     * First-moment form of {@link #evaluateLogicalCalls}. It preserves lazy reachability and all
     * per-stack function kernels while avoiding full output-list products across rolls and pools.
     */
    public static LogicalResult evaluateMarginalCalls(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates) {
        LinkedHashSet<Diagnostic> diagnostics = new LinkedHashSet<>();
        try {
            if (maxStates <= 0) {
                diagnostics.add(
                        Diagnostic.randomSemantics(
                                tableId,
                                "",
                                List.of(tableId.toString()),
                                "State-space limit must be positive: " + maxStates));
                return LogicalResult.unsupported(List.copyOf(diagnostics));
            }
            ExpectedTableEvaluation evaluation =
                    executeExpected(
                            new RuntimeLootAstSource(server),
                            tableId,
                            context,
                            maxStates,
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            Collections.newSetFromMap(new IdentityHashMap<>()),
                            List.of(tableId.toString()),
                            diagnostics,
                            true);
            return evaluation.supported()
                    ? new LogicalResult(
                            true,
                            evaluation.measure(),
                            evaluation.terminalMeasure(),
                            evaluation.fullStackMeasureAvailable(),
                            evaluation.hasRandomCalls(),
                            List.copyOf(diagnostics))
                    : LogicalResult.unsupported(List.copyOf(diagnostics));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            diagnostics.add(
                    Diagnostic.randomSemantics(
                            tableId, "", List.of(tableId.toString()), exception.getMessage()));
            return LogicalResult.unsupported(List.copyOf(diagnostics));
        } catch (RuntimeException exception) {
            diagnostics.add(
                    Diagnostic.unsupportedType(
                            tableId,
                            "",
                            List.of(tableId.toString()),
                            malformedTableMessage(exception)));
            return LogicalResult.unsupported(List.copyOf(diagnostics));
        }
    }

    private static ExpectedTableEvaluation executeExpected(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath,
            Set<Diagnostic> diagnostics,
            boolean allowTerminalCompression) {
        Optional<RuntimeLootAstSource.RuntimeAst<LootTable>> resolved = source.table(tableId);
        if (resolved.isEmpty()) {
            diagnostics.add(ReferenceSemantics1201.missingTable(tableId, tableId, "", callPath));
            return ExpectedTableEvaluation.exact(
                    new StackMeasure(), TerminalStackMeasure.empty(), true, false, 0, 0);
        }
        Object identity = resolved.get().identity();
        if (!activeTables.add(identity)) {
            diagnostics.add(ReferenceSemantics1201.recursiveTable(tableId, tableId, "", callPath));
            return ExpectedTableEvaluation.exact(
                    new StackMeasure(), TerminalStackMeasure.empty(), true, false, 0, 0);
        }
        try {
            JsonElement serialized = resolved.get().json();
            if (!serialized.isJsonObject()) {
                unsupported(
                        tableId, "", callPath, "Runtime table AST is not an object", diagnostics);
                return ExpectedTableEvaluation.unsupported(
                        "", "Runtime table AST is not an object");
            }
            JsonObject table = serialized.getAsJsonObject();
            JsonElement poolsElement = table.get("pools");
            if (poolsElement != null && !poolsElement.isJsonArray()) {
                String message = "Loot table pools is not an array";
                unsupported(tableId, "/pools", callPath, message, diagnostics);
                return ExpectedTableEvaluation.unsupported("/pools", message);
            }
            JsonArray pools =
                    poolsElement == null ? new JsonArray() : poolsElement.getAsJsonArray();
            JsonElement tableFunctions = table.get("functions");
            StackMeasure result = new StackMeasure();
            LinkedHashMap<TerminalStackKey, ExactProbability> terminalValues =
                    new LinkedHashMap<>();
            boolean fullStackMeasureAvailable = true;
            boolean hasRandomCalls = false;
            int maxOutputs = 0;
            int maxMapAllocations = 0;
            PredicateResolver predicates =
                    new PredicateResolver(
                            source,
                            context,
                            maxStates,
                            activePredicates,
                            diagnostics,
                            tableId,
                            callPath);
            FunctionResolver functionResolver =
                    new FunctionResolver(
                            source,
                            context,
                            maxStates,
                            activePredicates,
                            activeFunctions,
                            diagnostics,
                            tableId,
                            callPath);
            for (int poolIndex = 0; poolIndex < pools.size(); poolIndex++) {
                String poolPointer = "/pools/" + poolIndex;
                JsonElement poolElement = pools.get(poolIndex);
                if (!poolElement.isJsonObject()) {
                    unsupported(
                            tableId, poolPointer, callPath, "Pool is not an object", diagnostics);
                    return ExpectedTableEvaluation.unsupported(
                            poolPointer, "Pool is not an object");
                }
                JsonObject pool = poolElement.getAsJsonObject();
                JsonElement poolFunctions = pool.get("functions");
                DistributionalLootPool1201.ExpectedExecutionEvaluation<ExpectedOutput> poolResult =
                        DistributionalLootPool1201.expectation(
                                pool,
                                context,
                                maxStates,
                                DistributionalLootTableExecutor1201::expandTag,
                                predicates,
                                (selected, ignored) ->
                                        emitEntryExpected(
                                                source,
                                                tableId,
                                                context,
                                                maxStates,
                                                activeTables,
                                                activePredicates,
                                                activeFunctions,
                                                callPath,
                                                selected,
                                                poolFunctions,
                                                poolPointer + "/functions",
                                                tableFunctions,
                                                "/functions",
                                                predicates,
                                                functionResolver,
                                                diagnostics,
                                                allowTerminalCompression),
                                poolPointer);
                if (!poolResult.supported()) {
                    unsupported(
                            tableId,
                            poolResult.pointer(),
                            callPath,
                            poolResult.message(),
                            poolResult.failureKind(),
                            diagnostics);
                    return ExpectedTableEvaluation.unsupported(
                            poolResult.pointer(), poolResult.message(), poolResult.failureKind());
                }
                for (Map.Entry<ExpectedOutput, ExactProbability> occurrence :
                        poolResult.occurrences().entrySet()) {
                    ExpectedOutput output = occurrence.getKey();
                    if (output.full() != null) {
                        result.add(output.full(), occurrence.getValue());
                        terminalValues.merge(
                                TerminalStackKey.from(output.full()),
                                occurrence.getValue(),
                                ExactProbability::add);
                    } else {
                        fullStackMeasureAvailable = false;
                        terminalValues.merge(
                                output.terminal(), occurrence.getValue(), ExactProbability::add);
                    }
                }
                hasRandomCalls |= poolResult.hasRandomCalls();
                maxOutputs = cappedSum(maxOutputs, poolResult.maxOutputs());
                maxMapAllocations = cappedSum(maxMapAllocations, poolResult.maxMapAllocations());
                if (maxMapAllocations > 1) {
                    String message =
                            "More than one exploration-map SavedData allocation is reachable in a"
                                    + " single table execution";
                    diagnostics.add(
                            Diagnostic.randomSemantics(tableId, poolPointer, callPath, message));
                    return ExpectedTableEvaluation.randomSemantics(poolPointer, message);
                }
                if (result.values().size() > maxStates) {
                    String message =
                            "Expected stack state space "
                                    + result.values().size()
                                    + " exceeds limit "
                                    + maxStates;
                    unsupported(
                            tableId,
                            poolPointer,
                            callPath,
                            message,
                            EvaluationFailureKind.RANDOM_SEMANTICS,
                            diagnostics);
                    return ExpectedTableEvaluation.randomSemantics(poolPointer, message);
                }
                if (terminalValues.size() > maxStates) {
                    String message =
                            "Terminal stack state space "
                                    + terminalValues.size()
                                    + " exceeds limit "
                                    + maxStates;
                    return ExpectedTableEvaluation.randomSemantics(poolPointer, message);
                }
            }
            return ExpectedTableEvaluation.exact(
                    result,
                    TerminalStackMeasure.of(terminalValues),
                    fullStackMeasureAvailable,
                    hasRandomCalls,
                    maxOutputs,
                    maxMapAllocations);
        } finally {
            activeTables.remove(identity);
        }
    }

    private static DistributionalLootPool1201.ExpectedSelectionEvaluation<ExpectedOutput>
            emitEntryExpected(
                    RuntimeLootAstSource source,
                    ResourceLocation tableId,
                    LootAnalysisContext context,
                    int maxStates,
                    Set<Object> activeTables,
                    Set<Object> activePredicates,
                    Set<Object> activeFunctions,
                    List<String> callPath,
                    DistributionalLootPool1201.SelectedEntry selected,
                    JsonElement poolFunctionsElement,
                    String poolFunctionsPointer,
                    JsonElement tableFunctionsElement,
                    String tableFunctionsPointer,
                    PredicateResolver predicates,
                    FunctionResolver functionResolver,
                    Set<Diagnostic> diagnostics,
                    boolean allowTerminalCompression) {
        JsonObject entry = selected.entry();
        String pointer = selected.pointer();
        String type = stringField(entry, "type");
        if (type == null) {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    pointer + "/type", "Invalid emitted entry type");
        }
        JsonElement entryFunctionsElement = entry.get("functions");
        StackMeasure generated = new StackMeasure();
        boolean hasRandomCalls = false;
        int maxOutputs = 0;
        int maxMapAllocations = 0;
        if (type.equals("minecraft:empty")) {
            // No output.
        } else if (type.equals("minecraft:item")) {
            ResourceLocation itemId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                        pointer + "/name", "Missing item reference " + itemId);
            }
            generated.add(new StackState(new ItemStack(item)), ExactProbability.ONE);
            maxOutputs = 1;
        } else if (type.equals("minecraft:tag")) {
            ResourceLocation tagId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            if (tagId == null) {
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                        pointer + "/name", "Invalid item tag");
            }
            for (Item item :
                    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId))) {
                generated.add(new StackState(new ItemStack(item)), ExactProbability.ONE);
            }
            maxOutputs = generated.values().isEmpty() ? 0 : generated.values().size() == 1 ? 1 : 2;
        } else if (type.equals("minecraft:loot_table")) {
            ResourceLocation nestedId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            if (nestedId == null) {
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                        pointer + "/name", "Invalid nested table reference");
            }
            ArrayList<String> nestedPath = new ArrayList<>(callPath);
            nestedPath.add(nestedId.toString());
            ExpectedTableEvaluation nested =
                    executeExpected(
                            source,
                            nestedId,
                            context,
                            maxStates,
                            activeTables,
                            activePredicates,
                            activeFunctions,
                            List.copyOf(nestedPath),
                            diagnostics,
                            allowTerminalCompression
                                    && isAbsentOrEmptyFunctionList(entryFunctionsElement)
                                    && isAbsentOrEmptyFunctionList(poolFunctionsElement)
                                    && isAbsentOrEmptyFunctionList(tableFunctionsElement));
            if (!nested.supported()) {
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                        pointer,
                        "Nested table contains an unsupported reachable mechanism",
                        nested.failureKind());
            }
            if (allowTerminalCompression
                    && isAbsentOrEmptyFunctionList(entryFunctionsElement)
                    && isAbsentOrEmptyFunctionList(poolFunctionsElement)
                    && isAbsentOrEmptyFunctionList(tableFunctionsElement)) {
                LinkedHashMap<ExpectedOutput, ExactProbability> terminalOutputs =
                        new LinkedHashMap<>();
                nested.terminalMeasure()
                        .values()
                        .forEach(
                                (key, mass) ->
                                        terminalOutputs.merge(
                                                ExpectedOutput.terminal(key),
                                                mass,
                                                ExactProbability::add));
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                        terminalOutputs,
                        nested.hasRandomCalls(),
                        nested.maxOutputs(),
                        nested.maxMapAllocations());
            }
            generated.addAll(nested.measure(), ExactProbability.ONE);
            hasRandomCalls = nested.hasRandomCalls();
            maxOutputs = nested.maxOutputs();
            maxMapAllocations = nested.maxMapAllocations();
        } else {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    pointer, "Unsupported emitted entry " + type);
        }

        if (generated.values().isEmpty()) {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                    Map.of(), hasRandomCalls, maxOutputs, maxMapAllocations);
        }
        String invalidFunctions =
                invalidFunctionListPointer(
                        entryFunctionsElement,
                        pointer + "/functions",
                        poolFunctionsElement,
                        poolFunctionsPointer,
                        tableFunctionsElement,
                        tableFunctionsPointer);
        if (invalidFunctions != null) {
            String layer =
                    invalidFunctions.equals(pointer + "/functions")
                            ? "Loot entry"
                            : invalidFunctions.equals(poolFunctionsPointer)
                                    ? "Loot pool"
                                    : "Loot table";
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    invalidFunctions, layer + " functions is not an array");
        }
        JsonArray entryFunctions = functionArray(entryFunctionsElement);
        JsonArray poolFunctions = functionArray(poolFunctionsElement);
        JsonArray tableFunctions = functionArray(tableFunctionsElement);

        if (allowTerminalCompression
                && poolFunctions.isEmpty()
                && tableFunctions.isEmpty()
                && isTerminalEnchantWithLevels(entryFunctions)) {
            int terminalIndex = entryFunctions.size() - 1;
            JsonObject terminalFunction = entryFunctions.get(terminalIndex).getAsJsonObject();
            JsonElement conditions = terminalFunction.get("conditions");
            // Conditions with random or context-dependent behavior must remain in the full
            // StackState executor; this sink is only used after a proof that the final function
            // is unconditionally terminal.
            if (conditions == null
                    || (conditions.isJsonArray() && conditions.getAsJsonArray().isEmpty())) {
                JsonArray prefix = new JsonArray();
                for (int index = 0; index < terminalIndex; index++) {
                    prefix.add(entryFunctions.get(index));
                }
                ExpectedLayerEvaluation prefixLayer =
                        applyExpectedLayer(
                                generated,
                                prefix,
                                context,
                                maxStates,
                                pointer + "/functions",
                                predicates,
                                functionResolver,
                                maxOutputs);
                if (!prefixLayer.supported()) {
                    return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                            prefixLayer.pointer(),
                            prefixLayer.message(),
                            prefixLayer.failureKind());
                }
                DistributionalNumberProvider1201.Evaluation<Integer> levels =
                        DistributionalNumberProvider1201.getInt(
                                terminalFunction.get("levels"),
                                context,
                                maxStates,
                                pointer + "/functions/" + terminalIndex + "/levels");
                if (!levels.supported()) {
                    return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                            levels.pointer(), levels.message(), levels.failureKind());
                }
                boolean treasure =
                        terminalFunction.has("treasure")
                                && terminalFunction.get("treasure").getAsBoolean();
                ExactEnchantmentSemantics1201.TerminalEvaluation terminal =
                        ExactEnchantmentSemantics1201.enchantItemsTerminal(
                                prefixLayer.measure(),
                                levels.distribution().marginal(),
                                treasure,
                                maxStates);
                if (!terminal.supported()) {
                    return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                            pointer + "/functions/" + terminalIndex,
                            terminal.message(),
                            terminal.failureKind());
                }
                LinkedHashMap<ExpectedOutput, ExactProbability> terminalOutputs =
                        new LinkedHashMap<>();
                terminal.terminalMeasure()
                        .values()
                        .forEach(
                                (key, mass) ->
                                        terminalOutputs.merge(
                                                ExpectedOutput.terminal(key),
                                                mass,
                                                ExactProbability::add));
                return DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                        terminalOutputs,
                        prefixLayer.hasRandomCalls()
                                || terminal.hasRandomCalls()
                                || levels.distribution().masses().keySet().stream()
                                        .anyMatch(outcome -> !outcome.calls().isEmpty()),
                        1,
                        prefixLayer.maxMapAllocations());
            }
        }

        ExpectedLayerEvaluation entryLayer =
                applyExpectedLayer(
                        generated,
                        entryFunctions,
                        context,
                        maxStates,
                        pointer + "/functions",
                        predicates,
                        functionResolver,
                        maxOutputs);
        if (!entryLayer.supported()) {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    entryLayer.pointer(), entryLayer.message(), entryLayer.failureKind());
        }
        ExpectedLayerEvaluation poolLayer =
                applyExpectedLayer(
                        entryLayer.measure(),
                        poolFunctions,
                        context,
                        maxStates,
                        poolFunctionsPointer,
                        predicates,
                        functionResolver,
                        entryLayer.maxOutputs());
        if (!poolLayer.supported()) {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    poolLayer.pointer(), poolLayer.message(), poolLayer.failureKind());
        }
        ExpectedLayerEvaluation tableLayer =
                applyExpectedLayer(
                        poolLayer.measure(),
                        tableFunctions,
                        context,
                        maxStates,
                        tableFunctionsPointer,
                        predicates,
                        functionResolver,
                        poolLayer.maxOutputs());
        if (!tableLayer.supported()) {
            return DistributionalLootPool1201.ExpectedSelectionEvaluation.unsupported(
                    tableLayer.pointer(), tableLayer.message(), tableLayer.failureKind());
        }
        hasRandomCalls |=
                entryLayer.hasRandomCalls()
                        || poolLayer.hasRandomCalls()
                        || tableLayer.hasRandomCalls();
        maxMapAllocations = cappedSum(maxMapAllocations, entryLayer.maxMapAllocations());
        maxMapAllocations = cappedSum(maxMapAllocations, poolLayer.maxMapAllocations());
        maxMapAllocations = cappedSum(maxMapAllocations, tableLayer.maxMapAllocations());
        LinkedHashMap<ExpectedOutput, ExactProbability> outputs = new LinkedHashMap<>();
        tableLayer
                .measure()
                .values()
                .forEach(
                        (state, mass) ->
                                outputs.merge(
                                        ExpectedOutput.full(state), mass, ExactProbability::add));
        return DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                outputs, hasRandomCalls, tableLayer.maxOutputs(), maxMapAllocations);
    }

    private static ExpectedLayerEvaluation applyExpectedLayer(
            StackMeasure input,
            JsonArray functions,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            PredicateResolver predicates,
            FunctionResolver functionResolver,
            int maxInputStacks) {
        if (functions.isEmpty() || input.values().isEmpty()) {
            return ExpectedLayerEvaluation.exact(input, false, maxInputStacks, 0);
        }
        StackMeasure result = new StackMeasure();
        boolean hasRandomCalls = false;
        boolean mayAllocateMap = false;
        for (Map.Entry<StackState, ExactProbability> inputBranch : input.values().entrySet()) {
            DistributionalFunction1201.ExpectedEvaluation transformed =
                    DistributionalFunction1201.applyAllExpected(
                            inputBranch.getKey(),
                            functions,
                            context,
                            maxStates,
                            pointer,
                            predicates,
                            functionResolver);
            if (!transformed.supported()) {
                return ExpectedLayerEvaluation.unsupported(
                        transformed.pointer(), transformed.message(), transformed.failureKind());
            }
            mayAllocateMap |= transformed.mayAllocateMap();
            hasRandomCalls |= transformed.hasRandomCalls();
            for (Map.Entry<StackState, ExactProbability> branch :
                    transformed.distribution().masses().entrySet()) {
                result.add(branch.getKey(), inputBranch.getValue().multiply(branch.getValue()));
            }
            if (result.values().size() > maxStates) {
                return ExpectedLayerEvaluation.randomSemantics(
                        pointer,
                        "Expected stack state space "
                                + result.values().size()
                                + " exceeds limit "
                                + maxStates);
            }
        }
        return ExpectedLayerEvaluation.exact(
                result, hasRandomCalls, maxInputStacks, mayAllocateMap ? maxInputStacks : 0);
    }

    private static int cappedSum(int left, int right) {
        int sum = left + right;
        return sum <= 0 ? 0 : Math.min(2, sum);
    }

    private static TableEvaluation execute(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath,
            Set<Diagnostic> diagnostics) {
        Optional<RuntimeLootAstSource.RuntimeAst<LootTable>> resolved = source.table(tableId);
        if (resolved.isEmpty()) {
            diagnostics.add(ReferenceSemantics1201.missingTable(tableId, tableId, "", callPath));
            return TableEvaluation.exact(RandomTraceDistribution.singleton(List.of()));
        }
        Object identity = resolved.get().identity();
        if (!activeTables.add(identity)) {
            diagnostics.add(ReferenceSemantics1201.recursiveTable(tableId, tableId, "", callPath));
            return TableEvaluation.exact(RandomTraceDistribution.singleton(List.of()));
        }
        try {
            JsonElement serialized = resolved.get().json();
            if (!serialized.isJsonObject()) {
                return unsupported(
                        tableId, "", callPath, "Runtime table AST is not an object", diagnostics);
            }
            JsonObject table = serialized.getAsJsonObject();
            JsonElement poolsElement = table.get("pools");
            if (poolsElement != null && !poolsElement.isJsonArray()) {
                return unsupported(
                        tableId,
                        "/pools",
                        callPath,
                        "Loot table pools is not an array",
                        diagnostics);
            }
            JsonArray pools =
                    poolsElement == null ? new JsonArray() : poolsElement.getAsJsonArray();
            JsonElement tableFunctions = table.get("functions");
            RandomTraceDistribution<List<StackState>> current =
                    RandomTraceDistribution.singleton(List.of());
            PredicateResolver predicates =
                    new PredicateResolver(
                            source,
                            context,
                            maxStates,
                            activePredicates,
                            diagnostics,
                            tableId,
                            callPath);
            FunctionResolver functionResolver =
                    new FunctionResolver(
                            source,
                            context,
                            maxStates,
                            activePredicates,
                            activeFunctions,
                            diagnostics,
                            tableId,
                            callPath);
            for (int poolIndex = 0; poolIndex < pools.size(); poolIndex++) {
                String poolPointer = "/pools/" + poolIndex;
                JsonElement poolElement = pools.get(poolIndex);
                if (!poolElement.isJsonObject()) {
                    return unsupported(
                            tableId, poolPointer, callPath, "Pool is not an object", diagnostics);
                }
                JsonObject pool = poolElement.getAsJsonObject();
                JsonElement poolFunctions = pool.get("functions");
                DistributionalLootPool1201.ExecutionEvaluation<StackState> poolResult =
                        DistributionalLootPool1201.execute(
                                pool,
                                context,
                                maxStates,
                                DistributionalLootTableExecutor1201::expandTag,
                                predicates,
                                (selected, ignored) ->
                                        emitEntry(
                                                source,
                                                tableId,
                                                context,
                                                maxStates,
                                                activeTables,
                                                activePredicates,
                                                activeFunctions,
                                                callPath,
                                                selected,
                                                poolFunctions,
                                                poolPointer + "/functions",
                                                tableFunctions,
                                                "/functions",
                                                predicates,
                                                functionResolver,
                                                diagnostics),
                                poolPointer);
                if (!poolResult.supported()) {
                    return unsupported(
                            tableId,
                            poolResult.pointer(),
                            callPath,
                            poolResult.message(),
                            poolResult.failureKind(),
                            diagnostics);
                }
                try {
                    RandomTraceDistribution<List<StackState>> poolDistribution =
                            poolResult.distribution();
                    current =
                            current.flatMap(
                                    prior ->
                                            poolDistribution.flatMap(
                                                    generated -> {
                                                        ArrayList<StackState> outputs =
                                                                new ArrayList<>(prior);
                                                        outputs.addAll(generated);
                                                        return RandomTraceDistribution.singleton(
                                                                List.copyOf(outputs));
                                                    },
                                                    maxStates),
                                    maxStates);
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    return unsupported(
                            tableId,
                            poolPointer,
                            callPath,
                            exception.getMessage(),
                            EvaluationFailureKind.RANDOM_SEMANTICS,
                            diagnostics);
                }
            }
            return TableEvaluation.exact(current);
        } finally {
            activeTables.remove(identity);
        }
    }

    private static DistributionalLootPool1201.SelectionEvaluation<StackState> emitEntry(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            int maxStates,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath,
            DistributionalLootPool1201.SelectedEntry selected,
            JsonElement poolFunctionsElement,
            String poolFunctionsPointer,
            JsonElement tableFunctionsElement,
            String tableFunctionsPointer,
            PredicateResolver predicates,
            FunctionResolver functionResolver,
            Set<Diagnostic> diagnostics) {
        JsonObject entry = selected.entry();
        String pointer = selected.pointer();
        String type = stringField(entry, "type");
        if (type == null) {
            return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                    pointer + "/type", "Invalid emitted entry type");
        }
        TableEvaluation generated;
        if (type.equals("minecraft:empty")) {
            generated = TableEvaluation.exact(RandomTraceDistribution.singleton(List.of()));
        } else if (type.equals("minecraft:item")) {
            ResourceLocation itemId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                        pointer + "/name", "Missing item reference " + itemId);
            }
            generated =
                    TableEvaluation.exact(
                            RandomTraceDistribution.singleton(
                                    List.of(new StackState(new ItemStack(item)))));
        } else if (type.equals("minecraft:tag")) {
            ResourceLocation tagId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            if (tagId == null) {
                return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                        pointer + "/name", "Invalid item tag");
            }
            ArrayList<StackState> stacks = new ArrayList<>();
            for (Item item :
                    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId))) {
                stacks.add(new StackState(new ItemStack(item)));
            }
            generated =
                    TableEvaluation.exact(RandomTraceDistribution.singleton(List.copyOf(stacks)));
        } else if (type.equals("minecraft:loot_table")) {
            ResourceLocation nestedId =
                    entry.has("name")
                            ? ResourceLocation.tryParse(entry.get("name").getAsString())
                            : null;
            if (nestedId == null) {
                return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                        pointer + "/name", "Invalid nested table reference");
            }
            ArrayList<String> nestedPath = new ArrayList<>(callPath);
            nestedPath.add(nestedId.toString());
            generated =
                    execute(
                            source,
                            nestedId,
                            context,
                            maxStates,
                            activeTables,
                            activePredicates,
                            activeFunctions,
                            List.copyOf(nestedPath),
                            diagnostics);
            if (!generated.supported()) {
                return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                        pointer,
                        "Nested table contains an unsupported reachable mechanism",
                        generated.failureKind());
            }
        } else {
            return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                    pointer, "Unsupported emitted entry " + type);
        }

        boolean hasGeneratedStack =
                generated.distribution().marginal().masses().keySet().stream()
                        .anyMatch(stacks -> !stacks.isEmpty());
        if (!hasGeneratedStack) {
            return DistributionalLootPool1201.SelectionEvaluation.exact(generated.distribution());
        }
        JsonElement entryFunctionsElement = entry.get("functions");
        String invalidFunctions =
                invalidFunctionListPointer(
                        entryFunctionsElement,
                        pointer + "/functions",
                        poolFunctionsElement,
                        poolFunctionsPointer,
                        tableFunctionsElement,
                        tableFunctionsPointer);
        if (invalidFunctions != null) {
            String layer =
                    invalidFunctions.equals(pointer + "/functions")
                            ? "Loot entry"
                            : invalidFunctions.equals(poolFunctionsPointer)
                                    ? "Loot pool"
                                    : "Loot table";
            return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                    invalidFunctions, layer + " functions is not an array");
        }
        JsonArray entryFunctions = functionArray(entryFunctionsElement);
        JsonArray poolFunctions = functionArray(poolFunctionsElement);
        JsonArray tableFunctions = functionArray(tableFunctionsElement);
        Map<List<StackState>, RandomTraceDistribution<List<StackState>>> kernels =
                new LinkedHashMap<>();
        for (List<StackState> stacks : generated.distribution().marginal().masses().keySet()) {
            TableEvaluation processed =
                    processStacks(
                            stacks,
                            entryFunctions,
                            pointer + "/functions",
                            poolFunctions,
                            poolFunctionsPointer,
                            tableFunctions,
                            tableFunctionsPointer,
                            context,
                            maxStates,
                            predicates,
                            functionResolver);
            if (!processed.supported()) {
                return DistributionalLootPool1201.SelectionEvaluation.unsupported(
                        processed.pointer(), processed.message(), processed.failureKind());
            }
            kernels.put(stacks, processed.distribution());
        }
        try {
            return DistributionalLootPool1201.SelectionEvaluation.exact(
                    generated.distribution().flatMap(kernels::get, maxStates));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return DistributionalLootPool1201.SelectionEvaluation.randomSemantics(
                    pointer, exception.getMessage());
        }
    }

    private static TableEvaluation processStacks(
            List<StackState> stacks,
            JsonArray entryFunctions,
            String entryPointer,
            JsonArray poolFunctions,
            String poolPointer,
            JsonArray tableFunctions,
            String tablePointer,
            LootAnalysisContext context,
            int maxStates,
            PredicateResolver predicates,
            FunctionResolver functions) {
        RandomTraceDistribution<List<StackState>> current =
                RandomTraceDistribution.singleton(List.of());
        for (StackState stack : stacks) {
            DistributionalFunction1201.Evaluation transformed =
                    applyFunctionLayers(
                            stack,
                            entryFunctions,
                            entryPointer,
                            poolFunctions,
                            poolPointer,
                            tableFunctions,
                            tablePointer,
                            context,
                            maxStates,
                            predicates,
                            functions);
            if (!transformed.supported()) {
                return TableEvaluation.unsupported(
                        transformed.pointer(), transformed.message(), transformed.failureKind());
            }
            try {
                RandomTraceDistribution<StackState> stackDistribution = transformed.distribution();
                current =
                        current.flatMap(
                                outputs ->
                                        stackDistribution.flatMap(
                                                output -> {
                                                    ArrayList<StackState> next =
                                                            new ArrayList<>(outputs);
                                                    next.add(output);
                                                    return RandomTraceDistribution.singleton(
                                                            List.copyOf(next));
                                                },
                                                maxStates),
                                maxStates);
            } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                return TableEvaluation.randomSemantics(entryPointer, exception.getMessage());
            }
        }
        return TableEvaluation.exact(current);
    }

    private static DistributionalFunction1201.Evaluation applyFunctionLayers(
            StackState input,
            JsonArray entryFunctions,
            String entryPointer,
            JsonArray poolFunctions,
            String poolPointer,
            JsonArray tableFunctions,
            String tablePointer,
            LootAnalysisContext context,
            int maxStates,
            PredicateResolver predicates,
            FunctionResolver functions) {
        DistributionalFunction1201.Evaluation entry =
                DistributionalFunction1201.applyAll(
                        input,
                        entryFunctions,
                        context,
                        maxStates,
                        entryPointer,
                        predicates,
                        functions);
        if (!entry.supported()) return entry;
        LayerEvaluation pool =
                applyLayer(
                        entry.distribution(),
                        poolFunctions,
                        context,
                        maxStates,
                        poolPointer,
                        predicates,
                        functions);
        if (!pool.supported()) {
            return DistributionalFunction1201.Evaluation.unsupported(
                    pool.pointer(), pool.message(), pool.failureKind());
        }
        LayerEvaluation table =
                applyLayer(
                        pool.distribution(),
                        tableFunctions,
                        context,
                        maxStates,
                        tablePointer,
                        predicates,
                        functions);
        return table.supported()
                ? DistributionalFunction1201.Evaluation.exact(table.distribution())
                : DistributionalFunction1201.Evaluation.unsupported(
                        table.pointer(), table.message(), table.failureKind());
    }

    private static LayerEvaluation applyLayer(
            RandomTraceDistribution<StackState> input,
            JsonArray functions,
            LootAnalysisContext context,
            int maxStates,
            String pointer,
            PredicateResolver predicates,
            FunctionResolver functionResolver) {
        Map<StackState, RandomTraceDistribution<StackState>> kernels = new LinkedHashMap<>();
        for (StackState stack : input.marginal().masses().keySet()) {
            DistributionalFunction1201.Evaluation result =
                    DistributionalFunction1201.applyAll(
                            stack,
                            functions,
                            context,
                            maxStates,
                            pointer,
                            predicates,
                            functionResolver);
            if (!result.supported()) {
                return LayerEvaluation.unsupported(
                        result.pointer(), result.message(), result.failureKind());
            }
            kernels.put(stack, result.distribution());
        }
        try {
            return LayerEvaluation.exact(input.flatMap(kernels::get, maxStates));
        } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return LayerEvaluation.randomSemantics(pointer, exception.getMessage());
        }
    }

    private static List<JsonObject> expandTag(JsonObject entry) {
        String name = stringField(entry, "name");
        ResourceLocation tagId = name == null ? null : ResourceLocation.tryParse(name);
        if (tagId == null) return null;
        ArrayList<JsonObject> result = new ArrayList<>();
        for (Item item :
                ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId))) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;
            JsonObject expanded = entry.deepCopy();
            expanded.addProperty("type", "minecraft:item");
            expanded.addProperty("name", itemId.toString());
            expanded.remove("expand");
            result.add(expanded);
        }
        return List.copyOf(result);
    }

    private static JsonArray functions(JsonObject object) {
        return object.has("functions") && object.get("functions").isJsonArray()
                ? object.getAsJsonArray("functions")
                : new JsonArray();
    }

    private static JsonArray functionArray(JsonElement functions) {
        return functions == null ? new JsonArray() : functions.getAsJsonArray();
    }

    private static boolean isAbsentOrEmptyFunctionList(JsonElement functions) {
        return functions == null
                || (functions.isJsonArray() && functions.getAsJsonArray().isEmpty());
    }

    private static String invalidFunctionListPointer(
            JsonElement entryFunctions,
            String entryPointer,
            JsonElement poolFunctions,
            String poolPointer,
            JsonElement tableFunctions,
            String tablePointer) {
        if (entryFunctions != null && !entryFunctions.isJsonArray()) return entryPointer;
        if (poolFunctions != null && !poolFunctions.isJsonArray()) return poolPointer;
        if (tableFunctions != null && !tableFunctions.isJsonArray()) return tablePointer;
        return null;
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedTableMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable table AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static boolean isTerminalEnchantWithLevels(JsonArray functions) {
        if (functions.isEmpty()) return false;
        JsonElement element = functions.get(functions.size() - 1);
        if (!element.isJsonObject()) return false;
        JsonObject function = element.getAsJsonObject();
        return function.has("function")
                && function.get("function").isJsonPrimitive()
                && "minecraft:enchant_with_levels".equals(function.get("function").getAsString());
    }

    private static TableEvaluation unsupported(
            ResourceLocation tableId,
            String pointer,
            List<String> callPath,
            String message,
            Set<Diagnostic> diagnostics) {
        return unsupported(
                tableId,
                pointer,
                callPath,
                message,
                EvaluationFailureKind.UNSUPPORTED_TYPE,
                diagnostics);
    }

    private static TableEvaluation unsupported(
            ResourceLocation tableId,
            String pointer,
            List<String> callPath,
            String message,
            EvaluationFailureKind failureKind,
            Set<Diagnostic> diagnostics) {
        Diagnostic diagnostic =
                failureKind == EvaluationFailureKind.RANDOM_SEMANTICS
                        ? Diagnostic.randomSemantics(tableId, pointer, callPath, message)
                        : Diagnostic.unsupportedType(tableId, pointer, callPath, message);
        diagnostics.add(diagnostic);
        return TableEvaluation.unsupported(pointer, message, failureKind);
    }

    private static final class PredicateResolver
            implements DistributionalCondition1201.ReferenceResolver {
        private final RuntimeLootAstSource source;
        private final LootAnalysisContext context;
        private final int maxStates;
        private final Set<Object> active;
        private final Set<Diagnostic> diagnostics;
        private final ResourceLocation ownerTableId;
        private final List<String> callPath;

        private PredicateResolver(
                RuntimeLootAstSource source,
                LootAnalysisContext context,
                int maxStates,
                Set<Object> active,
                Set<Diagnostic> diagnostics,
                ResourceLocation ownerTableId,
                List<String> callPath) {
            this.source = source;
            this.context = context;
            this.maxStates = maxStates;
            this.active = active;
            this.diagnostics = diagnostics;
            this.ownerTableId = ownerTableId;
            this.callPath = List.copyOf(callPath);
        }

        @Override
        public DistributionalCondition1201.Evaluation resolve(
                ResourceLocation id, int ignored, String pointer) {
            var resolved = source.predicate(id);
            if (resolved.isEmpty()) {
                diagnostics.add(
                        ReferenceSemantics1201.missingPredicate(
                                id, ownerTableId, pointer, callPath));
                return DistributionalCondition1201.Evaluation.exact(
                        RandomTraceDistribution.singleton(false));
            }
            Object identity = resolved.get().identity();
            if (!active.add(identity)) {
                diagnostics.add(
                        ReferenceSemantics1201.recursivePredicate(
                                id, ownerTableId, pointer, callPath));
                return DistributionalCondition1201.Evaluation.exact(
                        RandomTraceDistribution.singleton(false));
            }
            try {
                return DistributionalCondition1201.test(
                        resolved.get().json(), context, maxStates, pointer, this);
            } finally {
                active.remove(identity);
            }
        }
    }

    private static final class FunctionResolver
            implements DistributionalFunction1201.FunctionReferenceResolver {
        private final RuntimeLootAstSource source;
        private final LootAnalysisContext context;
        private final int maxStates;
        private final Set<Object> activePredicates;
        private final Set<Object> activeFunctions;
        private final Set<Diagnostic> diagnostics;
        private final ResourceLocation ownerTableId;
        private final List<String> callPath;

        private FunctionResolver(
                RuntimeLootAstSource source,
                LootAnalysisContext context,
                int maxStates,
                Set<Object> activePredicates,
                Set<Object> activeFunctions,
                Set<Diagnostic> diagnostics,
                ResourceLocation ownerTableId,
                List<String> callPath) {
            this.source = source;
            this.context = context;
            this.maxStates = maxStates;
            this.activePredicates = activePredicates;
            this.activeFunctions = activeFunctions;
            this.diagnostics = diagnostics;
            this.ownerTableId = ownerTableId;
            this.callPath = List.copyOf(callPath);
        }

        @Override
        public DistributionalFunction1201.Evaluation resolve(
                ResourceLocation id, StackState input, int ignored, String pointer) {
            var resolved = source.modifier(id);
            if (resolved.isEmpty()) {
                diagnostics.add(
                        ReferenceSemantics1201.missingFunction(
                                id, ownerTableId, pointer, callPath));
                return DistributionalFunction1201.Evaluation.exact(
                        RandomTraceDistribution.singleton(input));
            }
            Object identity = resolved.get().identity();
            if (!activeFunctions.add(identity)) {
                diagnostics.add(
                        ReferenceSemantics1201.recursiveFunction(
                                id, ownerTableId, pointer, callPath));
                return DistributionalFunction1201.Evaluation.exact(
                        RandomTraceDistribution.singleton(input));
            }
            try {
                JsonElement json = resolved.get().json();
                JsonArray array;
                if (json.isJsonArray()) {
                    array = json.getAsJsonArray();
                } else {
                    array = new JsonArray();
                    array.add(json);
                }
                return DistributionalFunction1201.applyAll(
                        input,
                        array,
                        context,
                        maxStates,
                        pointer,
                        new PredicateResolver(
                                source,
                                context,
                                maxStates,
                                activePredicates,
                                diagnostics,
                                ownerTableId,
                                callPath),
                        this);
            } finally {
                activeFunctions.remove(identity);
            }
        }
    }

    public record LogicalResult(
            boolean supported,
            StackMeasure measure,
            TerminalStackMeasure terminalMeasure,
            boolean fullStackMeasureAvailable,
            boolean hasRandomCalls,
            List<Diagnostic> diagnostics) {
        public LogicalResult {
            diagnostics = List.copyOf(diagnostics);
        }

        private static LogicalResult unsupported(List<Diagnostic> diagnostics) {
            return new LogicalResult(
                    false,
                    new StackMeasure(),
                    TerminalStackMeasure.empty(),
                    false,
                    false,
                    diagnostics);
        }
    }

    /** One first-moment output, either a complete stack or a proven terminal valuation key. */
    private record ExpectedOutput(StackState full, TerminalStackKey terminal) {
        private ExpectedOutput {
            if ((full == null) == (terminal == null)) {
                throw new IllegalArgumentException("Expected output must have exactly one form");
            }
        }

        private static ExpectedOutput full(StackState state) {
            return new ExpectedOutput(java.util.Objects.requireNonNull(state), null);
        }

        private static ExpectedOutput terminal(TerminalStackKey key) {
            return new ExpectedOutput(null, java.util.Objects.requireNonNull(key));
        }
    }

    private record TableEvaluation(
            boolean supported,
            RandomTraceDistribution<List<StackState>> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static TableEvaluation exact(
                RandomTraceDistribution<List<StackState>> distribution) {
            return new TableEvaluation(true, distribution, "", "", null);
        }

        private static TableEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static TableEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new TableEvaluation(false, null, pointer, message, failureKind);
        }

        private static TableEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record LayerEvaluation(
            boolean supported,
            RandomTraceDistribution<StackState> distribution,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static LayerEvaluation exact(RandomTraceDistribution<StackState> distribution) {
            return new LayerEvaluation(true, distribution, "", "", null);
        }

        private static LayerEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static LayerEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new LayerEvaluation(false, null, pointer, message, failureKind);
        }

        private static LayerEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record ExpectedTableEvaluation(
            boolean supported,
            StackMeasure measure,
            TerminalStackMeasure terminalMeasure,
            boolean fullStackMeasureAvailable,
            boolean hasRandomCalls,
            int maxOutputs,
            int maxMapAllocations,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static ExpectedTableEvaluation exact(
                StackMeasure measure,
                TerminalStackMeasure terminalMeasure,
                boolean fullStackMeasureAvailable,
                boolean hasRandomCalls,
                int maxOutputs,
                int maxMapAllocations) {
            return new ExpectedTableEvaluation(
                    true,
                    measure,
                    terminalMeasure,
                    fullStackMeasureAvailable,
                    hasRandomCalls,
                    maxOutputs,
                    maxMapAllocations,
                    "",
                    "",
                    null);
        }

        private static ExpectedTableEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static ExpectedTableEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedTableEvaluation(
                    false,
                    new StackMeasure(),
                    TerminalStackMeasure.empty(),
                    false,
                    false,
                    0,
                    0,
                    pointer,
                    message,
                    failureKind);
        }

        private static ExpectedTableEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }

    private record ExpectedLayerEvaluation(
            boolean supported,
            StackMeasure measure,
            boolean hasRandomCalls,
            int maxOutputs,
            int maxMapAllocations,
            String pointer,
            String message,
            EvaluationFailureKind failureKind) {
        private static ExpectedLayerEvaluation exact(
                StackMeasure measure,
                boolean hasRandomCalls,
                int maxOutputs,
                int maxMapAllocations) {
            return new ExpectedLayerEvaluation(
                    true, measure, hasRandomCalls, maxOutputs, maxMapAllocations, "", "", null);
        }

        private static ExpectedLayerEvaluation unsupported(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.UNSUPPORTED_TYPE);
        }

        private static ExpectedLayerEvaluation unsupported(
                String pointer, String message, EvaluationFailureKind failureKind) {
            return new ExpectedLayerEvaluation(
                    false, new StackMeasure(), false, 0, 0, pointer, message, failureKind);
        }

        private static ExpectedLayerEvaluation randomSemantics(String pointer, String message) {
            return unsupported(pointer, message, EvaluationFailureKind.RANDOM_SEMANTICS);
        }
    }
}
