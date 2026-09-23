package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 结构矿机界面启用/停用某个槽位。客户端 → 服务端。 */
public record StructureMinerSlotTogglePacket(int containerId, int slot)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureMinerSlotTogglePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "miner_slot_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMinerSlotTogglePacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureMinerSlotTogglePacket::encode,
                            StructureMinerSlotTogglePacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureMinerSlotTogglePacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureMinerSlotTogglePacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.slot());
    }

    public static StructureMinerSlotTogglePacket decode(RegistryFriendlyByteBuf buffer) {
        return new StructureMinerSlotTogglePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(StructureMinerSlotTogglePacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureMinerMenu menu
                && menu.containerId == payload.containerId()
                && payload.slot() >= 0
                && payload.slot() < menu.getContainerSlotCount()
                && menu.stillValid(player)) {
            menu.getBlockEntity().toggleSlotEnabled(payload.slot());
        }
    }
}