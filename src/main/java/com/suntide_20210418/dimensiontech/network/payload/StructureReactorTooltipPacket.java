package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.ClientPayloadHandlers;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 结构反应堆界面的 tooltip 快照。服务端 → 客户端。 */
public record StructureReactorTooltipPacket(int containerId, ReactorTooltipSnapshot snapshot)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureReactorTooltipPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "reactor_tooltip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureReactorTooltipPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureReactorTooltipPacket::encode,
                            StructureReactorTooltipPacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureReactorTooltipPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureReactorTooltipPacket payload) {
        ReactorTooltipSnapshot snapshot = payload.snapshot();
        buffer.writeVarInt(payload.containerId());
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
        List<StructureReactorCycle.Resolution> outcomes = snapshot.stateOutcomes();
        buffer.writeVarInt(outcomes.size());
        for (StructureReactorCycle.Resolution outcome : outcomes) {
            buffer.writeVarInt(outcome.ordinal());
        }
        buffer.writeVarInt(fluidId(snapshot.expectedInputFluid()));
        buffer.writeVarInt(fluidId(snapshot.expectedOutputFluid()));
    }

    public static StructureReactorTooltipPacket decode(RegistryFriendlyByteBuf buffer) {
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
        List<StructureReactorCycle.Resolution> outcomes = new ArrayList<>();
        int outcomeCount = buffer.readVarInt();
        StructureReactorCycle.Resolution[] resolutionValues =
                StructureReactorCycle.Resolution.values();
        for (int index = 0; index < outcomeCount; index++) {
            int ordinal = buffer.readVarInt();
            outcomes.add(
                    ordinal >= 0 && ordinal < resolutionValues.length
                            ? resolutionValues[ordinal]
                            : StructureReactorCycle.Resolution.NONE);
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

    public static void handle(StructureReactorTooltipPacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPayloadHandlers.receiveReactorTooltip(payload.containerId(), payload.snapshot());
        }
    }

    private static void writeCandidates(
            RegistryFriendlyByteBuf buffer, List<ItemStack> candidates) {
        int count = Math.min(ReactorTooltipSnapshot.MAX_CANDIDATES, candidates.size());
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, candidates.get(index));
        }
    }

    private static List<ItemStack> readCandidates(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        // 越界计数来自恶意包，必须显式拒绝而不是让它决定循环次数。
        if (count < 0 || count > ReactorTooltipSnapshot.MAX_CANDIDATES) {
            throw new IllegalArgumentException("Invalid reactor tooltip candidate count: " + count);
        }
        List<ItemStack> candidates = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            candidates.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
        return List.copyOf(candidates);
    }

    private static int fluidId(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY ? -1 : BuiltInRegistries.FLUID.getId(fluid);
    }

    private static Fluid fluid(int id) {
        if (id < 0) return Fluids.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return fluid == null ? Fluids.EMPTY : fluid;
    }
}
