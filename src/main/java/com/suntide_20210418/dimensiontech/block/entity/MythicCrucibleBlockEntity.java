package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import com.suntide_20210418.dimensiontech.integration.ae2.Ae2Integration;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleFormula;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleSequenceTelemetry;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleTooltipSnapshot;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateId;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateStep;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Single-block crucible: two 16,000 mB tanks, a fragment slot and an immediate operation slot. */
public final class MythicCrucibleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLUID_TANK_CAPACITY_MB = 16_000;
    public static final int FRAGMENT_SLOT = 0;
    public static final int OPERATION_SLOT = 1;

    /** Index of the cycle status ordinal in the container data channel. */
    public static final int DATA_STATUS = 0;

    public static final int DATA_STATE_TICKS = 1;
    public static final int DATA_STATE_INDEX = 2;
    public static final int DATA_INPUT_AMOUNT = 3;
    public static final int DATA_OUTPUT_AMOUNT = 4;

    /** Ordinal of the state being resolved, or -1 while no cycle runs. */
    public static final int DATA_CURRENT_STATE = 5;

    /** Fluid registry id of the tank content, or -1 when the tank is empty. */
    public static final int DATA_INPUT_FLUID = 6;

    public static final int DATA_OUTPUT_FLUID = 7;
    public static final int DATA_SEQUENCE_LENGTH = 8;
    public static final int DATA_SEQUENCE_WORD_0 = 9;
    public static final int DATA_SEQUENCE_WORD_1 = 10;

    /**
     * Ordinal of the last settle result that was not {@code NONE}, or -1 when none happened yet.
     */
    public static final int DATA_LAST_RESOLUTION = 11;

    /** Ordinal of the state that result settled, or -1 when no cycle is running. */
    public static final int DATA_EVENT_STATE = 12;

    /** The current cycle's accumulated rewards first, then its accumulated penalties. */
    public static final int DATA_TIME_REDUCTION = 13;

    public static final int DATA_TIME_PENALTY = 14;
    public static final int DATA_FLUID_REDUCTION_BP = 15;
    public static final int DATA_FLUID_PENALTY_BP = 16;
    public static final int DATA_OUTPUT_BONUS_BP = 17;
    public static final int DATA_OUTPUT_PENALTY_BP = 18;
    public static final int DATA_EXTRA_FRAGMENTS = 19;

    /** The settlement the current cycle would produce right now. */
    public static final int DATA_RESULT_TIME = 20;

    public static final int DATA_RESULT_FLUID = 21;
    public static final int DATA_RESULT_OUTPUT = 22;

    /** Ticks the current cycle has run, across every state it settled so far. */
    public static final int DATA_ELAPSED_TICKS = 23;

    public static final int DATA_REFINING_TICKS = 24;
    public static final int DATA_AUTO_PULL = 25;
    public static final int DATA_AUTO_PUSH = 26;
    public static final int DATA_ME_NETWORK = 27;
    public static final int DATA_FLUID_FACE_MODES = 28;
    public static final int DATA_INPUT_FLUID_LOCKED = 29;

    public static final int DATA_SLOT_COUNT = 30;
    public static final int BUTTON_TOGGLE_AUTO_PULL = 30;
    public static final int BUTTON_TOGGLE_AUTO_PUSH = 31;
    public static final int BUTTON_TOGGLE_ME_NETWORK = 32;
    public static final int BUTTON_TOGGLE_INPUT_LOCK = 33;
    public static final int BUTTON_CLEAR_INPUT_TANK = 34;
    public static final int BUTTON_CLEAR_OUTPUT_TANK = 35;

    /** Everything from here up is a fluid-face cycle id, so screen buttons must stay below it. */
    public static final int BUTTON_CYCLE_FLUID_FACE_BASE = 40;

    private final MythicCrucibleCycle<ItemStack> cycle = new MythicCrucibleCycle<>();
    private final ItemStackHandler inventory =
            new ItemStackHandler(2) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return slot == FRAGMENT_SLOT || slot == OPERATION_SLOT;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };
    private boolean inputFluidLocked;
    private Fluid lockedInputFluid = Fluids.EMPTY;
    private final FluidTank inputTank = tank(true);
    private final FluidTank outputTank = tank(false);
    private FluidStack reservedFluid = FluidStack.EMPTY;
    private ItemStack reservedFragments = ItemStack.EMPTY;
    private final FluidFaceMode[] fluidFaceModes = new FluidFaceMode[Direction.values().length];
    private boolean autoPullFluid;
    private boolean autoPushFluid;
    private boolean meNetwork;

    /** Client-side mirror of the container data channel; the server always reads live state. */
    private final int[] syncedData = new int[DATA_SLOT_COUNT];

    private final int[] packedSequence = new int[CrucibleSequenceTelemetry.WORD_COUNT];
    private MythicCrucibleRecipe<ItemStack> packedRecipe;
    private MythicCrucibleRecipe.Branch packedBranch = MythicCrucibleRecipe.Branch.A;
    private int packedSequenceLength;

    /**
     * Client-facing feedback for the last settle. It is deliberately transient: a reload restores
     * the running cycle's accumulated modifiers, but there is no past event left to announce.
     */
    private MythicCrucibleCycle.Resolution lastResolution = MythicCrucibleCycle.Resolution.NONE;

    private StateId lastEventState;
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final LazyOptional<IFluidHandler> dualFluidCapability =
            LazyOptional.of(() -> new DualTankAccess(inputTank, outputTank));
    private final Map<Direction, LazyOptional<IFluidHandler>> faceFluidCapabilities =
            new EnumMap<>(Direction.class);

    public MythicCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MYTHIC_CRUCIBLE.get(), pos, state);
        Arrays.fill(fluidFaceModes, FluidFaceMode.INPUT);
        fluidFaceModes[Direction.DOWN.ordinal()] = FluidFaceMode.OUTPUT;
        for (Direction direction : Direction.values())
            faceFluidCapabilities.put(
                    direction, LazyOptional.of(() -> new FaceFluidAccess(this, direction)));
    }

    private FluidTank tank(boolean input) {
        return new FluidTank(FLUID_TANK_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return (!input
                                || !inputFluidLocked
                                || lockedInputFluid == Fluids.EMPTY
                                || lockedInputFluid.isSame(stack.getFluid()))
                        && super.isFluidValid(stack);
            }

            @Override
            protected void onContentsChanged() {
                if (input && inputFluidLocked && lockedInputFluid == Fluids.EMPTY && !isEmpty())
                    lockedInputFluid = getFluid().getFluid();
                setChanged();
            }
        };
    }

    public IItemHandler inventory() {
        return inventory;
    }

    public FluidTank inputTank() {
        return inputTank;
    }

    public FluidTank outputTank() {
        return outputTank;
    }

    public MythicCrucibleCycle cycle() {
        return cycle;
    }

    public FluidFaceMode getFluidFaceMode(Direction direction) {
        return fluidFaceModes[direction.ordinal()];
    }

    public int getFluidFaceModesPacked() {
        int packed = 0;
        for (Direction direction : Direction.values())
            packed |= getFluidFaceMode(direction).ordinal() << (direction.ordinal() * 2);
        return packed;
    }

    private int defaultFluidFaceModesPacked() {
        int packed = 0;
        for (Direction direction : Direction.values()) {
            FluidFaceMode mode =
                    direction == Direction.DOWN ? FluidFaceMode.OUTPUT : FluidFaceMode.INPUT;
            packed |= mode.ordinal() << (direction.ordinal() * 2);
        }
        return packed;
    }

    public void cycleFluidFace(Direction direction) {
        if (level != null && level.isClientSide) return;
        FluidFaceMode[] values = FluidFaceMode.values();
        fluidFaceModes[direction.ordinal()] =
                values[(getFluidFaceMode(direction).ordinal() + 1) % values.length];
        setChanged();
    }

    public boolean isAutoPullFluidEnabled() {
        return autoPullFluid;
    }

    public boolean isAutoPushFluidEnabled() {
        return autoPushFluid;
    }

    public boolean isMeNetworkEnabled() {
        return meNetwork;
    }

    public boolean isInputFluidLocked() {
        return inputFluidLocked;
    }

    public void toggleAutoPullFluid() {
        autoPullFluid = !autoPullFluid;
        setChanged();
    }

    public void toggleAutoPushFluid() {
        autoPushFluid = !autoPushFluid;
        setChanged();
    }

    public void toggleMeNetwork() {
        meNetwork = !meNetwork;
        setChanged();
    }

    public void toggleInputFluidLock() {
        inputFluidLocked = !inputFluidLocked;
        if (inputFluidLocked && !inputTank.isEmpty())
            lockedInputFluid = inputTank.getFluid().getFluid();
        if (!inputFluidLocked) lockedInputFluid = Fluids.EMPTY;
        setChanged();
    }

    /**
     * Empties the input tank, voiding whatever it held. The input lock is left alone: it fixes
     * which fluid the tank accepts, which stays meaningful while the tank is empty.
     */
    public void clearInputTank() {
        if (inputTank.isEmpty()) return;
        inputTank.drain(inputTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    /** Empties the output tank, voiding whatever it held. */
    public void clearOutputTank() {
        if (outputTank.isEmpty()) return;
        outputTank.drain(outputTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    /**
     * Moves fluid between one held fluid container and this crucible's tanks, each interaction
     * moving as much as both sides allow.
     *
     * <p>A filled container feeds the input tank, which accepts any fluid while it is empty and
     * only its own once it holds one. When that cannot happen — a foreign fluid in the input, or no
     * room for it — a container that is empty, or that already holds the output fluid and still has
     * room, is instead filled from the output tank.
     *
     * @return true when any fluid actually moved, so the caller knows to re-place the container
     */
    public boolean exchangeWithFluidContainer(IFluidHandlerItem container) {
        if (container.getTanks() < 1) return false;
        FluidStack held = container.getFluidInTank(0);
        boolean moved = !held.isEmpty() && fillInputFrom(container, held);
        if (!moved) moved = fillContainerFromOutput(container);
        if (moved) setChanged();
        return moved;
    }

    /** Feeds the input tank from a filled container, as far as its remaining room allows. */
    private boolean fillInputFrom(IFluidHandlerItem container, FluidStack held) {
        if (!inputTank.isEmpty() && !inputTank.getFluid().isFluidEqual(held)) return false;
        int wanted =
                Math.min(inputTank.getCapacity() - inputTank.getFluidAmount(), held.getAmount());
        if (wanted <= 0) return false;
        FluidStack drained = container.drain(wanted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;
        int accepted = inputTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (accepted < drained.getAmount()) {
            // The tank only ever refuses what the room above ruled out, but stay exact regardless.
            FluidStack refund = drained.copy();
            refund.setAmount(drained.getAmount() - accepted);
            container.fill(refund, IFluidHandler.FluidAction.EXECUTE);
        }
        return accepted > 0;
    }

    /** Draws the output tank into a container that has room and will not end up with a mixture. */
    private boolean fillContainerFromOutput(IFluidHandlerItem container) {
        if (outputTank.isEmpty()) return false;
        FluidStack stored = outputTank.getFluid();
        FluidStack held = container.getFluidInTank(0);
        if (!held.isEmpty() && !held.isFluidEqual(stored)) return false;
        int room = container.getTankCapacity(0) - held.getAmount();
        if (room <= 0) return false;
        FluidStack offered =
                outputTank.drain(
                        Math.min(room, stored.getAmount()), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return false;
        int accepted = container.fill(offered, IFluidHandler.FluidAction.EXECUTE);
        if (accepted <= 0) return false;
        outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void pullFluids(ServerLevel serverLevel) {
        if (inputTank.getFluidAmount() >= inputTank.getCapacity()) return;
        int remaining = inputTank.getCapacity() - inputTank.getFluidAmount();
        List<net.minecraft.world.level.material.Fluid> candidates = new java.util.ArrayList<>();
        if (!inputTank.isEmpty()) candidates.add(inputTank.getFluid().getFluid());
        else
            MythicCrucibleRecipes.all().stream()
                    .map(MythicCrucibleRecipe::input)
                    .distinct()
                    .forEach(candidates::add);
        for (Direction direction : Direction.values()) {
            if (remaining <= 0 || !acceptsInput(getFluidFaceMode(direction))) continue;
            BlockEntity adjacent = serverLevel.getBlockEntity(worldPosition.relative(direction));
            if (adjacent == null) continue;
            for (net.minecraft.world.level.material.Fluid fluid : candidates) {
                if (remaining <= 0) break;
                if (meNetwork
                        && ModList.get().isLoaded("ae2")
                        && Ae2Integration.isOnlineInterface(adjacent)) {
                    remaining -=
                            Ae2Integration.extractFluidFromInterfaceNetwork(
                                    adjacent, fluid, remaining, inputTank);
                    continue;
                }
                IFluidHandler handler =
                        adjacent.getCapability(
                                        ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                                .orElse(null);
                if (handler == null) continue;
                FluidStack simulated =
                        handler.drain(
                                new FluidStack(fluid, remaining),
                                IFluidHandler.FluidAction.SIMULATE);
                int accepted = inputTank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) continue;
                FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                int filled = inputTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                remaining -= filled;
            }
        }
    }

    private void pushFluid(ServerLevel serverLevel) {
        if (outputTank.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            if (outputTank.isEmpty() || !acceptsOutput(getFluidFaceMode(direction))) continue;
            BlockEntity adjacent = serverLevel.getBlockEntity(worldPosition.relative(direction));
            if (adjacent == null) continue;
            if (meNetwork
                    && ModList.get().isLoaded("ae2")
                    && Ae2Integration.isOnlineInterface(adjacent)) {
                FluidStack remainder =
                        Ae2Integration.insertFluidIntoInterfaceNetwork(
                                adjacent, outputTank.getFluid());
                int moved = outputTank.getFluidAmount() - remainder.getAmount();
                if (moved > 0) outputTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                continue;
            }
            IFluidHandler handler =
                    adjacent.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                            .orElse(null);
            if (handler == null) continue;
            int accepted = handler.fill(outputTank.getFluid(), IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack moved = outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                handler.fill(moved, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static boolean acceptsInput(FluidFaceMode mode) {
        return mode == FluidFaceMode.INPUT || mode == FluidFaceMode.INPUT_OUTPUT;
    }

    private static boolean acceptsOutput(FluidFaceMode mode) {
        return mode == FluidFaceMode.OUTPUT || mode == FluidFaceMode.INPUT_OUTPUT;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState blockState, MythicCrucibleBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            if (be.autoPullFluid) be.pullFluids(serverLevel);
            if (be.autoPushFluid) be.pushFluid(serverLevel);
        }
        if (cycleNeedsStart(be)) be.tryStart();
        if (be.cycle.status() == MythicCrucibleCycle.Status.RUNNING) {
            be.cycle.tick();
            ItemStack operation = be.inventory.getStackInSlot(OPERATION_SLOT);
            // The state the crucible is waiting on, captured before the settle advances it.
            StateId expected = be.cycle.currentState();
            // The slot is empty on most ticks; only a real submission may settle or penalize.
            MythicCrucibleCycle.Resolution result =
                    be.cycle.resolve(operation, operation.isEmpty());
            if (result != MythicCrucibleCycle.Resolution.NONE) be.recordEvent(result, expected);
            if (result.consumesInput()) be.inventory.extractItem(OPERATION_SLOT, 1, false);
            be.setChanged();
        }
        if (be.cycle.status() == MythicCrucibleCycle.Status.REFINING) {
            be.cycle.tick();
            be.setChanged();
        }
        if (be.cycle.status() == MythicCrucibleCycle.Status.READY_TO_COMMIT) be.tryCommit();
    }

    /**
     * Remembers what just happened, so the screen can report the reward or penalty it triggered.
     */
    private void recordEvent(MythicCrucibleCycle.Resolution result, StateId settledState) {
        lastResolution = result;
        lastEventState = settledState;
    }

    private void clearEvent() {
        lastResolution = MythicCrucibleCycle.Resolution.NONE;
        lastEventState = null;
    }

    private static boolean cycleNeedsStart(MythicCrucibleBlockEntity be) {
        return be.cycle.status() == MythicCrucibleCycle.Status.IDLE;
    }

    private void tryStart() {
        MythicCrucibleRecipe recipe =
                MythicCrucibleRecipes.firstMatching(inputTank.getFluid().getFluid());
        if (recipe == null || inputTank.getFluidAmount() < recipe.baseFluidCost()) return;
        ItemStack fragments = inventory.getStackInSlot(FRAGMENT_SLOT);
        if (!recipe.fragment().test(fragments) || fragments.getCount() < recipe.fragmentCount())
            return;
        if (outputTank.getCapacity() - outputTank.getFluidAmount() < recipe.targetOutput()) return;
        FluidStack extracted =
                inputTank.drain(recipe.baseFluidCost(), IFluidHandler.FluidAction.EXECUTE);
        if (extracted.getAmount() != recipe.baseFluidCost()) return;
        reservedFluid = extracted;
        reservedFragments = inventory.extractItem(FRAGMENT_SLOT, recipe.fragmentCount(), false);
        clearEvent();
        cycle.start(recipe);
        setChanged();
    }

    private void tryCommit() {
        MythicCrucibleRecipe recipe = cycle.recipe();
        CrucibleFormula.Result result = cycle.finalResult();
        int additionalFluid = Math.max(0, result.fluidCostMb() - reservedFluid.getAmount());
        int refundFluid = Math.max(0, reservedFluid.getAmount() - result.fluidCostMb());
        int additionalFragments = cycle.extraFragmentCost();
        ItemStack fragments = inventory.getStackInSlot(FRAGMENT_SLOT);
        if (additionalFluid > 0
                && (!inputTank.getFluid().getFluid().isSame(recipe.input())
                        || inputTank.getFluidAmount() < additionalFluid)) return;
        if (additionalFragments > 0
                && (!recipe.fragment().test(fragments)
                        || fragments.getCount() < additionalFragments)) return;
        if (!outputTank.isEmpty() && !outputTank.getFluid().getFluid().isSame(recipe.output()))
            return;
        if (outputTank.getCapacity() - outputTank.getFluidAmount() < result.outputAmountMb())
            return;
        // All preconditions above are checked before any irreversible mutation.
        if (additionalFluid > 0)
            inputTank.drain(additionalFluid, IFluidHandler.FluidAction.EXECUTE);
        if (refundFluid > 0)
            inputTank.fill(
                    new FluidStack(recipe.input(), refundFluid), IFluidHandler.FluidAction.EXECUTE);
        if (additionalFragments > 0)
            inventory.extractItem(FRAGMENT_SLOT, additionalFragments, false);
        returnReservedFragments();
        outputTank.fill(
                new FluidStack(recipe.output(), result.outputAmountMb()),
                IFluidHandler.FluidAction.EXECUTE);
        reservedFluid = FluidStack.EMPTY;
        reservedFragments = ItemStack.EMPTY;
        cycle.commit();
        setChanged();
    }

    /**
     * Returns the fragments that were reserved at startup but not charged. Anything that no longer
     * fits in the fragment slot is dropped next to the block, mirroring the abort path.
     */
    private void returnReservedFragments() {
        if (reservedFragments.isEmpty()) return;
        ItemStack remainder = inventory.insertItem(FRAGMENT_SLOT, reservedFragments, false);
        if (!remainder.isEmpty() && level != null)
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    remainder);
    }

    /**
     * Used by block replacement and recipe changes; reserved resources are restored before reset.
     */
    public void abortAndReturnResources() {
        if (!reservedFluid.isEmpty())
            inputTank.fill(reservedFluid, IFluidHandler.FluidAction.EXECUTE);
        returnReservedFragments();
        reservedFluid = FluidStack.EMPTY;
        reservedFragments = ItemStack.EMPTY;
        clearEvent();
        cycle.abort(true);
        setChanged();
    }

    /** Drops both cached operation inputs and ordinary fragment inventory on block removal. */
    public void dropContents() {
        if (level == null) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimension_tech.mythic_crucible");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
        return new MythicCrucibleMenu(id, player, this);
    }

    /** Builds the server-authoritative details used by the resource-region tooltips. */
    public CrucibleTooltipSnapshot tooltipSnapshot() {
        MythicCrucibleRecipe<ItemStack> recipe = cycle.recipe();
        if (recipe == null && !inputTank.isEmpty())
            recipe = MythicCrucibleRecipes.firstMatching(inputTank.getFluid().getFluid());
        if (recipe == null) return CrucibleTooltipSnapshot.empty();

        boolean active = cycle.recipe() != null;
        int inputRequired = active ? cycle.finalResult().fluidCostMb() : recipe.baseFluidCost();
        int outputExpected = active ? cycle.finalResult().outputAmountMb() : recipe.targetOutput();
        List<ItemStack> fragmentCandidates = candidates(recipe.fragment());
        int fragmentCandidateTotal = candidateTotal(recipe.fragment());
        List<ItemStack> operationCandidates = List.of();
        int operationCandidateTotal = 0;
        boolean operationKnown = false;
        int stepIndex = 0;
        int stepCount = 0;
        if (active) {
            List<StateStep<ItemStack>> sequence = recipe.sequence(cycle.branch());
            stepIndex = Math.min(cycle.stateIndex(), sequence.size());
            stepCount = sequence.size();
            if (cycle.status() == MythicCrucibleCycle.Status.RUNNING
                    && cycle.stateIndex() < sequence.size()) {
                var ingredient = sequence.get(cycle.stateIndex()).operation().asIngredient();
                if (ingredient.isPresent()) {
                    operationKnown = true;
                    operationCandidates = candidates(ingredient.get());
                    operationCandidateTotal = candidateTotal(ingredient.get());
                }
            }
        }
        return new CrucibleTooltipSnapshot(
                fragmentCandidates,
                fragmentCandidateTotal,
                recipe.fragmentCount(),
                operationCandidates,
                operationCandidateTotal,
                operationKnown,
                stepIndex,
                stepCount,
                inputRequired,
                outputExpected,
                recipe.input(),
                recipe.output(),
                -1);
    }

    private static List<ItemStack> candidates(
            net.minecraft.world.item.crafting.Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return List.of();
        return Arrays.stream(ingredient.getItems())
                .limit(CrucibleTooltipSnapshot.MAX_CANDIDATES)
                .map(ItemStack::copy)
                .toList();
    }

    private static int candidateTotal(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return ingredient == null || ingredient.isEmpty() ? 0 : ingredient.getItems().length;
    }

    /**
     * Container data channel. The server reads the live crucible, while {@link #set} mirrors
     * whatever the server broadcasts so the client screen never renders from its own inert block
     * entity.
     */
    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (level != null && level.isClientSide) {
                    return index >= 0 && index < syncedData.length ? syncedData[index] : 0;
                }
                return serverValue(index);
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < syncedData.length) syncedData[index] = value;
            }

            @Override
            public int getCount() {
                return DATA_SLOT_COUNT;
            }
        };
    }

    private int serverValue(int index) {
        return switch (index) {
            case DATA_STATUS -> cycle.status().ordinal();
            case DATA_STATE_TICKS -> cycle.stateTicks();
            case DATA_STATE_INDEX -> cycle.stateIndex();
            case DATA_INPUT_AMOUNT -> inputTank.getFluidAmount();
            case DATA_OUTPUT_AMOUNT -> outputTank.getFluidAmount();
            case DATA_CURRENT_STATE ->
                    cycle.currentState() == null ? -1 : cycle.currentState().ordinal();
            case DATA_INPUT_FLUID -> fluidId(inputTank);
            case DATA_OUTPUT_FLUID -> fluidId(outputTank);
            case DATA_SEQUENCE_LENGTH -> {
                refreshPackedSequence();
                yield packedSequenceLength;
            }
            case DATA_SEQUENCE_WORD_0 -> sequenceWord(0);
            case DATA_SEQUENCE_WORD_1 -> sequenceWord(1);
            case DATA_LAST_RESOLUTION ->
                    lastResolution == MythicCrucibleCycle.Resolution.NONE
                            ? -1
                            : lastResolution.ordinal();
            case DATA_EVENT_STATE -> lastEventState == null ? -1 : lastEventState.ordinal();
            // The cycle zeroes every accumulator when it clears, so these need no idle guard.
            case DATA_TIME_REDUCTION -> cycle.timeReduction();
            case DATA_TIME_PENALTY -> cycle.timePenalty();
            case DATA_FLUID_REDUCTION_BP -> cycle.fluidReductionBp();
            case DATA_FLUID_PENALTY_BP -> cycle.fluidPenaltyBp();
            case DATA_OUTPUT_BONUS_BP -> cycle.outputBonusBp();
            case DATA_OUTPUT_PENALTY_BP -> cycle.outputPenaltyBp();
            case DATA_EXTRA_FRAGMENTS -> cycle.extraFragmentCost();
            case DATA_RESULT_TIME -> resultField(CrucibleFormula.Result::timeTicks);
            case DATA_RESULT_FLUID -> resultField(CrucibleFormula.Result::fluidCostMb);
            case DATA_RESULT_OUTPUT -> resultField(CrucibleFormula.Result::outputAmountMb);
            case DATA_ELAPSED_TICKS -> cycle.elapsedTicks();
            case DATA_REFINING_TICKS -> cycle.elapsedTicks();
            case DATA_AUTO_PULL -> autoPullFluid ? 1 : 0;
            case DATA_AUTO_PUSH -> autoPushFluid ? 1 : 0;
            case DATA_ME_NETWORK -> meNetwork ? 1 : 0;
            case DATA_FLUID_FACE_MODES -> getFluidFaceModesPacked();
            case DATA_INPUT_FLUID_LOCKED -> inputFluidLocked ? 1 : 0;
            default -> 0;
        };
    }

    /** The settlement the running cycle would produce right now, or null while it is idle. */
    private CrucibleFormula.Result resultValue() {
        return cycle.recipe() == null ? null : cycle.finalResult();
    }

    private int resultField(java.util.function.ToIntFunction<CrucibleFormula.Result> field) {
        CrucibleFormula.Result result = resultValue();
        return result == null ? 0 : field.applyAsInt(result);
    }

    private static int fluidId(FluidTank tank) {
        return tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
    }

    private int sequenceWord(int wordIndex) {
        refreshPackedSequence();
        return wordIndex >= 0 && wordIndex < packedSequence.length ? packedSequence[wordIndex] : 0;
    }

    /**
     * Repacking only depends on the active recipe and branch, so the words are cached until either
     * changes instead of being rebuilt for every data query.
     */
    private void refreshPackedSequence() {
        MythicCrucibleRecipe<ItemStack> recipe = cycle.recipe();
        MythicCrucibleRecipe.Branch branch = cycle.branch();
        if (recipe == packedRecipe && branch == packedBranch) return;
        packedRecipe = recipe;
        packedBranch = branch;
        if (recipe == null) {
            packedSequenceLength = 0;
            Arrays.fill(packedSequence, 0);
            return;
        }
        List<StateId> states = recipe.sequence(branch).stream().map(StateStep::state).toList();
        packedSequenceLength = Math.min(states.size(), CrucibleSequenceTelemetry.MAX_STATES);
        System.arraycopy(
                CrucibleSequenceTelemetry.pack(states),
                0,
                packedSequence,
                0,
                packedSequence.length);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("CrucibleItems", inventory.serializeNBT());
        tag.put("CrucibleInput", inputTank.writeToNBT(new CompoundTag()));
        tag.put("CrucibleOutput", outputTank.writeToNBT(new CompoundTag()));
        tag.put("CrucibleReservedFluid", reservedFluid.writeToNBT(new CompoundTag()));
        tag.put("CrucibleReservedFragments", reservedFragments.save(new CompoundTag()));
        tag.putInt("FluidFaceModes", getFluidFaceModesPacked());
        tag.putBoolean("AutoPullFluid", autoPullFluid);
        tag.putBoolean("AutoPushFluid", autoPushFluid);
        tag.putBoolean("MeNetwork", meNetwork);
        tag.putBoolean("InputFluidLocked", inputFluidLocked);
        tag.putInt(
                "LockedInputFluid",
                lockedInputFluid == Fluids.EMPTY
                        ? -1
                        : BuiltInRegistries.FLUID.getId(lockedInputFluid));
        cycle.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("CrucibleItems"));
        inputTank.readFromNBT(tag.getCompound("CrucibleInput"));
        outputTank.readFromNBT(tag.getCompound("CrucibleOutput"));
        reservedFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("CrucibleReservedFluid"));
        reservedFragments = ItemStack.of(tag.getCompound("CrucibleReservedFragments"));
        int packed =
                tag.contains("FluidFaceModes")
                        ? tag.getInt("FluidFaceModes")
                        : defaultFluidFaceModesPacked();
        for (Direction direction : Direction.values()) {
            int ordinal = (packed >> (direction.ordinal() * 2)) & 3;
            fluidFaceModes[direction.ordinal()] =
                    ordinal < FluidFaceMode.values().length
                            ? FluidFaceMode.values()[ordinal]
                            : FluidFaceMode.DISABLED;
        }
        autoPullFluid = tag.getBoolean("AutoPullFluid");
        autoPushFluid = tag.getBoolean("AutoPushFluid");
        meNetwork = tag.getBoolean("MeNetwork");
        inputFluidLocked = tag.getBoolean("InputFluidLocked");
        Fluid savedLockedFluid = BuiltInRegistries.FLUID.byId(tag.getInt("LockedInputFluid"));
        lockedInputFluid =
                savedLockedFluid == null || savedLockedFluid == Fluids.EMPTY
                        ? (inputFluidLocked && !inputTank.isEmpty()
                                ? inputTank.getFluid().getFluid()
                                : Fluids.EMPTY)
                        : savedLockedFluid;
        cycle.<ItemStack>loadPersistent(tag, id -> MythicCrucibleRecipes.get(id));
    }

    @Override
    public <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null) return dualFluidCapability.cast();
            FluidFaceMode mode = getFluidFaceMode(side);
            return mode == FluidFaceMode.DISABLED
                    ? LazyOptional.empty()
                    : faceFluidCapabilities.get(side).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        dualFluidCapability.invalidate();
        faceFluidCapabilities.values().forEach(LazyOptional::invalidate);
    }

    private record DualTankAccess(FluidTank input, FluidTank output) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? input.getFluid() : output.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? input.getCapacity() : output.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && input.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return input.fill(stack, action);
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return output.drain(stack, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return output.drain(amount, action);
        }
    }

    private record FaceFluidAccess(MythicCrucibleBlockEntity crucible, Direction direction)
            implements IFluidHandler {
        private FluidFaceMode mode() {
            return crucible.getFluidFaceMode(direction);
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? crucible.inputTank.getFluid() : crucible.outputTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? crucible.inputTank.getCapacity() : crucible.outputTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && acceptsInput(mode()) && crucible.inputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return acceptsInput(mode()) ? crucible.inputTank.fill(stack, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return acceptsOutput(mode())
                    ? crucible.outputTank.drain(stack, action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return acceptsOutput(mode())
                    ? crucible.outputTank.drain(amount, action)
                    : FluidStack.EMPTY;
        }
    }

    public enum FluidFaceMode {
        DISABLED,
        INPUT,
        OUTPUT,
        INPUT_OUTPUT
    }
}
