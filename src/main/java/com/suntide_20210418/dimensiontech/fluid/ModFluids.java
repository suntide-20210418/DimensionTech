package com.suntide_20210418.dimensiontech.fluid;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, DimensionTechMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, DimensionTechMod.MOD_ID);

    public static final EssenceFluid MYTHIC_ESSENCE = essence("mythic_essence", 0xFF57327A);
    public static final EssenceFluid SURGING_MYTHIC_ESSENCE =
            essence("surging_mythic_essence", 0xFFA14F4F);
    public static final EssenceFluid RECURSIVE_ESSENCE = essence("recursive_essence", 0xFFA604A4);
    public static final EssenceFluid SURGING_RECURSIVE_ESSENCE =
            essence("surging_recursive_essence", 0xFF302F10);
    public static final EssenceFluid FRACTAL_ESSENCE = essence("fractal_essence", 0xFF76B38E);

    public static Fluid forMinerTier(int tier) {
        return switch (tier) {
            case 2 -> MYTHIC_ESSENCE.source().get();
            case 3 -> SURGING_MYTHIC_ESSENCE.source().get();
            case 4 -> RECURSIVE_ESSENCE.source().get();
            case 5 -> SURGING_RECURSIVE_ESSENCE.source().get();
            case 6 -> FRACTAL_ESSENCE.source().get();
            default -> null;
        };
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private static EssenceFluid essence(String id, int color) {
        return new EssenceFluid(id, color);
    }

    public static final class EssenceFluid {
        private final RegistryObject<FluidType> type;
        private final RegistryObject<ForgeFlowingFluid.Source> source;
        private final RegistryObject<ForgeFlowingFluid.Flowing> flowing;
        private final Lazy<ForgeFlowingFluid.Properties> properties;
        private RegistryObject<? extends Item> bucket;

        private EssenceFluid(String id, int color) {
            type = FLUID_TYPES.register(id, () -> new EssenceFluidType(id, color));
            properties =
                    Lazy.of(
                            () ->
                                    new ForgeFlowingFluid.Properties(
                                                    type, this::sourceFluid, this::flowingFluid)
                                            .slopeFindDistance(3)
                                            .levelDecreasePerBlock(2)
                                            .bucket(this::bucketItem));
            source = FLUIDS.register(id, () -> new ForgeFlowingFluid.Source(properties.get()));
            flowing =
                    FLUIDS.register(
                            "flowing_" + id, () -> new ForgeFlowingFluid.Flowing(properties.get()));
        }

        public RegistryObject<ForgeFlowingFluid.Source> source() {
            return source;
        }

        public void setBucket(RegistryObject<? extends Item> bucket) {
            this.bucket = bucket;
        }

        private Item bucketItem() {
            return bucket.get();
        }

        private ForgeFlowingFluid.Source sourceFluid() {
            return source.get();
        }

        private ForgeFlowingFluid.Flowing flowingFluid() {
            return flowing.get();
        }
    }

    private ModFluids() {}
}
