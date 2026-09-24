package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.mojang.logging.LogUtils;

import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureAnalysisService;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;

public final class StructureDataOperatorBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int TARGET = 0;
    public static final int OPERAND_START = 1;

    /**
     * 36 write slots, laid out 9x4 on the operator face.
     *
     * <p>The count lives here rather than in {@code StructureDataOperatorLayout} because a block
     * entity is instantiated on both sides while the layout class is client-only — a dedicated
     * server must never load it. The two must agree: {@code OPERAND_COLUMNS * OPERAND_ROWS} in the
     * layout is 9 * 4 = 36.
     */
    public static final int OPERAND_COUNT = 36;

    public static final int INTEGRATOR = OPERAND_START + OPERAND_COUNT;
    public static final int INTERPRETER = INTEGRATOR + 1;
    public static final int INVENTORY_SIZE = INTERPRETER + 1;

    private final ItemStackHandler inventory =
            new ItemStackHandler(INVENTORY_SIZE) {
                @Override
                public void deserializeNBT(CompoundTag tag) {
                    super.deserializeNBT(migrateInventory(tag));
                }

                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    if (slot == TARGET || isOperandSlot(slot)) return ModItems.isMarker(stack);
                    if (slot == INTEGRATOR) return stack.is(ModItems.DATA_INTEGRATOR.get());
                    return slot == INTERPRETER
                            && stack.is(ModItems.STRUCTURE_INTERPRETER.get())
                            && !getStackInSlot(INTEGRATOR).isEmpty();
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == INTEGRATOR && !getStackInSlot(INTERPRETER).isEmpty())
                        return ItemStack.EMPTY;
                    return super.extractItem(slot, amount, simulate);
                }
            };
    private List<StructureCatalogueEntry> catalogue = List.of();
    private final Map<CatalogueKey, ItemStack> analysedCatalogueEntries = new HashMap<>();
    private final Map<CatalogueKey, String> catalogueValueConfigs = new HashMap<>();
    private final Map<CatalogueKey, Object> pendingCatalogueValues = new HashMap<>();

    public StructureDataOperatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRUCTURE_DATA_OPERATOR.get(), pos, state);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
        return new StructureDataOperatorMenu(id, player, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.dimension_tech.structure_operator.title");
    }

    public IItemHandler inventory() {
        return inventory;
    }

    /**
     * Drops every slot when the block is removed: the target marker, the operand slots and both
     * plugin slots. The catalogue and the cached per-entry analysis results are not dropped on
     * purpose — they are derived data the player can regenerate, not items that were inserted.
     */
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    stack);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public List<StructureCatalogueEntry> catalogue() {
        return catalogue;
    }

    public void refreshCatalogue(ServerPlayer player, boolean includeAllStructures) {
        Set<ResourceLocation> explored = new HashSet<>();
        if (!includeAllStructures) {
            Registry<Structure> registry =
                    player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (int rawId :
                    player.getPersistentData().getIntArray("DimensionTechDiscoveredStructures")) {
                Structure structure = registry.byId(rawId);
                ResourceLocation id = structure == null ? null : registry.getKey(structure);
                if (id != null) explored.add(id);
            }
        }
        List<StructureCatalogueEntry> entries = new ArrayList<>();
        for (ServerLevel candidateLevel : player.server.getAllLevels()) {
            Registry<Structure> registry =
                    candidateLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Set<Holder<Biome>> possibleBiomes =
                    candidateLevel
                            .getChunkSource()
                            .getGenerator()
                            .getBiomeSource()
                            .possibleBiomes();
            for (Structure structure : registry) {
                ResourceLocation structureId = registry.getKey(structure);
                if (structureId == null
                        || (!includeAllStructures && !explored.contains(structureId))) continue;
                if (structure.biomes().stream().noneMatch(possibleBiomes::contains)) continue;
                entries.add(
                        new StructureCatalogueEntry(
                                candidateLevel.dimension().location(), structureId));
            }
        }
        entries.sort(
                Comparator.comparing(StructureCatalogueEntry::dimension)
                        .thenComparing(StructureCatalogueEntry::structure));
        catalogue = List.copyOf(entries);
        analysedCatalogueEntries.clear();
    }

    public boolean copyData() {
        ItemStack source = inventory.getStackInSlot(TARGET);
        if (source.isEmpty() || StructMarkerItem.getMarkerInfo(source).isEmpty()) return false;
        CompoundTag sourceTag = source.getTag();
        if (sourceTag == null || !sourceTag.contains("StructureMarkerData", Tag.TAG_COMPOUND))
            return false;
        boolean copied = false;
        for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++) {
            ItemStack destination = inventory.getStackInSlot(slot);
            if (destination.isEmpty()) continue;
            CompoundTag destinationTag = destination.getOrCreateTag();
            destinationTag.remove("StructureMarkerData");
            destinationTag.put(
                    "StructureMarkerData", sourceTag.getCompound("StructureMarkerData").copy());
            inventory.setStackInSlot(slot, destination);
            copied = true;
        }
        if (copied) setChanged();
        return copied;
    }

    public boolean writeCatalogueEntry(ResourceLocation dimension, ResourceLocation id) {
        StructureCatalogueEntry entry =
                catalogue.stream()
                        .filter(
                                candidate ->
                                        candidate.dimension().equals(dimension)
                                                && candidate.structure().equals(id))
                        .findFirst()
                        .orElse(null);
        if (entry == null) return false;
        ItemStack analysisMarker = analysedCatalogueEntries.get(new CatalogueKey(dimension, id));
        if (analysisMarker == null) return false;
        ItemStack marker = inventory.getStackInSlot(TARGET);
        if (marker.isEmpty()) return false;
        CompoundTag markerTag = marker.getOrCreateTag();
        markerTag.remove("StructureMarkerData");
        markerTag.put(
                "StructureMarkerData", analysisMarker.getTagElement("StructureMarkerData").copy());
        inventory.setStackInSlot(TARGET, marker);
        setChanged();
        return true;
    }

    public ItemStack analyseCatalogueEntry(ResourceLocation dimension, ResourceLocation id) {
        CatalogueKey key = new CatalogueKey(dimension, id);
        ItemStack cached = analysedCatalogueEntries.get(key);
        if (cached != null
                && ModConfigs.STRUCTURE_VALUE
                        .calculationFingerprint()
                        .equals(catalogueValueConfigs.get(key))) return cached.copy();
        if (catalogue.stream()
                .noneMatch(
                        entry ->
                                entry.dimension().equals(dimension)
                                        && entry.structure().equals(id))) return ItemStack.EMPTY;
        if (level == null || level.getServer() == null) return ItemStack.EMPTY;
        ServerLevel candidateLevel =
                level.getServer()
                        .getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                        Registries.DIMENSION, dimension));
        if (candidateLevel == null) return ItemStack.EMPTY;
        requestCatalogueValue(candidateLevel, id);
        return cached == null ? ItemStack.EMPTY : cached.copy();
    }

    private void requestCatalogueValue(ServerLevel level, ResourceLocation id) {
        CatalogueKey key = new CatalogueKey(level.dimension().location(), id);
        if (pendingCatalogueValues.containsKey(key)) return;
        long requestStart = System.nanoTime();
        Object request = new Object();
        pendingCatalogueValues.put(key, request);
        String config = ModConfigs.STRUCTURE_VALUE.calculationFingerprint();
        StructMarkerItem.MarkedStructure structure =
                new StructMarkerItem.MarkedStructure(
                        id,
                        new net.minecraft.world.level.levelgen.structure.BoundingBox(
                                0, 0, 0, 0, 0, 0));
        StructMarkerItem.MarkerInfo info =
                new StructMarkerItem.MarkerInfo(
                        level.dimension().location(), net.minecraft.core.BlockPos.ZERO, structure);
        StructureAnalysisService.forServer(level.getServer())
                .discover(level, id)
                .thenComposeAsync(
                        discovery -> {
                            List<ResourceLocation> roots =
                                    discovery.structures().stream()
                                            .flatMap(value -> value.lootTables().stream())
                                            .distinct()
                                            .toList();
                            RuntimeLootAstSource source =
                                    RuntimeLootAstSource.snapshotTables(level.getServer(), roots);
                            return StructureValueCalculator.calculateAsync(
                                            level.getServer(), info, 0.0F, discovery, source)
                                    .thenApply(value -> new CatalogueCalculation(discovery, value));
                        },
                        level.getServer())
                .whenComplete(
                        (calculation, error) ->
                                level.getServer()
                                        .execute(
                                                () -> {
                                                    if (!pendingCatalogueValues.remove(key, request)
                                                            || isRemoved()
                                                            || !config.equals(
                                                                    ModConfigs.STRUCTURE_VALUE
                                                                            .calculationFingerprint()))
                                                        return;
                                                    // Every terminal outcome is archived, including a bare
                                                    // failure, so the client always receives a
                                                    // marker and stops polling instead of showing
                                                    // "analysing" forever. On success the discovery
                                                    // and value are reused; on failure a synthesized
                                                    // UNSUPPORTED value carries the error diagnostic.
                                                    long elapsedMillis =
                                                            (System.nanoTime() - requestStart)
                                                                    / 1_000_000L;
                                                    LOGGER.warn(
                                                            "[TEMP PROBE] requestCatalogueValue {} reached archive in {}ms error={} on {}",
                                                            id,
                                                            elapsedMillis,
                                                            error != null,
                                                            java.lang.Thread
                                                                    .currentThread()
                                                                    .getName());
                                                    StructureLootAnalyzer.DiscoveryResult resultDiscovery;
                                                    StructureValueCalculator.StructureValue resultValue;
                                                    if (error == null && calculation != null) {
                                                        resultDiscovery = calculation.discovery();
                                                        resultValue = calculation.value();
                                                    } else {
                                                        List<Diagnostic> failure =
                                                                error == null
                                                                        ? List.of()
                                                                        : List.of(
                                                                                new Diagnostic(
                                                                                        "ANALYSIS_FAILED",
                                                                                        error
                                                                                                .toString()));
                                                        resultDiscovery =
                                                                new StructureLootAnalyzer
                                                                                .DiscoveryResult(
                                                                        AnalysisStatus.UNSUPPORTED,
                                                                        List.of(),
                                                                        failure);
                                                        resultValue =
                                                                new StructureValueCalculator
                                                                                .StructureValue(
                                                                        AnalysisStatus.UNSUPPORTED,
                                                                        ModConfigs.STRUCTURE_VALUE
                                                                                .dimensionValue(
                                                                                        level
                                                                                                .dimension()
                                                                                                .location()),
                                                                        0.0D,
                                                                        new StackMeasure(),
                                                                        failure);
                                                    }
                                                    ItemStack marker =
                                                            new ItemStack(
                                                                    ModItems.STRUCTURE_MARKER
                                                                            .get());
                                                    marker.getOrCreateTag()
                                                            .put(
                                                                    "StructureMarkerData",
                                                                    StructMarkerItem
                                                                            .createCatalogueMarkerData(
                                                                                    level,
                                                                                    id,
                                                                                    resultDiscovery,
                                                                                    resultValue));
                                                    analysedCatalogueEntries.put(key, marker);
                                                    catalogueValueConfigs.put(key, config);
                                                }));
    }

    public void refreshCatalogueAnalysis(ResourceLocation dimension, ResourceLocation id) {
        if (level == null || level.getServer() == null) return;
        ServerLevel candidateLevel =
                level.getServer()
                        .getLevel(
                                net.minecraft.resources.ResourceKey.create(
                                        Registries.DIMENSION, dimension));
        if (candidateLevel == null) return;
        CatalogueKey key = new CatalogueKey(dimension, id);
        catalogueValueConfigs.remove(key);
        pendingCatalogueValues.remove(key);
        StructureAnalysisService.forServer(level.getServer()).request(candidateLevel, id, true);
        requestCatalogueValue(candidateLevel, id);
    }

    private record CatalogueCalculation(
            StructureLootAnalyzer.DiscoveryResult discovery,
            StructureValueCalculator.StructureValue value) {}

    public boolean clearOperandData() {
        boolean cleared = false;
        for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++) {
            ItemStack marker = inventory.getStackInSlot(slot);
            if (marker.isEmpty()
                    || marker.getTag() == null
                    || !marker.getTag().contains("StructureMarkerData", Tag.TAG_COMPOUND)) continue;
            marker.getTag().remove("StructureMarkerData");
            inventory.setStackInSlot(slot, marker);
            cleared = true;
        }
        if (cleared) setChanged();
        return cleared;
    }

    public boolean hasOperands() {
        for (int slot = OPERAND_START; slot < OPERAND_START + OPERAND_COUNT; slot++)
            if (!inventory.getStackInSlot(slot).isEmpty()) return true;
        return false;
    }

    public static boolean isOperandSlot(int slot) {
        return slot >= OPERAND_START && slot < OPERAND_START + OPERAND_COUNT;
    }

    /**
     * Rewrites a saved inventory tag so it describes {@link #INVENTORY_SIZE} slots.
     *
     * <p>{@link ItemStackHandler#deserializeNBT} resizes itself to whatever {@code Size} the tag
     * carries — the capacity is read from the save file, not from the constructor. A world saved
     * while the write array held 27 markers therefore loads this handler with 30 slots, while the
     * menu registers all 39; the first {@code AbstractContainerMenu.broadcastChanges} then reads
     * slot 30 out of range and kills the server thread with
     * {@code Slot 30 not in valid range - [0,30)}.
     *
     * <p>The handler's capacity belongs to the code, so the tag is normalised instead of the menu
     * being made to tolerate it. Growing the write array also moves the two plugin slots — an older
     * tag keeps them at {@code 28} and {@code 29}, which are write slots now — so those two entries
     * are remapped to where the current layout expects them. The write slots themselves keep their
     * indices, because the array only ever grew at its end.
     *
     * <p>Package-private rather than private so {@code StructureDataOperatorInventoryMigrationTest}
     * can drive it directly; exercising it through a load would need a live block entity.
     */
    static CompoundTag migrateInventory(CompoundTag saved) {
        CompoundTag tag = saved.copy();
        int savedSize = tag.contains("Size", Tag.TAG_INT) ? tag.getInt("Size") : INVENTORY_SIZE;
        if (savedSize == INVENTORY_SIZE) return tag;
        /*
         * A well-formed older tag holds at least the read slot, one write slot and both plugin
         * slots. Anything smaller is not a layout this code ever wrote, and the arithmetic below
         * would drag the read slot into a plugin slot, so such a tag is only clamped.
         */
        int smallestRecognisable = OPERAND_START + 3;
        if (savedSize >= smallestRecognisable && savedSize < INVENTORY_SIZE) {
            remapSlot(tag, savedSize - 2, INTEGRATOR);
            remapSlot(tag, savedSize - 1, INTERPRETER);
        }
        tag.putInt("Size", INVENTORY_SIZE);
        return tag;
    }

    /** Moves the entry sitting in {@code from} to {@code to}, if that slot holds anything. */
    private static void remapSlot(CompoundTag inventoryTag, int from, int to) {
        if (from == to) return;
        ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompound(index);
            if (entry.getInt("Slot") == from) entry.putInt("Slot", to);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    /**
     * One catalogue row. Only the pair of ids crosses the wire — the analysed marker is kept in
     * {@code analysedCatalogueEntries}, keyed the same way, and is looked up on demand.
     */
    public record StructureCatalogueEntry(
            ResourceLocation dimension, ResourceLocation structure) {}

    private record CatalogueKey(ResourceLocation dimension, ResourceLocation structure) {}
}
