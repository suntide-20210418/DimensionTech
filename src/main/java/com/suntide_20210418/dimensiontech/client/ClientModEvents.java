package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureMinerScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureReactorScreen;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

// 不指定 bus：NeoForge 21.1 已忽略该值，改为按事件是否实现 IModBusEvent 自动判定——
// RegisterMenuScreensEvent / RegisterKeyMappingsEvent / RegisterColorHandlersEvent 都是
// IModBusEvent，故仍落在 mod 总线上，与显式指定等价。
@EventBusSubscriber(modid = DimensionTechMod.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {}

    /**
     * Screens are registered here rather than through {@code MenuScreens.register}, which vanilla
     * made private in 1.21 and now points at this event.
     *
     * <p>The three machine models no longer pick their render pass from code: the cutout /
     * translucent passes are declared in the block model JSONs as {@code "render_type"}, which is
     * the route the deprecated {@code ItemBlockRenderTypes.setRenderLayer} asked mods to take.
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

    /** 精华流体的贴图与着色。1.20.1 走 {@code FluidType#initializeClient}，该回调在 1.21 已标记 forRemoval，官方指向本事件。 */
    @SubscribeEvent
    public static void registerFluidTypeExtensions(RegisterClientExtensionsEvent event) {
        for (ModFluids.EssenceFluid essence : ModFluids.ALL) {
            event.registerFluidType(
                    new EssenceFluidClientExtensions(essence.color()), essence.type().get());
        }
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
