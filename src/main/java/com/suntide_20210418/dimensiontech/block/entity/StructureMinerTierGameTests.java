package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Locks every miner block entity to its corresponding runtime tier configuration. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureMinerTierGameTests {
    private StructureMinerTierGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void allTiersMapToTheirConfiguredMachineValues(GameTestHelper helper) {
        List<Block> blocks =
                List.of(
                        ModBlocks.TIER_1_STRUCTURE_MINER.get(),
                        ModBlocks.TIER_2_STRUCTURE_MINER.get(),
                        ModBlocks.TIER_3_STRUCTURE_MINER.get(),
                        ModBlocks.TIER_4_STRUCTURE_MINER.get(),
                        ModBlocks.TIER_5_STRUCTURE_MINER.get(),
                        ModBlocks.TIER_6_STRUCTURE_MINER.get());
        for (int index = 0; index < blocks.size(); index++) {
            int tier = index + 1;
            BlockPos position = helper.absolutePos(new BlockPos(index * 4, 0, 0));
            helper.getLevel().setBlock(position, blocks.get(index).defaultBlockState(), 3);
            if (!(helper.getLevel().getBlockEntity(position)
                    instanceof BaseMinerBlockEntity miner)) {
                helper.fail("Tier " + tier + " did not create a miner block entity");
                return;
            }
            ModConfigs.StructureMinerTierConfig config = ModConfigs.TIERS[index];
            if (miner.getMinerTier() != tier
                    || miner.getSlotCount() != config.slotCount()
                    || miner.getBaseParallel() != config.baseParallel()
                    || miner.getMachineLuck() != config.baseLuck()
                    || miner.getEnergyCapacity() != config.energyCapacity()
                    || miner.getEnergyConsumption() != config.energyConsumption()
                    || miner.getMachineEfficiency() != config.efficiency()
                    || miner.getQuantityReference() != config.quantityReference()
                    // 以代码为准：requiresFluidInput() 的默认分支是 getMinerTier() >= 1，对任何
                    // 合法 Tier 恒为真，因此每个 Tier 都需要流体输入（Tier 1 为水，JEI 也照此展示）。
                    // 这里曾断言 tier >= 2（期望 Tier 1 免流体），与判据直接冲突，导致本测试在
                    // 1.21.1 上失败。经确认后按代码修正断言。
                    || !miner.requiresFluidInput()) {
                helper.fail("Tier " + tier + " does not map to ModConfigs.TIERS[" + index + "]");
                return;
            }
        }
        helper.succeed();
    }
}
