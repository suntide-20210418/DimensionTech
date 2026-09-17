package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleSequenceTelemetry;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleTooltipSnapshot;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public final class MythicCrucibleMenu extends AbstractContainerMenu {
    private final MythicCrucibleBlockEntity crucible;
    private final ContainerData data;
    private final Player viewer;
    private CrucibleTooltipSnapshot tooltipSnapshot = CrucibleTooltipSnapshot.empty();
    private int tooltipRevision = -1;

    public MythicCrucibleMenu(int id, Inventory player, FriendlyByteBuf data) {
        this(id, player, get(player, data.readBlockPos()));
    }

    public MythicCrucibleMenu(int id, Inventory player, MythicCrucibleBlockEntity crucible) {
        super(ModMenu.MYTHIC_CRUCIBLE.get(), id);
        this.crucible = crucible;
        this.viewer = player.player;
        addSlot(
                new SlotItemHandler(
                        crucible.inventory(),
                        MythicCrucibleBlockEntity.FRAGMENT_SLOT,
                        MythicCrucibleLayout.FRAGMENT_SLOT.x(),
                        MythicCrucibleLayout.FRAGMENT_SLOT.y()));
        addSlot(
                new SlotItemHandler(
                        crucible.inventory(),
                        MythicCrucibleBlockEntity.OPERATION_SLOT,
                        MythicCrucibleLayout.OPERATION_SLOT.x(),
                        MythicCrucibleLayout.OPERATION_SLOT.y()));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(
                        new Slot(
                                player,
                                column + row * 9 + 9,
                                MythicCrucibleLayout.playerSlotX(column),
                                MythicCrucibleLayout.playerSlotY(row)));
        for (int column = 0; column < 9; column++)
            addSlot(
                    new Slot(
                            player,
                            column,
                            MythicCrucibleLayout.hotbarSlotX(column),
                            MythicCrucibleLayout.hotbarSlotY()));
        data = crucible.data();
        addDataSlots(data);
    }

    private static MythicCrucibleBlockEntity get(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (!(entity instanceof MythicCrucibleBlockEntity crucible))
            throw new IllegalStateException("Missing mythic crucible at " + pos);
        return crucible;
    }

    public MythicCrucibleBlockEntity crucible() {
        return crucible;
    }

    public int status() {
        return data.get(MythicCrucibleBlockEntity.DATA_STATUS);
    }

    public int stateTicks() {
        return data.get(MythicCrucibleBlockEntity.DATA_STATE_TICKS);
    }

    public int stateIndex() {
        return data.get(MythicCrucibleBlockEntity.DATA_STATE_INDEX);
    }

    public int inputAmount() {
        return data.get(MythicCrucibleBlockEntity.DATA_INPUT_AMOUNT);
    }

    public int outputAmount() {
        return data.get(MythicCrucibleBlockEntity.DATA_OUTPUT_AMOUNT);
    }

    public int inputCapacity() {
        return MythicCrucibleBlockEntity.FLUID_TANK_CAPACITY_MB;
    }

    public int outputCapacity() {
        return MythicCrucibleBlockEntity.FLUID_TANK_CAPACITY_MB;
    }

    public Fluid inputFluid() {
        return fluid(data.get(MythicCrucibleBlockEntity.DATA_INPUT_FLUID));
    }

    public Fluid outputFluid() {
        return fluid(data.get(MythicCrucibleBlockEntity.DATA_OUTPUT_FLUID));
    }

    /** The state the crucible is currently waiting on, or {@code null} while idle. */
    @Nullable
    public StateId currentState() {
        int ordinal = data.get(MythicCrucibleBlockEntity.DATA_CURRENT_STATE);
        StateId[] values = StateId.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** Number of ritual steps in the active recipe's sequence, or 0 while idle. */
    public int sequenceLength() {
        return data.get(MythicCrucibleBlockEntity.DATA_SEQUENCE_LENGTH);
    }

    /** The recipe's real state at {@code index}, resolved from the packed sequence words. */
    public StateId sequenceState(int index) {
        return CrucibleSequenceTelemetry.stateAt(
                data.get(MythicCrucibleBlockEntity.DATA_SEQUENCE_WORD_0),
                data.get(MythicCrucibleBlockEntity.DATA_SEQUENCE_WORD_1),
                sequenceLength(),
                index);
    }

    public CrucibleTooltipSnapshot tooltipSnapshot() {
        return tooltipSnapshot;
    }

    public void applyTooltipSnapshot(CrucibleTooltipSnapshot snapshot) {
        if (snapshot != null && snapshot.revision() >= tooltipRevision) {
            tooltipSnapshot = snapshot;
            tooltipRevision = snapshot.revision();
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof net.minecraft.server.level.ServerPlayer player)) return;
        CrucibleTooltipSnapshot current = crucible.tooltipSnapshot();
        if (current.contentEquals(tooltipSnapshot)) return;
        CrucibleTooltipSnapshot packetSnapshot = current.withRevision(++tooltipRevision);
        tooltipSnapshot = packetSnapshot;
        com.suntide_20210418.dimensiontech.network.ModNetwork.sendCrucibleTooltipSnapshot(
                player, containerId, packetSnapshot);
    }

    /**
     * The last settle that was not a no-op, or {@code null} when nothing has happened yet. It tells
     * the screen whether a reward or a penalty was just triggered.
     */
    @Nullable
    public MythicCrucibleCycle.Resolution lastResolution() {
        int ordinal = data.get(MythicCrucibleBlockEntity.DATA_LAST_RESOLUTION);
        MythicCrucibleCycle.Resolution[] values = MythicCrucibleCycle.Resolution.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** The state that the last resolution settled, or {@code null} when none has. */
    @Nullable
    public StateId eventState() {
        int ordinal = data.get(MythicCrucibleBlockEntity.DATA_EVENT_STATE);
        StateId[] values = StateId.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    /** Rewards accumulated by the cycle in progress, in ticks. */
    public int timeReduction() {
        return data.get(MythicCrucibleBlockEntity.DATA_TIME_REDUCTION);
    }

    /** Time penalties accumulated by the cycle in progress, in ticks. */
    public int timePenalty() {
        return data.get(MythicCrucibleBlockEntity.DATA_TIME_PENALTY);
    }

    public int fluidReductionBp() {
        return data.get(MythicCrucibleBlockEntity.DATA_FLUID_REDUCTION_BP);
    }

    public int fluidPenaltyBp() {
        return data.get(MythicCrucibleBlockEntity.DATA_FLUID_PENALTY_BP);
    }

    public int outputBonusBp() {
        return data.get(MythicCrucibleBlockEntity.DATA_OUTPUT_BONUS_BP);
    }

    public int outputPenaltyBp() {
        return data.get(MythicCrucibleBlockEntity.DATA_OUTPUT_PENALTY_BP);
    }

    public int extraFragments() {
        return data.get(MythicCrucibleBlockEntity.DATA_EXTRA_FRAGMENTS);
    }

    /** The settlement the cycle in progress would produce right now. */
    public int resultTimeTicks() {
        return data.get(MythicCrucibleBlockEntity.DATA_RESULT_TIME);
    }

    /** Ticks the cycle in progress has run, across every state it settled so far. */
    public int elapsedTicks() {
        return data.get(MythicCrucibleBlockEntity.DATA_ELAPSED_TICKS);
    }

    public int refiningTicks() {
        return data.get(MythicCrucibleBlockEntity.DATA_REFINING_TICKS);
    }

    public boolean autoPullFluid() {
        return data.get(MythicCrucibleBlockEntity.DATA_AUTO_PULL) != 0;
    }

    public boolean autoPushFluid() {
        return data.get(MythicCrucibleBlockEntity.DATA_AUTO_PUSH) != 0;
    }

    public boolean meNetwork() {
        return data.get(MythicCrucibleBlockEntity.DATA_ME_NETWORK) != 0;
    }

    public boolean inputFluidLocked() {
        return data.get(MythicCrucibleBlockEntity.DATA_INPUT_FLUID_LOCKED) != 0;
    }

    public MythicCrucibleBlockEntity.FluidFaceMode fluidFaceMode(Direction direction) {
        int packed = data.get(MythicCrucibleBlockEntity.DATA_FLUID_FACE_MODES);
        int ordinal = (packed >> (direction.ordinal() * 2)) & 3;
        return MythicCrucibleBlockEntity.FluidFaceMode.values()[ordinal];
    }

    public int resultFluidMb() {
        return data.get(MythicCrucibleBlockEntity.DATA_RESULT_FLUID);
    }

    public int resultOutputMb() {
        return data.get(MythicCrucibleBlockEntity.DATA_RESULT_OUTPUT);
    }

    private static Fluid fluid(int registryId) {
        if (registryId <= 0) return Fluids.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.byId(registryId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(crucible.getBlockPos()) == crucible
                && player.distanceToSqr(
                                crucible.getBlockPos().getX() + .5,
                                crucible.getBlockPos().getY() + .5,
                                crucible.getBlockPos().getZ() + .5)
                        <= 64;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == MythicCrucibleBlockEntity.BUTTON_TOGGLE_AUTO_PULL) {
            if (!player.level().isClientSide) crucible.toggleAutoPullFluid();
            return true;
        }
        if (id == MythicCrucibleBlockEntity.BUTTON_TOGGLE_AUTO_PUSH) {
            if (!player.level().isClientSide) crucible.toggleAutoPushFluid();
            return true;
        }
        if (id == MythicCrucibleBlockEntity.BUTTON_TOGGLE_ME_NETWORK) {
            if (!player.level().isClientSide) crucible.toggleMeNetwork();
            return true;
        }
        if (id == MythicCrucibleBlockEntity.BUTTON_TOGGLE_INPUT_LOCK) {
            if (!player.level().isClientSide) crucible.toggleInputFluidLock();
            return true;
        }
        if (id == MythicCrucibleBlockEntity.BUTTON_CLEAR_INPUT_TANK) {
            if (!player.level().isClientSide) crucible.clearInputTank();
            return true;
        }
        if (id == MythicCrucibleBlockEntity.BUTTON_CLEAR_OUTPUT_TANK) {
            if (!player.level().isClientSide) crucible.clearOutputTank();
            return true;
        }
        int face = id - MythicCrucibleBlockEntity.BUTTON_CYCLE_FLUID_FACE_BASE;
        if (face >= 0 && face < Direction.values().length) {
            if (!player.level().isClientSide) crucible.cycleFluidFace(Direction.values()[face]);
            return true;
        }
        return false;
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
