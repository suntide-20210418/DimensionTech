package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.config.ModConfigs.ItemExpectationMethod;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.*;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToDoubleFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Coordinates exact loot analysis and applies runtime rarity/dimension valuation. */
public final class StructureValueCalculator {
    private static final int EXPECTATION_ALGORITHM_VERSION = 1;

    private StructureValueCalculator() {}

    public static StructureValue calculate(ServerLevel level, MarkerInfo markerInfo) {
        return calculate(level, markerInfo, 0.0F);
    }

    public static StructureValue calculate(ServerLevel level, MarkerInfo markerInfo, float luck) {
        return calculate(
                level, markerInfo, luck, StructureLootAnalyzer.discoverForValue(level, markerInfo));
    }

    /** Calculates a value from a pre-discovered profile, used by detached catalogue analysis. */
    public static StructureValue calculate(
            ServerLevel level, MarkerInfo markerInfo, float luck, DiscoveryResult discovery) {
        return calculate(level, markerInfo, luck, discovery, null);
    }

    /**
     * Asynchronously evaluates a frozen loot source. The source must not reference a live server
     * resource manager; callers should create it with {@code RuntimeLootAstSource.snapshot}.
     */
    public static CompletableFuture<StructureValue> calculateAsync(
            MinecraftServer server,
            MarkerInfo markerInfo,
            float luck,
            DiscoveryResult discovery,
            RuntimeLootAstSource source) {
        if (!server.isSameThread())
            throw new IllegalStateException("Freeze analysis on the server thread");
        if (!Float.isFinite(luck)) {
            return CompletableFuture.completedFuture(
                    unsupported(
                            ModConfigs.STRUCTURE_VALUE.dimensionValue(markerInfo.dimension()),
                            List.of(
                                    new Diagnostic(
                                            "VALUE_SEMANTICS",
                                            "Machine luck must be finite: " + luck))));
        }
        com.google.gson.JsonObject input = new com.google.gson.JsonObject();
        input.addProperty("loot", source.inputFingerprint());
        input.addProperty("dimension", markerInfo.dimension().toString());
        input.addProperty("x", markerInfo.position().getX());
        input.addProperty("y", markerInfo.position().getY());
        input.addProperty("z", markerInfo.position().getZ());
        input.addProperty("status", discovery.status().name());
        com.google.gson.JsonObject roots = new com.google.gson.JsonObject();
        rootTableWeightsForValue(discovery)
                .forEach((id, weight) -> roots.addProperty(id.toString(), weight.toString()));
        input.add("roots", roots);
        String contentFingerprint = FrozenJson.freeze(input).fingerprint();
        StructureAnalysisService service = StructureAnalysisService.forServer(server);
        ItemExpectationMethod method = ModConfigs.STRUCTURE_VALUE.itemExpectationMethod();
        int samples = ModConfigs.STRUCTURE_VALUE.samplingCount();
        String expectationConfig =
                ModConfigs.STRUCTURE_VALUE.expectationFingerprint()
                        + "|luck="
                        + Float.floatToIntBits(luck);
        net.minecraft.core.BlockPos position = markerInfo.position().immutable();
        CompletableFuture<LootExpectationSnapshot> expectation;
        if (method == ItemExpectationMethod.SAMPLING) {
            expectation =
                    requestSamples(
                            server,
                            markerInfo,
                            luck,
                            discovery,
                            source,
                            contentFingerprint,
                            expectationConfig,
                            samples);
        } else {
            expectation =
                    service.capture(
                            server,
                            "loot-expectation",
                            contentFingerprint,
                            expectationConfig,
                            EXPECTATION_ALGORITHM_VERSION,
                            () -> exactExpectation(source, position, luck, discovery));
            expectation =
                    expectation.thenComposeAsync(
                            result -> {
                                if (result.status() == AnalysisStatus.EXACT
                                        || result.status() == AnalysisStatus.APPROXIMATE
                                        || (discovery.status() != AnalysisStatus.EXACT
                                                && discovery.status()
                                                        != AnalysisStatus.APPROXIMATE)) {
                                    return CompletableFuture.completedFuture(result);
                                }
                                return requestSamples(
                                        server,
                                        markerInfo,
                                        luck,
                                        discovery,
                                        source,
                                        contentFingerprint,
                                        expectationConfig,
                                        samples);
                            },
                            server);
        }
        return expectation.thenComposeAsync(
                result -> {
                    if (!ModConfigs.STRUCTURE_VALUE.allowsDimension(markerInfo.dimension())
                            || !ModConfigs.STRUCTURE_VALUE.allowsStructure(
                                    markerInfo.structure().id())) {
                        return CompletableFuture.completedFuture(
                                unsupported(
                                        ModConfigs.STRUCTURE_VALUE.dimensionValue(
                                                markerInfo.dimension()),
                                        List.of(
                                                new Diagnostic(
                                                        "STRUCTURE_FILTERED",
                                                        "Structure or dimension is blocked by"
                                                                + " configuration."))));
                    }
                    StructureValueSnapshot.Config valueConfig =
                            captureValueConfig(result.terminal(), markerInfo);
                    return service.value(result.terminal(), valueConfig)
                            .thenApplyAsync(value -> restoreValue(result, value), server);
                },
                server);
    }

