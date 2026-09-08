package com.suntide_20210418.dimensiontech.network;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.MythicMinerAnalysisSnapshot;
import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
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
    private static final String VERSION = "6";
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
        CHANNEL.messageBuilder(StructureChoicesPacket.class, nextId++)
                .encoder(StructureChoicesPacket::encode)
                .decoder(StructureChoicesPacket::decode)
                .consumerMainThread(StructureChoicesPacket::handle)
                .add();
        CHANNEL.messageBuilder(MythicMinerAnalysisRequestPacket.class, nextId++)
                .encoder(MythicMinerAnalysisRequestPacket::encode)
                .decoder(MythicMinerAnalysisRequestPacket::decode)
                .consumerMainThread(MythicMinerAnalysisRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(MythicMinerAnalysisPacket.class, nextId++)
                .encoder(MythicMinerAnalysisPacket::encode)
                .decoder(MythicMinerAnalysisPacket::decode)
                .consumerMainThread(MythicMinerAnalysisPacket::handle)
                .add();
        CHANNEL.messageBuilder(MythicMinerExpectedItemTogglePacket.class, nextId++)
                .encoder(MythicMinerExpectedItemTogglePacket::encode)
                .decoder(MythicMinerExpectedItemTogglePacket::decode)
                .consumerMainThread(MythicMinerExpectedItemTogglePacket::handle)
                .add();
        CHANNEL.messageBuilder(MythicMinerSlotTogglePacket.class, nextId++)
                .encoder(MythicMinerSlotTogglePacket::encode)
                .decoder(MythicMinerSlotTogglePacket::decode)
                .consumerMainThread(MythicMinerSlotTogglePacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureOperatorActionPacket.class, nextId++)
                .encoder(StructureOperatorActionPacket::encode)
                .decoder(StructureOperatorActionPacket::decode)
                .consumerMainThread(StructureOperatorActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureOperatorCataloguePacket.class, nextId++)
                .encoder(StructureOperatorCataloguePacket::encode)
                .decoder(StructureOperatorCataloguePacket::decode)
                .consumerMainThread(StructureOperatorCataloguePacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureOperatorDetailPacket.class, nextId++)
                .encoder(StructureOperatorDetailPacket::encode)
                .decoder(StructureOperatorDetailPacket::decode)
                .consumerMainThread(StructureOperatorDetailPacket::handle)
                .add();
    }

    public static void requestStructureSelection(InteractionHand hand) {
        CHANNEL.sendToServer(
                new StructMarkerActionPacket(
                        hand, MarkerAction.REQUEST_SELECTION, BlockPos.ZERO, -1));
    }

    public static void selectStructure(
            InteractionHand hand, BlockPos selectionPosition, int selectionIndex) {
        CHANNEL.sendToServer(
                new StructMarkerActionPacket(
                        hand, MarkerAction.SELECT, selectionPosition, selectionIndex));
    }

    public static void clear(InteractionHand hand) {
        CHANNEL.sendToServer(
                new StructMarkerActionPacket(hand, MarkerAction.CLEAR, BlockPos.ZERO, -1));
    }

    public static void requestMythicMinerAnalysis(int containerId, int slot) {
        CHANNEL.sendToServer(new MythicMinerAnalysisRequestPacket(containerId, slot));
    }

    public static void toggleMythicMinerExpectedItem(
            int containerId, int slot, ResourceLocation itemId) {
        CHANNEL.sendToServer(new MythicMinerExpectedItemTogglePacket(containerId, slot, itemId));
    }

    public static void toggleMythicMinerSlot(int containerId, int slot) {
        CHANNEL.sendToServer(new MythicMinerSlotTogglePacket(containerId, slot));
    }

    public static void structureOperatorCopy(int containerId) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId, StructureOperatorAction.COPY, null, null));
    }

    public static void structureOperatorClearOperands(int containerId) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId, StructureOperatorAction.CLEAR_OPERANDS, null, null));
    }

    public static void structureOperatorLoadCatalogue(int containerId, boolean interpreter) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId,
                        interpreter
                                ? StructureOperatorAction.LOAD_INTERPRETER
                                : StructureOperatorAction.LOAD_INTEGRATOR,
                        null,
                        null));
    }

    public static void structureOperatorRequestDetail(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId, StructureOperatorAction.REQUEST_DETAIL, dimension, structure));
    }

    public static void structureOperatorRefreshDetail(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId, StructureOperatorAction.REFRESH_DETAIL, dimension, structure));
    }

    public static void structureOperatorWrite(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new StructureOperatorActionPacket(
                        containerId, StructureOperatorAction.WRITE, dimension, structure));
    }

    private enum StructureOperatorAction {
        COPY,
        CLEAR_OPERANDS,
        LOAD_INTEGRATOR,
        LOAD_INTERPRETER,
        REQUEST_DETAIL,
        REFRESH_DETAIL,
        WRITE
    }

    private record StructureOperatorActionPacket(
            int containerId,
            StructureOperatorAction action,
            ResourceLocation dimension,
            ResourceLocation structure) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(containerId);
            b.writeEnum(action);
            b.writeBoolean(dimension != null);
            if (dimension != null) b.writeResourceLocation(dimension);
            b.writeBoolean(structure != null);
            if (structure != null) b.writeResourceLocation(structure);
        }

        static StructureOperatorActionPacket decode(FriendlyByteBuf b) {
            return new StructureOperatorActionPacket(
                    b.readVarInt(),
                    b.readEnum(StructureOperatorAction.class),
                    b.readBoolean() ? b.readResourceLocation() : null,
                    b.readBoolean() ? b.readResourceLocation() : null);
        }

        static void handle(StructureOperatorActionPacket p, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            ServerPlayer player = c.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureDataOperatorMenu menu
                    && menu.containerId == p.containerId
                    && menu.stillValid(player)) {
                StructureDataOperatorBlockEntity be = menu.blockEntity();
                switch (p.action) {
                    case COPY -> be.copyData();
                    case CLEAR_OPERANDS -> be.clearOperandData();
                    case LOAD_INTEGRATOR -> {
                        if (be.hasIntegrator()) {
                            be.refreshCatalogue(player, false);
                            sendCatalogue(player, be.catalogue());
                        }
                    }
                    case LOAD_INTERPRETER -> {
                        if (be.hasIntegrator() && be.hasInterpreter()) {
                            be.refreshCatalogue(player, true);
                            sendCatalogue(player, be.catalogue());
                        }
                    }
                    case REQUEST_DETAIL -> {
                        if (p.dimension != null && p.structure != null)
                            sendDetail(player, be, p.dimension, p.structure);
                    }
                    case REFRESH_DETAIL -> {
                        if (p.dimension != null && p.structure != null) {
                            be.refreshCatalogueAnalysis(p.dimension, p.structure);
                            sendDetail(player, be, p.dimension, p.structure);
                        }
                    }
                    case WRITE -> {
                        if (p.dimension != null && p.structure != null)
                            be.writeCatalogueEntry(p.dimension, p.structure);
                    }
                }
                player.containerMenu.broadcastChanges();
            }
            c.setPacketHandled(true);
        }
    }

    private static void sendCatalogue(
            ServerPlayer player,
            List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> entries) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StructureOperatorCataloguePacket(entries));
    }

    private static void sendDetail(
            ServerPlayer player,
            StructureDataOperatorBlockEntity be,
            ResourceLocation dimension,
            ResourceLocation structure) {
        be.catalogue().stream()
                .filter(
                        entry ->
                                entry.dimension().equals(dimension)
                                        && entry.structure().equals(structure))
                .findFirst()
                .ifPresent(
                        entry -> {
                            ItemStack analysisMarker =
                                    be.analyseCatalogueEntry(entry.dimension(), entry.structure());
                            CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> player),
                                    new StructureOperatorDetailPacket(
                                            entry.dimension(),
                                            entry.structure(),
                                            analysisMarker,
                                            be.catalogueAnalysisState(
                                                            entry.dimension(), entry.structure())
                                                    .completedSamples(),
                                            be.catalogueAnalysisState(
                                                            entry.dimension(), entry.structure())
                                                    .totalSamples()));
                        });
    }

    private record StructureOperatorCataloguePacket(
            List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> entries) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(Math.min(1024, entries.size()));
            entries.stream()
                    .limit(1024)
                    .forEach(
                            entry -> {
                                b.writeResourceLocation(entry.dimension());
                                b.writeResourceLocation(entry.structure());
                            });
        }

        static StructureOperatorCataloguePacket decode(FriendlyByteBuf b) {
            int n = Math.min(1024, Math.max(0, b.readVarInt()));
            List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> r = new ArrayList<>();
            for (int i = 0; i < n; i++)
                r.add(
                        new StructureDataOperatorBlockEntity.StructureCatalogueEntry(
                                b.readResourceLocation(),
                                b.readResourceLocation(),
                                ItemStack.EMPTY));
            return new StructureOperatorCataloguePacket(List.copyOf(r));
        }

        static void handle(StructureOperatorCataloguePacket p, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () -> {
                                if (net.minecraft.client.Minecraft.getInstance().screen
                                        instanceof
                                        com.suntide_20210418.dimensiontech.client.gui.screen
                                                        .StructureDataOperatorScreen
                                                screen) screen.receiveCatalogue(p.entries());
                            });
            c.setPacketHandled(true);
        }
    }

    private record StructureOperatorDetailPacket(
            ResourceLocation dimension,
            ResourceLocation structure,
            ItemStack marker,
            int completedSamples,
            int totalSamples) {
        void encode(FriendlyByteBuf b) {
            b.writeResourceLocation(dimension);
            b.writeResourceLocation(structure);
            b.writeItem(marker);
            b.writeVarInt(completedSamples);
            b.writeVarInt(totalSamples);
        }

        static StructureOperatorDetailPacket decode(FriendlyByteBuf b) {
            return new StructureOperatorDetailPacket(
                    b.readResourceLocation(),
                    b.readResourceLocation(),
                    b.readItem(),
                    b.readVarInt(),
                    b.readVarInt());
        }

        static void handle(StructureOperatorDetailPacket p, Supplier<NetworkEvent.Context> s) {
            NetworkEvent.Context c = s.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () -> {
                                if (net.minecraft.client.Minecraft.getInstance().screen
                                        instanceof
                                        com.suntide_20210418.dimensiontech.client.gui.screen
                                                        .StructureDataOperatorScreen
                                                screen)
                                    screen.receiveDetail(
                                            p.dimension(),
                                            p.structure(),
                                            p.marker(),
                                            p.completedSamples(),
                                            p.totalSamples());
                            });
            c.setPacketHandled(true);
        }
    }

    public static void openRefreshedMarker(
            ServerPlayer player, ItemStack marker, InteractionHand hand) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RefreshedMarkerPacket(marker.copy(), hand));
    }

    private enum MarkerAction {
        REQUEST_SELECTION,
        SELECT,
        CLEAR
    }

    private record StructMarkerActionPacket(
            InteractionHand hand,
            MarkerAction action,
            BlockPos selectionPosition,
            int selectionIndex) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            buffer.writeEnum(action);
            buffer.writeBlockPos(selectionPosition);
            buffer.writeVarInt(selectionIndex);
        }

        private static StructMarkerActionPacket decode(FriendlyByteBuf buffer) {
            return new StructMarkerActionPacket(
                    buffer.readEnum(InteractionHand.class),
                    buffer.readEnum(MarkerAction.class),
                    buffer.readBlockPos(),
                    buffer.readVarInt());
        }

        private static void handle(
                StructMarkerActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(packet.hand());
                if (stack.is(ModItems.STRUCTURE_MARKER.get())) {
                    switch (packet.action()) {
                        case CLEAR -> {
                            StructMarkerItem.clearMarker(stack);
                            openRefreshedMarker(player, stack, packet.hand());
                        }
                        case REQUEST_SELECTION -> requestChoices(player, stack, packet.hand());
                        case SELECT -> {
                            if (player.blockPosition().equals(packet.selectionPosition())
                                    && StructMarkerItem.markAt(
                                            player.serverLevel(),
                                            stack,
                                            packet.selectionPosition(),
                                            packet.selectionIndex())) {
                                openRefreshedMarker(player, stack, packet.hand());
                            } else {
                                player.displayClientMessage(
                                        net.minecraft.network.chat.Component.translatable(
                                                "message.dimension_tech.struct_marker.selection_invalid"),
                                        true);
                            }
                        }
                    }
                }
            }
            context.setPacketHandled(true);
        }
    }

    private static void requestChoices(
            ServerPlayer player, ItemStack marker, InteractionHand hand) {
        List<StructMarkerItem.MarkedStructure> structures =
                StructMarkerItem.findStructuresAt(player.serverLevel(), player.blockPosition());
        if (structures.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.dimension_tech.struct_marker.no_structure_here"),
                    true);
            return;
        }
        if (structures.size() == 1) {
            StructMarkerItem.markAt(player.serverLevel(), marker, player.blockPosition(), 0);
            openRefreshedMarker(player, marker, hand);
            return;
        }
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StructureChoicesPacket(hand, player.blockPosition(), structures));
    }

    private record RefreshedMarkerPacket(ItemStack marker, InteractionHand hand) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeItem(marker);
            buffer.writeEnum(hand);
        }

        private static RefreshedMarkerPacket decode(FriendlyByteBuf buffer) {
            return new RefreshedMarkerPacket(
                    buffer.readItem(), buffer.readEnum(InteractionHand.class));
        }

        private static void handle(
                RefreshedMarkerPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () ->
                                    com.suntide_20210418.dimensiontech.client.StructMarkerClient
                                            .open(packet.marker(), packet.hand()));
            context.setPacketHandled(true);
        }
    }

    private record StructureChoicesPacket(
            InteractionHand hand,
            BlockPos position,
            List<StructMarkerItem.MarkedStructure> structures) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            buffer.writeBlockPos(position);
            buffer.writeVarInt(structures.size());
            for (StructMarkerItem.MarkedStructure structure : structures) {
                buffer.writeResourceLocation(structure.id());
                buffer.writeInt(structure.bounds().minX());
                buffer.writeInt(structure.bounds().minY());
                buffer.writeInt(structure.bounds().minZ());
                buffer.writeInt(structure.bounds().maxX());
                buffer.writeInt(structure.bounds().maxY());
                buffer.writeInt(structure.bounds().maxZ());
            }
        }

        private static StructureChoicesPacket decode(FriendlyByteBuf buffer) {
            InteractionHand hand = buffer.readEnum(InteractionHand.class);
            BlockPos position = buffer.readBlockPos();
            int count = Math.min(128, Math.max(0, buffer.readVarInt()));
            List<StructMarkerItem.MarkedStructure> structures = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                ResourceLocation id = buffer.readResourceLocation();
                structures.add(
                        new StructMarkerItem.MarkedStructure(
                                id,
                                new net.minecraft.world.level.levelgen.structure.BoundingBox(
                                        buffer.readInt(),
                                        buffer.readInt(),
                                        buffer.readInt(),
                                        buffer.readInt(),
                                        buffer.readInt(),
                                        buffer.readInt())));
            }
            return new StructureChoicesPacket(hand, position, List.copyOf(structures));
        }

        private static void handle(
                StructureChoicesPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () ->
                                    com.suntide_20210418.dimensiontech.client.StructMarkerClient
                                            .showStructureChoices(
                                                    packet.hand(),
                                                    packet.position(),
                                                    packet.structures()));
            context.setPacketHandled(true);
        }
    }

    private record MythicMinerAnalysisRequestPacket(int containerId, int slot) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
        }

        private static MythicMinerAnalysisRequestPacket decode(FriendlyByteBuf buffer) {
            return new MythicMinerAnalysisRequestPacket(buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(
                MythicMinerAnalysisRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof MythicMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().refreshMarkerAnalysis();
                MythicMinerAnalysisSnapshot snapshot =
                        menu.getBlockEntity().getMarkerAnalysisSnapshot(packet.slot());
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new MythicMinerAnalysisPacket(
                                packet.containerId(),
                                packet.slot(),
                                snapshot.dimensionValue(),
                                snapshot.structureValue(),
                                snapshot.equipmentDismantling(),
                                snapshot.itemExpectations(),
                                snapshot.disabledItems()));
            }
            context.setPacketHandled(true);
        }
    }

    private record MythicMinerAnalysisPacket(
            int containerId,
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        private static final int MAX_ITEMS = 4096;

        private MythicMinerAnalysisPacket {
            itemExpectations = Map.copyOf(itemExpectations);
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
            buffer.writeDouble(dimensionValue);
            buffer.writeDouble(structureValue);
            buffer.writeBoolean(equipmentDismantling);
            buffer.writeVarInt(Math.min(MAX_ITEMS, itemExpectations.size()));
            int written = 0;
            for (Map.Entry<ResourceLocation, Double> entry : itemExpectations.entrySet()) {
                if (written++ >= MAX_ITEMS) {
                    break;
                }
                buffer.writeResourceLocation(entry.getKey());
                buffer.writeDouble(entry.getValue());
            }
            buffer.writeVarInt(disabledItems.size());
            for (ResourceLocation item : disabledItems) buffer.writeResourceLocation(item);
        }

        private static MythicMinerAnalysisPacket decode(FriendlyByteBuf buffer) {
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
            Set<ResourceLocation> disabledItems = new java.util.LinkedHashSet<>();
            for (int index = 0; index < disabledCount; index++) {
                disabledItems.add(buffer.readResourceLocation());
            }
            return new MythicMinerAnalysisPacket(
                    containerId,
                    slot,
                    dimensionValue,
                    structureValue,
                    equipmentDismantling,
                    itemExpectations,
                    disabledItems);
        }

        private static void handle(
                MythicMinerAnalysisPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () ->
                                    com.suntide_20210418.dimensiontech.client.gui.screen
                                            .MythicMinerScreen.receiveAnalysis(
                                            packet.containerId(),
                                            packet.slot(),
                                            packet.dimensionValue(),
                                            packet.structureValue(),
                                            packet.equipmentDismantling(),
                                            packet.itemExpectations(),
                                            packet.disabledItems()));
            context.setPacketHandled(true);
        }
    }

    private record MythicMinerExpectedItemTogglePacket(
            int containerId, int slot, ResourceLocation itemId) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
            buffer.writeResourceLocation(itemId);
        }

        private static MythicMinerExpectedItemTogglePacket decode(FriendlyByteBuf buffer) {
            return new MythicMinerExpectedItemTogglePacket(
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation());
        }

        private static void handle(
                MythicMinerExpectedItemTogglePacket packet,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof MythicMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().toggleExpectedItem(packet.itemId());
                menu.getBlockEntity().refreshMarkerAnalysis();
                MythicMinerAnalysisSnapshot snapshot =
                        menu.getBlockEntity().getMarkerAnalysisSnapshot(packet.slot());
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new MythicMinerAnalysisPacket(
                                packet.containerId(),
                                packet.slot(),
                                snapshot.dimensionValue(),
                                snapshot.structureValue(),
                                snapshot.equipmentDismantling(),
                                snapshot.itemExpectations(),
                                snapshot.disabledItems()));
            }
            context.setPacketHandled(true);
        }
    }

    private record MythicMinerSlotTogglePacket(int containerId, int slot) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
        }

        private static MythicMinerSlotTogglePacket decode(FriendlyByteBuf buffer) {
            return new MythicMinerSlotTogglePacket(buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(
                MythicMinerSlotTogglePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof MythicMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().toggleSlotEnabled(packet.slot());
            }
            context.setPacketHandled(true);
        }
    }
}
