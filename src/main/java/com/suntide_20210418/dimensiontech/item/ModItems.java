package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final ResourceLocation STRUCTURE_MARKER_ID =
            ResourceLocationHelper.item("structure_marker");
    public static final ResourceLocation ENCHANTMENT_MARK_ID =
            ResourceLocationHelper.item("enchantment_mark");
    public static final ResourceLocation DIMENSION_DECONSTRUCTION_CORE_ID =
            ResourceLocationHelper.item("dimension_deconstruction_core");

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DimensionTechMod.MOD_ID);

    public static final RegistryObject<Item> STRUCTURE_MARKER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(STRUCTURE_MARKER_ID),
                    () -> new StructMarkerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENCHANTMENT_MARK =
            ITEMS.register(
                    ResourceLocationHelper.getPath(ENCHANTMENT_MARK_ID),
                    () -> new EnchantmentMarkItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DIMENSION_DECONSTRUCTION_CORE =
            ITEMS.register(
                    ResourceLocationHelper.getPath(DIMENSION_DECONSTRUCTION_CORE_ID),
                    () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item>[] DIMENSION_FRAGMENTS =
            tieredItems("dimension_fragment");
    public static final RegistryObject<Item>[] MINING_TOKENS = tieredItems("mining_token");
    public static final RegistryObject<Item> DATA_INTEGRATOR =
            ITEMS.register("data_integrator", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> STRUCTURE_INTERPRETER =
            ITEMS.register(
                    "structure_interpreter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> STRUCTURE_DATA_OPERATOR =
            blockItem("structure_data_operator", ModBlocks.STRUCTURE_DATA_OPERATOR);

    public static final RegistryObject<Item> MYTHIC_ESSENCE_BUCKET =
            fluidBucket("mythic_essence_bucket", ModFluids.MYTHIC_ESSENCE);
    public static final RegistryObject<Item> SURGING_MYTHIC_ESSENCE_BUCKET =
            fluidBucket("surging_mythic_essence_bucket", ModFluids.SURGING_MYTHIC_ESSENCE);
    public static final RegistryObject<Item> RECURSIVE_ESSENCE_BUCKET =
            fluidBucket("recursive_essence_bucket", ModFluids.RECURSIVE_ESSENCE);
    public static final RegistryObject<Item> SURGING_RECURSIVE_ESSENCE_BUCKET =
            fluidBucket("surging_recursive_essence_bucket", ModFluids.SURGING_RECURSIVE_ESSENCE);
    public static final RegistryObject<Item> FRACTAL_ESSENCE_BUCKET =
            fluidBucket("fractal_essence_bucket", ModFluids.FRACTAL_ESSENCE);

    public static final RegistryObject<Item> TIER_1_MYTHIC_MINER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(ModBlocks.TIER_1_MYTHIC_MINER_ID),
                    () ->
                            new BlockItem(
                                    ModBlocks.TIER_1_MYTHIC_MINER.get(), new Item.Properties()));
    public static final RegistryObject<Item> TIER_2_MYTHIC_MINER =
            blockItem("tier_2_mythic_miner", ModBlocks.TIER_2_MYTHIC_MINER);
    public static final RegistryObject<Item> TIER_3_MYTHIC_MINER =
            blockItem("tier_3_mythic_miner", ModBlocks.TIER_3_MYTHIC_MINER);
    public static final RegistryObject<Item> TIER_4_MYTHIC_MINER =
            blockItem("tier_4_mythic_miner", ModBlocks.TIER_4_MYTHIC_MINER);
    public static final RegistryObject<Item> TIER_5_MYTHIC_MINER =
            blockItem("tier_5_mythic_miner", ModBlocks.TIER_5_MYTHIC_MINER);
    public static final RegistryObject<Item> TIER_6_MYTHIC_MINER =
            blockItem("tier_6_mythic_miner", ModBlocks.TIER_6_MYTHIC_MINER);

    public static final RegistryObject<Item> MYTHIC_MINER_CASING =
            blockItem("mythic_miner_casing", ModBlocks.MYTHIC_MINER_CASING);
    public static final RegistryObject<Item> MYTHIC_MINER_STRUCTURE =
            blockItem("mythic_miner_structure", ModBlocks.MYTHIC_MINER_STRUCTURE);
    public static final RegistryObject<Item> UPGRADE_PARALLEL =
            blockItem("mythic_miner_upgrade_parallel", ModBlocks.UPGRADE_PARALLEL);
    public static final RegistryObject<Item> UPGRADE_NONE =
            blockItem("mythic_miner_upgrade_none", ModBlocks.UPGRADE_NONE);
    public static final RegistryObject<Item> UPGRADE_LUCK =
            blockItem("mythic_miner_upgrade_luck", ModBlocks.UPGRADE_LUCK);
    public static final RegistryObject<Item> UPGRADE_ENERGY =
            blockItem("mythic_miner_upgrade_energy", ModBlocks.UPGRADE_ENERGY);
    public static final RegistryObject<Item> UPGRADE_EFFICIENCY =
            blockItem("mythic_miner_upgrade_efficiency", ModBlocks.UPGRADE_EFFICIENCY);
    public static final RegistryObject<Item> UPGRADE_AGGREGATE =
            blockItem("mythic_miner_upgrade_aggregate", ModBlocks.UPGRADE_AGGREGATE);
    public static final RegistryObject<Item>[] UPGRADE_PARALLEL_TIERS =
            upgradeItems("parallel", ModBlocks.UPGRADE_PARALLEL_TIERS, UPGRADE_PARALLEL);
    public static final RegistryObject<Item>[] UPGRADE_LUCK_TIERS =
            upgradeItems("luck", ModBlocks.UPGRADE_LUCK_TIERS, UPGRADE_LUCK);
    public static final RegistryObject<Item>[] UPGRADE_ENERGY_TIERS =
            upgradeItems("energy", ModBlocks.UPGRADE_ENERGY_TIERS, UPGRADE_ENERGY);
    public static final RegistryObject<Item>[] UPGRADE_EFFICIENCY_TIERS =
            upgradeItems("efficiency", ModBlocks.UPGRADE_EFFICIENCY_TIERS, UPGRADE_EFFICIENCY);
    public static final RegistryObject<Item>[] UPGRADE_AGGREGATE_TIERS =
            upgradeItems("aggregate", ModBlocks.UPGRADE_AGGREGATE_TIERS, UPGRADE_AGGREGATE);
    public static final RegistryObject<Item>[] DIMENSION_FOCUS = new RegistryObject[6];

    static {
        for (int tier = 1; tier <= 6; tier++) {
            final int level = tier;
            DIMENSION_FOCUS[tier - 1] =
                    ITEMS.register(
                            "dimension_focus_tier_" + tier,
                            () ->
                                    new BlockItem(
                                            ModBlocks.DIMENSION_FOCUS[level - 1].get(),
                                            new Item.Properties()));
        }
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> fluidBucket(String name, ModFluids.EssenceFluid fluid) {
        RegistryObject<Item> bucket =
                ITEMS.register(
                        name,
                        () ->
                                new BucketItem(
                                        fluid.source(),
                                        new Item.Properties()
                                                .stacksTo(1)
                                                .craftRemainder(Items.BUCKET)));
        fluid.setBucket(bucket);
        return bucket;
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<Item>[] tieredItems(String name) {
        RegistryObject<Item>[] tiers = new RegistryObject[6];
        for (int tier = 1; tier <= 6; tier++) {
            tiers[tier - 1] =
                    ITEMS.register(
                            name + "_tier_" + tier,
                            () -> new Item(new Item.Properties().stacksTo(64)));
        }
        return tiers;
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<Item>[] upgradeItems(
            String name, RegistryObject<Block>[] blocks, RegistryObject<Item> tierOne) {
        RegistryObject<Item>[] tiers = new RegistryObject[6];
        tiers[0] = tierOne;
        for (int tier = 2; tier <= 6; tier++) {
            tiers[tier - 1] =
                    blockItem("mythic_miner_upgrade_" + name + "_tier_" + tier, blocks[tier - 1]);
        }
        return tiers;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