    private static LootExpectationSnapshot exactExpectation(
            RuntimeLootAstSource source,
            net.minecraft.core.BlockPos position,
            float luck,
            DiscoveryResult discovery) {
        source.verifyRuntimeInputs();
        List<Diagnostic> diagnostics = new ArrayList<>(discovery.diagnostics());
        StackMeasure measure = new StackMeasure();
        TerminalStackMeasure terminal = TerminalStackMeasure.empty();
        boolean full = true;
        AnalysisStatus status = discovery.status();
        LootAnalysisContext context = LootAnalysisContext.snapshot(position, luck);
        for (var root : rootTableWeightsForValue(discovery).entrySet()) {
            var result =
                    DistributionalLootTableExecutor1201.evaluate(
                            source, root.getKey(), context, 1_000_000);
            diagnostics.addAll(result.diagnostics());
            if (result.status() != AnalysisStatus.EXACT) {
                return freezeExpectation(
                        AnalysisStatus.UNSUPPORTED,
                        new StackMeasure(),
                        TerminalStackMeasure.empty(),
                        false,
                        diagnostics);
            }
            terminal = terminal.plus(result.terminalMeasure().scale(root.getValue()));
            if (full && result.fullStackMeasureAvailable())
                measure.addAll(result.measure(), root.getValue());
            if (!result.fullStackMeasureAvailable()) full = false;
        }
        return freezeExpectation(
                status, full ? measure : new StackMeasure(), terminal, full, diagnostics);
    }

