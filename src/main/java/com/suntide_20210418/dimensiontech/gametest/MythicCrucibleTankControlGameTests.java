package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The screen's shift-click "empty this tank" action, which voids whatever the tank held. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicCrucibleTankControlGameTests {
    private static final BlockPos CRUCIBLE = new BlockPos(1, 2, 1);
    private static final int BUCKET = 1_000;

    private MythicCrucibleTankControlGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingEachTankLeavesTheOtherAlone(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        crucible.inputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        crucible.outputTank().fill(new FluidStack(Fluids.LAVA, BUCKET), FluidAction.EXECUTE);

        crucible.clearInputTank();
        helper.assertTrue(crucible.inputTank().isEmpty(), "the input tank must be emptied");
        helper.assertTrue(
                crucible.outputTank().getFluidAmount() == BUCKET,
                "clearing the input must leave the output tank alone");

        crucible.clearOutputTank();
        helper.assertTrue(crucible.outputTank().isEmpty(), "the output tank must be emptied");
        helper.succeed();
    }

    /**
     * The input lock fixes which fluid the tank accepts, so emptying the tank must not clear it.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingTheInputTankKeepsItsFluidLock(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);
        crucible.inputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        crucible.toggleInputFluidLock();

        crucible.clearInputTank();
        helper.assertTrue(crucible.inputTank().isEmpty(), "the input tank must be emptied");
        helper.assertTrue(crucible.isInputFluidLocked(), "the input lock must survive the clear");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingAnEmptyTankChangesNothing(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = place(helper);

        crucible.clearInputTank();
        crucible.clearOutputTank();
        helper.assertTrue(crucible.inputTank().isEmpty(), "an empty input tank stays empty");
        helper.assertTrue(crucible.outputTank().isEmpty(), "an empty output tank stays empty");
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
}
