package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.loot.expectation.MarkerAnalysis;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfig;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfigService;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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

    /**
     * A cycle must span the complete natural observation window used for acceleration accounting.
     */
    public static final int MINIMUM_PROCESSING_TIME =
            MinerAccelerationController.MINIMUM_NATURAL_TICKS;

    private final ItemStackHandler itemHandler;
    private final MinerEnergyStorage energyStorage;
    private LazyOptional<IItemHandler> itemHandlerCapability;
    private LazyOptional<IEnergyStorage> energyCapability;
    private LazyOptional<IFluidHandler> fluidCapability;
    private LazyOptional<IFluidHandler> fluidOutputCapability;
    private final FluidTank fluidTank;
    public static final int DEFAULT_PROCESSING_TIME = MINIMUM_PROCESSING_TIME;
    private final MinerAnalysisController analysisController;
    private final MinerUpgradeController upgradeController;
    private final MinerAccelerationController accelerationController;
    private final MinerOutputController outputController = new MinerOutputController();
    private final boolean[] slotEnabled;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private int redstoneThreshold = 8;
    private final FluidFaceMode[] fluidFaceModes = new FluidFaceMode[Direction.values().length];
    private boolean autoExtractFluid;
    private boolean structureComplete;
    /** Natural game time for which this machine has already paid its energy cost. */
    private long lastEnergyConsumptionGameTime = Long.MIN_VALUE;

    protected BaseMinerBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
        this.itemHandler = createItemHandler();
        this.upgradeController = new MinerUpgradeController(position);
        this.analysisController =
                new MinerAnalysisController(
                        itemHandler, this::getEffectiveMachineLuck, this::isRemoved);
        int slotCount = itemHandler.getSlots();
        this.accelerationController = new MinerAccelerationController(slotCount);
        this.slotEnabled = new boolean[slotCount];
        java.util.Arrays.fill(this.slotEnabled, true);
        java.util.Arrays.fill(this.fluidFaceModes, FluidFaceMode.INPUT);
        this.energyStorage = new MinerEnergyStorage(getEnergyCapacity());
        this.fluidTank =
                new FluidTank(1000) {
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
        MinerScriptConfig c = scriptConfig();
        return c != null && c.requiresFluid() != null ? c.requiresFluid() : getMinerTier() >= 2;
    }

    public Fluid getRequiredFluid() {
        return ModFluids.forMinerTier(getMinerTier());
    }

    public final int getMinerTier() {
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
                FluidFaceMode.values()[
                        (fluidFaceModes[worldDirection.ordinal()].ordinal() + 1)
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

    public int getUpgradeCount(MythicMinerUpgradeBlock.Type type) {
        return switch (type) {
            case EFFICIENCY -> upgradeController.state().efficiencyUpgradeCount();
            case ENERGY -> upgradeController.state().energyUpgradeCount();
            case PARALLEL -> upgradeController.state().parallelUpgradeCount();
            case LUCK -> upgradeController.state().luckUpgradeCount();
            case AGGREGATE -> upgradeController.state().aggregateUpgradeCount();
            case NONE -> 0;
        };
    }

    public int getUpgradeCount(MythicMinerUpgradeBlock.Type type, int tier) {
        if (tier < 1 || tier > 6 || type == MythicMinerUpgradeBlock.Type.NONE) {
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

    public void cycleOutputState() {
        outputController.cycleOutputState();
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

    public int getOutputFaceMask() {
        return outputController.outputFaceMask();
    }

    public boolean isOutputFaceEnabled(Direction direction) {
        return outputController.outputFaceEnabled(toWorldDirection(direction));
    }

    public void toggleOutputFace(Direction direction) {
        outputController.toggleOutputFace(toWorldDirection(direction));
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
        return outputController.outputFaceEnabled(worldDirection);
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

    public MythicMinerAnalysisSnapshot getMarkerAnalysisSnapshot(int slot) {
        if (!(level instanceof ServerLevel serverLevel) || !validSlot(slot)) {
            return MythicMinerAnalysisSnapshot.EMPTY;
        }
        return analysisController.snapshot(slot, serverLevel, slotAverageParallelHundredths(slot) / 100.0D,
                DRAWS_PER_PARALLEL, getQuantityReference(), isEquipmentDismantlingEnabled(),
                outputController.disabledItems());
    }

    /**
     * Explicitly refreshes marker analysis and processing plans; queries never invoke this work.
     */
    public void refreshMarkerAnalysis() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        refreshMarkerLootCache(serverLevel.getServer());
        updateProcessingPlans();
    }

    public void toggleExpectedItem(ResourceLocation itemId) {
        outputController.toggleDisabledItem(itemId);
        setChanged();
    }

    public boolean isExpectedItemDisabled(ResourceLocation itemId) {
        return outputController.disabled(itemId);
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
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

    public OutputState getOutputState() {
        if (!(level instanceof ServerLevel)) {
            return OutputState.NONE;
        }
        return outputController.outputState();
    }

    /** Runs the miner contract: state, eligibility, resources, progress, completion, output. */
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        updateMachineState(serverLevel);
        if (!canRunThisTick(serverLevel)) return;
        if (!consumeWorkResources(serverLevel)) return;
        List<MarkerAnalysis> completedSlots =
                advanceWork(serverLevel);
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
        applyUpgradeBonuses(
                complete ? upgradeController.refresh(serverLevel) : upgradeController.clear());
    }

    private boolean canRunThisTick(ServerLevel serverLevel) {
        if (!structureComplete) return false;
        if (!redstoneMode.allows(level.getBestNeighborSignal(worldPosition), redstoneThreshold)) {
            return false;
        }
        autoExtractFluid(serverLevel);
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
                && (energyConsumption <= 0 || !energyStorage.canConsume(energyConsumption))) {
            return false;
        }
        if (!consumeCycleFluid(startingCycles)) return false;
        if (gameTime != lastEnergyConsumptionGameTime) {
            energyStorage.consume(energyConsumption);
            lastEnergyConsumptionGameTime = gameTime;
        }
        return true;
    }

    /**
     * Advances only per-slot progress and acceleration state; completion work follows this phase.
     */
    private List<MarkerAnalysis> advanceWork(
            ServerLevel serverLevel) {
        List<MarkerAnalysis> completedSlots = new ArrayList<>();
        for (MinerAccelerationController.CompletedSlot completed :
                accelerationController.advance(serverLevel.getGameTime(), slotEnabled)) {
            MarkerAnalysis cachedLoot =
                    analysisController.entryForSlot(completed.slot());
            if (cachedLoot != null) completedSlots.add(cachedLoot);
        }
        return completedSlots;
    }

    /** Completes all reached slots before any loot is generated or output is attempted. */
    private List<CompletedMarker> completeWork(
            ServerLevel serverLevel,
            List<MarkerAnalysis> completedSlots) {
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
        if (!completedMarkers.isEmpty()) drawMarkerLoot(serverLevel.getServer(), completedMarkers);
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
        return structureComplete;
    }

    private boolean isStructureComplete(ServerLevel serverLevel) {
        return MythicMinerMultiblock.isComplete(serverLevel, worldPosition, getMinerTier());
    }

    private void applyUpgradeBonuses(MinerUpgradeController.UpgradeState bonuses) {
        long capacity = Math.round(getBaseEnergyCapacity() * bonuses.energyCapacityMultiplier());
        energyStorage.setCapacity((int) Math.max(1L, Math.min(Integer.MAX_VALUE, capacity)));
    }

    private boolean hasValidMarker() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (marker.is(ModItems.STRUCTURE_MARKER.get())
                    && StructMarkerItem.getMarkerInfo(marker).isPresent()) {
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
        long machineParallelHundredths =
                (long) getBaseParallelCount()
                        * accelerationController.currentParallelHundredths(slot)
                        * upgradeController.state().parallelMultiplierHundredths()
                        / 100L;
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths + accelerationController.previousExtraParallel(slot));
    }

    private long slotDisplayParallelHundredths(int slot) {
        long machineParallelHundredths =
                (long) getBaseParallelCount()
                        * accelerationController.currentParallelHundredths(slot)
                        * upgradeController.state().parallelMultiplierHundredths()
                        / 100L;
        return Math.min(
                (long) Integer.MAX_VALUE * 100L,
                machineParallelHundredths + accelerationController.currentExtraParallel(slot));
    }

    private void drawMarkerLoot(MinecraftServer server, List<CompletedMarker> completedMarkers) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }
        outputController.setPending(outputController.emit(
                        this,
                        server,
                        outputLevel,
                        worldPosition,
                        completedMarkers,
                        getMinerTier(),
                        this::isWorldOutputFaceEnabled,
                        slot ->
                                accelerationController.drawsForQuantity(
                                        slot,
                                        completedMarkers.stream()
                                                .filter(value -> value.loot().slot() == slot)
                                                .findFirst().orElseThrow().parallel(),
                                        analysisController.entryForSlot(slot).quantity(),
                                        getQuantityReference())));
        setChanged();
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
        double efficiency = getBaseMachineEfficiency() * upgradeController.state().efficiencyMultiplier();
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

    private void autoExtractFluid(ServerLevel serverLevel) {
        outputController.extractFluid(this, serverLevel, worldPosition, fluidTank, getRequiredFluid(),
                isAutoExtractFluidEnabled(), this::getFluidFaceMode);
    }

    private IFluidHandler createFluidInputHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return fluidTank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return fluidTank.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return fluidTank.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return fluidTank.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack stack, FluidAction action) {
                return fluidTank.fill(stack, action);
            }

            @Override
            public FluidStack drain(FluidStack stack, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    private IFluidHandler createFluidOutputHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return fluidTank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return fluidTank.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return fluidTank.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return false;
            }

            @Override
            public int fill(FluidStack stack, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack stack, FluidAction action) {
                return fluidTank.drain(stack, action);
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                return fluidTank.drain(amount, action);
            }
        };
    }

    private void retryPendingOutput(ServerLevel outputLevel) {
        outputController.setPending(outputController.retry(
                        outputLevel,
                        worldPosition,
                        this::isWorldOutputFaceEnabled));
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
            throw new IllegalStateException("Mythic miner slot count must be positive");
        }

        return new ItemStackHandler(slotCount) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.is(ModItems.STRUCTURE_MARKER.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (analysisController != null) analysisController.invalidateIfInputsChanged();
                ItemStack marker = getStackInSlot(slot);
                if (!marker.is(ModItems.STRUCTURE_MARKER.get())
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
        tag.putInt("FluidFaceModes", getFluidFaceModesPacked());
        tag.putBoolean("AutoExtractFluid", autoExtractFluid);
        int[] enabledSlots = new int[slotEnabled.length];
        for (int index = 0; index < slotEnabled.length; index++) {
            enabledSlots[index] = slotEnabled[index] ? 1 : 0;
        }
        tag.putIntArray(SLOT_ENABLED_TAG, enabledSlots);
        outputController.save(tag, EQUIPMENT_DISMANTLING_TAG, PENDING_OUTPUT_TAG);
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
        outputController.load(tag, EQUIPMENT_DISMANTLING_TAG, PENDING_OUTPUT_TAG);
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
