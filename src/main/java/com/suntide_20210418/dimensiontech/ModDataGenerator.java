package com.suntide_20210418.dimensiontech;

import com.suntide_20210418.dimensiontech.datagen.ModEnusLangProvider;
import com.suntide_20210418.dimensiontech.datagen.ModItemModelsProvider;
import com.suntide_20210418.dimensiontech.datagen.ModRecipesProvider;
import com.suntide_20210418.dimensiontech.datagen.ModZhcnLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new ModRecipesProvider(packOutput));

        generator.addProvider(
                event.includeClient(), new ModItemModelsProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModEnusLangProvider(packOutput));
        generator.addProvider(event.includeClient(), new ModZhcnLangProvider(packOutput));
    }
}
