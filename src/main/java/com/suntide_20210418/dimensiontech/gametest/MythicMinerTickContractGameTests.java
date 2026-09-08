package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.integration.kubejs.MinerBlockEntityJS;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Server-side stop conditions from the miner tick contract. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerTickContractGameTests {
    private MythicMinerTickContractGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void incompleteStructureDoesNotConsumeEnergy(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        miner.getEnergyStorage().receiveEnergy(10_000, false);
        int before = miner.getEnergyStored();

        miner.serverTick();

        if (miner.getEnergyStored() != before || miner.isStructureComplete()) {
            helper.fail("Incomplete miner consumed energy or reported a completed structure");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void completeStructureWithoutMarkerDoesNotConsumeEnergy(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        MythicMinerMultiblock.place(level, miner.getBlockPos(), miner.getMinerTier());
        miner.getEnergyStorage().receiveEnergy(10_000, false);
        int before = miner.getEnergyStored();

        miner.serverTick();

        if (miner.getEnergyStored() != before || !miner.isStructureComplete()) {
            helper.fail("Complete miner without a marker consumed energy or lost its structure state");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void redstoneStopDoesNotConsumeEnergyOrAdvanceProgress(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeAnalyzedMiner(helper, 1);
        miner.getEnergyStorage().receiveEnergy(10_000, false);

        helper.succeedWhen(() -> {
            if (miner.getSlotProcessingTime(0) <= 0) {
                miner.refreshMarkerAnalysis();
                return;
            }
            int energyBefore = miner.getEnergyStored();
            int progressBefore = miner.getSlotProgress(0);
            miner.serverTick();
            if (miner.getEnergyStored() != energyBefore || miner.getSlotProgress(0) != progressBefore) {
                helper.fail("Redstone stop consumed energy or advanced progress");
                return;
            }
        });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void insufficientEnergyDoesNotDrainFluidOrAdvanceProgress(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeAnalyzedMiner(helper, 2);
        miner.getFluidTank()
                .fill(new FluidStack(miner.getRequiredFluid(), BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        helper.succeedWhen(() -> {
            if (miner.getSlotProcessingTime(0) <= 0) {
                miner.refreshMarkerAnalysis();
                return;
            }
            miner.cycleRedstoneMode();
            int fluidBefore = miner.getFluidTank().getFluidAmount();
            int progressBefore = miner.getSlotProgress(0);
            miner.serverTick();
            if (miner.getFluidTank().getFluidAmount() != fluidBefore
                    || miner.getSlotProgress(0) != progressBefore
                    || miner.getEnergyStored() != 0) {
                helper.fail("Insufficient energy drained fluid or advanced progress");
                return;
            }
        });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void insufficientFluidDoesNotConsumeEnergyOrAdvanceProgress(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeAnalyzedMiner(helper, 2);
        miner.getEnergyStorage().receiveEnergy(10_000, false);

        helper.succeedWhen(() -> {
            if (miner.getSlotProcessingTime(0) <= 0) {
                miner.refreshMarkerAnalysis();
                return;
            }
            miner.cycleRedstoneMode();
            int energyBefore = miner.getEnergyStored();
            int progressBefore = miner.getSlotProgress(0);
            miner.serverTick();
            if (miner.getEnergyStored() != energyBefore || miner.getSlotProgress(0) != progressBefore) {
                helper.fail("Insufficient fluid consumed energy or advanced progress");
                return;
            }
        });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void successfulResourceConsumptionPrecedesProgressAdvance(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeAnalyzedMiner(helper, 1);
        miner.getEnergyStorage().receiveEnergy(10_000, false);

        helper.succeedWhen(() -> {
            if (miner.getSlotProcessingTime(0) <= 0) {
                miner.refreshMarkerAnalysis();
                return;
            }
            miner.cycleRedstoneMode();
            int energyBefore = miner.getEnergyStored();
            int progressBefore = miner.getSlotProgress(0);
            miner.serverTick();
            if (miner.getEnergyStored() >= energyBefore
                    || miner.getSlotProgress(0) <= progressBefore) {
                helper.fail("A successful tick did not consume energy before advancing progress");
                return;
            }
        });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void repeatedCallsInOneNaturalTickConsumeResourcesOnlyOnce(
            GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeAnalyzedMiner(helper, 2);
        miner.getEnergyStorage().receiveEnergy(100_000, false);
        miner.getFluidTank()
                .fill(
                        new FluidStack(
                                miner.getRequiredFluid(),
                                BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        helper.succeedWhen(() -> {
            if (miner.getSlotProcessingTime(0) <= 0) {
                miner.refreshMarkerAnalysis();
                return;
            }
            int energyBefore = miner.getEnergyStored();
            int fluidBefore = miner.getFluidTank().getFluidAmount();
            miner.serverTick();
            miner.serverTick();
            int expectedEnergy = energyBefore - miner.getEffectiveEnergyConsumption();
            if (miner.getEnergyStored() != expectedEnergy
                    || miner.getFluidTank().getFluidAmount()
                            != fluidBefore - BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB
                    || miner.getSlotProgress(0) != 1) {
                helper.fail("Same gameTime repeated resource use or logical progress");
                return;
            }
        });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void pendingOutputOnlyRetriesOutput(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper, 2);
        ServerLevel level = helper.getLevel();
        MythicMinerMultiblock.place(level, miner.getBlockPos(), miner.getMinerTier());
        miner.getEnergyStorage().receiveEnergy(10_000, false);
        miner.getFluidTank()
                .fill(
                        new FluidStack(miner.getRequiredFluid(), 100),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        CompoundTag saved = miner.saveWithFullMetadata();
        ListTag pendingOutput = new ListTag();
        pendingOutput.add(new ItemStack(ModItems.STRUCTURE_MARKER.get()).save(new CompoundTag()));
        saved.put("PendingOutput", pendingOutput);
        miner.load(saved);

        int energyBefore = miner.getEnergyStored();
        int fluidBefore = miner.getFluidTank().getFluidAmount();
        int progressBefore = miner.getSlotProgress(0);
        miner.serverTick();

        if (!miner.isOutputBlocked()
                || miner.getEnergyStored() != energyBefore
                || miner.getFluidTank().getFluidAmount() != fluidBefore
                || miner.getSlotProgress(0) != progressBefore) {
            helper.fail("Blocked output started work instead of only retrying output");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void autoExtractFluidPrecedesPendingOutputRetry(GameTestHelper helper) {
        BaseMinerBlockEntity target = placeMiner(helper, 2);
        ServerLevel level = helper.getLevel();
        MythicMinerMultiblock.place(level, target.getBlockPos(), target.getMinerTier());

        BlockPos sourcePosition = target.getBlockPos().north();
        level.setBlock(sourcePosition, ModBlocks.TIER_2_MYTHIC_MINER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(sourcePosition) instanceof BaseMinerBlockEntity source)) {
            helper.fail("Fluid source miner did not create a block entity");
            return;
        }
        for (Direction logicalDirection : Direction.values()) {
            source.cycleFluidFace(logicalDirection);
            if (source.getFluidFaceMode(Direction.SOUTH)
                    == BaseMinerBlockEntity.FluidFaceMode.OUTPUT) {
                break;
            }
        }
        if (source.getFluidFaceMode(Direction.SOUTH) != BaseMinerBlockEntity.FluidFaceMode.OUTPUT) {
            helper.fail("Fluid source miner did not expose its south face as output");
            return;
        }
        source.getFluidTank()
                .fill(
                        new FluidStack(target.getRequiredFluid(), 100),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        target.toggleAutoExtractFluid();
        for (Direction logicalDirection : Direction.values()) {
            target.toggleOutputFace(logicalDirection);
        }

        CompoundTag saved = target.saveWithFullMetadata();
        ListTag pendingOutput = new ListTag();
        pendingOutput.add(new ItemStack(ModItems.STRUCTURE_MARKER.get()).save(new CompoundTag()));
        saved.put("PendingOutput", pendingOutput);
        target.load(saved);

        int energyBefore = target.getEnergyStored();
        target.serverTick();

        if (target.getFluidTank().getFluidAmount() != 100
                || source.getFluidTank().getFluidAmount() != 0
                || !target.isOutputBlocked()
                || target.getEnergyStored() != energyBefore
                || target.getSlotProgress(0) != 0) {
            helper.fail("Pending output retried before automatic fluid extraction");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void saveLoadPreservesMinerWorkState(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper, 2);
        miner.getEnergyStorage().receiveEnergy(4_321, false);
        miner.getFluidTank()
                .fill(
                        new FluidStack(miner.getRequiredFluid(), 123),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        miner.toggleSlotEnabled(0);
        CompoundTag saved = miner.saveWithFullMetadata();
        saved.putIntArray("SlotProgress", new int[] {17});
        ListTag pendingOutput = new ListTag();
        pendingOutput.add(new ItemStack(ModItems.STRUCTURE_MARKER.get(), 3).save(new CompoundTag()));
        saved.put("PendingOutput", pendingOutput);

        miner.load(saved);

        if (miner.getEnergyStored() != 4_321
                || miner.getFluidTank().getFluidAmount() != 123
                || miner.getSlotProgress(0) != 17
                || miner.isSlotEnabled(0)
                || miner.getPendingOutputCount() != 3) {
            helper.fail("Miner save/load did not preserve energy, fluid, progress, slots, or output");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void kubeJsMinerWrapperReportsTheRealMachineTier(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper, 2);
        MinerBlockEntityJS wrapper = new MinerBlockEntityJS(miner);
        if (wrapper.minerTier() != 2 || wrapper.slotCount() != miner.getSlotCountForScript()) {
            helper.fail("KubeJS miner wrapper no longer reflects the block entity tier or slots");
            return;
        }
        helper.succeed();
    }

    private static BaseMinerBlockEntity placeAnalyzedMiner(GameTestHelper helper, int tier) {
        BaseMinerBlockEntity miner = placeMiner(helper, tier);
        ServerLevel level = helper.getLevel();
        MythicMinerMultiblock.place(level, miner.getBlockPos(), miner.getMinerTier());
        ItemStack marker = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        marker.getOrCreateTag().put(
                "StructureMarkerData",
                StructMarkerItem.createCatalogueMarkerData(
                        level, ResourceLocation.fromNamespaceAndPath("minecraft", "jungle_pyramid")));
        miner.getItemHandler().insertItem(0, marker, false);
        miner.cycleRedstoneMode();
        miner.cycleRedstoneMode();
        miner.cycleRedstoneMode();
        return miner;
    }

    private static BaseMinerBlockEntity placeMiner(GameTestHelper helper) {
        return placeMiner(helper, 1);
    }

    private static BaseMinerBlockEntity placeMiner(GameTestHelper helper, int tier) {
        BlockPos position = helper.absolutePos(BlockPos.ZERO);
        ServerLevel level = helper.getLevel();
        level.setBlock(
                position,
                (tier == 2 ? ModBlocks.TIER_2_MYTHIC_MINER.get() : ModBlocks.TIER_1_MYTHIC_MINER.get())
                        .defaultBlockState(),
                3);
        if (level.getBlockEntity(position) instanceof BaseMinerBlockEntity miner) return miner;
        throw new IllegalStateException("Miner did not create a block entity");
    }
}
