package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 某条目录项分析出的标记物品快照。服务端 → 客户端。 */
public record OperatorAnalysisPacket(
        int containerId,
        ResourceLocation dimension,
        ResourceLocation structure,
        ItemStack marker)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OperatorAnalysisPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "operator_analysis"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorAnalysisPacket> STREAM_CODEC =
            StreamCodec.of(OperatorAnalysisPacket::encode, OperatorAnalysisPacket::decode);

    @Override
    public CustomPacketPayload.Type<OperatorAnalysisPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, OperatorAnalysisPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeResourceLocation(payload.dimension());
        buffer.writeResourceLocation(payload.structure());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.marker());
    }

    public static OperatorAnalysisPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OperatorAnalysisPacket(
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public static void handle(OperatorAnalysisPacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.receiveOperatorAnalysis(
                    payload.dimension(), payload.structure(), payload.marker());
        }
    }
}