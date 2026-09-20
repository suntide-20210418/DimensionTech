package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Container for the Structure Data Operator.
 *
 * <p><b>Single slot layout.</b> The previous revision carried two coordinate sets — one for the
 * compact operate window and one for the wide catalogue window — and switched between them by
 * tagging every slot with a page predicate. The authored texture has one face with one set of
 * recesses, so that mechanism is gone: every slot lives at exactly one place.
 *
 * <p>Slot order matches {@link StructureDataOperatorBlockEntity}: {@code 0} the read slot,
 * {@code 1..36} the 9x4 write array in row-major order, {@code 37} the Data Integrator,
 * {@code 38} the Structure Interpreter, then the player inventory and hotbar.
 */
public final class StructureDataOperatorMenu extends AbstractContainerMenu {
    public static final int PLAYER_SLOT_START = StructureDataOperatorBlockEntity.INVENTORY_SIZE;

    private final StructureDataOperatorBlockEntity blockEntity;

    public StructureDataOperatorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf));
    }

    public StructureDataOperatorMenu(
            int id, Inventory inv, StructureDataOperatorBlockEntity blockEntity) {
        super(ModMenu.STRUCTURE_DATA_OPERATOR.get(), id);
        this.blockEntity = blockEntity;
        var handler = blockEntity.inventory();
        addSlot(
                new SlotItemHandler(
                        handler,
                        StructureDataOperatorBlockEntity.TARGET,
                        StructureDataOperatorLayout.TARGET_SLOT.x(),
                        StructureDataOperatorLayout.TARGET_SLOT.y()));
        for (int index = 0; index < StructureDataOperatorBlockEntity.OPERAND_COUNT; index++) {
            addSlot(
                    new SlotItemHandler(
                            handler,
                            StructureDataOperatorBlockEntity.OPERAND_START + index,
                            StructureDataOperatorLayout.operandX(
                                    StructureDataOperatorLayout.operandColumn(index)),
                            StructureDataOperatorLayout.operandY(
                                    StructureDataOperatorLayout.operandRow(index))));
        }
        addSlot(
                new SlotItemHandler(
                        handler,
                        StructureDataOperatorBlockEntity.INTEGRATOR,
                        StructureDataOperatorLayout.INTEGRATOR_SLOT.x(),
                        StructureDataOperatorLayout.INTEGRATOR_SLOT.y()));
        addSlot(
                new SlotItemHandler(
                        handler,
                        StructureDataOperatorBlockEntity.INTERPRETER,
                        StructureDataOperatorLayout.INTERPRETER_SLOT.x(),
                        StructureDataOperatorLayout.INTERPRETER_SLOT.y()));
        addPlayerInventory(inv);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < StructureDataOperatorLayout.PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < StructureDataOperatorLayout.PLAYER_INVENTORY_COLUMNS;
                    column++) {
                addSlot(
                        new Slot(
                                inv,
                                column + row * 9 + 9,
                                StructureDataOperatorLayout.playerSlotX(column),
                                StructureDataOperatorLayout.playerSlotY(row)));
            }
        }
        for (int column = 0; column < StructureDataOperatorLayout.PLAYER_INVENTORY_COLUMNS;
                column++) {
            addSlot(
                    new Slot(
                            inv,
                            column,
                            StructureDataOperatorLayout.playerSlotX(column),
                            StructureDataOperatorLayout.hotbarSlotY()));
        }
    }

    public StructureDataOperatorBlockEntity blockEntity() {
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_SLOT_START) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoMachine(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * Routes a stack from the player inventory into the machine. Markers fall back to the write
     * array only after the read slot is taken, because the read slot is what makes the machine do
     * anything — filling it first matches the order of use.
     */
    private boolean moveIntoMachine(ItemStack stack) {
        if (stack.is(ModItems.DATA_INTEGRATOR.get())) {
            return moveItemStackTo(
                    stack,
                    StructureDataOperatorBlockEntity.INTEGRATOR,
                    StructureDataOperatorBlockEntity.INTEGRATOR + 1,
                    false);
        }
        if (stack.is(ModItems.STRUCTURE_INTERPRETER.get())) {
            return moveItemStackTo(
                    stack,
                    StructureDataOperatorBlockEntity.INTERPRETER,
                    StructureDataOperatorBlockEntity.INTERPRETER + 1,
                    false);
        }
        if (stack.is(ModItems.STRUCTURE_MARKER.get())) {
            if (moveItemStackTo(
                    stack,
                    StructureDataOperatorBlockEntity.TARGET,
                    StructureDataOperatorBlockEntity.TARGET + 1,
                    false)) {
                return true;
            }
            return moveItemStackTo(
                    stack,
                    StructureDataOperatorBlockEntity.OPERAND_START,
                    StructureDataOperatorBlockEntity.OPERAND_START
                            + StructureDataOperatorBlockEntity.OPERAND_COUNT,
                    false);
        }
        return false;
    }

    private static StructureDataOperatorBlockEntity resolve(Inventory inv, FriendlyByteBuf buf) {
        var position = buf.readBlockPos();
        var blockEntity = inv.player.level().getBlockEntity(position);
        if (blockEntity instanceof StructureDataOperatorBlockEntity operator) return operator;
        throw new IllegalStateException(
                "Structure data operator menu opened without its block entity at " + position);
    }
}
