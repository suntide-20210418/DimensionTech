package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 结构数据算子的结构目录内容。服务端 → 客户端。 */
public record OperatorCataloguePacket(
        int containerId, boolean allStructures, List<CatalogueRow> rows)
        implements CustomPacketPayload {

    public static final int MAX_ROWS = 8192;

    public static final CustomPacketPayload.Type<OperatorCataloguePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "operator_catalogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorCataloguePacket> STREAM_CODEC =
            StreamCodec.of(OperatorCataloguePacket::encode, OperatorCataloguePacket::decode);

    /** 目录里一行的线上表示。刻意不复用方块实体自己的记录类型。 */
    public record CatalogueRow(ResourceLocation dimension, ResourceLocation structure) {}

    @Override
    public CustomPacketPayload.Type<OperatorCataloguePacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, OperatorCataloguePacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeBoolean(payload.allStructures());
        int count = Math.min(MAX_ROWS, payload.rows().size());
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            buffer.writeResourceLocation(payload.rows().get(index).dimension());
            buffer.writeResourceLocation(payload.rows().get(index).structure());
        }
    }

    public static OperatorCataloguePacket decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        boolean allStructures = buffer.readBoolean();
        int count = Math.max(0, Math.min(MAX_ROWS, buffer.readVarInt()));
        List<CatalogueRow> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rows.add(
                    new CatalogueRow(buffer.readResourceLocation(), buffer.readResourceLocation()));
        }
        return new OperatorCataloguePacket(containerId, allStructures, List.copyOf(rows));
    }

    public static void handle(OperatorCataloguePacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.receiveOperatorCatalogue(payload.rows());
        }
    }
}
