package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.StructureMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.energy.EnergyContainer;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.MarkerAnalysis;
import com.suntide_20210418.dimensiontech.structure.analysis.AnalysisDataFingerprint;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfig;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfigService;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The structure miner controller.
 *
 * <p><b>Its I/O lives in the chambers.</b> The controller no longer owns a fluid tank, an FE buffer
 * or an output router: those are the fluid input chamber, the energy input chamber and the item
 * output chamber, which stand in the multiblock's casing ring. On every tick the controller walks
 * the pattern's casing cells in a fixed order, takes the first chamber of each kind, and settles
 * fluid, energy and output through them. With no chamber of a given kind, that part of the cycle
 * simply cannot complete — the machine stalls rather than silently working without it.
 */
public abstract class BaseMinerBlockEntity extends BlockEntity implements MenuProvider {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String PROGRESS_TAG = "Progress";
    private static final String SLOT_PROGRESS_TAG = "SlotProgress";
    private static final String PENDING_OUTPUT_TAG = "PendingOutput";
    private static final String PARALLEL_FRACTION_TAG = "ParallelFractionHundredths";
    private static final String QUANTITY_FRACTION_TAG = "QuantityFractionHundredths";
    private static final String SLOT_PARALLEL_FRACTION_TAG = "SlotParallelFractionHundredths";
    private static final String SLOT_QUANTITY_FRACTION_TAG = "SlotQuantityFractionHundredths";
    private static final String EXTERNAL_ACCELERATION_STATES_TAG = "ExternalTickAccelerationStates";
    private static final String EQUIPMENT_DISMANTLING_TAG = "EquipmentDismantling";
    private static final String SLOT_ENABLED_TAG = "SlotEnabled";
    private static final String LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG =
            "LastEnergyConsumptionGameTime";
    private static final int DRAWS_PER_PARALLEL = 8;
    public static final int FLUID_PER_WORK_CYCLE_MB = 25;

    /** Capacity of the fluid input chamber's single tank. */
    public static final int FLUID_TANK_CAPACITY_MB = 16_000;

    /**
     * A cycle must span the complete natural observation window used for acceleration accounting.
     */
    public static final int MINIMUM_PROCESSING_TIME =
            MinerAccelerationController.MINIMUM_NATURAL_TICKS;

    /** Reported as the cycle length when no slot is running; also the natural-window floor. */
    public static final int DEFAULT_PROCESSING_TIME = MINIMUM_PROCESSING_TIME;

    private final ItemStackHandler itemHandler;
    private final IItemHandler insertOnlyItemHandler;
    private final MinerAnalysisController analysisController;
    private final MinerUpgradeController upgradeController;
    private final MinerAccelerationController accelerationController;
    private final MinerOutputController outputController = new MinerOutputController();
    private final boolean[] slotEnabled;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private int redstoneThreshold = 8;
    private boolean structureComplete;

    /** The chamber of each kind currently installed, or null when the ring has none. */
    @Nullable private FluidInputChamberBlockEntity fluidChamber;

    @Nullable private EnergyInputChamberBlockEntity energyChamber;
    @Nullable private ItemOutputChamberBlockEntity itemChamber;

    /** Natural game time for which this machine has already paid its energy cost. */
    private long lastEnergyConsumptionGameTime = Long.MIN_VALUE;

