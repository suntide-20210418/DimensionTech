package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.IdealRandomProbabilitySpace1201;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public class StructMarkerItem extends Item {
    private static final String MARKER_DATA_TAG = "StructureMarkerData";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String POSITION_TAG = "Position";
    private static final String STRUCTURES_TAG = "Structures";
    private static final String DIMENSION_VALUE_TAG = "DimensionValue";
    private static final String STRUCTURE_VALUE_TAG = "StructureValue";
    private static final String ANALYSIS_STATUS_TAG = "AnalysisStatus";
    private static final String ANALYSIS_LUCK_TAG = "AnalysisLuck";
    private static final String DIAGNOSTICS_TAG = "AnalysisDiagnostics";
    private static final String ALGORITHM_VERSION_TAG = "StructureValueAlgorithmVersion";
    private static final String RANDOM_PROBABILITY_SPACE_TAG = "RandomProbabilitySpace";
    private static final int ALGORITHM_VERSION = 4;

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
        List<MarkedStructure> structures = new ArrayList<>();
        ListTag structureTags = markerData.getList(STRUCTURES_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < structureTags.size(); index++) {
            CompoundTag structureData = structureTags.getCompound(index);
            ResourceLocation structureId = ResourceLocation.tryParse(structureData.getString("Id"));
            if (structureId == null || !structureData.contains("Bounds", Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag bounds = structureData.getCompound("Bounds");
            structures.add(
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
        return Optional.of(new MarkerInfo(dimension, position, List.copyOf(structures)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level instanceof ServerLevel serverLevel) {
            BlockPos position = player.blockPosition();
            CompoundTag markerData = createMarkerData(serverLevel, position);
            itemStack.getOrCreateTag().put(MARKER_DATA_TAG, markerData);

            int structureCount = markerData.getList(STRUCTURES_TAG, Tag.TAG_COMPOUND).size();
            player.displayClientMessage(
                    TranslateHelper.translate(
                            TranslateHelper.message("struct_marker.saved"),
                            markerData.getString(DIMENSION_TAG),
                            position.getX(),
                            position.getY(),
                            position.getZ(),
                            structureCount),
                    true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
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
                && analysisStatus == AnalysisStatus.EXACT
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

        ListTag structures = markerData.getList(STRUCTURES_TAG, Tag.TAG_COMPOUND);
        boolean hasStructure = false;
        for (int index = 0; index < structures.size(); index++) {
            String structureId = structures.getCompound(index).getString("Id");
            if (!structureId.isEmpty()) {
                hasStructure = true;
                tooltip.add(
                        TranslateHelper.translate(
                                        TranslateHelper.tooltip("struct_marker.structure"),
                                        Component.literal(structureId)
                                                .withStyle(ChatFormatting.LIGHT_PURPLE))
                                .withStyle(ChatFormatting.GRAY));
            }
        }

        if (!hasStructure) {
            tooltip.add(
                    TranslateHelper.translate(TranslateHelper.tooltip("struct_marker.no_structure"))
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static CompoundTag createMarkerData(ServerLevel level, BlockPos position) {
        CompoundTag markerData = new CompoundTag();
        markerData.putString(DIMENSION_TAG, level.dimension().location().toString());

        CompoundTag positionData = new CompoundTag();
        positionData.putInt("X", position.getX());
        positionData.putInt("Y", position.getY());
        positionData.putInt("Z", position.getZ());
        markerData.put(POSITION_TAG, positionData);

        ListTag structures = findStructures(level, position);
        markerData.put(STRUCTURES_TAG, structures);
        MarkerInfo markerInfo =
                new MarkerInfo(
                        level.dimension().location(), position, readMarkedStructures(structures));
        StructureValue value = StructureValueCalculator.calculate(level, markerInfo);
        writeAnalysisResult(markerData, value);
        return markerData;
    }

    /** Writes the versioned exact-analysis payload while keeping unsupported values non-readable. */
    static void writeAnalysisResult(CompoundTag markerData, StructureValue value) {
        markerData.putInt(ALGORITHM_VERSION_TAG, ALGORITHM_VERSION);
        markerData.putString(RANDOM_PROBABILITY_SPACE_TAG, IdealRandomProbabilitySpace1201.ID);
        markerData.putDouble(DIMENSION_VALUE_TAG, value.dimensionValue());
        markerData.putString(ANALYSIS_STATUS_TAG, value.status().name());
        markerData.putFloat(ANALYSIS_LUCK_TAG, value.luck());
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
        if (value.status() == AnalysisStatus.EXACT) {
            markerData.putDouble(STRUCTURE_VALUE_TAG, value.structureValue());
        } else {
            markerData.remove(STRUCTURE_VALUE_TAG);
        }
    }

    static AnalysisStatus analysisStatus(CompoundTag markerData) {
        if (!markerData.contains(ALGORITHM_VERSION_TAG, Tag.TAG_ANY_NUMERIC)
                || markerData.getInt(ALGORITHM_VERSION_TAG) != ALGORITHM_VERSION) {
            return AnalysisStatus.LEGACY;
        }
        try {
            AnalysisStatus status = AnalysisStatus.valueOf(markerData.getString(ANALYSIS_STATUS_TAG));
            if (!markerData.contains(RANDOM_PROBABILITY_SPACE_TAG, Tag.TAG_STRING)
                    || !IdealRandomProbabilitySpace1201.ID.equals(
                            markerData.getString(RANDOM_PROBABILITY_SPACE_TAG))
                    || !hasFiniteNumber(markerData, DIMENSION_VALUE_TAG)
                    || markerData.getDouble(DIMENSION_VALUE_TAG) < 0.0D
                    || !hasFiniteNumber(markerData, ANALYSIS_LUCK_TAG)
                    || !markerData.contains(DIAGNOSTICS_TAG, Tag.TAG_LIST)
                    || status == AnalysisStatus.LEGACY) {
                return AnalysisStatus.UNSUPPORTED;
            }
            if (status == AnalysisStatus.EXACT
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

    private static List<MarkedStructure> readMarkedStructures(ListTag structureTags) {
        List<MarkedStructure> structures = new ArrayList<>();
        for (int index = 0; index < structureTags.size(); index++) {
            CompoundTag structureData = structureTags.getCompound(index);
            ResourceLocation structureId = ResourceLocation.tryParse(structureData.getString("Id"));
            if (structureId == null || !structureData.contains("Bounds", Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag bounds = structureData.getCompound("Bounds");
            structures.add(
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
        return List.copyOf(structures);
    }

    private static String formatValue(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static ListTag findStructures(ServerLevel level, BlockPos position) {
        ListTag structures = new ListTag();
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
        discovered.stream()
                .sorted(
                        Comparator.comparing((CompoundTag tag) -> tag.getString("Id"))
                                .thenComparingInt(tag -> tag.getInt("StartChunkX"))
                                .thenComparingInt(tag -> tag.getInt("StartChunkZ"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MinX"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MinY"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MinZ"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MaxX"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MaxY"))
                                .thenComparingInt(
                                        tag -> tag.getCompound("Bounds").getInt("MaxZ")))
                .forEach(structures::add);
        return structures;
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

    public record MarkerInfo(
            ResourceLocation dimension, BlockPos position, List<MarkedStructure> structures) {}

    public record MarkedStructure(ResourceLocation id, BoundingBox bounds) {}
}
