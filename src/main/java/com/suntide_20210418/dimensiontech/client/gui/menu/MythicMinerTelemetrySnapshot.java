package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import java.util.List;

/** Named, immutable view of the legacy ContainerData telemetry channels. */
public record MythicMinerTelemetrySnapshot(
        int energyStored,
        int energyCapacity,
        int energyConsumption,
        int workingThreads,
        int totalParallel,
        int baseParallel,
        int efficiencyHundredths,
        int luckHundredths,
        BaseMinerBlockEntity.OutputState outputState,
        BaseMinerBlockEntity.RedstoneMode redstoneMode,
        int outputFaceMask,
        boolean structureComplete,
        boolean equipmentDismantling,
        boolean hasFluid,
        int fluidAmount,
        int fluidCapacity,
        List<Marker> markers) {

    public record Marker(long progress, int processingTime, int parallel, boolean enabled,
            long naturalTicks, long actualTicks, boolean waitingForNaturalWindow) {}

    static int combineUnsignedWords(int low, int high) {
        return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
    }

    static long combineUnsignedWords(int word0, int word1, int word2, int word3) {
        return (word0 & 0xFFFFL)
                | ((word1 & 0xFFFFL) << 16)
                | ((word2 & 0xFFFFL) << 32)
                | ((word3 & 0xFFFFL) << 48);
    }

    public static MythicMinerTelemetrySnapshot from(MythicMinerMenu menu) {
        var markers = new java.util.ArrayList<Marker>(menu.getContainerSlotCount());
        for (int slot = 0; slot < menu.getContainerSlotCount(); slot++) {
            markers.add(new Marker(
                    menu.getMarkerProgress(slot),
                    menu.getMarkerProcessingTime(slot),
                    menu.getMarkerTotalParallel(slot),
                    menu.isMarkerSlotEnabled(slot),
                    menu.getMarkerCurrentNaturalTicks(slot),
                    menu.getMarkerCurrentExternalAccelerationMachineTicks(slot),
                    menu.isMarkerWaitingForNaturalWindow(slot)));
        }
        int output = Math.max(0, Math.min(BaseMinerBlockEntity.OutputState.values().length - 1,
                menu.getTelemetry(5)));
        int redstone = Math.max(0, Math.min(BaseMinerBlockEntity.RedstoneMode.values().length - 1,
                menu.getTelemetry(11)));
        return new MythicMinerTelemetrySnapshot(
                menu.getEnergyStored(), menu.getEnergyCapacity(), menu.getEffectiveEnergyConsumption(),
                menu.getWorkingThreadCount(), menu.getTotalParallel(), menu.getBaseParallel(),
                menu.getEfficiencyHundredths(), menu.getLuckHundredths(),
                BaseMinerBlockEntity.OutputState.values()[output],
                BaseMinerBlockEntity.RedstoneMode.values()[redstone],
                menu.getTelemetry(13), menu.getTelemetry(14) != 0,
                menu.isEquipmentDismantlingEnabled(), menu.hasFluid(), menu.getFluidAmount(),
                menu.getFluidCapacity(), List.copyOf(markers));
    }
}
