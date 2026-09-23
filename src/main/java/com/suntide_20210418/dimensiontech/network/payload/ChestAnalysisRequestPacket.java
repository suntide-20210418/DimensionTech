package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 箱子标记器的分析键请求：客户端准星指向的方块与握持标记的手。服务端会重新校验位置，
 * 过时或被伪造的目标无法标记到够不着的箱子。客户端 → 服务端。
 */
public record ChestAnalysisRequestPacket(InteractionHand hand, BlockPos position)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChestAnalysisRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "chest_analysis_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChestAnalysisRequestPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            ChestAnalysisRequestPacket::encode, ChestAnalysisRequestPacket::decode);

    @Override
    public CustomPacketPayload.Type<ChestAnalysisRequestPacket> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buffer, ChestAnalysisRequestPacket payload) {
        buffer.writeEnum(payload.hand());
        buffer.writeBlockPos(payload.position());
    }

    public static ChestAnalysisRequestPacket decode(RegistryFriendlyByteBuf buffer) {
        return new ChestAnalysisRequestPacket(
                buffer.readEnum(InteractionHand.class), buffer.readBlockPos());
    }

    public static void handle(ChestAnalysisRequestPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!stack.is(ModItems.CHEST_MARKER.get())) return;
        ServerLevel serverLevel = player.serverLevel();
        BlockPos position = payload.position();
        if (player.blockPosition().distSqr(position) <= 64.0D) {
            ResourceLocation lootTable = ChestMarkerItem.lootTableAt(serverLevel, position);
            if (lootTable != null) {
                ChestMarkerItem.markChest(serverLevel, stack, position, lootTable);
                ModNetwork.openRefreshedMarker(player, stack, payload.hand());
                return;
            }
        }
        player.displayClientMessage(
                Component.translatable("message.dimension_tech.chest_marker.no_target"), true);
    }
}