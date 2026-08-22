package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.IdealRandomProbabilitySpace1201;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
    private static final String MARKER_DATA_TAG = "StructureMarkerData";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String POSITION_TAG = "Position";
    private static final String STRUCTURE_TAG = "Structure";
    private static final String LEGACY_STRUCTURES_TAG = "Structures";
    private static final String DIMENSION_VALUE_TAG = "DimensionValue";
    private static final String STRUCTURE_VALUE_TAG = "StructureValue";
    private static final String EXPECTED_ITEM_COUNTS_TAG = "ExpectedItemCounts";
    private static final String ANALYSIS_STATUS_TAG = "AnalysisStatus";
    private static final String DIAGNOSTICS_TAG = "AnalysisDiagnostics";
    private static final String ALGORITHM_VERSION_TAG = "StructureValueAlgorithmVersion";
    private static final String RANDOM_PROBABILITY_SPACE_TAG = "RandomProbabilitySpace";
    private static final String ANALYSIS_FINGERPRINT_TAG = "AnalysisFingerprint";
    private static final int ALGORITHM_VERSION = 5;

    public StructMarkerItem(Properties properties) {
        super(properties);
    }

    public static Optional<MarkerInfo> getMarkerInfo(ItemStack itemStack) {
        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        if (markerData == null) {
            return Optional.empty();
        }

        ResourceLocation dimension = ResourceLocation.tryParse(markerData.getString(DIMENSION_TAG));
        if (dimension == null || !markerData.contains(POSITION_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag positionData = markerData.getCompound(POSITION_TAG);
        BlockPos position =
                new BlockPos(
                        positionData.getInt("X"),
                        positionData.getInt("Y"),
                        positionData.getInt("Z"));
        return readMarkedStructure(markerData)
                .map(structure -> new MarkerInfo(dimension, position, structure));
    }

    /** Reads the per-item expectations persisted by the last successful analysis. */
    public static Map<ResourceLocation, ExactProbability> getExpectedItemCounts(
            ItemStack itemStack) {
        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        if (markerData == null || !markerData.contains(EXPECTED_ITEM_COUNTS_TAG, Tag.TAG_LIST)) {
            return Map.of();
        }
        LinkedHashMap<ResourceLocation, ExactProbability> result = new LinkedHashMap<>();
        ListTag entries = markerData.getList(EXPECTED_ITEM_COUNTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            ResourceLocation item = ResourceLocation.tryParse(entry.getString("Item"));
            if (item == null) continue;
            try {
                ExactProbability expected =
                        ExactProbability.of(
                                new BigInteger(entry.getString("Numerator")),
                                new BigInteger(entry.getString("Denominator")));
                result.merge(item, expected, ExactProbability::add);
            } catch (IllegalArgumentException exception) {
                // Ignore malformed entries so a damaged optional payload cannot invalidate a
                // marker.
            }
        }
        return Map.copyOf(result);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level instanceof ServerLevel serverLevel) {
            refreshAnalysisIfNeeded(serverLevel, itemStack);
            com.suntide_20210418.dimensiontech.network.ModNetwork.openRefreshedMarker(
                    (net.minecraft.server.level.ServerPlayer) player, itemStack, usedHand);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public static boolean markAt(
            ServerLevel level, ItemStack itemStack, BlockPos position, int selectionIndex) {
        List<MarkedStructure> structures = findStructuresAt(level, position);
        if (selectionIndex < 0 || selectionIndex >= structures.size()) return false;
        Optional<CompoundTag> markerData =
                createMarkerData(level, position, structures.get(selectionIndex));
        markerData.ifPresent(data -> itemStack.getOrCreateTag().put(MARKER_DATA_TAG, data));
        return markerData.isPresent();
    }

    public static void clearMarker(ItemStack itemStack) {
        itemStack.removeTagKey(MARKER_DATA_TAG);
    }

    public static AnalysisStatus getAnalysisStatus(ItemStack itemStack) {
        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        return markerData == null ? AnalysisStatus.LEGACY : analysisStatus(markerData);
    }

    /** Recomputes the persisted value using the current world data and configuration. */
    public static void refreshAnalysis(ServerLevel level, ItemStack itemStack) {
        getMarkerInfo(itemStack)
                .ifPresent(
                        markerInfo -> {
                            CompoundTag markerData =
                                    itemStack.getOrCreateTagElement(MARKER_DATA_TAG);
                            markerData.put(
                                    STRUCTURE_TAG, createStructureData(markerInfo.structure()));
                            markerData.remove(LEGACY_STRUCTURES_TAG);
                            writeAnalysisResult(
                                    markerData,
                                    StructureValueCalculator.calculate(level, markerInfo));
                            markerData.putString(
                                    ANALYSIS_FINGERPRINT_TAG, analysisFingerprint(markerInfo));
                        });
    }

    private static void refreshAnalysisIfNeeded(ServerLevel level, ItemStack itemStack) {
        Optional<MarkerInfo> markerInfo = getMarkerInfo(itemStack);
        if (markerInfo.isEmpty()) return;
        CompoundTag markerData = itemStack.getOrCreateTagElement(MARKER_DATA_TAG);
        String fingerprint = analysisFingerprint(markerInfo.get());
        if (!fingerprint.equals(markerData.getString(ANALYSIS_FINGERPRINT_TAG))) {
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
        CompoundTag data = itemStack.getTagElement(MARKER_DATA_TAG);
        return data != null && hasFiniteNumber(data, DIMENSION_VALUE_TAG)
                ? data.getDouble(DIMENSION_VALUE_TAG)
                : 0.0D;
    }

    public static double getStructureValue(ItemStack itemStack) {
        CompoundTag data = itemStack.getTagElement(MARKER_DATA_TAG);
        return data != null && hasFiniteNumber(data, STRUCTURE_VALUE_TAG)
                ? data.getDouble(STRUCTURE_VALUE_TAG)
                : 0.0D;
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, tooltip, flag);

        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        if (markerData == null) {
            return;
        }

        Component dimension =
                Component.literal(markerData.getString(DIMENSION_TAG))
                        .withStyle(ChatFormatting.AQUA);
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.dimension"), dimension)
                        .withStyle(ChatFormatting.GRAY));
        if (hasFiniteNumber(markerData, DIMENSION_VALUE_TAG)) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.dimension_value"),
                                    formatValue(markerData.getDouble(DIMENSION_VALUE_TAG)))
                            .withStyle(ChatFormatting.GRAY));
        }
        AnalysisStatus analysisStatus = analysisStatus(markerData);
        boolean currentAlgorithm = markerData.getInt(ALGORITHM_VERSION_TAG) == ALGORITHM_VERSION;
        if (currentAlgorithm
                && (analysisStatus == AnalysisStatus.EXACT
                        || analysisStatus == AnalysisStatus.APPROXIMATE)
                && markerData.contains(STRUCTURE_VALUE_TAG, Tag.TAG_ANY_NUMERIC)) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.structure_value"),
                                    formatValue(markerData.getDouble(STRUCTURE_VALUE_TAG)))
                            .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.analysis_status"),
                                Component.literal(analysisStatus.name()))
                        .withStyle(ChatFormatting.DARK_GRAY));
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
                                                        Component.literal(
                                                                        info.structure()
                                                                                .id()
                                                                                .toString())
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

    private static Optional<CompoundTag> createMarkerData(
            ServerLevel level, BlockPos position, MarkedStructure selectedStructure) {
        CompoundTag markerData = new CompoundTag();
        markerData.putString(DIMENSION_TAG, level.dimension().location().toString());

        CompoundTag positionData = new CompoundTag();
        positionData.putInt("X", position.getX());
        positionData.putInt("Y", position.getY());
        positionData.putInt("Z", position.getZ());
        markerData.put(POSITION_TAG, positionData);

        markerData.put(STRUCTURE_TAG, createStructureData(selectedStructure));
        MarkerInfo markerInfo =
                new MarkerInfo(level.dimension().location(), position, selectedStructure);
        StructureValue value = StructureValueCalculator.calculate(level, markerInfo);
        writeAnalysisResult(markerData, value);
        markerData.putString(ANALYSIS_FINGERPRINT_TAG, analysisFingerprint(markerInfo));
        return Optional.of(markerData);
    }

    /**
     * Writes the versioned exact-analysis payload while keeping unsupported values non-readable.
     */
    static void writeAnalysisResult(CompoundTag markerData, StructureValue value) {
        markerData.putInt(ALGORITHM_VERSION_TAG, ALGORITHM_VERSION);
        markerData.putString(RANDOM_PROBABILITY_SPACE_TAG, IdealRandomProbabilitySpace1201.ID);
        markerData.putDouble(DIMENSION_VALUE_TAG, value.dimensionValue());
        markerData.putString(ANALYSIS_STATUS_TAG, value.status().name());
        markerData.remove("AnalysisLuck");
        ListTag diagnostics = new ListTag();
        for (Diagnostic diagnostic : value.diagnostics()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Code", diagnostic.code());
            entry.putString("Message", diagnostic.message());
            if (diagnostic.lootTableId() != null) {
                entry.putString("LootTable", diagnostic.lootTableId().toString());
            }
            if (!diagnostic.jsonPointer().isEmpty()) {
                entry.putString("JsonPointer", diagnostic.jsonPointer());
            }
            ListTag callPath = new ListTag();
            diagnostic
                    .callPath()
                    .forEach(pathElement -> callPath.add(StringTag.valueOf(pathElement)));
            entry.put("CallPath", callPath);
            diagnostics.add(entry);
        }
        markerData.put(DIAGNOSTICS_TAG, diagnostics);
        if (value.status() == AnalysisStatus.EXACT
                || value.status() == AnalysisStatus.APPROXIMATE) {
            markerData.putDouble(STRUCTURE_VALUE_TAG, value.structureValue());
            ListTag itemCounts = new ListTag();
            for (Map.Entry<Item, ExactProbability> entry : value.itemCounts().entrySet()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                if (itemId == null) {
                    continue;
                }
                ExactProbability expected = entry.getValue();
                CompoundTag itemCount = new CompoundTag();
                itemCount.putString("Item", itemId.toString());
                itemCount.putString("Numerator", expected.numerator().toString());
                itemCount.putString("Denominator", expected.denominator().toString());
                itemCounts.add(itemCount);
            }
            markerData.put(EXPECTED_ITEM_COUNTS_TAG, itemCounts);
        } else {
            markerData.remove(STRUCTURE_VALUE_TAG);
            markerData.remove(EXPECTED_ITEM_COUNTS_TAG);
        }
    }

    static AnalysisStatus analysisStatus(CompoundTag markerData) {
        if (!markerData.contains(ALGORITHM_VERSION_TAG, Tag.TAG_ANY_NUMERIC)
                || markerData.getInt(ALGORITHM_VERSION_TAG) != ALGORITHM_VERSION) {
            return AnalysisStatus.LEGACY;
        }
        try {
            AnalysisStatus status =
                    AnalysisStatus.valueOf(markerData.getString(ANALYSIS_STATUS_TAG));
            if (!markerData.contains(RANDOM_PROBABILITY_SPACE_TAG, Tag.TAG_STRING)
                    || !IdealRandomProbabilitySpace1201.ID.equals(
                            markerData.getString(RANDOM_PROBABILITY_SPACE_TAG))
                    || !hasFiniteNumber(markerData, DIMENSION_VALUE_TAG)
                    || markerData.getDouble(DIMENSION_VALUE_TAG) < 0.0D
                    || !markerData.contains(DIAGNOSTICS_TAG, Tag.TAG_LIST)
                    || status == AnalysisStatus.LEGACY) {
                return AnalysisStatus.UNSUPPORTED;
            }
            if ((status == AnalysisStatus.EXACT || status == AnalysisStatus.APPROXIMATE)
                    && (!hasFiniteNumber(markerData, STRUCTURE_VALUE_TAG)
                            || markerData.getDouble(STRUCTURE_VALUE_TAG) < 0.0D)) {
                return AnalysisStatus.UNSUPPORTED;
            }
            return status;
        } catch (IllegalArgumentException exception) {
            return AnalysisStatus.UNSUPPORTED;
        }
    }

    private static boolean hasFiniteNumber(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) && Double.isFinite(tag.getDouble(key));
    }

    private static Optional<MarkedStructure> readMarkedStructure(CompoundTag markerData) {
        if (markerData.contains(STRUCTURE_TAG, Tag.TAG_COMPOUND)) {
            return readStructure(markerData.getCompound(STRUCTURE_TAG));
        }
        ListTag legacyStructures = markerData.getList(LEGACY_STRUCTURES_TAG, Tag.TAG_COMPOUND);
        return legacyStructures.stream()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .map(StructMarkerItem::readStructure)
                .flatMap(Optional::stream)
                .min(MARKED_STRUCTURE_ORDER);
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

    private static CompoundTag createStructureData(MarkedStructure structure) {
        CompoundTag structureData = new CompoundTag();
        structureData.putString("Id", structure.id().toString());
        BoundingBox boundingBox = structure.bounds();
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

    private static final Comparator<MarkedStructure> MARKED_STRUCTURE_ORDER =
            Comparator.comparing((MarkedStructure structure) -> structure.id().toString())
                    .thenComparingInt(structure -> structure.bounds().minX())
                    .thenComparingInt(structure -> structure.bounds().minY())
                    .thenComparingInt(structure -> structure.bounds().minZ())
                    .thenComparingInt(structure -> structure.bounds().maxX())
                    .thenComparingInt(structure -> structure.bounds().maxY())
                    .thenComparingInt(structure -> structure.bounds().maxZ());

    public record MarkerInfo(
            ResourceLocation dimension, BlockPos position, MarkedStructure structure) {}

    public record MarkedStructure(ResourceLocation id, BoundingBox bounds) {}
}
