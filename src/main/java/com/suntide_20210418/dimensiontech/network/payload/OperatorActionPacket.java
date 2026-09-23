package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 结构数据算子按钮触发的动作请求。{@code dimension} 与 {@code structure} 仅在写入读取槽时使用，
 * 其余动作为 null。客户端 → 服务端。
 */
public record OperatorActionPacket(
        int containerId,
        ModNetwork.OperatorAction action,
        ResourceLocation dimension,
        ResourceLocation structure)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OperatorActionPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "operator_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OperatorActionPacket> STREAM_CODEC =
            StreamCodec.of(OperatorActionPacket::encode, OperatorActionPacket::decode);

    @Override
    public CustomPacketPayload.Type<OperatorActionPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, OperatorActionPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeEnum(payload.action());
        buffer.writeNullable(payload.dimension(), FriendlyByteBuf::writeResourceLocation);
        buffer.writeNullable(payload.structure(), FriendlyByteBuf::writeResourceLocation);
    }

    public static OperatorActionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OperatorActionPacket(
                buffer.readVarInt(),
                buffer.readEnum(ModNetwork.OperatorAction.class),
                buffer.readNullable(FriendlyByteBuf::readResourceLocation),
                buffer.readNullable(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(OperatorActionPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureDataOperatorMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
            switch (payload.action()) {
                case COPY_TO_OPERANDS -> blockEntity.copyData();
                case CLEAR_OPERANDS -> blockEntity.clearOperandData();
                case WRITE_TO_READ_SLOT -> {
                    if (payload.dimension() != null && payload.structure() != null) {
                        blockEntity.writeCatalogueEntry(payload.dimension(), payload.structure());
                    }
                }
            }
        }
    }
}