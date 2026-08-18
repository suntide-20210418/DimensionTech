package com.suntide_20210418.dimensiontech.network;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(DimensionTechMod.MOD_ID, "main"),
                    () -> VERSION,
                    VERSION::equals,
                    VERSION::equals);
    private static int nextId;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(StructMarkerActionPacket.class, nextId++)
                .encoder(StructMarkerActionPacket::encode)
                .decoder(StructMarkerActionPacket::decode)
                .consumerMainThread(StructMarkerActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(RefreshedMarkerPacket.class, nextId++)
                .encoder(RefreshedMarkerPacket::encode)
                .decoder(RefreshedMarkerPacket::decode)
                .consumerMainThread(RefreshedMarkerPacket::handle)
                .add();
    }

    public static void mark(InteractionHand hand) {
        CHANNEL.sendToServer(new StructMarkerActionPacket(hand, false));
    }

    public static void clear(InteractionHand hand) {
        CHANNEL.sendToServer(new StructMarkerActionPacket(hand, true));
    }

    public static void openRefreshedMarker(ServerPlayer player, ItemStack marker) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RefreshedMarkerPacket(marker.copy()));
    }

    private record StructMarkerActionPacket(InteractionHand hand, boolean clear) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            buffer.writeBoolean(clear);
        }

        private static StructMarkerActionPacket decode(FriendlyByteBuf buffer) {
            return new StructMarkerActionPacket(
                    buffer.readEnum(InteractionHand.class), buffer.readBoolean());
        }

        private static void handle(
                StructMarkerActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(packet.hand());
                if (stack.is(ModItems.STRUCT_MARKER.get())) {
                    if (packet.clear()) {
                        StructMarkerItem.clearMarker(stack);
                    } else {
                        StructMarkerItem.markAt(player.serverLevel(), stack, player.blockPosition());
                    }
                }
            }
            context.setPacketHandled(true);
        }
    }

    private record RefreshedMarkerPacket(ItemStack marker) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeItem(marker);
        }

        private static RefreshedMarkerPacket decode(FriendlyByteBuf buffer) {
            return new RefreshedMarkerPacket(buffer.readItem());
        }

        private static void handle(
                RefreshedMarkerPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () ->
                            com.suntide_20210418.dimensiontech.client.StructMarkerClient.open(
                                    packet.marker()));
            context.setPacketHandled(true);
        }
    }
}
