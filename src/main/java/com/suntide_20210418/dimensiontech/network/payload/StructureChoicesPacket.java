package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 同一位置命中多条结构时的候选列表。服务端 → 客户端。 */
public record StructureChoicesPacket(
        InteractionHand hand, BlockPos position, List<StructMarkerItem.MarkedStructure> structures)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureChoicesPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "structure_choices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureChoicesPacket> STREAM_CODEC =
            StreamCodec.of(StructureChoicesPacket::encode, StructureChoicesPacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureChoicesPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, StructureChoicesPacket payload) {
        buffer.writeEnum(payload.hand());
        buffer.writeBlockPos(payload.position());
        buffer.writeVarInt(payload.structures().size());
        for (StructMarkerItem.MarkedStructure structure : payload.structures()) {
            buffer.writeResourceLocation(structure.id());
            buffer.writeInt(structure.bounds().minX());
            buffer.writeInt(structure.bounds().minY());
            buffer.writeInt(structure.bounds().minZ());
            buffer.writeInt(structure.bounds().maxX());
            buffer.writeInt(structure.bounds().maxY());
            buffer.writeInt(structure.bounds().maxZ());
        }
    }

    public static StructureChoicesPacket decode(RegistryFriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        BlockPos position = buffer.readBlockPos();
        int count = Math.min(128, Math.max(0, buffer.readVarInt()));
        List<StructMarkerItem.MarkedStructure> structures = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            structures.add(
                    new StructMarkerItem.MarkedStructure(
                            id,
                            new BoundingBox(
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt())));
        }
        return new StructureChoicesPacket(hand, position, List.copyOf(structures));
    }

    public static void handle(StructureChoicesPacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.showStructureChoices(
                    payload.hand(), payload.position(), payload.structures());
        }
    }
}
