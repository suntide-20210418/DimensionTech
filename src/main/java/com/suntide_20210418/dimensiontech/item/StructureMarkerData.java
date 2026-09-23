package com.suntide_20210418.dimensiontech.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.IdealRandomProbabilitySpace1201;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The analysis payload carried by a structure marker.
 *
 * <p>This replaces the {@code StructureMarkerData} compound tag that 1.20.1 stored on the item
 * stack. ItemStack NBT is gone in 1.21, so the payload lives in a {@link
 * com.suntide_20210418.dimensiontech.item.ModDataComponents} component instead.
 *
 * <p>Field names deliberately match the old tag keys ({@code Dimension}, {@code Position}, {@code
 * Structure}, {@code StructureValueAlgorithmVersion}, {@code RandomProbabilitySpace}, {@code
 * DimensionValue}, {@code StructureValue}, {@code AnalysisStatus}, {@code AnalysisDiagnostics},
 * {@code ExpectedItemCounts}, {@code AnalysisFingerprint}, {@code DimensionTechCatalogueEntry}) so
 * the persisted form stays readable and stays comparable with the 1.20.1 layout.
 *
 * <p>{@link #structureValue()} and {@link #expectedItemCounts()} are only meaningful for an exact
 * or approximate analysis; the old code omitted the keys entirely for other statuses, which is why
 * the former is an {@link Optional}. An absent {@code ExpectedItemCounts} and an empty list mean
 * the same thing, so that one is a plain list.
 */
public record StructureMarkerData(
        ResourceLocation dimension,
        BlockPos position,
        MarkedStructure structure,
        Optional<ChestData> chestData,
        int algorithmVersion,
        String randomProbabilitySpace,
        double dimensionValue,
        Optional<Double> structureValue,
        AnalysisStatus analysisStatus,
        List<Diagnostic> diagnostics,
        List<ItemExpectation> expectedItemCounts,
        String analysisFingerprint,
        boolean catalogueEntry) {

    /** Matches {@code StructMarkerItem.ALGORITHM_VERSION}; a mismatch marks a payload as legacy. */
    public static final int CURRENT_ALGORITHM_VERSION = StructMarkerItem.ALGORITHM_VERSION;

    public StructureMarkerData {
        diagnostics = List.copyOf(diagnostics);
        expectedItemCounts = List.copyOf(expectedItemCounts);
    }

    /**
     * Chest markers name the container's loot table directly instead of resolving a structure
     * template, so they need one extra field the structure marker never uses.
     */
    public record ChestData(ResourceLocation lootTable, long lootTableSeed) {
        public ChestData {
            java.util.Objects.requireNonNull(lootTable, "lootTable");
        }
    }

    /** One persisted per-item expectation: an exact rational mass keyed by item id. */
    public record ItemExpectation(
            ResourceLocation item, BigInteger numerator, BigInteger denominator) {
        public ItemExpectation {
            java.util.Objects.requireNonNull(item, "item");
            java.util.Objects.requireNonNull(numerator, "numerator");
            java.util.Objects.requireNonNull(denominator, "denominator");
        }

        public ExactProbability probability() {
            return ExactProbability.of(numerator, denominator);
        }
    }

    /**
     * Re-derives the trustworthy status of this payload.
     *
     * <p>This mirrors the 1.20.1 validation that used to run against the raw tag: an algorithm
     * version mismatch makes the payload legacy, and any internally inconsistent value makes it
     * unsupported. Values are validated rather than trusted, so a payload that claims {@code EXACT}
     * without a usable structure value is rejected.
     *
     * <p>One deliberate difference: the old code treated a <em>missing</em> diagnostics list as
     * unsupported. A typed payload always has a list, so presence is no longer distinguishable from
     * emptiness; the algorithm-version gate covers the case that check was guarding (a payload
     * written by a different, older writer).
     */
    public AnalysisStatus status() {
        if (algorithmVersion != CURRENT_ALGORITHM_VERSION) {
            return AnalysisStatus.LEGACY;
        }
        AnalysisStatus declared = analysisStatus;
        if (declared == null
                || declared == AnalysisStatus.LEGACY
                || randomProbabilitySpace == null
                || !IdealRandomProbabilitySpace1201.ID.equals(randomProbabilitySpace)
                || !Double.isFinite(dimensionValue)
                || dimensionValue < 0.0D) {
            return AnalysisStatus.UNSUPPORTED;
        }
        if ((declared == AnalysisStatus.EXACT || declared == AnalysisStatus.APPROXIMATE)
                && (structureValue.isEmpty()
                        || !Double.isFinite(structureValue.get())
                        || structureValue.get() < 0.0D)) {
            return AnalysisStatus.UNSUPPORTED;
        }
        return declared;
    }

    /** The first {@code *_FILTERED} diagnostic message, which the tooltip surfaces in red. */
    public Optional<String> filteredDiagnostic() {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.code().endsWith("_FILTERED")) {
                return Optional.of(diagnostic.message());
            }
        }
        return Optional.empty();
    }

    public double structureValueOrZero() {
        return structureValue.filter(Double::isFinite).orElse(0.0D);
    }

    public Map<ResourceLocation, ExactProbability> expectedItemCountsById() {
        java.util.LinkedHashMap<ResourceLocation, ExactProbability> merged =
                new java.util.LinkedHashMap<>();
        for (ItemExpectation expectation : expectedItemCounts) {
            merged.merge(expectation.item(), expectation.probability(), ExactProbability::add);
        }
        return Map.copyOf(merged);
    }

    // --------------------------------------------------------------------- codecs

    private static final Codec<BlockPos> POSITION_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
                                            Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
                                            Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ))
                                    .apply(instance, BlockPos::new));

    private static final Codec<BoundingBox> BOUNDS_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.INT.fieldOf("MinX").forGetter(BoundingBox::minX),
                                            Codec.INT.fieldOf("MinY").forGetter(BoundingBox::minY),
                                            Codec.INT.fieldOf("MinZ").forGetter(BoundingBox::minZ),
                                            Codec.INT.fieldOf("MaxX").forGetter(BoundingBox::maxX),
                                            Codec.INT.fieldOf("MaxY").forGetter(BoundingBox::maxY),
                                            Codec.INT.fieldOf("MaxZ").forGetter(BoundingBox::maxZ))
                                    .apply(instance, BoundingBox::new));

    private static final Codec<MarkedStructure> STRUCTURE_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("Id")
                                                    .forGetter(MarkedStructure::id),
                                            BOUNDS_CODEC
                                                    .fieldOf("Bounds")
                                                    .forGetter(MarkedStructure::bounds))
                                    .apply(instance, MarkedStructure::new));

    private static final Codec<ChestData> CHEST_DATA_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("LootTable")
                                                    .forGetter(ChestData::lootTable),
                                            Codec.LONG
                                                    .fieldOf("LootTableSeed")
                                                    .forGetter(ChestData::lootTableSeed))
                                    .apply(instance, ChestData::new));

    private static final Codec<Diagnostic> DIAGNOSTIC_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.STRING
                                                    .fieldOf("Code")
                                                    .forGetter(Diagnostic::code),
                                            Codec.STRING
                                                    .fieldOf("Message")
                                                    .forGetter(Diagnostic::message),
                                            ResourceLocation.CODEC
                                                    .optionalFieldOf("LootTable")
                                                    .forGetter(
                                                            diagnostic ->
                                                                    Optional.ofNullable(
                                                                            diagnostic
                                                                                    .lootTableId())),
                                            Codec.STRING
                                                    .optionalFieldOf("JsonPointer", "")
                                                    .forGetter(Diagnostic::jsonPointer),
                                            Codec.STRING
                                                    .listOf()
                                                    .optionalFieldOf("CallPath", List.of())
                                                    .forGetter(Diagnostic::callPath))
                                    .apply(
                                            instance,
                                            (code, message, lootTable, pointer, callPath) ->
                                                    new Diagnostic(
                                                            code,
                                                            message,
                                                            lootTable.orElse(null),
                                                            pointer,
                                                            callPath)));

    private static final Codec<ItemExpectation> ITEM_EXPECTATION_CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("Item")
                                                    .forGetter(ItemExpectation::item),
                                            Codec.STRING
                                                    .fieldOf("Numerator")
                                                    .forGetter(
                                                            expectation ->
                                                                    expectation
                                                                            .numerator()
                                                                            .toString()),
                                            Codec.STRING
                                                    .fieldOf("Denominator")
                                                    .forGetter(
                                                            expectation ->
                                                                    expectation
                                                                            .denominator()
                                                                            .toString()))
                                    .apply(
                                            instance,
                                            (item, numerator, denominator) ->
                                                    new ItemExpectation(
                                                            item,
                                                            new BigInteger(numerator),
                                                            new BigInteger(denominator))));

    public static final Codec<StructureMarkerData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("Dimension")
                                                    .forGetter(StructureMarkerData::dimension),
                                            POSITION_CODEC
                                                    .fieldOf("Position")
                                                    .forGetter(StructureMarkerData::position),
                                            STRUCTURE_CODEC
                                                    .fieldOf("Structure")
                                                    .forGetter(StructureMarkerData::structure),
                                            CHEST_DATA_CODEC
                                                    .optionalFieldOf("ChestData")
                                                    .forGetter(StructureMarkerData::chestData),
                                            Codec.INT
                                                    .fieldOf("StructureValueAlgorithmVersion")
                                                    .forGetter(
                                                            StructureMarkerData::algorithmVersion),
                                            Codec.STRING
                                                    .fieldOf("RandomProbabilitySpace")
                                                    .forGetter(
                                                            StructureMarkerData
                                                                    ::randomProbabilitySpace),
                                            Codec.DOUBLE
                                                    .fieldOf("DimensionValue")
                                                    .forGetter(StructureMarkerData::dimensionValue),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("StructureValue")
                                                    .forGetter(StructureMarkerData::structureValue),
                                            AnalysisStatusCodec.ANALYSIS_STATUS_CODEC
                                                    .fieldOf("AnalysisStatus")
                                                    .forGetter(StructureMarkerData::analysisStatus),
                                            DIAGNOSTIC_CODEC
                                                    .listOf()
                                                    .optionalFieldOf(
                                                            "AnalysisDiagnostics", List.of())
                                                    .forGetter(StructureMarkerData::diagnostics),
                                            ITEM_EXPECTATION_CODEC
                                                    .listOf()
                                                    .optionalFieldOf(
                                                            "ExpectedItemCounts", List.of())
                                                    .forGetter(
                                                            StructureMarkerData
                                                                    ::expectedItemCounts),
                                            Codec.STRING
                                                    .fieldOf("AnalysisFingerprint")
                                                    .forGetter(
                                                            StructureMarkerData
                                                                    ::analysisFingerprint),
                                            Codec.BOOL
                                                    .optionalFieldOf(
                                                            "DimensionTechCatalogueEntry", false)
                                                    .forGetter(StructureMarkerData::catalogueEntry))
                                    .apply(instance, StructureMarkerData::new));

    /**
     * Marker payloads travel inside packets (a refreshed marker, an operator analysis result), so
     * the component needs a stream codec. Deriving it from {@link #CODEC} keeps the wire form and
     * the persisted form from drifting apart.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMarkerData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** {@link AnalysisStatus} is stored by name, matching the old string tag. */
    private static final class AnalysisStatusCodec {
        private static final Codec<AnalysisStatus> ANALYSIS_STATUS_CODEC =
                Codec.STRING.xmap(AnalysisStatus::valueOf, AnalysisStatus::name);

        private AnalysisStatusCodec() {}
    }
}
