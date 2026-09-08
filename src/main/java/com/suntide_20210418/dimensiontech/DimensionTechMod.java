package com.suntide_20210418.dimensiontech;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.LootAnalysisFingerprintGameTests;
import com.suntide_20210418.dimensiontech.block.entity.ModBlockEntities;
import com.suntide_20210418.dimensiontech.block.entity.MythicMinerLootMergeGameTests;
import com.suntide_20210418.dimensiontech.block.entity.MythicMinerMarkerAnalysisCacheGameTests;
import com.suntide_20210418.dimensiontech.mythicminer.output.MythicMinerOutputRouterGameTests;
import com.suntide_20210418.dimensiontech.block.entity.MythicMinerTierGameTests;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.gametest.LootExpectationGameTests;
import com.suntide_20210418.dimensiontech.gametest.MythicMinerJadeGameTests;
import com.suntide_20210418.dimensiontech.gametest.MythicMinerTickContractGameTests;
import com.suntide_20210418.dimensiontech.item.ModCreativeModeTabs;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculatorGameTests;
import com.suntide_20210418.dimensiontech.utils.VirtualStructureSamplerGameTests;
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
        ModFluids.register(modEventBus);
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
        event.enqueueWork(ModNetwork::register);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(LootExpectationGameTests.class);
        event.register(MythicMinerTickContractGameTests.class);
        event.register(MythicMinerJadeGameTests.class);
        event.register(MythicMinerLootMergeGameTests.class);
        event.register(MythicMinerMarkerAnalysisCacheGameTests.class);
        event.register(MythicMinerOutputRouterGameTests.class);
        event.register(MythicMinerTierGameTests.class);
        event.register(LootAnalysisFingerprintGameTests.class);
        event.register(StructureValueCalculatorGameTests.class);
        event.register(VirtualStructureSamplerGameTests.class);
    }
}
