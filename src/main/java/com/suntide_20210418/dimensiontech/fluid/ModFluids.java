package com.suntide_20210418.dimensiontech.fluid;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, DimensionTechMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, DimensionTechMod.MOD_ID);

    public static final EssenceFluid STRUCTURE_ESSENCE = essence("structure_essence", 0xFF57327A);
    public static final EssenceFluid SURGING_STRUCTURE_ESSENCE =
            essence("surging_structure_essence", 0xFFA14F4F);
    public static final EssenceFluid RECURSIVE_ESSENCE = essence("recursive_essence", 0xFFA604A4);
    public static final EssenceFluid SURGING_RECURSIVE_ESSENCE =
            essence("surging_recursive_essence", 0xFF302F10);
    public static final EssenceFluid FRACTAL_ESSENCE = essence("fractal_essence", 0xFF76B38E);

    /** 全部精华流体，声明顺序。供客户端注册 {@code IClientFluidTypeExtensions} 遍历使用。 */
    public static final List<EssenceFluid> ALL =
            List.of(
                    STRUCTURE_ESSENCE,
                    SURGING_STRUCTURE_ESSENCE,
                    RECURSIVE_ESSENCE,
                    SURGING_RECURSIVE_ESSENCE,
                    FRACTAL_ESSENCE);

    public static Fluid forMinerTier(int tier) {
        return switch (tier) {
            case 1 -> Fluids.WATER;
            case 2 -> STRUCTURE_ESSENCE.source().get();
            case 3 -> SURGING_STRUCTURE_ESSENCE.source().get();
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
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final Lazy<BaseFlowingFluid.Properties> properties;
        private final int color;
        private DeferredHolder<Item, ? extends Item> bucket;

        private EssenceFluid(String id, int color) {
            this.color = color;
            type = FLUID_TYPES.register(id, () -> new EssenceFluidType(id, color));
            properties =
                    Lazy.of(
                            () ->
                                    new BaseFlowingFluid.Properties(
                                                    type, this::sourceFluid, this::flowingFluid)
                                            .slopeFindDistance(3)
                                            .levelDecreasePerBlock(2)
                                            .bucket(this::bucketItem));
            source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties.get()));
            flowing =
                    FLUIDS.register(
                            "flowing_" + id, () -> new BaseFlowingFluid.Flowing(properties.get()));
        }

        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source() {
            return source;
        }

        public DeferredHolder<FluidType, FluidType> type() {
            return type;
        }

        /** 客户端着色用的 ARGB。 */
        public int color() {
            return color;
        }

        public void setBucket(DeferredHolder<Item, ? extends Item> bucket) {
            this.bucket = bucket;
        }

        private Item bucketItem() {
            return bucket.get();
        }

        private BaseFlowingFluid.Source sourceFluid() {
            return source.get();
        }

        private BaseFlowingFluid.Flowing flowingFluid() {
            return flowing.get();
        }
    }

    private ModFluids() {}
}
