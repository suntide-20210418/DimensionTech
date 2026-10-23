package com.suntide_20210418.dimensiontech.structure.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Places one generated structure into disposable {@link ProtoChunk}s and reads container NBT.
 *
 * <p>A sample is a resumable {@link Session}: {@link Session#advance} places whole footprint chunks
 * until the caller's deadline passes and then returns, so a structure spanning many chunks is
 * spread over several server ticks instead of blocking one. {@link SamplingControl} additionally
 * bounds the total cost of a sample and lets a caller cancel it.
 *
 * <p><b>Why one shared region is enough here.</b> 1.21 derives the write radius from the {@code
 * ChunkStep} being generated, which is why the 1.21 sampler has to rebuild the region for every
 * chunk. In 1.20.1 the radius is the fourth constructor argument, and this class passes the whole
 * footprint radius, so every {@code placeInChunk} target is writable through one region. That is
 * also why {@code ensureCanWrite} never logs "Detected setBlock in a far chunk" here.
 */
final class VirtualStructureSampler {
    private VirtualStructureSampler() {}

    /** Block accesses the region may serve between two control checks. */
    private static final int OPS_PER_CHECK = 1024;

    /**
     * Runs one sample to completion. Kept for the synchronous callers (tests) that do not need
     * slicing; production code goes through {@link #begin} so the work can be spread over ticks.
     */
    static Sample sample(ServerLevel level, Structure structure, BlockPos origin) {
        Session session = begin(level, structure, origin, SamplingControl.unlimited());
        while (!session.advance(Long.MAX_VALUE)) {
            // Long.MAX_VALUE never trips the deadline; the loop only documents the resumable shape.
        }
        return session.result();
    }

    /**
     * Starts a sample for one placement candidate; an ungeneratable candidate yields an invalid
     * sample.
     */
    static Session begin(
            ServerLevel level, Structure structure, BlockPos origin, SamplingControl control) {
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
        if (!start.isValid()) {
            return new Session(level, generator, null, null, List.of(), null, control);
        }

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
        WorldGenRegion region =
                new CountingWorldGenRegion(level, chunks, ChunkStatus.FEATURES, radius, control);
        return new Session(level, generator, start, bounds, chunks, region, control);
    }

    /** One detached sample, advanced in caller-sized time slices. */
    static final class Session {
        private final ServerLevel level;
        private final ChunkGenerator generator;
        private final SamplingControl control;

        /** {@code false} when the candidate cannot generate at all; {@link #start} is then null. */
        private final boolean generatable;

        private final List<ChunkAccess> chunks;

        /**
         * Shared by every {@code placeInChunk} call; its write radius covers the whole footprint.
         */
        @Nullable private final WorldGenRegion region;

        @Nullable private final StructureStart start;
        @Nullable private final BoundingBox bounds;
        private int next;
        private Sample result = Sample.invalid();

        private Session(
                ServerLevel level,
                ChunkGenerator generator,
                @Nullable StructureStart start,
                @Nullable BoundingBox bounds,
                List<ChunkAccess> chunks,
                @Nullable WorldGenRegion region,
                SamplingControl control) {
            this.level = level;
            this.generator = generator;
            this.start = start;
            this.bounds = bounds;
            this.chunks = chunks;
            this.region = region;
            this.control = control;
            this.generatable = start != null && bounds != null && region != null;
        }

        /** Places chunks until the deadline passes; {@code true} once the sample is complete. */
        boolean advance(long deadlineNanos) {
            if (!generatable) return true;
            control.resume();
            try {
                while (next < chunks.size()) {
                    if (System.nanoTime() >= deadlineNanos) return false;
                    control.check();
                    place(chunks.get(next++));
                }
                result = scan();
                return true;
            } finally {
                control.pause();
            }
        }

        Sample result() {
            return result;
        }

        void interrupt() {
            control.interrupt();
        }

        private void place(ChunkAccess chunk) {
            ChunkPos chunkPos = chunk.getPos();
            BoundingBox writable =
                    new BoundingBox(
                            chunkPos.getMinBlockX(),
                            level.getMinBuildHeight() + 1,
                            chunkPos.getMinBlockZ(),
                            chunkPos.getMaxBlockX(),
                            level.getMaxBuildHeight() - 1,
                            chunkPos.getMaxBlockZ());
            if (!bounds.intersects(writable)) return;
            start.placeInChunk(
                    region,
                    level.structureManager(),
                    generator,
                    RandomSource.create(level.getSeed() ^ chunkPos.toLong()),
                    writable,
                    chunkPos);
        }

        private Sample scan() {
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
                                            .ifPresent(
                                                    table -> result.merge(table, 1, Integer::sum));
                                });
                proto.getBlockEntityNbts()
                        .forEach(
                                (position, data) -> {
                                    if (!bounds.isInside(position)
                                            || scannedBlockEntities.contains(position)) return;
                                    StructureLootAnalyzer.containerLootTable(data)
                                            .ifPresent(
                                                    table -> result.merge(table, 1, Integer::sum));
                                });
                // Structure pieces add chest minecarts as entity NBT to ProtoChunk rather than as
                // block entities. Their serialized ContainerEntity data uses the same LootTable
                // key.
                proto.getEntities()
                        .forEach(
                                entityData ->
                                        StructureLootAnalyzer.containerLootTable(entityData)
                                                .ifPresent(
                                                        table ->
                                                                result.merge(
                                                                        table, 1, Integer::sum)));
            }
            return new Sample(true, result);
        }
    }

    record Sample(boolean generated, Map<ResourceLocation, Integer> lootTables) {
        private static Sample invalid() {
            return new Sample(false, Map.of());
        }
    }

    /**
     * 一次采样的工作预算与中断标志。预算只在采样真正运行时累计（{@link #resume()}/{@link #pause()} 夹住每一片）， 所以跨 tick
     * 让出的等待时间不会把预算耗光。预算在热路径上被检查（每 {@link #OPS_PER_CHECK} 次方块访问一次）， 所以超预算的采样会立刻中止，而不是把服务端线程占满。
     */
    static final class SamplingControl {
        private final long budgetNanos;
        private long accumulatedNanos;
        private long resumedAtNanos;
        private boolean running;
        private volatile boolean interrupted;

        private SamplingControl(long budgetNanos) {
            this.budgetNanos = budgetNanos;
        }

        static SamplingControl ofMillis(int budgetMillis) {
            return new SamplingControl(Math.max(1L, budgetMillis) * 1_000_000L);
        }

        /** No budget: used by the synchronous entry point and by tests. */
        static SamplingControl unlimited() {
            return new SamplingControl(Long.MAX_VALUE);
        }

        void interrupt() {
            interrupted = true;
        }

        void resume() {
            running = true;
            resumedAtNanos = System.nanoTime();
        }

        void pause() {
            if (running) {
                accumulatedNanos += System.nanoTime() - resumedAtNanos;
                running = false;
            }
        }

        /** 由采样热路径调用；超预算或被取消时抛出。 */
        void check() {
            if (interrupted) {
                throw new SampleAbortedException(
                        "VIRTUAL_SAMPLE_CANCELLED", "Virtual structure sampling was cancelled");
            }
            long spentNanos = spentNanos();
            if (spentNanos > budgetNanos) {
                throw new SampleAbortedException(
                        "VIRTUAL_SAMPLE_BUDGET",
                        "Virtual structure sampling exceeded its budget ("
                                + spentNanos / 1_000_000L
                                + "ms > "
                                + budgetNanos / 1_000_000L
                                + "ms); raise virtualStructureSampleBudgetMillis to analyse this"
                                + " structure");
            }
        }

        private long spentNanos() {
            if (!running) return accumulatedNanos;
            return accumulatedNanos + System.nanoTime() - resumedAtNanos;
        }
    }

    /** 采样中止信号：预算耗尽或调用方取消。{@code code} 会成为诊断键。 */
    static final class SampleAbortedException extends RuntimeException {
        private final String code;

        SampleAbortedException(String code, String message) {
            super(message);
            this.code = code;
        }

        String code() {
            return code;
        }
    }

    /**
     * 计数方块访问并在检查点执行 {@link SamplingControl#check()}。piece 直接读 chunk 的路径不计入， 但那条路径很便宜，而且 {@link
     * Session#advance} 在每个 chunk 之间还会再查一次。
     */
    private static final class CountingWorldGenRegion extends WorldGenRegion {
        private final SamplingControl control;
        private int ops;

        private CountingWorldGenRegion(
                ServerLevel level,
                List<ChunkAccess> cache,
                ChunkStatus status,
                int writeRadiusCutoff,
                SamplingControl control) {
            super(level, cache, status, writeRadiusCutoff);
            this.control = control;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            count();
            return super.getBlockState(pos);
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
            count();
            return super.setBlock(pos, state, flags, recursionLeft);
        }

        private void count() {
            if (++ops >= OPS_PER_CHECK) {
                ops = 0;
                control.check();
            }
        }
    }
}
