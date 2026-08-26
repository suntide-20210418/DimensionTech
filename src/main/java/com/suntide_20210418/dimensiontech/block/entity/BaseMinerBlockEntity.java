package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.utils.FullDurabilityLoot;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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

    private final ItemStackHandler itemHandler;
    private final MinerEnergyStorage energyStorage;
    private LazyOptional<IItemHandler> itemHandlerCapability;
    private LazyOptional<IEnergyStorage> energyCapability;
    private LazyOptional<IFluidHandler> fluidCapability;
    private LazyOptional<IFluidHandler> fluidOutputCapability;
    private final FluidTank fluidTank;
    private CompoundTag analyzedMarkerSnapshot;
    private List<CachedMarkerLoot> cachedMarkerLoot = List.of();
    private List<ItemStack> pendingOutput = new ArrayList<>();
    private final MythicMinerSlotProgress slotProgress;
    private final int[] slotProcessingTimes;
    private final int[] slotParallelHundredths;
    private final int[] slotParallelFractionHundredths;
    private final int[] slotQuantityFractionHundredths;
    private final boolean[] slotEnabled;
    private final MythicMinerExternalTickAcceleration[] externalTickAcceleration;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private int redstoneThreshold = 8;
    private OutputState configuredOutputState = OutputState.ITEM_HANDLER;
    private int outputFaceMask = (1 << Direction.values().length) - 1;
    private final FluidFaceMode[] fluidFaceModes = new FluidFaceMode[Direction.values().length];
    private boolean autoExtractFluid;
    private boolean equipmentDismantling;
    private final Set<ResourceLocation> disabledExpectedItems = new HashSet<>();
    private boolean structureComplete;
    private UpgradeBonuses upgradeBonuses = UpgradeBonuses.NONE;

    /** Natural game time for which this machine has already paid its energy cost. */
    private long lastEnergyConsumptionGameTime = Long.MIN_VALUE;

    protected BaseMinerBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
        this.itemHandler = createItemHandler();
        int slotCount = itemHandler.getSlots();
        this.slotProgress = new MythicMinerSlotProgress(slotCount);
        this.slotProcessingTimes = new int[slotCount];
        this.slotParallelHundredths = new int[slotCount];
        this.slotParallelFractionHundredths = new int[slotCount];
        this.slotQuantityFractionHundredths = new int[slotCount];
        this.externalTickAcceleration = new MythicMinerExternalTickAcceleration[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            this.externalTickAcceleration[slot] = new MythicMinerExternalTickAcceleration();
        }
        this.slotEnabled = new boolean[slotCount];
        java.util.Arrays.fill(this.slotEnabled, true);
        java.util.Arrays.fill(this.fluidFaceModes, FluidFaceMode.INPUT);
        this.energyStorage = new MinerEnergyStorage(getEnergyCapacity());
        this.fluidTank = new FluidTank(1000) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                Fluid required = ModFluids.forMinerTier(getMinerTier());
                return requiresFluidInput()
                        && required != null
                        && stack.getFluid().getFluidType() == required.getFluidType();
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.itemHandlerCapability = LazyOptional.of(() -> itemHandler);
        this.energyCapability = LazyOptional.of(() -> energyStorage);
        this.fluidCapability = LazyOptional.of(this::createFluidInputHandler);
        this.fluidOutputCapability = LazyOptional.of(this::createFluidOutputHandler);
    }

    public boolean requiresFluidInput() {
        return getMinerTier() >= 2;
    }

    public Fluid getRequiredFluid() {
        return ModFluids.forMinerTier(getMinerTier());
    }

    private int getMinerTier() {
        return getBlockState().getBlock() instanceof BaseMinerBlock miner ? miner.minerTier() : 1;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public FluidFaceMode getFluidFaceMode(Direction worldDirection) {
        return fluidFaceModes[worldDirection.ordinal()];
    }

    public void cycleFluidFace(Direction logicalDirection) {
        Direction worldDirection = toWorldDirection(logicalDirection);
        fluidFaceModes[worldDirection.ordinal()] =
                FluidFaceMode.values()[(fluidFaceModes[worldDirection.ordinal()].ordinal() + 1)
                        % FluidFaceMode.values().length];
        setChanged();
    }

    public int getFluidFaceModesPacked() {
        int packed = 0;
        for (Direction direction : Direction.values()) {
            packed |= fluidFaceModes[direction.ordinal()].ordinal() << (direction.ordinal() * 2);
        }
        return packed;
    }

    public boolean isAutoExtractFluidEnabled() {
        return requiresFluidInput() && autoExtractFluid;
    }

    public void toggleAutoExtractFluid() {
        if (requiresFluidInput()) {
            autoExtractFluid = !autoExtractFluid;
            setChanged();
        }
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
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotProcessingTime(slot) : 400;
    }

    public int getDrawParallel() {
        int slot = firstActiveSlot();
        return slot >= 0
                ? getSlotDrawParallel(slot)
                : MythicMinerUpgradeMath.totalParallel(
                        getBaseParallel(), 100, upgradeBonuses.parallelMultiplierHundredths());
    }

    public int getEffectiveBaseParallel() {
        return MythicMinerUpgradeMath.upgradedBaseParallel(
                getBaseParallel(), upgradeBonuses.parallelMultiplierHundredths());
    }

    public int getExtraEfficiencyParallel() {
        int slot = firstActiveSlot();
        return slot >= 0
                ? MythicMinerUpgradeMath.extraEfficiencyParallel(
                        getBaseParallel(),
                        slotParallelHundredths[slot],
                        upgradeBonuses.parallelMultiplierHundredths())
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
        return validSlot(slot)
                ? externalTickAcceleration[slot].currentExtraParallelHundredths()
                : 0;
    }

    public long getSlotExternalEquivalentAccelerationTicks(int slot) {
        return validSlot(slot)
                ? externalTickAcceleration[slot].currentEquivalentAccelerationTicks()
                : 0;
    }

    public double getSlotCurrentCycleExternalEquivalentAcceleration(int slot) {
        return slot >= 0 && slot < externalTickAcceleration.length
                ? externalTickAcceleration[slot].currentCycleEquivalentAcceleration()
                : 0.0D;
    }

    public long getExternalEquivalentAccelerationTicks() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotExternalEquivalentAccelerationTicks(slot) : 0L;
    }

    public long getSlotCurrentExternalAccelerationMachineTicks(int slot) {
        return validSlot(slot) ? externalTickAcceleration[slot].currentActualTicks() : 0;
    }

    public long getSlotCurrentNaturalTicks(int slot) {
        return validSlot(slot) ? externalTickAcceleration[slot].currentNaturalTicks() : 0;
    }

    /** Logical progress in the current machine cycle, independent of acceleration calls. */
    public long getSlotLogicalProgress(int slot) {
        return validSlot(slot) ? externalTickAcceleration[slot].currentCycleNaturalTicks() : 0;
    }

    public boolean isSlotWaitingForNaturalWindow(int slot) {
        return validSlot(slot) && externalTickAcceleration[slot].waitingForNaturalWindow();
    }

    public long getSlotPreviousExternalAccelerationMachineTicks(int slot) {
        return validSlot(slot) ? externalTickAcceleration[slot].previousActualTicks() : 0;
    }

    public int getSlotPreviousExternalAccelerationParallelHundredths(int slot) {
        return validSlot(slot)
                ? externalTickAcceleration[slot].previousExtraParallelHundredths()
                : 0;
    }

    public int getBaseParallelCount() {
        return getBaseParallel();
    }

    /** Energy consumed by one working marker slot per machine tick. */
    public int getEffectiveThreadEnergyConsumption() {
        return Math.max(
                1,
                (int)
                        Math.ceil(
                                getEnergyConsumption()
                                        * upgradeBonuses.energyConsumptionMultiplier()));
    }

    /** Total energy consumed per machine tick by all currently working slots. */
    public int getEffectiveEnergyConsumption() {
        long total = (long) getEffectiveThreadEnergyConsumption() * getWorkingThreadCount();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int getWorkingThreadCount() {
        int working = 0;
        for (int slot = 0; slot < slotProcessingTimes.length; slot++) {
            if (slotEnabled[slot] && slotProcessingTimes[slot] > 0) {
                working++;
            }
        }
        return working;
    }

    public double getEffectiveMachineEfficiency() {
        return getMachineEfficiency() * upgradeBonuses.efficiencyMultiplier();
    }

    public double getBaseMachineEfficiency() {
        return getMachineEfficiency();
    }

    public float getBaseMachineLuck() {
        return getMachineLuck();
    }

    public int getBaseEnergyCapacity() {
        return getEnergyCapacity();
    }

    public float getEffectiveMachineLuck() {
        return MythicMinerUpgradeMath.effectiveLuck(
                getMachineLuck(), upgradeBonuses.luckIncreasePercent());
    }

    public double getEfficiencyUpgradePercent() {
        return (upgradeBonuses.efficiencyMultiplier() - 1.0D) * 100.0D;
    }

    public double getEnergyCapacityUpgradePercent() {
        return (upgradeBonuses.energyCapacityMultiplier() - 1.0D) * 100.0D;
    }

    public double getEnergyConsumptionReductionPercent() {
        return (1.0D - upgradeBonuses.energyConsumptionMultiplier()) * 100.0D;
    }

    public double getParallelUpgradePercent() {
        return upgradeBonuses.parallelMultiplierHundredths() - 100.0D;
    }

    public double getLuckUpgradePercent() {
        return upgradeBonuses.luckIncreasePercent();
    }

    public int getUpgradeCount(MythicMinerUpgradeBlock.Type type) {
        return switch (type) {
            case EFFICIENCY -> upgradeBonuses.efficiencyUpgradeCount();
            case ENERGY -> upgradeBonuses.energyUpgradeCount();
            case PARALLEL -> upgradeBonuses.parallelUpgradeCount();
            case LUCK -> upgradeBonuses.luckUpgradeCount();
            case AGGREGATE -> upgradeBonuses.aggregateUpgradeCount();
            case NONE -> 0;
        };
    }

    public int getUpgradeCount(MythicMinerUpgradeBlock.Type type, int tier) {
        if (tier < 1 || tier > 6 || type == MythicMinerUpgradeBlock.Type.NONE) {
            return 0;
        }
        return upgradeBonuses.countFor(type, tier);
    }

    public int getTotalUpgradeCount() {
        return upgradeBonuses.efficiencyUpgradeCount()
                + upgradeBonuses.energyUpgradeCount()
                + upgradeBonuses.parallelUpgradeCount()
                + upgradeBonuses.luckUpgradeCount()
                + upgradeBonuses.aggregateUpgradeCount();
    }

    public int getAccumulatedParallelHundredths() {
        int slot = firstActiveSlot();
        return slot >= 0 ? slotParallelFractionHundredths[slot] : 0;
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

    public void cycleOutputState() {
        configuredOutputState =
                configuredOutputState == OutputState.ITEM_HANDLER
                        ? OutputState.ME_NETWORK
                        : OutputState.ITEM_HANDLER;
        setChanged();
    }

    public boolean supportsEquipmentDismantling() {
        return getBlockState().getBlock() instanceof BaseMinerBlock miner && miner.minerTier() >= 3;
    }

    public boolean isEquipmentDismantlingEnabled() {
        return supportsEquipmentDismantling() && equipmentDismantling;
    }

    public void toggleEquipmentDismantling() {
        if (supportsEquipmentDismantling()) {
            equipmentDismantling = !equipmentDismantling;
            setChanged();
        }
    }

    public int getOutputFaceMask() {
        return outputFaceMask;
    }

    public boolean isOutputFaceEnabled(Direction direction) {
        return (outputFaceMask & (1 << toWorldDirection(direction).ordinal())) != 0;
    }

    public void toggleOutputFace(Direction direction) {
        outputFaceMask ^= 1 << toWorldDirection(direction).ordinal();
        setChanged();
    }

    public Direction toWorldDirection(Direction logicalDirection) {
        if (level == null
                || !level.getBlockState(worldPosition).hasProperty(BaseMinerBlock.FACING)) {
            return logicalDirection;
        }
        Direction front = level.getBlockState(worldPosition).getValue(BaseMinerBlock.FACING);
        return switch (logicalDirection) {
            case NORTH -> front;
            case SOUTH -> front.getOpposite();
            case EAST -> front.getClockWise();
            case WEST -> front.getCounterClockWise();
            default -> logicalDirection;
        };
    }

    public boolean isWorldOutputFaceEnabled(Direction worldDirection) {
        return (outputFaceMask & (1 << worldDirection.ordinal())) != 0;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getProgress() {
        int slot = firstActiveSlot();
        return slot >= 0 ? getSlotProgress(slot) : 0;
    }

    public int getProgressPercent() {
        return (int) Math.min(100L, (long) getProgress() * 100L / getProcessingTime());
    }

    public int getSlotProgress(int slot) {
        return validSlot(slot) ? slotProgress.get(slot) : 0;
    }

    public int getSlotProcessingTime(int slot) {
        return validSlot(slot) && slotEnabled[slot] ? slotProcessingTimes[slot] : 0;
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
        if (!validSlot(slot) || !slotEnabled[slot] || slotProcessingTimes[slot] <= 0) {
            return 0;
        }
        return (int)
                Math.max(
                        1L,
                        Math.min(Integer.MAX_VALUE, slotDisplayParallelHundredths(slot) / 100L));
    }

    public MythicMinerAnalysisSnapshot getMarkerAnalysisSnapshot(int slot) {
        if (!(level instanceof ServerLevel serverLevel) || !validSlot(slot)) {
            return MythicMinerAnalysisSnapshot.EMPTY;
        }
        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlans();
        CachedMarkerLoot cachedLoot = cachedLootForSlot(slot);
        if (cachedLoot == null || cachedLoot.quantity() <= 0.0D) {
            return MythicMinerAnalysisSnapshot.EMPTY;
        }

        double averageParallel = slotAverageParallelHundredths(slot) / 100.0D;
        int quantityFactor =
                MythicMinerExpectationMath.quantityFactorHundredths(
                        cachedLoot.quantity(), getQuantityReference());
        double expectedDraws =
                MythicMinerExpectationMath.expectedDraws(
                        averageParallel, DRAWS_PER_PARALLEL, quantityFactor);
        Map<ResourceLocation, Double> effectiveExpectations = new LinkedHashMap<>();
        cachedLoot
                .expectedItems()
                .forEach(
                        (item, weight) -> {
                            double expected =
                                    MythicMinerExpectationMath.expectedItemCount(
                                            weight.finiteDoubleValue(),
                                            cachedLoot.quantity(),
                                            expectedDraws);
                            if (Double.isFinite(expected) && expected > 0.0D) {
                                effectiveExpectations.put(item, expected);
                            }
                        });
        boolean dismantling = isEquipmentDismantlingEnabled();
        Map<ResourceLocation, Double> displayedExpectations =
                dismantling
                        ? EquipmentDismantler.dismantleExpectations(
                                serverLevel, effectiveExpectations)
                        : effectiveExpectations;
        return new MythicMinerAnalysisSnapshot(
                cachedLoot.dimensionValue(),
                cachedLoot.structureValue(),
                dismantling,
                displayedExpectations,
                disabledExpectedItems);
    }

    public void toggleExpectedItem(ResourceLocation itemId) {
        if (!disabledExpectedItems.add(itemId)) {
            disabledExpectedItems.remove(itemId);
        }
        setChanged();
    }

    public boolean isExpectedItemDisabled(ResourceLocation itemId) {
        return disabledExpectedItems.contains(itemId);
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
                    .map(markerInfo -> markerInfo.structure().id())
                    .ifPresent(structures::add);
        }
        return structures.stream().distinct().toList();
    }

    public OutputState getOutputState() {
        if (!(level instanceof ServerLevel)) {
            return OutputState.NONE;
        }
        return configuredOutputState;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean complete = isStructureComplete(serverLevel);
        if (complete != structureComplete) {
            structureComplete = complete;
            setChanged();
        }
        if (!complete) {
            applyUpgradeBonuses(UpgradeBonuses.NONE);
            return;
        }
        applyUpgradeBonuses(calculateUpgradeBonuses(serverLevel));
        int signal = level.getBestNeighborSignal(worldPosition);
        if (!redstoneMode.allows(signal, redstoneThreshold)) {
            return;
        }
        autoExtractFluid(serverLevel);
        if (!pendingOutput.isEmpty()) {
            retryPendingOutput(serverLevel);
            return;
        }
        if (!hasValidMarker()) {
            return;
        }

        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlans();
        int startingCycles = countStartingCycles();
        long gameTime = serverLevel.getGameTime();
        int energyConsumption = getEffectiveEnergyConsumption();
        if (gameTime != lastEnergyConsumptionGameTime
                && (energyConsumption <= 0 || !energyStorage.canConsume(energyConsumption))) {
            return;
        }
        if (!consumeCycleFluid(startingCycles)) {
            return;
        }
        if (gameTime != lastEnergyConsumptionGameTime) {
            energyStorage.consume(energyConsumption);
            lastEnergyConsumptionGameTime = gameTime;
        }
        List<CompletedMarker> completedMarkers = new ArrayList<>();
        for (CachedMarkerLoot cachedLoot : cachedMarkerLoot) {
            int slot = cachedLoot.slot();
            if (!slotEnabled[slot]) {
                continue;
            }
            MythicMinerExternalTickAcceleration.Observation accelerationObservation =
                    externalTickAcceleration[slot].observe(
                            serverLevel.getGameTime(), slotProcessingTimes[slot]);
            // Persist the player-facing logical progress; accelerated execution is exposed
            // separately through the long actual-tick telemetry.
            slotProgress.set(
                    slot,
                    (int)
                            Math.min(
                                    Integer.MAX_VALUE,
                                    accelerationObservation.logicalProgressTicks()));
            if (accelerationObservation.complete()) {
                completedMarkers.add(new CompletedMarker(cachedLoot, drawParallelForCycle(slot)));
            }
        }
        if (!completedMarkers.isEmpty()) {
            drawMarkerLoot(serverLevel.getServer(), completedMarkers);
        }
        setChanged();
    }

    private int countStartingCycles() {
        if (!requiresFluidInput()) return 0;
        int startingCycles = 0;
        for (int slot = 0; slot < slotProcessingTimes.length; slot++) {
            if (slotEnabled[slot]
                    && slotProcessingTimes[slot] > 0
                    && externalTickAcceleration[slot].currentActualTicks() == 0L) {
                startingCycles++;
            }
        }
        return startingCycles;
    }

    private boolean consumeCycleFluid(int cycleCount) {
        if (cycleCount <= 0 || !requiresFluidInput()) return true;
        long requested = (long) cycleCount * FLUID_PER_WORK_CYCLE_MB;
        if (requested > Integer.MAX_VALUE) return false;
        Fluid required = getRequiredFluid();
        if (required == null) return false;
        FluidStack request = new FluidStack(required, (int) requested);
        FluidStack simulated = fluidTank.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() < requested) return false;
        FluidStack drained = fluidTank.drain(request, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount() == requested;
    }

    public boolean isStructureComplete() {
        if (level instanceof ServerLevel serverLevel) {
            structureComplete = isStructureComplete(serverLevel);
        }
        return structureComplete;
    }

    private boolean isStructureComplete(ServerLevel serverLevel) {
        int tier =
                getBlockState().getBlock() instanceof BaseMinerBlock miner ? miner.minerTier() : 1;
        return MythicMinerMultiblock.isComplete(serverLevel, worldPosition, tier);
    }

    private UpgradeBonuses calculateUpgradeBonuses(ServerLevel serverLevel) {
        double efficiencyPercent = 0.0D;
        double capacityPercent = 0.0D;
        double parallelPercent = 0.0D;
        double luckPercent = 0.0D;
        double additiveConsumptionReductionPercent = 0.0D;
        double consumptionMultiplier = 1.0D;
        int efficiencyUpgradeCount = 0;
        int energyUpgradeCount = 0;
        int parallelUpgradeCount = 0;
        int luckUpgradeCount = 0;
        int aggregateUpgradeCount = 0;
        int[] upgradeCountsByTypeAndTier =
                new int[MythicMinerUpgradeBlock.Type.values().length * 6];
        for (MythicMinerUpgradeBlock block :
                MythicMinerMultiblock.upgrades(serverLevel, worldPosition)) {
            if (block.getType() != MythicMinerUpgradeBlock.Type.NONE) {
                upgradeCountsByTypeAndTier[
                        (block.getType().ordinal() - 1) * 6 + block.getTier() - 1]++;
            }
            switch (block.getType()) {
                case EFFICIENCY -> {
                    efficiencyUpgradeCount++;
                    efficiencyPercent +=
                            ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                                    .efficiencyIncreasePercent();
                }
                case ENERGY -> {
                    energyUpgradeCount++;
                    ModConfigs.MythicMinerUpgradeTierConfig config =
                            ModConfigs.UPGRADE_TIERS[block.getTier() - 1];
                    capacityPercent += config.energyCapacityIncreasePercent();
                    consumptionMultiplier *=
                            1.0D - config.energyConsumptionReductionPercent() / 100.0D;
                }
                case PARALLEL -> {
                    parallelUpgradeCount++;
                    parallelPercent +=
                            ModConfigs.UPGRADE_TIERS[block.getTier() - 1].parallelIncreasePercent();
                }
                case LUCK -> {
                    luckUpgradeCount++;
                    luckPercent +=
                            ModConfigs.UPGRADE_TIERS[block.getTier() - 1].luckIncreasePercent();
                }
                case AGGREGATE -> {
                    aggregateUpgradeCount++;
                    ModConfigs.MythicMinerUpgradeTierConfig config =
                            ModConfigs.AGGREGATE_UPGRADE_TIERS[block.getTier() - 1];
                    efficiencyPercent += config.efficiencyIncreasePercent();
                    capacityPercent += config.energyCapacityIncreasePercent();
                    additiveConsumptionReductionPercent +=
                            config.energyConsumptionReductionPercent();
                    parallelPercent += config.parallelIncreasePercent();
                    luckPercent += config.luckIncreasePercent();
                }
                case NONE -> {}
            }
        }
        consumptionMultiplier *=
                Math.max(0.0D, 1.0D - additiveConsumptionReductionPercent / 100.0D);
        int parallelMultiplierHundredths =
                (int) Math.min(Integer.MAX_VALUE, Math.round(100.0D + parallelPercent));
        return new UpgradeBonuses(
                1.0D + efficiencyPercent / 100.0D,
                1.0D + capacityPercent / 100.0D,
                Math.max(100, parallelMultiplierHundredths),
                luckPercent,
                Math.max(0.0D, consumptionMultiplier),
                efficiencyUpgradeCount,
                energyUpgradeCount,
                parallelUpgradeCount,
                luckUpgradeCount,
                aggregateUpgradeCount,
                upgradeCountsByTypeAndTier);
    }

    private void applyUpgradeBonuses(UpgradeBonuses bonuses) {
        upgradeBonuses = bonuses;
        long capacity = Math.round(getEnergyCapacity() * bonuses.energyCapacityMultiplier());
        energyStorage.setCapacity((int) Math.max(1L, Math.min(Integer.MAX_VALUE, capacity)));
    }

    private boolean hasValidMarker() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (marker.is(ModItems.STRUCT_MARKER.get())
                    && StructMarkerItem.getMarkerInfo(marker).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private int drawParallelForCycle(int slot) {
        long scaledParallel = slotAverageParallelHundredths(slot);
        long integerPart = scaledParallel / 100L;
        int fractionalPart = (int) (scaledParallel % 100L);
        int accumulated = slotParallelFractionHundredths[slot] + fractionalPart;
        int carry = accumulated / 100;
        slotParallelFractionHundredths[slot] = accumulated % 100;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, integerPart + carry));
    }

    private long slotAverageParallelHundredths(int slot) {
        long machineParallelHundredths =
                (long) getBaseParallel()
                        * slotParallelHundredths[slot]
                        * upgradeBonuses.parallelMultiplierHundredths()
                        / 100L;
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths
                        + externalTickAcceleration[slot].settledExtraParallelHundredths());
    }

    private long slotDisplayParallelHundredths(int slot) {
        long machineParallelHundredths =
                (long) getBaseParallel()
                        * slotParallelHundredths[slot]
                        * upgradeBonuses.parallelMultiplierHundredths()
                        / 100L;
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths
                        + externalTickAcceleration[slot].currentExtraParallelHundredths());
    }

    private void drawMarkerLoot(MinecraftServer server, List<CompletedMarker> completedMarkers) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }

        refreshMarkerLootCache(server);
        List<ItemStack> mergedLoot = new ArrayList<>();
        for (CompletedMarker completed : completedMarkers) {
            CachedMarkerLoot cachedLoot = completed.loot();
            ResourceKey<Level> dimensionKey =
                    ResourceKey.create(Registries.DIMENSION, cachedLoot.dimension());
            ServerLevel lootLevel = server.getLevel(dimensionKey);
            if (lootLevel == null) continue;
            double quantity = cachedLoot.quantity();
            if (!Double.isFinite(quantity) || quantity <= 0.0D) continue;
            List<ItemStack> generatedLoot =
                    drawExpectedLoot(
                            lootLevel,
                            cachedLoot.expectedItems(),
                            drawsForQuantity(cachedLoot.slot(), completed.parallel(), quantity));
            for (ItemStack generated : generatedLoot) {
                List<ItemStack> output =
                        isEquipmentDismantlingEnabled()
                                ? EquipmentDismantler.dismantle(lootLevel, generated)
                                : List.of(generated);
                output.stream()
                        .filter(
                                stack ->
                                        !isExpectedItemDisabled(
                                                net.minecraft.core.registries.BuiltInRegistries.ITEM
                                                        .getKey(stack.getItem())))
                        .forEach(stack -> mergeLootStack(mergedLoot, stack));
            }
            addDimensionCoreReward(lootLevel, mergedLoot, completed.parallel());
        }

        AdjacentOutputs outputs = findAdjacentOutputs(outputLevel);
        pendingOutput = outputLoot(outputs, mergedLoot);
        setChanged();
    }

    /** Rolls once per parallel operation; the machine tier raises the base 5% chance. */
    private void addDimensionCoreReward(
            ServerLevel level, List<ItemStack> mergedLoot, int parallel) {
        ResourceLocation coreId = ModItems.DIMENSION_DECONSTRUCTION_CORE_ID;
        if (isExpectedItemDisabled(coreId)) return;
        int tier = 1;
        String name = getTranslationName();
        int marker = name.indexOf("tier_");
        if (marker >= 0 && marker + 6 < name.length()) {
            try {
                tier =
                        Math.max(
                                1,
                                Math.min(
                                        6,
                                        Integer.parseInt(name.substring(marker + 5, marker + 6))));
            } catch (NumberFormatException ignored) {
                // Keep tier one for custom miner implementations.
            }
        }
        float chance = Math.min(1.0F, 0.05F * tier);
        int rolls = Math.min(10, Math.max(0, parallel));
        int cores = 0;
        for (int roll = 0; roll < rolls; roll++) {
            if (level.random.nextFloat() < chance) cores++;
        }
        if (cores > 0) {
            mergeLootStack(
                    mergedLoot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get(), cores));
        }
    }

    private void refreshMarkerLootCache(MinecraftServer server) {
        CompoundTag currentSnapshot = itemHandler.serializeNBT();
        currentSnapshot.putFloat("MachineLuck", getEffectiveMachineLuck());
        if (currentSnapshot.equals(analyzedMarkerSnapshot)) {
            return;
        }

        List<CachedMarkerLoot> refreshedCache = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (!marker.is(ModItems.STRUCT_MARKER.get())) {
                resetSlotState(slot);
                continue;
            }

            CachedMarkerLoot previous = cachedLootForSlot(slot);
            if (previous == null || !ItemStack.isSameItemSameTags(previous.marker(), marker)) {
                resetSlotState(slot);
            }
            var markerInfo = StructMarkerItem.getMarkerInfo(marker);
            if (markerInfo.isEmpty()) {
                resetSlotState(slot);
                continue;
            }
            refreshedCache.add(analyzeMarkerLoot(server, slot, marker, markerInfo.get()));
        }
        cachedMarkerLoot = List.copyOf(refreshedCache);
        analyzedMarkerSnapshot = currentSnapshot;
    }

    private CachedMarkerLoot analyzeMarkerLoot(
            MinecraftServer server, int slot, ItemStack marker, MarkerInfo markerInfo) {
        ResourceKey<Level> dimensionKey =
                ResourceKey.create(Registries.DIMENSION, markerInfo.dimension());
        ServerLevel analysisLevel = server.getLevel(dimensionKey);
        if (analysisLevel == null) {
            analysisLevel = level instanceof ServerLevel current ? current : null;
        }
        if (analysisLevel == null) {
            return new CachedMarkerLoot(
                    slot,
                    marker.copy(),
                    markerInfo.dimension(),
                    markerInfo.position(),
                    0.0D,
                    0.0D,
                    0.0D,
                    Map.of());
        }
        var analysis =
                StructureValueCalculator.calculate(
                        analysisLevel, markerInfo, getEffectiveMachineLuck());
        Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
        analysis.itemCounts()
                .forEach(
                        (item, expected) -> {
                            ResourceLocation itemId =
                                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                            item);
                            if (itemId != null) {
                                expectedItems.put(itemId, expected);
                            }
                        });
        return new CachedMarkerLoot(
                slot,
                marker.copy(),
                markerInfo.dimension(),
                markerInfo.position(),
                analysis.dimensionValue(),
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
            Item item =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getOptional(entry.getKey())
                            .orElse(null);
            if (item == null || !Double.isFinite(weight) || weight <= 0.0D) continue;
            if (isExpectedItemDisabled(entry.getKey())) continue;
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

    private int drawsForQuantity(int slot, int parallel, double quantity) {
        int factorHundredths =
                MythicMinerExpectationMath.quantityFactorHundredths(
                        quantity, getQuantityReference());
        long scaled = (long) parallel * DRAWS_PER_PARALLEL * factorHundredths;
        long whole = scaled / 100L;
        int accumulated = slotQuantityFractionHundredths[slot] + (int) (scaled % 100L);
        whole += accumulated / 100;
        slotQuantityFractionHundredths[slot] = accumulated % 100;
        return (int) Math.min(Integer.MAX_VALUE, whole);
    }

    private void updateProcessingPlans() {
        java.util.Arrays.fill(slotProcessingTimes, 0);
        java.util.Arrays.fill(slotParallelHundredths, 0);
        for (CachedMarkerLoot cachedLoot : cachedMarkerLoot) {
            if (slotEnabled[cachedLoot.slot()]) {
                updateProcessingPlan(cachedLoot.slot(), cachedLoot.structureValue());
            }
        }
    }

    private void updateProcessingPlan(int slot, double structureValue) {
        double efficiency = getEffectiveMachineEfficiency();
        if (!Double.isFinite(efficiency) || efficiency <= 0.0D || structureValue <= 0.0D) {
            slotProcessingTimes[slot] = 400;
            slotParallelHundredths[slot] = 100;
            slotProgress.clamp(slot, slotProcessingTimes[slot]);
            return;
        }

        double calculatedTicks = structureValue / efficiency;
        if (!Double.isFinite(calculatedTicks)) {
            slotProcessingTimes[slot] =
                    (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(400.0D, calculatedTicks)));
            slotParallelHundredths[slot] = 100;
            slotProgress.clamp(slot, slotProcessingTimes[slot]);
            return;
        }

        if (calculatedTicks >= 400.0D) {
            slotProcessingTimes[slot] =
                    (int) Math.min(Integer.MAX_VALUE, Math.ceil(calculatedTicks));
            slotParallelHundredths[slot] = 100;
            slotProgress.clamp(slot, slotProcessingTimes[slot]);
            return;
        }

        slotProcessingTimes[slot] = 400;
        int maxParallelHundredths = Integer.MAX_VALUE / Math.max(1, getBaseParallel());
        slotParallelHundredths[slot] =
                BigDecimal.valueOf(400.0D)
                        .divide(BigDecimal.valueOf(calculatedTicks), 2, RoundingMode.DOWN)
                        .movePointRight(2)
                        .min(BigDecimal.valueOf(maxParallelHundredths))
                        .intValue();
        slotParallelHundredths[slot] = Math.max(100, slotParallelHundredths[slot]);
        slotProgress.clamp(slot, slotProcessingTimes[slot]);
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
            if (!isWorldOutputFaceEnabled(direction)) {
                continue;
            }
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

    private void autoExtractFluid(ServerLevel serverLevel) {
        if (!isAutoExtractFluidEnabled() || fluidTank.getFluidAmount() >= fluidTank.getCapacity()) {
            return;
        }
        Fluid required = getRequiredFluid();
        if (required == null) {
            return;
        }
        int remaining = fluidTank.getCapacity() - fluidTank.getFluidAmount();
        for (Direction direction : Direction.values()) {
            if (remaining <= 0 || getFluidFaceMode(direction) != FluidFaceMode.INPUT) {
                continue;
            }
            BlockEntity adjacent = serverLevel.getBlockEntity(worldPosition.relative(direction));
            if (adjacent == null) {
                continue;
            }
            if (AE2_LOADED && Ae2Integration.isOnlineInterface(adjacent)) {
                if (configuredOutputState == OutputState.ME_NETWORK) {
                    remaining -= Ae2Integration.extractFluidFromInterfaceNetwork(
                            adjacent, required, remaining, fluidTank);
                }
                continue;
            }
            IFluidHandler handler = adjacent
                    .getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                    .orElse(null);
            if (handler == null) {
                continue;
            }
            FluidStack simulated = handler.drain(new FluidStack(required, remaining), IFluidHandler.FluidAction.SIMULATE);
            int accepted = fluidTank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                int filled = fluidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                if (filled < drained.getAmount()) {
                    // The simulation contract of a fluid handler should prevent this path.
                    // Keeping the mismatch visible avoids silently duplicating fluid.
                    setChanged();
                }
                remaining -= filled;
            }
        }
    }

    private IFluidHandler createFluidInputHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return fluidTank.getTanks(); }
            @Override public FluidStack getFluidInTank(int tank) { return fluidTank.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return fluidTank.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { return fluidTank.isFluidValid(tank, stack); }
            @Override public int fill(FluidStack stack, FluidAction action) { return fluidTank.fill(stack, action); }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
        };
    }

    private IFluidHandler createFluidOutputHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return fluidTank.getTanks(); }
            @Override public FluidStack getFluidInTank(int tank) { return fluidTank.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return fluidTank.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
            @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) { return fluidTank.drain(stack, action); }
            @Override public FluidStack drain(int amount, FluidAction action) { return fluidTank.drain(amount, action); }
        };
    }

    private void retryPendingOutput(ServerLevel outputLevel) {
        pendingOutput = outputLoot(findAdjacentOutputs(outputLevel), pendingOutput);
        setChanged();
    }

    private List<ItemStack> outputLoot(AdjacentOutputs outputs, List<ItemStack> stacks) {
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remainder = FullDurabilityLoot.normalize(stack);
            if (configuredOutputState == OutputState.ME_NETWORK) {
                for (BlockEntity meInterface : outputs.meInterfaces()) {
                    remainder = Ae2Integration.insertIntoInterfaceNetwork(meInterface, remainder);
                    if (remainder.isEmpty()) {
                        break;
                    }
                }
            }
            if (configuredOutputState == OutputState.ITEM_HANDLER) {
                for (IItemHandler outputHandler : outputs.itemHandlers()) {
                    if (remainder.isEmpty()) {
                        break;
                    }
                    remainder =
                            ItemHandlerHelper.insertItemStacked(outputHandler, remainder, false);
                }
            }
            if (!remainder.isEmpty()) {
                remainders.add(remainder);
            }
        }
        return List.copyOf(remainders);
    }

    private int firstActiveSlot() {
        for (int slot = 0; slot < slotProcessingTimes.length; slot++) {
            if (slotProcessingTimes[slot] > 0) {
                return slot;
            }
        }
        return -1;
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < slotProcessingTimes.length;
    }

    private CachedMarkerLoot cachedLootForSlot(int slot) {
        for (CachedMarkerLoot cachedLoot : cachedMarkerLoot) {
            if (cachedLoot.slot() == slot) {
                return cachedLoot;
            }
        }
        return null;
    }

    private void resetSlotState(int slot) {
        slotProgress.reset(slot);
        slotProcessingTimes[slot] = 0;
        slotParallelHundredths[slot] = 0;
        slotParallelFractionHundredths[slot] = 0;
        slotQuantityFractionHundredths[slot] = 0;
        externalTickAcceleration[slot] = new MythicMinerExternalTickAcceleration();
    }

    private static void loadFractions(int[] target, int[] saved) {
        java.util.Arrays.fill(target, 0);
        for (int slot = 0; slot < Math.min(target.length, saved.length); slot++) {
            target[slot] = Math.max(0, Math.min(99, saved[slot]));
        }
    }

    private static long readLongCompat(CompoundTag tag, String key, String legacyKey) {
        if (tag.contains(key, Tag.TAG_LONG)) return Math.max(0L, tag.getLong(key));
        if (tag.contains(key, Tag.TAG_INT)) return Math.max(0L, tag.getInt(key));
        if (legacyKey != null && tag.contains(legacyKey, Tag.TAG_LONG)) {
            return Math.max(0L, tag.getLong(legacyKey));
        }
        if (legacyKey != null && tag.contains(legacyKey, Tag.TAG_INT)) {
            return Math.max(0L, tag.getInt(legacyKey));
        }
        return 0L;
    }

    private record AdjacentOutputs(
            List<BlockEntity> meInterfaces, List<IItemHandler> itemHandlers) {}

    private record CachedMarkerLoot(
            int slot,
            ItemStack marker,
            ResourceLocation dimension,
            BlockPos position,
            double dimensionValue,
            double structureValue,
            double quantity,
            Map<ResourceLocation, ExactProbability> expectedItems) {}

    private record CompletedMarker(CachedMarkerLoot loot, int parallel) {}

    private record WeightedItem(Item item, double weight) {}

    private record UpgradeBonuses(
            double efficiencyMultiplier,
            double energyCapacityMultiplier,
            int parallelMultiplierHundredths,
            double luckIncreasePercent,
            double energyConsumptionMultiplier,
            int efficiencyUpgradeCount,
            int energyUpgradeCount,
            int parallelUpgradeCount,
            int luckUpgradeCount,
            int aggregateUpgradeCount,
            int[] upgradeCountsByTypeAndTier) {
        private static final UpgradeBonuses NONE =
                new UpgradeBonuses(
                        1.0D,
                        1.0D,
                        100,
                        0.0D,
                        1.0D,
                        0,
                        0,
                        0,
                        0,
                        0,
                        new int[MythicMinerUpgradeBlock.Type.values().length * 6]);

        private int countFor(MythicMinerUpgradeBlock.Type type, int tier) {
            int index = (type.ordinal() - 1) * 6 + tier - 1;
            return index >= 0 && index < upgradeCountsByTypeAndTier.length
                    ? upgradeCountsByTypeAndTier[index]
                    : 0;
        }
    }

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
                analyzedMarkerSnapshot = null;
                ItemStack marker = getStackInSlot(slot);
                if (!marker.is(ModItems.STRUCT_MARKER.get())
                        || StructMarkerItem.getMarkerInfo(marker).isEmpty()) {
                    resetSlotState(slot);
                }
                setChanged();
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, itemHandler.serializeNBT());
        tag.putInt(ENERGY_TAG, energyStorage.getEnergyStored());
        if (requiresFluidInput()) tag.put("Fluid", fluidTank.writeToNBT(new CompoundTag()));
        tag.putLong(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG, lastEnergyConsumptionGameTime);
        tag.putInt(PROGRESS_TAG, getProgress());
        tag.putIntArray(SLOT_PROGRESS_TAG, slotProgress.save());
        tag.putInt(PARALLEL_FRACTION_TAG, getAccumulatedParallelHundredths());
        tag.putIntArray(SLOT_PARALLEL_FRACTION_TAG, slotParallelFractionHundredths);
        int firstActiveSlot = firstActiveSlot();
        tag.putInt(
                QUANTITY_FRACTION_TAG,
                firstActiveSlot >= 0 ? slotQuantityFractionHundredths[firstActiveSlot] : 0);
        tag.putIntArray(SLOT_QUANTITY_FRACTION_TAG, slotQuantityFractionHundredths);
        ListTag accelerationStates = new ListTag();
        for (MythicMinerExternalTickAcceleration acceleration : externalTickAcceleration) {
            MythicMinerExternalTickAcceleration.State state = acceleration.save();
            CompoundTag accelerationTag = new CompoundTag();
            accelerationTag.putLong("LastGameTime", state.lastGameTime());
            accelerationTag.putLong("ActualTicks", state.actualTicks());
            accelerationTag.putLong("NaturalTicks", state.naturalTicks());
            accelerationTag.putLong(
                    "ActualTicksAtNaturalTickStart", state.actualTicksAtNaturalTickStart());
            accelerationTag.putLong(
                    "EquivalentAccelerationTicks", state.equivalentAccelerationTicks());
            accelerationTag.putBoolean("TargetReached", state.targetReached());
            accelerationTag.putInt(
                    "SettledExtraParallelHundredths", state.settledExtraParallelHundredths());
            accelerationTag.putLong("PreviousActualTicks", state.previousActualTicks());
            accelerationTag.putInt(
                    "PreviousExtraParallelHundredths", state.previousExtraParallelHundredths());
            accelerationTag.putBoolean(
                    "ExternalParallelEligible", state.externalParallelEligible());
            accelerationStates.add(accelerationTag);
        }
        tag.put(EXTERNAL_ACCELERATION_STATES_TAG, accelerationStates);
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        tag.putInt("RedstoneThreshold", redstoneThreshold);
        tag.putInt("ConfiguredOutputState", configuredOutputState.ordinal());
        tag.putInt("OutputFaceMask", outputFaceMask);
        tag.putInt("FluidFaceModes", getFluidFaceModesPacked());
        tag.putBoolean("AutoExtractFluid", autoExtractFluid);
        tag.putBoolean(EQUIPMENT_DISMANTLING_TAG, equipmentDismantling);
        int[] enabledSlots = new int[slotEnabled.length];
        for (int index = 0; index < slotEnabled.length; index++) {
            enabledSlots[index] = slotEnabled[index] ? 1 : 0;
        }
        tag.putIntArray(SLOT_ENABLED_TAG, enabledSlots);
        ListTag disabledItems = new ListTag();
        this.disabledExpectedItems.forEach(
                item -> disabledItems.add(StringTag.valueOf(item.toString())));
        tag.put("DisabledExpectedItems", disabledItems);
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
        if (tag.contains("Fluid", Tag.TAG_COMPOUND)) {
            fluidTank.readFromNBT(tag.getCompound("Fluid"));
            if (!fluidTank.isEmpty() && !fluidTank.isFluidValid(fluidTank.getFluid())) {
                fluidTank.setFluid(FluidStack.EMPTY);
            }
        }
        lastEnergyConsumptionGameTime =
                tag.contains(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG, Tag.TAG_LONG)
                        ? tag.getLong(LAST_ENERGY_CONSUMPTION_GAME_TIME_TAG)
                        : Long.MIN_VALUE;
        if (tag.contains(SLOT_PROGRESS_TAG, Tag.TAG_INT_ARRAY)) {
            slotProgress.load(tag.getIntArray(SLOT_PROGRESS_TAG));
        } else {
            slotProgress.load(new int[] {Math.max(0, tag.getInt(PROGRESS_TAG))});
        }
        if (tag.contains(SLOT_PARALLEL_FRACTION_TAG, Tag.TAG_INT_ARRAY)) {
            loadFractions(
                    slotParallelFractionHundredths, tag.getIntArray(SLOT_PARALLEL_FRACTION_TAG));
        } else {
            loadFractions(
                    slotParallelFractionHundredths, new int[] {tag.getInt(PARALLEL_FRACTION_TAG)});
        }
        if (tag.contains(SLOT_QUANTITY_FRACTION_TAG, Tag.TAG_INT_ARRAY)) {
            loadFractions(
                    slotQuantityFractionHundredths, tag.getIntArray(SLOT_QUANTITY_FRACTION_TAG));
        } else {
            loadFractions(
                    slotQuantityFractionHundredths, new int[] {tag.getInt(QUANTITY_FRACTION_TAG)});
        }
        if (tag.contains(EXTERNAL_ACCELERATION_STATES_TAG, Tag.TAG_LIST)) {
            ListTag accelerationStates =
                    tag.getList(EXTERNAL_ACCELERATION_STATES_TAG, Tag.TAG_COMPOUND);
            for (int slot = 0;
                    slot < Math.min(accelerationStates.size(), externalTickAcceleration.length);
                    slot++) {
                CompoundTag accelerationTag = accelerationStates.getCompound(slot);
                externalTickAcceleration[slot].load(
                        new MythicMinerExternalTickAcceleration.State(
                                accelerationTag.getLong("LastGameTime"),
                                readLongCompat(
                                        accelerationTag, "ActualTicks", "StatisticsActualTicks"),
                                readLongCompat(
                                        accelerationTag, "NaturalTicks", "StatisticsNaturalTicks"),
                                readLongCompat(
                                        accelerationTag, "ActualTicksAtNaturalTickStart", null),
                                readLongCompat(
                                        accelerationTag, "EquivalentAccelerationTicks", null),
                                accelerationTag.contains("TargetReached", Tag.TAG_BYTE)
                                        ? accelerationTag.getBoolean("TargetReached")
                                        : readLongCompat(
                                                                accelerationTag,
                                                                "ActualTicks",
                                                                "StatisticsActualTicks")
                                                        >= MythicMinerExternalTickAcceleration
                                                                .MINIMUM_NATURAL_TICKS
                                                && readLongCompat(
                                                                accelerationTag,
                                                                "NaturalTicks",
                                                                "StatisticsNaturalTicks")
                                                        < MythicMinerExternalTickAcceleration
                                                                .MINIMUM_NATURAL_TICKS,
                                accelerationTag.getInt("SettledExtraParallelHundredths"),
                                readLongCompat(accelerationTag, "PreviousActualTicks", null),
                                accelerationTag.getInt("PreviousExtraParallelHundredths"),
                                !accelerationTag.contains("ExternalParallelEligible")
                                        || accelerationTag.getBoolean("ExternalParallelEligible")));
            }
        }
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
        int outputOrdinal =
                tag.contains("ConfiguredOutputState", Tag.TAG_INT)
                        ? tag.getInt("ConfiguredOutputState")
                        : OutputState.ITEM_HANDLER.ordinal();
        configuredOutputState =
                outputOrdinal == OutputState.ME_NETWORK.ordinal()
                        ? OutputState.ME_NETWORK
                        : OutputState.ITEM_HANDLER;
        outputFaceMask =
                tag.contains("OutputFaceMask", Tag.TAG_INT)
                        ? tag.getInt("OutputFaceMask") & ((1 << Direction.values().length) - 1)
                        : (1 << Direction.values().length) - 1;
        int fluidFaceModesPacked = tag.getInt("FluidFaceModes");
        for (Direction direction : Direction.values()) {
            int ordinal = (fluidFaceModesPacked >> (direction.ordinal() * 2)) & 3;
            fluidFaceModes[direction.ordinal()] =
                    !tag.contains("FluidFaceModes", Tag.TAG_INT)
                            ? FluidFaceMode.INPUT
                            : ordinal < FluidFaceMode.values().length
                            ? FluidFaceMode.values()[ordinal]
                            : FluidFaceMode.DISABLED;
        }
        autoExtractFluid = requiresFluidInput() && tag.getBoolean("AutoExtractFluid");
        equipmentDismantling = tag.getBoolean(EQUIPMENT_DISMANTLING_TAG);
        java.util.Arrays.fill(slotEnabled, true);
        if (tag.contains(SLOT_ENABLED_TAG, Tag.TAG_INT_ARRAY)) {
            int[] enabledSlots = tag.getIntArray(SLOT_ENABLED_TAG);
            for (int index = 0;
                    index < Math.min(slotEnabled.length, enabledSlots.length);
                    index++) {
                slotEnabled[index] = enabledSlots[index] != 0;
            }
        }
        disabledExpectedItems.clear();
        for (Tag item : tag.getList("DisabledExpectedItems", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(item.getAsString());
            if (id != null) disabledExpectedItems.add(id);
        }
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
        if (capability == ForgeCapabilities.FLUID_HANDLER && requiresFluidInput()) {
            if (side != null && getFluidFaceMode(side) == FluidFaceMode.OUTPUT) {
                return fluidOutputCapability.cast();
            }
            if (side == null || getFluidFaceMode(side) == FluidFaceMode.INPUT) {
                return fluidCapability.cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCapability.invalidate();
        energyCapability.invalidate();
        fluidCapability.invalidate();
        fluidOutputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandlerCapability = LazyOptional.of(() -> itemHandler);
        energyCapability = LazyOptional.of(() -> energyStorage);
        fluidCapability = LazyOptional.of(this::createFluidInputHandler);
        fluidOutputCapability = LazyOptional.of(this::createFluidOutputHandler);
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

    public enum FluidFaceMode {
        DISABLED,
        INPUT,
        OUTPUT
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

    private final class MinerEnergyStorage implements IEnergyStorage {
        private int capacity;
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

        private void setCapacity(int capacity) {
            this.capacity = Math.max(1, capacity);
            energy = Math.min(energy, this.capacity);
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
