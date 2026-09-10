package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleFormula;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Single-block crucible: two 16,000 mB tanks, a fragment slot and an immediate operation slot. */
public final class MythicCrucibleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLUID_TANK_CAPACITY_MB = 16_000;
    public static final int FRAGMENT_SLOT = 0;
    public static final int OPERATION_SLOT = 1;
    private final MythicCrucibleCycle cycle = new MythicCrucibleCycle();
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FRAGMENT_SLOT || slot == OPERATION_SLOT;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidTank inputTank = tank();
    private final FluidTank outputTank = tank();
    private FluidStack reservedFluid = FluidStack.EMPTY;
    private ItemStack reservedFragments = ItemStack.EMPTY;
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private final LazyOptional<IFluidHandler> inputFluidCapability = LazyOptional.of(() -> new TankAccess(inputTank, true, false));
    private final LazyOptional<IFluidHandler> outputFluidCapability = LazyOptional.of(() -> new TankAccess(outputTank, false, true));

    public MythicCrucibleBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.MYTHIC_CRUCIBLE.get(), pos, state); }
    private FluidTank tank() { return new FluidTank(FLUID_TANK_CAPACITY_MB) { @Override protected void onContentsChanged() { setChanged(); } }; }
    public IItemHandler inventory() { return inventory; }
    public FluidTank inputTank() { return inputTank; }
    public FluidTank outputTank() { return outputTank; }
    public MythicCrucibleCycle cycle() { return cycle; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MythicCrucibleBlockEntity be) {
        if (cycleNeedsStart(be)) be.tryStart();
        if (be.cycle.status() == MythicCrucibleCycle.Status.RUNNING) {
            be.cycle.tick();
            ItemStack operation = be.inventory.getStackInSlot(OPERATION_SLOT);
            MythicCrucibleCycle.Resolution result = be.cycle.resolve(operation);
            if (result.consumesInput()) be.inventory.extractItem(OPERATION_SLOT, 1, false);
            be.setChanged();
        }
        if (be.cycle.status() == MythicCrucibleCycle.Status.READY_TO_COMMIT) be.tryCommit();
    }
    private static boolean cycleNeedsStart(MythicCrucibleBlockEntity be) { return be.cycle.status() == MythicCrucibleCycle.Status.IDLE; }

    private void tryStart() {
        MythicCrucibleRecipe recipe = MythicCrucibleRecipes.firstMatching(inputTank.getFluid().getFluid());
        if (recipe == null || inputTank.getFluidAmount() < recipe.baseFluidCost()) return;
        ItemStack fragments = inventory.getStackInSlot(FRAGMENT_SLOT);
        if (!recipe.fragment().test(fragments) || fragments.getCount() < recipe.fragmentCount()) return;
        if (outputTank.getCapacity() - outputTank.getFluidAmount() < recipe.targetOutput()) return;
        FluidStack extracted = inputTank.drain(recipe.baseFluidCost(), IFluidHandler.FluidAction.EXECUTE);
        if (extracted.getAmount() != recipe.baseFluidCost()) return;
        reservedFluid = extracted;
        reservedFragments = inventory.extractItem(FRAGMENT_SLOT, recipe.fragmentCount(), false);
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
        if (additionalFluid > 0 && (!inputTank.getFluid().getFluid().isSame(recipe.input()) || inputTank.getFluidAmount() < additionalFluid)) return;
        if (additionalFragments > 0 && (!recipe.fragment().test(fragments) || fragments.getCount() < additionalFragments)) return;
        if (!outputTank.isEmpty() && !outputTank.getFluid().getFluid().isSame(recipe.output())) return;
        if (outputTank.getCapacity() - outputTank.getFluidAmount() < result.outputAmountMb()) return;
        // All preconditions above are checked before any irreversible mutation.
        if (additionalFluid > 0) inputTank.drain(additionalFluid, IFluidHandler.FluidAction.EXECUTE);
        if (refundFluid > 0) inputTank.fill(new FluidStack(recipe.input(), refundFluid), IFluidHandler.FluidAction.EXECUTE);
        if (additionalFragments > 0) inventory.extractItem(FRAGMENT_SLOT, additionalFragments, false);
        outputTank.fill(new FluidStack(recipe.output(), result.outputAmountMb()), IFluidHandler.FluidAction.EXECUTE);
        reservedFluid = FluidStack.EMPTY;
        reservedFragments = ItemStack.EMPTY;
        cycle.commit();
        setChanged();
    }

    /** Used by block replacement and recipe changes; reserved resources are restored before reset. */
    public void abortAndReturnResources() {
        if (!reservedFluid.isEmpty()) inputTank.fill(reservedFluid, IFluidHandler.FluidAction.EXECUTE);
        if (!reservedFragments.isEmpty()) {
            ItemStack remainder = inventory.insertItem(FRAGMENT_SLOT, reservedFragments, false);
            if (!remainder.isEmpty() && level != null)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
        }
        reservedFluid = FluidStack.EMPTY;
        reservedFragments = ItemStack.EMPTY;
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
                        level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.dimension_tech.mythic_crucible"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) { return new MythicCrucibleMenu(id, player, this); }
    public ContainerData data() { return new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> cycle.status().ordinal(); case 1 -> cycle.stateTicks(); case 2 -> cycle.stateIndex();
            case 3 -> inputTank.getFluidAmount(); case 4 -> outputTank.getFluidAmount();
            case 5 -> cycle.currentState() == null ? -1 : cycle.currentState().ordinal(); default -> 0; }; }
        @Override public void set(int i, int value) {}
        @Override public int getCount() { return 6; }
    }; }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("CrucibleItems", inventory.serializeNBT());
        tag.put("CrucibleInput", inputTank.writeToNBT(new CompoundTag())); tag.put("CrucibleOutput", outputTank.writeToNBT(new CompoundTag()));
        tag.put("CrucibleReservedFluid", reservedFluid.writeToNBT(new CompoundTag()));
        tag.put("CrucibleReservedFragments", reservedFragments.save(new CompoundTag())); cycle.save(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); inventory.deserializeNBT(tag.getCompound("CrucibleItems")); inputTank.readFromNBT(tag.getCompound("CrucibleInput")); outputTank.readFromNBT(tag.getCompound("CrucibleOutput"));
        reservedFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("CrucibleReservedFluid")); reservedFragments = ItemStack.of(tag.getCompound("CrucibleReservedFragments")); cycle.loadPersistent(tag);
    }

    @Override public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return (side == Direction.DOWN ? outputFluidCapability : inputFluidCapability).cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); inputFluidCapability.invalidate(); outputFluidCapability.invalidate(); }

    private record TankAccess(FluidTank tank, boolean canFill, boolean canDrain) implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return this.tank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return this.tank.getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return canFill && this.tank.isFluidValid(stack); }
        @Override public int fill(FluidStack stack, FluidAction action) { return canFill ? tank.fill(stack, action) : 0; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return canDrain ? tank.drain(stack, action) : FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return canDrain ? tank.drain(amount, action) : FluidStack.EMPTY; }
    }
}
