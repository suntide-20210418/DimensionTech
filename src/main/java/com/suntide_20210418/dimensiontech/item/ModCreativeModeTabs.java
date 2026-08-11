package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DimensionTechMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> DIMENSION_TECH_TAB =
            CREATIVE_MODE_TABS.register(
                    "dimension_tech_tab",
                    () ->
                            CreativeModeTab.builder()
                                    .icon(() -> new ItemStack(ModItems.STRUCT_MARKER.get()))
                                    .title(Component.translatable("itemGroup.dimension_tech.tab"))
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ModItems.STRUCT_MARKER.get());
                                            })
                                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
