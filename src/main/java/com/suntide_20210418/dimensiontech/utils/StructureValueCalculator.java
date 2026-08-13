package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.LootAnalysisContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Coordinates exact loot analysis and applies runtime rarity/dimension valuation. */
public final class StructureValueCalculator {
    private StructureValueCalculator() {}

    public static StructureValue calculate(ServerLevel level, MarkerInfo markerInfo) {
        double dimensionValue = ModConfigs.STRUCTURE_VALUE.dimensionValue(markerInfo.dimension());
        List<Diagnostic> diagnostics = new ArrayList<>();
        StackMeasure measure = new StackMeasure();
        List<net.minecraft.resources.ResourceLocation> roots = new ArrayList<>();
        DiscoveryResult discovery =
                StructureLootAnalyzer.discoverForValue(level, markerInfo);
        diagnostics.addAll(discovery.diagnostics());
        for (StructureLoot structure : discovery.structures()) {
            // A loot table ID identifies a table definition, not a chest instance.
            // Preserve every occurrence so two chests using the same table count twice.
            roots.addAll(structure.lootTables());
        }
        double weightedLootValue = 0.0D;
        AnalysisStatus status = discovery.status();
        LootAnalysisContext context = LootAnalysisContext.at(
                level,
                markerInfo.position(),
                ModConfigs.STRUCTURE_VALUE.luck());
        for (net.minecraft.resources.ResourceLocation root : roots) {
            var result = com.suntide_20210418.dimensiontech.utils.loot.expectation.JsonLootTableExecutor
                    .evaluate(level.getServer(), root, context);
            measure.addAll(result.measure(), ExactProbability.ONE);
            diagnostics.addAll(result.diagnostics());
            if (result.status() != AnalysisStatus.EXACT) status = AnalysisStatus.UNSUPPORTED;
        }
        if (status == AnalysisStatus.EXACT) {
            for (var entry : measure.values().entrySet()) {
                ItemStack stack = entry.getKey().stack();
                weightedLootValue += entry.getValue().doubleValue() * stack.getCount()
                        * ModConfigs.STRUCTURE_VALUE.rarityMultiplier(stack.getRarity());
            }
        }
        return new StructureValue(status, dimensionValue,
                status == AnalysisStatus.EXACT ? weightedLootValue * dimensionValue : 0.0D,
                ModConfigs.STRUCTURE_VALUE.luck(), measure, List.copyOf(diagnostics));
    }

    public record StructureValue(AnalysisStatus status, double dimensionValue, double structureValue,
                                 float luck, StackMeasure measure, List<Diagnostic> diagnostics) {
        public double itemCount(net.minecraft.world.item.Item item) {
            return measure.values().entrySet().stream().filter(e -> e.getKey().stack().is(item))
                    .mapToDouble(e -> e.getValue().doubleValue() * e.getKey().count()).sum();
        }
    }
}
