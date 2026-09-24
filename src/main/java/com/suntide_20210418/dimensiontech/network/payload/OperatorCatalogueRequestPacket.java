package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 请求结构数据算子重建结构目录。客户端 → 服务端。 */
public record OperatorCatalogueRequestPacket(int containerId, boolean allStructures)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OperatorCatalogueRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "operator_catalogue_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorCatalogueRequestPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            OperatorCatalogueRequestPacket::encode,
                            OperatorCatalogueRequestPacket::decode);

    @Override
    public CustomPacketPayload.Type<OperatorCatalogueRequestPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, OperatorCatalogueRequestPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeBoolean(payload.allStructures());
    }

    public static OperatorCatalogueRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OperatorCatalogueRequestPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(OperatorCatalogueRequestPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureDataOperatorMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
            blockEntity.refreshCatalogue(player, payload.allStructures());
            List<OperatorCataloguePacket.CatalogueRow> rows = new ArrayList<>();
            for (StructureDataOperatorBlockEntity.StructureCatalogueEntry entry :
                    blockEntity.catalogue()) {
                rows.add(
                        new OperatorCataloguePacket.CatalogueRow(
                                entry.dimension(), entry.structure()));
            }
            PacketDistributor.sendToPlayer(
                    player,
                    new OperatorCataloguePacket(
                            payload.containerId(), payload.allStructures(), rows));
        }
    }
}
