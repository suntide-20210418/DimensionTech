package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MythicMinerMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SLOT_Y = MythicMinerLayout.MARKER_SLOT_Y;
    public static final int BASE_PLAYER_INVENTORY_Y = MythicMinerLayout.BASE_PLAYER_INVENTORY_Y;
    public static final int MENU_WIDTH = 320;
    public static final int INVENTORY_START_X = (MENU_WIDTH - 162) / 2;

    private final BaseMinerBlockEntity blockEntity;
    private final int containerSlotCount;
    private final int containerRows;
    private final int playerInventoryY;
    private static final int ENERGY_STORED_HIGH = 15;
    private static final int ENERGY_CAPACITY_HIGH = 16;
    private static final int ENERGY_CONSUMPTION_HIGH = 17;
    private static final int PARALLEL_HIGH = 18;
    private static final int EFFICIENCY_LOW = 19;
    private static final int EFFICIENCY_HIGH = 20;
    private static final int LUCK_LOW = 21;
    private static final int LUCK_HIGH = 22;
    private static final int EFFICIENCY_BONUS_LOW = 23;
    private static final int EFFICIENCY_BONUS_HIGH = 24;
    private static final int CAPACITY_BONUS_LOW = 25;
    private static final int CAPACITY_BONUS_HIGH = 26;
    private static final int CONSUMPTION_BONUS_LOW = 27;
    private static final int CONSUMPTION_BONUS_HIGH = 28;
    private static final int PARALLEL_BONUS_LOW = 29;
    private static final int PARALLEL_BONUS_HIGH = 30;
    private static final int LUCK_BONUS_LOW = 31;
    private static final int LUCK_BONUS_HIGH = 32;
    private static final int EFFICIENCY_UPGRADE_COUNT = 33;
    private static final int ENERGY_UPGRADE_COUNT = 34;
    private static final int PARALLEL_UPGRADE_COUNT = 35;
    private static final int LUCK_UPGRADE_COUNT = 36;
    private static final int AGGREGATE_UPGRADE_COUNT = 37;
    private static final int BASE_PARALLEL_LOW = 38;
    private static final int BASE_PARALLEL_HIGH = 39;
    private static final int EQUIPMENT_DISMANTLING = 40;
    private static final int EXTERNAL_ACCELERATION_PARALLEL_LOW = 41;
    private static final int EXTERNAL_ACCELERATION_PARALLEL_HIGH = 42;
    private static final int EXTERNAL_ACCELERATION_TICKS_LOW = 43;
    private static final int EXTERNAL_ACCELERATION_TICKS_HIGH = 44;
    private static final int EXTERNAL_ACCELERATION_TICKS_LOW_HIGH = 45;
    private static final int EXTERNAL_ACCELERATION_TICKS_HIGH_HIGH = 46;
    private static final int EXTERNAL_EQUIVALENT_ACCELERATION_0 = 47;
    private static final int EXTERNAL_EQUIVALENT_ACCELERATION_1 = 48;
    private static final int EXTERNAL_EQUIVALENT_ACCELERATION_2 = 49;
    private static final int EXTERNAL_EQUIVALENT_ACCELERATION_3 = 50;
    private static final int TELEMETRY_BASE_COUNT = 51;
    private static final int SLOT_TELEMETRY_STRIDE = 26;
    private static final int SLOT_PROGRESS_LOW = 0;
    private static final int SLOT_PROGRESS_HIGH = 1;
    private static final int SLOT_PROCESSING_LOW = 2;
    private static final int SLOT_PROCESSING_HIGH = 3;
    private static final int SLOT_PARALLEL_LOW = 4;
    private static final int SLOT_PARALLEL_HIGH = 5;
    private static final int SLOT_ENABLED = 6;
    private static final int SLOT_EXTERNAL_PARALLEL_LOW = 7;
    private static final int SLOT_EXTERNAL_PARALLEL_HIGH = 8;
    private static final int SLOT_NATURAL_TICKS_LOW = 9;
    private static final int SLOT_NATURAL_TICKS_HIGH = 10;
    private static final int SLOT_ACTUAL_TICKS_0 = 11;
    private static final int SLOT_ACTUAL_TICKS_1 = 12;
    private static final int SLOT_ACTUAL_TICKS_2 = 13;
    private static final int SLOT_ACTUAL_TICKS_3 = 14;
    private static final int SLOT_PREVIOUS_TICKS_0 = 15;
    private static final int SLOT_PREVIOUS_TICKS_1 = 16;
    private static final int SLOT_PREVIOUS_TICKS_2 = 17;
    private static final int SLOT_PREVIOUS_TICKS_3 = 18;
    private static final int SLOT_PREVIOUS_PARALLEL_LOW = 19;
    private static final int SLOT_PREVIOUS_PARALLEL_HIGH = 20;
    private static final int SLOT_WAITING_FOR_NATURAL_WINDOW = 21;
    private static final int SLOT_EQUIVALENT_0 = 22;
    private static final int SLOT_EQUIVALENT_1 = 23;
    private static final int SLOT_EQUIVALENT_2 = 24;
    private static final int SLOT_EQUIVALENT_3 = 25;

    private final int[] telemetry;

    public MythicMinerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, data.readBlockPos()));
    }

    public MythicMinerMenu(
            int containerId, Inventory playerInventory, BaseMinerBlockEntity blockEntity) {
        super(ModMenu.MYTHIC_MINER.get(), containerId);
        this.blockEntity = blockEntity;
        IItemHandler itemHandler = blockEntity.getItemHandler();
        this.containerSlotCount = itemHandler.getSlots();
        this.containerRows = MythicMinerLayout.rowsForSlotCount(containerSlotCount);
        this.playerInventoryY = MythicMinerLayout.playerInventoryY(containerRows);
        this.telemetry =
                new int[TELEMETRY_BASE_COUNT + containerSlotCount * SLOT_TELEMETRY_STRIDE];

        addContainerSlots(itemHandler);
        addPlayerInventory(playerInventory);
        addDataSlots(
                new net.minecraft.world.inventory.ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> {
                                int slot = firstActiveSlot();
                                // ContainerData is an int-based legacy channel. The complete
                                // logical tick value is synchronized through slot telemetry;
                                // this slot carries only its low word for compatibility.
                                yield slot >= 0
                                        ? lowWord(blockEntity.getSlotLogicalProgress(slot))
                                        : 0;
                            }
                            case 1 -> blockEntity.getProcessingTime();
                            case 2 -> lowWord(blockEntity.getEnergyStorage().getEnergyStored());
                            case 3 -> lowWord(blockEntity.getEnergyStorage().getMaxEnergyStored());
                            case 4 -> lowWord(blockEntity.getDrawParallel());
                            case 5 -> blockEntity.getOutputState().ordinal();
                            case 6 -> blockEntity.getPendingOutputCount();
                            case 7 -> blockEntity.getBaseParallelCount();
                            case 8 -> blockEntity.getAccumulatedParallelHundredths();
                            case 9 -> blockEntity.getAdditionalItemCount();
                            case 10 -> lowWord(blockEntity.getEffectiveEnergyConsumption());
                            case 11 -> blockEntity.getRedstoneMode().ordinal();
                            case 12 -> blockEntity.getRedstoneThreshold();
                            case 13 -> blockEntity.getOutputFaceMask();
                            case 14 -> blockEntity.isStructureComplete() ? 1 : 0;
                            case ENERGY_STORED_HIGH ->
                                    highWord(blockEntity.getEnergyStorage().getEnergyStored());
                            case ENERGY_CAPACITY_HIGH ->
                                    highWord(blockEntity.getEnergyStorage().getMaxEnergyStored());
                            case ENERGY_CONSUMPTION_HIGH ->
                                    highWord(blockEntity.getEffectiveEnergyConsumption());
                            case PARALLEL_HIGH -> highWord(blockEntity.getDrawParallel());
                            case EFFICIENCY_LOW ->
                                    lowWord(
                                            toHundredths(
                                                    blockEntity.getEffectiveMachineEfficiency()));
                            case EFFICIENCY_HIGH ->
                                    highWord(
                                            toHundredths(
                                                    blockEntity.getEffectiveMachineEfficiency()));
                            case LUCK_LOW ->
                                    lowWord(toHundredths(blockEntity.getEffectiveMachineLuck()));
                            case LUCK_HIGH ->
                                    highWord(toHundredths(blockEntity.getEffectiveMachineLuck()));
                            case EFFICIENCY_BONUS_LOW ->
                                    lowWord(
                                            toHundredths(
                                                    blockEntity.getEfficiencyUpgradePercent()));
                            case EFFICIENCY_BONUS_HIGH ->
                                    highWord(
                                            toHundredths(
                                                    blockEntity.getEfficiencyUpgradePercent()));
                            case CAPACITY_BONUS_LOW ->
                                    lowWord(
                                            toHundredths(
                                                    blockEntity.getEnergyCapacityUpgradePercent()));
                            case CAPACITY_BONUS_HIGH ->
                                    highWord(
                                            toHundredths(
                                                    blockEntity.getEnergyCapacityUpgradePercent()));
                            case CONSUMPTION_BONUS_LOW ->
                                    lowWord(
                                            toHundredths(
                                                    blockEntity
                                                            .getEnergyConsumptionReductionPercent()));
                            case CONSUMPTION_BONUS_HIGH ->
                                    highWord(
                                            toHundredths(
                                                    blockEntity
                                                            .getEnergyConsumptionReductionPercent()));
                            case PARALLEL_BONUS_LOW ->
                                    lowWord(toHundredths(blockEntity.getParallelUpgradePercent()));
                            case PARALLEL_BONUS_HIGH ->
                                    highWord(toHundredths(blockEntity.getParallelUpgradePercent()));
                            case LUCK_BONUS_LOW ->
                                    lowWord(toHundredths(blockEntity.getLuckUpgradePercent()));
                            case LUCK_BONUS_HIGH ->
                                    highWord(toHundredths(blockEntity.getLuckUpgradePercent()));
                            case EFFICIENCY_UPGRADE_COUNT ->
                                    blockEntity.getUpgradeCount(
                                            MythicMinerUpgradeBlock.Type.EFFICIENCY);
                            case ENERGY_UPGRADE_COUNT ->
                                    blockEntity.getUpgradeCount(
                                            MythicMinerUpgradeBlock.Type.ENERGY);
                            case PARALLEL_UPGRADE_COUNT ->
                                    blockEntity.getUpgradeCount(
                                            MythicMinerUpgradeBlock.Type.PARALLEL);
                            case LUCK_UPGRADE_COUNT ->
                                    blockEntity.getUpgradeCount(MythicMinerUpgradeBlock.Type.LUCK);
                            case AGGREGATE_UPGRADE_COUNT ->
                                    blockEntity.getUpgradeCount(
                                            MythicMinerUpgradeBlock.Type.AGGREGATE);
                            case BASE_PARALLEL_LOW ->
                                    lowWord(blockEntity.getEffectiveBaseParallel());
                            case BASE_PARALLEL_HIGH ->
                                    highWord(blockEntity.getEffectiveBaseParallel());
                            case EQUIPMENT_DISMANTLING ->
                                    blockEntity.isEquipmentDismantlingEnabled() ? 1 : 0;
                            case EXTERNAL_ACCELERATION_PARALLEL_LOW ->
                                    lowWord(
                                            blockEntity
                                                    .getExternalAccelerationParallelHundredths());
                            case EXTERNAL_ACCELERATION_PARALLEL_HIGH ->
                                    highWord(
                                            blockEntity
                                                    .getExternalAccelerationParallelHundredths());
                            case EXTERNAL_ACCELERATION_TICKS_LOW ->
                                    word(
                                            blockEntity.getCurrentExternalAccelerationMachineTicks(),
                                            0);
                            case EXTERNAL_ACCELERATION_TICKS_HIGH ->
                                    word(
                                            blockEntity.getCurrentExternalAccelerationMachineTicks(),
                                            16);
                            case EXTERNAL_ACCELERATION_TICKS_LOW_HIGH ->
                                    word(
                                            blockEntity.getCurrentExternalAccelerationMachineTicks(),
                                            32);
                            case EXTERNAL_ACCELERATION_TICKS_HIGH_HIGH ->
                                    word(
                                            blockEntity.getCurrentExternalAccelerationMachineTicks(),
                                            48);
                            case EXTERNAL_EQUIVALENT_ACCELERATION_0 ->
                                    word(blockEntity.getExternalEquivalentAccelerationTicks(), 0);
                            case EXTERNAL_EQUIVALENT_ACCELERATION_1 ->
                                    word(blockEntity.getExternalEquivalentAccelerationTicks(), 16);
                            case EXTERNAL_EQUIVALENT_ACCELERATION_2 ->
                                    word(blockEntity.getExternalEquivalentAccelerationTicks(), 32);
                            case EXTERNAL_EQUIVALENT_ACCELERATION_3 ->
                                    word(blockEntity.getExternalEquivalentAccelerationTicks(), 48);
                            default -> getSlotTelemetryWord(index);
                        };
                    }

                    @Override
                    public void set(int index, int value) {
                        telemetry[index] = value;
                    }

                    @Override
                    public int getCount() {
                        return telemetry.length;
                    }
                });
    }

    private static BaseMinerBlockEntity getBlockEntity(
            Inventory playerInventory, BlockPos position) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(position);
        if (blockEntity instanceof BaseMinerBlockEntity baseMinerBlockEntity) {
            return baseMinerBlockEntity;
        }
        throw new IllegalStateException("Mythic miner block entity is missing at " + position);
    }

    private void addContainerSlots(IItemHandler itemHandler) {
        int columns = MythicMinerLayout.columnsForSlotCount(containerSlotCount);
        for (int slot = 0; slot < containerSlotCount; slot++) {
            int row = slot / columns;
            addSlot(
                    new SlotItemHandler(
                            itemHandler,
                            slot,
                            MythicMinerLayout.markerSlotX(slot, containerSlotCount),
                            MythicMinerLayout.markerSlotY(row)));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column + row * 9 + 9,
                                INVENTORY_START_X + column * 18,
                                playerInventoryY + row * 18));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_START_X + column * 18, hotbarY));
        }
    }

    public int getContainerRows() {
        return containerRows;
    }

    public int getContainerSlotCount() {
        return containerSlotCount;
    }

    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    public BaseMinerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1 && !player.level().isClientSide) {
            blockEntity.cycleOutputState();
            return true;
        }
        if (id == 2 && !player.level().isClientSide) {
            blockEntity.cycleRedstoneMode();
            return true;
        }
        if (id >= 10 && id < 10 + net.minecraft.core.Direction.values().length) {
            if (!player.level().isClientSide) {
                blockEntity.toggleOutputFace(net.minecraft.core.Direction.values()[id - 10]);
            }
            return true;
        }
        if (id == 3 && player.level() instanceof ServerLevel serverLevel) {
            int tier =
                    blockEntity.getBlockState().getBlock() instanceof BaseMinerBlock miner
                            ? miner.minerTier()
                            : 1;
            MythicMinerMultiblock.place(serverLevel, blockEntity.getBlockPos(), tier);
            return true;
        }
        if (id == 4 && blockEntity.supportsEquipmentDismantling()) {
            if (!player.level().isClientSide) {
                blockEntity.toggleEquipmentDismantling();
            }
            return true;
        }
        return id == 1 || id == 2 || id == 3;
    }

    /** Returns the latest server-synchronized telemetry value for client rendering. */
    public int getTelemetry(int index) {
        return index >= 0 && index < telemetry.length ? telemetry[index] : 0;
    }

    public int getEnergyStored() {
        return combineWords(2, ENERGY_STORED_HIGH);
    }

    public int getEnergyCapacity() {
        return combineWords(3, ENERGY_CAPACITY_HIGH);
    }

    public int getEffectiveEnergyConsumption() {
        return combineWords(10, ENERGY_CONSUMPTION_HIGH);
    }

    public int getTotalParallel() {
        return combineWords(4, PARALLEL_HIGH);
    }

    public int getBaseParallel() {
        return combineWords(BASE_PARALLEL_LOW, BASE_PARALLEL_HIGH);
    }

    public int getExtraEfficiencyParallel() {
        return Math.max(
                0,
                getTotalParallel()
                        - getBaseParallel()
                        - getExternalAccelerationParallelHundredths() / 100);
    }

    public int getExternalAccelerationParallelHundredths() {
        return combineWords(
                EXTERNAL_ACCELERATION_PARALLEL_LOW, EXTERNAL_ACCELERATION_PARALLEL_HIGH);
    }

    public long getExternalEquivalentAccelerationTicks() {
        return combineLongWords(
                EXTERNAL_EQUIVALENT_ACCELERATION_0,
                EXTERNAL_EQUIVALENT_ACCELERATION_1,
                EXTERNAL_EQUIVALENT_ACCELERATION_2,
                EXTERNAL_EQUIVALENT_ACCELERATION_3);
    }

    public long getCurrentExternalAccelerationMachineTicks() {
        return combineLongWords(
                EXTERNAL_ACCELERATION_TICKS_LOW,
                EXTERNAL_ACCELERATION_TICKS_HIGH,
                EXTERNAL_ACCELERATION_TICKS_LOW_HIGH,
                EXTERNAL_ACCELERATION_TICKS_HIGH_HIGH);
    }

    public long getMarkerProgress(int slot) {
        return combineSlotLongWords(slot, SLOT_PROGRESS_LOW, SLOT_PROGRESS_HIGH, -1, -1);
    }

    public int getMarkerProcessingTime(int slot) {
        return isMarkerWaitingForNaturalWindow(slot)
                ? 400
                : combineSlotWords(slot, SLOT_PROCESSING_LOW, SLOT_PROCESSING_HIGH);
    }

    public boolean isMarkerWaitingForNaturalWindow(int slot) {
        return getSlotTelemetryValue(slot, SLOT_WAITING_FOR_NATURAL_WINDOW) != 0;
    }

    public boolean isMarkerExternalAccelerationActive(int slot) {
        return getMarkerCurrentExternalAccelerationMachineTicks(slot)
                != getMarkerCurrentNaturalTicks(slot);
    }

    public boolean isExternalAccelerationActive() {
        for (int slot = 0; slot < containerSlotCount; slot++) {
            if (isMarkerExternalAccelerationActive(slot)) return true;
        }
        return false;
    }

    public int getMarkerTotalParallel(int slot) {
        return combineSlotWords(slot, SLOT_PARALLEL_LOW, SLOT_PARALLEL_HIGH);
    }

    public int getMarkerExtraEfficiencyParallel(int slot) {
        return Math.max(
                0,
                        getMarkerTotalParallel(slot)
                        - getBaseParallel()
                        - getMarkerExternalAccelerationParallelHundredths(slot) / 100);
    }

    public int getMarkerExternalAccelerationParallelHundredths(int slot) {
        return combineSlotWords(slot, SLOT_EXTERNAL_PARALLEL_LOW, SLOT_EXTERNAL_PARALLEL_HIGH);
    }

    public long getMarkerCurrentExternalAccelerationMachineTicks(int slot) {
        return combineSlotLongWords(
                slot,
                SLOT_ACTUAL_TICKS_0,
                SLOT_ACTUAL_TICKS_1,
                SLOT_ACTUAL_TICKS_2,
                SLOT_ACTUAL_TICKS_3);
    }

    public long getMarkerExternalEquivalentAccelerationTicks(int slot) {
        return combineSlotLongWords(
                slot, SLOT_EQUIVALENT_0, SLOT_EQUIVALENT_1, SLOT_EQUIVALENT_2, SLOT_EQUIVALENT_3);
    }

    public long getMarkerCurrentNaturalTicks(int slot) {
        return combineSlotLongWords(slot, SLOT_NATURAL_TICKS_LOW, SLOT_NATURAL_TICKS_HIGH, -1, -1);
    }

    public long getMarkerPreviousExternalAccelerationMachineTicks(int slot) {
        return combineSlotLongWords(
                slot,
                SLOT_PREVIOUS_TICKS_0,
                SLOT_PREVIOUS_TICKS_1,
                SLOT_PREVIOUS_TICKS_2,
                SLOT_PREVIOUS_TICKS_3);
    }

    public int getMarkerPreviousExternalAccelerationParallelHundredths(int slot) {
        return combineSlotWords(slot, SLOT_PREVIOUS_PARALLEL_LOW, SLOT_PREVIOUS_PARALLEL_HIGH);
    }

    public boolean isMarkerSlotEnabled(int slot) {
        return getSlotTelemetryValue(slot, SLOT_ENABLED) != 0;
    }

    public boolean supportsEquipmentDismantling() {
        return blockEntity.supportsEquipmentDismantling();
    }

    public boolean isEquipmentDismantlingEnabled() {
        return getTelemetry(EQUIPMENT_DISMANTLING) != 0;
    }

    public int getEfficiencyHundredths() {
        return combineWords(EFFICIENCY_LOW, EFFICIENCY_HIGH);
    }

    public int getLuckHundredths() {
        return combineWords(LUCK_LOW, LUCK_HIGH);
    }

    public int getEfficiencyBonusHundredths() {
        return combineWords(EFFICIENCY_BONUS_LOW, EFFICIENCY_BONUS_HIGH);
    }

    public int getCapacityBonusHundredths() {
        return combineWords(CAPACITY_BONUS_LOW, CAPACITY_BONUS_HIGH);
    }

    public int getConsumptionReductionHundredths() {
        return combineWords(CONSUMPTION_BONUS_LOW, CONSUMPTION_BONUS_HIGH);
    }

    public int getParallelBonusHundredths() {
        return combineWords(PARALLEL_BONUS_LOW, PARALLEL_BONUS_HIGH);
    }

    public int getLuckBonusHundredths() {
        return combineWords(LUCK_BONUS_LOW, LUCK_BONUS_HIGH);
    }

    public int getEfficiencyUpgradeCount() {
        return getTelemetry(EFFICIENCY_UPGRADE_COUNT);
    }

    public int getEnergyUpgradeCount() {
        return getTelemetry(ENERGY_UPGRADE_COUNT);
    }

    public int getParallelUpgradeCount() {
        return getTelemetry(PARALLEL_UPGRADE_COUNT);
    }

    public int getLuckUpgradeCount() {
        return getTelemetry(LUCK_UPGRADE_COUNT);
    }

    public int getAggregateUpgradeCount() {
        return getTelemetry(AGGREGATE_UPGRADE_COUNT);
    }

    public int getTotalUpgradeCount() {
        return getEfficiencyUpgradeCount()
                + getEnergyUpgradeCount()
                + getParallelUpgradeCount()
                + getLuckUpgradeCount()
                + getAggregateUpgradeCount();
    }

    private int combineWords(int lowIndex, int highIndex) {
        return (telemetry[lowIndex] & 0xFFFF) | ((telemetry[highIndex] & 0xFFFF) << 16);
    }

    private int combineSlotWords(int slot, int lowOffset, int highOffset) {
        if (slot < 0 || slot >= containerSlotCount) {
            return 0;
        }
        int start = TELEMETRY_BASE_COUNT + slot * SLOT_TELEMETRY_STRIDE;
        return combineWords(start + lowOffset, start + highOffset);
    }

    private long combineLongWords(int word0, int word1, int word2, int word3) {
        return (telemetry[word0] & 0xFFFFL)
                | ((telemetry[word1] & 0xFFFFL) << 16)
                | ((telemetry[word2] & 0xFFFFL) << 32)
                | ((telemetry[word3] & 0xFFFFL) << 48);
    }

    private long combineSlotLongWords(
            int slot, int word0Offset, int word1Offset, int word2Offset, int word3Offset) {
        if (slot < 0 || slot >= containerSlotCount) return 0L;
        int start = TELEMETRY_BASE_COUNT + slot * SLOT_TELEMETRY_STRIDE;
        long value = telemetry[start + word0Offset] & 0xFFFFL;
        if (word1Offset >= 0) value |= (telemetry[start + word1Offset] & 0xFFFFL) << 16;
        if (word2Offset >= 0) value |= (telemetry[start + word2Offset] & 0xFFFFL) << 32;
        if (word3Offset >= 0) value |= (telemetry[start + word3Offset] & 0xFFFFL) << 48;
        return value;
    }

    private int getSlotTelemetryValue(int slot, int offset) {
        if (slot < 0 || slot >= containerSlotCount) return 0;
        return telemetry[TELEMETRY_BASE_COUNT + slot * SLOT_TELEMETRY_STRIDE + offset];
    }

    private int firstActiveSlot() {
        for (int slot = 0; slot < containerSlotCount; slot++) {
            if (blockEntity.getSlotProcessingTime(slot) > 0) {
                return slot;
            }
        }
        return -1;
    }

    private int getSlotTelemetryWord(int index) {
        int relativeIndex = index - TELEMETRY_BASE_COUNT;
        if (relativeIndex < 0) {
            return 0;
        }
        int slot = relativeIndex / SLOT_TELEMETRY_STRIDE;
        if (slot >= containerSlotCount) {
            return 0;
        }
        return switch (relativeIndex % SLOT_TELEMETRY_STRIDE) {
            case SLOT_PROGRESS_LOW -> lowWord(blockEntity.getSlotLogicalProgress(slot));
            case SLOT_PROGRESS_HIGH -> highWord(blockEntity.getSlotLogicalProgress(slot));
            case SLOT_PROCESSING_LOW -> lowWord(blockEntity.getSlotProcessingTime(slot));
            case SLOT_PROCESSING_HIGH -> highWord(blockEntity.getSlotProcessingTime(slot));
            case SLOT_PARALLEL_LOW -> lowWord(blockEntity.getSlotDrawParallel(slot));
            case SLOT_PARALLEL_HIGH -> highWord(blockEntity.getSlotDrawParallel(slot));
            case SLOT_ENABLED -> blockEntity.isSlotEnabled(slot) ? 1 : 0;
            case SLOT_EXTERNAL_PARALLEL_LOW ->
                    lowWord(blockEntity.getSlotExternalAccelerationParallelHundredths(slot));
            case SLOT_EXTERNAL_PARALLEL_HIGH ->
                    highWord(blockEntity.getSlotExternalAccelerationParallelHundredths(slot));
            case SLOT_NATURAL_TICKS_LOW ->
                    lowWord(blockEntity.getSlotCurrentNaturalTicks(slot));
            case SLOT_NATURAL_TICKS_HIGH ->
                    highWord(blockEntity.getSlotCurrentNaturalTicks(slot));
            case SLOT_ACTUAL_TICKS_0 ->
                    word(blockEntity.getSlotCurrentExternalAccelerationMachineTicks(slot), 0);
            case SLOT_ACTUAL_TICKS_1 ->
                    word(blockEntity.getSlotCurrentExternalAccelerationMachineTicks(slot), 16);
            case SLOT_ACTUAL_TICKS_2 ->
                    word(blockEntity.getSlotCurrentExternalAccelerationMachineTicks(slot), 32);
            case SLOT_ACTUAL_TICKS_3 ->
                    word(blockEntity.getSlotCurrentExternalAccelerationMachineTicks(slot), 48);
            case SLOT_PREVIOUS_TICKS_0 ->
                    word(blockEntity.getSlotPreviousExternalAccelerationMachineTicks(slot), 0);
            case SLOT_PREVIOUS_TICKS_1 ->
                    word(blockEntity.getSlotPreviousExternalAccelerationMachineTicks(slot), 16);
            case SLOT_PREVIOUS_TICKS_2 ->
                    word(blockEntity.getSlotPreviousExternalAccelerationMachineTicks(slot), 32);
            case SLOT_PREVIOUS_TICKS_3 ->
                    word(blockEntity.getSlotPreviousExternalAccelerationMachineTicks(slot), 48);
            case SLOT_PREVIOUS_PARALLEL_LOW ->
                    lowWord(blockEntity.getSlotPreviousExternalAccelerationParallelHundredths(slot));
            case SLOT_PREVIOUS_PARALLEL_HIGH ->
                    highWord(blockEntity.getSlotPreviousExternalAccelerationParallelHundredths(slot));
            case SLOT_WAITING_FOR_NATURAL_WINDOW ->
                    blockEntity.isSlotWaitingForNaturalWindow(slot) ? 1 : 0;
            case SLOT_EQUIVALENT_0 -> word(blockEntity.getSlotExternalEquivalentAccelerationTicks(slot), 0);
            case SLOT_EQUIVALENT_1 -> word(blockEntity.getSlotExternalEquivalentAccelerationTicks(slot), 16);
            case SLOT_EQUIVALENT_2 -> word(blockEntity.getSlotExternalEquivalentAccelerationTicks(slot), 32);
            case SLOT_EQUIVALENT_3 -> word(blockEntity.getSlotExternalEquivalentAccelerationTicks(slot), 48);
            default -> 0;
        };
    }

    private static int lowWord(int value) {
        return value & 0xFFFF;
    }

    private static int lowWord(long value) {
        return (int) (value & 0xFFFFL);
    }

    private static int highWord(int value) {
        return value >>> 16;
    }

    private static int highWord(long value) {
        return (int) ((value >>> 16) & 0xFFFFL);
    }

    private static int word(long value, int shift) {
        return (int) ((value >>> shift) & 0xFFFFL);
    }

    private static int toHundredths(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value * 100.0D));
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(
                                blockEntity.getBlockPos().getX() + 0.5D,
                                blockEntity.getBlockPos().getY() + 0.5D,
                                blockEntity.getBlockPos().getZ() + 0.5D)
                        <= 64.0D;
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < containerSlotCount) {
            if (!moveItemStackTo(stack, containerSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!stack.is(ModItems.STRUCT_MARKER.get())
                    || !moveItemStackTo(stack, 0, containerSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
