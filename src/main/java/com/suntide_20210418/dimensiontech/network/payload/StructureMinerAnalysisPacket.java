package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 结构矿机某个槽位的分析结果。服务端 → 客户端。 */
public record StructureMinerAnalysisPacket(
        int containerId,
        int slot,
        double dimensionValue,
        double structureValue,
        boolean equipmentDismantling,
        Map<ResourceLocation, Double> itemExpectations,
        Set<ResourceLocation> disabledItems)
        implements CustomPacketPayload {

    public static final int MAX_ITEMS = 4096;

    public static final CustomPacketPayload.Type<StructureMinerAnalysisPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "miner_analysis"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMinerAnalysisPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureMinerAnalysisPacket::encode,
                            StructureMinerAnalysisPacket::decode);

    public StructureMinerAnalysisPacket {
        itemExpectations = Map.copyOf(itemExpectations);
    }

    @Override
    public CustomPacketPayload.Type<StructureMinerAnalysisPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureMinerAnalysisPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.slot());
        buffer.writeDouble(payload.dimensionValue());
        buffer.writeDouble(payload.structureValue());
        buffer.writeBoolean(payload.equipmentDismantling());
        buffer.writeVarInt(Math.min(MAX_ITEMS, payload.itemExpectations().size()));
        int written = 0;
        for (Map.Entry<ResourceLocation, Double> entry : payload.itemExpectations().entrySet()) {
            if (written++ >= MAX_ITEMS) {
                break;
            }
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeDouble(entry.getValue());
        }
        buffer.writeVarInt(payload.disabledItems().size());
        for (ResourceLocation item : payload.disabledItems()) buffer.writeResourceLocation(item);
    }

    public static StructureMinerAnalysisPacket decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int slot = buffer.readVarInt();
        double dimensionValue = buffer.readDouble();
        double structureValue = buffer.readDouble();
        boolean equipmentDismantling = buffer.readBoolean();
        int itemCount = Math.max(0, Math.min(MAX_ITEMS, buffer.readVarInt()));
        Map<ResourceLocation, Double> itemExpectations = new LinkedHashMap<>();
        for (int index = 0; index < itemCount; index++) {
            itemExpectations.put(buffer.readResourceLocation(), buffer.readDouble());
        }
        int disabledCount = Math.max(0, Math.min(MAX_ITEMS, buffer.readVarInt()));
        Set<ResourceLocation> disabledItems = new LinkedHashSet<>();
        for (int index = 0; index < disabledCount; index++) {
            disabledItems.add(buffer.readResourceLocation());
        }
        return new StructureMinerAnalysisPacket(
                containerId,
                slot,
                dimensionValue,
                structureValue,
                equipmentDismantling,
                itemExpectations,
                disabledItems);
    }

    public static void handle(StructureMinerAnalysisPacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.receiveMinerAnalysis(
                    payload.containerId(),
                    payload.slot(),
                    payload.dimensionValue(),
                    payload.structureValue(),
                    payload.equipmentDismantling(),
                    payload.itemExpectations(),
                    payload.disabledItems());
        }
    }
}