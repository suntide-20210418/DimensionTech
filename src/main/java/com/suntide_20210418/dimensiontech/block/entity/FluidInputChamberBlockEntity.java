package com.suntide_20210418.dimensiontech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The miner's working-fluid tank, carried by the fluid input chamber.
 *
 * <p>One tank of {@link BaseMinerBlockEntity#FLUID_TANK_CAPACITY_MB} mB, valid only for the fluid
 * the owning miner's tier consumes. The chamber exposes that tank to pipes as fill-only ({@code
 * Capabilities.FluidHandler.BLOCK}) and can pull the required fluid out of adjacent containers when
 * its auto-pull is on.
 */
public final class FluidInputChamberBlockEntity extends MinerChamberBlockEntity {
    private static final String TANK_TAG = "Tank";
    private static final String AUTO_PULL_TAG = "AutoPull";

    private final FluidTank tank =
            new FluidTank(BaseMinerBlockEntity.FLUID_TANK_CAPACITY_MB) {
                @Override
                public boolean isFluidValid(FluidStack stack) {
                    BaseMinerBlockEntity owner = miner();
                    return owner != null
                            && owner.requiresFluidInput()
                            && stack.getFluid() == owner.getRequiredFluid();
                }

                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };

    private final IFluidHandler inputHandler = createInputHandler();
    private boolean autoPull;

    public FluidInputChamberBlockEntity(BlockPos position, BlockState blockState) {
        super(ModBlockEntities.FLUID_INPUT_CHAMBER.get(), position, blockState);
    }

    public FluidTank tank() {
        return tank;
    }

    /** Fill-only view of the tank, used as the block capability a pipe sees. */
    public IFluidHandler inputHandler() {
        return inputHandler;
    }

    public boolean isAutoPullEnabled() {
        return autoPull;
    }

    /** Sets auto-pull directly; used by the script API, which states a desired value. */
    public void setAutoPull(boolean enabled) {
        if (autoPull == enabled) return;
        autoPull = enabled;
        setChanged();
    }

    @Override
    public void chamberTick(BaseMinerBlockEntity miner) {
        if (autoPull && level instanceof ServerLevel serverLevel) {
            pullFromNeighbours(serverLevel);
        }
    }

    /** Plain right-click reports the contents; shift+right-click flips auto-pull. */
    public void handleUse(Player player) {
        if (player.isShiftKeyDown()) {
            autoPull = !autoPull;
            setChanged();
            player.displayClientMessage(
                    Component.translatable(
                            autoPull
                                    ? "message.dimension_tech.fluid_input_chamber.auto_pull_on"
                                    : "message.dimension_tech.fluid_input_chamber.auto_pull_off",
                            tank.getFluidAmount(),
                            tank.getCapacity()),
                    false);
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "message.dimension_tech.fluid_input_chamber.contents",
                        tank.getFluidAmount(),
                        tank.getCapacity()),
                false);
    }

    /**
     * Moves fluid between a held container and the tank on direct interaction. A filled container
     * pours in when the tank is empty or already holds that fluid; failing that, an empty (or
     * matching) container draws the stored fluid back out.
     *
     * @return true when any fluid actually moved, so the caller knows to re-place the container
     */
    public boolean exchangeWithFluidContainer(IFluidHandlerItem container) {
        if (container.getTanks() < 1) return false;
        FluidStack held = container.getFluidInTank(0);
        boolean input = !held.isEmpty() && pourIntoFrom(container, held);
        boolean output = !input && scoopIntoContainer(container);
        if (input || output) {
            setChanged();
            playFluidTransferSound(input);
        }
        return input || output;
    }

    /** Pours a filled container into the tank, up to the tank's remaining room. */
    private boolean pourIntoFrom(IFluidHandlerItem container, FluidStack held) {
        if (!tank.isEmpty() && !FluidStack.isSameFluidSameComponents(tank.getFluid(), held)) {
            return false;
        }
        int wanted = Math.min(tank.getCapacity() - tank.getFluidAmount(), held.getAmount());
        if (wanted <= 0 || !tank.isFluidValid(held)) return false;
        FluidStack drained = container.drain(wanted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return false;
        int accepted = tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (accepted < drained.getAmount()) {
            FluidStack refund = drained.copy();
            refund.setAmount(drained.getAmount() - accepted);
            container.fill(refund, IFluidHandler.FluidAction.EXECUTE);
        }
        return accepted > 0;
    }

    /** Draws the tank into a container that has room and will not end up with a mixture. */
    private boolean scoopIntoContainer(IFluidHandlerItem container) {
        if (tank.isEmpty()) return false;
        FluidStack stored = tank.getFluid();
        FluidStack held = container.getFluidInTank(0);
        if (!held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, stored)) return false;
        int room = container.getTankCapacity(0) - held.getAmount();
        if (room <= 0) return false;
        FluidStack offered =
                tank.drain(Math.min(room, stored.getAmount()), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return false;
        int accepted = container.fill(offered, IFluidHandler.FluidAction.EXECUTE);
        if (accepted <= 0) return false;
        tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void playFluidTransferSound(boolean pouringIn) {
        if (level == null) return;
        level.playSound(
                null,
                worldPosition,
                pouringIn ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
    }

    /**
     * Pulls the required fluid out of every adjacent handler that offers it, until the tank is
     * full. Only the machine's own fluid is accepted, so an auto-pulling chamber cannot be poisoned
     * by a neighbouring tank of something else.
     */
    private void pullFromNeighbours(ServerLevel level) {
        BaseMinerBlockEntity owner = miner();
        if (owner == null) return;
        Fluid required = owner.getRequiredFluid();
        if (required == null || !owner.requiresFluidInput()) return;
        int remaining = tank.getCapacity() - tank.getFluidAmount();
        if (remaining <= 0) return;
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) break;
            IFluidHandler handler =
                    level.getCapability(
                            Capabilities.FluidHandler.BLOCK,
                            worldPosition.relative(direction),
                            direction.getOpposite());
            if (handler == null) continue;
            FluidStack simulated =
                    handler.drain(
                            new FluidStack(required, remaining),
                            IFluidHandler.FluidAction.SIMULATE);
            int accepted = tank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            int filled = tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            remaining -= filled;
            if (filled > 0) setChanged();
        }
    }

    private IFluidHandler createInputHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return tank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int slot) {
                return tank.getFluidInTank(slot);
            }

            @Override
            public int getTankCapacity(int slot) {
                return tank.getTankCapacity(slot);
            }

            @Override
            public boolean isFluidValid(int slot, FluidStack stack) {
                return tank.isFluidValid(slot, stack);
            }

            @Override
            public int fill(FluidStack stack, FluidAction action) {
                return tank.fill(stack, action);
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TANK_TAG, tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean(AUTO_PULL_TAG, autoPull);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TANK_TAG, Tag.TAG_COMPOUND)) {
            tank.readFromNBT(registries, tag.getCompound(TANK_TAG));
            if (!tank.isEmpty() && !tank.isFluidValid(tank.getFluid())) {
                tank.setFluid(FluidStack.EMPTY);
            }
        }
        autoPull = tag.getBoolean(AUTO_PULL_TAG);
    }
}
