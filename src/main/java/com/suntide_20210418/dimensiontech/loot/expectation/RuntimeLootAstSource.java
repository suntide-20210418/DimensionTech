package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

/**
 * Serializes the server's loaded loot objects, including NeoForge load-event modifications.
 *
 * <p>1.21 取消了 {@code LootDataManager} 与 {@code Deserializers.create*Serializer()}：战利品表、谓词与
 * 物品修饰器现在是普通注册表（{@code Registries.LOOT_TABLE} / {@code PREDICATE} / {@code ITEM_MODIFIER}），
 * 元素在装载时已经过 {@code LootDataType.deserialize}（其中会触发 NeoForge 的 {@code loadLootTable} 事件）。 序列化改走各
 * {@code LootDataType} 对应的 DIRECT/ROOT codec，并且必须配 {@link RegistryOps}： 1.21 的附魔是 {@code
 * Holder<Enchantment>}，Gson 路线无法还原。因此本类全程持有 {@link RegistryAccess}。
 */
public class RuntimeLootAstSource {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final RegistryAccess registries;
    private final Registry<LootTable> tables;
    private final Registry<LootItemCondition> predicates;
    private final Registry<LootItemFunction> modifiers;
    private final ResourceManager resources;
    private final Map<ResourceLocation, FrozenJson> tableSnapshot;
    private final Map<ResourceLocation, FrozenJson> predicateSnapshot;
    private final Map<ResourceLocation, FrozenJson> modifierSnapshot;
    private final String runtimeSemanticsFingerprint;

    public RuntimeLootAstSource(MinecraftServer server) {
        this.registries = server.registryAccess();
        this.tables = this.registries.registryOrThrow(Registries.LOOT_TABLE);
        this.predicates = this.registries.registryOrThrow(Registries.PREDICATE);
        this.modifiers = this.registries.registryOrThrow(Registries.ITEM_MODIFIER);
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
            RegistryAccess registries,
            String runtimeSemanticsFingerprint) {
        this.registries = java.util.Objects.requireNonNull(registries, "registries");
        this.tables = null;
        this.predicates = null;
        this.modifiers = null;
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
            Map<ResourceLocation, JsonElement> modifiers,
            RegistryAccess registries) {
        return new RuntimeLootAstSource(tables, predicates, modifiers, registries, "");
    }

    /** Captures root tables and recursively referenced loot tables on the owning server thread. */
    public static RuntimeLootAstSource snapshotTables(
            MinecraftServer server, Collection<ResourceLocation> roots) {
        long start = System.nanoTime();
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
        long fingerprintStart = System.nanoTime();
        String semanticsFingerprint = live.runtimeSemanticsFingerprint();
        long fingerprintMillis = (System.nanoTime() - fingerprintStart) / 1_000_000L;
        RuntimeLootAstSource result =
                new RuntimeLootAstSource(
                        tables, predicates, modifiers, live.registries, semanticsFingerprint);
        long totalMillis = (System.nanoTime() - start) / 1_000_000L;
        if (totalMillis > 20) {
            LOGGER.warn(
                    "[TEMP PROBE] snapshotTables roots={} tables={} preds={} mods={} totalMs={}"
                            + " fingerprintMs={} on {}",
                    roots.size(),
                    tables.size(),
                    predicates.size(),
                    modifiers.size(),
                    totalMillis,
                    fingerprintMillis,
                    java.lang.Thread.currentThread().getName());
        }
        return result;
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

    /** Registry context needed to decode/encode registry-backed component values. */
    public RegistryAccess registries() {
        return registries;
    }

    public Optional<RuntimeAst<LootTable>> table(ResourceLocation id) {
        FrozenJson snap = tableSnapshot.get(id);
        if (snap != null) return Optional.of(new RuntimeAst<LootTable>(snap, snap.toJson()));
        if (tables == null) return Optional.empty();
        LootTable value = tables.get(id);
        if (value == null) return Optional.empty();
        return Optional.of(new RuntimeAst<>(value, serializeObject(LootTable.DIRECT_CODEC, value)));
    }

    public Optional<RuntimeAst<LootItemCondition>> predicate(ResourceLocation id) {
        FrozenJson snap = predicateSnapshot.get(id);
        if (snap != null)
            return Optional.of(new RuntimeAst<LootItemCondition>(snap, snap.toJson()));
        if (predicates == null) return Optional.empty();
        LootItemCondition value = predicates.get(id);
        if (value == null) return Optional.empty();
        return Optional.of(
                new RuntimeAst<>(value, serializeObject(LootItemCondition.DIRECT_CODEC, value)));
    }

    public Optional<RuntimeAst<LootItemFunction>> modifier(ResourceLocation id) {
        FrozenJson snap = modifierSnapshot.get(id);
        if (snap != null) return Optional.of(new RuntimeAst<LootItemFunction>(snap, snap.toJson()));
        if (modifiers == null) return Optional.empty();
        LootItemFunction value = modifiers.get(id);
        if (value == null) return Optional.empty();
        return Optional.of(
                new RuntimeAst<>(value, serializeObject(LootItemFunctions.ROOT_CODEC, value)));
    }

    private <T> JsonObject serializeObject(Codec<T> codec, T value) {
        JsonElement element = codec.encodeStart(registryOps(), value).getOrThrow();
        if (!element.isJsonObject()) {
            throw new IllegalStateException("Loot serializer did not produce an object");
        }
        return element.getAsJsonObject();
    }

    private RegistryOps<JsonElement> registryOps() {
        return RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    /** Includes recursively referenced tables, predicates and functions, in stable key order. */
    public String inputFingerprint() {
        if (tables != null) throw new IllegalStateException("Freeze the live source first");
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

    /**
     * 1.21 的附魔不再位于内置注册表，而是 datapack 注册表 + 标签：可用附魔候选集由 {@code options}（默认整个注册表）与 {@code
     * ItemStack#isPrimaryItemFor} 决定 （见 {@code
     * EnchantmentHelper#getAvailableEnchantmentResults}），"可发现性" 由 {@code
     * EnchantmentTags#IN_ENCHANTING_TABLE} / {@code NON_TREASURE} 驱动 （见 {@code
     * EnchantmentMenu}）。这些数据都参与语义，所以一并纳入指纹。
     */
    private String runtimeSemanticsFingerprint() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("itemTags", tagFingerprint(BuiltInRegistries.ITEM));
        root.add("instrumentTags", tagFingerprint(BuiltInRegistries.INSTRUMENT));
        Registry<Enchantment> enchantments = registries.registryOrThrow(Registries.ENCHANTMENT);
        root.add("enchantmentTags", tagFingerprint(enchantments));
        com.google.gson.JsonArray enchantmentIds = new com.google.gson.JsonArray();
        enchantments.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(enchantmentIds::add);
        root.add("enchantments", enchantmentIds);
        return FrozenJson.freeze(root).fingerprint();
    }

    private static <T> JsonObject tagFingerprint(Registry<T> registry) {
        JsonObject tags = new JsonObject();
        registry.getTagNames()
                .sorted(Comparator.comparing(tag -> tag.location().toString()))
                .forEach(
                        tag -> {
                            JsonArray ids = new JsonArray();
                            registry.getTag(tag)
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
                            tags.add(tag.location().toString(), ids);
                        });
        return tags;
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
