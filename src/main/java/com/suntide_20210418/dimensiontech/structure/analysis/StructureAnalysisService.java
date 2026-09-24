package com.suntide_20210418.dimensiontech.structure.analysis;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import com.suntide_20210418.dimensiontech.utils.AnalysisTaskCache;
import com.suntide_20210418.dimensiontech.utils.MainThreadTaskCache;
import com.suntide_20210418.dimensiontech.utils.VanillaStructureLootResolver;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The only catalogue-analysis entry point. Callers never receive a generation world or chunks; they
 * only observe a cached lifecycle and its resulting loot profile.
 *
 * <p>The queue deliberately runs on the server tick. This keeps eventual detached-world work on the
 * owning server thread and gives the UI a stable progress model.
 */
@EventBusSubscriber
public final class StructureAnalysisService {
    private static final Map<MinecraftServer, StructureAnalysisService> INSTANCES =
            new WeakHashMap<>();

    public static synchronized StructureAnalysisService forServer(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, ignored -> new StructureAnalysisService());
    }

    @SubscribeEvent
    public static synchronized void onServerStopping(ServerStoppingEvent event) {
        StructureAnalysisService service = INSTANCES.remove(event.getServer());
        if (service != null) service.close();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 1.21 把 TickEvent.ServerTickEvent 拆成了 ServerTickEvent.Pre/.Post，
        // 原来的 Phase.END 对应 Post（见 net.neoforged.neoforge.event.tick.ServerTickEvent）。
        forServer(event.getServer()).tick(event.getServer());
    }

    /** Only non-vanilla structures may fall back to detached virtual generation. */
    static boolean mayUseVirtualAnalysis(ResourceLocation structure) {
        return !"minecraft".equals(structure.getNamespace());
    }

    /** Stable, seed-dependent origins reserved for a detached world implementation. */
    public static BlockPos sampleOrigin(
            long worldSeed,
            ResourceLocation dimension,
            ResourceLocation structure,
            int sampleIndex) {
        long value = worldSeed;
        value = 31L * value + dimension.hashCode();
        value = 31L * value + structure.hashCode();
        value = 31L * value + sampleIndex;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        int x = (int) value & 0x1fffff;
        int z = (int) (value >>> 21) & 0x1fffff;
        return new BlockPos((x - 0x100000) << 4, 64, (z - 0x100000) << 4);
    }

    /** Chooses an actual placement candidate instead of an arbitrary chunk in the target world. */
    static BlockPos sampleOrigin(ServerLevel level, ResourceLocation structureId, int sampleIndex) {
        BlockPos seedOrigin =
                sampleOrigin(
                        level.getSeed(), level.dimension().location(), structureId, sampleIndex);
        Holder<Structure> structure =
                level.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getHolder(ResourceKey.create(Registries.STRUCTURE, structureId))
                        .orElse(null);
        if (structure == null) return seedOrigin;
        var state = level.getChunkSource().getGeneratorState();
        List<StructurePlacement> placements = state.getPlacementsForStructure(structure);
        if (placements.isEmpty()) return seedOrigin;
        StructurePlacement placement =
                placements.get(Math.floorMod(sampleIndex, placements.size()));
        if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
            int spacing = randomSpread.spacing();
            int regionX = Math.floorDiv(seedOrigin.getX() >> 4, spacing);
            int regionZ = Math.floorDiv(seedOrigin.getZ() >> 4, spacing);
            return randomSpread
                    .getPotentialStructureChunk(level.getSeed(), regionX, regionZ)
                    .getWorldPosition();
        }
        if (placement instanceof ConcentricRingsStructurePlacement rings) {
            List<net.minecraft.world.level.ChunkPos> positions = state.getRingPositionsFor(rings);
            if (positions != null && !positions.isEmpty()) {
                return positions
                        .get(Math.floorMod(sampleIndex, positions.size()))
                        .getWorldPosition();
            }
        }
        int baseX = seedOrigin.getX() >> 4;
        int baseZ = seedOrigin.getZ() >> 4;
        for (int radius = 0; radius <= 128; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) continue;
                    int chunkX = baseX + offsetX;
                    int chunkZ = baseZ + offsetZ;
                    if (placement.isStructureChunk(state, chunkX, chunkZ)) {
                        return new net.minecraft.world.level.ChunkPos(chunkX, chunkZ)
                                .getWorldPosition();
                    }
                }
            }
        }
        return seedOrigin;
    }

    private final Map<Key, State> states = new HashMap<>();
    private final Map<LogicalKey, State> latestStates = new HashMap<>();
    private final MainThreadTaskCache<StaticKey, DiscoveryResult> templateTasks =
            new MainThreadTaskCache<>(32);
    private final Map<Key, CompletableFuture<DiscoveryResult>> discoveryFutures = new HashMap<>();

    /** In-flight detached samples; each one spans as many ticks as its slice needs. */
    private final Map<Key, VirtualStructureSampler.Session> sessions = new HashMap<>();

    private final Map<Key, DiscoveryResult> staticDiscoveries = new HashMap<>();
    private final Map<Key, Map<ResourceLocation, Integer>> observedTables = new HashMap<>();
    private final Map<Key, Integer> failedSamples = new HashMap<>();
    private final Map<Key, Integer> attemptedCandidates = new HashMap<>();
    private final ArrayDeque<Key> queue = new ArrayDeque<>();
    private final AnalysisTaskCache tasks = new AnalysisTaskCache(1, 32);
    private final MainThreadTaskCache<AnalysisTaskCache.Key, Object> runtimeCaptures =
            new MainThreadTaskCache<>(32);
    private boolean closed;
    private int admissionTick = Integer.MIN_VALUE;
    private int admittedThisTick;
    private int executionLayer;

    private StructureAnalysisService() {}

    public CompletableFuture<StructureValueSnapshot.Result> value(
            StructureValueSnapshot.Expectation input, StructureValueSnapshot.Config config) {
        return tasks.submit(
                new AnalysisTaskCache.Key(
                        "structure-value",
                        input.inputFingerprint(),
                        config.configFingerprint(),
                        StructureValueSnapshot.ALGORITHM_VERSION),
                () -> StructureValueSnapshot.calculate(input, config));
    }

    public <T> CompletableFuture<T> computeSnapshot(
            String layer,
            String input,
            String config,
            int algorithmVersion,
            Supplier<T> computation) {
        return tasks.submit(
                new AnalysisTaskCache.Key(layer, input, config, algorithmVersion), computation);
    }

    /** Shares the complete asynchronous analysis pipeline, not only its inner snapshots. */
    public <T> CompletableFuture<T> computeAsync(
            String layer,
            String input,
            String config,
            int algorithmVersion,
            Supplier<CompletableFuture<T>> computation) {
        return tasks.submitAsync(
                new AnalysisTaskCache.Key(layer, input, config, algorithmVersion), computation);
    }

    /**
     * Captures runtime-only data with the same shared-key and per-tick budget rules as discovery.
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> capture(
            MinecraftServer server,
            String layer,
            String input,
            String config,
            int algorithmVersion,
            Supplier<T> capture) {
        requireServerThread(server);
        return (CompletableFuture<T>)
                (CompletableFuture<?>)
                        runtimeCaptures.submit(
                                new AnalysisTaskCache.Key(layer, input, config, algorithmVersion),
                                capture::get,
                                () -> admit(server));
    }

    private synchronized void close() {
        if (closed) return;
        closed = true;
        tasks.close();
        runtimeCaptures.close();
        templateTasks.close();
        for (VirtualStructureSampler.Session session : sessions.values()) {
            session.interrupt();
        }
        sessions.clear();
        for (CompletableFuture<DiscoveryResult> future :
                new ArrayList<>(discoveryFutures.values())) {
            future.completeExceptionally(
                    new java.util.concurrent.CancellationException("Server stopped"));
        }
        discoveryFutures.clear();
        queue.clear();
        states.clear();
        latestStates.clear();
        staticDiscoveries.clear();
        observedTables.clear();
        failedSamples.clear();
        attemptedCandidates.clear();
    }

    public synchronized State request(
            ServerLevel level, ResourceLocation structure, boolean refresh) {
        requireServerThread(level.getServer());
        if (closed) return State.missing();
        Key key = Key.from(level, structure);
        LogicalKey logicalKey = LogicalKey.from(level, structure);
        if (refresh) templateTasks.invalidate(StaticKey.from(level, structure));
        State exact = states.get(key);
        State existing = exact == null ? latestStates.get(logicalKey) : exact;
        if (discoveryFutures.containsKey(key)) return existing == null ? State.missing() : existing;
        if (!refresh && exact != null && exact.complete()) {
            latestStates.put(logicalKey, exact);
            return exact;
        }
        State waiting =
                new State(
                        key,
                        0,
                        ModConfigs.STRUCTURE_VALUE.virtualStructureSamples(),
                        existing == null ? null : existing.result(),
                        existing == null ? List.of() : existing.diagnostics(),
                        false,
                        AnalysisLifecycle.TaskStatus.QUEUED,
                        0L,
                        null);
        states.put(key, waiting);
        latestStates.put(logicalKey, waiting);
        CompletableFuture<DiscoveryResult> future = new CompletableFuture<>();
        discoveryFutures.put(key, future);
        future.whenComplete((result, error) -> discoveryFutures.remove(key, future));
        boolean dimensionAllowed =
                ModConfigs.STRUCTURE_VALUE.allowsDimension(level.dimension().location());
        boolean structureAllowed = ModConfigs.STRUCTURE_VALUE.allowsStructure(structure);
        templateTasks
                .submit(
                        StaticKey.from(level, structure),
                        () -> discoverStatic(level, structure, dimensionAllowed, structureAllowed),
                        () -> admit(level.getServer()))
                .whenComplete(
                        (result, error) -> {
                            if (closed) return;
                            if (error != null) {
                                failDiscovery(key, error);
                            } else if (result.status() == AnalysisStatus.EXACT
                                    || result.status() == AnalysisStatus.APPROXIMATE
                                    || !mayUseVirtualAnalysis(structure)
                                    || !dimensionAllowed
                                    || !structureAllowed) {
                                publishState(key, waiting.complete(0, result));
                                future.complete(result);
                            } else if (queue.size() >= 32) {
                                failDiscovery(
                                        key,
                                        new java.util.concurrent.RejectedExecutionException(
                                                "Virtual sampling queue is full"));
                            } else {
                                /*
                                 * A non-vanilla structure with no statically discoverable root
                                 * table is handed to the detached-generation sampler; its loot is
                                 * read from the containers the generated structure actually places.
                                 */
                                staticDiscoveries.put(key, result);
                                observedTables.remove(key);
                                failedSamples.remove(key);
                                attemptedCandidates.remove(key);
                                queue.addLast(key);
                            }
                        });
        return states.getOrDefault(key, State.missing());
    }

    /** Shared discovery/sampling completion, always resolved on the owning server thread. */
    public synchronized CompletableFuture<DiscoveryResult> discover(
            ServerLevel level, ResourceLocation structure) {
        State state = request(level, structure, false);
        CompletableFuture<DiscoveryResult> future =
                discoveryFutures.get(Key.from(level, structure));
        if (future != null) return future;
        if (state.complete() && state.result() != null)
            return CompletableFuture.completedFuture(state.result());
        return CompletableFuture.failedFuture(
                new java.util.concurrent.RejectedExecutionException("Discovery unavailable"));
    }

    private void failDiscovery(Key key, Throwable error) {
        // 采样被预算中止或被取消时用它们自己的诊断键，其余失败沿用 DISCOVERY_FAILED。
        String code = "DISCOVERY_FAILED";
        String detail = error.toString();
        if (error instanceof VirtualStructureSampler.SampleAbortedException aborted) {
            code = aborted.code();
            detail = aborted.getMessage();
        }
        State old = states.get(key);
        if (old != null)
            publishState(
                    key,
                    new State(
                            key,
                            old.completedSamples(),
                            old.totalSamples(),
                            old.result(),
                            List.of(new Diagnostic(code, detail)),
                            false,
                            AnalysisLifecycle.TaskStatus.FAILED,
                            System.currentTimeMillis(),
                            detail));
        CompletableFuture<DiscoveryResult> future = discoveryFutures.get(key);
        if (future != null) future.completeExceptionally(error);
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread())
            throw new IllegalStateException("Discovery must run on the server thread");
    }

    private boolean admit(MinecraftServer server) {
        int tick = server.getTickCount();
        if (admissionTick != tick) {
            admissionTick = tick;
            admittedThisTick = 0;
        }
        int budget = Math.max(1, ModConfigs.STRUCTURE_VALUE.virtualStructureStepsPerTick());
        if (admittedThisTick >= budget) return false;
        admittedThisTick++;
        return true;
    }

    private DiscoveryResult discoverStatic(
            ServerLevel level,
            ResourceLocation structure,
            boolean dimensionAllowed,
            boolean structureAllowed) {
        List<Diagnostic> filterDiagnostics = new ArrayList<>();
        if (!dimensionAllowed)
            filterDiagnostics.add(
                    new Diagnostic(
                            "DIMENSION_FILTERED",
                            "Dimension is blocked by the configured whitelist/blacklist."));
        if (!structureAllowed)
            filterDiagnostics.add(
                    new Diagnostic(
                            "STRUCTURE_FILTERED",
                            "Structure is blocked by the configured whitelist/blacklist."));
        if (!filterDiagnostics.isEmpty()) {
            return new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), filterDiagnostics);
        }
        DiscoveryResult staticResult =
                StructureLootAnalyzer.discoverTemplateForValue(
                        level, structure, AnalysisStatus.EXACT, List.of());
        // A successful template scan is the conventional, exact path. Only structures with no
        // statically discoverable root table need virtual generation; this keeps ordinary template
        // structures out of the expensive asynchronous sampler.
        if (staticResult.status() == AnalysisStatus.EXACT) {
            return staticResult;
        }
        // 25 of the 34 vanilla structures carry no start_pool, so the template graph does not
        // contain them at all and the placement code fixes their tables instead: template NBT for
        // ruined_portal, data markers for shipwreck/igloo/ocean_ruin/mansion/end_city, direct
        // createChest calls for the rest. Use that authoritative map before paying the cost of
        // detached generation. Mod namespaces deliberately skip this resolver and continue to the
        // virtual sampler.
        if (!mayUseVirtualAnalysis(structure)) {
            var resolution = VanillaStructureLootResolver.resolve(structure);
            // A structure that provably places no container has an exact answer of "nothing", not a
            // gap in the analysis: reporting UNSUPPORTED here would tell the player the machine
            // cannot read a structure that simply has nothing to read.
            if (resolution.isPresent() && !resolution.get().hasContainers()) {
                List<Diagnostic> emptyDiagnostics = new ArrayList<>(staticResult.diagnostics());
                emptyDiagnostics.add(
                        new Diagnostic(
                                "VANILLA_NO_CONTAINER",
                                "Structure places no loot container: "
                                        + resolution.get().evidence()));
                return new DiscoveryResult(
                        AnalysisStatus.EXACT,
                        List.of(
                                new StructureLootAnalyzer.StructureLoot(
                                        structure, List.of(), List.of(), List.of(), Map.of())),
                        emptyDiagnostics);
            }
            if (resolution.isPresent()) {
                List<Diagnostic> fixedDiagnostics = new ArrayList<>(staticResult.diagnostics());
                fixedDiagnostics.add(
                        new Diagnostic(
                                "VANILLA_FIXED_LOOT_TABLE",
                                "Resolved vanilla structure loot from fixed container table"
                                        + " locations: "
                                        + resolution.get().evidence()));
                DiscoveryResult fixedResult =
                        StructureLootAnalyzer.discoverFixedForValue(
                                level, structure, resolution.get().tables(), fixedDiagnostics);
                if (fixedResult.status() == AnalysisStatus.EXACT) {
                    return fixedResult;
                }
            }
            List<Diagnostic> unavailableDiagnostics = new ArrayList<>(staticResult.diagnostics());
            unavailableDiagnostics.add(
                    new Diagnostic(
                            "VANILLA_FIXED_LOOT_TABLE_UNAVAILABLE",
                            "No fixed vanilla container loot-table location is registered for "
                                    + structure
                                    + "."));
            return new DiscoveryResult(
                    AnalysisStatus.UNSUPPORTED, List.of(), unavailableDiagnostics);
        }
        return staticResult;
    }

    public synchronized State state(ServerLevel level, ResourceLocation structure) {
        Key key = Key.from(level, structure);
        State exact = states.get(key);
        if (exact != null) return exact;
        State latest = latestStates.get(LogicalKey.from(level, structure));
        return latest == null ? State.missing() : latest.staleFor(key);
    }

    private void publishState(Key key, State state) {
        states.put(key, state);
        LogicalKey logicalKey = LogicalKey.from(key);
        State latest = latestStates.get(logicalKey);
        if (latest == null || key.equals(latest.key())) latestStates.put(logicalKey, state);
    }

    public synchronized void tick(MinecraftServer server) {
        requireServerThread(server);
        if (closed) return;
        int budget = Math.max(1, ModConfigs.STRUCTURE_VALUE.virtualStructureStepsPerTick());
        ExecutionBudget allocated =
                allocateExecutionBudget(
                        budget,
                        executionLayer,
                        templateTasks.queuedCount(),
                        runtimeCaptures.queuedCount(),
                        queue.size());
        executionLayer = allocated.nextLayer();
        templateTasks.tick(allocated.templates());
        runtimeCaptures.tick(allocated.runtimeCaptures());
        int virtualBudget = allocated.virtualSamples();
        // 一个 tick 内所有 sample 共用同一条时间线：无论 stepsPerTick 调到多大，本 tick 在虚拟
        // 采样上花费的时间都不会超过这个时间片。
        long sliceDeadlineNanos =
                System.nanoTime() + ModConfigs.STRUCTURE_VALUE.virtualStructureSampleSliceNanos();
        while (virtualBudget-- > 0 && !queue.isEmpty()) {
            Key key = queue.removeFirst();
            State state = states.get(key);
            ServerLevel level = server.getLevel(key.dimension());
            if (state == null || state.complete()) {
                discardSample(key);
                continue;
            }
            if (level == null) {
                discardSample(key);
                failDiscovery(key, new IllegalStateException("Discovery dimension is unavailable"));
                continue;
            }
            try {
                int next = state.completedSamples() + 1;
                Structure structure =
                        level.registryAccess()
                                .registryOrThrow(Registries.STRUCTURE)
                                .get(key.structure());
                if (structure == null) {
                    // 与旧行为一致：注册表里没有这个结构时按“一次没有产出的采样”计数。
                    sessions.remove(key);
                } else {
                    try {
                        VirtualStructureSampler.Session session = sessions.get(key);
                        if (session == null) {
                            /*
                             * Origin derivation is intentionally separate from placement. A
                             * placement adapter must write only to an in-memory WorldGenLevel; do
                             * not substitute ServerLevel here.
                             */
                            int attempt = attemptedCandidates.merge(key, 1, Integer::sum) - 1;
                            BlockPos origin = sampleOrigin(level, key.structure(), attempt);
                            session =
                                    VirtualStructureSampler.begin(
                                            level,
                                            structure,
                                            origin,
                                            new VirtualStructureSampler.SamplingControl(
                                                    ModConfigs.STRUCTURE_VALUE
                                                            .virtualStructureSampleBudgetMillis()));
                            sessions.put(key, session);
                        }
                        if (!session.advance(sliceDeadlineNanos)) {
                            // 时间片用完：本 tick 不再推进它，下一个 tick 接着放剩下的 chunk。
                            queue.addLast(key);
                            continue;
                        }
                        sessions.remove(key);
                        VirtualStructureSampler.Sample sample = session.result();
                        if (!sample.generated()) {
                            // Placement gave us a candidate, but biome/terrain/structure-specific
                            // generation conditions rejected it. It is not a statistical sample.
                            // attemptedCandidates 在创建 session 时 +1，所以当前候选序号是 get()-1。
                            if (attemptedCandidates.getOrDefault(key, 1) - 1
                                    < state.totalSamples() * 32) {
                                queue.addLast(key);
                                continue;
                            }
                            failedSamples.merge(key, 1, Integer::sum);
                        }
                        sample.lootTables()
                                .forEach(
                                        (table, count) ->
                                                observedTables
                                                        .computeIfAbsent(
                                                                key, ignored -> new HashMap<>())
                                                        .merge(table, count, Integer::sum));
                    } catch (RuntimeException exception) {
                        if (exception
                                instanceof VirtualStructureSampler.SampleAbortedException aborted) {
                            // 预算耗尽或被取消：交给外层，整个结构判为不可分析，不再重试。
                            throw aborted;
                        }
                        // A modded structure may require generation services outside WorldGenLevel.
                        // Keep processing the other deterministic samples and report the final
                        // count.
                        sessions.remove(key);
                        failedSamples.merge(key, 1, Integer::sum);
                    }
                }
                if (next < state.totalSamples()) {
                    publishState(key, state.withProgress(next));
                    queue.addLast(key);
                    continue;
                }
                List<Diagnostic> diagnostics = new ArrayList<>();
                diagnostics.add(
                        new Diagnostic(
                                "VIRTUAL_SAMPLE_REQUIRED",
                                "Static template discovery found no root LootTable; "
                                        + state.totalSamples()
                                        + " virtual samples were requested."));
                int failures = failedSamples.getOrDefault(key, 0);
                int attempts = attemptedCandidates.getOrDefault(key, 0);
                int acceptedSamples = state.totalSamples() - failures;
                if (acceptedSamples <= 0) {
                    throw new IllegalStateException(
                            "No virtual structure sample could be generated");
                }
                if (failures > 0) {
                    diagnostics.add(
                            new Diagnostic(
                                    "VIRTUAL_SAMPLE_FAILURES",
                                    failures
                                            + " virtual samples could not be generated or"
                                            + " placed."));
                }
                if (attempts > state.totalSamples()) {
                    diagnostics.add(
                            new Diagnostic(
                                    "VIRTUAL_GENERATION_CONDITIONS",
                                    "Accepted "
                                            + acceptedSamples
                                            + " structure samples after "
                                            + attempts
                                            + " placement/biome/terrain candidates."));
                }
                Map<ResourceLocation, Integer> observed = observedTables.remove(key);
                failedSamples.remove(key);
                attemptedCandidates.remove(key);
                DiscoveryResult result =
                        StructureLootAnalyzer.discoverVirtualForValue(
                                level,
                                key.structure(),
                                observed == null ? Map.of() : observed,
                                Math.max(1, acceptedSamples),
                                diagnostics);
                DiscoveryResult staticResult = staticDiscoveries.remove(key);
                if (result.status() == AnalysisStatus.UNSUPPORTED
                        && staticResult != null
                        && staticResult.status() == AnalysisStatus.EXACT) {
                    List<Diagnostic> fallbackDiagnostics = new ArrayList<>(result.diagnostics());
                    fallbackDiagnostics.add(
                            new Diagnostic(
                                    "VIRTUAL_FALLBACK_TO_TEMPLATE",
                                    "Virtual samples found no runtime container table; using the"
                                            + " static template result."));
                    result =
                            new DiscoveryResult(
                                    AnalysisStatus.EXACT,
                                    staticResult.structures(),
                                    fallbackDiagnostics,
                                    staticResult.occurrenceScale());
                }
                publishState(key, state.complete(next, result));
                CompletableFuture<DiscoveryResult> future = discoveryFutures.get(key);
                if (future != null) future.complete(result);
            } catch (VirtualStructureSampler.SampleAbortedException aborted) {
                // 预算耗尽或被取消：整个结构直接判为不可分析，不再重试同一个 sample。
                discardSample(key);
                failDiscovery(key, aborted);
            } catch (RuntimeException error) {
                // A modded structure may require generation services outside WorldGenLevel. The
                // failed sample is dropped and the structure is reported with the error diagnostic.
                discardSample(key);
                failDiscovery(key, error);
            }
        }
    }

    /**
     * Stops detached sampling still running for one structure and fails its discovery future. The
     * caller is gone (for example the operator block entity was removed), so nothing should keep
     * burning server time on it.
     */
    public synchronized void cancel(
            ResourceKey<net.minecraft.world.level.Level> dimension, ResourceLocation structure) {
        for (Key key : List.copyOf(discoveryFutures.keySet())) {
            if (!key.dimension().equals(dimension) || !key.structure().equals(structure)) continue;
            discardSample(key);
            failDiscovery(
                    key,
                    new java.util.concurrent.CancellationException(
                            "Virtual structure sampling was cancelled"));
        }
    }

    /** Drops every bookkeeping entry belonging to an in-flight virtual sample. */
    private void discardSample(Key key) {
        VirtualStructureSampler.Session session = sessions.remove(key);
        if (session != null) session.interrupt();
        queue.remove(key);
        observedTables.remove(key);
        failedSamples.remove(key);
        attemptedCandidates.remove(key);
        staticDiscoveries.remove(key);
    }

    public static ExecutionBudget allocateExecutionBudget(
            int total, int firstLayer, int templates, int captures, int virtualSamples) {
        int[] available = {
            Math.max(0, templates), Math.max(0, captures), Math.max(0, virtualSamples)
        };
        int[] allocated = new int[available.length];
        int next = Math.floorMod(firstLayer, available.length);
        for (int unit = 0; unit < Math.max(0, total); unit++) {
            int selected = -1;
            for (int offset = 0; offset < available.length; offset++) {
                int candidate = (next + offset) % available.length;
                if (available[candidate] > 0) {
                    selected = candidate;
                    break;
                }
            }
            if (selected < 0) break;
            available[selected]--;
            allocated[selected]++;
            next = (selected + 1) % available.length;
        }
        return new ExecutionBudget(allocated[0], allocated[1], allocated[2], next);
    }

    public record ExecutionBudget(
            int templates, int runtimeCaptures, int virtualSamples, int nextLayer) {}

    public record State(
            Key key,
            int completedSamples,
            int totalSamples,
            DiscoveryResult result,
            List<Diagnostic> diagnostics,
            boolean complete,
            AnalysisLifecycle.TaskStatus taskStatus,
            long failedAtMillis,
            String failure) {
        public State {
            diagnostics = List.copyOf(diagnostics);
        }

        public static State missing() {
            return new State(null, 0, 0, null, List.of(), false, null, 0L, null);
        }

        public AnalysisLifecycle.CacheStatus cacheStatus() {
            if (result == null) return AnalysisLifecycle.CacheStatus.MISSING;
            return complete
                    ? AnalysisLifecycle.CacheStatus.COMPLETE
                    : AnalysisLifecycle.CacheStatus.STALE;
        }

        State withProgress(int progress) {
            return new State(
                    key,
                    progress,
                    totalSamples,
                    result,
                    diagnostics,
                    false,
                    AnalysisLifecycle.TaskStatus.RUNNING,
                    0L,
                    null);
        }

        State complete(int progress, DiscoveryResult value) {
            return new State(
                    key,
                    progress,
                    totalSamples,
                    value,
                    value.diagnostics(),
                    true,
                    AnalysisLifecycle.TaskStatus.SUCCEEDED,
                    0L,
                    null);
        }

        State staleFor(Key requestedKey) {
            if (result == null) return missing();
            return new State(
                    requestedKey,
                    completedSamples,
                    totalSamples,
                    result,
                    diagnostics,
                    false,
                    null,
                    0L,
                    null);
        }
    }

    public record Key(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            ResourceLocation structure,
            long seed,
            String fingerprint) {
        static Key from(ServerLevel level, ResourceLocation structure) {
            return new Key(
                    level.dimension(),
                    structure,
                    // Static vanilla discovery is seed-independent; modded structures may need
                    // seed-dependent virtual placement sampling.
                    mayUseVirtualAnalysis(structure) ? level.getSeed() : 0L,
                    ModConfigs.STRUCTURE_VALUE.generationFingerprint());
        }
    }

    record LogicalKey(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            ResourceLocation structure) {
        static LogicalKey from(ServerLevel level, ResourceLocation structure) {
            return new LogicalKey(level.dimension(), structure);
        }

        static LogicalKey from(Key key) {
            return new LogicalKey(key.dimension(), key.structure());
        }
    }

    record StaticKey(String dimension, String structure, String configFingerprint) {
        static StaticKey from(ServerLevel level, ResourceLocation structure) {
            return new StaticKey(
                    level.dimension().location().toString(),
                    structure.toString(),
                    ModConfigs.STRUCTURE_VALUE.discoveryFingerprint());
        }
    }
}
