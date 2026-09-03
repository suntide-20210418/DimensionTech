package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The only catalogue-analysis entry point. Callers never receive a generation world or chunks;
 * they only observe a cached lifecycle and its resulting loot profile.
 *
 * <p>The queue deliberately runs on the server tick. This keeps eventual detached-world work on
 * the owning server thread and gives the UI a stable progress model.
 */
@Mod.EventBusSubscriber
public final class StructureAnalysisService {
    private static final Map<MinecraftServer, StructureAnalysisService> INSTANCES = new WeakHashMap<>();

    public static synchronized StructureAnalysisService forServer(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, ignored -> new StructureAnalysisService());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            forServer(event.getServer()).tick(event.getServer());
        }
    }

    /** Only non-vanilla structures may fall back to detached virtual generation. */
    static boolean mayUseVirtualAnalysis(ResourceLocation structure) {
        return !"minecraft".equals(structure.getNamespace());
    }

    /** Stable, seed-dependent origins reserved for a detached world implementation. */
    public static BlockPos sampleOrigin(
            long worldSeed, ResourceLocation dimension, ResourceLocation structure, int sampleIndex) {
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
                sampleOrigin(level.getSeed(), level.dimension().location(), structureId, sampleIndex);
        Holder<Structure> structure =
                level.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getHolder(ResourceKey.create(Registries.STRUCTURE, structureId))
                        .orElse(null);
        if (structure == null) return seedOrigin;
        var state = level.getChunkSource().getGeneratorState();
        List<StructurePlacement> placements = state.getPlacementsForStructure(structure);
        if (placements.isEmpty()) return seedOrigin;
        StructurePlacement placement = placements.get(Math.floorMod(sampleIndex, placements.size()));
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
                return positions.get(Math.floorMod(sampleIndex, positions.size())).getWorldPosition();
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
    private final Map<Key, DiscoveryResult> staticDiscoveries = new HashMap<>();
    private final Map<Key, Map<ResourceLocation, Integer>> observedTables = new HashMap<>();
    private final Map<Key, Integer> failedSamples = new HashMap<>();
    private final Map<Key, Integer> attemptedCandidates = new HashMap<>();
    private final ArrayDeque<Key> queue = new ArrayDeque<>();

    public synchronized State request(ServerLevel level, ResourceLocation structure, boolean refresh) {
        Key key = Key.from(level, structure);
        if (refresh) states.remove(key);
        if (refresh) staticDiscoveries.remove(key);
        if (refresh) observedTables.remove(key);
        if (refresh) failedSamples.remove(key);
        if (refresh) attemptedCandidates.remove(key);
        State existing = states.get(key);
        if (existing != null) return existing;
        DiscoveryResult staticResult =
                StructureLootAnalyzer.discoverTemplateForValue(
                        level, structure, AnalysisStatus.EXACT, List.of());
        // A successful template scan is the conventional, exact path. Only structures with no
        // statically discoverable root table need virtual generation; this keeps ordinary template
        // structures out of the expensive asynchronous sampler.
        if (staticResult.status() == AnalysisStatus.EXACT) {
            State completed =
                    new State(
                            key,
                            0,
                            ModConfigs.STRUCTURE_VALUE.virtualStructureSamples(),
                            staticResult,
                            staticResult.diagnostics(),
                            true);
            states.put(key, completed);
            return completed;
        }
        // Vanilla jigsaw pieces often assign fixed chest tables during placement, so the table
        // cannot be recovered from the template NBT. Use the authoritative vanilla locations
        // before paying the cost of detached generation. Mod namespaces deliberately skip this
        // resolver and continue to the virtual sampler.
        if (!mayUseVirtualAnalysis(structure)) {
            var fixedTables = VanillaStructureLootResolver.resolve(structure);
            if (fixedTables.isPresent()) {
                List<Diagnostic> fixedDiagnostics = new ArrayList<>(staticResult.diagnostics());
                fixedDiagnostics.add(
                        new Diagnostic(
                                "VANILLA_FIXED_LOOT_TABLE",
                                "Resolved vanilla structure loot from fixed container table locations."));
                DiscoveryResult fixedResult =
                        StructureLootAnalyzer.discoverFixedForValue(
                                level, structure, fixedTables.get(), fixedDiagnostics);
                if (fixedResult.status() == AnalysisStatus.EXACT) {
                    State completed =
                            new State(
                                    key,
                                    0,
                                    ModConfigs.STRUCTURE_VALUE.virtualStructureSamples(),
                                    fixedResult,
                                    fixedResult.diagnostics(),
                                    true);
                    states.put(key, completed);
                    return completed;
                }
            }
            List<Diagnostic> unavailableDiagnostics = new ArrayList<>(staticResult.diagnostics());
            unavailableDiagnostics.add(
                    new Diagnostic(
                            "VANILLA_FIXED_LOOT_TABLE_UNAVAILABLE",
                            "No fixed vanilla container loot-table location is registered for "
                                    + structure
                                    + "."));
            DiscoveryResult unavailable =
                    new DiscoveryResult(AnalysisStatus.UNSUPPORTED, List.of(), unavailableDiagnostics);
            State completed =
                    new State(
                            key,
                            0,
                            ModConfigs.STRUCTURE_VALUE.virtualStructureSamples(),
                            unavailable,
                            unavailable.diagnostics(),
                            true);
            states.put(key, completed);
            return completed;
        }
        staticDiscoveries.put(key, staticResult);
        State state =
                new State(
                        key,
                        0,
                        ModConfigs.STRUCTURE_VALUE.virtualStructureSamples(),
                        null,
                        List.of(),
                        false);
        states.put(key, state);
        queue.addLast(key);
        return state;
    }

    public synchronized State state(ServerLevel level, ResourceLocation structure) {
        return states.getOrDefault(Key.from(level, structure), State.missing());
    }

    public synchronized void tick(MinecraftServer server) {
        int budget = ModConfigs.STRUCTURE_VALUE.virtualStructureStepsPerTick();
        while (budget-- > 0 && !queue.isEmpty()) {
            Key key = queue.removeFirst();
            State state = states.get(key);
            ServerLevel level = server.getLevel(key.dimension());
            if (state == null || state.complete() || level == null) continue;
            int attempt = attemptedCandidates.merge(key, 1, Integer::sum) - 1;
            int next = state.completedSamples() + 1;
            // Origin derivation is intentionally separate from placement. A placement adapter must
            // write only to an in-memory WorldGenLevel; do not substitute ServerLevel here.
            BlockPos origin = sampleOrigin(level, key.structure(), attempt);
            Structure structure =
                    level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key.structure());
            if (structure != null) {
                try {
                    VirtualStructureSampler.Sample sample =
                            VirtualStructureSampler.sample(level, structure, origin);
                    if (!sample.generated()) {
                        // Placement gave us a candidate, but biome/terrain/structure-specific
                        // generation conditions rejected it. It is not a statistical sample.
                        if (attempt < state.totalSamples() * 32) {
                            queue.addLast(key);
                            continue;
                        }
                        failedSamples.merge(key, 1, Integer::sum);
                    }
                    sample.lootTables()
                            .forEach(
                                    (table, count) ->
                                            observedTables
                                                    .computeIfAbsent(key, ignored -> new HashMap<>())
                                                    .merge(table, count, Integer::sum));
                } catch (RuntimeException exception) {
                    // A modded structure may require generation services outside WorldGenLevel.
                    // Keep processing the other deterministic samples and report the final count.
                    failedSamples.merge(key, 1, Integer::sum);
                }
            }
            if (next < state.totalSamples()) {
                states.put(key, state.withProgress(next));
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
            if (failures > 0) {
                diagnostics.add(
                        new Diagnostic(
                                "VIRTUAL_SAMPLE_FAILURES",
                                failures + " virtual samples could not be generated or placed."));
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
                                "Virtual samples found no runtime container table; using the static template result."));
                result =
                        new DiscoveryResult(
                                AnalysisStatus.EXACT,
                                staticResult.structures(),
                                fallbackDiagnostics,
                                staticResult.occurrenceScale());
            }
            states.put(key, state.complete(next, result));
        }
    }

    public record State(
            Key key,
            int completedSamples,
            int totalSamples,
            DiscoveryResult result,
            List<Diagnostic> diagnostics,
            boolean complete) {
        public State {
            diagnostics = List.copyOf(diagnostics);
        }
        public static State missing() { return new State(null, 0, 0, null, List.of(), false); }
        State withProgress(int progress) { return new State(key, progress, totalSamples, null, diagnostics, false); }
        State complete(int progress, DiscoveryResult value) { return new State(key, progress, totalSamples, value, value.diagnostics(), true); }
    }

    public record Key(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, ResourceLocation structure, long seed, String fingerprint) {
        static Key from(ServerLevel level, ResourceLocation structure) {
            return new Key(level.dimension(), structure, level.getSeed(), ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
        }
    }
}