    private record SampleBatch(
            Map<StructureValueSnapshot.TerminalItem, Long> counts,
            int samples,
            ExactProbability occurrenceScale,
            List<Diagnostic> diagnostics) {
        private SampleBatch {
            counts = Map.copyOf(counts);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private static CompletableFuture<LootExpectationSnapshot> requestSamples(
            MinecraftServer server,
            MarkerInfo info,
            float luck,
            DiscoveryResult discovery,
            RuntimeLootAstSource source,
            String input,
            String config,
            int samples) {
        if (discovery.status() != AnalysisStatus.EXACT
                && discovery.status() != AnalysisStatus.APPROXIMATE) {
            return CompletableFuture.completedFuture(
                    new LootExpectationSnapshot(
                            discovery.status(),
                            new StructureValueSnapshot.Expectation(Map.of()),
                            Map.of(),
                            false,
                            discovery.diagnostics()));
        }
        StructureAnalysisService service = StructureAnalysisService.forServer(server);
        return service.capture(
                        server,
                        "loot-runtime-samples",
                        input,
                        config,
                        1,
                        () -> {
                            ServerLevel level =
                                    server.getLevel(
                                            net.minecraft.resources.ResourceKey.create(
                                                    net.minecraft.core.registries.Registries
                                                            .DIMENSION,
                                                    info.dimension()));
                            if (level == null)
                                throw new IllegalStateException(
                                        "Sampling dimension is unavailable");
                            if (samples <= 0)
                                throw new IllegalArgumentException("Sample count must be positive");
                            var roots = rootTableWeightsForValue(discovery).keySet();
                            String currentSource =
                                    RuntimeLootAstSource.snapshotTables(server, roots)
                                            .inputFingerprint();
                            if (!source.inputFingerprint().equals(currentSource))
                                throw new IllegalStateException(
                                        "Loot data changed before sampling");
                            Map<StructureValueSnapshot.TerminalItem, Long> counts =
                                    new LinkedHashMap<>();
                            for (StructureLoot structure : discovery.structures()) {
                                for (var table : structure.occurrences().entrySet()) {
                                    for (int occurrence = 0;
                                            occurrence < table.getValue();
                                            occurrence++) {
                                        for (ItemStack stack :
                                                LootTableLottery.draw(
                                                        level,
                                                        net.minecraft.world.phys.Vec3.atCenterOf(
                                                                info.position()),
                                                        List.of(table.getKey()),
                                                        null,
                                                        luck,
                                                        samples)) {
                                            if (stack.isEmpty() || stack.getCount() <= 0) continue;
                                            String id =
                                                    net.minecraft.core.registries.BuiltInRegistries
                                                            .ITEM
                                                            .getKey(stack.getItem())
                                                            .toString();
                                            var item =
                                                    new StructureValueSnapshot.TerminalItem(
                                                            id,
                                                            1,
                                                            new ItemStack(stack.getItem())
                                                                    .getRarity()
                                                                    .name());
                                            counts.merge(
                                                    item, (long) stack.getCount(), Math::addExact);
                                        }
                                    }
                                }
                            }
                            return new SampleBatch(
                                    counts,
                                    samples,
                                    discovery.occurrenceScale(),
                                    discovery.diagnostics());
                        })
                .thenCompose(
                        batch ->
                                service.computeSnapshot(
                                        "loot-sample-expectation",
                                        input,
                                        config,
                                        EXPECTATION_ALGORITHM_VERSION,
                                        () -> {
                                            Map<
                                                            StructureValueSnapshot.TerminalItem,
                                                            ExactProbability>
                                                    masses = new LinkedHashMap<>();
                                            batch.counts()
                                                    .forEach(
                                                            (item, count) ->
                                                                    masses.put(
                                                                            item,
                                                                            ExactProbability.of(
                                                                                            count,
                                                                                            batch
                                                                                                    .samples())
                                                                                    .multiply(
                                                                                            batch
                                                                                                    .occurrenceScale())));
                                            List<Diagnostic> diagnostics =
                                                    new ArrayList<>(batch.diagnostics());
                                            diagnostics.add(
                                                    new Diagnostic(
                                                            "SAMPLING_APPROXIMATION",
                                                            "Per-item expectations estimated from "
                                                                    + batch.samples()
                                                                    + " Monte Carlo samples"));
                                            return new LootExpectationSnapshot(
                                                    AnalysisStatus.APPROXIMATE,
                                                    new StructureValueSnapshot.Expectation(masses),
                                                    Map.of(),
                                                    false,
                                                    diagnostics);
                                        }));
    }

    private static LootExpectationSnapshot freezeExpectation(
            AnalysisStatus status,
            StackMeasure full,
            TerminalStackMeasure terminal,
            boolean fullAvailable,
            List<Diagnostic> diagnostics) {
        Map<StructureValueSnapshot.TerminalItem, ExactProbability> occurrences =
                new LinkedHashMap<>();
        terminal.values()
                .forEach(
                        (item, mass) ->
                                occurrences.put(
                                        new StructureValueSnapshot.TerminalItem(
                                                net.minecraft.core.registries.BuiltInRegistries.ITEM
                                                        .getKey(item.item())
                                                        .toString(),
                                                item.count(),
                                                item.rarity().name()),
                                        mass));
        Map<LootExpectationSnapshot.StackData, ExactProbability> stacks = new LinkedHashMap<>();
        full.values()
                .forEach(
                        (state, mass) -> {
                            ItemStack stack = state.stack();
                            stacks.put(
                                    new LootExpectationSnapshot.StackData(
                                            net.minecraft.core.registries.BuiltInRegistries.ITEM
                                                    .getKey(stack.getItem())
                                                    .toString(),
                                            state.count(),
                                            state.serializedStackData()),
                                    mass);
                        });
        return new LootExpectationSnapshot(
                status,
                new StructureValueSnapshot.Expectation(occurrences),
                stacks,
                fullAvailable,
                diagnostics);
    }

    private static StructureValueSnapshot.Config captureValueConfig(
            StructureValueSnapshot.Expectation expectation, MarkerInfo marker) {
        Map<String, Map<String, Double>> items = new LinkedHashMap<>();
        if (ModConfigs.STRUCTURE_VALUE.allowsDimension(marker.dimension())
                && ModConfigs.STRUCTURE_VALUE.allowsStructure(marker.structure().id())) {
            for (var terminal : expectation.occurrences().keySet()) {
                ResourceLocation id = ResourceLocation.parse(terminal.itemId());
                if (!ModConfigs.STRUCTURE_VALUE.allowsItem(id)) continue;
                Item item =
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getOptional(id)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Frozen item is no longer registered: "
                                                                + id));
                items.computeIfAbsent(terminal.itemId(), ignored -> new LinkedHashMap<>())
                        .put(
                                terminal.rarity(),
                                ModConfigs.STRUCTURE_VALUE.itemMultiplier(
                                        item,
                                        net.minecraft.world.item.Rarity.valueOf(
                                                terminal.rarity())));
            }
        }
        return new StructureValueSnapshot.Config(
                ModConfigs.STRUCTURE_VALUE.dimensionValue(marker.dimension()), items);
    }

