package com.suntide_20210418.dimensiontech.fluid;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

final class EssenceFluidType extends FluidType {
    private final String id;
    private final int color;

    EssenceFluidType(String id, int color) {
        super(Properties.create().density(1200).viscosity(1400));
        this.id = id;
        this.color = color;
    }

    @Override
    public String getDescriptionId() {
        return "fluid." + DimensionTechMod.MOD_ID + "." + id;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(
                new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(
                                "minecraft", "block/water_still");
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(
                                "minecraft", "block/water_flow");
                    }

                    @Override
                    public int getTintColor() {
                        return color;
                    }
                });
    }
}
