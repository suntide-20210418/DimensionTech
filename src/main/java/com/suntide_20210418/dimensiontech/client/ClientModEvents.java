package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureReactorScreen;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureMinerScreen;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
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
                () -> MenuScreens.register(ModMenu.STRUCTURE_MINER.get(), StructureMinerScreen::new));
        event.enqueueWork(
                () ->
                        MenuScreens.register(
                                ModMenu.STRUCTURE_REACTOR.get(), StructureReactorScreen::new));
        event.enqueueWork(
                () ->
                        MenuScreens.register(
                                ModMenu.STRUCTURE_DATA_OPERATOR.get(),
                                StructureDataOperatorScreen::new));
        // The reactor model's glass window shares the block atlas, whose transparent pixels the
        // default solid pass would write out as opaque black. Cutout discards on alpha so the
        // window stays see-through.
        event.enqueueWork(
                () ->
                        ItemBlockRenderTypes.setRenderLayer(
                                ModBlocks.STRUCTURE_REACTOR.get(), RenderType.cutout()));
        // The operator's model samples genuinely transparent regions of its atlas (screen
        // cutouts, hollow interior), which the solid pass would paint opaque black.
        event.enqueueWork(
                () ->
                        ItemBlockRenderTypes.setRenderLayer(
                                ModBlocks.STRUCTURE_DATA_OPERATOR.get(), RenderType.cutout()));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                new DynamicFluidContainerModel.Colors(),
                ModItems.STRUCTURE_ESSENCE_BUCKET.get(),
                ModItems.SURGING_STRUCTURE_ESSENCE_BUCKET.get(),
                ModItems.RECURSIVE_ESSENCE_BUCKET.get(),
                ModItems.SURGING_RECURSIVE_ESSENCE_BUCKET.get(),
                ModItems.FRACTAL_ESSENCE_BUCKET.get());
    }
}
