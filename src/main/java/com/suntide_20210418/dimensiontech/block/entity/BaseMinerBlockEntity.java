package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.LootTableLottery;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

    private final ItemStackHandler itemHandler;
    private final MinerEnergyStorage energyStorage;
    private LazyOptional<IItemHandler> itemHandlerCapability;
    private LazyOptional<IEnergyStorage> energyCapability;
    private CompoundTag analyzedMarkerSnapshot;
    private List<CachedMarkerLoot> cachedMarkerLoot = List.of();
    private int progress;

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

    protected abstract float getDrawLuck();

    protected abstract int getDrawParallel();

    protected abstract int getEnergyCapacity();

    protected abstract int getEnergyConsumption();

    protected abstract int getProcessingTime();

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

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || !hasValidMarker()) {
            return;
        }

        int energyConsumption = getEnergyConsumption();
        if (!energyStorage.canConsume(energyConsumption)) {
            return;
        }

        energyStorage.consume(energyConsumption);
        progress++;
        if (progress >= getProcessingTime()) {
            progress = 0;
            drawMarkerLoot(serverLevel.getServer());
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

    private void drawMarkerLoot(MinecraftServer server) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }

        refreshMarkerLootCache(server);
        List<ItemStack> mergedLoot = new ArrayList<>();
        for (CachedMarkerLoot cachedLoot : cachedMarkerLoot) {
            ResourceKey<Level> dimensionKey =
                    ResourceKey.create(Registries.DIMENSION, cachedLoot.dimension());
            ServerLevel lootLevel = server.getLevel(dimensionKey);
            if (lootLevel == null) {
                continue;
            }

            Vec3 origin = Vec3.atCenterOf(cachedLoot.position());
            for (StructureLoot structureLoot : cachedLoot.structures()) {
                List<ItemStack> loot =
                        LootTableLottery.draw(
                                lootLevel,
                                origin,
                                structureLoot.lootTables(),
                                null,
                                getDrawLuck(),
                                getDrawParallel());
                loot.forEach(stack -> mergeLootStack(mergedLoot, stack));
            }
        }

        AdjacentOutputs outputs = findAdjacentOutputs(outputLevel);
        mergedLoot.forEach(stack -> outputLoot(outputLevel, outputs, stack));
    }

    private void refreshMarkerLootCache(MinecraftServer server) {
        CompoundTag currentSnapshot = itemHandler.serializeNBT();
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
                    .map(markerInfo -> analyzeMarkerLoot(server, markerInfo))
                    .ifPresent(refreshedCache::add);
        }
        cachedMarkerLoot = List.copyOf(refreshedCache);
        analyzedMarkerSnapshot = currentSnapshot;
    }

    private CachedMarkerLoot analyzeMarkerLoot(MinecraftServer server, MarkerInfo markerInfo) {
        return new CachedMarkerLoot(
                markerInfo.dimension(),
                markerInfo.position(),
                StructureLootAnalyzer.analyze(server, markerInfo));
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

    private void outputLoot(ServerLevel outputLevel, AdjacentOutputs outputs, ItemStack stack) {
        ItemStack remainder = stack;
        for (BlockEntity meInterface : outputs.meInterfaces()) {
            remainder = Ae2Integration.insertIntoInterfaceNetwork(meInterface, remainder);
            if (remainder.isEmpty()) {
                return;
            }
        }
        for (IItemHandler outputHandler : outputs.itemHandlers()) {
            remainder = ItemHandlerHelper.insertItemStacked(outputHandler, remainder, false);
            if (remainder.isEmpty()) {
                return;
            }
        }
        Block.popResource(outputLevel, worldPosition, remainder);
    }

    private record AdjacentOutputs(
            List<BlockEntity> meInterfaces, List<IItemHandler> itemHandlers) {}

    private record CachedMarkerLoot(
            ResourceLocation dimension, BlockPos position, List<StructureLoot> structures) {}

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
