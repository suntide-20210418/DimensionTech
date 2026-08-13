package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.scores.Team;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import java.util.UUID;

/** Exact expectation evaluator for the JSON subset used by simple vanilla pools. */
public final class JsonLootTableExecutor {
    private JsonLootTableExecutor() {}

    public static LootExpectationResult evaluate(
            MinecraftServer server, ResourceLocation id, LootAnalysisContext context) {
        Set<Object> activeTables = Collections.newSetFromMap(new IdentityHashMap<>());
        return evaluate(new RuntimeLootAstSource(server), id, context, activeTables);
    }

    public static LootExpectationResult evaluate(ResourceManager manager, ResourceLocation id, float luck) {
        return evaluate(manager, id, new LootAnalysisContext(null, null, null, luck, Map.of(), null, null, ItemStack.EMPTY));
    }

    public static LootExpectationResult evaluate(ResourceManager manager, ResourceLocation id,
                                                 LootAnalysisContext context) {
        return LootExpectationResult.unsupported(
                "Runtime LootData is required to analyze " + id);
    }

    private static LootExpectationResult evaluate(RuntimeLootAstSource source, ResourceLocation id,
                                                  LootAnalysisContext context,
                                                  Set<Object> active) {
        Optional<RuntimeLootAstSource.RuntimeAst<net.minecraft.world.level.storage.loot.LootTable>> found =
                source.table(id);
        if (found.isEmpty()) {
            return ReferenceSemantics1201.emptyTable(ReferenceSemantics1201.missingTable(id));
        }
        Object tableIdentity = found.get().identity();
        if (!active.add(tableIdentity)) {
            return ReferenceSemantics1201.emptyTable(ReferenceSemantics1201.recursiveTable(id));
        }
        ResourceManager manager = source.resources();
        try {
            JsonObject table = found.get().json().getAsJsonObject();
            StackMeasure measure = new StackMeasure();
            List<Diagnostic> diagnostics = new ArrayList<>();
            boolean unsupported = false;
            if (table.has("random_sequence")) {
                diagnostics.add(Diagnostic.randomSemantics(
                        id,
                        "/random_sequence",
                        List.of(id.toString()),
                        "Persistent random_sequence state has no probability distribution "
                                + "defined by Minecraft 1.20.1 runtime semantics"));
                active.remove(tableIdentity);
                return new LootExpectationResult(
                        AnalysisStatus.UNSUPPORTED, measure, List.copyOf(diagnostics));
            }
            JsonArray pools = table.getAsJsonArray("pools");
            if (pools == null) {
                active.remove(tableIdentity);
                AnalysisStatus emptyStatus = unsupported ? AnalysisStatus.UNSUPPORTED : AnalysisStatus.EXACT;
                return new LootExpectationResult(emptyStatus, measure, List.copyOf(diagnostics));
            }
            for (int poolIndex = 0; poolIndex < pools.size(); poolIndex++) {
                JsonElement poolElement = pools.get(poolIndex);
                JsonObject pool = poolElement.getAsJsonObject();
                String poolPointer = "/pools/" + poolIndex;
                StackMeasure poolMeasure = new StackMeasure();
                ExactProbability conditionProbability = conditionProbability(
                        pool.get("conditions"), context, source, identitySet(), diagnostics);
                if (conditionProbability == null) {
                    diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Pool conditions in " + id));
                    unsupported = true;
                    continue;
                }
                if (conditionProbability.isZero()) continue;
                Map<Integer, ExactProbability> rollDistribution = integerDistribution(pool.get("rolls"), context);
                if (rollDistribution == null) {
                    diagnostics.add(Diagnostic.unsupportedType(
                            id, poolPointer + "/rolls", List.of(id.toString()),
                            "Unsupported reachable rolls provider with mass " + conditionProbability));
                    unsupported = true;
                    continue;
                }
                if (pool.has("bonus_rolls")) {
                    Map<Integer, ExactProbability> bonusDistribution = bonusDistribution(pool.get("bonus_rolls"), context.luck());
                    if (bonusDistribution == null) {
                        diagnostics.add(Diagnostic.unsupportedType(
                                id, poolPointer + "/bonus_rolls", List.of(id.toString()),
                                "Unsupported reachable bonus_rolls provider with mass " + conditionProbability));
                        unsupported = true;
                        continue;
                    }
                    rollDistribution = convolve(rollDistribution, bonusDistribution);
                }
                rollDistribution = clampNonNegative(rollDistribution);
                JsonArray entries = pool.getAsJsonArray("entries");
                if (entries == null || entries.isEmpty()) continue;
                JsonArray poolFunctions = pool.has("functions")
                                && pool.get("functions").isJsonArray()
                        ? pool.getAsJsonArray("functions")
                        : new JsonArray();
                for (ExactProbability rollInbound : positiveRollInboundMasses(rollDistribution)) {
                if (rollInbound.isZero()) continue;
                List<List<JsonObject>> candidateGroups = new ArrayList<>();
                Map<JsonObject, Integer> effectiveWeights = new IdentityHashMap<>();
                List<ExactProbability> inclusionProbabilities = new ArrayList<>();
                for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                    JsonElement entryElement = entries.get(entryIndex);
                    JsonObject entry = entryElement.getAsJsonObject();
                    String type = entry.has("type") ? entry.get("type").getAsString() : "";
                    if (!("minecraft:item".equals(type) || "minecraft:empty".equals(type)
                            || "minecraft:tag".equals(type) || "minecraft:loot_table".equals(type)
                            || "minecraft:group".equals(type) || "minecraft:sequence".equals(type)
                            || "minecraft:alternatives".equals(type))) {
                        ExactProbability reachability = ExactProbability.ONE;
                        if (entry.has("conditions")) {
                            reachability = conditionProbability(
                                    entry.get("conditions"), context, source, identitySet(), diagnostics);
                        }
                        if (reachability != null && reachability.isZero()) continue;
                        diagnostics.add(Diagnostic.unsupportedType(
                                id, poolPointer + "/entries/" + entryIndex, List.of(id.toString()),
                                "Unsupported reachable entry type " + type));
                        unsupported = true;
                        continue;
                    }
                    ExactProbability inclusion = ExactProbability.ONE;
                    if (entry.has("conditions") && entry.getAsJsonArray("conditions").size() > 0) {
                        inclusion = conditionProbability(
                                entry.get("conditions"), context, source, identitySet(), diagnostics);
                        if (inclusion == null) {
                            diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Entry conditions in " + id));
                            unsupported = true;
                            continue;
                        }
                        if (inclusion.isZero()) continue;
                    }
                    int baseWeight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                    double quality = entry.has("quality") ? entry.get("quality").getAsDouble() : 0.0D;
                    int weight = Math.max(0, (int) Math.floor(baseWeight + quality * context.luck()));
                    List<JsonObject> group = weight > 0 ? expandTagCandidates(entry) : List.of();
                    for (JsonObject candidate : group) effectiveWeights.put(candidate, weight);
                    candidateGroups.add(group);
                    inclusionProbabilities.add(inclusion);
                }
                FiniteDistribution<List<JsonObject>> candidateLists;
                try {
                    candidateLists = expandCandidateGroups(
                            candidateGroups, inclusionProbabilities, 1_000_000);
                } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                    diagnostics.add(new Diagnostic("STATE_SPACE_LIMIT", exception.getMessage()));
                    unsupported = true;
                    continue;
                }
                for (Map.Entry<List<JsonObject>, ExactProbability> candidateState
                        : candidateLists.masses().entrySet()) {
                if (candidateState.getKey().isEmpty()) continue;
                List<Integer> weights = candidateState.getKey().stream()
                        .map(entry -> effectiveWeights.getOrDefault(entry, 0)).toList();
                ExactRandomSemantics1201.RandomResult<Integer> selection;
                try {
                    selection = ExactRandomSemantics1201.weightedIndex(weights);
                } catch (IllegalArgumentException exception) {
                    diagnostics.add(new Diagnostic(
                            "RANDOM_SEMANTICS", "Invalid candidate weights in " + id));
                    unsupported = true;
                    continue;
                }
                for (Map.Entry<Integer, ExactProbability> selected
                        : selection.distribution().masses().entrySet()) {
                    JsonObject entry = candidateState.getKey().get(selected.getKey());
                    if ("minecraft:empty".equals(entry.get("type").getAsString())) continue;
                    ExactProbability occurrence = rollInbound
                            .multiply(candidateState.getValue())
                            .multiply(selected.getValue());
                    StackMeasure selectedMeasure = new StackMeasure();
                    if (!conditionProbability.equals(ExactProbability.ONE)) {
                        occurrence = occurrence.multiply(conditionProbability);
                    }
                    String entryType = entry.get("type").getAsString();
                    if (entryType.endsWith(":group") || entryType.endsWith(":sequence")
                            || entryType.endsWith(":alternatives")) {
                        if (!emitComposite(source, active, context, entry, occurrence, selectedMeasure, diagnostics, id)) {
                            unsupported = true;
                        }
                    } else if ("minecraft:loot_table".equals(entry.get("type").getAsString())) {
                        ResourceLocation nested = ResourceLocation.tryParse(entry.get("name").getAsString());
                        if (nested == null) { diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Invalid nested table")); continue; }
                        LootExpectationResult nestedResult = evaluate(source, nested, context, active);
                        StackMeasure nestedMeasure = new StackMeasure();
                        nestedMeasure.addAll(nestedResult.measure(), occurrence);
                        if (entry.has("functions") && entry.getAsJsonArray("functions").size() > 0) {
                            if (!applyFunctions(nestedMeasure, entry.getAsJsonArray("functions"), context, source, diagnostics, id, identitySet())) {
                                unsupported = true;
                            }
                        }
                        selectedMeasure.addAll(nestedMeasure, ExactProbability.ONE);
                        diagnostics.addAll(nestedResult.diagnostics());
                        if (nestedResult.status() != AnalysisStatus.EXACT) {
                            unsupported = true;
                            diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Nested loot table contains unsupported mechanism"));
                        }
                    } else if ("minecraft:tag".equals(entry.get("type").getAsString())) {
                        ResourceLocation tagId = ResourceLocation.tryParse(entry.get("name").getAsString());
                        if (tagId == null) { diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Invalid item tag")); continue; }
                        var holders = ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId));
                        for (Item taggedItem : holders) {
                            if (!addItemOutput(selectedMeasure, source, context, diagnostics, entry, taggedItem, occurrence, id)) {
                                unsupported = true;
                            }
                        }
                    } else if ("minecraft:item".equals(entry.get("type").getAsString())) {
                        ResourceLocation itemId = ResourceLocation.tryParse(entry.get("name").getAsString());
                        Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
                        if (item == null || item == net.minecraft.world.item.Items.AIR) {
                            diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Unknown item " + entry.get("name")));
                            unsupported = true;
                            continue;
                        }
                        if (!addItemOutput(selectedMeasure, source, context, diagnostics, entry, item, occurrence, id)) {
                            unsupported = true;
                        }
                    }
                    if (!poolFunctions.isEmpty()
                            && !applyFunctions(
                                    selectedMeasure,
                                    poolFunctions,
                                    context,
                                    source,
                                    diagnostics,
                                    id,
                                    identitySet())) {
                        unsupported = true;
                    }
                    poolMeasure.addAll(selectedMeasure, ExactProbability.ONE);
                }
                }
                }
                measure.addAll(poolMeasure, ExactProbability.ONE);
            }
            if (table.has("functions") && table.getAsJsonArray("functions").size() > 0) {
                if (!applyFunctions(measure, table.getAsJsonArray("functions"), context, source, diagnostics, id, identitySet())) {
                    unsupported = true;
                }
            }
            // This compatibility executor still reads resource JSON and marginalizes random calls.
            // It must never certify an analysis as exact until execution is driven by the loaded
            // LootData AST and an exact, ordered RandomSource state transition.
            diagnostics.add(new Diagnostic(
                    "RANDOM_SEMANTICS",
                    "Legacy JSON executor cannot certify exact RandomSource semantics for " + id));
            AnalysisStatus status = AnalysisStatus.UNSUPPORTED;
            active.remove(tableIdentity);
            return new LootExpectationResult(status, measure, List.copyOf(diagnostics));
        } catch (Exception e) {
            active.remove(tableIdentity);
            return LootExpectationResult.unsupported("Invalid loot table " + id + ": " + e.getClass().getSimpleName());
        }
    }

    private static void statusUnsupported(List<Diagnostic> diagnostics) {
        diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", "Nested loot table contains unsupported mechanism"));
    }

    private static boolean emitComposite(RuntimeLootAstSource source, Set<Object> active, LootAnalysisContext context,
                                            JsonObject entry, ExactProbability occurrence, StackMeasure measure,
                                            List<Diagnostic> diagnostics, ResourceLocation id) {
        ResourceManager manager = source.resources();
        JsonArray children = entry.getAsJsonArray("children");
        if (children == null) return false;
        JsonArray compositeFunctions = entry.has("functions") && entry.get("functions").isJsonArray()
                ? entry.getAsJsonArray("functions") : new JsonArray();
        StackMeasure childMeasure = new StackMeasure();
        String type = entry.get("type").getAsString();
        if (type.endsWith(":alternatives")) {
            for (JsonElement child : children) {
                JsonObject object = child.getAsJsonObject();
                ExactProbability p = conditionProbability(
                        object.get("conditions"), context, source, identitySet(), diagnostics);
                if (p == null) return false;
                if (p.isZero()) continue;
                if (!p.equals(ExactProbability.ONE)) return false;
                boolean emitted = emitSimpleEntry(source, active, context, object, occurrence, childMeasure, diagnostics, id);
                if (!emitted) return false;
                break;
            }
            if (!compositeFunctions.isEmpty()
                    && !applyFunctions(childMeasure, compositeFunctions, context, source, diagnostics, id, identitySet())) return false;
            measure.addAll(childMeasure, ExactProbability.ONE);
            return true;
        }
        if (type.endsWith(":sequence")) {
            for (JsonElement child : children) {
                JsonObject object = child.getAsJsonObject();
                String childType = object.has("type") ? object.get("type").getAsString() : "";
                if ((!childType.endsWith(":item") && !childType.endsWith(":empty"))) return false;
            }
            boolean emitted = false;
            for (JsonElement child : children) {
                JsonObject object = child.getAsJsonObject();
                ExactProbability childProbability = conditionProbability(
                        object.get("conditions"), context, source, identitySet(), diagnostics);
                if (childProbability == null
                        || (!childProbability.isZero()
                                && !childProbability.equals(ExactProbability.ONE))) return false;
                if (childProbability.isZero()) continue;
                String childType = object.get("type").getAsString();
                if (childType.endsWith(":empty")) continue;
                if (!emitSimpleEntry(source, active, context, object, occurrence, childMeasure, diagnostics, id)) {
                    return false;
                }
                emitted = true;
            }
            if (!emitted && !children.isEmpty()) return false;
            if (!compositeFunctions.isEmpty()
                    && !applyFunctions(childMeasure, compositeFunctions, context, source, diagnostics, id, identitySet())) return false;
            measure.addAll(childMeasure, ExactProbability.ONE);
            return true;
        }
        for (JsonElement child : children) {
            JsonObject object = child.getAsJsonObject();
            ExactProbability childProbability = conditionProbability(
                    object.get("conditions"), context, source, identitySet(), diagnostics);
            if (childProbability == null
                    || (!childProbability.isZero()
                            && !childProbability.equals(ExactProbability.ONE))) return false;
            if (childProbability.isZero()) continue;
            if (!emitSimpleEntry(source, active, context, object, occurrence,
                    childMeasure, diagnostics, id)) return false;
        }
        if (!compositeFunctions.isEmpty()
                && !applyFunctions(childMeasure, compositeFunctions, context, source, diagnostics, id, identitySet())) return false;
        measure.addAll(childMeasure, ExactProbability.ONE);
        return true;
    }

    private static boolean emitSimpleEntry(RuntimeLootAstSource source, Set<Object> active, LootAnalysisContext context,
                                            JsonObject entry, ExactProbability occurrence, StackMeasure measure,
                                            List<Diagnostic> diagnostics, ResourceLocation id) {
        ResourceManager manager = source.resources();
        ExactProbability entryProbability = conditionProbability(
                entry.get("conditions"), context, source, identitySet(), diagnostics);
        if (entryProbability == null
                || (!entryProbability.isZero()
                        && !entryProbability.equals(ExactProbability.ONE))) return false;
        if (entryProbability.isZero()) return true;
        String type = entry.has("type") ? entry.get("type").getAsString() : "";
        if (type.endsWith(":group") || type.endsWith(":sequence") || type.endsWith(":alternatives")) {
            return emitComposite(source, active, context, entry, occurrence, measure, diagnostics, id);
        }
        if (type.endsWith(":item")) {
            ResourceLocation itemId = ResourceLocation.tryParse(entry.get("name").getAsString());
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            return item != null && item != net.minecraft.world.item.Items.AIR
                    && addItemOutput(measure, source, context, diagnostics, entry, item, occurrence, id);
        }
        if (type.endsWith(":loot_table")) {
            ResourceLocation nested = ResourceLocation.tryParse(entry.get("name").getAsString());
            if (nested == null) return false;
            LootExpectationResult result = evaluate(source, nested, context, active);
            StackMeasure nestedMeasure = new StackMeasure();
            nestedMeasure.addAll(result.measure(), occurrence);
            if (entry.has("functions") && entry.getAsJsonArray("functions").size() > 0
                    && !applyFunctions(nestedMeasure, entry.getAsJsonArray("functions"), context, source, diagnostics, id, identitySet())) {
                return false;
            }
            measure.addAll(nestedMeasure, ExactProbability.ONE);
            diagnostics.addAll(result.diagnostics());
            return result.status() == AnalysisStatus.EXACT;
        }
        if (type.endsWith(":empty")) return true;
        return false;
    }

    private static double numberMean(JsonElement element) {
        if (element == null) return 0.0D;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) return element.getAsDouble();
        if (!element.isJsonObject()) return Double.NaN;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) return object.get("value").getAsDouble();
        if (type.endsWith(":uniform")) return (object.get("min").getAsDouble() + object.get("max").getAsDouble()) / 2.0D;
        if (type.endsWith(":binomial")) {
            int n = object.get("n").getAsInt();
            double p = object.get("p").getAsDouble();
            if (n < 0 || p < 0.0D || p > 1.0D) return Double.NaN;
            return n * p;
        }
        if (type.endsWith(":score")) return Double.NaN;
        return Double.NaN;
    }

    private static Map<Integer, ExactProbability> integerDistribution(
            JsonElement element, LootAnalysisContext context) {
        // LootPool.Builder defaults rolls to ConstantValue.exactly(1).
        if (element == null) return Map.of(1, ExactProbability.ONE);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return Map.of(constantIntValue(element), ExactProbability.ONE);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) {
            return Map.of(constantIntValue(object.get("value")), ExactProbability.ONE);
        }
        if (type.endsWith(":uniform")) {
            Integer min = constantProviderInt(object.get("min"));
            Integer max = constantProviderInt(object.get("max"));
            if (min == null || max == null) return null;
            if (min >= max) return Map.of(min, ExactProbability.ONE);
            try {
                return ExactRandomSemantics1201.uniformIntInclusive(min, max)
                        .distribution()
                        .masses();
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        if (type.endsWith(":binomial")) {
            Integer n = constantProviderInt(object.get("n"));
            Float p = constantProviderFloat(object.get("p"));
            if (n == null || p == null || n < 0 || n > 100000) return null;
            return ExactRandomSemantics1201.binomial(n, p).distribution().masses();
        }
        if (type.endsWith(":score")) {
            JsonElement scoreElement = object.get("score");
            if (scoreElement == null || !scoreElement.isJsonPrimitive()) return null;
            Integer score = context.scores().get(scoreElement.getAsString());
            if (score == null) return null;
            double value = score * (object.has("scale") ? object.get("scale").getAsDouble() : 1.0D);
            if (object.has("clamp") && object.get("clamp").isJsonObject()) {
                JsonObject clamp = object.getAsJsonObject("clamp");
                if (clamp.has("min")) value = Math.max(value, clamp.get("min").getAsDouble());
                if (clamp.has("max")) value = Math.min(value, clamp.get("max").getAsDouble());
            } else {
                if (object.has("clamp_min")) value = Math.max(value, object.get("clamp_min").getAsDouble());
                if (object.has("clamp_max")) value = Math.min(value, object.get("clamp_max").getAsDouble());
            }
            if (!Double.isFinite(value)) return null;
            return Map.of((int) Math.floor(value), ExactProbability.ONE);
        }
        return null;
    }

    private static Map<Integer, ExactProbability> bonusDistribution(
            JsonElement element, float luck) {
        if (element == null) return Map.of(0, ExactProbability.ONE);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            float bonus = element.getAsFloat();
            return Map.of(net.minecraft.util.Mth.floor(bonus * luck), ExactProbability.ONE);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) {
            float bonus = object.get("value").getAsFloat();
            return Map.of(net.minecraft.util.Mth.floor(bonus * luck), ExactProbability.ONE);
        }
        if (!type.endsWith(":uniform")) return null;
        Float min = constantProviderFloat(object.get("min"));
        Float max = constantProviderFloat(object.get("max"));
        if (min == null || max == null) return null;
        try {
            return ExactRandomSemantics1201.uniformFloatTimesLuckFloor(
                            min, max, luck, 1_000_000)
                    .distribution()
                    .masses();
        } catch (IllegalArgumentException
                | ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return null;
        }
    }

    private static Map<Integer, ExactProbability> convolve(
            Map<Integer, ExactProbability> left,
            Map<Integer, ExactProbability> right) {
        Map<Integer, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> a : left.entrySet()) {
            for (Map.Entry<Integer, ExactProbability> b : right.entrySet()) {
                result.merge(
                        a.getKey() + b.getKey(),
                        a.getValue().multiply(b.getValue()),
                        ExactProbability::add);
            }
        }
        return result;
    }

    private static ExactProbability expected(Map<Integer, ExactProbability> distribution) {
        ExactProbability result = ExactProbability.ZERO;
        for (Map.Entry<Integer, ExactProbability> entry : distribution.entrySet()) {
            if (entry.getKey() < 0) {
                throw new IllegalArgumentException("negative value cannot form a non-negative expectation");
            }
            result = result.add(entry.getValue().multiply(
                    ExactProbability.of(entry.getKey(), 1)));
        }
        return result;
    }

    /** Exact inbound mass for each actual roll invocation: element i is P(roll count > i). */
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

    /** Ordered expand result for independent boolean entry predicates. */
    static <T> FiniteDistribution<List<T>> expandCandidateLists(
            List<T> entries, List<ExactProbability> inclusionProbabilities, int maxStates) {
        return expandCandidateGroups(
                entries.stream().map(List::of).toList(), inclusionProbabilities, maxStates);
    }

    /** An entry condition is evaluated once; a successful expand may append many candidates. */
    static <T> FiniteDistribution<List<T>> expandCandidateGroups(
            List<List<T>> groups,
            List<ExactProbability> inclusionProbabilities,
            int maxStates) {
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
                            state.getKey(), state.getValue().multiply(excluded), ExactProbability::add);
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

    private static List<JsonObject> expandTagCandidates(JsonObject entry) {
        if (!"minecraft:tag".equals(entry.get("type").getAsString())
                || !entry.has("expand")
                || !entry.get("expand").getAsBoolean()) {
            return List.of(entry);
        }
        ResourceLocation tagId = ResourceLocation.tryParse(entry.get("name").getAsString());
        if (tagId == null) return List.of();
        var holders = ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId));
        ArrayList<JsonObject> result = new ArrayList<>();
        for (Item item : holders) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;
            JsonObject candidate = entry.deepCopy();
            candidate.addProperty("type", "minecraft:item");
            candidate.addProperty("name", itemId.toString());
            candidate.remove("expand");
            result.add(candidate);
        }
        return List.copyOf(result);
    }

    private static Map<Integer, ExactProbability> clampNonNegative(
            Map<Integer, ExactProbability> distribution) {
        Map<Integer, ExactProbability> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, ExactProbability> entry : distribution.entrySet()) {
            result.merge(
                    Math.max(0, entry.getKey()), entry.getValue(), ExactProbability::add);
        }
        return result;
    }

    private static Integer constantProviderInt(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return constantIntValue(element);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        return type.endsWith(":constant") && object.has("value")
                ? constantIntValue(object.get("value"))
                : null;
    }

    static int constantIntValue(JsonElement element) {
        return net.minecraft.util.Mth.floor(element.getAsFloat());
    }

    private static Float constantProviderFloat(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsFloat();
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        return type.endsWith(":constant") && object.has("value")
                ? object.get("value").getAsFloat()
                : null;
    }

    private static Boolean checkCoordinate(JsonElement predicate, double actual) {
        if (predicate == null) return true;
        if (!predicate.isJsonObject()) {
            return predicate.isJsonPrimitive()
                    && Math.floor(predicate.getAsDouble()) == Math.floor(actual);
        }
        JsonObject range = predicate.getAsJsonObject();
        for (String key : range.keySet()) {
            if (!key.equals("min") && !key.equals("max")) return null;
            if (!range.get(key).isJsonPrimitive() || !range.get(key).getAsJsonPrimitive().isNumber()) return null;
            if (!Double.isFinite(range.get(key).getAsDouble())) return null;
        }
        if (range.has("min") && range.has("max")
                && range.get("min").getAsDouble() > range.get("max").getAsDouble()) return false;
        if (range.has("min") && actual < range.get("min").getAsDouble()) return false;
        if (range.has("max") && actual > range.get("max").getAsDouble()) return false;
        return true;
    }

    private static Boolean checkRange(JsonElement element, double actual) {
        if (element == null) return true;
        if (!element.isJsonObject()) return null;
        JsonObject range = element.getAsJsonObject();
        for (String key : range.keySet()) {
            if (!key.equals("min") && !key.equals("max")) return null;
            if (!range.get(key).isJsonPrimitive() || !range.get(key).getAsJsonPrimitive().isNumber()) return null;
            if (!Double.isFinite(range.get(key).getAsDouble())) return null;
        }
        if (range.has("min") && range.has("max")
                && range.get("min").getAsDouble() > range.get("max").getAsDouble()) return false;
        return (!range.has("min") || actual >= range.get("min").getAsDouble())
                && (!range.has("max") || actual <= range.get("max").getAsDouble());
    }

    private static Double bonusFloor(JsonElement element, float luck) {
        if (element == null) return 0.0D;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return Math.floor(element.getAsDouble() * luck);
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (type.endsWith(":constant")) return Math.floor(object.get("value").getAsDouble() * luck);
        if (type.endsWith(":uniform") && luck == 0.0F) return 0.0D;
        return null;
    }

    private static ExactProbability conditionProbability(
            JsonElement element,
            LootAnalysisContext context,
            RuntimeLootAstSource source,
            Set<Object> activeReferences,
            List<Diagnostic> diagnostics) {
        if (element == null) return ExactProbability.ONE;
        if (!element.isJsonArray()) {
            return conditionProbabilitySingle(
                    element, context, source, activeReferences, diagnostics);
        }
        ExactProbability probability = ExactProbability.ONE;
        for (JsonElement condition : element.getAsJsonArray()) {
            if (probability.isZero()) break;
            ExactProbability value = conditionProbabilitySingle(
                    condition, context, source, activeReferences, diagnostics);
            if (value == null) return null;
            probability = probability.multiply(value);
        }
        return probability;
    }

    private static ExactProbability conditionProbabilitySingle(
            JsonElement element,
            LootAnalysisContext context,
            RuntimeLootAstSource source,
            Set<Object> activeReferences,
            List<Diagnostic> diagnostics) {
        float luck = context.luck();
        if (!element.isJsonObject()) return null;
        JsonObject condition = element.getAsJsonObject();
        String type = condition.has("condition") ? condition.get("condition").getAsString() : "";
        if (type.endsWith(":always_true")) return ExactProbability.ONE;
        if (type.endsWith(":always_false")) return ExactProbability.ZERO;
        if (type.endsWith(":killed_by_player")) {
            return context.killedByPlayer() == null
                    ? null
                    : (context.killedByPlayer()
                            ? ExactProbability.ONE
                            : ExactProbability.ZERO);
        }
        if (type.endsWith(":survives_explosion")) {
            return context.survivesExplosion() == null
                    ? null
                    : (context.survivesExplosion()
                            ? ExactProbability.ONE
                            : ExactProbability.ZERO);
        }
        if (type.endsWith(":location_check")) {
            if (context.origin() == null || !condition.has("predicate")) return null;
            JsonObject predicate = condition.getAsJsonObject("predicate");
            for (String key : predicate.keySet()) {
                if (!key.equals("dimension") && !key.equals("position") && !key.equals("block") && !key.equals("fluid")) return null;
            }
            if (predicate.has("dimension") && context.level() != null) {
                if (!predicate.get("dimension").isJsonPrimitive()) return null;
                ResourceLocation dimension = ResourceLocation.tryParse(predicate.get("dimension").getAsString());
                if (dimension == null || !context.level().dimension().location().equals(dimension)) return ExactProbability.ZERO;
            } else if (predicate.has("dimension")) return null;
            if (predicate.has("position")) {
                if (!predicate.get("position").isJsonObject()) return null;
                JsonObject position = predicate.getAsJsonObject("position");
                Boolean x = checkCoordinate(position.get("x"), context.origin().x());
                Boolean y = checkCoordinate(position.get("y"), context.origin().y());
                Boolean z = checkCoordinate(position.get("z"), context.origin().z());
                if (x == null || y == null || z == null) return null;
                if (!x || !y || !z) return ExactProbability.ZERO;
            }
            if (predicate.has("block") || predicate.has("fluid")) {
                if (context.level() == null || context.origin() == null) return null;
                BlockPos pos = BlockPos.containing(context.origin());
                if (predicate.has("block")) {
                    if (!predicate.get("block").isJsonPrimitive()) return null;
                    ResourceLocation expected = ResourceLocation.tryParse(predicate.get("block").getAsString());
                    ResourceLocation actual = ForgeRegistries.BLOCKS.getKey(context.level().getBlockState(pos).getBlock());
                    if (expected == null || !expected.equals(actual)) return ExactProbability.ZERO;
                }
                if (predicate.has("fluid")) {
                    if (!predicate.get("fluid").isJsonPrimitive()) return null;
                    ResourceLocation expected = ResourceLocation.tryParse(predicate.get("fluid").getAsString());
                    ResourceLocation actual = ForgeRegistries.FLUIDS.getKey(context.level().getFluidState(pos).getType());
                    if (expected == null || !expected.equals(actual)) return ExactProbability.ZERO;
                }
            }
            return ExactProbability.ONE;
        }
        if (type.endsWith(":entity_properties")) {
            if (context.entity() == null || !condition.has("predicate")) return null;
            JsonObject predicate = condition.getAsJsonObject("predicate");
            for (String key : predicate.keySet()) {
                if (!key.equals("type") && !key.equals("flags") && !key.equals("distance")
                        && !key.equals("nbt") && !key.equals("equipment") && !key.equals("team")) return null;
            }
            if (predicate.has("type")) {
                ResourceLocation actual = ForgeRegistries.ENTITY_TYPES.getKey(context.entity().getType());
                JsonElement typeElement = predicate.get("type");
                if (typeElement.isJsonPrimitive()) {
                    ResourceLocation expected = ResourceLocation.tryParse(typeElement.getAsString());
                    if (expected == null || !expected.equals(actual)) return ExactProbability.ZERO;
                } else if (typeElement.isJsonArray()) {
                    boolean match = false;
                    for (JsonElement value : typeElement.getAsJsonArray()) {
                        if (!value.isJsonPrimitive()) return null;
                        ResourceLocation expected = ResourceLocation.tryParse(value.getAsString());
                        if (expected != null && expected.equals(actual)) match = true;
                    }
                    if (!match) return ExactProbability.ZERO;
                } else return null;
            }
            if (predicate.has("flags")) {
                JsonObject flags = predicate.getAsJsonObject("flags");
                for (String key : flags.keySet()) {
                    if (!key.equals("is_baby") && !key.equals("is_on_fire") && !key.equals("is_sneaking")
                            && !key.equals("is_sprinting") && !key.equals("is_swimming")) return null;
                    if (!flags.get(key).isJsonPrimitive()) return null;
                    boolean expected = flags.get(key).getAsBoolean();
                    boolean actual = switch (key) {
                        case "is_baby" -> context.entity() instanceof net.minecraft.world.entity.AgeableMob ageable && ageable.isBaby();
                        case "is_on_fire" -> context.entity().isOnFire();
                        case "is_sneaking" -> context.entity().isCrouching();
                        case "is_sprinting" -> context.entity().isSprinting();
                        case "is_swimming" -> context.entity().isSwimming();
                        default -> false;
                    };
                    if (expected != actual) return ExactProbability.ZERO;
                }
            }
            if (predicate.has("distance")) {
                if (context.origin() == null) return null;
                JsonObject distance = predicate.getAsJsonObject("distance");
                for (String key : distance.keySet()) {
                    if (!key.equals("absolute") && !key.equals("horizontal") && !key.equals("x")
                            && !key.equals("y") && !key.equals("z")) return null;
                }
                double dx = context.entity().getX() - context.origin().x();
                double dy = context.entity().getY() - context.origin().y();
                double dz = context.entity().getZ() - context.origin().z();
                double absolute = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                Boolean absoluteMatch = checkRange(distance.get("absolute"), absolute);
                Boolean horizontalMatch = checkRange(distance.get("horizontal"), horizontal);
                Boolean xMatch = checkRange(distance.get("x"), Math.abs(dx));
                Boolean yMatch = checkRange(distance.get("y"), Math.abs(dy));
                Boolean zMatch = checkRange(distance.get("z"), Math.abs(dz));
                if (absoluteMatch == null || horizontalMatch == null || xMatch == null || yMatch == null || zMatch == null) return null;
                if (!absoluteMatch || !horizontalMatch || !xMatch || !yMatch || !zMatch) return ExactProbability.ZERO;
            }
            if (predicate.has("nbt")) {
                if (!predicate.get("nbt").isJsonObject()) return null;
                CompoundTag entityNbt = new CompoundTag();
                context.entity().saveWithoutId(entityNbt);
                Boolean matches = matchesNbt(predicate.getAsJsonObject("nbt"), entityNbt);
                if (matches == null) return null;
                if (!matches) return ExactProbability.ZERO;
            }
            if (predicate.has("equipment")) {
                if (!(context.entity() instanceof LivingEntity living)
                        || !predicate.get("equipment").isJsonObject()) return null;
                JsonObject equipment = predicate.getAsJsonObject("equipment");
                for (String slotName : equipment.keySet()) {
                    EquipmentSlot slot = equipmentSlot(slotName);
                    if (slot == null) return null;
                    if (!equipmentPredicateMatches(equipment.get(slotName), living.getItemBySlot(slot))) return ExactProbability.ZERO;
                }
            }
            if (predicate.has("team")) {
                if (!predicate.get("team").isJsonPrimitive()) return null;
                String expectedTeam = predicate.get("team").getAsString();
                Team team = context.entity().getTeam();
                if (team == null || !team.getName().equals(expectedTeam)) return ExactProbability.ZERO;
            }
            return ExactProbability.ONE;
        }
        if (type.endsWith(":match_tool")) {
            if (context.tool() == null || context.tool().isEmpty() || !condition.has("predicate")) return null;
            JsonObject predicate = condition.getAsJsonObject("predicate");
            for (String key : predicate.keySet()) {
                if (!key.equals("items") && !key.equals("count") && !key.equals("damage") && !key.equals("enchantments") && !key.equals("nbt")) return null;
            }
            if (predicate.has("items")) {
                JsonArray items = predicate.getAsJsonArray("items");
                ResourceLocation actual = ForgeRegistries.ITEMS.getKey(context.tool().getItem());
                boolean match = false;
                for (JsonElement item : items) {
                    if (!item.isJsonPrimitive()) return null;
                    String itemValue = item.getAsString();
                    if (itemValue.startsWith("#")) {
                        ResourceLocation tagId = ResourceLocation.tryParse(itemValue.substring(1));
                        if (tagId == null) return null;
                        var holders = ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tagId));
                        if (holders.isEmpty()) return null;
                        for (Item taggedItem : holders) {
                            if (ForgeRegistries.ITEMS.getKey(taggedItem).equals(actual)) match = true;
                        }
                    } else {
                        ResourceLocation expected = ResourceLocation.tryParse(itemValue);
                        if (expected != null && expected.equals(actual)) match = true;
                    }
                }
                if (!match) return ExactProbability.ZERO;
            }
            if (predicate.has("count")) {
                if (!predicate.get("count").isJsonPrimitive()) return null;
                if (context.tool().getCount() != predicate.get("count").getAsInt()) return ExactProbability.ZERO;
            }
            if (predicate.has("damage")) {
                JsonElement damage = predicate.get("damage");
                if (!context.tool().isDamageableItem()) return null;
                if (damage.isJsonPrimitive() && damage.getAsJsonPrimitive().isNumber()) {
                    if (context.tool().getDamageValue() != damage.getAsInt()) return ExactProbability.ZERO;
                } else {
                    Boolean match = checkRange(damage, context.tool().getDamageValue());
                    if (match == null) return null;
                    if (!match) return ExactProbability.ZERO;
                }
            }
            if (predicate.has("enchantments")) {
                if (!predicate.get("enchantments").isJsonArray()) return null;
                for (JsonElement enchantment : predicate.getAsJsonArray("enchantments")) {
                    if (!enchantment.isJsonObject()) return null;
                    JsonObject object = enchantment.getAsJsonObject();
                    ResourceLocation enchantmentId = ResourceLocation.tryParse(object.get("enchantment").getAsString());
                    Enchantment resolved = enchantmentId == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                    if (resolved == null) return null;
                    int level = context.tool().getEnchantmentLevel(resolved);
                    if (object.has("levels")) {
                        JsonElement levels = object.get("levels");
                        if (!levels.isJsonObject()) return null;
                        JsonObject range = levels.getAsJsonObject();
                        for (String key : range.keySet()) if (!key.equals("min") && !key.equals("max")) return null;
                        if (range.has("min") && level < range.get("min").getAsInt()) return ExactProbability.ZERO;
                        if (range.has("max") && level > range.get("max").getAsInt()) return ExactProbability.ZERO;
                    } else if (level <= 0) return ExactProbability.ZERO;
                }
            }
            if (predicate.has("nbt")) {
                if (!predicate.get("nbt").isJsonObject()) return null;
                CompoundTag toolNbt = context.tool().getTag();
                if (toolNbt == null) return ExactProbability.ZERO;
                Boolean matches = matchesNbt(predicate.getAsJsonObject("nbt"), toolNbt);
                if (matches == null) return null;
                if (!matches) return ExactProbability.ZERO;
            }
            return ExactProbability.ONE;
        }
        if (type.endsWith(":reference")) {
            JsonElement name = condition.get("name");
            if (name == null || !name.isJsonPrimitive()) return null;
            ResourceLocation reference = ResourceLocation.tryParse(name.getAsString());
            if (reference == null) return ExactProbability.ZERO;
            Optional<RuntimeLootAstSource.RuntimeAst<net.minecraft.world.level.storage.loot.predicates.LootItemCondition>> resolved =
                    source.predicate(reference);
            if (resolved.isEmpty()) {
                diagnostics.add(ReferenceSemantics1201.missingPredicate(reference));
                return ExactProbability.ZERO;
            }
            Object identity = resolved.get().identity();
            if (!activeReferences.add(identity)) {
                diagnostics.add(ReferenceSemantics1201.recursivePredicate(reference));
                return ExactProbability.ZERO;
            }
            try {
                return conditionProbability(
                        resolved.get().json(), context, source, activeReferences, diagnostics);
            } finally {
                activeReferences.remove(identity);
            }
        }
        if (type.endsWith(":inverted")) {
            ExactProbability value = conditionProbabilitySingle(
                    condition.get("term"), context, source, activeReferences, diagnostics);
            return value == null ? null : ExactProbability.ONE.subtract(value);
        }
        if (type.endsWith(":random_chance")) {
            if (!condition.has("chance") || !condition.get("chance").isJsonPrimitive()) return null;
            float chance = condition.get("chance").getAsFloat();
            return Float.isFinite(chance) ? nextFloatLessThanProbability(chance) : null;
        }
        if (type.endsWith(":random_chance_with_luck")) {
            if (!condition.has("chance") || !condition.get("chance").isJsonPrimitive()) return null;
            float chance = condition.get("chance").getAsFloat();
            float multiplier = condition.has("luck_multiplier")
                    ? condition.get("luck_multiplier").getAsFloat()
                    : 1.0F;
            float threshold = chance + luck * multiplier;
            return Float.isFinite(threshold) ? nextFloatLessThanProbability(threshold) : null;
        }
        if (type.endsWith(":table_bonus")) {
            if (context.tool() == null || context.tool().isEmpty()) return null;
            JsonElement enchantmentElement = condition.get("enchantment");
            JsonElement chances = condition.get("chances");
            if (enchantmentElement == null || !enchantmentElement.isJsonPrimitive()
                    || chances == null || !chances.isJsonArray() || chances.getAsJsonArray().isEmpty()) return null;
            ResourceLocation enchantmentId = ResourceLocation.tryParse(enchantmentElement.getAsString());
            Enchantment enchantment = enchantmentId == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchantment == null) return null;
            int level = context.tool().getEnchantmentLevel(enchantment);
            int index = Math.max(0, Math.min(chances.getAsJsonArray().size() - 1, level));
            JsonElement selected = chances.getAsJsonArray().get(index);
            if (!selected.isJsonPrimitive() || !selected.getAsJsonPrimitive().isNumber()) return null;
            return nextFloatLessThanProbability(selected.getAsFloat());
        }
        if (type.endsWith(":all_of")) {
            JsonArray terms = condition.getAsJsonArray("terms");
            ExactProbability result = ExactProbability.ONE;
            for (JsonElement term : terms) {
                if (result.isZero()) break;
                ExactProbability p = conditionProbabilitySingle(
                        term, context, source, activeReferences, diagnostics);
                if (p == null) return null;
                result = result.multiply(p);
            }
            return result;
        }
        if (type.endsWith(":any_of")) {
            JsonArray terms = condition.getAsJsonArray("terms");
            ExactProbability failure = ExactProbability.ONE;
            for (JsonElement term : terms) {
                if (failure.isZero()) break;
                ExactProbability p = conditionProbabilitySingle(
                        term, context, source, activeReferences, diagnostics);
                if (p == null) return null;
                failure = failure.multiply(ExactProbability.ONE.subtract(p));
            }
            return ExactProbability.ONE.subtract(failure);
        }
        return null;
    }

    /** RandomSource.nextFloat() in 1.20.1 has 24 bits of precision. */
    private static ExactProbability nextFloatLessThanProbability(float threshold) {
        return ExactRandomSemantics1201.nextFloatLessThan(threshold)
                .distribution()
                .masses()
                .getOrDefault(true, ExactProbability.ZERO);
    }

    private static boolean addItemOutput(StackMeasure measure, RuntimeLootAstSource source,
                                         LootAnalysisContext context, List<Diagnostic> diagnostics,
                                         JsonObject entry, Item item, ExactProbability occurrence, ResourceLocation id) {
        StackMeasure generated = new StackMeasure();
        generated.add(new StackState(new ItemStack(item, 1)), occurrence);
        if (entry.has("functions")
                && !applyFunctions(
                        generated,
                        entry.getAsJsonArray("functions"),
                        context,
                        source,
                        diagnostics,
                        id,
                        identitySet())) {
            return false;
        }
        measure.addAll(generated, ExactProbability.ONE);
        return true;
    }

    private static boolean applyFunctions(
            StackMeasure measure,
            JsonArray functions,
            LootAnalysisContext context,
            RuntimeLootAstSource source,
            List<Diagnostic> diagnostics,
            ResourceLocation id,
            Set<Object> activeFunctions) {
        Map<StackState, ExactProbability> states = new LinkedHashMap<>(measure.values());
        for (JsonElement functionElement : functions) {
            if (!functionElement.isJsonObject()) return unsupportedFunction(diagnostics, id, "invalid function");
            JsonObject function = functionElement.getAsJsonObject();
            ExactProbability conditionSuccess = ExactProbability.ONE;
            if (function.has("conditions") && function.get("conditions").isJsonArray()
                    && !function.getAsJsonArray("conditions").isEmpty()) {
                conditionSuccess = conditionProbability(
                        function.get("conditions"), context, source, identitySet(), diagnostics);
                if (conditionSuccess == null) {
                    return unsupportedFunction(diagnostics, id, "unsupported function condition");
                }
                if (conditionSuccess.isZero()) continue;
            }
            ExactProbability conditionFailure = ExactProbability.ONE.subtract(conditionSuccess);
            Map<StackState, ExactProbability> skipped = scaleStates(states, conditionFailure);
            if (!conditionSuccess.equals(ExactProbability.ONE)) {
                states = scaleStates(states, conditionSuccess);
            }
            String type = function.has("function") ? function.get("function").getAsString() : "";
            if (type.endsWith(":reference")) {
                JsonElement name = function.get("name");
                ResourceLocation reference = name != null && name.isJsonPrimitive()
                        ? ResourceLocation.tryParse(name.getAsString())
                        : null;
                if (reference == null) {
                    return unsupportedFunction(diagnostics, id, "invalid function reference");
                }
                Optional<RuntimeLootAstSource.RuntimeAst<net.minecraft.world.level.storage.loot.functions.LootItemFunction>> resolved =
                        source.modifier(reference);
                if (resolved.isEmpty()) {
                    diagnostics.add(ReferenceSemantics1201.missingFunction(reference));
                    mergeStates(states, skipped);
                    continue;
                }
                Object identity = resolved.get().identity();
                if (!activeFunctions.add(identity)) {
                    diagnostics.add(ReferenceSemantics1201.recursiveFunction(reference));
                    mergeStates(states, skipped);
                    continue;
                }
                JsonArray referencedFunctions = new JsonArray();
                JsonElement referencedJson = resolved.get().json();
                if (referencedJson.isJsonArray()) {
                    referencedJson.getAsJsonArray().forEach(referencedFunctions::add);
                } else {
                    referencedFunctions.add(referencedJson);
                }
                boolean applied;
                try {
                    StackMeasure referencedMeasure = new StackMeasure();
                    states.forEach(referencedMeasure::add);
                    applied = applyFunctions(
                            referencedMeasure,
                            referencedFunctions,
                            context,
                            source,
                            diagnostics,
                            id,
                            activeFunctions);
                    states = new LinkedHashMap<>(referencedMeasure.values());
                } finally {
                    activeFunctions.remove(identity);
                }
                if (!applied) return false;
                mergeStates(states, skipped);
                continue;
            }
            Map<StackState, ExactProbability> next = new LinkedHashMap<>();
            for (Map.Entry<StackState, ExactProbability> prior : states.entrySet()) {
                ItemStack base = prior.getKey().stack();
                if (type.endsWith(":set_count")) {
                    if (!function.has("count")) {
                        return unsupportedFunction(diagnostics, id, "missing set_count provider");
                    }
                    Map<Integer, ExactProbability> counts =
                            integerDistribution(function.get("count"), context);
                    if (counts == null) return unsupportedFunction(diagnostics, id, "unsupported set_count provider");
                    boolean add = function.has("add") && function.get("add").getAsBoolean();
                    for (Map.Entry<Integer, ExactProbability> count : counts.entrySet()) {
                        ItemStack output = base.copy();
                        int value = add ? output.getCount() + count.getKey() : count.getKey();
                        output.setCount(Math.max(0, Math.min(value, output.getMaxStackSize())));
                        next.merge(
                                new StackState(output),
                                prior.getValue().multiply(count.getValue()),
                                ExactProbability::add);
                    }
                } else if (type.endsWith(":enchant_randomly")) {
                    List<Enchantment> candidates = randomEnchantmentCandidates(function, base);
                    if (candidates == null) {
                        return unsupportedFunction(
                                diagnostics, id, "invalid enchant_randomly enchantment list");
                    }
                    if (candidates.isEmpty()) {
                        next.merge(new StackState(base), prior.getValue(), ExactProbability::add);
                        continue;
                    }
                    ExactProbability enchantmentMass =
                            ExactProbability.of(1, candidates.size());
                    for (Enchantment enchantment : candidates) {
                        int minLevel = enchantment.getMinLevel();
                        int maxLevel = enchantment.getMaxLevel();
                        Map<Integer, ExactProbability> levels = minLevel >= maxLevel
                                ? Map.of(minLevel, ExactProbability.ONE)
                                : ExactRandomSemantics1201
                                        .uniformIntInclusive(minLevel, maxLevel)
                                        .distribution()
                                        .masses();
                        for (Map.Entry<Integer, ExactProbability> level : levels.entrySet()) {
                            ItemStack output;
                            if (base.is(net.minecraft.world.item.Items.BOOK)) {
                                // EnchantRandomlyFunction replaces a book with a fresh count-one stack.
                                output = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                                EnchantedBookItem.addEnchantment(
                                        output,
                                        new EnchantmentInstance(
                                                enchantment, level.getKey()));
                            } else {
                                output = base.copy();
                                output.enchant(enchantment, level.getKey());
                            }
                            ExactProbability branch = enchantmentMass.multiply(level.getValue());
                            next.merge(
                                    new StackState(output),
                                    prior.getValue().multiply(branch),
                                    ExactProbability::add);
                        }
                    }
                } else if (type.endsWith(":enchant_with_levels")) {
                    Map<Integer, ExactProbability> levels =
                            integerDistribution(function.get("levels"), context);
                    if (levels == null) {
                        return unsupportedFunction(
                                diagnostics, id, "unsupported enchant_with_levels provider");
                    }
                    boolean treasure = function.has("treasure")
                            && function.get("treasure").getAsBoolean();
                    try {
                        for (Map.Entry<Integer, ExactProbability> level : levels.entrySet()) {
                            RandomTraceDistribution<StackState> enchanted =
                                    ExactEnchantmentSemantics1201.enchantItem(
                                            base, level.getKey(), treasure, 1_000_000);
                            for (Map.Entry<StackState, ExactProbability> output
                                    : enchanted.marginal().masses().entrySet()) {
                                ExactProbability branch =
                                        level.getValue().multiply(output.getValue());
                                next.merge(
                                        output.getKey(),
                                        prior.getValue().multiply(branch),
                                        ExactProbability::add);
                            }
                        }
                    } catch (ExactRandomSemantics1201.StateSpaceLimitException exception) {
                        diagnostics.add(new Diagnostic("STATE_SPACE_LIMIT", exception.getMessage()));
                        return false;
                    }
                } else if (type.endsWith(":set_name")) {
                    JsonElement name = function.get("name");
                    Component component;
                    try {
                        component = name != null && name.isJsonPrimitive() ? Component.literal(name.getAsString())
                                : Component.Serializer.fromJson(name);
                    } catch (RuntimeException exception) {
                        return unsupportedFunction(diagnostics, id, "invalid set_name component");
                    }
                    if (component == null) return unsupportedFunction(diagnostics, id, "null set_name component");
                    ItemStack output = base.copy();
                    output.setHoverName(component);
                    next.merge(new StackState(output), prior.getValue(), ExactProbability::add);
                } else if (type.endsWith(":set_damage")) {
                    JsonElement damage = function.get("damage");
                    if (!base.isDamageableItem()) {
                        diagnostics.add(new Diagnostic(
                                "RUNTIME_WARNING", "Couldn't set damage of loot item " + base + " in " + id));
                        next.merge(new StackState(base), prior.getValue(), ExactProbability::add);
                        continue;
                    }
                    Boolean add = function.has("add") && function.get("add").isJsonPrimitive()
                            ? function.get("add").getAsBoolean() : false;
                    Map<Integer, ExactProbability> damageValues = setDamageDistribution(
                            damage, add, base.getDamageValue(), base.getMaxDamage());
                    if (damageValues == null) {
                        return unsupportedFunction(diagnostics, id, "unsupported set_damage provider");
                    }
                    for (Map.Entry<Integer, ExactProbability> value : damageValues.entrySet()) {
                        ItemStack output = base.copy();
                        output.setDamageValue(value.getKey());
                        next.merge(
                                new StackState(output),
                                prior.getValue().multiply(value.getValue()),
                                ExactProbability::add);
                    }
                } else if (type.endsWith(":set_potion")) {
                    JsonElement potionElement = function.get("id");
                    ResourceLocation potionId = potionElement != null && potionElement.isJsonPrimitive()
                            ? ResourceLocation.tryParse(potionElement.getAsString()) : null;
                    Potion potion = potionId == null ? null : ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion == null) return unsupportedFunction(diagnostics, id, "missing set_potion reference");
                    ItemStack output = base.copy();
                    PotionUtils.setPotion(output, potion);
                    next.merge(new StackState(output), prior.getValue(), ExactProbability::add);
                } else if (type.endsWith(":set_lore")) {
                    ItemStack output = base.copy();
                    if (!applyLore(output, function)) {
                        return unsupportedFunction(diagnostics, id, "invalid set_lore component");
                    }
                    next.merge(new StackState(output), prior.getValue(), ExactProbability::add);
                } else if (type.endsWith(":set_attributes")) {
                    ItemStack output = base.copy();
                    if (!applyAttributes(output, function)) {
                        return unsupportedFunction(diagnostics, id, "invalid set_attributes");
                    }
                    next.merge(new StackState(output), prior.getValue(), ExactProbability::add);
                } else if (type.endsWith(":copy_nbt")) {
                    if (context.entity() == null) return unsupportedFunction(diagnostics, id, "copy_nbt requires entity context");
                    ItemStack output = base.copy();
                    if (!copyNbt(output, function, context.entity())) {
                        return unsupportedFunction(diagnostics, id, "unsupported copy_nbt source/path");
                    }
                    next.merge(new StackState(output), prior.getValue(), ExactProbability::add);
                } else if (type.endsWith(":set_stew_effect")) {
                    if (!base.is(net.minecraft.world.item.Items.SUSPICIOUS_STEW)) {
                        next.merge(new StackState(base), prior.getValue(), ExactProbability::add);
                        continue;
                    }
                    JsonArray effects = function.has("effects")
                            && function.get("effects").isJsonArray()
                            ? function.getAsJsonArray("effects")
                            : new JsonArray();
                    if (effects.isEmpty()) {
                        next.merge(new StackState(base), prior.getValue(), ExactProbability::add);
                        continue;
                    }
                    ExactProbability effectMass = ExactProbability.of(1, effects.size());
                    for (JsonElement effectElement : effects) {
                        if (!effectElement.isJsonObject()) {
                            return unsupportedFunction(
                                    diagnostics, id, "invalid set_stew_effect entry");
                        }
                        JsonObject effectObject = effectElement.getAsJsonObject();
                        ResourceLocation effectId = effectObject.has("type")
                                ? ResourceLocation.tryParse(effectObject.get("type").getAsString())
                                : null;
                        MobEffect effect = effectId == null
                                ? null
                                : ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                        Map<Integer, ExactProbability> durations =
                                integerDistribution(effectObject.get("duration"), context);
                        if (effect == null || durations == null) {
                            return unsupportedFunction(
                                    diagnostics, id, "invalid set_stew_effect provider");
                        }
                        for (Map.Entry<Integer, ExactProbability> duration : durations.entrySet()) {
                            ItemStack output = base.copy();
                            int ticks = effect.isInstantenous()
                                    ? duration.getKey()
                                    : duration.getKey() * 20;
                            net.minecraft.world.item.SuspiciousStewItem.saveMobEffect(
                                    output, effect, ticks);
                            ExactProbability branch = effectMass.multiply(duration.getValue());
                            next.merge(
                                    new StackState(output),
                                    prior.getValue().multiply(branch),
                                    ExactProbability::add);
                        }
                    }
                } else if (type.endsWith(":set_instrument")) {
                    JsonElement options = function.get("options");
                    if (options == null || !options.isJsonPrimitive()) {
                        return unsupportedFunction(diagnostics, id, "invalid set_instrument options");
                    }
                    String optionName = options.getAsString();
                    if (!optionName.startsWith("#")) {
                        return unsupportedFunction(diagnostics, id, "set_instrument options is not a tag");
                    }
                    ResourceLocation tagId = ResourceLocation.tryParse(optionName.substring(1));
                    if (tagId == null) {
                        return unsupportedFunction(diagnostics, id, "invalid set_instrument tag");
                    }
                    TagKey<Instrument> tag = TagKey.create(Registries.INSTRUMENT, tagId);
                    Optional<? extends net.minecraft.core.HolderSet<Instrument>> holders =
                            BuiltInRegistries.INSTRUMENT.getTag(tag);
                    if (holders.isEmpty() || holders.get().size() == 0) {
                        next.merge(new StackState(base), prior.getValue(), ExactProbability::add);
                        continue;
                    }
                    ExactProbability selectedMass = ExactProbability.of(1, holders.get().size());
                    for (net.minecraft.core.Holder<Instrument> holder : holders.get()) {
                        Optional<net.minecraft.resources.ResourceKey<Instrument>> key = holder.unwrapKey();
                        if (key.isEmpty()) {
                            return unsupportedFunction(
                                    diagnostics, id, "set_instrument tag contains an unkeyed holder");
                        }
                        ItemStack output = base.copy();
                        output.getOrCreateTag().putString("instrument", key.get().location().toString());
                        next.merge(
                                new StackState(output),
                                prior.getValue().multiply(selectedMass),
                                ExactProbability::add);
                    }
                } else {
                    return unsupportedFunction(diagnostics, id, "unsupported function " + type);
                }
            }
            mergeStates(next, skipped);
            states = next;
        }
        StackMeasure transformed = new StackMeasure();
        states.forEach(transformed::add);
        measure.replaceWith(transformed);
        return true;
    }

    static <T> Map<T, ExactProbability> scaleStates(
            Map<T, ExactProbability> states, ExactProbability multiplier) {
        Map<T, ExactProbability> result = new LinkedHashMap<>();
        if (multiplier.isZero()) return result;
        states.forEach((state, mass) -> result.put(state, mass.multiply(multiplier)));
        return result;
    }

    static <T> void mergeStates(
            Map<T, ExactProbability> destination,
            Map<T, ExactProbability> source) {
        source.forEach((state, mass) -> destination.merge(state, mass, ExactProbability::add));
    }

    private static boolean unsupportedFunction(List<Diagnostic> diagnostics, ResourceLocation id, String message) {
        diagnostics.add(new Diagnostic("UNSUPPORTED_TYPE", message + " in " + id));
        return false;
    }

    private static Map<Integer, ExactProbability> setDamageDistribution(
            JsonElement provider, boolean add, int currentDamage, int maxDamage) {
        Float constant = constantProviderFloat(provider);
        if (constant != null) {
            try {
                int damage = ExactRandomSemantics1201.setDamageValue(
                        constant, add, currentDamage, maxDamage);
                return Map.of(damage, ExactProbability.ONE);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        if (provider == null || !provider.isJsonObject()) return null;
        JsonObject object = provider.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "uniform";
        if (!type.endsWith(":uniform")) return null;
        Float min = constantProviderFloat(object.get("min"));
        Float max = constantProviderFloat(object.get("max"));
        if (min == null || max == null) return null;
        try {
            return ExactRandomSemantics1201.uniformFloatSetDamage(
                            min, max, add, currentDamage, maxDamage, maxDamage + 1)
                    .distribution()
                    .masses();
        } catch (IllegalArgumentException
                | ExactRandomSemantics1201.StateSpaceLimitException exception) {
            return null;
        }
    }

    private static List<Enchantment> randomEnchantmentCandidates(
            JsonObject function, ItemStack stack) {
        if (function.has("enchantments")) {
            JsonElement element = function.get("enchantments");
            if (!element.isJsonArray()) return null;
            List<Enchantment> result = new ArrayList<>();
            for (JsonElement value : element.getAsJsonArray()) {
                if (!value.isJsonPrimitive()) return null;
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                Enchantment enchantment = id == null
                        ? null
                        : ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment == null) return null;
                result.add(enchantment);
            }
            return List.copyOf(result);
        }
        boolean book = stack.is(net.minecraft.world.item.Items.BOOK);
        return BuiltInRegistries.ENCHANTMENT.stream()
                .filter(Enchantment::isDiscoverable)
                .filter(enchantment -> book || enchantment.canEnchant(stack))
                .toList();
    }

    private static Set<Object> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static boolean applyLore(ItemStack stack, JsonObject function) {
        JsonElement loreElement = function.get("lore");
        if (loreElement == null || !loreElement.isJsonArray()) return false;
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag display = root.getCompound("display");
        ListTag lore = function.has("replace") && function.get("replace").getAsBoolean()
                ? new ListTag() : display.getList("Lore", 8);
        for (JsonElement line : loreElement.getAsJsonArray()) {
            Component component;
            try {
                component = line.isJsonPrimitive() ? Component.literal(line.getAsString())
                        : Component.Serializer.fromJson(line);
            } catch (RuntimeException exception) {
                return false;
            }
            if (component == null) return false;
            lore.add(StringTag.valueOf(Component.Serializer.toJson(component)));
        }
        display.put("Lore", lore);
        root.put("display", display);
        return true;
    }

    private static boolean applyAttributes(ItemStack stack, JsonObject function) {
        JsonElement modifiers = function.get("modifiers");
        if (modifiers == null || !modifiers.isJsonArray()) return false;
        CompoundTag root = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (JsonElement element : modifiers.getAsJsonArray()) {
            if (!element.isJsonObject()) return false;
            JsonObject modifier = element.getAsJsonObject();
            if (!modifier.has("attribute") || !modifier.has("name") || !modifier.has("amount")
                    || !modifier.has("operation") || !modifier.has("uuid")) return false;
            if (!modifier.get("amount").isJsonPrimitive() || !modifier.get("amount").getAsJsonPrimitive().isNumber()) return false;
            UUID uuid;
            try {
                JsonElement uuidElement = modifier.get("uuid");
                if (uuidElement.isJsonPrimitive()) {
                    uuid = UUID.fromString(uuidElement.getAsString());
                } else if (uuidElement.isJsonArray() && uuidElement.getAsJsonArray().size() == 4) {
                    long most = (uuidElement.getAsJsonArray().get(0).getAsLong() << 32)
                            | (uuidElement.getAsJsonArray().get(1).getAsLong() & 0xffffffffL);
                    long least = (uuidElement.getAsJsonArray().get(2).getAsLong() << 32)
                            | (uuidElement.getAsJsonArray().get(3).getAsLong() & 0xffffffffL);
                    uuid = new UUID(most, least);
                } else return false;
            }
            catch (IllegalArgumentException exception) { return false; }
            String operation = modifier.get("operation").getAsString();
            if (!operation.equals("addition") && !operation.equals("multiply_base")
                    && !operation.equals("multiply_total")) return false;
            CompoundTag tag = new CompoundTag();
            tag.putString("AttributeName", modifier.get("attribute").getAsString());
            tag.putString("Name", modifier.get("name").getAsString());
            tag.putDouble("Amount", modifier.get("amount").getAsDouble());
            tag.putInt("Operation", operation.equals("addition") ? 0 : operation.equals("multiply_base") ? 1 : 2);
            tag.putUUID("UUID", uuid);
            if (modifier.has("slot")) tag.putString("Slot", modifier.get("slot").getAsString());
            list.add(tag);
        }
        root.put("AttributeModifiers", list);
        return true;
    }

    private static boolean isSupportedStewEffect(JsonObject function) {
        if (!function.has("effects") || !function.get("effects").isJsonObject()) return false;
        JsonObject effects = function.getAsJsonObject("effects");
        if (effects.size() != 1) return false;
        Map.Entry<String, JsonElement> effect = effects.entrySet().iterator().next();
        ResourceLocation id = ResourceLocation.tryParse(effect.getKey());
        JsonElement duration = effect.getValue();
        return id != null && ForgeRegistries.MOB_EFFECTS.getValue(id) != null
                && duration.isJsonPrimitive() && duration.getAsJsonPrimitive().isNumber()
                && Double.isFinite(duration.getAsDouble());
    }

    private static boolean applyStewEffect(ItemStack stack, JsonObject function) {
        if (!isSupportedStewEffect(function)) return false;
        Map.Entry<String, JsonElement> effectEntry = function.getAsJsonObject("effects").entrySet().iterator().next();
        ResourceLocation id = ResourceLocation.tryParse(effectEntry.getKey());
        MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        int duration = effectEntry.getValue().getAsInt();
        if (mobEffect == null || duration < 0 || effectEntry.getValue().getAsDouble() != duration) return false;
        CompoundTag root = stack.getOrCreateTag();
        ListTag effects = root.getList("Effects", 10);
        CompoundTag value = new CompoundTag();
        value.putByte("Effect", (byte) BuiltInRegistries.MOB_EFFECT.getId(mobEffect));
        value.putInt("Duration", duration);
        effects.add(value);
        root.put("Effects", effects);
        return true;
    }

    private static boolean copyNbt(ItemStack stack, JsonObject function, net.minecraft.world.entity.Entity entity) {
        if (!isSupportedCopyNbt(function)) return false;
        CompoundTag source = new CompoundTag();
        entity.saveWithoutId(source);
        CompoundTag target = stack.getOrCreateTag();
        for (JsonElement element : function.getAsJsonArray("ops")) {
            if (!element.isJsonObject()) return false;
            JsonObject operation = element.getAsJsonObject();
            if (!operation.has("op") || !operation.has("source") || !operation.has("target")) return false;
            String op = operation.get("op").getAsString();
            String sourcePath = operation.get("source").getAsString();
            String targetPath = operation.get("target").getAsString();
            if (sourcePath.indexOf('.') >= 0 || targetPath.indexOf('.') >= 0
                    || sourcePath.isEmpty() || targetPath.isEmpty()) return false;
            if (!source.contains(sourcePath)) continue;
            Tag value = source.get(sourcePath);
            if ("replace".equals(op)) {
                target.put(targetPath, value.copy());
            } else if ("merge".equals(op)) {
                if (!(value instanceof CompoundTag sourceCompound)) return false;
                CompoundTag targetCompound = target.getCompound(targetPath);
                targetCompound.merge(sourceCompound);
                target.put(targetPath, targetCompound);
            } else if ("append".equals(op)) {
                if (!(value instanceof ListTag sourceList)) return false;
                ListTag targetList = target.getList(targetPath, sourceList.getElementType());
                for (Tag child : sourceList) targetList.add(child.copy());
                target.put(targetPath, targetList);
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean isSupportedCopyNbt(JsonObject function) {
        if (!function.has("source") || !function.get("source").isJsonPrimitive()
                || !"this".equals(function.get("source").getAsString())
                || !function.has("ops") || !function.get("ops").isJsonArray()) return false;
        for (JsonElement element : function.getAsJsonArray("ops")) {
            if (!element.isJsonObject()) return false;
            JsonObject operation = element.getAsJsonObject();
            if (!operation.has("op") || !operation.has("source") || !operation.has("target")) return false;
            String sourcePath = operation.get("source").getAsString();
            String targetPath = operation.get("target").getAsString();
            String op = operation.get("op").getAsString();
            if (sourcePath.isEmpty() || targetPath.isEmpty() || sourcePath.indexOf('.') >= 0
                    || targetPath.indexOf('.') >= 0
                    || (!op.equals("replace") && !op.equals("merge") && !op.equals("append"))) return false;
        }
        return true;
    }

    private static Boolean matchesNbt(JsonObject expected, CompoundTag actual) {
        for (Map.Entry<String, JsonElement> entry : expected.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!actual.contains(key)) return false;
            Tag tag = actual.get(key);
            Boolean match = matchesNbtValue(value, tag);
            if (match == null || !match) return match;
        }
        return true;
    }

    private static Boolean matchesNbtValue(JsonElement expected, Tag actual) {
        if (expected.isJsonObject()) {
            return actual instanceof CompoundTag compound
                    ? matchesNbt(expected.getAsJsonObject(), compound) : Boolean.FALSE;
        }
        if (!expected.isJsonPrimitive()) return null;
        JsonPrimitive primitive = expected.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return actual instanceof net.minecraft.nbt.ByteTag byteTag
                    && byteTag.getAsByte() == (primitive.getAsBoolean() ? 1 : 0);
        }
        if (primitive.isNumber()) {
            if (actual instanceof net.minecraft.nbt.NumericTag numeric) {
                return Double.compare(numeric.getAsDouble(), primitive.getAsDouble()) == 0;
            }
            return false;
        }
        if (primitive.isString()) {
            return actual instanceof net.minecraft.nbt.StringTag stringTag
                    && stringTag.getAsString().equals(primitive.getAsString());
        }
        return null;
    }

    private static EquipmentSlot equipmentSlot(String name) {
        return switch (name) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            default -> null;
        };
    }

    private static Boolean equipmentPredicateMatches(JsonElement predicate, ItemStack stack) {
        if (!predicate.isJsonObject()) return null;
        JsonObject object = predicate.getAsJsonObject();
        for (String key : object.keySet()) {
            if (!key.equals("items") && !key.equals("count") && !key.equals("damage") && !key.equals("nbt")) return null;
        }
        if (object.has("items")) {
            JsonElement items = object.get("items");
            if (!items.isJsonArray()) return null;
            ResourceLocation actual = ForgeRegistries.ITEMS.getKey(stack.getItem());
            boolean match = false;
            for (JsonElement item : items.getAsJsonArray()) {
                if (!item.isJsonPrimitive()) return null;
                ResourceLocation expected = ResourceLocation.tryParse(item.getAsString());
                if (expected != null && expected.equals(actual)) match = true;
            }
            if (!match) return false;
        }
        if (object.has("count")) {
            Boolean match = checkRange(object.get("count"), stack.getCount());
            if (match == null) return null;
            if (!match) return false;
        }
        if (object.has("damage")) {
            if (!stack.isDamageableItem()) return false;
            Boolean match = checkRange(object.get("damage"), stack.getDamageValue());
            if (match == null) return null;
            if (!match) return false;
        }
        if (object.has("nbt")) {
            if (!object.get("nbt").isJsonObject()) return null;
            CompoundTag tag = stack.getTag();
            if (tag == null) return false;
            Boolean match = matchesNbt(object.getAsJsonObject("nbt"), tag);
            if (match == null) return null;
            if (!match) return false;
        }
        return true;
    }

}
