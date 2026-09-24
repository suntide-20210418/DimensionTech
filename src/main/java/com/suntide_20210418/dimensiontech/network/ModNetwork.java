package com.suntide_20210418.dimensiontech.network;

import com.suntide_20210418.dimensiontech.network.payload.ChestAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorActionPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorCatalogueRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.RefreshedMarkerPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructMarkerActionPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerExpectedItemTogglePacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerSlotTogglePacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureReactorTooltipPacket;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 发送侧门面：其余代码只通过这里的静态方法发包。payload 的类型、编解码与处理体各自放在 {@code network.payload} 下，注册在 {@link
 * NetworkHandler}。
 */
public final class ModNetwork {

    private ModNetwork() {}

    public static void requestStructureSelection(InteractionHand hand) {
        PacketDistributor.sendToServer(
                new StructMarkerActionPacket(
                        hand, MarkerAction.REQUEST_SELECTION, BlockPos.ZERO, -1));
    }

    public static void selectStructure(
            InteractionHand hand, BlockPos selectionPosition, int selectionIndex) {
        PacketDistributor.sendToServer(
                new StructMarkerActionPacket(
                        hand, MarkerAction.SELECT, selectionPosition, selectionIndex));
    }

    public static void clear(InteractionHand hand) {
        PacketDistributor.sendToServer(
                new StructMarkerActionPacket(hand, MarkerAction.CLEAR, BlockPos.ZERO, -1));
    }

    /** Asks the server to analyse the given position with the chest marker in {@code hand}. */
    public static void requestChestAnalysis(InteractionHand hand, BlockPos position) {
        PacketDistributor.sendToServer(new ChestAnalysisRequestPacket(hand, position));
    }

    public static void requestStructureMinerAnalysis(int containerId, int slot) {
        PacketDistributor.sendToServer(new StructureMinerAnalysisRequestPacket(containerId, slot));
    }

    public static void toggleStructureMinerExpectedItem(
            int containerId, int slot, ResourceLocation itemId) {
        PacketDistributor.sendToServer(
                new StructureMinerExpectedItemTogglePacket(containerId, slot, itemId));
    }

    public static void toggleStructureMinerSlot(int containerId, int slot) {
        PacketDistributor.sendToServer(new StructureMinerSlotTogglePacket(containerId, slot));
    }

    public static void sendReactorTooltipSnapshot(
            ServerPlayer player, int containerId, ReactorTooltipSnapshot snapshot) {
        PacketDistributor.sendToPlayer(
                player, new StructureReactorTooltipPacket(containerId, snapshot));
    }

    public static void openRefreshedMarker(
            ServerPlayer player, ItemStack marker, InteractionHand hand) {
        PacketDistributor.sendToPlayer(player, new RefreshedMarkerPacket(marker.copy(), hand));
    }

    // ------------------------------------------------- structure data operator

    /** Copies the read slot's marker data onto every filled write slot. */
    public static void structureOperatorCopy(int containerId) {
        PacketDistributor.sendToServer(
                new OperatorActionPacket(containerId, OperatorAction.COPY_TO_OPERANDS, null, null));
    }

    /** Strips marker data from every write slot. */
    public static void structureOperatorClearOperands(int containerId) {
        PacketDistributor.sendToServer(
                new OperatorActionPacket(containerId, OperatorAction.CLEAR_OPERANDS, null, null));
    }

    /**
     * Rebuilds the operator's structure catalogue.
     *
     * @param interpreter false loads the explored catalogue (Data Integrator), true loads every
     *     structure the level generator can place (Structure Interpreter).
     */
    public static void structureOperatorLoadCatalogue(int containerId, boolean interpreter) {
        PacketDistributor.sendToServer(
                new OperatorCatalogueRequestPacket(containerId, interpreter));
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
        PacketDistributor.sendToServer(
                new OperatorAnalysisRequestPacket(containerId, dimension, structure, false));
    }

    /** The same request, but discarding the cached snapshot first. */
    public static void structureOperatorRefreshDetail(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        PacketDistributor.sendToServer(
                new OperatorAnalysisRequestPacket(containerId, dimension, structure, true));
    }

    /** Writes the selected catalogue entry's snapshot onto the read slot's marker. */
    public static void structureOperatorWrite(
            int containerId, ResourceLocation dimension, ResourceLocation structure) {
        PacketDistributor.sendToServer(
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

    /** 结构标记器的操作种类，作为 {@link StructMarkerActionPacket} 的动作字段。 */
    public enum MarkerAction {
        REQUEST_SELECTION,
        SELECT,
        CLEAR
    }
}
