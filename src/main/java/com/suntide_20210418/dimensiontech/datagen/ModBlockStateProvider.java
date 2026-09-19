package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, DimensionTechMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Blocks with hand-authored models under src/main/resources reference them via
        // getExistingFile; generating a placeholder for these would either collide with the
        // hand-written file (fails processResources) or shadow it.
        simpleBlockWithItem(
                ModBlocks.STRUCTURE_REACTOR.get(),
                models().getExistingFile(modLoc("block/strcture_reactor")));
        simpleBlockWithItem(
                ModBlocks.STRUCTURE_DATA_OPERATOR.get(),
                models().getExistingFile(modLoc("block/structure_data_operator")));
        simpleBlockWithItem(
                ModBlocks.STRUCTURE_MINER_CASING.get(),
                models().getExistingFile(modLoc("block/structure_miner_casing")));

        Block tier1Miner = ModBlocks.TIER_1_STRUCTURE_MINER.get();
        ModelFile tier1MinerModel = models().getExistingFile(modLoc("block/tier_1_strcture_miner"));
        horizontalBlock(tier1Miner, tier1MinerModel);
        simpleBlockItem(tier1Miner, tier1MinerModel);
        registerMiner(ModBlocks.TIER_2_STRUCTURE_MINER.get(), "tier_2_structure_miner");
        registerMiner(ModBlocks.TIER_3_STRUCTURE_MINER.get(), "tier_3_structure_miner");
        registerMiner(ModBlocks.TIER_4_STRUCTURE_MINER.get(), "tier_4_structure_miner");
        registerMiner(ModBlocks.TIER_5_STRUCTURE_MINER.get(), "tier_5_structure_miner");
        registerMiner(ModBlocks.TIER_6_STRUCTURE_MINER.get(), "tier_6_structure_miner");

        simpleBlockWithItem(
                ModBlocks.STRUCTURE_MINER_STRUCTURE.get(),
                models().getExistingFile(modLoc("block/structure_miner_strcture")));

        registerUpgradeBase(ModBlocks.UPGRADE_PARALLEL);
        registerUpgradeBase(ModBlocks.UPGRADE_LUCK);
        registerUpgradeBase(ModBlocks.UPGRADE_ENERGY);
        registerUpgradeBase(ModBlocks.UPGRADE_EFFICIENCY);
        registerUpgradeBase(ModBlocks.UPGRADE_AGGREGATE);
        registerUpgradeTiers(ModBlocks.UPGRADE_PARALLEL_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_LUCK_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_ENERGY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_EFFICIENCY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_AGGREGATE_TIERS);

        for (int tier = 0; tier < ModBlocks.DIMENSION_FOCUS.length; tier++) {
            var focus = ModBlocks.DIMENSION_FOCUS[tier];
            ModelFile focusModel =
                    models().cubeAll(focus.getId().getPath(), mcLoc("block/amethyst_block"));
            simpleBlockWithItem(focus.get(), focusModel);
        }
    }

    private void registerMiner(Block block, String name) {
        ModelFile model = models().cubeAll(name, mcLoc("block/raw_iron_block"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }

    /** The base upgrade block is tier 1 and shares the hand-written tier-1 model. */
    private void registerUpgradeBase(RegistryObject<Block> base) {
        ModelFile model =
                models().getExistingFile(modLoc("block/" + base.getId().getPath() + "_tier_1"));
        simpleBlockWithItem(base.get(), model);
    }

    private void registerUpgradeTiers(RegistryObject<Block>[] tiers) {
        for (int tier = 1; tier < tiers.length; tier++) {
            Block block = tiers[tier].get();
            ModelFile model =
                    models().getExistingFile(modLoc("block/" + tiers[tier].getId().getPath()));
            simpleBlockWithItem(block, model);
        }
    }
}
