package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Locks every miner block entity to its corresponding runtime tier configuration. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerTierGameTests {
    private MythicMinerTierGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void allTiersMapToTheirConfiguredMachineValues(GameTestHelper helper) {
        List<Block> blocks =
                List.of(
                        ModBlocks.TIER_1_MYTHIC_MINER.get(),
                        ModBlocks.TIER_2_MYTHIC_MINER.get(),
                        ModBlocks.TIER_3_MYTHIC_MINER.get(),
                        ModBlocks.TIER_4_MYTHIC_MINER.get(),
                        ModBlocks.TIER_5_MYTHIC_MINER.get(),
                        ModBlocks.TIER_6_MYTHIC_MINER.get());
        for (int index = 0; index < blocks.size(); index++) {
            int tier = index + 1;
            BlockPos position = helper.absolutePos(new BlockPos(index * 4, 0, 0));
            helper.getLevel().setBlock(position, blocks.get(index).defaultBlockState(), 3);
            if (!(helper.getLevel().getBlockEntity(position)
                    instanceof BaseMinerBlockEntity miner)) {
                helper.fail("Tier " + tier + " did not create a miner block entity");
                return;
            }
            ModConfigs.MythicMinerTierConfig config = ModConfigs.TIERS[index];
            if (miner.getMinerTier() != tier
                    || miner.getSlotCount() != config.slotCount()
                    || miner.getBaseParallel() != config.baseParallel()
                    || miner.getMachineLuck() != config.baseLuck()
                    || miner.getEnergyCapacity() != config.energyCapacity()
                    || miner.getEnergyConsumption() != config.energyConsumption()
                    || miner.getMachineEfficiency() != config.efficiency()
                    || miner.getQuantityReference() != config.quantityReference()
                    || miner.requiresFluidInput() != (tier >= 2)) {
                helper.fail("Tier " + tier + " does not map to ModConfigs.TIERS[" + index + "]");
                return;
            }
        }
        helper.succeed();
    }
}