    protected BaseMinerBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
        this.itemHandler = createItemHandler();
        this.insertOnlyItemHandler = new InsertOnlyItemHandler(itemHandler);
        this.upgradeController = new MinerUpgradeController(position);
        this.analysisController =
                new MinerAnalysisController(
                        itemHandler, this::getEffectiveMachineLuck, this::isRemoved);
        int slotCount = itemHandler.getSlots();
        this.accelerationController = new MinerAccelerationController(slotCount);
        this.slotEnabled = new boolean[slotCount];
        java.util.Arrays.fill(this.slotEnabled, true);
    }

    public boolean requiresFluidInput() {
        MinerScriptConfig c = scriptConfig();
        return c != null && c.requiresFluid() != null ? c.requiresFluid() : getMinerTier() >= 1;
    }

    public Fluid getRequiredFluid() {
        return ModFluids.forMinerTier(getMinerTier());
    }

    public final int getMinerTier() {
        return getBlockState().getBlock() instanceof BaseMinerBlock miner ? miner.minerTier() : 1;
    }

    protected abstract int getSlotCount();

    public final int getSlotCountForScript() {
        return getSlotCount();
    }

    protected abstract String getTranslationName();

    protected abstract int getBaseParallel();

    protected abstract float getMachineLuck();

    protected abstract double getMachineEfficiency();

    protected abstract double getQuantityReference();

    protected abstract int getEnergyCapacity();

    private MinerScriptConfig scriptConfig() {
        return MinerScriptConfigService.get(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                        getBlockState().getBlock()));
    }

    public abstract int getEnergyConsumption();

    public int getProcessingTime() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotProcessingTime(slot) : DEFAULT_PROCESSING_TIME;
    }

    public int getDrawParallel() {
        int slot = firstActiveSlot();
        return slot >= 0
                ? getSlotDrawParallel(slot)
                : upgradeController.totalParallel(getBaseParallel(), 100);
    }

    public int getEffectiveBaseParallel() {
        return upgradeController.upgradedBaseParallel(getBaseParallelCount());
    }

    public int getExtraEfficiencyParallel() {
        int slot = firstActiveSlot();
        return slot >= 0
                ? upgradeController.extraEfficiencyParallel(
                        getBaseParallel(), accelerationController.currentParallelHundredths(slot))
                : 0;
    }

    public int getExternalAccelerationParallelHundredths() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotExternalAccelerationParallelHundredths(slot) : 0;
    }

    public long getCurrentExternalAccelerationMachineTicks() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotCurrentExternalAccelerationMachineTicks(slot) : 0;
    }

    public int getSlotExternalAccelerationParallelHundredths(int slot) {
        return validSlot(slot) ? accelerationController.currentExtraParallel(slot) : 0;
    }

    public long getSlotExternalEquivalentAccelerationTicks(int slot) {
        return validSlot(slot) ? accelerationController.equivalentTicks(slot) : 0;
    }

    public double getSlotCurrentCycleExternalEquivalentAcceleration(int slot) {
        return validSlot(slot) ? accelerationController.cycleEquivalent(slot) : 0.0D;
    }

    public long getExternalEquivalentAccelerationTicks() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotExternalEquivalentAccelerationTicks(slot) : 0L;
    }

    public long getSlotCurrentExternalAccelerationMachineTicks(int slot) {
        return validSlot(slot) ? accelerationController.currentActualTicks(slot) : 0;
    }

    public long getSlotCurrentNaturalTicks(int slot) {
        return validSlot(slot) ? accelerationController.currentNaturalTicks(slot) : 0;
    }

    /** Logical progress in the current machine cycle, independent of acceleration calls. */
    public long getSlotLogicalProgress(int slot) {
        return validSlot(slot) ? accelerationController.currentNaturalTicks(slot) : 0;
    }

    public boolean isSlotWaitingForNaturalWindow(int slot) {
        return validSlot(slot) && accelerationController.waitingForNaturalWindow(slot);
    }

    public long getSlotPreviousExternalAccelerationMachineTicks(int slot) {
        return validSlot(slot) ? accelerationController.previousActualTicks(slot) : 0;
    }

    public int getSlotPreviousExternalAccelerationParallelHundredths(int slot) {
        return validSlot(slot) ? accelerationController.previousExtraParallel(slot) : 0;
    }

    public int getBaseParallelCount() {
        MinerScriptConfig c = scriptConfig();
        return c != null && c.baseParallel() != null ? c.baseParallel() : getBaseParallel();
    }

    /** Energy consumed by one working marker slot per machine tick. */
    public int getEffectiveThreadEnergyConsumption() {
        return Math.max(
                1,
                (int)
                        Math.ceil(
                                (scriptConfig() != null
                                                        && scriptConfig().energyConsumption()
                                                                != null
                                                ? scriptConfig().energyConsumption()
                                                : getEnergyConsumption())
                                        * upgradeController.state().energyConsumptionMultiplier()));
    }

    /** Total energy consumed per machine tick by all currently working slots. */
    public int getEffectiveEnergyConsumption() {
        long total = (long) getEffectiveThreadEnergyConsumption() * getWorkingThreadCount();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int getWorkingThreadCount() {
        int working = 0;
        for (int slot = 0; slot < accelerationController.slotCount(); slot++) {
            if (slotEnabled[slot] && accelerationController.processingTime(slot) > 0) {
                working++;
            }
        }
        return working;
    }

    public double getEffectiveMachineEfficiency() {
        return getBaseMachineEfficiency() * upgradeController.state().efficiencyMultiplier();
    }

    public double getBaseMachineEfficiency() {
        MinerScriptConfig c = scriptConfig();
        return c != null && c.efficiency() != null ? c.efficiency() : getMachineEfficiency();
    }

    public float getBaseMachineLuck() {
        MinerScriptConfig c = scriptConfig();
        return c != null && c.luck() != null ? c.luck() : getMachineLuck();
    }

    public int getBaseEnergyCapacity() {
        MinerScriptConfig c = scriptConfig();
        return c != null && c.energyCapacity() != null ? c.energyCapacity() : getEnergyCapacity();
    }

    /**
     * The capacity the energy input chamber must mirror: the tier's base capacity scaled by the
     * energy upgrades. Zero-sum when the structure is incomplete, because the upgrades are cleared
     * then — which is exactly why the chamber caches the last value it read from a complete
     * machine.
     */
    public int getEffectiveEnergyCapacity() {
        long capacity =
                Math.round(
                        getBaseEnergyCapacity()
                                * upgradeController.state().energyCapacityMultiplier());
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, capacity));
    }

    public float getEffectiveMachineLuck() {
        return upgradeController.effectiveLuck(getBaseMachineLuck());
    }

    public double getEfficiencyUpgradePercent() {
        return (upgradeController.state().efficiencyMultiplier() - 1.0D) * 100.0D;
    }

    public double getEnergyCapacityUpgradePercent() {
        return (upgradeController.state().energyCapacityMultiplier() - 1.0D) * 100.0D;
    }

    public double getEnergyConsumptionReductionPercent() {
        return (1.0D - upgradeController.state().energyConsumptionMultiplier()) * 100.0D;
    }

    public double getParallelUpgradePercent() {
        return upgradeController.state().parallelMultiplierHundredths() - 100.0D;
    }

    public double getLuckUpgradePercent() {
        return upgradeController.state().luckIncreasePercent();
    }

    public int getUpgradeCount(StructureMinerUpgradeBlock.Type type) {
        return switch (type) {
            case EFFICIENCY -> upgradeController.state().efficiencyUpgradeCount();
            case ENERGY -> upgradeController.state().energyUpgradeCount();
            case PARALLEL -> upgradeController.state().parallelUpgradeCount();
            case LUCK -> upgradeController.state().luckUpgradeCount();
            case AGGREGATE -> upgradeController.state().aggregateUpgradeCount();
            case NONE -> 0;
        };
    }

    public int getUpgradeCount(StructureMinerUpgradeBlock.Type type, int tier) {
        if (tier < 1 || tier > 6 || type == StructureMinerUpgradeBlock.Type.NONE) {
            return 0;
        }
        return upgradeController.state().countFor(type, tier);
    }

    public int getTotalUpgradeCount() {
        MinerUpgradeController.UpgradeState upgrades = upgradeController.state();
        return upgrades.efficiencyUpgradeCount()
                + upgrades.energyUpgradeCount()
                + upgrades.parallelUpgradeCount()
                + upgrades.luckUpgradeCount()
                + upgrades.aggregateUpgradeCount();
    }

    public int getAccumulatedParallelHundredths() {
        int slot = firstActiveSlot();
        return slot >= 0 ? accelerationController.parallelFraction(slot) : 0;
    }

    public int getAdditionalItemCount() {
        return Math.max(0, getPendingOutputCount());
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public int getRedstoneThreshold() {
        return redstoneThreshold;
    }

    public void cycleRedstoneMode() {
        redstoneMode =
                RedstoneMode.values()[(redstoneMode.ordinal() + 1) % RedstoneMode.values().length];
        setChanged();
    }

    public void toggleRedstoneControl() {
        redstoneMode =
                redstoneMode == RedstoneMode.SIGNAL ? RedstoneMode.ALWAYS : RedstoneMode.SIGNAL;
        redstoneThreshold = 8;
        setChanged();
    }

    public boolean supportsEquipmentDismantling() {
        return getBlockState().getBlock() instanceof BaseMinerBlock miner && miner.minerTier() >= 3;
    }

    public boolean isEquipmentDismantlingEnabled() {
        return supportsEquipmentDismantling() && outputController.equipmentDismantling();
    }

    public void toggleEquipmentDismantling() {
        if (supportsEquipmentDismantling()) {
            outputController.toggleEquipmentDismantling();
            setChanged();
        }
    }

    /** The raw slot handler. Only the menu and the miner's own controllers may hold this one. */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Insert-only view of the marker slots, used as the block capability.
     *
     * <p>Every slot holds a marker the miner consumes, so an external handler must be able to load
     * them and never to take them: an extraction would feed the machine's own inputs to whatever
     * pipe asked. Everything outside the machine reads through this view, while the menu keeps the
     * raw handler so the player can still take a marker back out.
     */
    public IItemHandler insertOnlyItemHandler() {
        return insertOnlyItemHandler;
    }

    // --- chamber-backed readouts ---------------------------------------------

    /**
     * The installed fluid chamber's tank, or null when the ring has none. The controller has no
     * tank of its own any more, so every fluid readout has to come from here.
     */
    @Nullable
    private FluidTank fluidTank() {
        return fluidChamber == null ? null : fluidChamber.tank();
    }

    @Nullable
    private EnergyContainer energyContainer() {
        return energyChamber == null ? null : energyChamber.energy();
    }

    public int getFluidAmount() {
        FluidTank tank = fluidTank();
        return tank == null ? 0 : tank.getFluidAmount();
    }

    public int getFluidCapacity() {
        FluidTank tank = fluidTank();
        return tank == null ? 0 : tank.getCapacity();
    }

    public Fluid getStoredFluid() {
        FluidTank tank = fluidTank();
        return tank == null || tank.isEmpty() ? Fluids.EMPTY : tank.getFluid().getFluid();
    }

    public boolean hasStoredFluid() {
        FluidTank tank = fluidTank();
        return tank != null && !tank.isEmpty();
    }

    public boolean isFluidAutoPullEnabled() {
        return fluidChamber != null && fluidChamber.isAutoPullEnabled();
    }

    /** Sets the installed fluid chamber's auto-pull; a no-op when the ring has no fluid chamber. */
    public void setFluidAutoPull(boolean enabled) {
        if (fluidChamber != null) fluidChamber.setAutoPull(enabled);
    }

    /** The energy chamber's buffer, or 0 when the ring has no energy chamber. */
    public int getEnergyStored() {
        EnergyContainer energy = energyContainer();
        return energy == null ? 0 : energy.getEnergyStored();
    }

    /**
     * The energy chamber's current capacity, or 0 when the ring has no energy chamber. Named apart
     * from the abstract {@link #getEnergyCapacity()}, which is the tier's own base capacity: those
     * are different numbers, and conflating them would let a tier answer for a machine it is not
     * part of.
     */
    public int getStoredEnergyCapacity() {
        EnergyContainer energy = energyContainer();
        return energy == null ? 0 : energy.getMaxEnergyStored();
    }

    public boolean hasFluidChamber() {
        return fluidChamber != null;
    }

    public boolean hasEnergyChamber() {
        return energyChamber != null;
    }

    public boolean hasItemChamber() {
        return itemChamber != null;
    }

    /**
     * Drops the markers loaded into the machine and any reward that finished generating but has not
     * reached an output target yet, so breaking the machine never destroys player property.
     *
     * <p>Called from {@code BaseMinerBlock#onRemove}, which only runs server side ({@code
     * LevelChunk#setBlockState} guards the hook with {@code !level.isClientSide}) and still has the
     * block entity registered at that point. Each slot is emptied as it is dropped so a repeated
     * removal cannot duplicate the contents. The chambers' tanks are intentionally left alone, the
     * same stance {@code StructureReactorBlockEntity} takes for its own tanks.
     */
    public void dropContents() {
        if (level == null) return;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            Containers.dropItemStack(
                    level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
        for (ItemStack stack : outputController.pendingItems()) {
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(
                    level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
        outputController.setPending(List.of());
    }

    public int getProgress() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotProgress(slot) : 0;
    }

    public int getProgressPercent() {
        return (int) Math.min(100L, (long) getProgress() * 100L / getProcessingTime());
    }

    public int getSlotProgress(int slot) {
        return accelerationController.progress(slot);
    }

    public int getSlotProcessingTime(int slot) {
        return validSlot(slot) && slotEnabled[slot]
                ? accelerationController.processingTime(slot)
                : 0;
    }

    public boolean isSlotEnabled(int slot) {
        return validSlot(slot) && slotEnabled[slot];
    }

    public void toggleSlotEnabled(int slot) {
        if (!validSlot(slot)) return;
        slotEnabled[slot] = !slotEnabled[slot];
        setChanged();
    }

    public int getSlotDrawParallel(int slot) {
        if (!validSlot(slot)
                || !slotEnabled[slot]
                || accelerationController.processingTime(slot) <= 0) {
            return 0;
        }
        return (int)
                Math.max(
                        1L,
                        Math.min(Integer.MAX_VALUE, slotDisplayParallelHundredths(slot) / 100L));
    }

    public StructureMinerAnalysisSnapshot getMarkerAnalysisSnapshot(int slot) {
        if (!(level instanceof ServerLevel serverLevel) || !validSlot(slot)) {
            return StructureMinerAnalysisSnapshot.EMPTY;
        }
        return analysisController.snapshot(
                slot,
                serverLevel,
                slotAverageParallelHundredths(slot) / 100.0D,
                DRAWS_PER_PARALLEL,
                getQuantityReference(),
                isEquipmentDismantlingEnabled(),
                outputController.disabledItems(slot));
    }

    /**
     * Explicitly refreshes marker analysis and processing plans; queries never invoke this work.
     */
    public void refreshMarkerAnalysis() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlans();
    }

    public void toggleExpectedItem(int slot, ResourceLocation itemId) {
        if (!validSlot(slot)) return;
        outputController.toggleDisabledItem(slot, itemId);
        setChanged();
    }

    /**
     * Replaces one thread's disabled-output set wholesale, for the select-all and deselect-all
     * gestures.
     *
     * <p>Idempotent where {@link #toggleExpectedItem} is not: it states the desired set instead of
     * inverting the current one, so a repeated call is a no-op rather than a flip back.
     *
     * @return true when the set changed, so the caller can skip re-running the analysis.
     */
    public boolean setDisabledExpectedItems(int slot, Collection<ResourceLocation> itemIds) {
        if (!validSlot(slot)) return false;
        if (!outputController.replaceDisabledItems(slot, itemIds)) return false;
        setChanged();
        return true;
    }

    public boolean isExpectedItemDisabled(int slot, ResourceLocation itemId) {
        return validSlot(slot) && outputController.disabled(slot, itemId);
    }

    public boolean isOutputBlocked() {
        return outputController.blocked();
    }

    public int getPendingOutputCount() {
        return outputController.pendingCount();
    }

    public List<ResourceLocation> getMarkedStructures() {
        List<ResourceLocation> structures = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            StructMarkerItem.getMarkerInfo(itemHandler.getStackInSlot(slot))
                    .map(markerInfo -> markerInfo.structure().id())
                    .ifPresent(structures::add);
        }
        return structures.stream().distinct().toList();
    }

    /** Runs the miner contract: state, chambers, eligibility, resources, progress, output. */
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        updateMachineState(serverLevel);
        locateChambers(serverLevel);
        tickChambers();
        if (!canRunThisTick(serverLevel)) return;
        if (!consumeWorkResources(serverLevel)) return;
        List<MarkerAnalysis> completedSlots = advanceWork(serverLevel);
        List<CompletedMarker> completedMarkers = completeWork(serverLevel, completedSlots);
        outputCompletedWork(serverLevel, completedMarkers);
        setChanged();
    }

    private void updateMachineState(ServerLevel serverLevel) {
        boolean complete = isStructureComplete(serverLevel);
        if (complete != structureComplete) {
            structureComplete = complete;
            setChanged();
        }
        if (complete) {
            upgradeController.refresh(serverLevel);
        } else {
            upgradeController.clear();
        }
    }

    /**
     * Walks the casing cells in their fixed order and keeps the first chamber of each kind.
     * Chambers can be swapped without ever changing the structure's completeness — a chamber is a
     * valid casing — so this cannot be cached against a completeness transition; it is re-read
     * every tick.
     */
    private void locateChambers(ServerLevel serverLevel) {
        FluidInputChamberBlockEntity fluid = null;
        EnergyInputChamberBlockEntity energy = null;
        ItemOutputChamberBlockEntity item = null;
        for (BlockPos position :
                StructureMinerMultiblock.casingSearchOrder(serverLevel, worldPosition)) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(position);
            if (fluid == null && blockEntity instanceof FluidInputChamberBlockEntity candidate) {
                fluid = candidate;
            } else if (energy == null
                    && blockEntity instanceof EnergyInputChamberBlockEntity candidate) {
                energy = candidate;
            } else if (item == null
                    && blockEntity instanceof ItemOutputChamberBlockEntity candidate) {
                item = candidate;
            }
            if (fluid != null && energy != null && item != null) break;
        }
        fluidChamber = fluid;
        energyChamber = energy;
        itemChamber = item;
        if (fluid != null) fluid.bindMiner(this);
        if (energy != null) energy.bindMiner(this);
    }

    private void tickChambers() {
        if (fluidChamber != null) fluidChamber.chamberTick(this);
        if (energyChamber != null) energyChamber.chamberTick(this);
        if (itemChamber != null) itemChamber.chamberTick(this);
    }

    private boolean canRunThisTick(ServerLevel serverLevel) {
        if (!structureComplete) return false;
        if (!redstoneMode.allows(level.getBestNeighborSignal(worldPosition), redstoneThreshold)) {
            return false;
        }
        if (outputController.blocked()) {
            retryPendingOutput(serverLevel);
            return false;
        }
        if (!hasValidMarker()) return false;
        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlans();
        return !MinerIntegrationHooks.postWork(this);
    }

    private boolean consumeWorkResources(ServerLevel serverLevel) {
        int startingCycles = countStartingCycles();
        long gameTime = serverLevel.getGameTime();
        int energyConsumption = getEffectiveEnergyConsumption();
        if (gameTime != lastEnergyConsumptionGameTime
                && (energyConsumption <= 0 || !canConsumeEnergy(energyConsumption))) {
            return false;
        }
        if (!consumeCycleFluid(startingCycles)) return false;
        if (gameTime != lastEnergyConsumptionGameTime) {
            consumeEnergy(energyConsumption);
            lastEnergyConsumptionGameTime = gameTime;
        }
        return true;
    }

    /** Energy is paid out of the energy chamber; with none installed the cycle cannot start. */
    private boolean canConsumeEnergy(int amount) {
        EnergyContainer energy = energyContainer();
        return energy != null && energy.canConsume(amount);
    }

    private void consumeEnergy(int amount) {
        EnergyContainer energy = energyContainer();
        if (energy != null) energy.consume(amount);
    }

    /**
     * Advances only per-slot progress and acceleration state; completion work follows this phase.
     */
    private List<MarkerAnalysis> advanceWork(ServerLevel serverLevel) {
        List<MarkerAnalysis> completedSlots = new ArrayList<>();
        for (MinerAccelerationController.CompletedSlot completed :
                accelerationController.advance(serverLevel.getGameTime(), slotEnabled)) {
            MarkerAnalysis cachedLoot = analysisController.entryForSlot(completed.slot());
            if (cachedLoot != null) completedSlots.add(cachedLoot);
        }
        return completedSlots;
    }

    /** Completes all reached slots before any loot is generated or output is attempted. */
    private List<CompletedMarker> completeWork(
            ServerLevel serverLevel, List<MarkerAnalysis> completedSlots) {
        List<CompletedMarker> completedMarkers = new ArrayList<>();
        for (MarkerAnalysis cachedLoot : completedSlots) {
            int slot = cachedLoot.slot();
            int parallel = drawParallelForCycle(slot);
            ResourceLocation markerId =
                    StructMarkerItem.getMarkerInfo(cachedLoot.marker())
                            .map(info -> info.structure().id())
                            .orElse(null);
            if (!MinerIntegrationHooks.postCycle(this, serverLevel, slot, markerId, parallel)) {
                completedMarkers.add(new CompletedMarker(cachedLoot, parallel));
            }
        }
        return completedMarkers;
    }

    private void outputCompletedWork(
            ServerLevel serverLevel, List<CompletedMarker> completedMarkers) {
        if (completedMarkers.isEmpty()) return;
        drawMarkerLoot(serverLevel.getServer(), completedMarkers);
    }

    private int countStartingCycles() {
        if (!requiresFluidInput()) return 0;
        int startingCycles = 0;
        for (int slot = 0; slot < accelerationController.slotCount(); slot++) {
            if (slotEnabled[slot]
                    && accelerationController.processingTime(slot) > 0
                    && accelerationController.currentActualTicks(slot) == 0L) {
                startingCycles++;
            }
        }
        return startingCycles;
    }

    /** Fluid is paid out of the fluid chamber; with none installed the cycle cannot start. */
    private boolean consumeCycleFluid(int cycleCount) {
        if (cycleCount <= 0 || !requiresFluidInput()) return true;
        long requested = (long) cycleCount * FLUID_PER_WORK_CYCLE_MB;
        if (requested > Integer.MAX_VALUE) return false;
        Fluid required = getRequiredFluid();
        if (required == null) return false;
        FluidTank tank = fluidTank();
        if (tank == null) return false;
        FluidStack request = new FluidStack(required, (int) requested);
        FluidStack simulated = tank.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() < requested) return false;
        FluidStack drained = tank.drain(request, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount() == requested;
    }

    public boolean isStructureComplete() {
        return structureComplete;
    }

    private boolean isStructureComplete(ServerLevel serverLevel) {
        return StructureMinerMultiblock.isComplete(serverLevel, worldPosition);
    }

    private boolean hasValidMarker() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (ModItems.isMarker(marker) && StructMarkerItem.getMarkerInfo(marker).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private int drawParallelForCycle(int slot) {
        long scaledParallel = slotAverageParallelHundredths(slot);
        return accelerationController.drawParallel(slot, scaledParallel);
    }

    private long slotAverageParallelHundredths(int slot) {
        // Same formula as ProcessingMath.totalParallel, in hundredths of parallel.
        long machineParallelHundredths =
                (long) getBaseParallelCount()
                                * upgradeController.state().parallelMultiplierHundredths()
                        + (accelerationController.currentParallelHundredths(slot) - 100L);
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths + accelerationController.previousExtraParallel(slot));
    }

    private long slotDisplayParallelHundredths(int slot) {
        // Same formula as ProcessingMath.totalParallel, in hundredths of parallel.
        long machineParallelHundredths =
                (long) getBaseParallelCount()
                                * upgradeController.state().parallelMultiplierHundredths()
                        + (accelerationController.currentParallelHundredths(slot) - 100L);
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths + accelerationController.currentExtraParallel(slot));
    }

    private void drawMarkerLoot(MinecraftServer server, List<CompletedMarker> completedMarkers) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }
        outputController.setPending(
                outputController.emit(
                        this,
                        server,
                        outputLevel,
                        completedMarkers,
                        getMinerTier(),
                        slot ->
                                accelerationController.drawsForQuantity(
                                        slot,
                                        completedMarkers.stream()
                                                .filter(value -> value.loot().slot() == slot)
                                                .findFirst()
                                                .orElseThrow()
                                                .parallel(),
                                        analysisController.entryForSlot(slot).quantity(),
                                        getQuantityReference())));
        setChanged();
        // The first group leaves in the same tick the loot was generated; the rest drains one group
        // per tick from retryPendingOutput.
        retryPendingOutput(outputLevel);
    }

    private void refreshMarkerLootCache(MinecraftServer server) {
        long gameTime = level == null ? 0L : level.getGameTime();
        for (int slot : analysisController.refresh(server, gameTime)) {
            resetSlotState(slot);
        }
    }

    /** Cache freshness is independent from the execution outcome of the current task. */
    public String markerAnalysisCacheStatus(int slot) {
        return analysisController.cacheStatus(slot);
    }

    public AnalysisLifecycle.TaskStatus markerAnalysisTaskStatus(int slot) {
        return analysisController.taskStatus(slot);
    }

    private void updateProcessingPlans() {
        double efficiency =
                getBaseMachineEfficiency() * upgradeController.state().efficiencyMultiplier();
        MinerScriptConfig config = scriptConfig();
        int configuredProcessingTime =
                config != null && config.processingTime() != null ? config.processingTime() : 0;
        analysisController.refreshPlans(
                efficiency,
                configuredProcessingTime,
                MINIMUM_PROCESSING_TIME,
                getBaseParallelCount(),
                slotEnabled,
                accelerationController);
    }

    /** Hands the pending queue to the item output chamber; with none installed it stays blocked. */
    private void retryPendingOutput(ServerLevel outputLevel) {
        outputController.setPending(
                outputController.retry(outputLevel, worldPosition, itemChamber));
        setChanged();
    }

    private int firstActiveSlot() {
        for (int slot = 0; slot < accelerationController.slotCount(); slot++) {
            if (accelerationController.processingTime(slot) > 0) {
                return slot;
            }
        }
        return -1;
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < accelerationController.slotCount();
    }

    private void resetSlotState(int slot) {
        accelerationController.reset(slot);
    }

    record CompletedMarker(MarkerAnalysis loot, int parallel) {}

    private ItemStackHandler createItemHandler() {
        int slotCount = getSlotCount();
        if (slotCount <= 0) {
            throw new IllegalStateException("Structure miner slot count must be positive");
        }

        return new ItemStackHandler(slotCount) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return ModItems.isMarker(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (analysisController != null) analysisController.invalidateIfInputsChanged();
                ItemStack marker = getStackInSlot(slot);
                if (!ModItems.isMarker(marker)
                        || StructMarkerItem.getMarkerInfo(marker).isEmpty()) {
                    resetSlotState(slot);
                }
                setChanged();
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_TAG, itemHandler.serializeNBT(registries));
        tag.putLong(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG, lastEnergyConsumptionGameTime);
        tag.putInt(PROGRESS_TAG, getProgress());
        tag.putIntArray(SLOT_PROGRESS_TAG, accelerationController.progressValues());
        tag.putInt(PARALLEL_FRACTION_TAG, getAccumulatedParallelHundredths());
        tag.putIntArray(SLOT_PARALLEL_FRACTION_TAG, new int[] {getAccumulatedParallelHundredths()});
        int firstActiveSlot = firstActiveSlot();
        tag.putInt(
                QUANTITY_FRACTION_TAG,
                firstActiveSlot >= 0
                        ? accelerationController.quantityFraction(firstActiveSlot)
                        : 0);
        accelerationController.save(
                tag,
                SLOT_PROGRESS_TAG,
                SLOT_PARALLEL_FRACTION_TAG,
                SLOT_QUANTITY_FRACTION_TAG,
                EXTERNAL_ACCELERATION_STATES_TAG);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("RedstoneThreshold", redstoneThreshold);
        int[] enabledSlots = new int[slotEnabled.length];
        for (int index = 0; index < slotEnabled.length; index++) {
            enabledSlots[index] = slotEnabled[index] ? 1 : 0;
        }
        tag.putIntArray(SLOT_ENABLED_TAG, enabledSlots);
        outputController.save(tag, EQUIPMENT_DISMANTLING_TAG, PENDING_OUTPUT_TAG, registries);
        MinecraftServer analysisServer = level == null ? null : level.getServer();
        analysisController.saveAnalysis(
                tag,
                analysisServer == null ? null : AnalysisDataFingerprint.current(analysisServer),
                registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound(INVENTORY_TAG);
            inventoryTag.putInt("Size", itemHandler.getSlots());
            itemHandler.deserializeNBT(registries, inventoryTag);
        }
        // The fluid and energy tags an older save carries are deliberately ignored: those buffers
        // live in the chambers now, and the chambers have their own data.
        lastEnergyConsumptionGameTime =
                tag.contains(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG, Tag.TAG_LONG)
                        ? tag.getLong(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG)
                        : Long.MIN_VALUE;
        accelerationController.load(
                tag,
                SLOT_PROGRESS_TAG,
                PROGRESS_TAG,
                SLOT_PARALLEL_FRACTION_TAG,
                PARALLEL_FRACTION_TAG,
                SLOT_QUANTITY_FRACTION_TAG,
                QUANTITY_FRACTION_TAG,
                EXTERNAL_ACCELERATION_STATES_TAG);
        redstoneMode =
                RedstoneMode.values()[
                        Math.max(
                                0,
                                Math.min(
                                        RedstoneMode.values().length - 1,
                                        tag.getInt("RedstoneMode")))];
        redstoneThreshold =
                Math.max(
                        0,
                        Math.min(
                                15,
                                tag.contains("RedstoneThreshold", Tag.TAG_INT)
                                        ? tag.getInt("RedstoneThreshold")
                                        : 8));
        outputController.load(
                tag,
                EQUIPMENT_DISMANTLING_TAG,
                PENDING_OUTPUT_TAG,
                registries,
                itemHandler.getSlots());
        analysisController.loadAnalysis(tag, registries);
        java.util.Arrays.fill(slotEnabled, true);
        if (tag.contains(SLOT_ENABLED_TAG, Tag.TAG_INT_ARRAY)) {
            int[] enabledSlots = tag.getIntArray(SLOT_ENABLED_TAG);
            for (int index = 0;
                    index < Math.min(slotEnabled.length, enabledSlots.length);
                    index++) {
                slotEnabled[index] = enabledSlots[index] != 0;
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return TranslateHelper.translate(TranslateHelper.container(getTranslationName()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new StructureMinerMenu(containerId, playerInventory, this);
    }

    public enum RedstoneMode {
        ALWAYS {
            @Override
            boolean allows(int signal, int threshold) {
                return true;
            }
        },
        SIGNAL {
            @Override
            boolean allows(int signal, int threshold) {
                return signal >= threshold;
            }
        },
        NO_SIGNAL {
            @Override
            boolean allows(int signal, int threshold) {
                return signal < threshold;
            }
        },
        NEVER {
            @Override
            boolean allows(int signal, int threshold) {
                return false;
            }
        };

        abstract boolean allows(int signal, int threshold);
    }
}
