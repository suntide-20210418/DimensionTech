package com.suntide_20210418.dimensiontech.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ModConfigs {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final MythicMinerTierConfig TIER_1_MYTHIC_MINER;
    public static final StructureValueConfig STRUCTURE_VALUE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Mythic miner settings by tier").push("mythicMiner");
        TIER_1_MYTHIC_MINER =
                new MythicMinerTierConfig(builder, "tier1", 1, 0.0D, 1, 100_000, 100, 400);
        builder.pop();
        STRUCTURE_VALUE = new StructureValueConfig(builder);
        SERVER_SPEC = builder.build();
    }

    private ModConfigs() {}

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static final class StructureValueConfig {
        private final ForgeConfigSpec.DoubleValue commonMultiplier;
        private final ForgeConfigSpec.DoubleValue uncommonMultiplier;
        private final ForgeConfigSpec.DoubleValue rareMultiplier;
        private final ForgeConfigSpec.DoubleValue epicMultiplier;
        private final ForgeConfigSpec.DoubleValue luck;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionValues;

        private StructureValueConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Structure marker value calculation").push("structureValue");
            commonMultiplier = multiplier(builder, "commonRarityMultiplier", 1.0D);
            uncommonMultiplier = multiplier(builder, "uncommonRarityMultiplier", 10.0D);
            rareMultiplier = multiplier(builder, "rareRarityMultiplier", 50.0D);
            epicMultiplier = multiplier(builder, "epicRarityMultiplier", 100.0D);
            luck = builder.comment("Luck used for exact structure loot analysis")
                    .defineInRange("luck", 0.0D, -1024.0D, 1024.0D);
            dimensionValues =
                    builder.comment(
                                    "Dimension value entries in dimension_id=value format",
                                    "Dimensions not listed here use 1.0")
                            .defineListAllowEmpty(
                                    "dimensionValues",
                                    List.of(
                                            "minecraft:overworld=1.0",
                                            "minecraft:the_nether=10.0",
                                            "minecraft:the_end=20.0"),
                                    StructureValueConfig::isDimensionValueEntry);
            builder.pop();
        }

        private static ForgeConfigSpec.DoubleValue multiplier(
                ForgeConfigSpec.Builder builder, String name, double defaultValue) {
            return builder.defineInRange(name, defaultValue, 0.0D, Double.MAX_VALUE);
        }

        private static boolean isDimensionValueEntry(Object value) {
            if (!(value instanceof String entry)) {
                return false;
            }
            int separator = entry.lastIndexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                return false;
            }
            try {
                double dimensionValue = Double.parseDouble(entry.substring(separator + 1).trim());
                return ResourceLocation.tryParse(entry.substring(0, separator).trim()) != null
                        && Double.isFinite(dimensionValue)
                        && dimensionValue >= 0.0D;
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        public double rarityMultiplier(Rarity rarity) {
            return switch (rarity) {
                case COMMON -> commonMultiplier.get();
                case UNCOMMON -> uncommonMultiplier.get();
                case RARE -> rareMultiplier.get();
                case EPIC -> epicMultiplier.get();
            };
        }

        public float luck() {
            return luck.get().floatValue();
        }

        public double dimensionValue(ResourceLocation dimension) {
            for (String entry : dimensionValues.get()) {
                int separator = entry.lastIndexOf('=');
                ResourceLocation configuredDimension =
                        ResourceLocation.tryParse(entry.substring(0, separator).trim());
                if (dimension.equals(configuredDimension)) {
                    return Double.parseDouble(entry.substring(separator + 1).trim());
                }
            }
            return 1.0D;
        }
    }

    public static final class MythicMinerTierConfig {
        private final ForgeConfigSpec.IntValue baseParallel;
        private final ForgeConfigSpec.DoubleValue baseLuck;
        private final ForgeConfigSpec.IntValue slotCount;
        private final ForgeConfigSpec.IntValue energyCapacity;
        private final ForgeConfigSpec.IntValue energyConsumption;
        private final ForgeConfigSpec.IntValue processingTime;

        private MythicMinerTierConfig(
                ForgeConfigSpec.Builder builder,
                String tier,
                int defaultParallel,
                double defaultLuck,
                int defaultSlotCount,
                int defaultEnergyCapacity,
                int defaultEnergyConsumption,
                int defaultProcessingTime) {
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
            energyCapacity =
                    builder.comment("Internal energy capacity in FE")
                            .defineInRange(
                                    "energyCapacity", defaultEnergyCapacity, 1, Integer.MAX_VALUE);
            energyConsumption =
                    builder.comment("Energy consumed per processing tick in FE")
                            .defineInRange(
                                    "energyConsumption",
                                    defaultEnergyConsumption,
                                    1,
                                    Integer.MAX_VALUE);
            processingTime =
                    builder.comment("Powered ticks required to produce loot")
                            .defineInRange(
                                    "processingTime", defaultProcessingTime, 1, Integer.MAX_VALUE);
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

        public int energyCapacity() {
            return energyCapacity.get();
        }

        public int energyConsumption() {
            return energyConsumption.get();
        }

        public int processingTime() {
            return processingTime.get();
        }
    }
}
