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
    public static final RegistryObject<BlockEntityType<Tier2MythicMinerBlockEntity>>
            TIER_2_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_2_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier2MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_2_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<BlockEntityType<Tier3MythicMinerBlockEntity>>
            TIER_3_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_3_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier3MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_3_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<BlockEntityType<Tier4MythicMinerBlockEntity>>
            TIER_4_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_4_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier4MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_4_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<BlockEntityType<Tier5MythicMinerBlockEntity>>
            TIER_5_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_5_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier5MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_5_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<BlockEntityType<Tier6MythicMinerBlockEntity>>
            TIER_6_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_6_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier6MythicMinerBlockEntity::new,
                                                    ModBlocks.TIER_6_MYTHIC_MINER.get())
                                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
