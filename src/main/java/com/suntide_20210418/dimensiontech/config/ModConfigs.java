package com.suntide_20210418.dimensiontech.config;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ModConfigs {
    public enum ItemExpectationMethod {
        EXACT_THEN_SAMPLING,
        SAMPLING
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final MythicMinerTierConfig TIER_1_MYTHIC_MINER;
    public static final StructureValueConfig STRUCTURE_VALUE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Mythic miner settings by tier").push("mythicMiner");
        TIER_1_MYTHIC_MINER =
                new MythicMinerTierConfig(builder, "tier1", 1, 0.0D, 1, 100_000, 100);
        builder.pop();
        STRUCTURE_VALUE = new StructureValueConfig(builder);
        COMMON_SPEC = builder.build();
    }

    private ModConfigs() {}

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static final class StructureValueConfig {
        private final ForgeConfigSpec.DoubleValue commonMultiplier;
        private final ForgeConfigSpec.DoubleValue uncommonMultiplier;
        private final ForgeConfigSpec.DoubleValue rareMultiplier;
        private final ForgeConfigSpec.DoubleValue epicMultiplier;
        private final ForgeConfigSpec.EnumValue<ItemExpectationMethod> itemExpectationMethod;
        private final ForgeConfigSpec.IntValue samplingCount;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionValues;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> itemMultipliers;

        private StructureValueConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Structure marker value calculation").push("structureValue");
            commonMultiplier = multiplier(builder, "commonRarityMultiplier", 1.0D);
            uncommonMultiplier = multiplier(builder, "uncommonRarityMultiplier", 5.0D);
            rareMultiplier = multiplier(builder, "rareRarityMultiplier", 10.0D);
            epicMultiplier = multiplier(builder, "epicRarityMultiplier", 50.0D);
            itemExpectationMethod =
                    builder.comment(
                                    "Per-item expectation method",
                                    "EXACT_THEN_SAMPLING prefers exact analysis and falls back to Monte Carlo",
                                    "SAMPLING always uses Monte Carlo")
                            .defineEnum(
                                    "itemExpectationMethod", ItemExpectationMethod.EXACT_THEN_SAMPLING);
            samplingCount =
                    builder.comment("Monte Carlo samples per loot table")
                            .defineInRange("samplingCount", 1000, 1, 1_000_000);
            dimensionValues =
                    builder.comment(
                                    "Dimension value entries in dimension_id=value format",
                                    "Dimensions not listed here use 1.0")
                            .defineListAllowEmpty(
                                    "dimensionValues",
                                    List.of(
                                            "minecraft:overworld=1.0",
                                            "minecraft:the_nether=10.0",
                                            "minecraft:the_end=20.0",
                                            "allthemodium:the_other=100.0"),
                                    StructureValueConfig::isDimensionValueEntry);
            itemMultipliers =
                    builder.comment(
                                    "Per-item rarity multiplier overrides in regex=value format",
                                    "Regexes are matched against the complete item ID",
                                    "Items not matched here use their rarity multiplier above")
                            .defineListAllowEmpty(
                                    "itemMultipliers", List.of(
                                            "iron=2.0",
                                            "gold=5.0",
                                            "diamond=10.0",
                                            "netherite=20.0",
                                            "allthemodium=50.0",
                                            "vibranium=100.0",
                                            "unobtainium=500.0",
                                            "upgrade_smithing_template=50.0"
                                    ), StructureValueConfig::isItemMultiplierEntry);
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

        private static boolean isItemMultiplierEntry(Object value) {
            if (!(value instanceof String entry)) return false;
            int separator = entry.lastIndexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) return false;
            try {
                double multiplier = Double.parseDouble(entry.substring(separator + 1).trim());
                Pattern.compile(entry.substring(0, separator).trim());
                return Double.isFinite(multiplier)
                        && multiplier >= 0.0D;
            } catch (NumberFormatException | PatternSyntaxException exception) {
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

        public double itemMultiplier(Item item, Rarity rarity) {
            ResourceLocation itemId =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) return rarityMultiplier(rarity);
            for (String entry : itemMultipliers.get()) {
                int separator = entry.lastIndexOf('=');
                Pattern matcher = Pattern.compile(entry.substring(0, separator).trim());
                if (matcher.matcher(itemId.toString()).find()) {
                    return Double.parseDouble(entry.substring(separator + 1).trim());
                }
            }
            return rarityMultiplier(rarity);
        }

        public double itemMultiplier(TerminalStackKey key) {
            return itemMultiplier(key.item(), key.rarity());
        }

        public String calculationFingerprint() {
            return commonMultiplier.get()
                    + "|"
                    + uncommonMultiplier.get()
                    + "|"
                    + rareMultiplier.get()
                    + "|"
                    + epicMultiplier.get()
                    + "|"
                    + itemExpectationMethod.get()
                    + "|"
                    + samplingCount.get()
                    + "|"
                    + dimensionValues.get()
                    + "|"
                    + itemMultipliers.get();
        }

        public ItemExpectationMethod itemExpectationMethod() {
            return itemExpectationMethod.get();
        }

        public int samplingCount() {
            return samplingCount.get();
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
        private final ForgeConfigSpec.DoubleValue efficiency;
        private final ForgeConfigSpec.DoubleValue quantityReference;

        private MythicMinerTierConfig(
                ForgeConfigSpec.Builder builder,
                String tier,
                int defaultParallel,
                double defaultLuck,
                int defaultSlotCount,
                int defaultEnergyCapacity,
                int defaultEnergyConsumption) {
            builder.push(tier);
            baseParallel =
                    builder.comment("Base number of loot draws per loot table")
                            .defineInRange("baseParallel", defaultParallel, 1, 1024);
            baseLuck =
                    builder.comment("Luck applied when calculating structure loot expectations")
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
            efficiency =
                    builder.comment("Machine efficiency used to convert structure value to ticks")
                            .defineInRange("efficiency", 1.0D, 0.000001D, Double.MAX_VALUE);
            quantityReference =
                    builder.comment("Reference expected item quantity for output scaling")
                            .defineInRange("quantityReference", 16.0D, 0.000001D, Double.MAX_VALUE);
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

        public double efficiency() {
            return efficiency.get();
        }

        public double quantityReference() {
            return quantityReference.get();
        }
    }
}
