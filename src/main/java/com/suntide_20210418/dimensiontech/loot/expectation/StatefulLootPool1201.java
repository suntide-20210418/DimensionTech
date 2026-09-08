package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.Mth;

/** Ordered LootPool expansion and selection for one concrete 1.20.1 Xoroshiro state. */
public final class StatefulLootPool1201 {
    private StatefulLootPool1201() {}

    public static Result execute(
            JsonObject pool,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            String poolPointer) {
        ExecutionResult<JsonObject> result =
                execute(
                        pool,
                        context,
                        initialState,
                        tagExpander,
                        (entry, pointer, state) -> SelectionResult.exact(List.of(entry), state),
                        poolPointer);
        return new Result(
                result.supported(),
                result.outputs(),
                result.randomState(),
                result.pointer(),
                result.message());
    }

    public static <T> ExecutionResult<T> execute(
            JsonObject pool,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            SelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        return execute(
                pool, context, initialState, tagExpander, null, selectionExecutor, poolPointer);
    }

    public static <T> ExecutionResult<T> execute(
            JsonObject pool,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            StatefulCondition1201.ReferenceResolver conditionReferences,
            SelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        try {
            return executeUnchecked(
                    pool,
                    context,
                    initialState,
                    tagExpander,
                    conditionReferences,
                    selectionExecutor,
                    poolPointer);
        } catch (RuntimeException exception) {
            return ExecutionResult.unsupported(
                    initialState,
                    poolPointer == null ? "" : poolPointer,
                    malformedJsonMessage(exception));
        }
    }

