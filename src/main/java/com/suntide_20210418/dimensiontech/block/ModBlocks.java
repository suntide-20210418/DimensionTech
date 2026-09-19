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
    public static final ResourceLocation TIER_1_STRUCTURE_MINER_ID =
            ResourceLocationHelper.block("tier_1_structure_miner");

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DimensionTechMod.MOD_ID);
    public static final RegistryObject<Block> STRUCTURE_DATA_OPERATOR =
            BLOCKS.register(
                    "structure_data_operator",
                    () ->
                            new StructureDataOperatorBlock(
                                    BlockBehaviour.Properties.of().strength(4.0F)));
    public static final RegistryObject<Block> STRUCTURE_REACTOR =
            BLOCKS.register(
                    "structure_reactor",
                    () -> new StructureReactorBlock(BlockBehaviour.Properties.of().strength(4.0F)));

    public static final RegistryObject<Block> TIER_1_STRUCTURE_MINER =
            BLOCKS.register(
                    ResourceLocationHelper.getPath(TIER_1_STRUCTURE_MINER_ID),
                    () ->
                            new Tier1StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_2_STRUCTURE_MINER =
            BLOCKS.register(
                    "tier_2_structure_miner",
                    () ->
                            new Tier2StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_3_STRUCTURE_MINER =
            BLOCKS.register(
                    "tier_3_structure_miner",
                    () ->
                            new Tier3StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_4_STRUCTURE_MINER =
            BLOCKS.register(
                    "tier_4_structure_miner",
                    () ->
                            new Tier4StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_5_STRUCTURE_MINER =
            BLOCKS.register(
                    "tier_5_structure_miner",
                    () ->
                            new Tier5StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_6_STRUCTURE_MINER =
            BLOCKS.register(
                    "tier_6_structure_miner",
                    () ->
                            new Tier6StructureMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> STRUCTURE_MINER_CASING =
            BLOCKS.register(
                    "structure_miner_casing",
                    () -> new Block(BlockBehaviour.Properties.of().strength(4.0F)));
    public static final RegistryObject<Block> STRUCTURE_MINER_STRUCTURE =
            BLOCKS.register(
                    "structure_miner_structure",
                    () ->
                            new StructureMinerStructureBlock(
                                    BlockBehaviour.Properties.of().strength(3.0F)));
    public static final RegistryObject<Block> UPGRADE_PARALLEL =
            upgrade("parallel", StructureMinerUpgradeBlock.Type.PARALLEL, 1);
    public static final RegistryObject<Block> UPGRADE_LUCK =
            upgrade("luck", StructureMinerUpgradeBlock.Type.LUCK, 1);
    public static final RegistryObject<Block> UPGRADE_ENERGY =
            upgrade("energy", StructureMinerUpgradeBlock.Type.ENERGY, 1);
    public static final RegistryObject<Block> UPGRADE_EFFICIENCY =
            upgrade("efficiency", StructureMinerUpgradeBlock.Type.EFFICIENCY, 1);
    public static final RegistryObject<Block> UPGRADE_AGGREGATE =
            upgrade("aggregate", StructureMinerUpgradeBlock.Type.AGGREGATE, 1);
    public static final RegistryObject<Block>[] UPGRADE_PARALLEL_TIERS =
            upgradeTiers("parallel", StructureMinerUpgradeBlock.Type.PARALLEL, UPGRADE_PARALLEL);
    public static final RegistryObject<Block>[] UPGRADE_LUCK_TIERS =
            upgradeTiers("luck", StructureMinerUpgradeBlock.Type.LUCK, UPGRADE_LUCK);
    public static final RegistryObject<Block>[] UPGRADE_ENERGY_TIERS =
            upgradeTiers("energy", StructureMinerUpgradeBlock.Type.ENERGY, UPGRADE_ENERGY);
    public static final RegistryObject<Block>[] UPGRADE_EFFICIENCY_TIERS =
            upgradeTiers("efficiency", StructureMinerUpgradeBlock.Type.EFFICIENCY, UPGRADE_EFFICIENCY);
    public static final RegistryObject<Block>[] UPGRADE_AGGREGATE_TIERS =
            upgradeTiers("aggregate", StructureMinerUpgradeBlock.Type.AGGREGATE, UPGRADE_AGGREGATE);
    public static final RegistryObject<Block>[] DIMENSION_FOCUS = new RegistryObject[6];

    static {
        for (int tier = 1; tier <= 6; tier++) {
            final int level = tier;
            DIMENSION_FOCUS[tier - 1] =
                    BLOCKS.register(
                            "dimension_focus_tier_" + tier,
                            () -> new Block(BlockBehaviour.Properties.of().strength(4.0F)));
        }
    }

    private static RegistryObject<Block> upgrade(
            String name, StructureMinerUpgradeBlock.Type type, int tier) {
        return BLOCKS.register(
                "structure_miner_upgrade_" + name + (tier == 1 ? "" : "_tier_" + tier),
                () ->
                        new StructureMinerUpgradeBlock(
                                BlockBehaviour.Properties.of().strength(3.0F), type, tier));
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<Block>[] upgradeTiers(
            String name, StructureMinerUpgradeBlock.Type type, RegistryObject<Block> tierOne) {
        RegistryObject<Block>[] tiers = new RegistryObject[6];
        tiers[0] = tierOne;
        for (int tier = 2; tier <= 6; tier++) {
            tiers[tier - 1] = upgrade(name, type, tier);
        }
        return tiers;
    }

    private ModBlocks() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
