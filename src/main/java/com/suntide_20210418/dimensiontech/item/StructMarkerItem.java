package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.IdealRandomProbabilitySpace1211;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructMarkerItem extends Item {
    /**
     * Schema version of the persisted analysis payload. A payload carrying a different version is
     * treated as legacy and recomputed, which is how a semantics change invalidates old markers
     * without a migration step.
     */
    public static final int ALGORITHM_VERSION = 6;

    /**
     * Discovered-structure ids live on the player, not on the item stack, so this is unrelated to
     * the marker payload component.
     */
    private static final String DISCOVERED_STRUCTURES_TAG = "DimensionTechDiscoveredStructures";

    /**
     * Where the server enumerated the candidates for an overlapping-structure pick. Recorded and
     * consumed server side only; see {@link #rememberSelectionOrigin}.
     */
    private static final String SELECTION_ORIGIN_TAG = "DimensionTechStructureSelectionOrigin";

    public StructMarkerItem(Properties properties) {
        super(properties);
    }

    /** The marker payload, or empty when this stack carries no marker data. */
    public static Optional<StructureMarkerData> getMarkerData(ItemStack itemStack) {
        return Optional.ofNullable(itemStack.get(ModDataComponents.STRUCTURE_MARKER));
    }

    public static Optional<MarkerInfo> getMarkerInfo(ItemStack itemStack) {
        return getMarkerData(itemStack)
                .map(data -> new MarkerInfo(data.dimension(), data.position(), data.structure()));
    }

    /** Reads the per-item expectations persisted by the last successful analysis. */
    public static Map<ResourceLocation, ExactProbability> getExpectedItemCounts(
            ItemStack itemStack) {
        return getMarkerData(itemStack)
                .map(StructureMarkerData::expectedItemCountsById)
                .orElseGet(Map::of);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level instanceof ServerLevel serverLevel) {
            refreshAnalysisIfNeeded(serverLevel, itemStack);
            recordDiscoveredStructure(
                    serverLevel, (net.minecraft.server.level.ServerPlayer) player, itemStack);
            com.suntide_20210418.dimensiontech.network.ModNetwork.openRefreshedMarker(
                    (net.minecraft.server.level.ServerPlayer) player, itemStack, usedHand);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private static void recordDiscoveredStructure(
            ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            ItemStack itemStack) {
        Optional<MarkerInfo> markerInfo = getMarkerInfo(itemStack);
        if (markerInfo.isEmpty()) return;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure structure = registry.get(markerInfo.get().structure().id());
        if (structure == null) return;
        int structureId = registry.getId(structure);
        if (structureId < 0) return;
        CompoundTag data = player.getPersistentData();
        int[] ids = data.getIntArray(DISCOVERED_STRUCTURES_TAG);
        for (int id : ids) if (id == structureId) return;
        int[] updated = java.util.Arrays.copyOf(ids, ids.length + 1);
        updated[ids.length] = structureId;
        data.putIntArray(DISCOVERED_STRUCTURES_TAG, updated);
    }

    /**
     * Records where the server enumerated the overlapping candidates, so a later pick can be
     * resolved without asking the client where it thinks the player is.
     *
     * <p>The marker terminal does not pause the game, so the player may walk while the candidate
     * list is open: resolving a pick against the player's live position would discard a valid
     * selection, and trusting a position sent back by the client would let a modified client mark a
     * structure it never stood inside.
     */
    public static void rememberSelectionOrigin(Player player, BlockPos origin) {
        player.getPersistentData().putLong(SELECTION_ORIGIN_TAG, origin.asLong());
    }

    /**
     * Reads and clears the pending selection origin, so one enumeration authorises exactly one
     * pick.
     */
    public static Optional<BlockPos> consumeSelectionOrigin(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SELECTION_ORIGIN_TAG)) return Optional.empty();
        BlockPos origin = BlockPos.of(data.getLong(SELECTION_ORIGIN_TAG));
        data.remove(SELECTION_ORIGIN_TAG);
        return Optional.of(origin);
    }

    public static boolean markAt(
            ServerLevel level, ItemStack itemStack, BlockPos position, int selectionIndex) {
        List<MarkedStructure> structures = findStructuresAt(level, position);
        if (selectionIndex < 0 || selectionIndex >= structures.size()) return false;
        Optional<StructureMarkerData> markerData =
                createMarkerData(level, position, structures.get(selectionIndex));
        markerData.ifPresent(data -> itemStack.set(ModDataComponents.STRUCTURE_MARKER, data));
        return markerData.isPresent();
    }

    public static void clearMarker(ItemStack itemStack) {
        itemStack.remove(ModDataComponents.STRUCTURE_MARKER);
    }

    public static AnalysisStatus getAnalysisStatus(ItemStack itemStack) {
        return getMarkerData(itemStack)
                .map(StructureMarkerData::status)
                .orElse(AnalysisStatus.LEGACY);
    }

    /** Recomputes the persisted value using the current world data and configuration. */
    public static void refreshAnalysis(ServerLevel level, ItemStack itemStack) {
        getMarkerData(itemStack)
                .ifPresent(
                        existing -> {
                            MarkerInfo markerInfo =
                                    new MarkerInfo(
                                            existing.dimension(),
                                            existing.position(),
                                            existing.structure());
                            itemStack.set(
                                    ModDataComponents.STRUCTURE_MARKER,
                                    withAnalysis(
                                            existing,
                                            StructureValueCalculator.calculate(level, markerInfo),
                                            analysisFingerprint(markerInfo)));
                        });
    }

    private static void refreshAnalysisIfNeeded(ServerLevel level, ItemStack itemStack) {
        Optional<MarkerInfo> markerInfo = getMarkerInfo(itemStack);
        if (markerInfo.isEmpty()) return;
        String fingerprint = analysisFingerprint(markerInfo.get());
        String stored =
                getMarkerData(itemStack).map(StructureMarkerData::analysisFingerprint).orElse(null);
        if (!fingerprint.equals(stored)) {
            refreshAnalysis(level, itemStack);
        }
    }

    private static String analysisFingerprint(MarkerInfo markerInfo) {
        StringBuilder value =
                new StringBuilder(ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
        value.append('|').append(markerInfo.dimension()).append('|').append(markerInfo.position());
        value.append('|')
                .append(markerInfo.structure().id())
                .append('|')
                .append(markerInfo.structure().bounds());
        return Integer.toHexString(value.toString().hashCode());
    }

    public static double getDimensionValue(ItemStack itemStack) {
        return getMarkerData(itemStack)
                .map(StructureMarkerData::dimensionValue)
                .filter(Double::isFinite)
                .orElse(0.0D);
    }

    public static double getStructureValue(ItemStack itemStack) {
        return getMarkerData(itemStack).map(StructureMarkerData::structureValueOrZero).orElse(0.0D);
    }

    public static Optional<String> filterDiagnostic(ItemStack itemStack) {
        return getMarkerData(itemStack).flatMap(StructureMarkerData::filteredDiagnostic);
    }

    /**
     * Creates analysis data for a catalogue entry that is not tied to a generated structure start.
     */
    public static StructureMarkerData createCatalogueMarkerData(
            ServerLevel level, ResourceLocation structureId) {
        return createCatalogueMarkerData(
                level,
                structureId,
                com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer
                        .discoverTemplateForValue(
                                level, structureId, AnalysisStatus.EXACT, List.of()));
    }

    /**
     * Builds catalogue marker data from an analysis-service profile without querying world chunks.
     */
    public static StructureMarkerData createCatalogueMarkerData(
            ServerLevel level, ResourceLocation structureId, DiscoveryResult discovery) {
        MarkerInfo info = catalogueInfo(level, structureId);
        return withAnalysis(
                baseMarkerData(info, true),
                StructureValueCalculator.calculate(level, info, 0.0F, discovery),
                analysisFingerprint(info));
    }

    /** Builds catalogue data from a precomputed value without re-running loot analysis. */
    public static StructureMarkerData createCatalogueMarkerData(
            ServerLevel level,
            ResourceLocation structureId,
            DiscoveryResult discovery,
            StructureValueCalculator.StructureValue value) {
        MarkerInfo info = catalogueInfo(level, structureId);
        return withAnalysis(baseMarkerData(info, true), value, analysisFingerprint(info));
    }

    /**
     * A catalogue entry has no generated structure start, so it anchors at the origin with an empty
     * box; only its id carries meaning.
     */
    private static MarkerInfo catalogueInfo(ServerLevel level, ResourceLocation structureId) {
        return new MarkerInfo(
                level.dimension().location(),
                BlockPos.ZERO,
                new MarkedStructure(structureId, new BoundingBox(0, 0, 0, 0, 0, 0)));
    }

    /** A payload carrying identity only, before any analysis result is merged in. */
    private static StructureMarkerData baseMarkerData(MarkerInfo info, boolean catalogueEntry) {
        return baseMarkerData(info, catalogueEntry, Optional.empty());
    }

    /** The same, for a payload that also names a chest loot source. */
    static StructureMarkerData baseMarkerData(
            MarkerInfo info,
            boolean catalogueEntry,
            Optional<StructureMarkerData.ChestData> chestData) {
        return new StructureMarkerData(
                info.dimension(),
                info.position(),
                info.structure(),
                chestData,
                ALGORITHM_VERSION,
                IdealRandomProbabilitySpace1211.ID,
                0.0D,
                Optional.empty(),
                AnalysisStatus.LEGACY,
                List.of(),
                List.of(),
                analysisFingerprint(info),
                catalogueEntry);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(itemStack, context, tooltip, flag);

        Optional<StructureMarkerData> markerData = getMarkerData(itemStack);
        if (markerData.isEmpty()) {
            return;
        }
        StructureMarkerData data = markerData.get();

        // The payload's dimension is a parsed ResourceLocation by construction, so the 1.20.1
        // fallback that printed an unparseable id verbatim is unreachable.
        Component dimension =
                TranslateHelper.dimensionName(data.dimension()).withStyle(ChatFormatting.AQUA);
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.dimension"), dimension)
                        .withStyle(ChatFormatting.GRAY));
        if (Double.isFinite(data.dimensionValue())) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.dimension_value"),
                                    formatValue(data.dimensionValue()))
                            .withStyle(ChatFormatting.GRAY));
        }
        AnalysisStatus analysisStatus = data.status();
        boolean currentAlgorithm = data.algorithmVersion() == ALGORITHM_VERSION;
        if (currentAlgorithm
                && (analysisStatus == AnalysisStatus.EXACT
                        || analysisStatus == AnalysisStatus.APPROXIMATE)
                && data.structureValue().isPresent()) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.structure_value"),
                                    formatValue(data.structureValue().get()))
                            .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.analysis_status"),
                                Component.literal(analysisStatus.name()))
                        .withStyle(ChatFormatting.DARK_GRAY));
        data.filteredDiagnostic()
                .ifPresent(
                        message ->
                                tooltip.add(
                                        Component.literal(message).withStyle(ChatFormatting.RED)));
        if (!currentAlgorithm) {
            tooltip.add(
                    TranslateHelper.translate(TranslateHelper.tooltip("struct_marker.legacy"))
                            .withStyle(ChatFormatting.YELLOW));
        }

        getMarkerInfo(itemStack)
                .ifPresentOrElse(
                        info ->
                                tooltip.add(
                                        TranslateHelper.translate(
                                                        TranslateHelper.tooltip(
                                                                "struct_marker.structure"),
                                                        TranslateHelper.structureName(
                                                                        info.structure().id())
                                                                .withStyle(
                                                                        ChatFormatting
                                                                                .LIGHT_PURPLE))
                                                .withStyle(ChatFormatting.GRAY)),
                        () ->
                                tooltip.add(
                                        TranslateHelper.translate(
                                                        TranslateHelper.tooltip(
                                                                "struct_marker.no_structure"))
                                                .withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static Optional<StructureMarkerData> createMarkerData(
            ServerLevel level, BlockPos position, MarkedStructure selectedStructure) {
        MarkerInfo markerInfo =
                new MarkerInfo(level.dimension().location(), position, selectedStructure);
        return Optional.of(
                withAnalysis(
                        baseMarkerData(markerInfo, false),
                        StructureValueCalculator.calculate(level, markerInfo),
                        analysisFingerprint(markerInfo)));
    }

    /**
     * Merges a freshly computed analysis result into a marker payload, keeping the identity fields
     * (dimension, position, structure, chest source, catalogue flag) and re-stamping the algorithm
     * version.
     *
     * <p>An unsupported analysis deliberately persists no structure value and no per-item
     * expectations, which is what keeps an unusable payload non-readable downstream.
     *
     * <p>The fingerprint is passed in because structure markers and chest markers fingerprint
     * different inputs (a structure id versus a container loot table).
     */
    static StructureMarkerData withAnalysis(
            StructureMarkerData base, StructureValue value, String fingerprint) {
        boolean usable =
                value.status() == AnalysisStatus.EXACT
                        || value.status() == AnalysisStatus.APPROXIMATE;
        List<StructureMarkerData.ItemExpectation> itemCounts = new ArrayList<>();
        if (usable) {
            for (Map.Entry<Item, ExactProbability> entry : value.itemCounts().entrySet()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                if (itemId == null) {
                    continue;
                }
                ExactProbability expected = entry.getValue();
                itemCounts.add(
                        new StructureMarkerData.ItemExpectation(
                                itemId, expected.numerator(), expected.denominator()));
            }
        }
        return new StructureMarkerData(
                base.dimension(),
                base.position(),
                base.structure(),
                base.chestData(),
                ALGORITHM_VERSION,
                IdealRandomProbabilitySpace1211.ID,
                value.dimensionValue(),
                usable ? Optional.of(value.structureValue()) : Optional.empty(),
                value.status(),
                value.diagnostics(),
                itemCounts,
                fingerprint,
                base.catalogueEntry());
    }

    private static Optional<MarkedStructure> readStructure(CompoundTag structureData) {
        ResourceLocation structureId = ResourceLocation.tryParse(structureData.getString("Id"));
        if (structureId == null || !structureData.contains("Bounds", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag bounds = structureData.getCompound("Bounds");
        return Optional.of(
                new MarkedStructure(
                        structureId,
                        new BoundingBox(
                                bounds.getInt("MinX"),
                                bounds.getInt("MinY"),
                                bounds.getInt("MinZ"),
                                bounds.getInt("MaxX"),
                                bounds.getInt("MaxY"),
                                bounds.getInt("MaxZ"))));
    }

    private static String formatValue(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    public static List<MarkedStructure> findStructuresAt(ServerLevel level, BlockPos position) {
        List<CompoundTag> discovered = new ArrayList<>();
        StructureManager structureManager = level.structureManager();
        Registry<Structure> structureRegistry =
                level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (Structure structure : structureManager.getAllStructuresAt(position).keySet()) {
            for (StructureStart start :
                    structureManager.startsForStructure(SectionPos.of(position), structure)) {
                if (!start.isValid() || !start.getBoundingBox().isInside(position)) {
                    continue;
                }

                ResourceLocation structureId = structureRegistry.getKey(structure);
                if (structureId != null) {
                    discovered.add(createStructureData(structureId, start));
                }
            }
        }
        return discovered.stream()
                .sorted(STRUCTURE_DATA_ORDER)
                .map(StructMarkerItem::readStructure)
                .flatMap(Optional::stream)
                .toList();
    }

    private static CompoundTag createStructureData(
            ResourceLocation structureId, StructureStart start) {
        CompoundTag structureData = new CompoundTag();
        structureData.putString("Id", structureId.toString());
        structureData.putInt("StartChunkX", start.getChunkPos().x);
        structureData.putInt("StartChunkZ", start.getChunkPos().z);
        structureData.putInt("PieceCount", start.getPieces().size());
        structureData.putInt("References", start.getReferences());

        BoundingBox boundingBox = start.getBoundingBox();
        CompoundTag boundsData = new CompoundTag();
        boundsData.putInt("MinX", boundingBox.minX());
        boundsData.putInt("MinY", boundingBox.minY());
        boundsData.putInt("MinZ", boundingBox.minZ());
        boundsData.putInt("MaxX", boundingBox.maxX());
        boundsData.putInt("MaxY", boundingBox.maxY());
        boundsData.putInt("MaxZ", boundingBox.maxZ());
        structureData.put("Bounds", boundsData);
        return structureData;
    }

    private static final Comparator<CompoundTag> STRUCTURE_DATA_ORDER =
            Comparator.comparing((CompoundTag tag) -> tag.getString("Id"))
                    .thenComparingInt(tag -> tag.getInt("StartChunkX"))
                    .thenComparingInt(tag -> tag.getInt("StartChunkZ"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MinX"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MinY"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MinZ"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MaxX"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MaxY"))
                    .thenComparingInt(tag -> tag.getCompound("Bounds").getInt("MaxZ"));

    public record MarkerInfo(
            ResourceLocation dimension, BlockPos position, MarkedStructure structure) {}

    public record MarkedStructure(ResourceLocation id, BoundingBox bounds) {}
}
