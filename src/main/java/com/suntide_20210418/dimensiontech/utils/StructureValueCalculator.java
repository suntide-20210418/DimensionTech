package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.config.ModConfigs.ItemExpectationMethod;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.DistributionalLootTableExecutor1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackMeasure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Coordinates exact loot analysis and applies runtime rarity/dimension valuation. */
public final class StructureValueCalculator {
    private StructureValueCalculator() {}

    public static StructureValue calculate(ServerLevel level, MarkerInfo markerInfo) {
        return calculate(level, markerInfo, 0.0F);
    }

    public static StructureValue calculate(ServerLevel level, MarkerInfo markerInfo, float luck) {
        return calculate(level, markerInfo, luck, StructureLootAnalyzer.discoverForValue(level, markerInfo));
    }

    /** Calculates a value from a pre-discovered profile, used by detached catalogue analysis. */
    public static StructureValue calculate(
            ServerLevel level, MarkerInfo markerInfo, float luck, DiscoveryResult discovery) {
        double dimensionValue = ModConfigs.STRUCTURE_VALUE.dimensionValue(markerInfo.dimension());
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (!Float.isFinite(luck)) {
            diagnostics.add(
                    new Diagnostic("VALUE_SEMANTICS", "Machine luck must be finite: " + luck));
            return new StructureValue(
                    AnalysisStatus.UNSUPPORTED,
                    dimensionValue,
                    0.0D,
                    new StackMeasure(),
                    List.copyOf(diagnostics));
        }
        if (!Double.isFinite(dimensionValue) || dimensionValue < 0.0D) {
            diagnostics.add(
                    new Diagnostic(
                            "VALUE_SEMANTICS",
                            "Configured dimension value must be finite and non-negative: "
                                    + dimensionValue));
            return new StructureValue(
                    AnalysisStatus.UNSUPPORTED,
                    0.0D,
                    0.0D,
                    new StackMeasure(),
                    List.copyOf(diagnostics));
        }
        StackMeasure measure = new StackMeasure();
        TerminalStackMeasure terminalMeasure = TerminalStackMeasure.empty();
        boolean fullStackMeasureAvailable = true;
        diagnostics.addAll(discovery.diagnostics());
        AnalysisStatus status = discovery.status();
        Map<ResourceLocation, ExactProbability> roots = rootTableWeightsForValue(discovery);
        LootAnalysisContext context = LootAnalysisContext.at(level, markerInfo.position(), luck);
        if (ModConfigs.STRUCTURE_VALUE.itemExpectationMethod() == ItemExpectationMethod.SAMPLING) {
            return sampledValue(level, markerInfo, discovery, dimensionValue, luck, diagnostics);
        }
        for (Map.Entry<ResourceLocation, ExactProbability> root : roots.entrySet()) {
            var result =
                    DistributionalLootTableExecutor1201.evaluate(
                            level.getServer(), root.getKey(), context, 1_000_000);
            diagnostics.addAll(result.diagnostics());
            if (result.status() != AnalysisStatus.EXACT) {
                status = AnalysisStatus.UNSUPPORTED;
                break;
            }
            terminalMeasure = terminalMeasure.plus(result.terminalMeasure().scale(root.getValue()));
            if (fullStackMeasureAvailable && result.fullStackMeasureAvailable()) {
                measure.addAll(result.measure(), root.getValue());
            } else if (!result.fullStackMeasureAvailable()) {
                fullStackMeasureAvailable = false;
                measure = new StackMeasure();
            }
        }
        if (status != AnalysisStatus.EXACT && status != AnalysisStatus.APPROXIMATE) {
            return sampledValue(level, markerInfo, discovery, dimensionValue, luck, diagnostics);
        }
        double structureValue = 0.0D;
        if (status == AnalysisStatus.EXACT || status == AnalysisStatus.APPROXIMATE) {
            TerminalValueEvaluation valuation =
                    evaluateTerminalValue(
                            terminalMeasure,
                            key -> ModConfigs.STRUCTURE_VALUE.itemMultiplier(key),
                            dimensionValue);
            if (!valuation.supported()) {
                status = AnalysisStatus.UNSUPPORTED;
                diagnostics.add(valuation.diagnostic());
                return sampledValue(
                        level, markerInfo, discovery, dimensionValue, luck, diagnostics);
            } else {
                structureValue = finalStructureValue(valuation.value());
            }
        }
        return new StructureValue(
                status,
                dimensionValue,
                (status == AnalysisStatus.EXACT || status == AnalysisStatus.APPROXIMATE)
                        ? structureValue
                        : 0.0D,
                measure,
                terminalMeasure,
                fullStackMeasureAvailable,
                List.copyOf(diagnostics));
    }

