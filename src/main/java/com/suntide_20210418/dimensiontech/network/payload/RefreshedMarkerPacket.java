package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 把服务端改动后的标记物品快照推给持有它的一方，用于刷新或打开标记界面。服务端 → 客户端。 */
public record RefreshedMarkerPacket(ItemStack marker, InteractionHand hand)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RefreshedMarkerPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "refreshed_marker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshedMarkerPacket> STREAM_CODEC =
            StreamCodec.of(RefreshedMarkerPacket::encode, RefreshedMarkerPacket::decode);

    @Override
    public CustomPacketPayload.Type<RefreshedMarkerPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, RefreshedMarkerPacket payload) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.marker());
        buffer.writeEnum(payload.hand());
    }

    public static RefreshedMarkerPacket decode(RegistryFriendlyByteBuf buffer) {
        return new RefreshedMarkerPacket(
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readEnum(InteractionHand.class));
    }

    public static void handle(RefreshedMarkerPacket payload, IPayloadContext context) {
        // 守卫必须在处理体内部：专用服务端不会执行到这里，因此不会解析客户端类。
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.openRefreshedMarker(payload.marker(), payload.hand());
        }
    }
}