package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DimensionTechMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<Tier1MythicMinerBlockEntity>>
            TIER_1_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            ResourceLocationHelper.getPath(ModBlocks.TIER_1_MYTHIC_MINER_ID),
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier1MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_1_MYTHIC_MINER.get())
                                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