    private static StructureValue sampledValue(
            ServerLevel level,
            MarkerInfo markerInfo,
            DiscoveryResult discovery,
            double dimensionValue,
            float luck,
            List<Diagnostic> diagnostics) {
        if (discovery.status() != AnalysisStatus.EXACT
                && discovery.status() != AnalysisStatus.APPROXIMATE) {
            return new StructureValue(
                    AnalysisStatus.UNSUPPORTED,
                    dimensionValue,
                    0.0D,
                    new StackMeasure(),
                    List.copyOf(diagnostics));
        }
        int samples = ModConfigs.STRUCTURE_VALUE.samplingCount();
        LinkedHashMap<Item, Long> counts = new LinkedHashMap<>();
        for (StructureLoot structure : discovery.structures()) {
            for (Map.Entry<ResourceLocation, Integer> table : structure.occurrences().entrySet()) {
                for (int occurrence = 0; occurrence < table.getValue(); occurrence++) {
                List<ItemStack> outputs =
                        LootTableLottery.draw(
                                level,
                                net.minecraft.world.phys.Vec3.atCenterOf(markerInfo.position()),
                                List.of(table.getKey()),
                                null,
                                luck,
                                samples);
                for (ItemStack stack : outputs) {
                    if (!stack.isEmpty() && stack.getCount() > 0) {
                        counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
                    }
                }
                }
            }
        }
        LinkedHashMap<TerminalStackKey, ExactProbability> masses = new LinkedHashMap<>();
        for (Map.Entry<Item, Long> entry : counts.entrySet()) {
            masses.put(
                    new TerminalStackKey(
                            entry.getKey(), 1, new ItemStack(entry.getKey()).getRarity()),
                    ExactProbability.of(entry.getValue(), samples));
        }
        TerminalStackMeasure sampledMeasure = TerminalStackMeasure.of(masses);
        double value =
                finalStructureValue(
                        evaluateTerminalValue(
                                        sampledMeasure,
                                        key -> ModConfigs.STRUCTURE_VALUE.itemMultiplier(key),
                                        dimensionValue)
                                .value());
        diagnostics.add(
                new Diagnostic(
                        "SAMPLING_APPROXIMATION",
                        "Per-item expectations estimated from "
                                + samples
                                + " Monte Carlo samples"));
        return new StructureValue(
                AnalysisStatus.APPROXIMATE,
                dimensionValue,
                value,
                new StackMeasure(),
                sampledMeasure,
                false,
                List.copyOf(diagnostics));
    }

    /** Applies the final value compression after all configured multipliers are combined. */
    private static double finalStructureValue(double rawValue) {
        return Math.sqrt(rawValue) * 50;
    }

    /**
     * Applies rarity and dimension multipliers exactly, converting to a double only at the NBT
     * value boundary. The package-visible result lets boundary tests distinguish a true exact zero
     * from an unsupported overflow without constructing a live server.
     */
    static TerminalValueEvaluation evaluateTerminalValue(
            TerminalStackMeasure terminalMeasure,
            ToDoubleFunction<TerminalStackKey> rarityMultiplier,
            double dimensionValue) {
        Objects.requireNonNull(terminalMeasure, "terminalMeasure");
        Objects.requireNonNull(rarityMultiplier, "rarityMultiplier");
        if (!Double.isFinite(dimensionValue) || dimensionValue < 0.0D) {
            return TerminalValueEvaluation.unsupported(
                    "Configured dimension value must be finite and non-negative: "
                            + dimensionValue);
        }
        try {
            /* Keep both configured multipliers and the terminal expectation rational until the
             * value is committed to the legacy double/NBT boundary. */
            ExactProbability exactLootValue =
                    terminalMeasure.exactItemWeightedValueFromDouble(rarityMultiplier);
            ExactProbability exactStructureValue =
                    exactLootValue.multiply(ExactProbability.fromDouble(dimensionValue));
            return TerminalValueEvaluation.exact(exactStructureValue.finiteDoubleValue());
        } catch (ArithmeticException | IllegalArgumentException | IllegalStateException exception) {
            return TerminalValueEvaluation.unsupported(
                    "Exact structure value cannot be represented as a finite"
                            + " non-negative double: "
                            + exception.getMessage());
        }
    }

    static Map<ResourceLocation, ExactProbability> rootTableWeightsForValue(
            DiscoveryResult discovery) {
        if (discovery.status() != AnalysisStatus.EXACT
                && discovery.status() != AnalysisStatus.APPROXIMATE) {
            return Map.of();
        }
        Map<ResourceLocation, ExactProbability> roots = new LinkedHashMap<>();
        for (StructureLoot structure : discovery.structures()) {
            structure.occurrences()
                    .forEach(
                            (table, occurrences) ->
                                    roots.merge(
                                            table,
                                            ExactProbability.of(occurrences, 1)
                                                    .multiply(discovery.occurrenceScale()),
                                            ExactProbability::add));
        }
        return Map.copyOf(roots);
    }

