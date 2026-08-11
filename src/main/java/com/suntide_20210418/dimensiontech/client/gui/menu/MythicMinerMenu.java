package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.MythicMinerBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
    public static final int SLOT_COLUMNS = 9;
    public static final int CONTAINER_SLOT_Y = 32;
    public static final int BASE_PLAYER_INVENTORY_Y = 84;

    private final MythicMinerBlockEntity blockEntity;
    private final int containerSlotCount;
    private final int containerRows;
    private final int playerInventoryY;

    public MythicMinerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, data.readBlockPos()));
    }

    public MythicMinerMenu(
            int containerId,
            Inventory playerInventory,
            MythicMinerBlockEntity blockEntity) {
        super(ModMenu.MYTHIC_MINER.get(), containerId);
        this.blockEntity = blockEntity;
        IItemHandler itemHandler = blockEntity.getItemHandler();
        this.containerSlotCount = itemHandler.getSlots();
        this.containerRows = (containerSlotCount + SLOT_COLUMNS - 1) / SLOT_COLUMNS;
        this.playerInventoryY = BASE_PLAYER_INVENTORY_Y + (containerRows - 1) * 18;

        addContainerSlots(itemHandler);
        addPlayerInventory(playerInventory);
    }

    private static MythicMinerBlockEntity getBlockEntity(
            Inventory playerInventory, BlockPos position) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(position);
        if (blockEntity instanceof MythicMinerBlockEntity mythicMinerBlockEntity) {
            return mythicMinerBlockEntity;
        }
        throw new IllegalStateException("Mythic miner block entity is missing at " + position);
    }

    private void addContainerSlots(IItemHandler itemHandler) {
        int lastRowSlots = containerSlotCount % SLOT_COLUMNS;
        for (int slot = 0; slot < containerSlotCount; slot++) {
            int row = slot / SLOT_COLUMNS;
            int column = slot % SLOT_COLUMNS;
            int slotsInRow =
                    row == containerRows - 1 && lastRowSlots != 0
                            ? lastRowSlots
                            : SLOT_COLUMNS;
            int rowStartX = 8 + (SLOT_COLUMNS - slotsInRow) * 9;
            addSlot(
                    new SlotItemHandler(
                            itemHandler,
                            slot,
                            rowStartX + column * 18,
                            CONTAINER_SLOT_Y + row * 18));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column + row * 9 + 9,
                                8 + column * 18,
                                playerInventoryY + row * 18));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY));
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

    public MythicMinerBlockEntity getBlockEntity() {
        return blockEntity;
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
