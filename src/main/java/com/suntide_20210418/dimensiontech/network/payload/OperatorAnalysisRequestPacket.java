package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 请求某条目录项的快照，可要求丢弃缓存重算。客户端 → 服务端。 */
public record OperatorAnalysisRequestPacket(
        int containerId, ResourceLocation dimension, ResourceLocation structure, boolean force)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OperatorAnalysisRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "operator_analysis_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorAnalysisRequestPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            OperatorAnalysisRequestPacket::encode,
                            OperatorAnalysisRequestPacket::decode);

    @Override
    public CustomPacketPayload.Type<OperatorAnalysisRequestPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, OperatorAnalysisRequestPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeResourceLocation(payload.dimension());
        buffer.writeResourceLocation(payload.structure());
        buffer.writeBoolean(payload.force());
    }

    public static OperatorAnalysisRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OperatorAnalysisRequestPacket(
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                buffer.readResourceLocation(),
                buffer.readBoolean());
    }

    public static void handle(OperatorAnalysisRequestPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureDataOperatorMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
            if (payload.force()) {
                blockEntity.refreshCatalogueAnalysis(payload.dimension(), payload.structure());
            }
            ItemStack marker =
                    blockEntity.analyseCatalogueEntry(payload.dimension(), payload.structure());
            PacketDistributor.sendToPlayer(
                    player,
                    new OperatorAnalysisPacket(
                            payload.containerId(),
                            payload.dimension(),
                            payload.structure(),
                            marker));
        }
    }
}