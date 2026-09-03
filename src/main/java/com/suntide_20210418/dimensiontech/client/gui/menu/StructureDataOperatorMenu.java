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

public final class StructureDataOperatorMenu extends AbstractContainerMenu {
    public static final int SOURCE_MARKER_MENU_SLOT = StructureDataOperatorBlockEntity.INVENTORY_SIZE;
    public static final int PLAYER_SLOT_START = SOURCE_MARKER_MENU_SLOT + 1;
    public static final int COMPACT_PLAYER_SLOT_START = PLAYER_SLOT_START + 36;
    private final StructureDataOperatorBlockEntity blockEntity;
    private boolean operationPage = true;
    public StructureDataOperatorMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, (StructureDataOperatorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())); }
    public StructureDataOperatorMenu(int id, Inventory inv, StructureDataOperatorBlockEntity be) {
        super(ModMenu.STRUCTURE_DATA_OPERATOR.get(), id); blockEntity = be;
        addSlot(new OperatorSlot(be.inventory(), StructureDataOperatorBlockEntity.TARGET, 80, 52, () -> operationPage));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new OperatorSlot(be.inventory(), StructureDataOperatorBlockEntity.OPERAND_START + row * 9 + column, 8 + column * 18, 116 + row * 18, () -> operationPage));
        addSlot(new OperatorSlot(be.inventory(), StructureDataOperatorBlockEntity.INTEGRATOR, -26, 50, () -> true));
        addSlot(new OperatorSlot(be.inventory(), StructureDataOperatorBlockEntity.INTERPRETER, -26, 78, () -> true));
        addSlot(new OperatorSlot(be.inventory(), StructureDataOperatorBlockEntity.TARGET, 28, 58, () -> !operationPage));
        addPlayerInventory(inv);
    }

    public void setOperationPage(boolean operationPage) {
        this.operationPage = operationPage;
    }
    private void addPlayerInventory(Inventory inv) {
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new PlayerSlot(inv, col+row*9+9, 79+col*18, 258+row*18, () -> !operationPage));
        for (int col=0;col<9;col++) addSlot(new PlayerSlot(inv,col,79+col*18,316, () -> !operationPage));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new PlayerSlot(inv, col+row*9+9, 8+col*18, 198+row*18, () -> operationPage));
        for (int col=0;col<9;col++) addSlot(new PlayerSlot(inv,col,8+col*18,256, () -> operationPage));
    }
    public StructureDataOperatorBlockEntity blockEntity() { return blockEntity; }
    public boolean stillValid(Player p) { return p.distanceToSqr(blockEntity.getBlockPos().getX()+.5, blockEntity.getBlockPos().getY()+.5, blockEntity.getBlockPos().getZ()+.5) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY; Slot slot = slots.get(index); if (!slot.hasItem()) return empty; ItemStack stack = slot.getItem().copy();
        if (index < PLAYER_SLOT_START) { if (!moveItemStackTo(stack,PLAYER_SLOT_START,slots.size(),true)) return empty; } else if (!moveItemStackTo(stack,0,StructureDataOperatorBlockEntity.INVENTORY_SIZE,false)) return empty;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setByPlayer(stack); return stack;
    }

    private static final class OperatorSlot extends SlotItemHandler {
        private final java.util.function.BooleanSupplier active;

        private OperatorSlot(net.minecraftforge.items.IItemHandler handler, int index, int x, int y, java.util.function.BooleanSupplier active) {
            super(handler, index, x, y);
            this.active = active;
        }

        @Override public boolean isActive() { return active.getAsBoolean(); }
    }

    private static final class PlayerSlot extends Slot {
        private final java.util.function.BooleanSupplier active;

        private PlayerSlot(Inventory inventory, int index, int x, int y, java.util.function.BooleanSupplier active) {
            super(inventory, index, x, y);
            this.active = active;
        }

        @Override public boolean isActive() { return active.getAsBoolean(); }
    }
}