    private static StructureValue restoreValue(
            LootExpectationSnapshot input, StructureValueSnapshot.Result value) {
        List<Diagnostic> diagnostics = new ArrayList<>(input.diagnostics());
        if (!value.supported()) diagnostics.add(new Diagnostic("VALUE_SEMANTICS", value.failure()));
        if (!value.supported()
                || (input.status() != AnalysisStatus.EXACT
                        && input.status() != AnalysisStatus.APPROXIMATE)) {
            return unsupported(value.dimensionValue(), diagnostics);
        }
        Map<TerminalStackKey, ExactProbability> terminal = new LinkedHashMap<>();
        value.expectation()
                .occurrences()
                .forEach(
                        (item, mass) ->
                                terminal.put(
                                        new TerminalStackKey(
                                                net.minecraft.core.registries.BuiltInRegistries.ITEM
                                                        .get(ResourceLocation.parse(item.itemId())),
                                                item.count(),
                                                net.minecraft.world.item.Rarity.valueOf(
                                                        item.rarity())),
                                        mass));
        StackMeasure full = new StackMeasure();
        java.util.Set<String> includedItems =
                value.expectation().occurrences().keySet().stream()
                        .map(StructureValueSnapshot.TerminalItem::itemId)
                        .collect(java.util.stream.Collectors.toSet());
        if (input.fullStackMeasureAvailable())
            input.stacks()
                    .forEach(
                            (data, mass) -> {
                                if (!includedItems.contains(data.itemId())) return;
                                try {
                                    ItemStack stack =
                                            ItemStack.of(
                                                    net.minecraft.nbt.TagParser.parseTag(
                                                            data.serializedNbt()));
                                    stack.setCount(data.count());
                                    full.add(new StackState(stack), mass);
                                } catch (
                                        com.mojang.brigadier.exceptions.CommandSyntaxException
                                                error) {
                                    throw new IllegalStateException(
                                            "Invalid frozen stack data", error);
                                }
                            });
        return new StructureValue(
                input.status(),
                value.dimensionValue(),
                value.structureValue(),
                full,
                TerminalStackMeasure.of(terminal),
                input.fullStackMeasureAvailable(),
                diagnostics);
    }

