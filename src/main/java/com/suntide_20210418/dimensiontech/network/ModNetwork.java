package com.suntide_20210418.dimensiontech.network;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.StructureMinerAnalysisSnapshot;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.item.ChestMarkerItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "10";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(DimensionTechMod.MOD_ID, "main"),
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
        CHANNEL.messageBuilder(StructureMinerAnalysisRequestPacket.class, nextId++)
                .encoder(StructureMinerAnalysisRequestPacket::encode)
                .decoder(StructureMinerAnalysisRequestPacket::decode)
                .consumerMainThread(StructureMinerAnalysisRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureMinerAnalysisPacket.class, nextId++)
                .encoder(StructureMinerAnalysisPacket::encode)
                .decoder(StructureMinerAnalysisPacket::decode)
                .consumerMainThread(StructureMinerAnalysisPacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureMinerExpectedItemTogglePacket.class, nextId++)
                .encoder(StructureMinerExpectedItemTogglePacket::encode)
                .decoder(StructureMinerExpectedItemTogglePacket::decode)
                .consumerMainThread(StructureMinerExpectedItemTogglePacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureMinerSlotTogglePacket.class, nextId++)
                .encoder(StructureMinerSlotTogglePacket::encode)
                .decoder(StructureMinerSlotTogglePacket::decode)
                .consumerMainThread(StructureMinerSlotTogglePacket::handle)
                .add();
        CHANNEL.messageBuilder(StructureReactorTooltipPacket.class, nextId++)
                .encoder(StructureReactorTooltipPacket::encode)
                .decoder(StructureReactorTooltipPacket::decode)
                .consumerMainThread(StructureReactorTooltipPacket::handle)
                .add();
        CHANNEL.messageBuilder(OperatorCatalogueRequestPacket.class, nextId++)
                .encoder(OperatorCatalogueRequestPacket::encode)
                .decoder(OperatorCatalogueRequestPacket::decode)
                .consumerMainThread(OperatorCatalogueRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(OperatorCataloguePacket.class, nextId++)
                .encoder(OperatorCataloguePacket::encode)
                .decoder(OperatorCataloguePacket::decode)
                .consumerMainThread(OperatorCataloguePacket::handle)
                .add();
        CHANNEL.messageBuilder(OperatorAnalysisRequestPacket.class, nextId++)
                .encoder(OperatorAnalysisRequestPacket::encode)
                .decoder(OperatorAnalysisRequestPacket::decode)
                .consumerMainThread(OperatorAnalysisRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(OperatorAnalysisPacket.class, nextId++)
                .encoder(OperatorAnalysisPacket::encode)
                .decoder(OperatorAnalysisPacket::decode)
                .consumerMainThread(OperatorAnalysisPacket::handle)
                .add();
        CHANNEL.messageBuilder(OperatorActionPacket.class, nextId++)
                .encoder(OperatorActionPacket::encode)
                .decoder(OperatorActionPacket::decode)
                .consumerMainThread(OperatorActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChestAnalysisRequestPacket.class, nextId++)
                .encoder(ChestAnalysisRequestPacket::encode)
                .decoder(ChestAnalysisRequestPacket::decode)
                .consumerMainThread(ChestAnalysisRequestPacket::handle)
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

    /** Asks the server to analyse the given position with the chest marker in {@code hand}. */
    public static void requestChestAnalysis(InteractionHand hand, BlockPos position) {
        CHANNEL.sendToServer(new ChestAnalysisRequestPacket(hand, position));
    }

    public static void requestStructureMinerAnalysis(int containerId, int slot) {
        CHANNEL.sendToServer(new StructureMinerAnalysisRequestPacket(containerId, slot));
    }

    public static void toggleStructureMinerExpectedItem(
            int containerId, int slot, ResourceLocation itemId) {
        CHANNEL.sendToServer(new StructureMinerExpectedItemTogglePacket(containerId, slot, itemId));
    }

    public static void toggleStructureMinerSlot(int containerId, int slot) {
        CHANNEL.sendToServer(new StructureMinerSlotTogglePacket(containerId, slot));
    }

    public static void sendReactorTooltipSnapshot(
            ServerPlayer player, int containerId, ReactorTooltipSnapshot snapshot) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StructureReactorTooltipPacket(containerId, snapshot));
    }

    public static void openRefreshedMarker(
            ServerPlayer player, ItemStack marker, InteractionHand hand) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RefreshedMarkerPacket(marker.copy(), hand));
    }

    // ------------------------------------------------- structure data operator

    /** Copies the read slot's marker data onto every filled write slot. */
    public static void structureOperatorCopy(int containerId) {
        CHANNEL.sendToServer(
                new OperatorActionPacket(containerId, OperatorAction.COPY_TO_OPERANDS, null, null));
    }

    /** Strips marker data from every write slot. */
    public static void structureOperatorClearOperands(int containerId) {
        CHANNEL.sendToServer(
                new OperatorActionPacket(containerId, OperatorAction.CLEAR_OPERANDS, null, null));
    }

    /**
     * Rebuilds the operator's structure catalogue.
     *
     * @param interpreter false loads the explored catalogue (Data Integrator), true loads every
     *     structure the level generator can place (Structure Interpreter).
     */
    public static void structureOperatorLoadCatalogue(int containerId, boolean interpreter) {
        CHANNEL.sendToServer(new OperatorCatalogueRequestPacket(containerId, interpreter));
    }

    /**
     * Asks for one catalogue entry's snapshot, reusing whatever the block entity has cached.
     *
     * <p>Analysis is asynchronous, so the first answer can come back empty. The screen re-asks on a
     * tick timer until the snapshot lands; the block entity deduplicates and caches, so the repeats
     * are cheap.
     */
    public static void structureOperatorRequestDetail(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new OperatorAnalysisRequestPacket(containerId, dimension, structure, false));
    }

    /** The same request, but discarding the cached snapshot first. */
    public static void structureOperatorRefreshDetail(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new OperatorAnalysisRequestPacket(containerId, dimension, structure, true));
    }

    /** Writes the selected catalogue entry's snapshot onto the read slot's marker. */
    public static void structureOperatorWrite(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        CHANNEL.sendToServer(
                new OperatorActionPacket(
                        containerId, OperatorAction.WRITE_TO_READ_SLOT, dimension, structure));
    }

    /** What the structure data operator's action buttons ask the server to do. */
    public enum OperatorAction {
        /** Copy the read slot's marker data onto every filled write slot. */
        COPY_TO_OPERANDS,
        /** Strip marker data from every write slot. */
        CLEAR_OPERANDS,
        /** Write the selected catalogue entry's snapshot onto the read slot's marker. */
        WRITE_TO_READ_SLOT
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
                boolean structureMarker = stack.is(ModItems.STRUCTURE_MARKER.get());
                boolean chestMarker = stack.is(ModItems.CHEST_MARKER.get());
                if (structureMarker || chestMarker) {
                    switch (packet.action()) {
                        case CLEAR -> {
                            if (structureMarker) StructMarkerItem.clearMarker(stack);
                            else ChestMarkerItem.clearMarker(stack);
                            openRefreshedMarker(player, stack, packet.hand());
                        }
                        case REQUEST_SELECTION -> {
                            if (structureMarker) requestChoices(player, stack, packet.hand());
                        }
                        case SELECT -> {
                            if (structureMarker
                                    && player.blockPosition().equals(packet.selectionPosition())
                                    && StructMarkerItem.markAt(
                                            player.serverLevel(),
                                            stack,
                                            packet.selectionPosition(),
                                            packet.selectionIndex())) {
                                openRefreshedMarker(player, stack, packet.hand());
                            } else if (structureMarker) {
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
                            () -> {
                                if (packet.marker().is(ModItems.CHEST_MARKER.get())) {
                                    com.suntide_20210418.dimensiontech.client.ChestMarkerClient
                                            .open(packet.marker(), packet.hand());
                                } else {
                                    com.suntide_20210418.dimensiontech.client.StructMarkerClient
                                            .open(packet.marker(), packet.hand());
                                }
                            });
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

    private record StructureMinerAnalysisRequestPacket(int containerId, int slot) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
        }

        private static StructureMinerAnalysisRequestPacket decode(FriendlyByteBuf buffer) {
            return new StructureMinerAnalysisRequestPacket(buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(
                StructureMinerAnalysisRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().refreshMarkerAnalysis();
                StructureMinerAnalysisSnapshot snapshot =
                        menu.getBlockEntity().getMarkerAnalysisSnapshot(packet.slot());
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new StructureMinerAnalysisPacket(
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

    private record StructureMinerAnalysisPacket(
            int containerId,
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        private static final int MAX_ITEMS = 4096;

        private StructureMinerAnalysisPacket {
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

        private static StructureMinerAnalysisPacket decode(FriendlyByteBuf buffer) {
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
            return new StructureMinerAnalysisPacket(
                    containerId,
                    slot,
                    dimensionValue,
                    structureValue,
                    equipmentDismantling,
                    itemExpectations,
                    disabledItems);
        }

        private static void handle(
                StructureMinerAnalysisPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () ->
                                    com.suntide_20210418.dimensiontech.client.gui.screen
                                            .StructureMinerScreen.receiveAnalysis(
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

    private record StructureMinerExpectedItemTogglePacket(
            int containerId, int slot, ResourceLocation itemId) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
            buffer.writeResourceLocation(itemId);
        }

        private static StructureMinerExpectedItemTogglePacket decode(FriendlyByteBuf buffer) {
            return new StructureMinerExpectedItemTogglePacket(
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation());
        }

        private static void handle(
                StructureMinerExpectedItemTogglePacket packet,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().toggleExpectedItem(packet.itemId());
                menu.getBlockEntity().refreshMarkerAnalysis();
                StructureMinerAnalysisSnapshot snapshot =
                        menu.getBlockEntity().getMarkerAnalysisSnapshot(packet.slot());
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new StructureMinerAnalysisPacket(
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

    private record StructureMinerSlotTogglePacket(int containerId, int slot) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(slot);
        }

        private static StructureMinerSlotTogglePacket decode(FriendlyByteBuf buffer) {
            return new StructureMinerSlotTogglePacket(buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(
                StructureMinerSlotTogglePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureMinerMenu menu
                    && menu.containerId == packet.containerId()
                    && packet.slot() >= 0
                    && packet.slot() < menu.getContainerSlotCount()
                    && menu.stillValid(player)) {
                menu.getBlockEntity().toggleSlotEnabled(packet.slot());
            }
            context.setPacketHandled(true);
        }
    }

    private record StructureReactorTooltipPacket(int containerId, ReactorTooltipSnapshot snapshot) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(snapshot.revision());
            writeCandidates(buffer, snapshot.fragmentCandidates());
            buffer.writeVarInt(snapshot.fragmentCandidateTotal());
            buffer.writeVarInt(snapshot.fragmentRequiredCount());
            buffer.writeBoolean(snapshot.operationRequirementKnown());
            writeCandidates(buffer, snapshot.operationCandidates());
            buffer.writeVarInt(snapshot.operationCandidateTotal());
            buffer.writeVarInt(snapshot.operationStepIndex());
            buffer.writeVarInt(snapshot.operationStepCount());
            buffer.writeVarInt(snapshot.inputRequiredAmount());
            buffer.writeVarInt(snapshot.outputExpectedAmount());
            List<com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle.Resolution>
                    outcomes = snapshot.stateOutcomes();
            buffer.writeVarInt(outcomes.size());
            for (var outcome : outcomes) buffer.writeVarInt(outcome.ordinal());
            buffer.writeVarInt(fluidId(snapshot.expectedInputFluid()));
            buffer.writeVarInt(fluidId(snapshot.expectedOutputFluid()));
        }

        private static StructureReactorTooltipPacket decode(FriendlyByteBuf buffer) {
            int containerId = buffer.readVarInt();
            int revision = buffer.readVarInt();
            List<ItemStack> fragmentCandidates = readCandidates(buffer);
            int fragmentCandidateTotal = buffer.readVarInt();
            int fragmentRequiredCount = buffer.readVarInt();
            boolean operationRequirementKnown = buffer.readBoolean();
            List<ItemStack> operationCandidates = readCandidates(buffer);
            int operationCandidateTotal = buffer.readVarInt();
            int operationStepIndex = buffer.readVarInt();
            int operationStepCount = buffer.readVarInt();
            int inputRequiredAmount = buffer.readVarInt();
            int outputExpectedAmount = buffer.readVarInt();
            var outcomes = new ArrayList<
                    com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle
                            .Resolution>();
            int outcomeCount = buffer.readVarInt();
            var resolutionValues =
                    com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle
                            .Resolution.values();
            for (int index = 0; index < outcomeCount; index++) {
                int ordinal = buffer.readVarInt();
                outcomes.add(
                        ordinal >= 0 && ordinal < resolutionValues.length
                                ? resolutionValues[ordinal]
                                : com.suntide_20210418.dimensiontech.structurereactor
                                        .StructureReactorCycle.Resolution.NONE);
            }
            Fluid expectedInputFluid = fluid(buffer.readVarInt());
            Fluid expectedOutputFluid = fluid(buffer.readVarInt());
            return new StructureReactorTooltipPacket(
                    containerId,
                    new ReactorTooltipSnapshot(
                            fragmentCandidates,
                            fragmentCandidateTotal,
                            fragmentRequiredCount,
                            operationCandidates,
                            operationCandidateTotal,
                            operationRequirementKnown,
                            operationStepIndex,
                            operationStepCount,
                            inputRequiredAmount,
                            outputExpectedAmount,
                            expectedInputFluid,
                            expectedOutputFluid,
                            outcomes,
                            revision));
        }

        private static void handle(
                StructureReactorTooltipPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () -> {
                                net.minecraft.client.Minecraft minecraft =
                                        net.minecraft.client.Minecraft.getInstance();
                                if (minecraft.player != null
                                        && minecraft.player.containerMenu
                                                instanceof
                                                com.suntide_20210418.dimensiontech.client.gui.menu
                                                                .StructureReactorMenu
                                                        menu
                                        && menu.containerId == packet.containerId())
                                    menu.applyTooltipSnapshot(packet.snapshot());
                            });
            context.setPacketHandled(true);
        }

        private static void writeCandidates(FriendlyByteBuf buffer, List<ItemStack> candidates) {
            int count = Math.min(ReactorTooltipSnapshot.MAX_CANDIDATES, candidates.size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) buffer.writeItem(candidates.get(index));
        }

        private static List<ItemStack> readCandidates(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > ReactorTooltipSnapshot.MAX_CANDIDATES)
                throw new IllegalArgumentException(
                        "Invalid reactor tooltip candidate count: " + count);
            List<ItemStack> candidates = new ArrayList<>(count);
            for (int index = 0; index < count; index++) candidates.add(buffer.readItem());
            return List.copyOf(candidates);
        }

        private static int fluidId(Fluid fluid) {
            return fluid == null || fluid == Fluids.EMPTY
                    ? -1
                    : BuiltInRegistries.FLUID.getId(fluid);
        }

        private static Fluid fluid(int id) {
            if (id < 0) return Fluids.EMPTY;
            Fluid fluid = BuiltInRegistries.FLUID.byId(id);
            return fluid == null ? Fluids.EMPTY : fluid;
        }
    }

    // ------------------------------------------------------- operator packets

    /** One catalogue row on the wire. Deliberately not the block entity's own record type. */
    private record CatalogueRow(ResourceLocation dimension, ResourceLocation structure) {}

    private record OperatorCatalogueRequestPacket(int containerId, boolean allStructures) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeBoolean(allStructures);
        }

        private static OperatorCatalogueRequestPacket decode(FriendlyByteBuf buffer) {
            return new OperatorCatalogueRequestPacket(buffer.readVarInt(), buffer.readBoolean());
        }

        private static void handle(
                OperatorCatalogueRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureDataOperatorMenu menu
                    && menu.containerId == packet.containerId()
                    && menu.stillValid(player)) {
                StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
                blockEntity.refreshCatalogue(player, packet.allStructures());
                List<CatalogueRow> rows = new ArrayList<>();
                for (StructureDataOperatorBlockEntity.StructureCatalogueEntry entry :
                        blockEntity.catalogue()) {
                    rows.add(new CatalogueRow(entry.dimension(), entry.structure()));
                }
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OperatorCataloguePacket(
                                packet.containerId(), packet.allStructures(), rows));
            }
            context.setPacketHandled(true);
        }
    }

    private record OperatorCataloguePacket(
            int containerId, boolean allStructures, List<CatalogueRow> rows) {
        private static final int MAX_ROWS = 8192;

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeBoolean(allStructures);
            int count = Math.min(MAX_ROWS, rows.size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                buffer.writeResourceLocation(rows.get(index).dimension());
                buffer.writeResourceLocation(rows.get(index).structure());
            }
        }

        private static OperatorCataloguePacket decode(FriendlyByteBuf buffer) {
            int containerId = buffer.readVarInt();
            boolean all = buffer.readBoolean();
            int count = Math.max(0, Math.min(MAX_ROWS, buffer.readVarInt()));
            List<CatalogueRow> rows = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                rows.add(
                        new CatalogueRow(
                                buffer.readResourceLocation(), buffer.readResourceLocation()));
            }
            return new OperatorCataloguePacket(containerId, all, List.copyOf(rows));
        }

        private static void handle(
                OperatorCataloguePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () -> {
                                if (net.minecraft.client.Minecraft.getInstance().screen
                                        instanceof StructureDataOperatorScreen screen) {
                                    List<StructureDataOperatorBlockEntity.StructureCatalogueEntry>
                                            entries = new ArrayList<>(packet.rows().size());
                                    for (CatalogueRow row : packet.rows()) {
                                        entries.add(
                                                new StructureDataOperatorBlockEntity
                                                        .StructureCatalogueEntry(
                                                        row.dimension(), row.structure()));
                                    }
                                    screen.receiveCatalogue(entries);
                                }
                            });
            context.setPacketHandled(true);
        }
    }

    private record OperatorAnalysisRequestPacket(
            int containerId, ResourceLocation dimension, ResourceLocation structure, boolean force) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeResourceLocation(dimension);
            buffer.writeResourceLocation(structure);
            buffer.writeBoolean(force);
        }

        private static OperatorAnalysisRequestPacket decode(FriendlyByteBuf buffer) {
            return new OperatorAnalysisRequestPacket(
                    buffer.readVarInt(),
                    buffer.readResourceLocation(),
                    buffer.readResourceLocation(),
                    buffer.readBoolean());
        }

        private static void handle(
                OperatorAnalysisRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureDataOperatorMenu menu
                    && menu.containerId == packet.containerId()
                    && menu.stillValid(player)) {
                StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
                if (packet.force()) {
                    blockEntity.refreshCatalogueAnalysis(packet.dimension(), packet.structure());
                }
                ItemStack marker =
                        blockEntity.analyseCatalogueEntry(packet.dimension(), packet.structure());
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OperatorAnalysisPacket(
                                packet.containerId(),
                                packet.dimension(),
                                packet.structure(),
                                marker));
            }
            context.setPacketHandled(true);
        }
    }

    private record OperatorAnalysisPacket(
            int containerId,
            ResourceLocation dimension,
            ResourceLocation structure,
            ItemStack marker) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeResourceLocation(dimension);
            buffer.writeResourceLocation(structure);
            buffer.writeItem(marker);
        }

        private static OperatorAnalysisPacket decode(FriendlyByteBuf buffer) {
            return new OperatorAnalysisPacket(
                    buffer.readVarInt(),
                    buffer.readResourceLocation(),
                    buffer.readResourceLocation(),
                    buffer.readItem());
        }

        private static void handle(
                OperatorAnalysisPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                            () -> {
                                if (net.minecraft.client.Minecraft.getInstance().screen
                                        instanceof StructureDataOperatorScreen screen) {
                                    screen.receiveDetail(
                                            packet.dimension(),
                                            packet.structure(),
                                            packet.marker());
                                }
                            });
            context.setPacketHandled(true);
        }
    }

    private record OperatorActionPacket(
            int containerId,
            OperatorAction action,
            ResourceLocation dimension,
            ResourceLocation structure) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeEnum(action);
            buffer.writeNullable(dimension, FriendlyByteBuf::writeResourceLocation);
            buffer.writeNullable(structure, FriendlyByteBuf::writeResourceLocation);
        }

        private static OperatorActionPacket decode(FriendlyByteBuf buffer) {
            return new OperatorActionPacket(
                    buffer.readVarInt(),
                    buffer.readEnum(OperatorAction.class),
                    buffer.readNullable(FriendlyByteBuf::readResourceLocation),
                    buffer.readNullable(FriendlyByteBuf::readResourceLocation));
        }

        private static void handle(
                OperatorActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.containerMenu instanceof StructureDataOperatorMenu menu
                    && menu.containerId == packet.containerId()
                    && menu.stillValid(player)) {
                StructureDataOperatorBlockEntity blockEntity = menu.blockEntity();
                switch (packet.action()) {
                    case COPY_TO_OPERANDS -> blockEntity.copyData();
                    case CLEAR_OPERANDS -> blockEntity.clearOperandData();
                    case WRITE_TO_READ_SLOT -> {
                        if (packet.dimension() != null && packet.structure() != null) {
                            blockEntity.writeCatalogueEntry(packet.dimension(), packet.structure());
                        }
                    }
                }
            }
            context.setPacketHandled(true);
        }
    }

    // ------------------------------------------------------- chest analysis

    /**
     * The chest marker's analyse-key request: which block the client's crosshair is pointing at and
     * which hand holds the marker. The server re-validates the position against the player before
     * marking, so a stale or spoofed target cannot mark an out-of-reach chest.
     */
    private record ChestAnalysisRequestPacket(InteractionHand hand, BlockPos position) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            buffer.writeBlockPos(position);
        }

        private static ChestAnalysisRequestPacket decode(FriendlyByteBuf buffer) {
            return new ChestAnalysisRequestPacket(
                    buffer.readEnum(InteractionHand.class), buffer.readBlockPos());
        }

        private static void handle(
                ChestAnalysisRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(packet.hand());
                if (stack.is(ModItems.CHEST_MARKER.get())) {
                    net.minecraft.server.level.ServerLevel serverLevel = player.serverLevel();
                    BlockPos position = packet.position();
                    if (player.blockPosition().distSqr(position) <= 64.0D) {
                        ResourceLocation lootTable =
                                ChestMarkerItem.lootTableAt(serverLevel, position);
                        if (lootTable != null) {
                            ChestMarkerItem.markChest(serverLevel, stack, position, lootTable);
                            openRefreshedMarker(player, stack, packet.hand());
                            context.setPacketHandled(true);
                            return;
                        }
                    }
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.dimension_tech.chest_marker.no_target"),
                            true);
                }
            }
            context.setPacketHandled(true);
        }
    }
}
