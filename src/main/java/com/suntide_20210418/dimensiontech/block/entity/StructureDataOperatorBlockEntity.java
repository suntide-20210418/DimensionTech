package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.StructureAnalysisService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

public final class StructureDataOperatorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TARGET = 0;
    public static final int OPERAND_START = 1;
    public static final int OPERAND_COUNT = 27;
    public static final int INTEGRATOR = OPERAND_START + OPERAND_COUNT;
    public static final int INTERPRETER = INTEGRATOR + 1;
    public static final int INVENTORY_SIZE = INTERPRETER + 1;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == TARGET || isOperandSlot(slot)) return stack.is(ModItems.STRUCTURE_MARKER.get());
            if (slot == INTEGRATOR) return stack.is(ModItems.DATA_INTEGRATOR.get());
            return slot == INTERPRETER && stack.is(ModItems.STRUCTURE_INTERPRETER.get()) && !getStackInSlot(INTEGRATOR).isEmpty();
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == INTEGRATOR && !getStackInSlot(INTERPRETER).isEmpty()) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };
    private List<StructureCatalogueEntry> catalogue = List.of();
    private final Map<CatalogueKey, ItemStack> analysedCatalogueEntries = new HashMap<>();
    public StructureDataOperatorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.STRUCTURE_DATA_OPERATOR.get(), pos, state); }
    public IItemHandler inventory() { return inventory; }
    public boolean hasIntegrator() { return !inventory.getStackInSlot(INTEGRATOR).isEmpty(); }
    public boolean hasInterpreter() { return !inventory.getStackInSlot(INTERPRETER).isEmpty(); }
    public List<StructureCatalogueEntry> catalogue() { return catalogue; }

    public void refreshCatalogue(ServerPlayer player, boolean includeAllStructures) {
        Set<ResourceLocation> explored = new HashSet<>();
        if (!includeAllStructures) {
            Registry<Structure> registry = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (int rawId : player.getPersistentData().getIntArray("DimensionTechDiscoveredStructures")) {
                Structure structure = registry.byId(rawId);
                ResourceLocation id = structure == null ? null : registry.getKey(structure);
                if (id != null) explored.add(id);
            }
        }
        List<StructureCatalogueEntry> entries = new ArrayList<>();
        for (ServerLevel candidateLevel : player.server.getAllLevels()) {
            Registry<Structure> registry = candidateLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Set<Holder<Biome>> possibleBiomes = candidateLevel.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            for (Structure structure : registry) {
                ResourceLocation structureId = registry.getKey(structure);
                if (structureId == null || (!includeAllStructures && !explored.contains(structureId))) continue;
                if (structure.biomes().stream().noneMatch(possibleBiomes::contains)) continue;
                entries.add(new StructureCatalogueEntry(candidateLevel.dimension().location(), structureId, ItemStack.EMPTY));
            }
        }
        entries.sort(Comparator.comparing(StructureCatalogueEntry::dimension).thenComparing(StructureCatalogueEntry::structure));
        catalogue = List.copyOf(entries);
        analysedCatalogueEntries.clear();
    }
    public boolean copyData() {
        ItemStack source = inventory.getStackInSlot(TARGET);
        if (source.isEmpty() || StructMarkerItem.getMarkerInfo(source).isEmpty()) return false;
        CompoundTag sourceTag = source.getTag(); if (sourceTag == null || !sourceTag.contains("StructureMarkerData", Tag.TAG_COMPOUND)) return false;
        boolean copied = false;
        for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++) {
            ItemStack destination = inventory.getStackInSlot(slot);
            if (destination.isEmpty()) continue;
            CompoundTag destinationTag = destination.getOrCreateTag();
            destinationTag.remove("StructureMarkerData");
            destinationTag.put("StructureMarkerData", sourceTag.getCompound("StructureMarkerData").copy());
            inventory.setStackInSlot(slot, destination);
            copied = true;
        }
        if (copied) setChanged();
        return copied;
    }
    public boolean writeCatalogueEntry(ResourceLocation dimension, ResourceLocation id) {
        StructureCatalogueEntry entry = catalogue.stream()
                .filter(candidate -> candidate.dimension().equals(dimension) && candidate.structure().equals(id))
                .findFirst().orElse(null);
        if (entry == null) return false;
        ItemStack analysisMarker = analysedCatalogueEntries.get(new CatalogueKey(dimension, id));
        if (analysisMarker == null) return false;
        ItemStack marker = inventory.getStackInSlot(TARGET);
        if (marker.isEmpty()) return false;
        CompoundTag markerTag = marker.getOrCreateTag();
        markerTag.remove("StructureMarkerData");
        markerTag.put("StructureMarkerData", analysisMarker.getTagElement("StructureMarkerData").copy());
        inventory.setStackInSlot(TARGET, marker);
        setChanged();
        return true;
    }
    public ItemStack analyseCatalogueEntry(ResourceLocation dimension, ResourceLocation id) {
        CatalogueKey key = new CatalogueKey(dimension, id);
        ItemStack cached = analysedCatalogueEntries.get(key);
        if (cached != null) return cached.copy();
        if (catalogue.stream().noneMatch(entry -> entry.dimension().equals(dimension) && entry.structure().equals(id))) return ItemStack.EMPTY;
        if (level == null || level.getServer() == null) return ItemStack.EMPTY;
        ServerLevel candidateLevel = level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension));
        if (candidateLevel == null) return ItemStack.EMPTY;
        StructureAnalysisService.State state =
                StructureAnalysisService.forServer(level.getServer()).request(candidateLevel, id, false);
        if (!state.complete() || state.result() == null) return ItemStack.EMPTY;
        ItemStack analysisMarker = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        analysisMarker
                .getOrCreateTag()
                .put(
                        "StructureMarkerData",
                        StructMarkerItem.createCatalogueMarkerData(candidateLevel, id, state.result()));
        analysedCatalogueEntries.put(key, analysisMarker.copy());
        return analysisMarker;
    }
    public StructureAnalysisService.State catalogueAnalysisState(
            ResourceLocation dimension, ResourceLocation id) {
        if (level == null || level.getServer() == null) return StructureAnalysisService.State.missing();
        ServerLevel candidateLevel =
                level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension));
        return candidateLevel == null
                ? StructureAnalysisService.State.missing()
                : StructureAnalysisService.forServer(level.getServer()).state(candidateLevel, id);
    }
    public void refreshCatalogueAnalysis(ResourceLocation dimension, ResourceLocation id) {
        if (level == null || level.getServer() == null) return;
        ServerLevel candidateLevel =
                level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension));
        if (candidateLevel == null) return;
        analysedCatalogueEntries.remove(new CatalogueKey(dimension, id));
        StructureAnalysisService.forServer(level.getServer()).request(candidateLevel, id, true);
    }
    public boolean clearOperandData() {
        boolean cleared = false;
        for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++) {
            ItemStack marker = inventory.getStackInSlot(slot);
            if (marker.isEmpty() || marker.getTag() == null || !marker.getTag().contains("StructureMarkerData", Tag.TAG_COMPOUND)) continue;
            marker.getTag().remove("StructureMarkerData");
            inventory.setStackInSlot(slot, marker);
            cleared = true;
        }
        if (cleared) setChanged();
        return cleared;
    }
    public boolean hasOperands() { for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++) if (!inventory.getStackInSlot(slot).isEmpty()) return true; return false; }
    public static boolean isOperandSlot(int slot) { return slot >= OPERAND_START && slot < OPERAND_START + OPERAND_COUNT; }
    @Override public Component getDisplayName() { return Component.translatable("block.dimension_tech.structure_data_operator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new StructureDataOperatorMenu(id, inv, this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Inventory", inventory.serializeNBT()); }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("Inventory")); }

    public record StructureCatalogueEntry(ResourceLocation dimension, ResourceLocation structure, ItemStack analysisMarker) {
        public StructureCatalogueEntry { analysisMarker = analysisMarker.copy(); }
        public ItemStack analysisMarker() { return analysisMarker.copy(); }
    }
    private record CatalogueKey(ResourceLocation dimension, ResourceLocation structure) {}
}
