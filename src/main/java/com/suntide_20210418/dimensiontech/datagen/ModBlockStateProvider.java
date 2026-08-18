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
        ModelFile model =
                models().cubeAll(
                                ModBlocks.TIER_1_MYTHIC_MINER_ID.getPath(),
                                mcLoc("block/raw_iron_block"));
        simpleBlockWithItem(miner, model);
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
