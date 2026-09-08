package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.MarkerAnalysis;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Owns marker-analysis identity and the explicit asynchronous analysis refresh lifecycle. */
final class MinerAnalysisController {
    private final ItemStackHandler inventory;
    private final Supplier<Float> luck;
    private final MythicMinerMarkerAnalysisCache cache;
    private Map<Integer, MythicMinerUpgradeMath.ProcessingPlan> plans = Map.of();

    MinerAnalysisController(
            ItemStackHandler inventory, Supplier<Float> luck, BooleanSupplier removed) {
        this.inventory = inventory;
        this.luck = luck;
        this.cache = new MythicMinerMarkerAnalysisCache(inventory, this::fingerprint, removed);
    }

    List<Integer> refresh(MinecraftServer server, long gameTime) {
        return cache.refresh(server, luck.get(), gameTime);
    }

    void invalidateIfInputsChanged() {
        cache.invalidateIfAnalysisInputsChanged();
    }

    List<MarkerAnalysis> entries() {
        return cache.entries().stream().map(MinerAnalysisController::from).toList();
    }

    MarkerAnalysis entryForSlot(int slot) {
        var entry = cache.entryForSlot(slot);
        return entry == null ? null : from(entry);
    }

    String cacheStatus(int slot) {
        return cache.cacheStatus(slot);
    }

    AnalysisLifecycle.TaskStatus taskStatus(int slot) {
        return cache.taskStatus(slot);
    }

    void refreshPlans(double efficiency, int configuredProcessingTime, int minimumNaturalTicks,
            int baseParallel, boolean[] enabledSlots) {
        Map<Integer, MythicMinerUpgradeMath.ProcessingPlan> next = new HashMap<>();
        for (MarkerAnalysis loot : entries()) {
            int slot = loot.slot();
            if (slot >= 0 && slot < enabledSlots.length && enabledSlots[slot]) {
                next.put(slot, MythicMinerUpgradeMath.processingPlan(loot.structureValue(), efficiency,
                        configuredProcessingTime, minimumNaturalTicks, baseParallel));
            }
        }
        plans = Map.copyOf(next);
    }

    MythicMinerUpgradeMath.ProcessingPlan plan(int slot) {
        return plans.getOrDefault(slot, new MythicMinerUpgradeMath.ProcessingPlan(0, 0));
    }

    MythicMinerAnalysisSnapshot snapshot(int slot, ServerLevel level, double averageParallel,
            int drawsPerParallel, double quantityReference, boolean dismantling,
            java.util.Set<ResourceLocation> disabledItems) {
        MarkerAnalysis loot = entryForSlot(slot);
        if (loot == null || loot.quantity() <= 0.0D) return MythicMinerAnalysisSnapshot.EMPTY;
        int factor = MythicMinerExpectationMath.quantityFactorHundredths(loot.quantity(), quantityReference);
        double draws = MythicMinerExpectationMath.expectedDraws(averageParallel, drawsPerParallel, factor);
        Map<ResourceLocation, Double> expectations = new java.util.LinkedHashMap<>();
        loot.expectedItems().forEach((item, weight) -> {
            double value = MythicMinerExpectationMath.expectedItemCount(weight.finiteDoubleValue(), loot.quantity(), draws);
            if (Double.isFinite(value) && value > 0.0D) expectations.put(item, value);
        });
        Map<ResourceLocation, Double> displayed = dismantling
                ? EquipmentDismantler.dismantleExpectations(level, expectations) : expectations;
        return new MythicMinerAnalysisSnapshot(loot.dimensionValue(), loot.structureValue(), dismantling,
                displayed, disabledItems);
    }

    private LootAnalysisFingerprint fingerprint() {
        return LootAnalysisFingerprint.from(
                inventory, luck.get(), ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
    }

    private static MarkerAnalysis from(MythicMinerMarkerAnalysisCache.CachedMarkerLoot value) {
        return new MarkerAnalysis(value.slot(), value.marker(), value.dimension(), value.position(),
                value.dimensionValue(), value.structureValue(), value.quantity(), value.expectedItems());
    }
}
