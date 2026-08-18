package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.FullDurabilityLoot;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public abstract class BaseMinerBlockEntity extends BlockEntity implements MenuProvider {
    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");
    private static final String INVENTORY_TAG = "Inventory";
    private static final String ENERGY_TAG = "Energy";
    private static final String PROGRESS_TAG = "Progress";
    private static final String PENDING_OUTPUT_TAG = "PendingOutput";
    private static final String PARALLEL_FRACTION_TAG = "ParallelFractionHundredths";
    private static final String QUANTITY_FRACTION_TAG = "QuantityFractionHundredths";
    private static final int DRAWS_PER_PARALLEL = 8;

    private final ItemStackHandler itemHandler;
    private final MinerEnergyStorage energyStorage;
    private LazyOptional<IItemHandler> itemHandlerCapability;
    private LazyOptional<IEnergyStorage> energyCapability;
    private CompoundTag analyzedMarkerSnapshot;
    private List<CachedMarkerLoot> cachedMarkerLoot = List.of();
    private List<ItemStack> pendingOutput = new ArrayList<>();
    private int progress;
    private int dynamicProcessingTime = 400;
    private int dynamicParallelHundredths = 100;
    private int parallelFractionHundredths;
    private int quantityFractionHundredths;

    protected BaseMinerBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
        this.itemHandler = createItemHandler();
        this.energyStorage = new MinerEnergyStorage(getEnergyCapacity());
        this.itemHandlerCapability = LazyOptional.of(() -> itemHandler);
        this.energyCapability = LazyOptional.of(() -> energyStorage);
    }

    protected abstract int getSlotCount();

    protected abstract String getTranslationName();

    protected abstract int getBaseParallel();

    protected abstract float getMachineLuck();

    protected abstract double getMachineEfficiency();

    protected abstract double getQuantityReference();

    protected abstract int getEnergyCapacity();

    public abstract int getEnergyConsumption();

    public int getProcessingTime() {
        return dynamicProcessingTime;
    }

    public int getDrawParallel() {
        return Math.max(1, getBaseParallel() * dynamicParallelHundredths / 100);
    }

    public int getBaseParallelCount() {
        return getBaseParallel();
    }

    public int getAccumulatedParallelHundredths() {
        return parallelFractionHundredths;
    }

    public int getAdditionalItemCount() {
        return Math.max(0, getPendingOutputCount());
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressPercent() {
        return (int) Math.min(100L, (long) progress * 100L / getProcessingTime());
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public boolean isOutputBlocked() {
        return !pendingOutput.isEmpty();
    }

    public int getPendingOutputCount() {
        return pendingOutput.stream().mapToInt(ItemStack::getCount).sum();
    }

    public List<ResourceLocation> getMarkedStructures() {
        List<ResourceLocation> structures = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            StructMarkerItem.getMarkerInfo(itemHandler.getStackInSlot(slot))
                    .ifPresent(
                            markerInfo ->
                                    markerInfo.structures().stream()
                                            .map(StructMarkerItem.MarkedStructure::id)
                                            .forEach(structures::add));
        }
        return structures.stream().distinct().toList();
    }

    public OutputState getOutputState() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return OutputState.NONE;
        }
        AdjacentOutputs outputs = findAdjacentOutputs(serverLevel);
        if (!outputs.meInterfaces().isEmpty()) {
            return OutputState.ME_NETWORK;
        }
        if (!outputs.itemHandlers().isEmpty()) {
            return OutputState.ITEM_HANDLER;
        }
        return OutputState.NONE;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!pendingOutput.isEmpty()) {
            retryPendingOutput(serverLevel);
            return;
        }
        if (!hasValidMarker()) {
            return;
        }

        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlan();

        int energyConsumption = getEnergyConsumption();
        if (!energyStorage.canConsume(energyConsumption)) {
            return;
        }

        energyStorage.consume(energyConsumption);
        progress++;
        if (progress >= getProcessingTime()) {
            progress = 0;
            drawMarkerLoot(serverLevel.getServer(), drawParallelForCycle());
        }
        setChanged();
    }

    private boolean hasValidMarker() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (marker.is(ModItems.STRUCT_MARKER.get())
                    && StructMarkerItem.getMarkerInfo(marker)
                            .filter(markerInfo -> !markerInfo.structures().isEmpty())
                            .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private int drawParallelForCycle() {
        int scaledParallel = getBaseParallel() * dynamicParallelHundredths;
        int integerPart = scaledParallel / 100;
        int fractionalPart = scaledParallel % 100;
        int accumulated = parallelFractionHundredths + fractionalPart;
        int carry = accumulated / 100;
        parallelFractionHundredths = accumulated % 100;
        return Math.max(1, integerPart + carry);
    }

    private void drawMarkerLoot(MinecraftServer server, int parallel) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }

        refreshMarkerLootCache(server);
        List<ItemStack> mergedLoot = new ArrayList<>();
        Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
        double quantity = 0.0D;
        ServerLevel randomLevel = null;
        for (CachedMarkerLoot cachedLoot : cachedMarkerLoot) {
            ResourceKey<Level> dimensionKey =
                    ResourceKey.create(Registries.DIMENSION, cachedLoot.dimension());
            ServerLevel lootLevel = server.getLevel(dimensionKey);
            if (lootLevel == null) continue;
            if (randomLevel == null) randomLevel = lootLevel;
            quantity += cachedLoot.quantity();
            cachedLoot.expectedItems().forEach(
                    (item, expected) -> expectedItems.merge(item, expected, ExactProbability::add));
        }
        if (randomLevel != null && Double.isFinite(quantity) && quantity > 0.0D) {
            drawExpectedLoot(randomLevel, expectedItems, drawsForQuantity(parallel, quantity))
                    .forEach(stack -> mergeLootStack(mergedLoot, stack));
        }

        AdjacentOutputs outputs = findAdjacentOutputs(outputLevel);
        pendingOutput = outputLoot(outputs, mergedLoot);
        setChanged();
    }

    private void refreshMarkerLootCache(MinecraftServer server) {
        CompoundTag currentSnapshot = itemHandler.serializeNBT();
        currentSnapshot.putFloat("MachineLuck", getMachineLuck());
        if (currentSnapshot.equals(analyzedMarkerSnapshot)) {
            return;
        }

        List<CachedMarkerLoot> refreshedCache = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (!marker.is(ModItems.STRUCT_MARKER.get())) {
                continue;
            }

            StructMarkerItem.getMarkerInfo(marker)
                    .filter(markerInfo -> !markerInfo.structures().isEmpty())
                    .map(
                                    markerInfo ->
                                    analyzeMarkerLoot(server, marker, markerInfo))
                    .ifPresent(refreshedCache::add);
        }
        cachedMarkerLoot = List.copyOf(refreshedCache);
        analyzedMarkerSnapshot = currentSnapshot;
    }

    private CachedMarkerLoot analyzeMarkerLoot(
            MinecraftServer server, ItemStack marker, MarkerInfo markerInfo) {
        ResourceKey<Level> dimensionKey =
                ResourceKey.create(Registries.DIMENSION, markerInfo.dimension());
        ServerLevel analysisLevel = server.getLevel(dimensionKey);
        if (analysisLevel == null) {
            analysisLevel = level instanceof ServerLevel current ? current : null;
        }
        if (analysisLevel == null) {
            return new CachedMarkerLoot(
                    markerInfo.dimension(), markerInfo.position(), 0.0D, 0.0D, Map.of());
        }
        var analysis =
                StructureValueCalculator.calculate(analysisLevel, markerInfo, getMachineLuck());
        Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
        analysis.itemCounts().forEach(
                (item, expected) -> {
                    ResourceLocation itemId =
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                    if (itemId != null) {
                        expectedItems.put(itemId, expected);
                    }
                });
        return new CachedMarkerLoot(
                markerInfo.dimension(),
                markerInfo.position(),
                analysis.structureValue(),
                analysis.itemCounts().values().stream()
                        .mapToDouble(ExactProbability::finiteDoubleValue)
                        .filter(Double::isFinite)
                        .filter(value -> value > 0.0D)
                        .sum(),
                expectedItems);
    }

    private List<ItemStack> drawExpectedLoot(
            ServerLevel level, Map<ResourceLocation, ExactProbability> expectedItems, int draws) {
        List<WeightedItem> weightedItems = new ArrayList<>();
        double total = 0.0D;
        for (Map.Entry<ResourceLocation, ExactProbability> entry : expectedItems.entrySet()) {
            double weight = entry.getValue().finiteDoubleValue();
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
            if (item == null || !Double.isFinite(weight) || weight <= 0.0D) continue;
            weightedItems.add(new WeightedItem(item, weight));
            total += weight;
        }
        if (weightedItems.isEmpty() || !Double.isFinite(total) || total <= 0.0D) return List.of();

        List<ItemStack> result = new ArrayList<>();
        for (int draw = 0; draw < draws; draw++) {
            double target = level.random.nextDouble() * total;
            for (WeightedItem weighted : weightedItems) {
                target -= weighted.weight();
                if (target <= 0.0D) {
                    result.add(new ItemStack(weighted.item(), 1));
                    break;
                }
            }
        }
        return result;
    }

    private int drawsForQuantity(int parallel, double quantity) {
        double reference = getQuantityReference();
        if (!Double.isFinite(reference) || reference <= 0.0D) {
            return parallel * DRAWS_PER_PARALLEL;
        }
        int factorHundredths =
                BigDecimal.valueOf(quantity)
                        .divide(BigDecimal.valueOf(reference), 2, RoundingMode.DOWN)
                        .movePointRight(2)
                        .min(BigDecimal.valueOf(Integer.MAX_VALUE))
                        .intValue();
        long scaled = (long) parallel * DRAWS_PER_PARALLEL * factorHundredths;
        long whole = scaled / 100L;
        int accumulated = quantityFractionHundredths + (int) (scaled % 100L);
        whole += accumulated / 100;
        quantityFractionHundredths = accumulated % 100;
        return (int) Math.min(Integer.MAX_VALUE, whole);
    }

    private void updateProcessingPlan() {
        double structureValue =
                cachedMarkerLoot.stream()
                        .mapToDouble(CachedMarkerLoot::structureValue)
                        .filter(Double::isFinite)
                        .filter(value -> value > 0.0D)
                        .sum();
        double efficiency = getMachineEfficiency();
        if (!Double.isFinite(efficiency) || efficiency <= 0.0D || structureValue <= 0.0D) {
            dynamicProcessingTime = 400;
            dynamicParallelHundredths = 100;
            return;
        }

        double calculatedTicks = structureValue / efficiency;
        if (!Double.isFinite(calculatedTicks) || calculatedTicks >= 400.0D) {
            dynamicProcessingTime =
                    (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(400.0D, calculatedTicks)));
            dynamicParallelHundredths = 100;
            return;
        }

        dynamicProcessingTime = 400;
        int maxParallelHundredths = Integer.MAX_VALUE / Math.max(1, getBaseParallel());
        dynamicParallelHundredths =
                BigDecimal.valueOf(400.0D)
                        .divide(BigDecimal.valueOf(calculatedTicks), 2, RoundingMode.DOWN)
                        .movePointRight(2)
                        .min(BigDecimal.valueOf(maxParallelHundredths))
                        .intValue();
        dynamicParallelHundredths = Math.max(100, dynamicParallelHundredths);
    }

    private void mergeLootStack(List<ItemStack> mergedLoot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack mergedStack : mergedLoot) {
            if (ItemHandlerHelper.canItemStacksStack(mergedStack, stack)) {
                mergedStack.grow(stack.getCount());
                return;
            }
        }
        mergedLoot.add(stack.copy());
    }

    private AdjacentOutputs findAdjacentOutputs(ServerLevel outputLevel) {
        List<BlockEntity> meInterfaces = new ArrayList<>();
        List<IItemHandler> handlers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity adjacent = outputLevel.getBlockEntity(worldPosition.relative(direction));
            if (adjacent == null) {
                continue;
            }
            if (AE2_LOADED && Ae2Integration.isOnlineInterface(adjacent)) {
                meInterfaces.add(adjacent);
                continue;
            }
            adjacent.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .resolve()
                    .ifPresent(handlers::add);
        }
        return new AdjacentOutputs(List.copyOf(meInterfaces), List.copyOf(handlers));
    }

    private void retryPendingOutput(ServerLevel outputLevel) {
        pendingOutput = outputLoot(findAdjacentOutputs(outputLevel), pendingOutput);
        setChanged();
    }

    private List<ItemStack> outputLoot(AdjacentOutputs outputs, List<ItemStack> stacks) {
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remainder = FullDurabilityLoot.normalize(stack);
            for (BlockEntity meInterface : outputs.meInterfaces()) {
                remainder = Ae2Integration.insertIntoInterfaceNetwork(meInterface, remainder);
                if (remainder.isEmpty()) {
                    break;
                }
            }
            for (IItemHandler outputHandler : outputs.itemHandlers()) {
                if (remainder.isEmpty()) {
                    break;
                }
                remainder = ItemHandlerHelper.insertItemStacked(outputHandler, remainder, false);
            }
            if (!remainder.isEmpty()) {
                remainders.add(remainder);
            }
        }
        return List.copyOf(remainders);
    }

    private record AdjacentOutputs(
            List<BlockEntity> meInterfaces, List<IItemHandler> itemHandlers) {}

    private record CachedMarkerLoot(
            ResourceLocation dimension,
            BlockPos position,
            double structureValue,
            double quantity,
            Map<ResourceLocation, ExactProbability> expectedItems) {}

    private record WeightedItem(Item item, double weight) {}

    private ItemStackHandler createItemHandler() {
        int slotCount = getSlotCount();
        if (slotCount <= 0) {
            throw new IllegalStateException("Mythic miner slot count must be positive");
        }

        return new ItemStackHandler(slotCount) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.is(ModItems.STRUCT_MARKER.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, itemHandler.serializeNBT());
        tag.putInt(ENERGY_TAG, energyStorage.getEnergyStored());
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(PARALLEL_FRACTION_TAG, parallelFractionHundredths);
        tag.putInt(QUANTITY_FRACTION_TAG, quantityFractionHundredths);
        ListTag pendingOutputTag = new ListTag();
        pendingOutput.forEach(stack -> pendingOutputTag.add(stack.save(new CompoundTag())));
        tag.put(PENDING_OUTPUT_TAG, pendingOutputTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound(INVENTORY_TAG);
            inventoryTag.putInt("Size", itemHandler.getSlots());
            itemHandler.deserializeNBT(inventoryTag);
        }
        if (tag.contains(ENERGY_TAG, Tag.TAG_INT)) {
            energyStorage.setEnergy(tag.getInt(ENERGY_TAG));
        }
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        parallelFractionHundredths = Math.max(0, Math.min(99, tag.getInt(PARALLEL_FRACTION_TAG)));
        quantityFractionHundredths = Math.max(0, Math.min(99, tag.getInt(QUANTITY_FRACTION_TAG)));
        List<ItemStack> loadedPendingOutput = new ArrayList<>();
        ListTag pendingOutputTag = tag.getList(PENDING_OUTPUT_TAG, Tag.TAG_COMPOUND);
        pendingOutputTag.forEach(
                stackTag -> {
                    ItemStack stack = ItemStack.of((CompoundTag) stackTag);
                    if (!stack.isEmpty()) {
                        loadedPendingOutput.add(stack);
                    }
                });
        pendingOutput = List.copyOf(loadedPendingOutput);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCapability.cast();
        }
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCapability.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandlerCapability = LazyOptional.of(() -> itemHandler);
        energyCapability = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public Component getDisplayName() {
        return TranslateHelper.translate(TranslateHelper.container(getTranslationName()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MythicMinerMenu(containerId, playerInventory, this);
    }

    public enum OutputState {
        ME_NETWORK,
        ITEM_HANDLER,
        NONE
    }

    private final class MinerEnergyStorage implements IEnergyStorage {
        private final int capacity;
        private int energy;

        private MinerEnergyStorage(int capacity) {
            if (capacity <= 0) {
                throw new IllegalStateException("Mythic miner energy capacity must be positive");
            }
            this.capacity = capacity;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = Math.min(capacity - energy, Math.max(0, maxReceive));
            if (!simulate && received > 0) {
                energy += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private void setEnergy(int energy) {
            this.energy = Math.min(capacity, Math.max(0, energy));
        }

        private boolean canConsume(int amount) {
            return amount > 0 && energy >= amount;
        }

        private void consume(int amount) {
            energy -= amount;
            setChanged();
        }
    }
}
