package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.ForgeRegistries;

/** Serializes the server's loaded loot objects, including Forge load-event modifications. */
public class RuntimeLootAstSource {
    // A frozen source must be usable without initializing Minecraft's live loot registries.
    private static final class RuntimeSerializers {
        static final Gson TABLE = Deserializers.createLootTableSerializer().create();
        static final Gson PREDICATE = Deserializers.createConditionSerializer().create();
        static final Gson MODIFIER = Deserializers.createFunctionSerializer().create();
    }

    private final LootDataManager lootData;
    private final ResourceManager resources;
    private final Map<ResourceLocation, FrozenJson> tableSnapshot;
    private final Map<ResourceLocation, FrozenJson> predicateSnapshot;
    private final Map<ResourceLocation, FrozenJson> modifierSnapshot;
    private final String runtimeSemanticsFingerprint;

    public RuntimeLootAstSource(MinecraftServer server) {
        this.lootData = server.getLootData();
        this.resources = server.getResourceManager();
        this.tableSnapshot = Map.of();
        this.predicateSnapshot = Map.of();
        this.modifierSnapshot = Map.of();
        this.runtimeSemanticsFingerprint = "";
    }

    private RuntimeLootAstSource(
            Map<ResourceLocation, JsonElement> tables,
            Map<ResourceLocation, JsonElement> predicates,
            Map<ResourceLocation, JsonElement> modifiers,
            String runtimeSemanticsFingerprint) {
        this.lootData = null;
        this.resources = null;
        this.tableSnapshot = deepCopy(tables);
        this.predicateSnapshot = deepCopy(predicates);
        this.modifierSnapshot = deepCopy(modifiers);
        this.runtimeSemanticsFingerprint =
                java.util.Objects.requireNonNull(runtimeSemanticsFingerprint);
    }

    /** Creates a thread-safe source from already serialized, deeply copied JSON data. */
    public static RuntimeLootAstSource snapshot(
            Map<ResourceLocation, JsonElement> tables,
            Map<ResourceLocation, JsonElement> predicates,
            Map<ResourceLocation, JsonElement> modifiers) {
        return new RuntimeLootAstSource(tables, predicates, modifiers, "");
    }

