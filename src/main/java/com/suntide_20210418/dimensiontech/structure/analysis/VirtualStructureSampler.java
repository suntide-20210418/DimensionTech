package com.suntide_20210418.dimensiontech.structure.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Places one generated structure into disposable {@link ProtoChunk}s and reads container NBT. */
final class VirtualStructureSampler {
    private VirtualStructureSampler() {}

    static Sample sample(ServerLevel level, Structure structure, BlockPos origin) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos startChunk = new ChunkPos(origin);
        StructureStart start =
                structure.generate(
                        level.registryAccess(),
                        generator,
                        generator.getBiomeSource(),
                        level.getChunkSource().randomState(),
                        level.getServer().getStructureManager(),
                        level.getSeed(),
                        startChunk,
                        0,
                        level,
                        structure.biomes()::contains);
        if (!start.isValid()) return Sample.invalid();

        BoundingBox bounds = start.getBoundingBox();
        int radius =
                Math.max(
                        Math.max(
                                Math.abs((bounds.minX() >> 4) - startChunk.x),
                                Math.abs((bounds.maxX() >> 4) - startChunk.x)),
                        Math.max(
                                Math.abs((bounds.minZ() >> 4) - startChunk.z),
                                Math.abs((bounds.maxZ() >> 4) - startChunk.z)));
        // Structure pieces may query their immediately adjacent chunk while placing.
        radius += 1;
        Registry<net.minecraft.world.level.biome.Biome> biomes =
                level.registryAccess().registryOrThrow(Registries.BIOME);
        List<ChunkAccess> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int z = startChunk.z - radius; z <= startChunk.z + radius; z++) {
            for (int x = startChunk.x - radius; x <= startChunk.x + radius; x++) {
                ProtoChunk chunk =
                        new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, biomes, null);
                chunk.setStatus(ChunkStatus.FEATURES);
                chunks.add(chunk);
            }
        }
        WorldGenRegion region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, radius);
        for (ChunkAccess chunk : chunks) {
            ChunkPos chunkPos = chunk.getPos();
            BoundingBox writable =
                    new BoundingBox(
                            chunkPos.getMinBlockX(),
                            level.getMinBuildHeight() + 1,
                            chunkPos.getMinBlockZ(),
                            chunkPos.getMaxBlockX(),
                            level.getMaxBuildHeight() - 1,
                            chunkPos.getMaxBlockZ());
            if (bounds.intersects(writable)) {
                start.placeInChunk(
                        region,
                        level.structureManager(),
                        generator,
                        RandomSource.create(level.getSeed() ^ chunkPos.toLong()),
                        writable,
                        chunkPos);
            }
        }
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        Set<BlockPos> scannedBlockEntities = new HashSet<>();
        for (ChunkAccess chunk : chunks) {
            ProtoChunk proto = (ProtoChunk) chunk;
            proto.getBlockEntities()
                    .forEach(
                            (position, blockEntity) -> {
                                if (!bounds.isInside(position)) return;
                                scannedBlockEntities.add(position.immutable());
                                StructureLootAnalyzer.containerLootTable(
                                                blockEntity.saveWithoutMetadata())
                                        .ifPresent(table -> result.merge(table, 1, Integer::sum));
                            });
            proto.getBlockEntityNbts()
                    .forEach(
                            (position, data) -> {
                                if (!bounds.isInside(position)
                                        || scannedBlockEntities.contains(position)) return;
                                StructureLootAnalyzer.containerLootTable(data)
                                        .ifPresent(table -> result.merge(table, 1, Integer::sum));
                            });
            // Structure pieces add chest minecarts as entity NBT to ProtoChunk rather than as
            // block entities. Their serialized ContainerEntity data uses the same LootTable key.
            proto.getEntities()
                    .forEach(
                            entityData ->
                                    StructureLootAnalyzer.containerLootTable(entityData)
                                            .ifPresent(
                                                    table -> result.merge(table, 1, Integer::sum)));
        }
        return new Sample(true, result);
    }

    record Sample(boolean generated, Map<ResourceLocation, Integer> lootTables) {
        private static Sample invalid() {
            return new Sample(false, Map.of());
        }
    }
}
