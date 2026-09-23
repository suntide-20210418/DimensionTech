package com.suntide_20210418.dimensiontech;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.ModBlockEntities;
import com.suntide_20210418.dimensiontech.block.entity.ModCapabilities;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.item.ModCreativeModeTabs;
import com.suntide_20210418.dimensiontech.item.ModDataComponents;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.recipe.ModRecipes;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DimensionTechMod.MOD_ID)
public class DimensionTechMod {
    public static final String MOD_ID = "dimension_tech";

    public static final Logger LOGGER = LogUtils.getLogger();

    public DimensionTechMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Dimension Tech Mod is loading");

        // Config registration has to happen as early as possible.
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigs.COMMON_SPEC);

        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModDataComponents.register(modEventBus);

        // Game event listeners.
        NeoForge.EVENT_BUS.register(this);
        ModMenu.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(DimensionTechMod::commonSetup);
        modEventBus.addListener(ModCapabilities::register);
        // ModNetwork.register() still carries Forge's zero-arg SimpleChannel signature, so it cannot
        // be wired as a mod-bus listener yet; the network migration owns that line.
        modEventBus.addListener(ModNetwork::register);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(StructureReactorRecipes::resetDefaults);
    }
}
