package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.ForgeRegistries;

/** Exact ordered table transition for an explicitly supplied Minecraft 1.20.1 RNG state. */
public final class StatefulLootTableExecutor1201 {
    private StatefulLootTableExecutor1201() {}

    public static Result execute(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            XoroshiroState1201 initialState) {
        java.util.function.Supplier<Result> execution =
                () ->
                        executeSafely(
                                new RuntimeLootAstSource(server),
                                tableId,
                                context,
                                initialState,
                                Collections.newSetFromMap(new IdentityHashMap<>()),
                                Collections.newSetFromMap(new IdentityHashMap<>()),
                                Collections.newSetFromMap(new IdentityHashMap<>()),
                                List.of(tableId.toString()));
        return context.level() instanceof ServerLevel level
                ? SavedDataTransaction1201.run(level, execution)
                : execution.get();
    }

    public static LootExpectationResult expectation(
            MinecraftServer server,
            ResourceLocation tableId,
            LootAnalysisContext context,
            FiniteDistribution<XoroshiroState1201> initialStates,
            int maxStates) {
        if (initialStates.masses().size() > maxStates) {
            return LootExpectationResult.unsupported(
                    "Initial RNG distribution exceeds state limit " + maxStates);
        }
        StackMeasure measure = new StackMeasure();
        LinkedHashSet<Diagnostic> diagnostics = new LinkedHashSet<>();
        int processed = 0;
        for (Map.Entry<XoroshiroState1201, ExactProbability> branch :
                initialStates.masses().entrySet()) {
            Result result = execute(server, tableId, context, branch.getKey());
            diagnostics.addAll(result.diagnostics());
            if (!result.supported()) {
                return new LootExpectationResult(
                        AnalysisStatus.UNSUPPORTED, measure, List.copyOf(diagnostics));
            }
            for (StackState output : result.outputs()) {
                measure.add(output, branch.getValue());
            }
            processed++;
            if (processed > maxStates) {
                diagnostics.add(
                        new Diagnostic(
                                "STATE_SPACE_LIMIT",
                                "Execution exceeded state limit " + maxStates));
                return new LootExpectationResult(
                        AnalysisStatus.UNSUPPORTED, measure, List.copyOf(diagnostics));
            }
        }
        return new LootExpectationResult(AnalysisStatus.EXACT, measure, List.copyOf(diagnostics));
    }

    private static Result execute(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath) {
        Optional<RuntimeLootAstSource.RuntimeAst<LootTable>> resolved = source.table(tableId);
        if (resolved.isEmpty()) {
            return Result.exact(
                    List.of(),
                    initialState,
                    List.of(ReferenceSemantics1201.missingTable(tableId, tableId, "", callPath)));
        }
        Object identity = resolved.get().identity();
        if (!activeTables.add(identity)) {
            return Result.exact(
                    List.of(),
                    initialState,
                    List.of(ReferenceSemantics1201.recursiveTable(tableId, tableId, "", callPath)));
        }
        try {
            JsonElement serialized = resolved.get().json();
            if (!serialized.isJsonObject()) {
                return Result.unsupported(
                        initialState,
                        List.of(),
                        Diagnostic.unsupportedType(
                                tableId, "", callPath, "Runtime table AST is not an object"));
            }
            JsonObject table = serialized.getAsJsonObject();
            JsonElement poolsElement = table.get("pools");
            if (poolsElement != null && !poolsElement.isJsonArray()) {
                return Result.unsupported(
                        initialState,
                        List.of(),
                        Diagnostic.unsupportedType(
                                tableId, "/pools", callPath, "Loot table pools is not an array"));
            }
            JsonArray pools =
                    poolsElement == null ? new JsonArray() : poolsElement.getAsJsonArray();
            JsonElement tableFunctions = table.get("functions");
            ArrayList<StackState> outputs = new ArrayList<>();
            ArrayList<Diagnostic> diagnostics = new ArrayList<>();
            PredicateResolver predicateResolver =
                    new PredicateResolver(
                            source, context, activePredicates, diagnostics, tableId, callPath);
            XoroshiroState1201 state = initialState;
            for (int poolIndex = 0; poolIndex < pools.size(); poolIndex++) {
                String poolPointer = "/pools/" + poolIndex;
                JsonElement poolElement = pools.get(poolIndex);
                if (!poolElement.isJsonObject()) {
                    return Result.unsupported(
                            state,
                            diagnostics,
                            Diagnostic.unsupportedType(
                                    tableId, poolPointer, callPath, "Pool is not an object"));
                }
                JsonObject pool = poolElement.getAsJsonObject();
                JsonElement poolFunctions = pool.get("functions");
                var poolResult =
                        StatefulLootPool1201.execute(
                                pool,
                                context,
                                state,
                                StatefulLootTableExecutor1201::expandTag,
                                predicateResolver,
                                (entry, entryPointer, entryState) ->
                                        emitEntry(
                                                source,
                                                tableId,
                                                context,
                                                activeTables,
                                                activePredicates,
                                                activeFunctions,
                                                callPath,
                                                entry,
                                                entryPointer,
                                                poolFunctions,
                                                poolPointer + "/functions",
                                                tableFunctions,
                                                "/functions",
                                                entryState,
                                                diagnostics),
                                poolPointer);
                if (!poolResult.supported()) {
                    diagnostics.add(
                            Diagnostic.unsupportedType(
                                    tableId, poolResult.pointer(), callPath, poolResult.message()));
                    return new Result(false, outputs, poolResult.randomState(), diagnostics);
                }
                outputs.addAll(poolResult.outputs());
                state = poolResult.randomState();
            }
            return Result.exact(outputs, state, diagnostics);
        } finally {
            activeTables.remove(identity);
        }
    }

