package com.suntide_20210418.dimensiontech.utils.loot.expectation;

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
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(initialState, "initialState");
        Objects.requireNonNull(tagExpander, "tagExpander");
        String pointer = poolPointer == null ? "" : poolPointer;

        StatefulCondition1201.Result conditions =
                StatefulCondition1201.testAll(pool.get("conditions"), context, initialState);
        if (conditions == null) {
            return Result.unsupported(
                    initialState, pointer + "/conditions", "Unsupported reachable pool condition");
        }
        if (!conditions.value()) return Result.exact(List.of(), conditions.randomState());

        StatefulNumberProvider1201.IntResult rolls =
                StatefulNumberProvider1201.getInt(pool.get("rolls"), context, conditions.randomState());
        if (rolls == null) {
            return Result.unsupported(
                    conditions.randomState(), pointer + "/rolls", "Unsupported reachable rolls provider");
        }
        StatefulNumberProvider1201.FloatResult bonus = pool.has("bonus_rolls")
                ? StatefulNumberProvider1201.getFloat(pool.get("bonus_rolls"), context, rolls.randomState())
                : new StatefulNumberProvider1201.FloatResult(0.0F, rolls.randomState());
        if (bonus == null) {
            return Result.unsupported(
                    rolls.randomState(),
                    pointer + "/bonus_rolls",
                    "Unsupported reachable bonus_rolls provider");
        }

        int rollCount = Math.max(0, rolls.value() + Mth.floor(bonus.value() * context.luck()));
        JsonArray entries = pool.has("entries") && pool.get("entries").isJsonArray()
                ? pool.getAsJsonArray("entries")
                : new JsonArray();
        ArrayList<JsonObject> selected = new ArrayList<>();
        XoroshiroState1201 state = bonus.randomState();
        for (int rollIndex = 0; rollIndex < rollCount; rollIndex++) {
            Expansion expansion = expandEntries(entries, context, state, tagExpander, pointer + "/entries");
            if (!expansion.supported()) {
                return Result.unsupported(expansion.randomState(), expansion.pointer(), expansion.message());
            }
            state = expansion.randomState();
            if (expansion.candidates().isEmpty()) continue;

            Candidate chosen;
            if (expansion.candidates().size() == 1) {
                chosen = expansion.candidates().get(0);
            } else {
                long totalLong = 0L;
                for (Candidate candidate : expansion.candidates()) totalLong += candidate.weight();
                if (totalLong > Integer.MAX_VALUE) {
                    return Result.unsupported(
                            state, pointer + "/entries", "Candidate total weight exceeds int range");
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
            selected.add(chosen.entry());
        }
        return Result.exact(selected, state);
    }

    private static Expansion expandEntries(
            JsonArray entries,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            TagExpander tagExpander,
            String pointer) {
        ArrayList<Candidate> candidates = new ArrayList<>();
        XoroshiroState1201 state = initialState;
        for (int index = 0; index < entries.size(); index++) {
            Expansion child = expand(
                    entries.get(index), context, state, tagExpander, pointer + "/" + index);
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
            String pointer) {
        if (element == null || !element.isJsonObject()) {
            return Expansion.unsupported(initialState, pointer, "Entry is not an object");
        }
        JsonObject entry = element.getAsJsonObject();
        StatefulCondition1201.Result conditions =
                StatefulCondition1201.testAll(entry.get("conditions"), context, initialState);
        if (conditions == null) {
            return Expansion.unsupported(
                    initialState, pointer + "/conditions", "Unsupported reachable entry condition");
        }
        if (!conditions.value()) return Expansion.exact(List.of(), conditions.randomState(), false);

        String type = entry.has("type") ? entry.get("type").getAsString() : "";
        if (type.endsWith(":item") || type.endsWith(":empty") || type.endsWith(":loot_table")) {
            return singleton(entry, context.luck(), conditions.randomState());
        }
        if (type.endsWith(":tag")) {
            if (!entry.has("expand") || !entry.get("expand").getAsBoolean()) {
                return singleton(entry, context.luck(), conditions.randomState());
            }
            List<JsonObject> expanded = tagExpander.expand(entry);
            if (expanded == null) {
                return Expansion.unsupported(
                        conditions.randomState(), pointer, "Unable to expand reachable item tag");
            }
            int weight = effectiveWeight(entry, context.luck());
            if (weight <= 0 || expanded.isEmpty()) {
                return Expansion.exact(List.of(), conditions.randomState(), true);
            }
            return Expansion.exact(
                    expanded.stream().map(value -> new Candidate(value, weight)).toList(),
                    conditions.randomState(),
                    true);
        }
        JsonArray children = entry.has("children") && entry.get("children").isJsonArray()
                ? entry.getAsJsonArray("children")
                : null;
        if (type.endsWith(":group") || type.endsWith(":sequence") || type.endsWith(":alternatives")) {
            if (children == null) {
                return Expansion.unsupported(
                        conditions.randomState(), pointer + "/children", "Composite entry has no children");
            }
            ArrayList<Candidate> candidates = new ArrayList<>();
            XoroshiroState1201 state = conditions.randomState();
            for (int index = 0; index < children.size(); index++) {
                Expansion child = expand(
                        children.get(index), context, state, tagExpander, pointer + "/children/" + index);
                if (!child.supported()) return child;
                state = child.randomState();
                candidates.addAll(child.candidates());
                if (type.endsWith(":sequence") && !child.expanded()) {
                    return Expansion.exact(List.of(), state, false);
                }
                if (type.endsWith(":alternatives") && child.expanded()) {
                    return Expansion.exact(candidates, state, true);
                }
            }
            return Expansion.exact(candidates, state, true);
        }
        return Expansion.unsupported(
                conditions.randomState(), pointer, "Unsupported reachable entry type " + type);
    }

    private static Expansion singleton(
            JsonObject entry, float luck, XoroshiroState1201 state) {
        int weight = effectiveWeight(entry, luck);
        return Expansion.exact(
                weight > 0 ? List.of(new Candidate(entry, weight)) : List.of(), state, true);
    }

    private static int effectiveWeight(JsonObject entry, float luck) {
        int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
        int quality = entry.has("quality") ? entry.get("quality").getAsInt() : 0;
        return Math.max(Mth.floor((float) weight + (float) quality * luck), 0);
    }

    @FunctionalInterface
    public interface TagExpander {
        List<JsonObject> expand(JsonObject tagEntry);
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

        private static Result unsupported(XoroshiroState1201 state, String pointer, String message) {
            return new Result(false, List.of(), state, pointer, message);
        }
    }

    private record Candidate(JsonObject entry, int weight) {}

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
