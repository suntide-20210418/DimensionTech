package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.loot.expectation.ExpectationMath;
import com.suntide_20210418.dimensiontech.loot.expectation.MarkerAnalysis;
import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;
import com.suntide_20210418.dimensiontech.mythicminer.output.EquipmentDismantler;
import com.suntide_20210418.dimensiontech.mythicminer.processing.ProcessingMath;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.items.ItemStackHandler;

/** Owns marker-analysis identity and the explicit asynchronous analysis refresh lifecycle. */
final class MinerAnalysisController {
    private final ItemStackHandler inventory;
    private final Supplier<Float> luck;
    private final MythicMinerMarkerAnalysisCache cache;
    private Map<Integer, ProcessingMath.ProcessingPlan> plans = Map.of();

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

    void refreshPlans(
            double efficiency,
            int configuredProcessingTime,
            int minimumNaturalTicks,
            int baseParallel,
            boolean[] enabledSlots,
            MinerAccelerationController acceleration) {
        Map<Integer, ProcessingMath.ProcessingPlan> next = new HashMap<>();
        for (MarkerAnalysis loot : entries()) {
            int slot = loot.slot();
            if (slot >= 0 && slot < enabledSlots.length && enabledSlots[slot]) {
                next.put(
                        slot,
                        ProcessingMath.processingPlan(
                                loot.structureValue(),
                                efficiency,
                                configuredProcessingTime,
                                minimumNaturalTicks,
                                baseParallel));
            }
        }
        plans = Map.copyOf(next);
        acceleration.clearPlans();
        for (int slot = 0; slot < acceleration.slotCount(); slot++) {
            ProcessingMath.ProcessingPlan plan = plan(slot);
            acceleration.setPlan(slot, plan.processingTicks(), plan.parallelHundredths());
        }
    }

    ProcessingMath.ProcessingPlan plan(int slot) {
        return plans.getOrDefault(slot, new ProcessingMath.ProcessingPlan(0, 0));
    }

    MythicMinerAnalysisSnapshot snapshot(
            int slot,
            ServerLevel level,
            double averageParallel,
            int drawsPerParallel,
            double quantityReference,
            boolean dismantling,
            java.util.Set<ResourceLocation> disabledItems) {
        MarkerAnalysis loot = entryForSlot(slot);
        if (loot == null || loot.quantity() <= 0.0D) return MythicMinerAnalysisSnapshot.EMPTY;
        int factor = ExpectationMath.quantityFactorHundredths(loot.quantity(), quantityReference);
        double draws = ExpectationMath.expectedDraws(averageParallel, drawsPerParallel, factor);
        Map<ResourceLocation, Double> expectations = new java.util.LinkedHashMap<>();
        loot.expectedItems()
                .forEach(
                        (item, weight) -> {
                            double value =
                                    ExpectationMath.expectedItemCount(
                                            weight.finiteDoubleValue(), loot.quantity(), draws);
                            if (Double.isFinite(value) && value > 0.0D)
                                expectations.put(item, value);
                        });
        Map<ResourceLocation, Double> displayed =
                dismantling
                        ? EquipmentDismantler.dismantleExpectations(level, expectations)
                        : expectations;
        return new MythicMinerAnalysisSnapshot(
                loot.dimensionValue(),
                loot.structureValue(),
                dismantling,
                displayed,
                disabledItems);
    }

    private LootAnalysisFingerprint fingerprint() {
        return LootAnalysisFingerprint.from(
                inventory, luck.get(), ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
    }

    private static MarkerAnalysis from(MythicMinerMarkerAnalysisCache.CachedMarkerLoot value) {
        return new MarkerAnalysis(
                value.slot(),
                value.marker(),
                value.dimension(),
                value.position(),
                value.dimensionValue(),
                value.structureValue(),
                value.quantity(),
                value.expectedItems());
    }
}