    private static <T> ExecutionResult<T> executeUnchecked(
            JsonObject pool,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            StatefulCondition1201.ReferenceResolver conditionReferences,
            SelectionExecutor<T> selectionExecutor,
            String poolPointer) {
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(initialState, "initialState");
        Objects.requireNonNull(tagExpander, "tagExpander");
        Objects.requireNonNull(selectionExecutor, "selectionExecutor");
        String pointer = poolPointer == null ? "" : poolPointer;

        StatefulCondition1201.Result conditions =
                StatefulCondition1201.testAll(
                        pool.get("conditions"), context, initialState, conditionReferences);
        if (conditions == null) {
            return ExecutionResult.unsupported(
                    initialState, pointer + "/conditions", "Unsupported reachable pool condition");
        }
        if (!conditions.value()) return ExecutionResult.exact(List.of(), conditions.randomState());

        StatefulNumberProvider1201.IntResult rolls =
                StatefulNumberProvider1201.getInt(
                        pool.get("rolls"), context, conditions.randomState());
        if (rolls == null) {
            return ExecutionResult.unsupported(
                    conditions.randomState(),
                    pointer + "/rolls",
                    "Unsupported reachable rolls provider");
        }
        StatefulNumberProvider1201.FloatResult bonus =
                pool.has("bonus_rolls")
                        ? StatefulNumberProvider1201.getFloat(
                                pool.get("bonus_rolls"), context, rolls.randomState())
                        : new StatefulNumberProvider1201.FloatResult(0.0F, rolls.randomState());
        if (bonus == null) {
            return ExecutionResult.unsupported(
                    rolls.randomState(),
                    pointer + "/bonus_rolls",
                    "Unsupported reachable bonus_rolls provider");
        }

        int rollCount = Math.max(0, rolls.value() + Mth.floor(bonus.value() * context.luck()));
        JsonElement entriesElement = pool.get("entries");
        if (rollCount > 0 && entriesElement != null && !entriesElement.isJsonArray()) {
            return ExecutionResult.unsupported(
                    bonus.randomState(), pointer + "/entries", "Entry list is not an array");
        }
        // A malformed entries value is only reachable when at least one roll occurs.  Keep the
        // zero-roll path lazy, but never call getAsJsonArray() on a non-array value.
        JsonArray entries =
                entriesElement == null || !entriesElement.isJsonArray()
                        ? new JsonArray()
                        : entriesElement.getAsJsonArray();
        ArrayList<T> outputs = new ArrayList<>();
        XoroshiroState1201 state = bonus.randomState();
        for (int rollIndex = 0; rollIndex < rollCount; rollIndex++) {
            Expansion expansion =
                    expandEntries(
                            entries,
                            context,
                            state,
                            tagExpander,
                            conditionReferences,
                            pointer + "/entries");
            if (!expansion.supported()) {
                return ExecutionResult.unsupported(
                        expansion.randomState(), expansion.pointer(), expansion.message());
            }
            state = expansion.randomState();
            if (expansion.candidates().isEmpty()) continue;

            Candidate chosen;
            if (expansion.candidates().size() == 1) {
                chosen = expansion.candidates().get(0);
            } else {
                long totalLong = 0L;
                for (Candidate candidate : expansion.candidates()) {
                    if (candidate.weight() > Integer.MAX_VALUE - totalLong) {
                        return ExecutionResult.unsupported(
                                state,
                                pointer + "/entries",
                                "Candidate total weight exceeds int range");
                    }
                    totalLong += candidate.weight();
                }
                if (totalLong > Integer.MAX_VALUE) {
                    return ExecutionResult.unsupported(
                            state,
                            pointer + "/entries",
                            "Candidate total weight exceeds int range");
                }
                var draw = state.nextInt((int) totalLong);
                state = draw.state();
                int target = draw.value();
                chosen = expansion.candidates().get(expansion.candidates().size() - 1);
                for (Candidate candidate : expansion.candidates()) {
                    target -= candidate.weight();
                    if (target < 0) {
                        chosen = candidate;
                        break;
                    }
                }
            }
            SelectionResult<T> processed;
            try {
                processed = selectionExecutor.execute(chosen.entry(), chosen.pointer(), state);
            } catch (RuntimeException exception) {
                return ExecutionResult.unsupported(
                        state, chosen.pointer(), malformedSelectionMessage(exception));
            }
            if (processed == null) {
                return ExecutionResult.unsupported(
                        state, chosen.pointer(), "Selection executor returned no result");
            }
            if (!processed.supported()) {
                return ExecutionResult.unsupported(
                        processed.randomState(), processed.pointer(), processed.message());
            }
            outputs.addAll(processed.outputs());
            state = processed.randomState();
        }
        return ExecutionResult.exact(outputs, state);
    }

    private static Expansion expandEntries(
            JsonArray entries,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            StatefulCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        ArrayList<Candidate> candidates = new ArrayList<>();
        XoroshiroState1201 state = initialState;
        for (int index = 0; index < entries.size(); index++) {
            Expansion child =
                    expand(
                            entries.get(index),
                            context,
                            state,
                            tagExpander,
                            conditionReferences,
                            pointer + "/" + index);
            if (!child.supported()) return child;
            state = child.randomState();
            candidates.addAll(child.candidates());
        }
        return Expansion.exact(candidates, state, true);
    }

