package com.suntide_20210418.dimensiontech.block.entity;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentMarginal;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;
import com.suntide_20210418.dimensiontech.structure.analysis.AnalysisDataFingerprint;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureAnalysisService;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

/**
 * Owns the miner-specific Marker analysis lifecycle.
 *
 * <p>The submitted and completion-time fingerprints must match before an async result is accepted.
 * Consequently, a completed entry can only be reused while its analysis inputs remain equal. This
 * is deliberately not a general-purpose task cache.
 */
final class StructureMinerMarkerAnalysisCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long RETRY_DELAY_TICKS = 100L;
    private static final String ANALYSIS_TAG = "MarkerAnalysis";
    private static final String DATA_FINGERPRINT_TAG = "DataFingerprint";
    private static final String FINGERPRINT_TAG = "Fingerprint";
    private static final String ENTRIES_TAG = "Entries";

    private final ItemStackHandler inventory;
    private final Supplier<LootAnalysisFingerprint> currentFingerprint;
    private final BooleanSupplier removed;
    private final AnalysisStarter analysisStarter;
    private LootAnalysisFingerprint analyzedFingerprint;
    private List<CachedMarkerLoot> cachedLoot = List.of();

    /**
     * A persisted analysis read back from NBT, held until the first refresh can validate it against
     * the live fingerprint and data fingerprint. Only then is it promoted into {@link #cachedLoot},
     * so a stale save never becomes visible.
     */
    private LootAnalysisFingerprint restoredFingerprint;

    private String restoredDataFingerprint;
    private List<CachedMarkerLoot> restoredEntries = List.of();
    private final Map<Integer, CompletableFuture<StructureValueCalculator.StructureValue>> pending =
            new LinkedHashMap<>();
    private final Map<Integer, AnalysisLifecycle.TaskStatus> tasks = new LinkedHashMap<>();
    private long generation;
    private long retryAt;

    StructureMinerMarkerAnalysisCache(
            ItemStackHandler inventory,
            Supplier<LootAnalysisFingerprint> currentFingerprint,
            BooleanSupplier removed) {
        this(
                inventory,
                currentFingerprint,
                removed,
                StructureMinerMarkerAnalysisCache::startAnalysis);
    }

    StructureMinerMarkerAnalysisCache(
            ItemStackHandler inventory,
            Supplier<LootAnalysisFingerprint> currentFingerprint,
            BooleanSupplier removed,
            AnalysisStarter analysisStarter) {
        this.inventory = inventory;
        this.currentFingerprint = currentFingerprint;
        this.removed = removed;
        this.analysisStarter = analysisStarter;
    }

    /**
     * Refreshes only when analysis inputs changed or a failed analysis becomes eligible to retry.
     */
    List<Integer> refresh(MinecraftServer server, float luck, long gameTime) {
        LootAnalysisFingerprint fingerprint = currentFingerprint.get();
        // Promote a persisted analysis when it still matches. Report no invalidated slots, so the
        // caller does not reset the progress loaded from the same save.
        if (adoptRestored(server, fingerprint)) return List.of();
        boolean retry =
                gameTime >= retryAt && tasks.containsValue(AnalysisLifecycle.TaskStatus.FAILED);
        if (fingerprint.equals(analyzedFingerprint) && !retry) return List.of();

        long refreshGeneration = ++generation;
        List<Integer> invalidatedSlots = new ArrayList<>();
        List<CachedMarkerLoot> refreshed = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack marker = inventory.getStackInSlot(slot);
            if (!ModItems.isMarker(marker)) {
                invalidate(slot, invalidatedSlots);
                continue;
            }
            CachedMarkerLoot previous = cachedLootForSlot(slot);
            if (previous == null
                    || !ItemStack.isSameItemSameComponents(previous.marker(), marker)) {
                invalidate(slot, invalidatedSlots);
                previous = null;
            }
            var info = StructMarkerItem.getMarkerInfo(marker);
            if (info.isEmpty()) {
                invalidate(slot, invalidatedSlots);
                continue;
            }
            request(
                    server,
                    refreshGeneration,
                    fingerprint,
                    slot,
                    marker,
                    info.get(),
                    luck,
                    gameTime);
            if (previous != null) refreshed.add(previous);
        }
        cachedLoot = List.copyOf(refreshed);
        analyzedFingerprint = fingerprint;
        return List.copyOf(invalidatedSlots);
    }

    List<CachedMarkerLoot> entries() {
        return cachedLoot;
    }

    CachedMarkerLoot entryForSlot(int slot) {
        return cachedLootForSlot(slot);
    }

    String cacheStatus(int slot) {
        CachedMarkerLoot cached = cachedLootForSlot(slot);
        if (cached == null) return AnalysisLifecycle.CacheStatus.MISSING.name();
        return pending.containsKey(slot) || tasks.get(slot) == AnalysisLifecycle.TaskStatus.FAILED
                ? AnalysisLifecycle.CacheStatus.STALE.name()
                : AnalysisLifecycle.CacheStatus.COMPLETE.name();
    }

    AnalysisLifecycle.TaskStatus taskStatus(int slot) {
        return tasks.getOrDefault(slot, AnalysisLifecycle.TaskStatus.SUCCEEDED);
    }

    /** Cancels stale lifecycle state only when the cache-reuse invariant no longer holds. */
    void invalidateIfAnalysisInputsChanged() {
        if (analyzedFingerprint == null || analyzedFingerprint.equals(currentFingerprint.get()))
            return;
        generation++;
        pending.clear();
        tasks.clear();
        cachedLoot = List.of();
        analyzedFingerprint = null;
    }

    /**
     * Promotes a persisted analysis into the live cache when the marker/luck/config fingerprint and
     * the data-source fingerprint both still match.
     *
     * @return true when the restored entries became live, so the caller can skip re-analysis and
     *     the slot reset that follows an invalidation
     */
    private boolean adoptRestored(MinecraftServer server, LootAnalysisFingerprint fingerprint) {
        if (analyzedFingerprint != null || restoredFingerprint == null) return false;
        boolean reusable =
                restoredFingerprint.equals(fingerprint)
                        && restoredDataFingerprint != null
                        && restoredDataFingerprint.equals(AnalysisDataFingerprint.current(server));
        if (reusable) {
            cachedLoot = restoredEntries;
            analyzedFingerprint = fingerprint;
        }
        // Consume the restore state either way: a mismatch falls through to the ordinary recompute
        // path, which invalidates and resets the slots as a genuine input change should.
        restoredFingerprint = null;
        restoredDataFingerprint = null;
        restoredEntries = List.of();
        return reusable;
    }

    /**
     * Writes the effective analysis snapshot so a later load can resume without re-running it.
     *
     * <p>Only the derived quantities are stored: the expected-item weights are written as their
     * terminal doubles, because that is the only form any consumer reads them in, and exact
     * rational masses carry denominators far too large for NBT.
     */
    void saveAnalysis(
            CompoundTag parent, String dataFingerprint, HolderLookup.Provider registries) {
        // A loaded-but-not-yet-adopted snapshot is written back with the fingerprint it was saved
        // under, so a later load re-validates it instead of trusting the current server state.
        boolean useRestored = cachedLoot.isEmpty();
        LootAnalysisFingerprint fingerprint =
                useRestored ? restoredFingerprint : analyzedFingerprint;
        List<CachedMarkerLoot> source = useRestored ? restoredEntries : cachedLoot;
        String effectiveDataFingerprint = useRestored ? restoredDataFingerprint : dataFingerprint;
        if (fingerprint == null || source.isEmpty() || effectiveDataFingerprint == null) return;

        CompoundTag analysis = new CompoundTag();
        analysis.putString(DATA_FINGERPRINT_TAG, effectiveDataFingerprint);
        analysis.put(FINGERPRINT_TAG, writeFingerprint(fingerprint));
        ListTag entries = new ListTag();
        for (CachedMarkerLoot entry : source) {
            CompoundTag serialized = new CompoundTag();
            serialized.putInt("Slot", entry.slot());
            // The analyzed marker stack itself, so a load can enforce the same component equality
            // the live cache uses; the fingerprint alone misses NBT-carried marker semantics such
            // as a chest marker's loot table.
            serialized.put("Marker", entry.marker().saveOptional(registries));
            serialized.putDouble("StructureValue", entry.structureValue());
            serialized.putDouble("Quantity", entry.quantity());
            serialized.putDouble("DimensionValue", entry.dimensionValue());
            CompoundTag items = new CompoundTag();
            entry.expectedItems()
                    .forEach(
                            (item, probability) -> {
                                double weight = probability.doubleValue();
                                if (Double.isFinite(weight) && weight >= 0.0D) {
                                    items.putDouble(item.toString(), weight);
                                }
                            });
            serialized.put("Items", items);
            entries.add(serialized);
        }
        analysis.put(ENTRIES_TAG, entries);
        parent.put(ANALYSIS_TAG, analysis);
    }

    /**
     * Reads a persisted analysis into the restore buffer; validation happens on the first refresh.
     */
    void loadAnalysis(CompoundTag parent, HolderLookup.Provider registries) {
        restoredFingerprint = null;
        restoredDataFingerprint = null;
        restoredEntries = List.of();
        if (!parent.contains(ANALYSIS_TAG, Tag.TAG_COMPOUND)) return;
        CompoundTag analysis = parent.getCompound(ANALYSIS_TAG);
        LootAnalysisFingerprint fingerprint =
                readFingerprint(analysis.getCompound(FINGERPRINT_TAG));
        if (fingerprint == null) return;

        List<CachedMarkerLoot> restored = new ArrayList<>();
        for (Tag value : analysis.getList(ENTRIES_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag serialized = (CompoundTag) value;
            int slot = serialized.getInt("Slot");
            if (slot < 0 || slot >= inventory.getSlots()) continue;
            ItemStack analyzedMarker =
                    ItemStack.parseOptional(registries, serialized.getCompound("Marker"));
            ItemStack marker = inventory.getStackInSlot(slot);
            if (analyzedMarker.isEmpty()
                    || !ItemStack.isSameItemSameComponents(analyzedMarker, marker)) continue;
            var info = StructMarkerItem.getMarkerInfo(marker);
            if (info.isEmpty()) continue;
            Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
            CompoundTag items = serialized.getCompound("Items");
            for (String key : items.getAllKeys()) {
                ResourceLocation item = ResourceLocation.tryParse(key);
                double weight = items.getDouble(key);
                if (item == null || !Double.isFinite(weight) || weight < 0.0D) continue;
                expectedItems.put(item, ExactProbability.fromDouble(weight));
            }
            restored.add(
                    new CachedMarkerLoot(
                            slot,
                            marker.copy(),
                            info.get().dimension(),
                            info.get().position(),
                            serialized.getDouble("DimensionValue"),
                            serialized.getDouble("StructureValue"),
                            serialized.getDouble("Quantity"),
                            Map.copyOf(expectedItems),
                            EnchantmentMarginal.EMPTY));
        }
        if (restored.isEmpty()) return;
        restoredFingerprint = fingerprint;
        restoredDataFingerprint = analysis.getString(DATA_FINGERPRINT_TAG);
        restoredEntries = List.copyOf(restored);
    }

    private static CompoundTag writeFingerprint(LootAnalysisFingerprint fingerprint) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("AlgorithmVersion", fingerprint.algorithmVersion());
        tag.putInt("LuckBits", fingerprint.luckBits());
        tag.putString("AnalysisConfig", fingerprint.analysisConfig());
        ListTag slots = new ListTag();
        for (String slot : fingerprint.markerSlots()) slots.add(StringTag.valueOf(slot));
        tag.put("MarkerSlots", slots);
        return tag;
    }

    private static LootAnalysisFingerprint readFingerprint(CompoundTag tag) {
        if (tag.isEmpty()) return null;
        List<String> slots = new ArrayList<>();
        for (Tag value : tag.getList("MarkerSlots", Tag.TAG_STRING)) slots.add(value.getAsString());
        return new LootAnalysisFingerprint(
                tag.getInt("AlgorithmVersion"),
                slots,
                tag.getInt("LuckBits"),
                tag.getString("AnalysisConfig"));
    }

    private void invalidate(int slot, List<Integer> invalidatedSlots) {
        if (!invalidatedSlots.contains(slot)) invalidatedSlots.add(slot);
        pending.remove(slot);
        tasks.remove(slot);
    }

    private void request(
            MinecraftServer server,
            long requestGeneration,
            LootAnalysisFingerprint requestFingerprint,
            int slot,
            ItemStack marker,
            MarkerInfo info,
            float luck,
            long gameTime) {
        tasks.put(slot, AnalysisLifecycle.TaskStatus.QUEUED);
        ResourceKey<Level> dimensionKey =
                ResourceKey.create(Registries.DIMENSION, info.dimension());
        ServerLevel analysisLevel = server.getLevel(dimensionKey);
        if (analysisLevel == null) {
            tasks.put(slot, AnalysisLifecycle.TaskStatus.FAILED);
            retryAt = gameTime + RETRY_DELAY_TICKS;
            return;
        }
        ItemStack requestedMarker = marker.copy();
        CompletableFuture<StructureValueCalculator.StructureValue> future =
                analysisStarter.start(server, analysisLevel, info, luck, requestedMarker);
        pending.put(slot, future);
        tasks.put(slot, AnalysisLifecycle.TaskStatus.RUNNING);
        future.whenComplete(
                (analysis, error) ->
                        server.tell(
                                new TickTask(
                                        server.getTickCount(),
                                        () ->
                                                finish(
                                                        requestGeneration,
                                                        requestFingerprint,
                                                        slot,
                                                        requestedMarker,
                                                        info,
                                                        future,
                                                        analysis,
                                                        error,
                                                        gameTime))));
    }

    private void finish(
            long requestGeneration,
            LootAnalysisFingerprint requestFingerprint,
            int slot,
            ItemStack requestedMarker,
            MarkerInfo info,
            CompletableFuture<StructureValueCalculator.StructureValue> future,
            StructureValueCalculator.StructureValue analysis,
            Throwable error,
            long requestTime) {
        if (requestGeneration != generation
                || !requestFingerprint.equals(currentFingerprint.get())
                || removed.getAsBoolean()
                || !markerStillPresent(slot, requestedMarker)) {
            return;
        }
        pending.remove(slot, future);
        if (error != null
                || analysis == null
                || (analysis.status() != AnalysisStatus.EXACT
                        && analysis.status() != AnalysisStatus.APPROXIMATE)) {
            tasks.put(slot, AnalysisLifecycle.TaskStatus.FAILED);
            retryAt = requestTime + RETRY_DELAY_TICKS;
            // The retry loop re-enters here every RETRY_DELAY_TICKS while a marker stays
            // unsupported, so this is the one place a failed analysis can speak. Without it the
            // machine just sits at zero progress and the reason exists nowhere outside this frame.
            if (error != null) {
                LOGGER.warn(
                        "Marker analysis for slot {} ({}) failed: {}",
                        slot,
                        info.structure().id(),
                        error.toString());
            } else {
                LOGGER.warn(
                        "Marker analysis for slot {} ({}) produced status {} with diagnostics {}",
                        slot,
                        info.structure().id(),
                        analysis == null ? "null" : analysis.status(),
                        analysis == null ? List.of() : analysis.diagnostics());
            }
            return;
        }
        tasks.put(slot, AnalysisLifecycle.TaskStatus.SUCCEEDED);
        replace(slot, requestedMarker, info, analysis);
    }

    private boolean markerStillPresent(int slot, ItemStack marker) {
        return slot >= 0
                && slot < inventory.getSlots()
                && ItemStack.isSameItemSameComponents(inventory.getStackInSlot(slot), marker);
    }

    private CachedMarkerLoot cachedLootForSlot(int slot) {
        for (CachedMarkerLoot entry : cachedLoot) {
            if (entry.slot() == slot) return entry;
        }
        return null;
    }

    private static CompletableFuture<StructureValueCalculator.StructureValue> startAnalysis(
            MinecraftServer server,
            ServerLevel analysisLevel,
            MarkerInfo info,
            float luck,
            ItemStack marker) {
        String input = info.dimension() + "|" + info.position() + "|" + info.structure().id();
        String config = ModConfigs.STRUCTURE_VALUE.calculationFingerprint() + "|luck=" + luck;
        return StructureAnalysisService.forServer(server)
                .computeAsync(
                        "miner-structure-value",
                        input,
                        config,
                        LootAnalysisFingerprint.ALGORITHM_VERSION,
                        () -> {
                            // `discover` mutates the service's request bookkeeping and is guarded
                            // to the server thread, while computeAsync runs this supplier on the
                            // analysis worker. Hop first; the discovery work itself is already
                            // server-tick scheduled, and the compose stage below stays on the
                            // server thread for the loot-table freeze.
                            CompletableFuture<DiscoveryResult> discoveryStage =
                                    new CompletableFuture<>();
                            server.execute(
                                    () -> {
                                        try {
                                            startDiscovery(
                                                    server,
                                                    analysisLevel,
                                                    info,
                                                    marker,
                                                    discoveryStage);
                                        } catch (Throwable error) {
                                            discoveryStage.completeExceptionally(error);
                                        }
                                    });
                            return discoveryStage.thenComposeAsync(
                                    discovery -> {
                                        List<ResourceLocation> roots =
                                                discovery.structures().stream()
                                                        .flatMap(
                                                                value ->
                                                                        value.lootTables().stream())
                                                        .distinct()
                                                        .toList();
                                        RuntimeLootAstSource source =
                                                RuntimeLootAstSource.snapshotTables(server, roots);
                                        return StructureValueCalculator.calculateAsync(
                                                server, info, luck, discovery, source);
                                    },
                                    server);
                        });
    }

    /**
     * Starts the discovery stage of a marker analysis.
     *
     * <p>Chest markers carry a virtual structure id that has no templates and no registered
     * structure, so the structure-id discovery pipeline would fail them. Their profile is instead
     * built from the loot table recorded in the marker NBT, which is also why the discovery stage
     * needs the marker stack itself.
     *
     * <p>In-world markers carry the generated structure's real bounding box, so the loaded world is
     * the most authoritative loot source: scan the marker's containers before falling back to the
     * detached static discovery pipeline. Detached catalogue markers (degenerate all-zero box) and
     * filtered structures skip the world scan.
     */
    private static void startDiscovery(
            MinecraftServer server,
            ServerLevel analysisLevel,
            MarkerInfo info,
            ItemStack marker,
            CompletableFuture<DiscoveryResult> discoveryStage) {
        if (info.structure().id().equals(ChestMarkerItem.CHEST_MARKER_ID)) {
            DiscoveryResult discovery =
                    ChestMarkerItem.getChestInfo(marker)
                            .map(
                                    chest ->
                                            StructureLootAnalyzer.discoverFixedForValue(
                                                    analysisLevel,
                                                    info.structure().id(),
                                                    List.of(chest.lootTable()),
                                                    List.of()))
                            .orElseGet(
                                    () ->
                                            new DiscoveryResult(
                                                    AnalysisStatus.UNSUPPORTED,
                                                    List.of(),
                                                    List.of(
                                                            new Diagnostic(
                                                                    "CHEST_MARKER_INVALID",
                                                                    "Chest marker carries no"
                                                                            + " chest data"))));
            discoveryStage.complete(discovery);
            return;
        }
        if (!isDetachedCatalogueMarker(info)
                && ModConfigs.STRUCTURE_VALUE.allowsDimension(info.dimension())
                && ModConfigs.STRUCTURE_VALUE.allowsStructure(info.structure().id())) {
            DiscoveryResult worldResult =
                    StructureLootAnalyzer.discoverForValue(analysisLevel, info);
            if (worldResult.status() == AnalysisStatus.EXACT
                    || worldResult.status() == AnalysisStatus.APPROXIMATE) {
                discoveryStage.complete(worldResult);
                return;
            }
        }
        StructureAnalysisService.forServer(server)
                .discover(analysisLevel, info.structure().id())
                .whenComplete(
                        (result, error) -> {
                            if (error != null) discoveryStage.completeExceptionally(error);
                            else discoveryStage.complete(result);
                        });
    }

    /**
     * A detached catalogue marker carries a degenerate all-zero bounding box; real markers carry
     * the generated structure's bounds. The box is the only client-visible discriminator between
     * the two creation paths, so it decides whether the loaded world is scanned for loot.
     */
    private static boolean isDetachedCatalogueMarker(MarkerInfo info) {
        BoundingBox bounds = info.structure().bounds();
        return bounds.minX() == 0
                && bounds.minY() == 0
                && bounds.minZ() == 0
                && bounds.maxX() == 0
                && bounds.maxY() == 0
                && bounds.maxZ() == 0;
    }

    @FunctionalInterface
    interface AnalysisStarter {
        CompletableFuture<StructureValueCalculator.StructureValue> start(
                MinecraftServer server,
                ServerLevel level,
                MarkerInfo info,
                float luck,
                ItemStack marker);
    }

    private void replace(
            int slot,
            ItemStack marker,
            MarkerInfo info,
            StructureValueCalculator.StructureValue analysis) {
        Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
        analysis.itemCounts()
                .forEach(
                        (item, expected) -> {
                            ResourceLocation id =
                                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                            item);
                            if (id != null) expectedItems.put(id, expected);
                        });
        CachedMarkerLoot entry =
                new CachedMarkerLoot(
                        slot,
                        marker.copy(),
                        info.dimension(),
                        info.position(),
                        analysis.dimensionValue(),
                        analysis.structureValue(),
                        analysis.itemCounts().values().stream()
                                .mapToDouble(ExactProbability::finiteDoubleValue)
                                .filter(Double::isFinite)
                                .filter(count -> count > 0.0D)
                                .sum(),
                        expectedItems,
                        analysis.enchantmentMarginal());
        List<CachedMarkerLoot> updated = new ArrayList<>(cachedLoot);
        updated.removeIf(value -> value.slot() == slot);
        updated.add(entry);
        cachedLoot = List.copyOf(updated);
    }

    record CachedMarkerLoot(
            int slot,
            ItemStack marker,
            ResourceLocation dimension,
            BlockPos position,
            double dimensionValue,
            double structureValue,
            double quantity,
            Map<ResourceLocation, ExactProbability> expectedItems,
            EnchantmentMarginal enchantmentMarginal) {}
}