    /** Captures root tables and recursively referenced loot tables on the owning server thread. */
    public static RuntimeLootAstSource snapshotTables(
            MinecraftServer server, Collection<ResourceLocation> roots) {
        RuntimeLootAstSource live = new RuntimeLootAstSource(server);
        LinkedHashMap<ResourceLocation, JsonElement> tables = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonElement> predicates = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, JsonElement> modifiers = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> pending = new LinkedHashSet<>(roots);
        LinkedHashSet<ResourceLocation> referenced = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            ResourceLocation id = pending.iterator().next();
            pending.remove(id);
            if (tables.containsKey(id)) continue;
            Optional<RuntimeAst<LootTable>> table = live.table(id);
            if (table.isEmpty()) continue;
            JsonElement json = table.get().json().deepCopy();
            tables.put(id, json);
            collectNestedTables(json, pending);
            collectReferences(json, referenced);
        }
        LinkedHashSet<ResourceLocation> visitedReferences = new LinkedHashSet<>();
        while (!referenced.isEmpty()) {
            ResourceLocation id = referenced.iterator().next();
            referenced.remove(id);
            if (!visitedReferences.add(id)) continue;
            live.predicate(id)
                    .ifPresent(
                            value -> {
                                predicates.put(id, value.json());
                                collectReferences(value.json(), referenced);
                            });
            live.modifier(id)
                    .ifPresent(
                            value -> {
                                modifiers.put(id, value.json());
                                collectReferences(value.json(), referenced);
                            });
        }
        return new RuntimeLootAstSource(
                tables, predicates, modifiers, runtimeSemanticsFingerprint());
    }

    private static void collectNestedTables(
            JsonElement element, Collection<ResourceLocation> output) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectNestedTables(child, output));
            return;
        }
        if (!element.isJsonObject()) return;
        var object = element.getAsJsonObject();
        if (object.has("type")
                && object.has("name")
                && "minecraft:loot_table".equals(object.get("type").getAsString())) {
            ResourceLocation id = ResourceLocation.tryParse(object.get("name").getAsString());
            if (id != null) output.add(id);
        }
        object.entrySet().forEach(entry -> collectNestedTables(entry.getValue(), output));
    }

    private static void collectReferences(
            JsonElement element, Collection<ResourceLocation> output) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectReferences(child, output));
            return;
        }
        if (!element.isJsonObject()) return;
        var object = element.getAsJsonObject();
        if (object.has("name")) {
            JsonElement kind =
                    object.has("condition")
                            ? object.get("condition")
                            : object.has("function") ? object.get("function") : object.get("type");
            String type = kind != null && kind.isJsonPrimitive() ? kind.getAsString() : "";
            if (type.equals("reference")
                    || type.equals("referenced")
                    || type.endsWith(":reference")
                    || type.endsWith(":referenced")) {
                ResourceLocation id = ResourceLocation.tryParse(object.get("name").getAsString());
                if (id != null) output.add(id);
            }
        }
        object.entrySet().forEach(entry -> collectReferences(entry.getValue(), output));
    }

    public ResourceManager resources() {
        return resources;
    }

    public Optional<RuntimeAst<LootTable>> table(ResourceLocation id) {
        FrozenJson snap = tableSnapshot.get(id);
        if (snap != null) return Optional.of(new RuntimeAst<LootTable>(snap, snap.toJson()));
        if (lootData == null) return Optional.empty();
        return lootData.getElementOptional(LootDataType.TABLE, id)
                .map(
                        value ->
                                new RuntimeAst<>(
                                        value, serializeObject(RuntimeSerializers.TABLE, value)));
    }

    public Optional<RuntimeAst<LootItemCondition>> predicate(ResourceLocation id) {
        FrozenJson snap = predicateSnapshot.get(id);
        if (snap != null)
            return Optional.of(new RuntimeAst<LootItemCondition>(snap, snap.toJson()));
        if (lootData == null) return Optional.empty();
        return lootData.getElementOptional(LootDataType.PREDICATE, id)
                .map(
                        value ->
                                new RuntimeAst<>(
                                        value, RuntimeSerializers.PREDICATE.toJsonTree(value)));
    }

    public Optional<RuntimeAst<LootItemFunction>> modifier(ResourceLocation id) {
        FrozenJson snap = modifierSnapshot.get(id);
        if (snap != null) return Optional.of(new RuntimeAst<LootItemFunction>(snap, snap.toJson()));
        if (lootData == null) return Optional.empty();
        return lootData.getElementOptional(LootDataType.MODIFIER, id)
                .map(
                        value ->
                                new RuntimeAst<>(
                                        value, RuntimeSerializers.MODIFIER.toJsonTree(value)));
    }

    private static JsonObject serializeObject(Gson serializer, Object value) {
        JsonElement element = serializer.toJsonTree(value);
        if (!element.isJsonObject()) {
            throw new IllegalStateException("Loot table serializer did not produce an object");
        }
        return element.getAsJsonObject();
    }

    /** Includes recursively referenced tables, predicates and functions, in stable key order. */
    public String inputFingerprint() {
        if (lootData != null) throw new IllegalStateException("Freeze the live source first");
        return new FrozenJson.ObjectValue(
                        Map.of(
                                "tables", frozenEntries(tableSnapshot),
                                "predicates", frozenEntries(predicateSnapshot),
                                "modifiers", frozenEntries(modifierSnapshot),
                                "runtimeSemantics",
                                        new FrozenJson.ScalarValue(
                                                new com.google.gson.Gson()
                                                        .toJson(runtimeSemanticsFingerprint))))
                .fingerprint();
    }

    /** Must be called on the server thread immediately before runtime-backed evaluation. */
    public void verifyRuntimeInputs() {
        if (!runtimeSemanticsFingerprint.isEmpty()
                && !runtimeSemanticsFingerprint.equals(runtimeSemanticsFingerprint())) {
            throw new IllegalStateException(
                    "Runtime registry/tag inputs changed after snapshot capture");
        }
    }

    private static String runtimeSemanticsFingerprint() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonObject itemTags = new com.google.gson.JsonObject();
        if (ForgeRegistries.ITEMS.tags() != null) {
            ForgeRegistries.ITEMS
                    .tags()
                    .getTagNames()
                    .sorted(Comparator.comparing(tag -> tag.location().toString()))
                    .forEach(
                            tag -> {
                                com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
                                ForgeRegistries.ITEMS.tags().getTag(tag).stream()
                                        .map(ForgeRegistries.ITEMS::getKey)
                                        .filter(java.util.Objects::nonNull)
                                        .map(ResourceLocation::toString)
                                        .sorted()
                                        .forEach(ids::add);
                                itemTags.add(tag.location().toString(), ids);
                            });
        }
        root.add("itemTags", itemTags);
        com.google.gson.JsonObject instrumentTags = new com.google.gson.JsonObject();
        BuiltInRegistries.INSTRUMENT
                .getTagNames()
                .sorted(Comparator.comparing(tag -> tag.location().toString()))
                .forEach(
                        tag -> {
                            com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
                            BuiltInRegistries.INSTRUMENT
                                    .getTag(tag)
                                    .ifPresent(
                                            set ->
                                                    set.forEach(
                                                            holder ->
                                                                    holder.unwrapKey()
                                                                            .ifPresent(
                                                                                    key ->
                                                                                            ids.add(
                                                                                                    key.location()
                                                                                                            .toString()))));
                            instrumentTags.add(tag.location().toString(), ids);
                        });
        root.add("instrumentTags", instrumentTags);
        return FrozenJson.freeze(root).fingerprint();
    }

    private static FrozenJson frozenEntries(Map<ResourceLocation, FrozenJson> entries) {
        Map<String, FrozenJson> fields = new LinkedHashMap<>();
        entries.forEach((id, value) -> fields.put(id.toString(), value));
        return new FrozenJson.ObjectValue(fields);
    }

    private static <T extends JsonElement> Map<ResourceLocation, FrozenJson> deepCopy(
            Map<ResourceLocation, T> source) {
        LinkedHashMap<ResourceLocation, FrozenJson> copy = new LinkedHashMap<>();
        source.forEach((id, json) -> copy.put(id, FrozenJson.freeze(json)));
        return Map.copyOf(copy);
    }

    public record RuntimeAst<T>(Object identity, JsonElement json) {}
}