    private static Expansion expand(
            JsonElement element,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            StatefulCondition1201.ReferenceResolver conditionReferences,
            String pointer) {
        if (element == null || !element.isJsonObject()) {
            return Expansion.unsupported(initialState, pointer, "Entry is not an object");
        }
        JsonObject entry = element.getAsJsonObject();
        StatefulCondition1201.Result conditions =
                StatefulCondition1201.testAll(
                        entry.get("conditions"), context, initialState, conditionReferences);
        if (conditions == null) {
            return Expansion.unsupported(
                    initialState, pointer + "/conditions", "Unsupported reachable entry condition");
        }
        if (!conditions.value()) return Expansion.exact(List.of(), conditions.randomState(), false);

        WeightResult entryWeight = effectiveWeight(entry, context.luck(), pointer);
        if (!entryWeight.supported()) {
            return Expansion.unsupported(
                    conditions.randomState(), entryWeight.pointer(), entryWeight.message());
        }
        // A valid zero effective weight proves that the complete entry is unreachable.  Do not
        // inspect its type-specific fields (or invoke a tag expander) in that case.
        if (entryWeight.value() <= 0) {
            return Expansion.exact(List.of(), conditions.randomState(), true);
        }
        if (!entry.has("type")) {
            return Expansion.unsupported(
                    conditions.randomState(), pointer, "Entry type is missing");
        }
        String type = stringField(entry, "type");
        if (type == null) {
            return Expansion.unsupported(
                    conditions.randomState(), pointer, "Entry type is not a string");
        }
        if (type.equals("minecraft:item")
                || type.equals("minecraft:empty")
                || type.equals("minecraft:loot_table")) {
            if (!type.equals("minecraft:empty") && !validName(entry)) {
                return Expansion.unsupported(
                        conditions.randomState(),
                        pointer + "/name",
                        "Entry name is missing or not a string");
            }
            return Expansion.exact(
                    List.of(new Candidate(entry, entryWeight.value(), pointer)),
                    conditions.randomState(),
                    true);
        }
        if (type.equals("minecraft:tag")) {
            Boolean expand = booleanField(entry, "expand", false);
            if (expand == null) {
                return Expansion.unsupported(
                        conditions.randomState(),
                        pointer + "/expand",
                        "Entry expand is not a boolean");
            }
            if (!expand) {
                if (!validName(entry)) {
                    return Expansion.unsupported(
                            conditions.randomState(),
                            pointer + "/name",
                            "Entry name is missing or not a string");
                }
                return Expansion.exact(
                        List.of(new Candidate(entry, entryWeight.value(), pointer)),
                        conditions.randomState(),
                        true);
            }
            if (!validName(entry)) {
                return Expansion.unsupported(
                        conditions.randomState(),
                        pointer + "/name",
                        "Entry name is missing or not a string");
            }
            List<JsonObject> expanded;
            try {
                expanded = tagExpander.expand(entry);
            } catch (RuntimeException exception) {
                return Expansion.unsupported(
                        conditions.randomState(),
                        pointer + "/name",
                        malformedTagMessage(exception));
            }
            if (expanded == null) {
                return Expansion.unsupported(
                        conditions.randomState(), pointer, "Unable to expand reachable item tag");
            }
            for (JsonObject value : expanded) {
                if (value == null) {
                    return Expansion.unsupported(
                            conditions.randomState(),
                            pointer,
                            "Tag expansion returned a null entry");
                }
            }
            if (expanded.isEmpty()) {
                return Expansion.exact(List.of(), conditions.randomState(), true);
            }
            return Expansion.exact(
                    expanded.stream()
                            .map(value -> new Candidate(value, entryWeight.value(), pointer))
                            .toList(),
                    conditions.randomState(),
                    true);
        }
        JsonArray children =
                entry.has("children") && entry.get("children").isJsonArray()
                        ? entry.getAsJsonArray("children")
                        : null;
        if (type.equals("minecraft:group")
                || type.equals("minecraft:sequence")
                || type.equals("minecraft:alternatives")) {
            if (children == null) {
                return Expansion.unsupported(
                        conditions.randomState(),
                        pointer + "/children",
                        entry.has("children")
                                ? "Composite entry children is not an array"
                                : "Composite entry has no children");
            }
            ArrayList<Candidate> candidates = new ArrayList<>();
            XoroshiroState1201 state = conditions.randomState();
            for (int index = 0; index < children.size(); index++) {
                Expansion child =
                        expand(
                                children.get(index),
                                context,
                                state,
                                tagExpander,
                                conditionReferences,
                                pointer + "/children/" + index);
                if (!child.supported()) return child;
                state = child.randomState();
                candidates.addAll(child.candidates());
                if (type.equals("minecraft:sequence") && !child.expanded()) {
                    return Expansion.exact(candidates, state, false);
                }
                if (type.equals("minecraft:alternatives") && child.expanded()) {
                    return Expansion.exact(candidates, state, true);
                }
            }
            return Expansion.exact(candidates, state, true);
        }
        return Expansion.unsupported(
                conditions.randomState(), pointer, "Unsupported reachable entry type " + type);
    }

