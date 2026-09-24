package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final ResourceLocation STRUCTURE_MARKER_ID =
            ResourceLocationHelper.item("structure_marker");
    public static final ResourceLocation CHEST_MARKER_ID =
            ResourceLocationHelper.item("chest_marker");
    public static final ResourceLocation DIMENSION_DECONSTRUCTION_CORE_ID =
            ResourceLocationHelper.item("dimension_deconstruction_core");
    public static final ResourceLocation WRENCH_ID = ResourceLocationHelper.item("wrench");

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DimensionTechMod.MOD_ID);

    public static final DeferredHolder<Item, Item> STRUCTURE_MARKER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(STRUCTURE_MARKER_ID),
                    () -> new StructMarkerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> CHEST_MARKER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(CHEST_MARKER_ID),
                    () -> new ChestMarkerItem(new Item.Properties().stacksTo(1)));

    /**
     * Whether a stack is a marker the miner/operator can consume. Structure and chest markers share
     * the same {@code StructureMarkerData} NBT, so both flow through the same copy and production
     * chains.
     */
    public static boolean isMarker(ItemStack stack) {
        return stack.is(STRUCTURE_MARKER.get()) || stack.is(CHEST_MARKER.get());
    }

    public static final DeferredHolder<Item, Item> DIMENSION_DECONSTRUCTION_CORE =
            ITEMS.register(
                    ResourceLocationHelper.getPath(DIMENSION_DECONSTRUCTION_CORE_ID),
                    () -> new DimensionDeconstructionCoreItem(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, Item>[] DIMENSION_FRAGMENTS =
            tieredItems("dimension_fragment");
    public static final DeferredHolder<Item, Item>[] MINING_TOKENS = tieredItems("mining_token");
    public static final DeferredHolder<Item, Item> DATA_INTEGRATOR =
            ITEMS.register("data_integrator", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> STRUCTURE_INTERPRETER =
            ITEMS.register(
                    "structure_interpreter", () -> new Item(new Item.Properties().stacksTo(1)));

    /**
     * The projection wrench: toggles the multiblock projection on a structure miner, and on
     * shift+right-click builds it out of the player's inventory. It replaces the ad-hoc
     * wooden-stick trigger in {@code BaseMinerBlock}.
     */
    public static final DeferredHolder<Item, Item> WRENCH =
            ITEMS.register(
                    ResourceLocationHelper.getPath(WRENCH_ID),
                    () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> STRUCTURE_DATA_OPERATOR =
            blockItem("structure_data_operator", ModBlocks.STRUCTURE_DATA_OPERATOR);
    public static final DeferredHolder<Item, Item> STRUCTURE_REACTOR =
            blockItem("structure_reactor", ModBlocks.STRUCTURE_REACTOR);

    public static final DeferredHolder<Item, Item> STRUCTURE_ESSENCE_BUCKET =
            fluidBucket("structure_essence_bucket", ModFluids.STRUCTURE_ESSENCE);
    public static final DeferredHolder<Item, Item> SURGING_STRUCTURE_ESSENCE_BUCKET =
            fluidBucket("surging_structure_essence_bucket", ModFluids.SURGING_STRUCTURE_ESSENCE);
    public static final DeferredHolder<Item, Item> RECURSIVE_ESSENCE_BUCKET =
            fluidBucket("recursive_essence_bucket", ModFluids.RECURSIVE_ESSENCE);
    public static final DeferredHolder<Item, Item> SURGING_RECURSIVE_ESSENCE_BUCKET =
            fluidBucket("surging_recursive_essence_bucket", ModFluids.SURGING_RECURSIVE_ESSENCE);
    public static final DeferredHolder<Item, Item> FRACTAL_ESSENCE_BUCKET =
            fluidBucket("fractal_essence_bucket", ModFluids.FRACTAL_ESSENCE);

    public static final DeferredHolder<Item, Item> TIER_1_STRUCTURE_MINER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(ModBlocks.TIER_1_STRUCTURE_MINER_ID),
                    () ->
                            new BlockItem(
                                    ModBlocks.TIER_1_STRUCTURE_MINER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TIER_2_STRUCTURE_MINER =
            blockItem("tier_2_structure_miner", ModBlocks.TIER_2_STRUCTURE_MINER);
    public static final DeferredHolder<Item, Item> TIER_3_STRUCTURE_MINER =
            blockItem("tier_3_structure_miner", ModBlocks.TIER_3_STRUCTURE_MINER);
    public static final DeferredHolder<Item, Item> TIER_4_STRUCTURE_MINER =
            blockItem("tier_4_structure_miner", ModBlocks.TIER_4_STRUCTURE_MINER);
    public static final DeferredHolder<Item, Item> TIER_5_STRUCTURE_MINER =
            blockItem("tier_5_structure_miner", ModBlocks.TIER_5_STRUCTURE_MINER);
    public static final DeferredHolder<Item, Item> TIER_6_STRUCTURE_MINER =
            blockItem("tier_6_structure_miner", ModBlocks.TIER_6_STRUCTURE_MINER);

    public static final DeferredHolder<Item, Item> STRUCTURE_MINER_CASING =
            blockItem("structure_miner_casing", ModBlocks.STRUCTURE_MINER_CASING);
    public static final DeferredHolder<Item, Item> STRUCTURE_MINER_STRUCTURE =
            blockItem("structure_miner_structure", ModBlocks.STRUCTURE_MINER_STRUCTURE);
    public static final DeferredHolder<Item, Item> STRUCTURE_MINER_GLASS =
            blockItem("structure_miner_glass", ModBlocks.STRUCTURE_MINER_GLASS);
    public static final DeferredHolder<Item, Item> UPGRADE_PARALLEL =
            blockItem("structure_miner_upgrade_parallel", ModBlocks.UPGRADE_PARALLEL);
    public static final DeferredHolder<Item, Item> UPGRADE_LUCK =
            blockItem("structure_miner_upgrade_luck", ModBlocks.UPGRADE_LUCK);
    public static final DeferredHolder<Item, Item> UPGRADE_ENERGY =
            blockItem("structure_miner_upgrade_energy", ModBlocks.UPGRADE_ENERGY);
    public static final DeferredHolder<Item, Item> UPGRADE_EFFICIENCY =
            blockItem("structure_miner_upgrade_efficiency", ModBlocks.UPGRADE_EFFICIENCY);
    public static final DeferredHolder<Item, Item> UPGRADE_AGGREGATE =
            blockItem("structure_miner_upgrade_aggregate", ModBlocks.UPGRADE_AGGREGATE);
    public static final DeferredHolder<Item, Item>[] UPGRADE_PARALLEL_TIERS =
            upgradeItems("parallel", ModBlocks.UPGRADE_PARALLEL_TIERS, UPGRADE_PARALLEL);
    public static final DeferredHolder<Item, Item>[] UPGRADE_LUCK_TIERS =
            upgradeItems("luck", ModBlocks.UPGRADE_LUCK_TIERS, UPGRADE_LUCK);
    public static final DeferredHolder<Item, Item>[] UPGRADE_ENERGY_TIERS =
            upgradeItems("energy", ModBlocks.UPGRADE_ENERGY_TIERS, UPGRADE_ENERGY);
    public static final DeferredHolder<Item, Item>[] UPGRADE_EFFICIENCY_TIERS =
            upgradeItems("efficiency", ModBlocks.UPGRADE_EFFICIENCY_TIERS, UPGRADE_EFFICIENCY);
    public static final DeferredHolder<Item, Item>[] UPGRADE_AGGREGATE_TIERS =
            upgradeItems("aggregate", ModBlocks.UPGRADE_AGGREGATE_TIERS, UPGRADE_AGGREGATE);

    private static DeferredHolder<Item, Item> blockItem(
            String name, DeferredHolder<Block, Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> fluidBucket(
            String name, ModFluids.EssenceFluid fluid) {
        DeferredHolder<Item, Item> bucket =
                ITEMS.register(
                        name,
                        () ->
                                new BucketItem(
                                        // 1.21's BucketItem takes a resolved Fluid rather than the
                                        // 1.20.1 Supplier overload, so the holder has to be
                                        // resolved
                                        // eagerly here. This is safe because FML fires one
                                        // RegisterEvent per registry in BuiltInRegistries
                                        // declaration
                                        // order, and FLUID is declared before ITEM
                                        // (BuiltInRegistries
                                        // lines 138 and 144) - the fluid is already populated by
                                        // the
                                        // time this item supplier runs.
                                        fluid.source().get(),
                                        new Item.Properties()
                                                .stacksTo(1)
                                                .craftRemainder(Items.BUCKET)));
        fluid.setBucket(bucket);
        return bucket;
    }

    @SuppressWarnings("unchecked")
    private static DeferredHolder<Item, Item>[] tieredItems(String name) {
        DeferredHolder<Item, Item>[] tiers = new DeferredHolder[6];
        for (int tier = 1; tier <= 6; tier++) {
            tiers[tier - 1] =
                    ITEMS.register(
                            name + "_tier_" + tier,
                            () -> new Item(new Item.Properties().stacksTo(64)));
        }
        return tiers;
    }

    @SuppressWarnings("unchecked")
    private static DeferredHolder<Item, Item>[] upgradeItems(
            String name,
            DeferredHolder<Block, Block>[] blocks,
            DeferredHolder<Item, Item> tierOne) {
        DeferredHolder<Item, Item>[] tiers = new DeferredHolder[6];
        tiers[0] = tierOne;
        for (int tier = 2; tier <= 6; tier++) {
            tiers[tier - 1] =
                    blockItem(
                            "structure_miner_upgrade_" + name + "_tier_" + tier, blocks[tier - 1]);
        }
        return tiers;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
