package com.suntide_20210418.dimensiontech.structure.analysis;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
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

/**
 * Places one generated structure into disposable {@link ProtoChunk}s and reads container NBT.
 *
 * <p>A sample is a resumable {@link Session}: {@link Session#advance} places whole footprint chunks
 * until the caller's deadline passes and then returns, so a structure spanning many chunks is
 * spread over several server ticks instead of blocking one. {@link SamplingControl} additionally
 * bounds the total cost of a sample and lets a caller cancel it.
 */
final class VirtualStructureSampler {
    private VirtualStructureSampler() {}

    /** Block accesses the region may serve between two control checks. */
    private static final int OPS_PER_CHECK = 1024;

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
        Registry<net.minecraft.world.level.biome.Biome> biomes =
                level.registryAccess().registryOrThrow(Registries.BIOME);
        ChunkStep featuresStep = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        int readRadius = featuresStep.directDependencies().getRadius();
        if (!start.isValid()) {
            return new Session(
                    level,
                    null,
                    null,
                    generator,
                    featuresStep,
                    List.of(),
                    Map.of(),
                    biomes,
                    control,
                    readRadius);
        }

        BoundingBox bounds = start.getBoundingBox();
        int boundsRadius =
                Math.max(
                        Math.max(
                                Math.abs((bounds.minX() >> 4) - startChunk.x),
                                Math.abs((bounds.maxX() >> 4) - startChunk.x)),
                        Math.max(
                                Math.abs((bounds.minZ() >> 4) - startChunk.z),
                                Math.abs((bounds.maxZ() >> 4) - startChunk.z)));
        // Structure pieces may query their immediately adjacent chunk while placing.
        int footprint = boundsRadius + 1;
        Map<ChunkPos, VirtualChunkHolder> holders = new HashMap<>();
        List<ProtoChunk> chunks = new ArrayList<>((footprint * 2 + 1) * (footprint * 2 + 1));
        for (int z = startChunk.z - footprint; z <= startChunk.z + footprint; z++) {
            for (int x = startChunk.x - footprint; x <= startChunk.x + footprint; x++) {
                ChunkPos pos = new ChunkPos(x, z);
                ProtoChunk chunk = newChunk(level, pos, biomes);
                holders.put(pos, new VirtualChunkHolder(pos, chunk));
                chunks.add(chunk);
            }
        }
        return new Session(
                level,
                start,
                bounds,
                generator,
                featuresStep,
                chunks,
                holders,
                biomes,
                control,
                Math.max(footprint, readRadius));
    }

    private static ProtoChunk newChunk(
            ServerLevel level,
            ChunkPos pos,
            Registry<net.minecraft.world.level.biome.Biome> biomes) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
        // 1.21 把 ChunkStatus 的持久化状态改写入口改名为 setPersistedStatus(ChunkStatus)。
        chunk.setPersistedStatus(ChunkStatus.FEATURES);
        return chunk;
    }

    /** One detached sample, advanced in caller-sized time slices. */
    static final class Session {
        private final ServerLevel level;
        private final ChunkGenerator generator;
        private final ChunkStep featuresStep;
        private final Registry<net.minecraft.world.level.biome.Biome> biomes;
        private final SamplingControl control;
        private final List<ProtoChunk> chunks;
        private final Map<ChunkPos, VirtualChunkHolder> holders;
        private final int cacheRadius;

        /** {@code false} when the candidate cannot generate at all; {@link #start} is then null. */
        private final boolean generatable;

        @Nullable private final StructureStart start;
        @Nullable private final BoundingBox bounds;
        @Nullable private VirtualChunkHolder halo;
        private int next;
        private Sample result = Sample.invalid();

        private Session(
                ServerLevel level,
                @Nullable StructureStart start,
                @Nullable BoundingBox bounds,
                ChunkGenerator generator,
                ChunkStep featuresStep,
                List<ProtoChunk> chunks,
                Map<ChunkPos, VirtualChunkHolder> holders,
                Registry<net.minecraft.world.level.biome.Biome> biomes,
                SamplingControl control,
                int cacheRadius) {
            this.level = level;
            this.start = start;
            this.bounds = bounds;
            this.generator = generator;
            this.featuresStep = featuresStep;
            this.chunks = chunks;
            this.holders = holders;
            this.biomes = biomes;
            this.control = control;
            this.generatable = start != null && bounds != null;
            this.cacheRadius = cacheRadius;
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

        private void place(ProtoChunk chunk) {
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
            /*
             * 原版 FEATURES step 的写入半径只有 1 个 chunk，而 region 的中心就是“正在生成的那个
             * chunk”（ChunkStatusTasks.generateFeatures 用被生成的 chunk 构造 WorldGenRegion）。
             * 所以 region 必须逐个 chunk 重建：共用一个以起点 chunk 为中心的 region 时，footprint
             * 大于 3×3 的结构在中心 ±1 之外的每一次 setBlock 都会被 ensureCanWrite 拒绝，并逐条打
             * 出 "Detected setBlock in a far chunk" ERROR（AE2 陨石那次刷了 55 万行、卡住 17.9 秒）。
             */
            StaticCache2D<GenerationChunkHolder> cache =
                    StaticCache2D.create(
                            chunkPos.x,
                            chunkPos.z,
                            cacheRadius,
                            (x, z) -> holderAt(new ChunkPos(x, z)));
            WorldGenRegion region =
                    new CountingWorldGenRegion(level, cache, featuresStep, chunk, control);
            start.placeInChunk(
                    region,
                    level.structureManager(),
                    generator,
                    RandomSource.create(level.getSeed() ^ chunkPos.toLong()),
                    writable,
                    chunkPos);
        }

        /** 写入半径是 1，所以写入目标必然落在预建的 footprint 内；更远的坐标只会被读取，共用一个空 chunk 顶替即可——虚拟采样里任何坐标读到的都是空气。 */
        private VirtualChunkHolder holderAt(ChunkPos pos) {
            VirtualChunkHolder holder = holders.get(pos);
            if (holder != null) return holder;
            if (halo == null) halo = new VirtualChunkHolder(pos, newChunk(level, pos, biomes));
            return halo;
        }

        private Sample scan() {
            Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
            Set<BlockPos> scannedBlockEntities = new HashSet<>();
            for (ProtoChunk proto : chunks) {
                proto.getBlockEntities()
                        .forEach(
                                (position, blockEntity) -> {
                                    if (!bounds.isInside(position)) return;
                                    scannedBlockEntities.add(position.immutable());
                                    StructureLootAnalyzer.containerLootTable(
                                                    blockEntity.saveWithoutMetadata(
                                                            level.registryAccess()))
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
     * 一次采样的工作预算与中断标志。预算只在采样真正运行时累计（{@link #resume()}/{@link #pause()} 夹住每一片），所以跨 tick
     * 让出的等待时间不会把预算耗光。预算在热路径上被检查（每 {@link #OPS_PER_CHECK} 次方块访问一次），所以超预算的采样会立刻中止，而不是把服务端线程占满。
     */
    static final class SamplingControl {
        private final long budgetNanos;
        private long accumulatedNanos;
        private long resumedAtNanos;
        private boolean running;
        private volatile boolean interrupted;

        SamplingControl(long budgetMillis) {
            this.budgetNanos = Math.max(1L, budgetMillis) * 1_000_000L;
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
                StaticCache2D<GenerationChunkHolder> cache,
                ChunkStep step,
                ChunkAccess center,
                SamplingControl control) {
            super(level, cache, step, center);
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
