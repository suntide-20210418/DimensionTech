package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The held-container transfer: a fluid container feeds the input tank, an empty one draws the
 * output tank, and a container that already holds the output fluid only tops itself up. Each
 * interaction is meant to move as much as both sides allow.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicCrucibleFluidContainerGameTests {
    private static final BlockPos CRUCIBLE = new BlockPos(1, 2, 1);
    private static final int BUCKET = 1_000;

    private MythicCrucibleFluidContainerGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void aFilledContainerFeedsTheInputTank(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        IFluidHandlerItem water = container(new ItemStack(Items.WATER_BUCKET));

        helper.assertTrue(
                crucible.exchangeWithFluidContainer(water), "the transfer must move fluid");
        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == BUCKET,
                "the input tank must hold the bucket, held "
                        + crucible.inputTank().getFluidAmount());
        helper.assertTrue(
                crucible.inputTank().getFluid().getFluid() == Fluids.WATER,
                "the input tank must hold water");
        helper.assertTrue(
                water.getContainer().is(Items.BUCKET), "the spent bucket must come back empty");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anEmptyContainerDrawsTheOutputTank(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        crucible.outputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        IFluidHandlerItem bucket = container(new ItemStack(Items.BUCKET));

        helper.assertTrue(
                crucible.exchangeWithFluidContainer(bucket), "the transfer must move fluid");
        helper.assertTrue(
                crucible.outputTank().isEmpty(),
                "the output tank must be emptied, left " + crucible.outputTank().getFluidAmount());
        helper.assertTrue(
                bucket.getContainer().is(Items.WATER_BUCKET), "the bucket must come back filled");
        helper.succeed();
    }

    /**
     * Both tanks hold fluid and so does the container: matching the input feeds the input, matching
     * the output tops up the container, and a fluid matching neither does nothing at all.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void aFilledContainerMatchesTheTankItMayUse(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        crucible.inputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        crucible.outputTank().fill(new FluidStack(Fluids.LAVA, BUCKET), FluidAction.EXECUTE);

        IFluidHandlerItem water = container(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                crucible.exchangeWithFluidContainer(water),
                "a container matching the input fluid must feed the input tank");
        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == BUCKET * 2,
                "the input tank must take the second bucket, held "
                        + crucible.inputTank().getFluidAmount());
        helper.assertTrue(
                crucible.outputTank().getFluidAmount() == BUCKET,
                "feeding the input must not touch the output tank");

        IFluidHandlerItem lava = container(new ItemStack(Items.LAVA_BUCKET));
        helper.assertFalse(
                crucible.exchangeWithFluidContainer(lava),
                "a container matching neither tank must not move anything");
        helper.assertTrue(
                lava.getContainer().is(Items.LAVA_BUCKET),
                "a refused container must stay untouched");
        helper.succeed();
    }

    /**
     * A vanilla bucket cannot hold a partial amount, so a tank with too little room must refuse it
     * outright rather than lose the fluid that would not fit.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void aBucketThatWouldNotFitIsRefusedWhole(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        int room = 500;
        crucible.inputTank()
                .fill(
                        new FluidStack(
                                Fluids.WATER,
                                MythicCrucibleBlockEntity.FLUID_TANK_CAPACITY_MB - room),
                        FluidAction.EXECUTE);
        IFluidHandlerItem water = container(new ItemStack(Items.WATER_BUCKET));

        helper.assertFalse(
                crucible.exchangeWithFluidContainer(water),
                "a bucket that does not fit must not pour part of its contents");
        helper.assertTrue(
                water.getContainer().is(Items.WATER_BUCKET), "a refused bucket must stay full");
        helper.assertTrue(
                crucible.inputTank().getFluidAmount()
                        == MythicCrucibleBlockEntity.FLUID_TANK_CAPACITY_MB - room,
                "a refused bucket must not change the tank");
        helper.succeed();
    }

    /** The transfer is bounded by the smaller side, here the output tank. */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void transfersAreLimitedByTheSmallerSide(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        int partial = 400;
        crucible.outputTank().fill(new FluidStack(Fluids.WATER, partial), FluidAction.EXECUTE);
        IFluidHandlerItem container = new PartialContainer(BUCKET);

        helper.assertTrue(
                crucible.exchangeWithFluidContainer(container), "the transfer must move fluid");
        helper.assertTrue(
                crucible.outputTank().isEmpty(), "the output tank must be drained completely");
        helper.assertTrue(
                container.getFluidInTank(0).getAmount() == partial,
                "the container must hold exactly what the output tank had, held "
                        + container.getFluidInTank(0).getAmount());
        helper.succeed();
    }

    /**
     * A container that already holds the output fluid is topped up from the output tank, which is
     * the only way it can take anything once the input tank holds a different fluid.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void aPartlyFilledContainerIsToppedUpFromTheOutput(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        crucible.inputTank().fill(new FluidStack(Fluids.LAVA, BUCKET), FluidAction.EXECUTE);
        crucible.outputTank().fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        IFluidHandlerItem container = new PartialContainer(2_000);
        container.fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);

        helper.assertTrue(
                crucible.exchangeWithFluidContainer(container), "the transfer must move fluid");
        helper.assertTrue(
                crucible.outputTank().isEmpty(),
                "the output tank must be drained into the container, left "
                        + crucible.outputTank().getFluidAmount());
        helper.assertTrue(
                container.getFluidInTank(0).getAmount() == 1_500,
                "the container must be topped up, held " + container.getFluidInTank(0).getAmount());
        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == BUCKET,
                "the foreign input fluid must be left alone");
        helper.succeed();
    }

    private static MythicCrucibleBlockEntity place(GameTestHelper helper) {
        helper.setBlock(CRUCIBLE, ModBlocks.MYTHIC_CRUCIBLE.get());
        BlockEntity blockEntity = helper.getBlockEntity(CRUCIBLE);
        helper.assertTrue(
                blockEntity instanceof MythicCrucibleBlockEntity,
                "the placed block must carry a crucible block entity");
        return (MythicCrucibleBlockEntity) blockEntity;
    }

    private static IFluidHandlerItem container(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .resolve()
                .orElseThrow(() -> new AssertionError("the container must expose a fluid handler"));
    }

    /**
     * A container that, unlike a vanilla bucket, can hold a partial amount. Vanilla buckets refuse
     * anything but a whole bucket, so they cannot exercise the "as much as both sides allow" rule.
     */
    private static final class PartialContainer implements IFluidHandlerItem {
        private final int capacity;
        private FluidStack content = FluidStack.EMPTY;

        private PartialContainer(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public ItemStack getContainer() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return content;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (!content.isEmpty() && !content.isFluidEqual(resource)) return 0;
            int accepted = Math.min(capacity - content.getAmount(), resource.getAmount());
            if (accepted <= 0) return 0;
            if (action.execute())
                content = new FluidStack(resource, content.getAmount() + accepted);
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.isFluidEqual(content)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int drained = Math.min(maxDrain, content.getAmount());
            if (drained <= 0) return FluidStack.EMPTY;
            FluidStack result = new FluidStack(content, drained);
            if (action.execute()) content = new FluidStack(content, content.getAmount() - drained);
            return result;
        }
    }
}
