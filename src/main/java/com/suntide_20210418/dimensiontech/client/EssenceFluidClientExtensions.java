package com.suntide_20210418.dimensiontech.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * 精华流体的客户端外观：借用原版水的流动贴图，仅以 ARGB 着色区分各 Tier。
 *
 * <p>1.20.1 把这段实现写在 {@code FluidType#initializeClient} 里；该回调在 1.21 已标记 forRemoval， 改为由客户端在 {@code
 * RegisterClientExtensionsEvent} 中注册本类实例。放在 client 包是因为 {@link IClientFluidTypeExtensions}
 * 本身是客户端专用类型，不能让它在专用服务端被解析。
 */
final class EssenceFluidClientExtensions implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");

    private final int tintColor;

    EssenceFluidClientExtensions(int tintColor) {
        this.tintColor = tintColor;
    }

    @Override
    public ResourceLocation getStillTexture() {
        return STILL_TEXTURE;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING_TEXTURE;
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }
}
