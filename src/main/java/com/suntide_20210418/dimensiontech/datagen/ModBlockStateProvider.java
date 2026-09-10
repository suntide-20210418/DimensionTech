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
        Block miner = ModBlocks.TIER_1_MYTHIC_MINER.get();
        Block operator = ModBlocks.STRUCTURE_DATA_OPERATOR.get();
        simpleBlockWithItem(ModBlocks.MYTHIC_CRUCIBLE.get(), models().cubeAll("mythic_crucible", mcLoc("block/raw_iron_block")));
        ModelFile operatorModel =
                models().cubeAll("structure_data_operator", mcLoc("block/raw_iron_block"));
        simpleBlockWithItem(operator, operatorModel);
        ModelFile model =
                models().cubeAll(
                                ModBlocks.TIER_1_MYTHIC_MINER_ID.getPath(),
                                mcLoc("block/raw_iron_block"));
        horizontalBlock(miner, model);
        simpleBlockItem(miner, model);
        registerMiner(ModBlocks.TIER_2_MYTHIC_MINER.get(), "tier_2_mythic_miner");
        registerMiner(ModBlocks.TIER_3_MYTHIC_MINER.get(), "tier_3_mythic_miner");
        registerMiner(ModBlocks.TIER_4_MYTHIC_MINER.get(), "tier_4_mythic_miner");
        registerMiner(ModBlocks.TIER_5_MYTHIC_MINER.get(), "tier_5_mythic_miner");
        registerMiner(ModBlocks.TIER_6_MYTHIC_MINER.get(), "tier_6_mythic_miner");
        for (Block block :
                new Block[] {
                    ModBlocks.MYTHIC_MINER_CASING.get(),
                    ModBlocks.MYTHIC_MINER_STRUCTURE.get(),
                    ModBlocks.UPGRADE_NONE.get(),
                    ModBlocks.UPGRADE_PARALLEL.get(),
                    ModBlocks.UPGRADE_LUCK.get(),
                    ModBlocks.UPGRADE_ENERGY.get(),
                    ModBlocks.UPGRADE_EFFICIENCY.get(),
                    ModBlocks.UPGRADE_AGGREGATE.get()
                }) {
            ModelFile blockModel =
                    models().cubeAll(
                                    block.getDescriptionId().replace("block.dimension_tech.", ""),
                                    mcLoc("block/raw_iron_block"));
            simpleBlockWithItem(block, blockModel);
        }
        registerUpgradeTiers(ModBlocks.UPGRADE_PARALLEL_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_LUCK_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_ENERGY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_EFFICIENCY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_AGGREGATE_TIERS);
        for (var focus : ModBlocks.DIMENSION_FOCUS) {
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

    private void registerUpgradeTiers(RegistryObject<Block>[] tiers) {
        for (int tier = 1; tier < tiers.length; tier++) {
            Block block = tiers[tier].get();
            ModelFile model =
                    models().cubeAll(tiers[tier].getId().getPath(), mcLoc("block/raw_iron_block"));
            simpleBlockWithItem(block, model);
        }
    }

    private <T extends Block> void blockItem(RegistryObject<T> block) {
        simpleBlockItem(
                block.get(),
                new ModelFile.UncheckedModelFile(
                        DimensionTechMod.MOD_ID + ":block/" + block.getId().getPath()));
    }

    private <T extends Block> void blockItem(RegistryObject<T> block, String append) {
        simpleBlockItem(
                block.get(),
                new ModelFile.UncheckedModelFile(
                        DimensionTechMod.MOD_ID + ":block/" + block.getId().getPath() + append));
    }
}
