package com.suntide_20210418.dimensiontech.integration.kubejs;

import com.suntide_20210418.dimensiontech.structurereactor.OperationMatcher;
import com.suntide_20210418.dimensiontech.structurereactor.StateStep;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipe;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipes;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfigService;
import com.suntide_20210418.dimensiontech.utils.StructureScriptConfigService;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class DimensionTechJS {
    private DimensionTechJS() {}

    public static String version() {
        return "1.0.0-1.20.1";
    }

    /** Starts a structure reactor recipe builder: create new or modify an existing recipe by id. */
    public static ReactorRecipeJS reactorRecipe(Object id) {
        return new ReactorRecipeJS(parse(id));
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

    private static Ingredient parseIngredient(Object value) {
        return IngredientJS.of(value);
    }

    private static Fluid parseFluid(Object value) {
        if (value instanceof Fluid fluid) return fluid;
        ResourceLocation rl = ResourceLocation.tryParse(String.valueOf(value));
        if (rl == null) throw new IllegalArgumentException("Invalid fluid id: " + value);
        Fluid fluid = Registries.FLUID.getValue(rl);
        if (fluid == null || fluid.isSame(Fluids.EMPTY))
            throw new IllegalArgumentException("Unknown fluid: " + value);
        return fluid;
    }

    private static List<StateStep<ItemStack>> sequenceWithOverrides(
            List<StateStep<ItemStack>> sequence, Map<String, Ingredient> overrides) {
        if (overrides.isEmpty()) return sequence;
        for (String state : overrides.keySet()) {
            boolean present =
                    sequence.stream().anyMatch(step -> step.state().name().equalsIgnoreCase(state));
            if (!present)
                throw new IllegalArgumentException(
                        "Operation override state not in this branch's DSL: " + state);
        }
        return sequence.stream()
                .map(
                        step -> {
                            Ingredient override =
                                    overrides.get(step.state().name().toLowerCase(Locale.ROOT));
                            return override == null
                                    ? step
                                    : new StateStep<ItemStack>(
                                            step.state(), OperationMatcher.of(override));
                        })
                .toList();
    }

    /**
     * Builds a structure reactor recipe. Call {@code add()} to register; omissions reuse the
     * existing recipe of the same id when present (or fail validation for a fresh recipe).
     */
    public static final class ReactorRecipeJS {
        private final ResourceLocation id;
        private final StructureReactorRecipe<ItemStack> base;
        private Fluid input, output;
        private Ingredient fragment;
        private Integer fragmentCount, baseFluidCost, targetOutput;
        private String dslA, dslB;
        private final Map<String, Ingredient> overridesA = new HashMap<>();
        private final Map<String, Ingredient> overridesB = new HashMap<>();

        ReactorRecipeJS(ResourceLocation id) {
            this.id = id;
            this.base = StructureReactorRecipes.get(id);
        }

        public ReactorRecipeJS input(Object fluid) {
            input = parseFluid(fluid);
            return this;
        }

        public ReactorRecipeJS output(Object fluid) {
            output = parseFluid(fluid);
            return this;
        }

        public ReactorRecipeJS fragment(Object ingredient) {
            fragment = parseIngredient(ingredient);
            return this;
        }

        public ReactorRecipeJS fragmentCount(int value) {
            fragmentCount = value;
            return this;
        }

        public ReactorRecipeJS baseFluidCost(int value) {
            baseFluidCost = value;
            return this;
        }

        public ReactorRecipeJS targetOutput(int value) {
            targetOutput = value;
            return this;
        }

        public ReactorRecipeJS sequenceA(String dsl) {
            dslA = dsl;
            return this;
        }

        public ReactorRecipeJS sequenceB(String dsl) {
            dslB = dsl;
            return this;
        }

        /**
         * Overrides the ritual operation item for one state in one branch. {@code stateName} must
         * occur in that branch's DSL.
         */
        public ReactorRecipeJS overrideOperation(String side, String stateName, Object ingredient) {
            Ingredient parsed = parseIngredient(ingredient);
            (isB(side) ? overridesB : overridesA).put(stateName, parsed);
            return this;
        }

        /** Merges omissions from the current recipe and registers the result. */
        public ReactorRecipeJS add() {
            build();
            return this;
        }

        private void build() {
            List<StateStep<ItemStack>> seqA =
                    sequenceWithOverrides(
                            dslA != null
                                    ? StructureReactorRecipes.sequence(dslA)
                                    : base.sequence(StructureReactorRecipe.Branch.A),
                            overridesA);
            List<StateStep<ItemStack>> seqB =
                    dslB != null
                            ? sequenceWithOverrides(
                                    StructureReactorRecipes.sequence(dslB), overridesB)
                            : base.hasAlternateBranch()
                                    ? sequenceWithOverrides(
                                            base.sequence(StructureReactorRecipe.Branch.B),
                                            overridesB)
                                    : null;
            StructureReactorRecipes.add(
                    new StructureReactorRecipe<>(
                            id,
                            input != null ? input : base.input(),
                            output != null ? output : base.output(),
                            fragment != null ? fragment : base.fragment(),
                            fragmentCount != null ? fragmentCount : base.fragmentCount(),
                            baseFluidCost != null ? baseFluidCost : base.baseFluidCost(),
                            targetOutput != null ? targetOutput : base.targetOutput(),
                            seqA,
                            seqB));
        }

        private static boolean isB(String side) {
            return "B".equalsIgnoreCase(side);
        }
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
