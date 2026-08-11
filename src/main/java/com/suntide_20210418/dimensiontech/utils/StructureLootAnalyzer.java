package com.suntide_20210418.dimensiontech.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.custom.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.custom.StructMarkerItem.MarkerInfo;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class StructureLootAnalyzer {

    private StructureLootAnalyzer() {
    }

    public static List<StructureLoot> analyze(MinecraftServer server, MarkerInfo markerInfo) {
        ResourceKey<Level> dimensionKey =
                ResourceKey.create(Registries.DIMENSION, markerInfo.dimension());
        ServerLevel targetLevel = server.getLevel(dimensionKey);
        if (targetLevel == null) {
            return List.of();
        }

        Registry<Structure> structureRegistry =
                targetLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<StructureLoot> results = new ArrayList<>();
        for (MarkedStructure markedStructure : markerInfo.structures()) {
            Structure structure = structureRegistry.get(markedStructure.id());
            if (structure == null) {
                continue;
            }

            StructureStart start =
                    targetLevel.structureManager().getStructureAt(markerInfo.position(), structure);
            Set<ResourceLocation> lootTables = new HashSet<>();
            if (start.isValid()) {
                collectPieceLootTables(targetLevel, start, lootTables);
            }
            collectBlockEntityLootTables(targetLevel, markedStructure, lootTables);

            LootTableItems tableItems = resolveLootTableItems(server, lootTables);
            results.add(
                    new StructureLoot(
                            markedStructure.id(),
                            sorted(lootTables),
                            sorted(tableItems.items()),
                            sorted(tableItems.resolvedTables())));
        }
        return List.copyOf(results);
    }

    private static void collectPieceLootTables(
            ServerLevel level, StructureStart start, Set<ResourceLocation> lootTables) {
        StructurePieceSerializationContext context =
                StructurePieceSerializationContext.fromLevel(level);
        for (StructurePiece piece : start.getPieces()) {
            scanNbtForLootTables(piece.createTag(context), lootTables);
            if (piece instanceof TemplateStructurePiece templatePiece) {
                scanNbtForLootTables(
                        templatePiece.template().save(new CompoundTag()), lootTables);
            }
        }
    }

    private static void collectBlockEntityLootTables(
            ServerLevel level,
            MarkedStructure markedStructure,
            Set<ResourceLocation> lootTables) {
        int minChunkX = markedStructure.bounds().minX() >> 4;
        int maxChunkX = markedStructure.bounds().maxX() >> 4;
        int minChunkZ = markedStructure.bounds().minZ() >> 4;
        int maxChunkZ = markedStructure.bounds().maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Collection<BlockEntity> blockEntities =
                        level.getChunk(chunkX, chunkZ).getBlockEntities().values();
                for (BlockEntity blockEntity : blockEntities) {
                    if (markedStructure.bounds().isInside(blockEntity.getBlockPos())) {
                        scanNbtForLootTables(blockEntity.saveWithoutMetadata(), lootTables);
                    }
                }
            }
        }
    }

    private static void scanNbtForLootTables(Tag tag, Set<ResourceLocation> lootTables) {
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
                } else {
                    scanNbtForLootTables(child, lootTables);
                }
            }
        } else if (tag instanceof ListTag listTag) {
            for (Tag child : listTag) {
                scanNbtForLootTables(child, lootTables);
            }
        }
    }

    private static LootTableItems resolveLootTableItems(
            MinecraftServer server, Set<ResourceLocation> rootTables) {
        Set<ResourceLocation> resolvedTables = new HashSet<>();
        Set<ResourceLocation> items = new HashSet<>();
        for (ResourceLocation lootTable : rootTables) {
            resolveLootTable(
                    server.getResourceManager(), lootTable, resolvedTables, items);
        }
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
                        lootTable.getNamespace(),
                        "loot_tables/" + lootTable.getPath() + ".json");
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isEmpty()) {
            return;
        }

        try (Reader reader = resource.get().openAsReader()) {
            collectJsonEntries(
                    JsonParser.parseReader(reader), resourceManager, resolvedTables, items);
        } catch (IOException | RuntimeException exception) {
            DimensionTechMod.LOGGER.debug(
                    "Unable to inspect loot table {}", lootTable, exception);
        }
    }

    private static void collectJsonEntries(
            JsonElement element,
            ResourceManager resourceManager,
            Set<ResourceLocation> resolvedTables,
            Set<ResourceLocation> items) {
        if (element.isJsonArray()) {
            element.getAsJsonArray()
                    .forEach(
                            child ->
                                    collectJsonEntries(
                                            child, resourceManager, resolvedTables, items));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        if ("minecraft:item".equals(type) && object.has("name")) {
            addResourceLocation(object.get("name").getAsString(), items);
        } else if ("minecraft:tag".equals(type) && object.has("name")) {
            addItemTag(object.get("name").getAsString(), items);
        } else if ("minecraft:loot_table".equals(type) && object.has("name")) {
            ResourceLocation nestedTable =
                    ResourceLocation.tryParse(object.get("name").getAsString());
            if (nestedTable != null) {
                resolveLootTable(resourceManager, nestedTable, resolvedTables, items);
            }
        }

        object.entrySet()
                .forEach(
                        entry ->
                                collectJsonEntries(
                                        entry.getValue(), resourceManager, resolvedTables, items));
    }

    private static void addItemTag(String value, Set<ResourceLocation> items) {
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

    private static void addResourceLocation(
            String value, Set<ResourceLocation> resourceLocations) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(value);
        if (resourceLocation != null) {
            resourceLocations.add(resourceLocation);
        }
    }

    private static List<ResourceLocation> sorted(Set<ResourceLocation> values) {
        return values.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private record LootTableItems(
            Set<ResourceLocation> resolvedTables, Set<ResourceLocation> items) {
    }

    public record StructureLoot(
            ResourceLocation structure,
            List<ResourceLocation> lootTables,
            List<ResourceLocation> items,
            List<ResourceLocation> resolvedTables) {
    }
}