    /** Keep malformed runtime ASTs inside the structured concrete-execution result. */
    private static Result executeSafely(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath) {
        try {
            return execute(
                    source,
                    tableId,
                    context,
                    initialState,
                    activeTables,
                    activePredicates,
                    activeFunctions,
                    callPath);
        } catch (RuntimeException exception) {
            return Result.unsupported(
                    initialState,
                    List.of(),
                    Diagnostic.unsupportedType(
                            tableId, "", callPath, malformedTableMessage(exception)));
        }
    }

    private static StatefulLootPool1201.SelectionResult<StackState> emitEntry(
            RuntimeLootAstSource source,
            ResourceLocation tableId,
            LootAnalysisContext context,
            Set<Object> activeTables,
            Set<Object> activePredicates,
            Set<Object> activeFunctions,
            List<String> callPath,
            JsonObject entry,
            String entryPointer,
            JsonElement poolFunctions,
            String poolFunctionsPointer,
            JsonElement tableFunctions,
            String tableFunctionsPointer,
            XoroshiroState1201 initialState,
            List<Diagnostic> diagnostics) {
        if (!entry.has("type")) {
            return unsupportedSelection(initialState, entryPointer, "Entry type is missing");
        }
        String type = stringField(entry, "type");
        if (type == null) {
            return unsupportedSelection(initialState, entryPointer, "Entry type is not a string");
        }
        if (type.equals("minecraft:empty")) {
            return StatefulLootPool1201.SelectionResult.exact(List.of(), initialState);
        }
        ArrayList<ItemStack> generated = new ArrayList<>();
        XoroshiroState1201 state = initialState;
        if (type.equals("minecraft:item")) {
            ResourceLocation itemId = resourceLocationField(entry, "name");
            if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
                return unsupportedSelection(
                        state, entryPointer + "/name", "Missing item reference " + itemId);
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            generated.add(new ItemStack(item));
        } else if (type.equals("minecraft:tag")) {
            ResourceLocation tagId = resourceLocationField(entry, "name");
            if (tagId == null) {
                return unsupportedSelection(state, entryPointer + "/name", "Invalid item tag");
            }
            for (Item item :
                    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId))) {
                generated.add(new ItemStack(item));
            }
        } else if (type.equals("minecraft:loot_table")) {
            ResourceLocation nestedId = resourceLocationField(entry, "name");
            if (nestedId == null) {
                return unsupportedSelection(
                        state, entryPointer + "/name", "Invalid nested table reference");
            }
            ArrayList<String> nestedPath = new ArrayList<>(callPath);
            nestedPath.add(nestedId.toString());
            Result nested =
                    executeSafely(
                            source,
                            nestedId,
                            context,
                            state,
                            activeTables,
                            activePredicates,
                            activeFunctions,
                            List.copyOf(nestedPath));
            diagnostics.addAll(nested.diagnostics());
            if (!nested.supported()) {
                Diagnostic failure =
                        nested.diagnostics().isEmpty()
                                ? Diagnostic.unsupportedType(
                                        tableId,
                                        entryPointer,
                                        callPath,
                                        "Nested table contains an unsupported mechanism")
                                : nested.diagnostics().get(nested.diagnostics().size() - 1);
                return StatefulLootPool1201.SelectionResult.unsupported(
                        nested.randomState(), failure.jsonPointer(), failure.message());
            }
            nested.outputs().forEach(output -> generated.add(output.stack()));
            state = nested.randomState();
        } else {
            return unsupportedSelection(
                    state, entryPointer, "Unsupported reachable emitted entry " + type);
        }

        ArrayList<StackState> outputs = new ArrayList<>();
        PredicateResolver predicateResolver =
                new PredicateResolver(
                        source, context, activePredicates, diagnostics, tableId, callPath);
        FunctionResolver functionResolver =
                new FunctionResolver(
                        source,
                        context,
                        activePredicates,
                        activeFunctions,
                        diagnostics,
                        tableId,
                        callPath);
        for (ItemStack stack : generated) {
            StatefulFunction1201.Result entryResult =
                    StatefulFunction1201.applyAll(
                            stack,
                            entry.get("functions"),
                            context,
                            state,
                            predicateResolver,
                            functionResolver,
                            entryPointer + "/functions");
            if (!entryResult.supported()) return unsupportedSelection(entryResult);
            StatefulFunction1201.Result poolResult =
                    StatefulFunction1201.applyAll(
                            entryResult.stack(),
                            poolFunctions,
                            context,
                            entryResult.randomState(),
                            predicateResolver,
                            functionResolver,
                            poolFunctionsPointer);
            if (!poolResult.supported()) return unsupportedSelection(poolResult);
            StatefulFunction1201.Result tableResult =
                    StatefulFunction1201.applyAll(
                            poolResult.stack(),
                            tableFunctions,
                            context,
                            poolResult.randomState(),
                            predicateResolver,
                            functionResolver,
                            tableFunctionsPointer);
            if (!tableResult.supported()) return unsupportedSelection(tableResult);
            outputs.add(new StackState(tableResult.stack()));
            state = tableResult.randomState();
        }
        return StatefulLootPool1201.SelectionResult.exact(outputs, state);
    }

    private static StatefulLootPool1201.SelectionResult<StackState> unsupportedSelection(
            StatefulFunction1201.Result result) {
        return unsupportedSelection(result.randomState(), result.pointer(), result.message());
    }

    private static StatefulLootPool1201.SelectionResult<StackState> unsupportedSelection(
            XoroshiroState1201 state, String pointer, String message) {
        return StatefulLootPool1201.SelectionResult.unsupported(state, pointer, message);
    }

    private static String malformedTableMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable table AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
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

    private static ResourceLocation resourceLocationField(JsonObject object, String name) {
        String value = stringField(object, name);
        if (value == null || value.isEmpty()) return null;
        try {
            return ResourceLocation.tryParse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static List<JsonObject> expandTag(JsonObject entry) {
        ResourceLocation tagId = resourceLocationField(entry, "name");
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

    private static final class PredicateResolver
            implements StatefulCondition1201.ReferenceResolver {
        private final RuntimeLootAstSource source;
        private final LootAnalysisContext context;
        private final Set<Object> activePredicates;
        private final List<Diagnostic> diagnostics;
        private final ResourceLocation ownerTableId;
        private final List<String> callPath;

        private PredicateResolver(
                RuntimeLootAstSource source,
                LootAnalysisContext context,
                Set<Object> activePredicates,
                List<Diagnostic> diagnostics,
                ResourceLocation ownerTableId,
                List<String> callPath) {
            this.source = source;
            this.context = context;
            this.activePredicates = activePredicates;
            this.diagnostics = diagnostics;
            this.ownerTableId = ownerTableId;
            this.callPath = List.copyOf(callPath);
        }

        @Override
        public StatefulCondition1201.Result resolve(
                ResourceLocation id, XoroshiroState1201 randomState) {
            return resolve(id, randomState, "");
        }

        @Override
        public StatefulCondition1201.Result resolve(
                ResourceLocation id, XoroshiroState1201 randomState, String pointer) {
            var resolved = source.predicate(id);
            if (resolved.isEmpty()) {
                diagnostics.add(
                        ReferenceSemantics1201.missingPredicate(
                                id, ownerTableId, pointer, callPath));
                return new StatefulCondition1201.Result(false, randomState);
            }
            Object identity = resolved.get().identity();
            if (!activePredicates.add(identity)) {
                diagnostics.add(
                        ReferenceSemantics1201.recursivePredicate(
                                id, ownerTableId, pointer, callPath));
                return new StatefulCondition1201.Result(false, randomState);
            }
            try {
                return StatefulCondition1201.test(
                        resolved.get().json(), context, randomState, this);
            } finally {
                activePredicates.remove(identity);
            }
        }
    }

    private static final class FunctionResolver
            implements StatefulFunction1201.FunctionReferenceResolver {
        private final RuntimeLootAstSource source;
        private final LootAnalysisContext context;
        private final Set<Object> activePredicates;
        private final Set<Object> activeFunctions;
        private final List<Diagnostic> diagnostics;
        private final ResourceLocation ownerTableId;
        private final List<String> callPath;

        private FunctionResolver(
                RuntimeLootAstSource source,
                LootAnalysisContext context,
                Set<Object> activePredicates,
                Set<Object> activeFunctions,
                List<Diagnostic> diagnostics,
                ResourceLocation ownerTableId,
                List<String> callPath) {
            this.source = source;
            this.context = context;
            this.activePredicates = activePredicates;
            this.activeFunctions = activeFunctions;
            this.diagnostics = diagnostics;
            this.ownerTableId = ownerTableId;
            this.callPath = List.copyOf(callPath);
        }

        @Override
        public StatefulFunction1201.Result resolve(
                ResourceLocation id, ItemStack input, XoroshiroState1201 randomState) {
            return resolve(id, input, randomState, "");
        }

        @Override
        public StatefulFunction1201.Result resolve(
                ResourceLocation id,
                ItemStack input,
                XoroshiroState1201 randomState,
                String pointer) {
            var resolved = source.modifier(id);
            if (resolved.isEmpty()) {
                diagnostics.add(
                        ReferenceSemantics1201.missingFunction(
                                id, ownerTableId, pointer, callPath));
                return new StatefulFunction1201.Result(true, input.copy(), randomState, "", "");
            }
            Object identity = resolved.get().identity();
            if (!activeFunctions.add(identity)) {
                diagnostics.add(
                        ReferenceSemantics1201.recursiveFunction(
                                id, ownerTableId, pointer, callPath));
                return new StatefulFunction1201.Result(true, input.copy(), randomState, "", "");
            }
            try {
                JsonElement json = resolved.get().json();
                JsonArray functions;
                if (json.isJsonArray()) {
                    functions = json.getAsJsonArray();
                } else {
                    functions = new JsonArray();
                    functions.add(json);
                }
                return StatefulFunction1201.applyAll(
                        input,
                        functions,
                        context,
                        randomState,
                        new PredicateResolver(
                                source,
                                context,
                                activePredicates,
                                diagnostics,
                                ownerTableId,
                                callPath),
                        this,
                        "");
            } finally {
                activeFunctions.remove(identity);
            }
        }
    }

    public record Result(
            boolean supported,
            List<StackState> outputs,
            XoroshiroState1201 randomState,
            List<Diagnostic> diagnostics) {
        public Result {
            outputs = List.copyOf(outputs);
            diagnostics = List.copyOf(diagnostics);
        }

        private static Result exact(
                List<StackState> outputs,
                XoroshiroState1201 randomState,
                List<Diagnostic> diagnostics) {
            return new Result(true, outputs, randomState, diagnostics);
        }

        private static Result unsupported(
                XoroshiroState1201 randomState, List<Diagnostic> diagnostics, Diagnostic failure) {
            ArrayList<Diagnostic> all = new ArrayList<>(diagnostics);
            all.add(failure);
            return new Result(false, List.of(), randomState, all);
        }
    }
}
