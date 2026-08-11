package com.suntide_20210418.dimensiontech;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
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
        LOGGER.info("DimensionTechMod is loading");

        // 注册配置文件（需在最早时机注册）
//        ModConfigs.register();
//
//        ModItems.register(modEventBus);
//        ModCreativeModeTabs.register(modEventBus);

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
//        ModMenu.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(DimensionTechMod::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing card modes...");
    }
}