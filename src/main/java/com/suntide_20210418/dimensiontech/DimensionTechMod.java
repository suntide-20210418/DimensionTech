package com.suntide_20210418.dimensiontech;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.ModBlockEntities;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.gametest.LootExpectationGameTests;
import com.suntide_20210418.dimensiontech.item.ModCreativeModeTabs;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DimensionTechMod.MOD_ID)
public class DimensionTechMod {
    public static final String MOD_ID = "dimension_tech";

    public static final Logger LOGGER = LogUtils.getLogger();

    public DimensionTechMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        LOGGER.info("Dimension Tech Mod is loading");

        // 注册配置文件（需在最早时机注册）
        ModConfigs.register(context);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
        ModMenu.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(DimensionTechMod::commonSetup);
        modEventBus.addListener(DimensionTechMod::registerGameTests);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing card modes...");
        event.enqueueWork(ModNetwork::register);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(LootExpectationGameTests.class);
    }
}