    private static boolean validName(JsonObject entry) {
        String name = stringField(entry, "name");
        return name != null && !name.isEmpty();
    }

    private static WeightResult effectiveWeight(JsonObject entry, float luck, String pointer) {
        Integer weight = integerField(entry, "weight", 1);
        if (weight == null) {
            return WeightResult.unsupported(pointer + "/weight", "Entry weight is not an integer");
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
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Boolean booleanField(JsonObject object, String name, boolean defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            return null;
        try {
            return value.getAsBoolean();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Integer integerField(JsonObject object, String name, int defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            return null;
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable pool AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static String malformedTagMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable tag expansion ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    private static String malformedSelectionMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Selection executor failed ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    @FunctionalInterface
    public interface TagExpander {
        List<JsonObject> expand(JsonObject tagEntry);
    }

    @FunctionalInterface
    public interface SelectionExecutor<T> {
        SelectionResult<T> execute(
                JsonObject selectedEntry, String entryPointer, XoroshiroState1201 randomState);
    }

    public record SelectionResult<T>(
            boolean supported,
            List<T> outputs,
            XoroshiroState1201 randomState,
            String pointer,
            String message) {
        public SelectionResult {
            outputs = List.copyOf(outputs);
        }

        public static <T> SelectionResult<T> exact(
                List<T> outputs, XoroshiroState1201 randomState) {
            return new SelectionResult<>(true, outputs, randomState, "", "");
        }

        public static <T> SelectionResult<T> unsupported(
                XoroshiroState1201 randomState, String pointer, String message) {
            return new SelectionResult<>(false, List.of(), randomState, pointer, message);
        }
    }

    public record ExecutionResult<T>(
            boolean supported,
            List<T> outputs,
            XoroshiroState1201 randomState,
            String pointer,
            String message) {
        public ExecutionResult {
            outputs = List.copyOf(outputs);
        }

        private static <T> ExecutionResult<T> exact(
                List<T> outputs, XoroshiroState1201 randomState) {
            return new ExecutionResult<>(true, outputs, randomState, "", "");
        }

        private static <T> ExecutionResult<T> unsupported(
                XoroshiroState1201 randomState, String pointer, String message) {
            return new ExecutionResult<>(false, List.of(), randomState, pointer, message);
        }
    }

    public record Result(
            boolean supported,
            List<JsonObject> selectedEntries,
            XoroshiroState1201 randomState,
            String pointer,
            String message) {
        private static Result exact(List<JsonObject> entries, XoroshiroState1201 state) {
            return new Result(true, List.copyOf(entries), state, "", "");
        }

        private static Result unsupported(
                XoroshiroState1201 state, String pointer, String message) {
            return new Result(false, List.of(), state, pointer, message);
        }
    }

    private record Candidate(JsonObject entry, int weight, String pointer) {}

    private record WeightResult(boolean supported, int value, String pointer, String message) {
        private static WeightResult exact(int value) {
            return new WeightResult(true, value, "", "");
        }

        private static WeightResult unsupported(String pointer, String message) {
            return new WeightResult(false, 0, pointer, message);
        }
    }

    private record Expansion(
            boolean supported,
            List<Candidate> candidates,
            XoroshiroState1201 randomState,
            boolean expanded,
            String pointer,
            String message) {
        private static Expansion exact(
                List<Candidate> candidates, XoroshiroState1201 state, boolean expanded) {
            return new Expansion(true, List.copyOf(candidates), state, expanded, "", "");
        }

        private static Expansion unsupported(
                XoroshiroState1201 state, String pointer, String message) {
            return new Expansion(false, List.of(), state, false, pointer, message);
        }
    }
}
