package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.MythicMinerScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = DimensionTechMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> MenuScreens.register(ModMenu.MYTHIC_MINER.get(), MythicMinerScreen::new));
        event.enqueueWork(
                () ->
                        MenuScreens.register(
                                ModMenu.STRUCTURE_DATA_OPERATOR.get(),
                                StructureDataOperatorScreen::new));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                new DynamicFluidContainerModel.Colors(),
                ModItems.MYTHIC_ESSENCE_BUCKET.get(),
                ModItems.SURGING_MYTHIC_ESSENCE_BUCKET.get(),
                ModItems.RECURSIVE_ESSENCE_BUCKET.get(),
                ModItems.SURGING_RECURSIVE_ESSENCE_BUCKET.get(),
                ModItems.FRACTAL_ESSENCE_BUCKET.get());
    }
}
