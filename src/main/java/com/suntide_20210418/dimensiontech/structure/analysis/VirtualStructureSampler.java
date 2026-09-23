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
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
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
        Map<ChunkPos, VirtualChunkHolder> holders = new LinkedHashMap<>();
        ProtoChunk center = null;
        for (int z = startChunk.z - radius; z <= startChunk.z + radius; z++) {
            for (int x = startChunk.x - radius; x <= startChunk.x + radius; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
                // 1.21 把 ChunkStatus 的持久化状态改写入口改名为 setPersistedStatus(ChunkStatus)。
                chunk.setPersistedStatus(ChunkStatus.FEATURES);
                chunks.add(chunk);
                holders.put(pos, new VirtualChunkHolder(pos, chunk));
                if (pos.equals(startChunk)) center = chunk;
            }
        }
        if (center == null) return Sample.invalid();
        /*
         * 1.21 的 WorldGenRegion 不再是 (level, List<ChunkAccess>, status, radius)，而是
         * (level, StaticCache2D<GenerationChunkHolder>, ChunkStep, centerChunk)。ChunkStep 从
         * ChunkPyramid 取：FEATURES 的 directDependencies（STRUCTURE_STARTS@8 / CARVERS@1）与
         * blockStateWriteRadius=1 与 1.20.1 的 ChunkStatus 写入半径一致，所以采样语义不变。
         */
        ChunkStep featuresStep = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        StaticCache2D<GenerationChunkHolder> cache =
                StaticCache2D.create(
                        startChunk.x,
                        startChunk.z,
                        radius,
                        (x, z) -> holders.get(new ChunkPos(x, z)));
        WorldGenRegion region = new WorldGenRegion(level, cache, featuresStep, center);
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
                                                blockEntity.saveWithoutMetadata(
                                                        level.registryAccess()))
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

    /**
     * 1.21 的 {@link WorldGenRegion} 只能通过 {@link GenerationChunkHolder} 取块，而 holder 的块存放是私有的 futures
     * 数组（唯一写入入口 {@code completeFuture} 是私有的）。这里继承 holder 并覆写读取入口，让 region 看到我们预先放好的 ProtoChunk。
     *
     * <p>{@code getChunkIfPresentUnchecked} 是 public 且非 final，语义上等价于 1.20.1 {@code
     * WorldGenRegion#getChunk} 中"缓存块的 status 已满足要求就返回它"的那一支；我们预置的状态是 FEATURES，因此任何 FEATURES
     * 之前的状态请求都会命中同一份 ProtoChunk，与旧实现一致。
     */
    private static final class VirtualChunkHolder extends GenerationChunkHolder {
        private final ProtoChunk chunk;

        private VirtualChunkHolder(ChunkPos pos, ProtoChunk chunk) {
            super(pos);
            this.chunk = chunk;
        }

        @Override
        public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
            return chunk;
        }

        @Override
        public ChunkStatus getPersistedStatus() {
            return chunk.getPersistedStatus();
        }

        @Override
        public int getTicketLevel() {
            return 0;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }
}
