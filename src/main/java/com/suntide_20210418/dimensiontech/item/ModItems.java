package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final ResourceLocation STRUCT_MARKER_ID =
            ResourceLocationHelper.item("struct_marker");
    public static final ResourceLocation ENCHANTMENT_MARK_ID =
            ResourceLocationHelper.item("enchantment_mark");

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DimensionTechMod.MOD_ID);

    public static final RegistryObject<Item> STRUCT_MARKER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(STRUCT_MARKER_ID),
                    () -> new StructMarkerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENCHANTMENT_MARK =
            ITEMS.register(
                    ResourceLocationHelper.getPath(ENCHANTMENT_MARK_ID),
                    () -> new EnchantmentMarkItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> TIER_1_MYTHIC_MINER =
            ITEMS.register(
                    ResourceLocationHelper.getPath(ModBlocks.TIER_1_MYTHIC_MINER_ID),
                    () ->
                            new BlockItem(
                                    ModBlocks.TIER_1_MYTHIC_MINER.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
