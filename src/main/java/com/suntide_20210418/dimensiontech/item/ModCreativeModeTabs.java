package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
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
                                    .icon(() -> new ItemStack(ModItems.STRUCT_MARKER.get()))
                                    .title(TranslateHelper.translate(TranslateHelper.itemGroup("tab")))
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ModItems.STRUCT_MARKER.get());
                                                output.accept(ModItems.ENCHANTMENT_MARK.get());
                                                output.accept(ModItems.TIER_1_MYTHIC_MINER.get());
                                            })
                                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
