package com.suntide_20210418.dimensiontech;

import com.suntide_20210418.dimensiontech.datagen.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = DimensionTechMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // This event is only fired while data generation is running, which makes it the right place
        // to make the JVM terminate once the providers are done. See DatagenExitWatchdog.
        DatagenExitWatchdog.install();

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        // 1.21 起 RecipeProvider / BlockLootSubProvider 都需要一个 HolderLookup.Provider，由事件提供。
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        generator.addProvider(
                event.includeServer(), new ModRecipesProvider(packOutput, registries));
        generator.addProvider(
                event.includeServer(),
                new LootTableProvider(
                        packOutput,
                        Set.of(),
                        List.of(
                                new LootTableProvider.SubProviderEntry(
                                        ModBlockLootTablesProvider::new,
                                        LootContextParamSets.BLOCK)),
                        registries));

        generator.addProvider(
                event.includeClient(), new ModItemModelsProvider(packOutput, existingFileHelper));
        generator.addProvider(
                event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModEnusLangProvider(packOutput));
        generator.addProvider(event.includeClient(), new ModZhcnLangProvider(packOutput));
    }
}
