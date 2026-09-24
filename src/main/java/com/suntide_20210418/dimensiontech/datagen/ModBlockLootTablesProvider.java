package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.block.ModBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

public class ModBlockLootTablesProvider extends BlockLootSubProvider {

    public ModBlockLootTablesProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        // Every registered block has a corresponding BlockItem, so generate a
        // self-drop table for the complete registry (including tiered miners).
        ModBlocks.BLOCKS.getEntries().forEach(entry -> dropSelf(entry.get()));
    }

    @Override
    public Iterable<Block> getKnownBlocks() {
        // getEntries() 的元素类型是 DeferredHolder<Block, ? extends Block>，直接把方法引用交给
        // map 会得到捕获类型，无法作为 Iterable<Block> 返回，因此在 lambda 里显式收窄。
        return ModBlocks.BLOCKS.getEntries().stream().map(entry -> (Block) entry.get())::iterator;
    }
}
