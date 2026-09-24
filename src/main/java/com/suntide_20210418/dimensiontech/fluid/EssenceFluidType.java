package com.suntide_20210418.dimensiontech.fluid;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.neoforged.neoforge.fluids.FluidType;

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

    /**
     * 贴图与着色在 1.21 不再经 {@code initializeClient}（该方法已标记 forRemoval），改由客户端在 {@code
     * RegisterClientExtensionsEvent} 里注册 {@code IClientFluidTypeExtensions}。见 {@code
     * client/EssenceFluidClientExtensions}。这里保留 color 供其读取。
     */
    int color() {
        return color;
    }
}
