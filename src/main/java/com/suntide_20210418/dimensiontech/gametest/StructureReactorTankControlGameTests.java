package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
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
public final class StructureReactorTankControlGameTests {
    private static final BlockPos REACTOR = new BlockPos(1, 2, 1);
    private static final int BUCKET = 1_000;

    private StructureReactorTankControlGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingEachTankLeavesTheOtherAlone(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = place(helper);
        reactor.inputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        reactor.outputTank().fill(new FluidStack(Fluids.LAVA, BUCKET), FluidAction.EXECUTE);

        reactor.clearInputTank();
        helper.assertTrue(reactor.inputTank().isEmpty(), "the input tank must be emptied");
        helper.assertTrue(
                reactor.outputTank().getFluidAmount() == BUCKET,
                "clearing the input must leave the output tank alone");

        reactor.clearOutputTank();
        helper.assertTrue(reactor.outputTank().isEmpty(), "the output tank must be emptied");
        helper.succeed();
    }

    /**
     * The input lock fixes which fluid the tank accepts, so emptying the tank must not clear it.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingTheInputTankKeepsItsFluidLock(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = place(helper);
        reactor.inputTank().fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE);
        reactor.toggleInputFluidLock();

        reactor.clearInputTank();
        helper.assertTrue(reactor.inputTank().isEmpty(), "the input tank must be emptied");
        helper.assertTrue(reactor.isInputFluidLocked(), "the input lock must survive the clear");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void clearingAnEmptyTankChangesNothing(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = place(helper);

        reactor.clearInputTank();
        reactor.clearOutputTank();
        helper.assertTrue(reactor.inputTank().isEmpty(), "an empty input tank stays empty");
        helper.assertTrue(reactor.outputTank().isEmpty(), "an empty output tank stays empty");
        helper.succeed();
    }

    private static StructureReactorBlockEntity place(GameTestHelper helper) {
        helper.setBlock(REACTOR, ModBlocks.STRUCTURE_REACTOR.get());
        BlockEntity blockEntity = helper.getBlockEntity(REACTOR);
        helper.assertTrue(
                blockEntity instanceof StructureReactorBlockEntity,
                "the placed block must carry a reactor block entity");
        return (StructureReactorBlockEntity) blockEntity;
    }
}