    private static StructureValue calculate(
            ServerLevel level,
            MarkerInfo markerInfo,
            float luck,
            DiscoveryResult discovery,
            RuntimeLootAstSource sourceOverride) {
        double dimensionValue = ModConfigs.STRUCTURE_VALUE.dimensionValue(markerInfo.dimension());
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (!ModConfigs.STRUCTURE_VALUE.allowsDimension(markerInfo.dimension())) {
            diagnostics.add(
                    new Diagnostic(
                            "DIMENSION_FILTERED",
                            "Dimension is blocked by the configured whitelist/blacklist."));
            return unsupported(dimensionValue, diagnostics);
        }
        if (!ModConfigs.STRUCTURE_VALUE.allowsStructure(markerInfo.structure().id())) {
            diagnostics.add(
                    new Diagnostic(
                            "STRUCTURE_FILTERED",
                            "Structure is blocked by the configured whitelist/blacklist."));
            return unsupported(dimensionValue, diagnostics);
        }
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
        LootAnalysisContext context =
                sourceOverride == null
                        ? LootAnalysisContext.at(level, markerInfo.position(), luck)
                        : LootAnalysisContext.snapshot(markerInfo.position(), luck);
        if (ModConfigs.STRUCTURE_VALUE.itemExpectationMethod() == ItemExpectationMethod.SAMPLING) {
            return sampledValue(level, markerInfo, discovery, dimensionValue, luck, diagnostics);
        }
        for (Map.Entry<ResourceLocation, ExactProbability> root : roots.entrySet()) {
            var result =
                    sourceOverride == null
                            ? DistributionalLootTableExecutor1201.evaluate(
                                    level.getServer(), root.getKey(), context, 1_000_000)
                            : DistributionalLootTableExecutor1201.evaluate(
                                    sourceOverride, root.getKey(), context, 1_000_000);
            diagnostics.addAll(result.diagnostics());
            if (result.status() != AnalysisStatus.EXACT) {
                status = AnalysisStatus.UNSUPPORTED;
                break;
            }
            terminalMeasure =
                    terminalMeasure.plus(
                            result.terminalMeasure()
                                    .filter(
                                            item -> {
                                                ResourceLocation id =
                                                        net.minecraft.core.registries
                                                                .BuiltInRegistries.ITEM
                                                                .getKey(item);
                                                return id == null
                                                        || ModConfigs.STRUCTURE_VALUE.allowsItem(
                                                                id);
                                            })
                                    .scale(root.getValue()));
            if (fullStackMeasureAvailable && result.fullStackMeasureAvailable()) {
                measure.addAll(
                        result.measure()
                                .filter(
                                        state -> {
                                            ResourceLocation id =
                                                    net.minecraft.core.registries.BuiltInRegistries
                                                            .ITEM
                                                            .getKey(state.stack().getItem());
                                            return id == null
                                                    || ModConfigs.STRUCTURE_VALUE.allowsItem(id);
                                        }),
                        root.getValue());
            } else if (!result.fullStackMeasureAvailable()) {
                fullStackMeasureAvailable = false;
                measure = new StackMeasure();
            }
        }
        if (status != AnalysisStatus.EXACT && status != AnalysisStatus.APPROXIMATE) {
            if (sourceOverride != null) {
                return new StructureValue(
                        AnalysisStatus.UNSUPPORTED,
                        dimensionValue,
                        0.0D,
                        new StackMeasure(),
                        List.copyOf(diagnostics));
            }
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
                            ResourceLocation itemId =
                                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                            stack.getItem());
                            if (itemId == null || ModConfigs.STRUCTURE_VALUE.allowsItem(itemId)) {
                                counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
                            }
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
            structure
                    .occurrences()
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

    private static StructureValue unsupported(double dimensionValue, List<Diagnostic> diagnostics) {
        return new StructureValue(
                AnalysisStatus.UNSUPPORTED,
                dimensionValue,
                0.0D,
                new StackMeasure(),
                List.copyOf(diagnostics));
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
                    .sorted(
                            Map.Entry.comparingByKey(
                                    Comparator.comparing(ResourceLocation::toString)))
                    .forEach(
                            entry -> {
                                for (int count = 0; count < entry.getValue(); count++)
                                    roots.add(entry.getKey());
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
