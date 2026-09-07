package com.suntide_20210418.dimensiontech.integration.kubejs;

import com.suntide_20210418.dimensiontech.utils.MinerScriptConfigService;
import com.suntide_20210418.dimensiontech.utils.StructureScriptConfigService;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class DimensionTechJS {
    private DimensionTechJS() {}

    public static String version() {
        return "1.0.0-1.20.1";
    }

    public static MinerConfigJS miner(Object id) {
        return new MinerConfigJS(parse(id));
    }

    public static MinerConfigJS miner(Object id, Object options) {
        MinerConfigJS config = new MinerConfigJS(parse(id));
        if (options instanceof java.util.Map<?, ?> map) {
            map.forEach((key, value) -> config.set(String.valueOf(key), value));
        } else if (options != null) {
            throw new IllegalArgumentException("miner options must be an object");
        }
        return config;
    }

    public static ResourceLocation structure(Object id) {
        return parse(id);
    }

    public static StructureConfigJS structureConfig() {
        return new StructureConfigJS();
    }

    static ResourceLocation parse(Object id) {
        if (id instanceof ResourceLocation rl) return rl;
        if (id == null) throw new IllegalArgumentException("id is required");
        ResourceLocation rl = ResourceLocation.tryParse(String.valueOf(id));
        if (rl == null) throw new IllegalArgumentException("Invalid resource location: " + id);
        return rl;
    }

    public static final class StructureConfigJS {
        public StructureConfigJS dimensionValue(Object id, double value) {
            check(value);
            StructureScriptConfigService.dimensionValue(parse(id), value);
            return this;
        }

        public StructureConfigJS itemMultiplier(String regex, double value) {
            check(value);
            StructureScriptConfigService.itemMultiplier(regex, value);
            return this;
        }

        public StructureConfigJS dimensionWhitelist(List<String> values) {
            StructureScriptConfigService.whitelist("dimension", values);
            return this;
        }

        public StructureConfigJS dimensionBlacklist(List<String> values) {
            StructureScriptConfigService.blacklist("dimension", values);
            return this;
        }

        public StructureConfigJS structureWhitelist(List<String> values) {
            StructureScriptConfigService.whitelist("structure", values);
            return this;
        }

        public StructureConfigJS structureBlacklist(List<String> values) {
            StructureScriptConfigService.blacklist("structure", values);
            return this;
        }

        public StructureConfigJS itemWhitelist(List<String> values) {
            StructureScriptConfigService.whitelist("item", values);
            return this;
        }

        public StructureConfigJS itemBlacklist(List<String> values) {
            StructureScriptConfigService.blacklist("item", values);
            return this;
        }

        public StructureConfigJS rarityMultiplier(String rarity, double value) {
            check(value);
            StructureScriptConfigService.rarity(rarity, value);
            return this;
        }

        public StructureConfigJS itemExpectationMethod(String method) {
            StructureScriptConfigService.expectationMethod(method);
            return this;
        }

        public StructureConfigJS samplingCount(int value) {
            if (value < 1) throw new IllegalArgumentException("samplingCount must be positive");
            StructureScriptConfigService.samplingCount(value);
            return this;
        }

        public StructureConfigJS virtualStructureSamples(int value) {
            if (value < 1)
                throw new IllegalArgumentException("virtualStructureSamples must be positive");
            StructureScriptConfigService.virtualSamples(value);
            return this;
        }

        public StructureConfigJS virtualStructureStepsPerTick(int value) {
            if (value < 1)
                throw new IllegalArgumentException("virtualStructureStepsPerTick must be positive");
            StructureScriptConfigService.stepsPerTick(value);
            return this;
        }

        private static void check(double value) {
            if (!Double.isFinite(value) || value < 0)
                throw new IllegalArgumentException("value must be finite and non-negative");
        }
    }

    public static final class MinerConfigJS {
        private final ResourceLocation id;
        private Integer processingTime, energyConsumption, energyCapacity, baseParallel;
        private Double efficiency;
        private Float luck;
        private Boolean requiresFluid;

        MinerConfigJS(ResourceLocation id) {
            this.id = id;
        }

        public MinerConfigJS processingTime(int v) {
            processingTime = v;
            save();
            return this;
        }

        public MinerConfigJS energyConsumption(int v) {
            energyConsumption = v;
            save();
            return this;
        }

        public MinerConfigJS energyCapacity(int v) {
            energyCapacity = v;
            save();
            return this;
        }

        public MinerConfigJS baseParallel(int v) {
            baseParallel = v;
            save();
            return this;
        }

        public MinerConfigJS efficiency(double v) {
            efficiency = v;
            save();
            return this;
        }

        public MinerConfigJS luck(double v) {
            luck = (float) v;
            save();
            return this;
        }

        public MinerConfigJS requiresFluid(boolean v) {
            requiresFluid = v;
            save();
            return this;
        }

        private void save() {
            MinerScriptConfigService.merge(
                    id,
                    processingTime,
                    energyConsumption,
                    energyCapacity,
                    baseParallel,
                    efficiency,
                    luck,
                    requiresFluid);
        }

        private void set(String key, Object value) {
            switch (key) {
                case "processingTime" -> processingTime((int) number(value, key));
                case "energyConsumption" -> energyConsumption((int) number(value, key));
                case "energyCapacity" -> energyCapacity((int) number(value, key));
                case "baseParallel" -> baseParallel((int) number(value, key));
                case "efficiency" -> efficiency(number(value, key));
                case "luck" -> luck(number(value, key));
                case "requiresFluid" -> {
                    if (!(value instanceof Boolean b))
                        throw new IllegalArgumentException(key + " must be boolean");
                    requiresFluid(b);
                }
                default -> throw new IllegalArgumentException("Unknown miner option: " + key);
            }
        }

        private static double number(Object value, String key) {
            if (!(value instanceof Number n))
                throw new IllegalArgumentException(key + " must be numeric");
            return n.doubleValue();
        }
    }
}
