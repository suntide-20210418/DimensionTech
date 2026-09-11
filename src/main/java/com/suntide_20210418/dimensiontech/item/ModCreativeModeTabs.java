package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeModeTabs {
    public static final ResourceLocation DIMENSION_TECH_TAB_ID =
            ResourceLocationHelper.modLoc("dimension_tech_tab");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DimensionTechMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> DIMENSION_TECH_TAB =
            CREATIVE_MODE_TABS.register(
                    ResourceLocationHelper.getPath(DIMENSION_TECH_TAB_ID),
                    () ->
                            CreativeModeTab.builder()
                                    .icon(() -> new ItemStack(ModItems.STRUCTURE_MARKER.get()))
                                    .title(
                                            TranslateHelper.translate(
                                                    TranslateHelper.itemGroup("tab")))
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ModItems.STRUCTURE_MARKER.get());
                                                output.accept(ModItems.ENCHANTMENT_MARK.get());
                                                output.accept(
                                                        ModItems.DIMENSION_DECONSTRUCTION_CORE
                                                                .get());
                                                for (RegistryObject<Item> item :
                                                        ModItems.DIMENSION_FRAGMENTS)
                                                    output.accept(item.get());
                                                for (RegistryObject<Item> item :
                                                        ModItems.MINING_TOKENS)
                                                    output.accept(item.get());
                                                output.accept(ModItems.DATA_INTEGRATOR.get());
                                                output.accept(ModItems.STRUCTURE_INTERPRETER.get());
                                                output.accept(
                                                        ModItems.STRUCTURE_DATA_OPERATOR.get());
                                                output.accept(ModItems.MYTHIC_CRUCIBLE.get());
                                                output.accept(ModItems.MYTHIC_ESSENCE_BUCKET.get());
                                                output.accept(
                                                        ModItems.SURGING_MYTHIC_ESSENCE_BUCKET
                                                                .get());
                                                output.accept(
                                                        ModItems.RECURSIVE_ESSENCE_BUCKET.get());
                                                output.accept(
                                                        ModItems.SURGING_RECURSIVE_ESSENCE_BUCKET
                                                                .get());
                                                output.accept(
                                                        ModItems.FRACTAL_ESSENCE_BUCKET.get());
                                                output.accept(ModItems.TIER_1_MYTHIC_MINER.get());
                                                output.accept(ModItems.TIER_2_MYTHIC_MINER.get());
                                                output.accept(ModItems.TIER_3_MYTHIC_MINER.get());
                                                output.accept(ModItems.TIER_4_MYTHIC_MINER.get());
                                                output.accept(ModItems.TIER_5_MYTHIC_MINER.get());
                                                output.accept(ModItems.TIER_6_MYTHIC_MINER.get());
                                                output.accept(ModItems.MYTHIC_MINER_CASING.get());
                                                output.accept(
                                                        ModItems.MYTHIC_MINER_STRUCTURE.get());
                                                output.accept(ModItems.UPGRADE_NONE.get());
                                                for (RegistryObject<Item> upgrade :
                                                        ModItems.UPGRADE_EFFICIENCY_TIERS)
                                                    output.accept(upgrade.get());
                                                for (RegistryObject<Item> upgrade :
                                                        ModItems.UPGRADE_ENERGY_TIERS)
                                                    output.accept(upgrade.get());
                                                for (RegistryObject<Item> upgrade :
                                                        ModItems.UPGRADE_PARALLEL_TIERS)
                                                    output.accept(upgrade.get());
                                                for (RegistryObject<Item> upgrade :
                                                        ModItems.UPGRADE_LUCK_TIERS)
                                                    output.accept(upgrade.get());
                                                for (RegistryObject<Item> upgrade :
                                                        ModItems.UPGRADE_AGGREGATE_TIERS)
                                                    output.accept(upgrade.get());
                                                for (RegistryObject<Item> focus :
                                                        ModItems.DIMENSION_FOCUS)
                                                    output.accept(focus.get());
                                            })
                                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
