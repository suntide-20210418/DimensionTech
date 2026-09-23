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

/** 结构矿机界面切换某个“预期物品”的启用状态。客户端 → 服务端。 */
public record StructureMinerExpectedItemTogglePacket(
        int containerId, int slot, ResourceLocation itemId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureMinerExpectedItemTogglePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "miner_expected_item_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMinerExpectedItemTogglePacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureMinerExpectedItemTogglePacket::encode,
                            StructureMinerExpectedItemTogglePacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureMinerExpectedItemTogglePacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureMinerExpectedItemTogglePacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.slot());
        buffer.writeResourceLocation(payload.itemId());
    }

    public static StructureMinerExpectedItemTogglePacket decode(RegistryFriendlyByteBuf buffer) {
        return new StructureMinerExpectedItemTogglePacket(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation());
    }

    public static void handle(
            StructureMinerExpectedItemTogglePacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.containerMenu instanceof StructureMinerMenu menu
                && menu.containerId == payload.containerId()
                && payload.slot() >= 0
                && payload.slot() < menu.getContainerSlotCount()
                && menu.stillValid(player)) {
            menu.getBlockEntity().toggleExpectedItem(payload.itemId());
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
