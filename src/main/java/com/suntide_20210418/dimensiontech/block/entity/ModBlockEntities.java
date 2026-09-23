package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DimensionTechMod.MOD_ID);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<StructureDataOperatorBlockEntity>>
            STRUCTURE_DATA_OPERATOR =
                    BLOCK_ENTITY_TYPES.register(
                            "structure_data_operator",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    StructureDataOperatorBlockEntity::new,
                                                    ModBlocks.STRUCTURE_DATA_OPERATOR.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<StructureReactorBlockEntity>>
            STRUCTURE_REACTOR =
                    BLOCK_ENTITY_TYPES.register(
                            "structure_reactor",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    StructureReactorBlockEntity::new,
                                                    ModBlocks.STRUCTURE_REACTOR.get())
                                            .build(null));

    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier1StructureMinerBlockEntity>>
            TIER_1_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            ResourceLocationHelper.getPath(ModBlocks.TIER_1_STRUCTURE_MINER_ID),
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier1StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_1_STRUCTURE_MINER.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier2StructureMinerBlockEntity>>
            TIER_2_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_2_structure_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier2StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_2_STRUCTURE_MINER.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier3StructureMinerBlockEntity>>
            TIER_3_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_3_structure_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier3StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_3_STRUCTURE_MINER.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier4StructureMinerBlockEntity>>
            TIER_4_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_4_structure_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier4StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_4_STRUCTURE_MINER.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier5StructureMinerBlockEntity>>
            TIER_5_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_5_structure_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier5StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_5_STRUCTURE_MINER.get())
                                            .build(null));
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<Tier6StructureMinerBlockEntity>>
            TIER_6_STRUCTURE_MINER =
                    BLOCK_ENTITY_TYPES.register(
                            "tier_6_structure_miner",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    Tier6StructureMinerBlockEntity::new,
                                                    ModBlocks.TIER_6_STRUCTURE_MINER.get())
                                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
