package com.suntide_20210418.dimensiontech.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ModConfigs {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final MythicMinerTierConfig TIER_1_MYTHIC_MINER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Mythic miner settings by tier").push("mythicMiner");
        TIER_1_MYTHIC_MINER = new MythicMinerTierConfig(builder, "tier1", 1, 0.0D, 1);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private ModConfigs() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static final class MythicMinerTierConfig {
        private final ForgeConfigSpec.IntValue baseParallel;
        private final ForgeConfigSpec.DoubleValue baseLuck;
        private final ForgeConfigSpec.IntValue slotCount;

        private MythicMinerTierConfig(
                ForgeConfigSpec.Builder builder,
                String tier,
                int defaultParallel,
                double defaultLuck,
                int defaultSlotCount) {
            builder.push(tier);
            baseParallel =
                    builder.comment("Base number of loot draws per loot table")
                            .defineInRange("baseParallel", defaultParallel, 1, 1024);
            baseLuck =
                    builder.comment("Luck applied to each loot draw")
                            .defineInRange("baseLuck", defaultLuck, -1024.0D, 1024.0D);
            slotCount =
                    builder.comment("Number of structure marker slots")
                            .defineInRange("slotCount", defaultSlotCount, 1, 54);
            builder.pop();
        }

        public int baseParallel() {
            return baseParallel.get();
        }

        public float baseLuck() {
            return baseLuck.get().floatValue();
        }

        public int slotCount() {
            return slotCount.get();
        }
    }
}
