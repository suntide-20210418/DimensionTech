package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureMinerScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureReactorScreen;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@EventBusSubscriber(
        modid = DimensionTechMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () ->
                        MenuScreens.register(
                                ModMenu.STRUCTURE_MINER.get(), StructureMinerScreen::new));
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
        // The glass body is genuinely translucent (alpha ramp, not a two-state cutout), so it needs
        // the translucent pass to blend; cutout would hard-render every sub-threshold pixel opaque.
        event.enqueueWork(
                () ->
                        ItemBlockRenderTypes.setRenderLayer(
                                ModBlocks.STRUCTURE_MINER_GLASS.get(), RenderType.translucent()));
    }

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.CHEST_ANALYSE);
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
