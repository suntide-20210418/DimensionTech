package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {
    public static final ResourceLocation DIMENSION_TECH_TAB_ID =
            ResourceLocationHelper.modLoc("dimension_tech_tab");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DimensionTechMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DIMENSION_TECH_TAB =
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
                                                output.accept(ModItems.CHEST_MARKER.get());
                                                output.accept(
                                                        ModItems.DIMENSION_DECONSTRUCTION_CORE
                                                                .get());
                                                for (DeferredHolder<Item, Item> item :
                                                        ModItems.DIMENSION_FRAGMENTS)
                                                    output.accept(item.get());
                                                for (DeferredHolder<Item, Item> item :
                                                        ModItems.MINING_TOKENS)
                                                    output.accept(item.get());
                                                output.accept(ModItems.DATA_INTEGRATOR.get());
                                                output.accept(ModItems.STRUCTURE_INTERPRETER.get());
                                                output.accept(ModItems.WRENCH.get());
                                                output.accept(
                                                        ModItems.STRUCTURE_DATA_OPERATOR.get());
                                                output.accept(ModItems.STRUCTURE_REACTOR.get());
                                                output.accept(
                                                        ModItems.STRUCTURE_ESSENCE_BUCKET.get());
                                                output.accept(
                                                        ModItems.SURGING_STRUCTURE_ESSENCE_BUCKET
                                                                .get());
                                                output.accept(
                                                        ModItems.RECURSIVE_ESSENCE_BUCKET.get());
                                                output.accept(
                                                        ModItems.SURGING_RECURSIVE_ESSENCE_BUCKET
                                                                .get());
                                                output.accept(
                                                        ModItems.FRACTAL_ESSENCE_BUCKET.get());
                                                output.accept(
                                                        ModItems.TIER_1_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.TIER_2_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.TIER_3_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.TIER_4_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.TIER_5_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.TIER_6_STRUCTURE_MINER.get());
                                                output.accept(
                                                        ModItems.STRUCTURE_MINER_CASING.get());
                                                output.accept(
                                                        ModItems.STRUCTURE_MINER_STRUCTURE.get());
                                                output.accept(ModItems.STRUCTURE_MINER_GLASS.get());
                                                for (DeferredHolder<Item, Item> upgrade :
                                                        ModItems.UPGRADE_EFFICIENCY_TIERS)
                                                    output.accept(upgrade.get());
                                                for (DeferredHolder<Item, Item> upgrade :
                                                        ModItems.UPGRADE_ENERGY_TIERS)
                                                    output.accept(upgrade.get());
                                                for (DeferredHolder<Item, Item> upgrade :
                                                        ModItems.UPGRADE_PARALLEL_TIERS)
                                                    output.accept(upgrade.get());
                                                for (DeferredHolder<Item, Item> upgrade :
                                                        ModItems.UPGRADE_LUCK_TIERS)
                                                    output.accept(upgrade.get());
                                                for (DeferredHolder<Item, Item> upgrade :
                                                        ModItems.UPGRADE_AGGREGATE_TIERS)
                                                    output.accept(upgrade.get());
                                            })
                                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
