package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 标记器操作请求：请求选择结构、选定某条候选、清空标记。客户端 → 服务端。 */
public record StructMarkerActionPacket(
        InteractionHand hand,
        ModNetwork.MarkerAction action,
        BlockPos selectionPosition,
        int selectionIndex)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructMarkerActionPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "struct_marker_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructMarkerActionPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructMarkerActionPacket::encode, StructMarkerActionPacket::decode);

    @Override
    public CustomPacketPayload.Type<StructMarkerActionPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, StructMarkerActionPacket payload) {
        buffer.writeEnum(payload.hand());
        buffer.writeEnum(payload.action());
        buffer.writeBlockPos(payload.selectionPosition());
        buffer.writeVarInt(payload.selectionIndex());
    }

    public static StructMarkerActionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StructMarkerActionPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readEnum(ModNetwork.MarkerAction.class),
                buffer.readBlockPos(),
                buffer.readVarInt());
    }

    public static void handle(StructMarkerActionPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(payload.hand());
        boolean structureMarker = stack.is(ModItems.STRUCTURE_MARKER.get());
        boolean chestMarker = stack.is(ModItems.CHEST_MARKER.get());
        if (structureMarker || chestMarker) {
            switch (payload.action()) {
                case CLEAR -> {
                    if (structureMarker) StructMarkerItem.clearMarker(stack);
                    else ChestMarkerItem.clearMarker(stack);
                    ModNetwork.openRefreshedMarker(player, stack, payload.hand());
                }
                case REQUEST_SELECTION -> {
                    if (structureMarker) requestChoices(player, stack, payload.hand());
                }
                case SELECT -> {
                    if (structureMarker
                            && player.blockPosition().equals(payload.selectionPosition())
                            && StructMarkerItem.markAt(
                                    player.serverLevel(),
                                    stack,
                                    payload.selectionPosition(),
                                    payload.selectionIndex())) {
                        ModNetwork.openRefreshedMarker(player, stack, payload.hand());
                    } else if (structureMarker) {
                        player.displayClientMessage(
                                Component.translatable(
                                        "message.dimension_tech.struct_marker.selection_invalid"),
                                true);
                    }
                }
            }
        }
    }

    private static void requestChoices(
            ServerPlayer player, ItemStack marker, InteractionHand hand) {
        List<StructMarkerItem.MarkedStructure> structures =
                StructMarkerItem.findStructuresAt(player.serverLevel(), player.blockPosition());
        if (structures.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.dimension_tech.struct_marker.no_structure_here"),
                    true);
            return;
        }
        if (structures.size() == 1) {
            StructMarkerItem.markAt(player.serverLevel(), marker, player.blockPosition(), 0);
            ModNetwork.openRefreshedMarker(player, marker, hand);
            return;
        }
        PacketDistributor.sendToPlayer(
                player, new StructureChoicesPacket(hand, player.blockPosition(), structures));
    }
}