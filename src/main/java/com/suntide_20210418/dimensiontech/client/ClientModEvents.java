package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureMinerScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureReactorScreen;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@EventBusSubscriber(
        modid = DimensionTechMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {}

    /**
     * Screens are registered here rather than through {@code MenuScreens.register}, which vanilla
     * made private in 1.21 and now points at this event.
     *
     * <p>The three machine models no longer pick their render pass from code: the cutout / translucent
     * passes are declared in the block model JSONs as {@code "render_type"}, which is the route the
     * deprecated {@code ItemBlockRenderTypes.setRenderLayer} asked mods to take.
     */
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenu.STRUCTURE_MINER.get(), StructureMinerScreen::new);
        event.register(ModMenu.STRUCTURE_REACTOR.get(), StructureReactorScreen::new);
        event.register(ModMenu.STRUCTURE_DATA_OPERATOR.get(), StructureDataOperatorScreen::new);
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