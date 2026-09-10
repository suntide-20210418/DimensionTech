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
    public static final RegistryObject<Block> STRUCTURE_DATA_OPERATOR =
            BLOCKS.register(
                    "structure_data_operator",
                    () ->
                            new StructureDataOperatorBlock(
                            BlockBehaviour.Properties.of().strength(4.0F)));
    public static final RegistryObject<Block> MYTHIC_CRUCIBLE =
            BLOCKS.register("mythic_crucible", () -> new MythicCrucibleBlock(BlockBehaviour.Properties.of().strength(4.0F)));

    public static final RegistryObject<Block> TIER_1_MYTHIC_MINER =
            BLOCKS.register(
                    ResourceLocationHelper.getPath(TIER_1_MYTHIC_MINER_ID),
                    () ->
                            new Tier1MythicShellChikensVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_2_MYTHIC_MINER =
            BLOCKS.register(
                    "tier_2_mythic_miner",
                    () ->
                            new Tier2MythicShellChikensVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_3_MYTHIC_MINER =
            BLOCKS.register(
                    "tier_3_mythic_miner",
                    () ->
                            new Tier3MythicShellChikensVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_4_MYTHIC_MINER =
            BLOCKS.register(
                    "tier_4_mythic_miner",
                    () ->
                            new Tier4MythicShellChikensVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_5_MYTHIC_MINER =
            BLOCKS.register(
                    "tier_5_mythic_miner",
                    () ->
                            new Tier5MythicShellChikensVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> TIER_6_MYTHIC_MINER =
            BLOCKS.register(
                    "tier_6_mythic_miner",
                    () ->
                            new KashanVoidStructreResourceMinerBlock(
                                    BlockBehaviour.Properties.of().strength(5.0F)));
    public static final RegistryObject<Block> MYTHIC_MINER_CASING =
            BLOCKS.register(
                    "mythic_miner_casing",
                    () -> new Block(BlockBehaviour.Properties.of().strength(4.0F)));
    public static final RegistryObject<Block> MYTHIC_MINER_STRUCTURE =
            BLOCKS.register(
                    "mythic_miner_structure",
                    () ->
                            new MythicMinerStructureBlock(
                                    BlockBehaviour.Properties.of().strength(3.0F)));
    public static final RegistryObject<Block> UPGRADE_PARALLEL =
            upgrade("parallel", MythicMinerUpgradeBlock.Type.PARALLEL, 1);
    public static final RegistryObject<Block> UPGRADE_NONE =
            upgrade("none", MythicMinerUpgradeBlock.Type.NONE, 1);
    public static final RegistryObject<Block> UPGRADE_LUCK =
            upgrade("luck", MythicMinerUpgradeBlock.Type.LUCK, 1);
    public static final RegistryObject<Block> UPGRADE_ENERGY =
            upgrade("energy", MythicMinerUpgradeBlock.Type.ENERGY, 1);
    public static final RegistryObject<Block> UPGRADE_EFFICIENCY =
            upgrade("efficiency", MythicMinerUpgradeBlock.Type.EFFICIENCY, 1);
    public static final RegistryObject<Block> UPGRADE_AGGREGATE =
            upgrade("aggregate", MythicMinerUpgradeBlock.Type.AGGREGATE, 1);
    public static final RegistryObject<Block>[] UPGRADE_PARALLEL_TIERS =
            upgradeTiers("parallel", MythicMinerUpgradeBlock.Type.PARALLEL, UPGRADE_PARALLEL);
    public static final RegistryObject<Block>[] UPGRADE_LUCK_TIERS =
            upgradeTiers("luck", MythicMinerUpgradeBlock.Type.LUCK, UPGRADE_LUCK);
    public static final RegistryObject<Block>[] UPGRADE_ENERGY_TIERS =
            upgradeTiers("energy", MythicMinerUpgradeBlock.Type.ENERGY, UPGRADE_ENERGY);
    public static final RegistryObject<Block>[] UPGRADE_EFFICIENCY_TIERS =
            upgradeTiers("efficiency", MythicMinerUpgradeBlock.Type.EFFICIENCY, UPGRADE_EFFICIENCY);
    public static final RegistryObject<Block>[] UPGRADE_AGGREGATE_TIERS =
            upgradeTiers("aggregate", MythicMinerUpgradeBlock.Type.AGGREGATE, UPGRADE_AGGREGATE);
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
            String name, MythicMinerUpgradeBlock.Type type, int tier) {
        return BLOCKS.register(
                "mythic_miner_upgrade_" + name + (tier == 1 ? "" : "_tier_" + tier),
                () ->
                        new MythicMinerUpgradeBlock(
                                BlockBehaviour.Properties.of().strength(3.0F), type, tier));
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<Block>[] upgradeTiers(
            String name, MythicMinerUpgradeBlock.Type type, RegistryObject<Block> tierOne) {
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
