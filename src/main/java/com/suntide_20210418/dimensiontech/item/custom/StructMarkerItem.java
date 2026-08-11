package com.suntide_20210418.dimensiontech.item.custom;

import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
    private static final String STRUCTURES_TAG = "Structures";

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
            ItemStack itemStack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
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
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.no_structure"))
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

        markerData.put(STRUCTURES_TAG, findStructures(level, position));
        return markerData;
    }

    private static ListTag findStructures(ServerLevel level, BlockPos position) {
        ListTag structures = new ListTag();
        StructureManager structureManager = level.structureManager();
        Registry<Structure> structureRegistry =
                level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (Structure structure : structureManager.getAllStructuresAt(position).keySet()) {
            StructureStart start = structureManager.getStructureAt(position, structure);
            if (!start.isValid()) {
                continue;
            }

            ResourceLocation structureId = structureRegistry.getKey(structure);
            if (structureId != null) {
                structures.add(createStructureData(structureId, start));
            }
        }
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
            ResourceLocation dimension, BlockPos position, List<MarkedStructure> structures) {
    }

    public record MarkedStructure(ResourceLocation id, BoundingBox bounds) {
    }
}
