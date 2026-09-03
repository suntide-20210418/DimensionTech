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
    public static final RegistryObject<BlockEntityType<StructureDataOperatorBlockEntity>> STRUCTURE_DATA_OPERATOR =
            BLOCK_ENTITY_TYPES.register("structure_data_operator", () -> BlockEntityType.Builder.of(
                    StructureDataOperatorBlockEntity::new, ModBlocks.STRUCTURE_DATA_OPERATOR.get()).build(null));

    public static final RegistryObject<
                    BlockEntityType<Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity>>
            TIER_1_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            ResourceLocationHelper.getPath(ModBlocks.TIER_1_MYTHIC_MINER_ID),
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity
                                                            ::new,
                                                    ModBlocks.TIER_1_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<
                    BlockEntityType<Tier2MythicShellChikensVoidStructreResourceMinerBlockEntity>>
            TIER_2_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_2_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier2MythicShellChikensVoidStructreResourceMinerBlockEntity
                                                            ::new,
                                                    ModBlocks.TIER_2_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<
                    BlockEntityType<Tier3MythicShellChikensVoidStructreResourceMinerBlockEntity>>
            TIER_3_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_3_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier3MythicShellChikensVoidStructreResourceMinerBlockEntity
                                                            ::new,
                                                    ModBlocks.TIER_3_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<
                    BlockEntityType<Tier4MythicShellChikensVoidStructreResourceMinerBlockEntity>>
            TIER_4_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_4_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier4MythicShellChikensVoidStructreResourceMinerBlockEntity
                                                            ::new,
                                                    ModBlocks.TIER_4_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<
                    BlockEntityType<Tier5MythicShellChikensVoidStructreResourceMinerBlockEntity>>
            TIER_5_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_5_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier5MythicShellChikensVoidStructreResourceMinerBlockEntity
                                                            ::new,
                                                    ModBlocks.TIER_5_MYTHIC_MINER.get())
                                            .build(null));
    public static final RegistryObject<BlockEntityType<KashanVoidStructreResourceMinerBlockEntity>>
            TIER_6_MYTHIC_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_6_mythic_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    KashanVoidStructreResourceMinerBlockEntity::new,
                                                    ModBlocks.TIER_6_MYTHIC_MINER.get())
                                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
