package com.suntide_20210418.dimensiontech.api;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureAnalysisService;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Read-mostly facade for other mods to consume DimensionTech's structure/chest valuation without
 * depending on its internal analysis machinery.
 *
 * <p>This is deliberately thin: every method delegates straight to the mod's own implementation, so
 * an external consumer can never diverge from what DimensionTech itself reports (same config
 * filters, same fingerprints, same caches). Nothing here mutates world or config state except
 * {@link #markChest}, which reuses the chest marker's own marking path.
 *
 * <p>Callers must gate on {@code ModList.get().isLoaded("dimension_tech")} and keep the class load
 * off the failure path: if DimensionTech is absent, referencing this class throws {@code
 * NoClassDefFoundError}, so isolate calls behind a compat shim.
 */
public final class DimensionTechApi {

    /** The chest marker item's registry id ({@code dimension_tech:chest_marker}). */
    public static final ResourceLocation CHEST_MARKER_ITEM_ID = ChestMarkerItem.CHEST_MARKER_ID;

    /** Cache layer name for external structure valuations. */
    private static final String STRUCTURE_VALUE_LAYER = "skill-structure-value";

    private DimensionTechApi() {}

    /** Whether the stack is the chest marker item (identity only; marker data is not required). */
    public static boolean isChestMarker(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModItems.CHEST_MARKER.get();
    }

    /**
     * Values the container at {@code pos} by the loot table it carries, exactly as the chest marker
     * does when it analyses a chest.
     *
     * @return the valuation, or empty when the position holds no loot-carrying container
     */
    public static Optional<StructureValuation> valuateChest(ServerLevel level, BlockPos pos) {
        ResourceLocation lootTable = ChestMarkerItem.lootTableAt(level, pos);
        if (lootTable == null) {
            return Optional.empty();
        }
        MarkerInfo info = chestMarkerInfo(level, pos);
        DiscoveryResult discovery =
                StructureLootAnalyzer.discoverFixedForValue(
                        level, CHEST_MARKER_ITEM_ID, List.of(lootTable), List.of());
        StructureValue value = StructureValueCalculator.calculate(level, info, 0.0F, discovery);
        return Optional.of(toValuation(value));
    }

    /**
     * Values a generated structure asynchronously.
     *
     * <p>Discovery reads world data and therefore hops to the server thread; the exact expectation
     * then runs on the analysis worker pool and shares the {@link StructureAnalysisService} cache,
     * so repeated queries for the same structure/box are free. The returned future completes with
     * empty on failure rather than propagating an exception into the caller's tick loop.
     *
     * @param structureId the structure's registry id
     * @param bounds the generated structure's bounding box (used for both discovery and cache key)
     */
    public static CompletableFuture<Optional<StructureValuation>> valuateStructureAsync(
            MinecraftServer server,
            ServerLevel level,
            ResourceLocation structureId,
            BoundingBox bounds) {
        BlockPos anchor =
                new BlockPos(
                        (bounds.minX() + bounds.maxX()) / 2,
                        (bounds.minY() + bounds.maxY()) / 2,
                        (bounds.minZ() + bounds.maxZ()) / 2);
        MarkerInfo info =
                new MarkerInfo(
                        level.dimension().location(),
                        anchor,
                        new MarkedStructure(structureId, bounds));
        String input =
                level.dimension().location()
                        + "|"
                        + structureId
                        + "|"
                        + bounds.minX()
                        + ","
                        + bounds.minY()
                        + ","
                        + bounds.minZ()
                        + ","
                        + bounds.maxX()
                        + ","
                        + bounds.maxY()
                        + ","
                        + bounds.maxZ();
        String config = ModConfigs.STRUCTURE_VALUE.calculationFingerprint() + "|luck=0.0";
        return StructureAnalysisService.forServer(server)
                .computeAsync(
                        STRUCTURE_VALUE_LAYER,
                        input,
                        config,
                        LootAnalysisFingerprint.ALGORITHM_VERSION,
                        () -> {
                            // Discovery touches world chunks and service bookkeeping, so it must
                            // run on the server thread; the compose stage below stays there too for
                            // the loot-table freeze.
                            CompletableFuture<DiscoveryResult> discoveryStage =
                                    new CompletableFuture<>();
                            server.execute(
                                    () -> {
                                        try {
                                            discoveryStage.complete(
                                                    StructureLootAnalyzer.discoverForValue(
                                                            level, info));
                                        } catch (Throwable error) {
                                            discoveryStage.completeExceptionally(error);
                                        }
                                    });
                            return discoveryStage.thenComposeAsync(
                                    discovery -> {
                                        List<ResourceLocation> roots =
                                                discovery.structures().stream()
                                                        .flatMap(
                                                                entry ->
                                                                        entry.lootTables().stream())
                                                        .distinct()
                                                        .toList();
                                        RuntimeLootAstSource source =
                                                RuntimeLootAstSource.snapshotTables(server, roots);
                                        return StructureValueCalculator.calculateAsync(
                                                server, info, 0.0F, discovery, source);
                                    },
                                    server);
                        })
                .thenApply(value -> Optional.of(toValuation(value)))
                .exceptionally(error -> Optional.empty());
    }

    /**
     * Marks the chest at {@code pos} onto the held marker stack, reusing the chest marker's own
     * analysis + persistence path.
     *
     * @return true when the position held a loot-carrying container and the marker was written
     */
    public static boolean markChest(ServerLevel level, ItemStack markerStack, BlockPos pos) {
        ResourceLocation lootTable = ChestMarkerItem.lootTableAt(level, pos);
        if (lootTable == null) {
            return false;
        }
        return ChestMarkerItem.markChest(level, markerStack, pos, lootTable);
    }

    /** Localized structure name, with a readable identifier fallback. */
    public static Component structureName(ResourceLocation id) {
        return TranslateHelper.structureName(id);
    }

    /** Localized dimension name, with a readable identifier fallback. */
    public static Component dimensionName(ResourceLocation id) {
        return TranslateHelper.dimensionName(id);
    }

    private static MarkerInfo chestMarkerInfo(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        BoundingBox box =
                new BoundingBox(
                        immutable.getX(),
                        immutable.getY(),
                        immutable.getZ(),
                        immutable.getX(),
                        immutable.getY(),
                        immutable.getZ());
        return new MarkerInfo(
                level.dimension().location(),
                immutable,
                new MarkedStructure(CHEST_MARKER_ITEM_ID, box));
    }

    private static StructureValuation toValuation(StructureValue value) {
        List<StructureValuation.ItemExpectation> expectations = new ArrayList<>();
        if (value.status() == AnalysisStatus.EXACT
                || value.status() == AnalysisStatus.APPROXIMATE) {
            for (var entry : value.itemCounts().entrySet()) {
                Item item = entry.getKey();
                ExactProbability expected = entry.getValue();
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                if (itemId == null) {
                    continue;
                }
                double count = expected.doubleValue();
                if (!Double.isFinite(count) || count <= 0.0D) {
                    continue;
                }
                expectations.add(new StructureValuation.ItemExpectation(itemId, count));
            }
            expectations.sort(
                    Comparator.comparingDouble(StructureValuation.ItemExpectation::count)
                            .reversed());
        }
        return new StructureValuation(
                value.status(), value.dimensionValue(), value.structureValue(), expectations);
    }
}
