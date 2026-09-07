package com.suntide_20210418.dimensiontech.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class StructureLootAnalyzer {

    private StructureLootAnalyzer() {}

    public static List<StructureLoot> analyze(MinecraftServer server, MarkerInfo markerInfo) {
        MarkedStructure markedStructure = markerInfo.structure();
        Set<ResourceLocation> lootTables = findLootTables(server, markedStructure.id());
        LootTableItems tableItems = resolveLootTableItems(server, lootTables);
        return List.of(
                new StructureLoot(
                        markedStructure.id(),
                        sorted(lootTables),
                        sorted(tableItems.items()),
                        sorted(tableItems.resolvedTables())));
    }

    /**
     * Discovery result for value analysis; unlike {@link #analyze}, failure is never an empty list.
     */
    public static DiscoveryResult discoverForValue(ServerLevel level, MarkerInfo markerInfo) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        MarkedStructure marked = markerInfo.structure();
        Map<ResourceLocation, Integer> roots = new LinkedHashMap<>();
        findLootTables(level.getServer(), marked.id()).forEach(table -> roots.put(table, 1));
        findLoadedContainerLootTables(level, marked.bounds())
                .forEach((table, occurrences) -> roots.merge(table, occurrences, Integer::sum));
        if (roots.isEmpty()) {
            diagnostics.add(
                    new Diagnostic(
                            "DISCOVERY_SEMANTICS",
                            "No LootTable root could be found in structure templates or loaded "
                                    + "containers for structure "
                                    + marked.id()));
            return new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), diagnostics);
        }
        LootTableItems items = resolveLootTableItems(level.getServer(), roots.keySet(), false);
        StructureLoot structure =
                new StructureLoot(
                        marked.id(),
                        sorted(roots.keySet()),
                        sorted(items.items()),
                        sorted(items.resolvedTables()),
                        roots);
        return new DiscoveryResult(AnalysisStatus.EXACT, List.of(structure), List.of());
    }

    private static Map<ResourceLocation, Integer> findLoadedContainerLootTables(
            ServerLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox bounds) {
        Map<ResourceLocation, Integer> lootTables = new LinkedHashMap<>();
        for (int chunkX = bounds.minX() >> 4; chunkX <= bounds.maxX() >> 4; chunkX++) {
            for (int chunkZ = bounds.minZ() >> 4; chunkZ <= bounds.maxZ() >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                chunk.getBlockEntities()
                        .forEach(
                                (position, blockEntity) -> {
                                    if (bounds.isInside(position)) {
                                        containerLootTable(blockEntity.saveWithoutMetadata())
                                                .ifPresent(
                                                        table ->
                                                                lootTables.merge(
                                                                        table, 1, Integer::sum));
                                    }
                                });
            }
        }
        return lootTables;
    }

    private static Set<ResourceLocation> findLootTables(
            MinecraftServer server, ResourceLocation structureId) {

        Set<ResourceLocation> templateIds = new HashSet<>();
        Set<ResourceLocation> visitedPools = new HashSet<>();
        findStructureTemplates(server.getResourceManager(), structureId, visitedPools, templateIds);
        if (server.getStructureManager().get(structureId).isPresent()) {
            templateIds.add(structureId);
        }
        Set<ResourceLocation> lootTables = new HashSet<>();
        Set<ResourceLocation> scannedTemplates = new HashSet<>();
        while (scannedTemplates.size() < templateIds.size()) {
            ResourceLocation templateId =
                    sorted(templateIds).stream()
                            .filter(candidate -> !scannedTemplates.contains(candidate))
                            .findFirst()
                            .orElseThrow();
            scannedTemplates.add(templateId);
            Optional<StructureTemplate> template = server.getStructureManager().get(templateId);
            if (template.isEmpty()) {
                continue;
            }
            Set<ResourceLocation> referencedPools = new HashSet<>();
            scanTemplateNbt(
                    template.get().save(new CompoundTag()),
                    templateId,
                    lootTables,
                    referencedPools);
            for (ResourceLocation referencedPool : sorted(referencedPools)) {
                collectTemplatePool(
                        server.getResourceManager(), referencedPool, visitedPools, templateIds);
            }
        }
        return lootTables;
    }

    /**
     * Discovers data-pack templates without touching a world chunk. Catalogue analysis starts from
     * this safe baseline and supplements it with virtual sample observations when available.
     */
    public static DiscoveryResult discoverTemplateForValue(
            ServerLevel level,
            ResourceLocation structureId,
            AnalysisStatus status,
            List<Diagnostic> diagnostics) {
        Set<ResourceLocation> roots = findLootTables(level.getServer(), structureId);
        if (roots.isEmpty()) {
            List<Diagnostic> result = new ArrayList<>(diagnostics);
            result.add(
                    new Diagnostic(
                            "DISCOVERY_SEMANTICS",
                            "No LootTable root was found in templates for structure "
                                    + structureId));
            return new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), result);
        }
        LootTableItems items = resolveLootTableItems(level.getServer(), roots, false);
        Map<ResourceLocation, Integer> occurrences = new LinkedHashMap<>();
        roots.forEach(root -> occurrences.put(root, 1));
        return new DiscoveryResult(
                status,
                List.of(
                        new StructureLoot(
                                structureId,
                                sorted(roots),
                                sorted(items.items()),
                                sorted(items.resolvedTables()),
                                occurrences)),
                diagnostics);
    }

    public static DiscoveryResult discoverVirtualForValue(
            ServerLevel level,
            ResourceLocation structureId,
            Map<ResourceLocation, Integer> occurrences,
            int sampleCount,
            List<Diagnostic> diagnostics) {
        if (occurrences.isEmpty()) {
            List<Diagnostic> result = new ArrayList<>(diagnostics);
            result.add(
                    new Diagnostic(
                            "VIRTUAL_DISCOVERY",
                            "No container LootTable was found in valid virtual samples for"
                                    + " structure "
                                    + structureId));
            return new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), result);
        }
        LootTableItems items = resolveLootTableItems(level.getServer(), occurrences.keySet(), false);
        return new DiscoveryResult(
                AnalysisStatus.APPROXIMATE,
                List.of(
                        new StructureLoot(
                                structureId,
                                sorted(occurrences.keySet()),
                                sorted(items.items()),
                                sorted(items.resolvedTables()),
                                occurrences)),
                diagnostics,
                ExactProbability.of(1, Math.max(1, sampleCount)));
    }

    /** Builds an exact profile from a vanilla structure's fixed chest table locations. */
    public static DiscoveryResult discoverFixedForValue(
            ServerLevel level,
            ResourceLocation structureId,
            Collection<ResourceLocation> roots,
            List<Diagnostic> diagnostics) {
        if (roots.isEmpty()) {
            return new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), diagnostics);
        }
        Set<ResourceLocation> rootSet = new HashSet<>(roots);
        LootTableItems items = resolveLootTableItems(level.getServer(), rootSet, false);
        Map<ResourceLocation, Integer> occurrences = new LinkedHashMap<>();
        rootSet.forEach(root -> occurrences.put(root, 1));
        return new DiscoveryResult(
                AnalysisStatus.EXACT,
                List.of(
                        new StructureLoot(
                                structureId,
                                sorted(rootSet),
                                sorted(items.items()),
                                sorted(items.resolvedTables()),
                                occurrences)),
                diagnostics);
    }

    /** Extracts the table assigned by structure generation to a container block entity. */
    static Optional<ResourceLocation> containerLootTable(CompoundTag containerData) {
        if (!containerData.contains("LootTable", Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(containerData.getString("LootTable")));
    }

    private static void findStructureTemplates(
            ResourceManager resourceManager,
            ResourceLocation structureId,
            Set<ResourceLocation> visitedPools,
            Set<ResourceLocation> templateIds) {
        ResourceLocation structureResource =
                dataResource(structureId, "worldgen/structure/", ".json");
        Optional<JsonElement> structureJson = readJson(resourceManager, structureResource);
        if (structureJson.isEmpty() || !structureJson.get().isJsonObject()) {
            return;
        }

        JsonObject structure = structureJson.get().getAsJsonObject();
        if (!structure.has("start_pool") || !structure.get("start_pool").isJsonPrimitive()) {
            return;
        }
        ResourceLocation startPool =
                ResourceLocation.tryParse(structure.get("start_pool").getAsString());
        if (startPool == null) {
            return;
        }
        collectTemplatePool(resourceManager, startPool, visitedPools, templateIds);
    }

    private static void collectTemplatePool(
            ResourceManager resourceManager,
            ResourceLocation poolId,
            Set<ResourceLocation> visitedPools,
            Set<ResourceLocation> templateIds) {
        if (!visitedPools.add(poolId)) {
            return;
        }

        ResourceLocation poolResource = dataResource(poolId, "worldgen/template_pool/", ".json");
        Optional<JsonElement> poolJson = readJson(resourceManager, poolResource);
        if (poolJson.isEmpty() || !poolJson.get().isJsonObject()) {
            return;
        }

        JsonObject pool = poolJson.get().getAsJsonObject();
        collectPoolElements(pool, poolId, templateIds);

        if (pool.has("fallback") && pool.get("fallback").isJsonPrimitive()) {
            ResourceLocation fallback =
                    ResourceLocation.tryParse(pool.get("fallback").getAsString());
            if (fallback != null
                    && !fallback.equals(ResourceLocationHelper.loc("minecraft", "empty"))) {
                collectTemplatePool(resourceManager, fallback, visitedPools, templateIds);
            }
        }
    }

    private static void collectPoolElements(
            JsonElement element, ResourceLocation poolId, Set<ResourceLocation> templateIds) {
        if (element.isJsonArray()) {
            element.getAsJsonArray()
                    .forEach(child -> collectPoolElements(child, poolId, templateIds));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        String elementType =
                object.has("element_type") ? object.get("element_type").getAsString() : "";
        if (("minecraft:single_pool_element".equals(elementType)
                        || "minecraft:legacy_single_pool_element".equals(elementType))
                && object.has("location")
                && object.get("location").isJsonPrimitive()) {
            ResourceLocation templateId =
                    ResourceLocation.tryParse(object.get("location").getAsString());
            if (templateId != null) {
                templateIds.add(templateId);
            }
        }
        object.entrySet()
                .forEach(entry -> collectPoolElements(entry.getValue(), poolId, templateIds));
    }

    private static void scanTemplateNbt(
            Tag tag,
            ResourceLocation templateId,
            Set<ResourceLocation> lootTables,
            Set<ResourceLocation> referencedPools) {
        if (tag instanceof CompoundTag compoundTag) {
            for (String key : compoundTag.getAllKeys()) {
                Tag child = compoundTag.get(key);
                if (child == null) {
                    continue;
                }
                if ("LootTable".equals(key) && child.getId() == Tag.TAG_STRING) {
                    ResourceLocation lootTable = ResourceLocation.tryParse(child.getAsString());
                    if (lootTable != null) {
                        lootTables.add(lootTable);
                    }
                } else if ("pool".equals(key) && child.getId() == Tag.TAG_STRING) {
                    ResourceLocation poolId = ResourceLocation.tryParse(child.getAsString());
                    if (poolId != null
                            && !poolId.equals(ResourceLocationHelper.loc("minecraft", "empty"))) {
                        referencedPools.add(poolId);
                    }
                } else {
                    scanTemplateNbt(child, templateId, lootTables, referencedPools);
                }
            }
        } else if (tag instanceof ListTag listTag) {
            listTag.forEach(
                    child -> scanTemplateNbt(child, templateId, lootTables, referencedPools));
        }
    }

    private static Optional<JsonElement> readJson(
            ResourceManager resourceManager, ResourceLocation resourceId) {
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return Optional.of(JsonParser.parseReader(reader));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static ResourceLocation dataResource(
            ResourceLocation id, String directory, String extension) {
        return ResourceLocationHelper.loc(id.getNamespace(), directory + id.getPath() + extension);
    }

    private static LootTableItems resolveLootTableItems(
            MinecraftServer server, Set<ResourceLocation> rootTables) {
        return resolveLootTableItems(server, rootTables, true);
    }

    private static LootTableItems resolveLootTableItems(
            MinecraftServer server, Set<ResourceLocation> rootTables, boolean applyItemFilter) {
        Set<ResourceLocation> resolvedTables = new HashSet<>();
        Set<ResourceLocation> items = new HashSet<>();
        for (ResourceLocation lootTable : sorted(rootTables)) {
            resolveLootTable(server.getResourceManager(), lootTable, resolvedTables, items);
        }
        if (applyItemFilter) items.removeIf(item -> !ModConfigs.STRUCTURE_VALUE.allowsItem(item));
        return new LootTableItems(resolvedTables, items);
    }

    private static void resolveLootTable(
            ResourceManager resourceManager,
            ResourceLocation lootTable,
            Set<ResourceLocation> resolvedTables,
            Set<ResourceLocation> items) {
        if (!resolvedTables.add(lootTable)) {
            return;
        }

        ResourceLocation resourceId =
                ResourceLocationHelper.loc(
                        lootTable.getNamespace(), "loot_tables/" + lootTable.getPath() + ".json");
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return;
        }

        try (Reader reader = resource.get().openAsReader()) {
            collectJsonEntries(
                    JsonParser.parseReader(reader),
                    lootTable,
                    resourceManager,
                    resolvedTables,
                    items);
        } catch (IOException | RuntimeException exception) {
        }
    }

    private static void collectJsonEntries(
            JsonElement element,
            ResourceLocation currentTable,
            ResourceManager resourceManager,
            Set<ResourceLocation> resolvedTables,
            Set<ResourceLocation> items) {
        if (element.isJsonArray()) {
            element.getAsJsonArray()
                    .forEach(
                            child ->
                                    collectJsonEntries(
                                            child,
                                            currentTable,
                                            resourceManager,
                                            resolvedTables,
                                            items));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        if ("minecraft:item".equals(type) && object.has("name")) {
            addItem(currentTable, object.get("name").getAsString(), items);
        } else if ("minecraft:tag".equals(type) && object.has("name")) {
            addItemTag(currentTable, object.get("name").getAsString(), items);
        } else if ("minecraft:loot_table".equals(type) && object.has("name")) {
            String nestedTableValue = object.get("name").getAsString();
            ResourceLocation nestedTable = ResourceLocation.tryParse(nestedTableValue);
            if (nestedTable != null) {
                resolveLootTable(resourceManager, nestedTable, resolvedTables, items);
            }
        }
        if (object.has("stack") && object.get("stack").isJsonObject()) {
            JsonObject stack = object.getAsJsonObject("stack");
            if (stack.has("item") && stack.get("item").isJsonPrimitive()) {
                addItem(currentTable, stack.get("item").getAsString(), items);
            }
        }

        object.entrySet()
                .forEach(
                        entry ->
                                collectJsonEntries(
                                        entry.getValue(),
                                        currentTable,
                                        resourceManager,
                                        resolvedTables,
                                        items));
    }

    private static void addItemTag(
            ResourceLocation currentTable, String value, Set<ResourceLocation> items) {
        ResourceLocation tagId = ResourceLocation.tryParse(value);
        if (tagId == null) {
            return;
        }
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        BuiltInRegistries.ITEM
                .getTag(tagKey)
                .ifPresent(
                        holders ->
                                holders.forEach(
                                        holder ->
                                                items.add(
                                                        BuiltInRegistries.ITEM.getKey(
                                                                holder.value()))));
    }

    private static void addItem(
            ResourceLocation currentTable, String value, Set<ResourceLocation> items) {
        ResourceLocation itemId = ResourceLocation.tryParse(value);
        if (itemId == null) {
            return;
        }
        items.add(itemId);
    }

    private static List<ResourceLocation> sorted(Collection<ResourceLocation> values) {
        return values.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private record LootTableItems(
            Set<ResourceLocation> resolvedTables, Set<ResourceLocation> items) {}

    public record StructureLoot(
            ResourceLocation structure,
            List<ResourceLocation> lootTables,
            List<ResourceLocation> items,
            List<ResourceLocation> resolvedTables,
            Map<ResourceLocation, Integer> occurrences) {
        public StructureLoot {
            lootTables = List.copyOf(lootTables);
            items = List.copyOf(items);
            resolvedTables = List.copyOf(resolvedTables);
            occurrences = Map.copyOf(occurrences);
        }

        /** Compatibility constructor for callers that only have distinct root tables. */
        public StructureLoot(
                ResourceLocation structure,
                List<ResourceLocation> lootTables,
                List<ResourceLocation> items,
                List<ResourceLocation> resolvedTables) {
            this(structure, lootTables, items, resolvedTables, unitOccurrences(lootTables));
        }

        private static Map<ResourceLocation, Integer> unitOccurrences(
                List<ResourceLocation> lootTables) {
            Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
            lootTables.forEach(table -> result.merge(table, 1, Integer::sum));
            return result;
        }
    }

    public record DiscoveryResult(
            AnalysisStatus status,
            List<StructureLoot> structures,
            List<Diagnostic> diagnostics,
            ExactProbability occurrenceScale) {
        public DiscoveryResult {
            structures = List.copyOf(structures);
            diagnostics = List.copyOf(diagnostics);
        }

        public DiscoveryResult(
                AnalysisStatus status,
                List<StructureLoot> structures,
                List<Diagnostic> diagnostics) {
            this(status, structures, diagnostics, ExactProbability.ONE);
        }
    }
}
