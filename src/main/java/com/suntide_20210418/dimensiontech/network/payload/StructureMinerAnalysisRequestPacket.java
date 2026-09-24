package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureMinerAnalysisSnapshot;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 结构矿机界面请求某个槽位的分析快照。客户端 → 服务端。 */
public record StructureMinerAnalysisRequestPacket(int containerId, int slot)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureMinerAnalysisRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "miner_analysis_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMinerAnalysisRequestPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureMinerAnalysisRequestPacket::encode,
                            StructureMinerAnalysisRequestPacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureMinerAnalysisRequestPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureMinerAnalysisRequestPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.slot());
    }

    public static StructureMinerAnalysisRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StructureMinerAnalysisRequestPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            StructureMinerAnalysisRequestPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureMinerMenu menu
                && menu.containerId == payload.containerId()
                && payload.slot() >= 0
                && payload.slot() < menu.getContainerSlotCount()
                && menu.stillValid(player)) {
            menu.getBlockEntity().refreshMarkerAnalysis();
            StructureMinerAnalysisSnapshot snapshot =
                    menu.getBlockEntity().getMarkerAnalysisSnapshot(payload.slot());
            PacketDistributor.sendToPlayer(
                    player,
                    new StructureMinerAnalysisPacket(
                            payload.containerId(),
                            payload.slot(),
                            snapshot.dimensionValue(),
                            snapshot.structureValue(),
                            snapshot.equipmentDismantling(),
                            snapshot.itemExpectations(),
                            snapshot.disabledItems()));
        }
    }
}
