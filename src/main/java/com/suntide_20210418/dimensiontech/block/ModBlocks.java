package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final ResourceLocation TIER_1_MYTHIC_MINER_ID =
            ResourceLocationHelper.block("tier_1_mythic_miner");

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DimensionTechMod.MOD_ID);

    public static final RegistryObject<Block> TIER_1_MYTHIC_MINER =
            BLOCKS.register(
                    ResourceLocationHelper.getPath(TIER_1_MYTHIC_MINER_ID),
                    () -> new Tier1BaseMinerBlock(BlockBehaviour.Properties.of().strength(5.0F)));

    private ModBlocks() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
