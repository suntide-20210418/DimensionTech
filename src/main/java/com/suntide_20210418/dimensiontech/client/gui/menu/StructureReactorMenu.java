package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorSequenceTelemetry;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public final class StructureReactorMenu extends AbstractContainerMenu
        implements OutputFaceConfigMenu {
    private final StructureReactorBlockEntity reactor;
    private final ContainerData data;
    private final Player viewer;
    private ReactorTooltipSnapshot tooltipSnapshot = ReactorTooltipSnapshot.empty();
    private int tooltipRevision = -1;

    public StructureReactorMenu(int id, Inventory player, RegistryFriendlyByteBuf data) {
        this(id, player, get(player, data.readBlockPos()));
    }

    public StructureReactorMenu(int id, Inventory player, StructureReactorBlockEntity reactor) {
        super(ModMenu.STRUCTURE_REACTOR.get(), id);
        this.reactor = reactor;
        this.viewer = player.player;
        addSlot(
                new SlotItemHandler(
                        reactor.inventory(),
                        StructureReactorBlockEntity.FRAGMENT_SLOT,
                        StructureReactorLayout.FRAGMENT_SLOT.x(),
                        StructureReactorLayout.FRAGMENT_SLOT.y()));
        addSlot(
                new SlotItemHandler(
                        reactor.inventory(),
                        StructureReactorBlockEntity.OPERATION_SLOT,
                        StructureReactorLayout.OPERATION_SLOT.x(),
                        StructureReactorLayout.OPERATION_SLOT.y()));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(
                        new Slot(
                                player,
                                column + row * 9 + 9,
                                StructureReactorLayout.playerSlotX(column),
                                StructureReactorLayout.playerSlotY(row)));
        for (int column = 0; column < 9; column++)
            addSlot(
                    new Slot(
                            player,
                            column,
                            StructureReactorLayout.hotbarSlotX(column),
                            StructureReactorLayout.hotbarSlotY()));
        data = reactor.data();
        addDataSlots(data);
    }

    private static StructureReactorBlockEntity get(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (!(entity instanceof StructureReactorBlockEntity reactor))
            throw new IllegalStateException("Missing structure reactor at " + pos);
        return reactor;
    }

    public StructureReactorBlockEntity reactor() {
        return reactor;
    }

    public int status() {
        return data.get(StructureReactorBlockEntity.DATA_STATUS);
    }

    public int stateTicks() {
        return data.get(StructureReactorBlockEntity.DATA_STATE_TICKS);
    }

    public int stateIndex() {
        return data.get(StructureReactorBlockEntity.DATA_STATE_INDEX);
    }

    /** Settlement outcome of the sequence step at {@code index}, or NONE if not settled yet. */
    public StructureReactorCycle.Resolution stateOutcome(int index) {
        var outcomes = tooltipSnapshot.stateOutcomes();
        return index >= 0 && index < outcomes.size()
                ? outcomes.get(index)
                : StructureReactorCycle.Resolution.NONE;
    }

    public int inputAmount() {
        return data.get(StructureReactorBlockEntity.DATA_INPUT_AMOUNT);
    }

    public int outputAmount() {
        return data.get(StructureReactorBlockEntity.DATA_OUTPUT_AMOUNT);
    }

    public int inputCapacity() {
        return StructureReactorBlockEntity.FLUID_TANK_CAPACITY_MB;
    }

    public int outputCapacity() {
        return StructureReactorBlockEntity.FLUID_TANK_CAPACITY_MB;
    }

    public Fluid inputFluid() {
        return fluid(data.get(StructureReactorBlockEntity.DATA_INPUT_FLUID));
    }

    public Fluid outputFluid() {
        return fluid(data.get(StructureReactorBlockEntity.DATA_OUTPUT_FLUID));
    }

    /** The state the reactor is currently waiting on, or {@code null} while idle. */
    @Nullable
    public StateId currentState() {
        int ordinal = data.get(StructureReactorBlockEntity.DATA_CURRENT_STATE);
        StateId[] values = StateId.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** Number of ritual steps in the active recipe's sequence, or 0 while idle. */
    public int sequenceLength() {
        return data.get(StructureReactorBlockEntity.DATA_SEQUENCE_LENGTH);
    }

    /** The recipe's real state at {@code index}, resolved from the packed sequence words. */
    public StateId sequenceState(int index) {
        return ReactorSequenceTelemetry.stateAt(
                data.get(StructureReactorBlockEntity.DATA_SEQUENCE_WORD_0),
                data.get(StructureReactorBlockEntity.DATA_SEQUENCE_WORD_1),
                sequenceLength(),
                index);
    }

    public ReactorTooltipSnapshot tooltipSnapshot() {
        return tooltipSnapshot;
    }

    public void applyTooltipSnapshot(ReactorTooltipSnapshot snapshot) {
        if (snapshot != null && snapshot.revision() >= tooltipRevision) {
            tooltipSnapshot = snapshot;
            tooltipRevision = snapshot.revision();
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof net.minecraft.server.level.ServerPlayer player)) return;
        ReactorTooltipSnapshot current = reactor.tooltipSnapshot();
        if (current.contentEquals(tooltipSnapshot)) return;
        ReactorTooltipSnapshot packetSnapshot = current.withRevision(++tooltipRevision);
        tooltipSnapshot = packetSnapshot;
        com.suntide_20210418.dimensiontech.network.ModNetwork.sendReactorTooltipSnapshot(
                player, containerId, packetSnapshot);
    }

    /**
     * The last settle that was not a no-op, or {@code null} when nothing has happened yet. It tells
     * the screen whether a reward or a penalty was just triggered.
     */
    @Nullable
    public StructureReactorCycle.Resolution lastResolution() {
        int ordinal = data.get(StructureReactorBlockEntity.DATA_LAST_RESOLUTION);
        StructureReactorCycle.Resolution[] values = StructureReactorCycle.Resolution.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** The state that the last resolution settled, or {@code null} when none has. */
    @Nullable
    public StateId eventState() {
        int ordinal = data.get(StructureReactorBlockEntity.DATA_EVENT_STATE);
        StateId[] values = StateId.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** Rewards accumulated by the cycle in progress, in ticks. */
    public int timeReduction() {
        return data.get(StructureReactorBlockEntity.DATA_TIME_REDUCTION);
    }

    /** Time penalties accumulated by the cycle in progress, in ticks. */
    public int timePenalty() {
        return data.get(StructureReactorBlockEntity.DATA_TIME_PENALTY);
    }

    public int fluidReductionBp() {
        return data.get(StructureReactorBlockEntity.DATA_FLUID_REDUCTION_BP);
    }

    public int fluidPenaltyBp() {
        return data.get(StructureReactorBlockEntity.DATA_FLUID_PENALTY_BP);
    }

    public int outputBonusBp() {
        return data.get(StructureReactorBlockEntity.DATA_OUTPUT_BONUS_BP);
    }

    public int outputPenaltyBp() {
        return data.get(StructureReactorBlockEntity.DATA_OUTPUT_PENALTY_BP);
    }

    public int extraFragments() {
        return data.get(StructureReactorBlockEntity.DATA_EXTRA_FRAGMENTS);
    }

    /** The settlement the cycle in progress would produce right now. */
    public int resultTimeTicks() {
        return data.get(StructureReactorBlockEntity.DATA_RESULT_TIME);
    }

    /** Ticks the cycle in progress has run, across every state it settled so far. */
    public int elapsedTicks() {
        return data.get(StructureReactorBlockEntity.DATA_ELAPSED_TICKS);
    }

    public int refiningTicks() {
        return data.get(StructureReactorBlockEntity.DATA_REFINING_TICKS);
    }

    public boolean autoPullFluid() {
        return data.get(StructureReactorBlockEntity.DATA_AUTO_PULL) != 0;
    }

    public boolean autoPushFluid() {
        return data.get(StructureReactorBlockEntity.DATA_AUTO_PUSH) != 0;
    }

    public boolean meNetwork() {
        return data.get(StructureReactorBlockEntity.DATA_ME_NETWORK) != 0;
    }

    public boolean isRedstoneControlEnabled() {
        return data.get(StructureReactorBlockEntity.DATA_REDSTONE) != 0;
    }

    public boolean inputFluidLocked() {
        return data.get(StructureReactorBlockEntity.DATA_INPUT_FLUID_LOCKED) != 0;
    }

    public StructureReactorBlockEntity.FluidFaceMode fluidFaceMode(Direction direction) {
        int packed = data.get(StructureReactorBlockEntity.DATA_FLUID_FACE_MODES);
        int ordinal = (packed >> (direction.ordinal() * 2)) & 3;
        return StructureReactorBlockEntity.FluidFaceMode.values()[ordinal];
    }

    public int resultFluidMb() {
        return data.get(StructureReactorBlockEntity.DATA_RESULT_FLUID);
    }

    public int resultOutputMb() {
        return data.get(StructureReactorBlockEntity.DATA_RESULT_OUTPUT);
    }

    private static Fluid fluid(int registryId) {
        if (registryId <= 0) return Fluids.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.byId(registryId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(reactor.getBlockPos()) == reactor
                && player.distanceToSqr(
                                reactor.getBlockPos().getX() + .5,
                                reactor.getBlockPos().getY() + .5,
                                reactor.getBlockPos().getZ() + .5)
                        <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == StructureReactorBlockEntity.BUTTON_TOGGLE_AUTO_PULL) {
            if (!player.level().isClientSide) reactor.toggleAutoPullFluid();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_TOGGLE_AUTO_PUSH) {
            if (!player.level().isClientSide) reactor.toggleAutoPushFluid();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_TOGGLE_ME_NETWORK) {
            if (!player.level().isClientSide) reactor.toggleMeNetwork();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_TOGGLE_INPUT_LOCK) {
            if (!player.level().isClientSide) reactor.toggleInputFluidLock();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_CLEAR_INPUT_TANK) {
            if (!player.level().isClientSide) reactor.clearInputTank();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_CLEAR_OUTPUT_TANK) {
            if (!player.level().isClientSide) reactor.clearOutputTank();
            return true;
        }
        if (id == StructureReactorBlockEntity.BUTTON_TOGGLE_REDSTONE_CONTROL) {
            if (!player.level().isClientSide) reactor.toggleRedstoneControl();
            return true;
        }
        int face = id - StructureReactorBlockEntity.BUTTON_CYCLE_FLUID_FACE_BASE;
        if (face >= 0 && face < Direction.values().length) {
            if (!player.level().isClientSide) reactor.cycleFluidFace(Direction.values()[face]);
            return true;
        }
        return false;
    }

    // --- OutputFaceConfigMenu -------------------------------------------------

    @Override
    public int containerId() {
        return containerId;
    }

    /** True when the side is set to push output fluid (OUTPUT or INPUT_OUTPUT). */
    @Override
    public boolean isOutputFaceEnabled(Direction d) {
        StructureReactorBlockEntity.FluidFaceMode mode = fluidFaceMode(d);
        return mode == StructureReactorBlockEntity.FluidFaceMode.OUTPUT
                || mode == StructureReactorBlockEntity.FluidFaceMode.INPUT_OUTPUT;
    }

    /** True when the side is set to receive input fluid (INPUT or INPUT_OUTPUT). */
    @Override
    public boolean isInputFaceEnabled(Direction d) {
        StructureReactorBlockEntity.FluidFaceMode mode = fluidFaceMode(d);
        return mode == StructureReactorBlockEntity.FluidFaceMode.INPUT
                || mode == StructureReactorBlockEntity.FluidFaceMode.INPUT_OUTPUT;
    }

    @Override
    public boolean isModernModeEnabled() {
        return meNetwork();
    }

    @Override
    public boolean isAutoExtractEnabled() {
        return autoPullFluid();
    }

    @Override
    public void cycleOutputFace(Direction d) {
        net.minecraft.client.Minecraft.getInstance()
                .gameMode
                .handleInventoryButtonClick(
                        containerId,
                        StructureReactorBlockEntity.BUTTON_CYCLE_FLUID_FACE_BASE + d.ordinal());
    }

    @Override
    public void cycleModernMode() {
        net.minecraft.client.Minecraft.getInstance()
                .gameMode
                .handleInventoryButtonClick(
                        containerId, StructureReactorBlockEntity.BUTTON_TOGGLE_ME_NETWORK);
    }

    @Override
    public void cycleAutoExtract() {
        net.minecraft.client.Minecraft.getInstance()
                .gameMode
                .handleInventoryButtonClick(
                        containerId, StructureReactorBlockEntity.BUTTON_TOGGLE_AUTO_PULL);
    }

    @Override
    public BlockPos getBlockPos() {
        return reactor.getBlockPos();
    }

    /** The reactor's fluid faces are world-oriented, so there is no logical-to-world remap. */
    @Override
    public Direction toWorldDirection(Direction d) {
        return d;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
