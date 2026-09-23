package com.suntide_20210418.dimensiontech.network;

import com.suntide_20210418.dimensiontech.network.payload.ChestAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorActionPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorAnalysisPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorCataloguePacket;
import com.suntide_20210418.dimensiontech.network.payload.OperatorCatalogueRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.RefreshedMarkerPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureChoicesPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerAnalysisPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerAnalysisRequestPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerExpectedItemTogglePacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureMinerSlotTogglePacket;
import com.suntide_20210418.dimensiontech.network.payload.StructureReactorTooltipPacket;
import com.suntide_20210418.dimensiontech.network.payload.StructMarkerActionPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络注册入口。payload 的身份是各自的 {@code TYPE} id，不再有手写序号。
 *
 * <p>这里是网络协议版本号的唯一归属（沿用旧的 "10"），只影响 Neo-Neo 连接时的兼容判定。
 */
public final class NetworkHandler {
    private static final String VERSION = "10";

    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        // 客户端 → 服务端
        registrar.playToServer(
                StructMarkerActionPacket.TYPE,
                StructMarkerActionPacket.STREAM_CODEC,
                StructMarkerActionPacket::handle);
        registrar.playToServer(
                ChestAnalysisRequestPacket.TYPE,
                ChestAnalysisRequestPacket.STREAM_CODEC,
                ChestAnalysisRequestPacket::handle);
        registrar.playToServer(
                StructureMinerAnalysisRequestPacket.TYPE,
                StructureMinerAnalysisRequestPacket.STREAM_CODEC,
                StructureMinerAnalysisRequestPacket::handle);
        registrar.playToServer(
                StructureMinerExpectedItemTogglePacket.TYPE,
                StructureMinerExpectedItemTogglePacket.STREAM_CODEC,
                StructureMinerExpectedItemTogglePacket::handle);
        registrar.playToServer(
                StructureMinerSlotTogglePacket.TYPE,
                StructureMinerSlotTogglePacket.STREAM_CODEC,
                StructureMinerSlotTogglePacket::handle);
        registrar.playToServer(
                OperatorCatalogueRequestPacket.TYPE,
                OperatorCatalogueRequestPacket.STREAM_CODEC,
                OperatorCatalogueRequestPacket::handle);
        registrar.playToServer(
                OperatorAnalysisRequestPacket.TYPE,
                OperatorAnalysisRequestPacket.STREAM_CODEC,
                OperatorAnalysisRequestPacket::handle);
        registrar.playToServer(
                OperatorActionPacket.TYPE,
                OperatorActionPacket.STREAM_CODEC,
                OperatorActionPacket::handle);

        // 服务端 → 客户端
        registrar.playToClient(
                RefreshedMarkerPacket.TYPE,
                RefreshedMarkerPacket.STREAM_CODEC,
                RefreshedMarkerPacket::handle);
        registrar.playToClient(
                StructureChoicesPacket.TYPE,
                StructureChoicesPacket.STREAM_CODEC,
                StructureChoicesPacket::handle);
        registrar.playToClient(
                StructureMinerAnalysisPacket.TYPE,
                StructureMinerAnalysisPacket.STREAM_CODEC,
                StructureMinerAnalysisPacket::handle);
        registrar.playToClient(
                StructureReactorTooltipPacket.TYPE,
                StructureReactorTooltipPacket.STREAM_CODEC,
                StructureReactorTooltipPacket::handle);
        registrar.playToClient(
                OperatorCataloguePacket.TYPE,
                OperatorCataloguePacket.STREAM_CODEC,
                OperatorCataloguePacket::handle);
        registrar.playToClient(
                OperatorAnalysisPacket.TYPE,
                OperatorAnalysisPacket.STREAM_CODEC,
                OperatorAnalysisPacket::handle);
    }
}