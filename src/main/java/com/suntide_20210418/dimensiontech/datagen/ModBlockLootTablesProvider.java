package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.block.ModBlocks;
import java.util.Set;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockLootTablesProvider extends BlockLootSubProvider {

    public ModBlockLootTablesProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.TIER_1_MYTHIC_MINER.get());
    }

    @Override
    public Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