    /** Compatibility projection for existing integrations that need one entry per occurrence. */
    static List<ResourceLocation> rootTablesForValue(DiscoveryResult discovery) {
        if (discovery.status() != AnalysisStatus.EXACT
                && discovery.status() != AnalysisStatus.APPROXIMATE) {
            return List.of();
        }
        List<ResourceLocation> roots = new ArrayList<>();
        for (StructureLoot structure : discovery.structures()) {
            structure.occurrences().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .forEach(entry -> {
                        for (int count = 0; count < entry.getValue(); count++) roots.add(entry.getKey());
                    });
        }
        return List.copyOf(roots);
    }

    static double weightedLootValue(
            StackMeasure measure, ToDoubleFunction<ItemStack> rarityMultiplier) {
        Objects.requireNonNull(measure, "measure");
        Objects.requireNonNull(rarityMultiplier, "rarityMultiplier");
        ExactProbability exactValue = ExactProbability.ZERO;
        for (var entry : measure.values().entrySet()) {
            ItemStack stack = entry.getKey().stack();
            int count = stack.getCount();
            if (count < 0) {
                throw new IllegalStateException("stack count must be non-negative: " + count);
            }
            double multiplier = rarityMultiplier.applyAsDouble(stack);
            if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
                throw new IllegalArgumentException(
                        "rarity multiplier must be finite and non-negative: " + multiplier);
            }
            ExactProbability counted = entry.getValue().multiply(ExactProbability.of(count, 1L));
            exactValue = exactValue.add(counted.multiply(ExactProbability.fromDouble(multiplier)));
        }
        return exactValue.finiteDoubleValue();
    }

    record TerminalValueEvaluation(boolean supported, double value, Diagnostic diagnostic) {
        private static TerminalValueEvaluation exact(double value) {
            return new TerminalValueEvaluation(true, value, null);
        }

        private static TerminalValueEvaluation unsupported(String message) {
            return new TerminalValueEvaluation(
                    false, 0.0D, new Diagnostic("VALUE_SEMANTICS", message));
        }
    }

    public record StructureValue(
            AnalysisStatus status,
            double dimensionValue,
            double structureValue,
            StackMeasure measure,
            TerminalStackMeasure terminalMeasure,
            boolean fullStackMeasureAvailable,
            List<Diagnostic> diagnostics) {
        /** Compatibility overload for callers compiled against the pre-extraction API. */
        @Deprecated
        public StructureValue(
                AnalysisStatus status,
                double dimensionValue,
                double structureValue,
                float ignoredLuck,
                StackMeasure measure,
                List<Diagnostic> diagnostics) {
            this(status, dimensionValue, structureValue, measure, diagnostics);
        }

        /** Compatibility overload for callers compiled against the pre-extraction API. */
        @Deprecated
        public StructureValue(
                AnalysisStatus status,
                double dimensionValue,
                double structureValue,
                float ignoredLuck,
                StackMeasure measure,
                TerminalStackMeasure terminalMeasure,
                boolean fullStackMeasureAvailable,
                List<Diagnostic> diagnostics) {
            this(
                    status,
                    dimensionValue,
                    structureValue,
                    measure,
                    terminalMeasure,
                    fullStackMeasureAvailable,
                    diagnostics);
        }

        public StructureValue(
                AnalysisStatus status,
                double dimensionValue,
                double structureValue,
                StackMeasure measure,
                List<Diagnostic> diagnostics) {
            this(
                    status,
                    dimensionValue,
                    structureValue,
                    measure,
                    status == AnalysisStatus.EXACT
                            ? TerminalStackMeasure.from(measure)
                            : TerminalStackMeasure.empty(),
                    status == AnalysisStatus.EXACT,
                    diagnostics);
        }

        public StructureValue {
            status = Objects.requireNonNull(status, "status");
            measure = Objects.requireNonNull(measure, "measure");
            terminalMeasure = Objects.requireNonNull(terminalMeasure, "terminalMeasure");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
            if (!Double.isFinite(dimensionValue) || dimensionValue < 0.0D) {
                throw new IllegalArgumentException(
                        "dimension value must be finite and non-negative: " + dimensionValue);
            }
            if (status == AnalysisStatus.UNSUPPORTED || status == AnalysisStatus.LEGACY) {
                structureValue = 0.0D;
                measure = new StackMeasure();
                terminalMeasure = TerminalStackMeasure.empty();
                fullStackMeasureAvailable = false;
            } else {
                if (!Double.isFinite(structureValue) || structureValue < 0.0D) {
                    throw new IllegalArgumentException(
                            "exact structure value must be finite and non-negative: "
                                    + structureValue);
                }
                if (!fullStackMeasureAvailable) {
                    measure = new StackMeasure();
                }
            }
        }

        public double itemCount(net.minecraft.world.item.Item item) {
            return terminalMeasure.itemCountAsDouble(item);
        }

        public Map<net.minecraft.world.item.Item, ExactProbability> itemCounts() {
            return terminalMeasure.exactItemCounts();
        }
    }
}
