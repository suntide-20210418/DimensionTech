package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.ModMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public final class MythicCrucibleMenu extends AbstractContainerMenu {
    private final MythicCrucibleBlockEntity crucible;
    private final ContainerData data;
    public MythicCrucibleMenu(int id, Inventory player, FriendlyByteBuf data) { this(id, player, get(player, data.readBlockPos())); }
    public MythicCrucibleMenu(int id, Inventory player, MythicCrucibleBlockEntity crucible) {
        super(ModMenu.MYTHIC_CRUCIBLE.get(), id); this.crucible = crucible;
        addSlot(new SlotItemHandler(crucible.inventory(), MythicCrucibleBlockEntity.FRAGMENT_SLOT, 54, 35));
        addSlot(new SlotItemHandler(crucible.inventory(), MythicCrucibleBlockEntity.OPERATION_SLOT, 108, 35));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(player, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(player, column, 8 + column * 18, 142));
        data = crucible.data();
        addDataSlots(data);
    }
    private static MythicCrucibleBlockEntity get(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (!(entity instanceof MythicCrucibleBlockEntity crucible)) throw new IllegalStateException("Missing mythic crucible at " + pos);
        return crucible;
    }
    public MythicCrucibleBlockEntity crucible() { return crucible; }
    public int status() { return data.get(0); }
    public int stateTicks() { return data.get(1); }
    public int stateIndex() { return data.get(2); }
    public int inputAmount() { return data.get(3); }
    public int outputAmount() { return data.get(4); }
    public int currentState() { return data.get(5); }
    @Override public boolean stillValid(Player player) { return player.level().getBlockEntity(crucible.getBlockPos()) == crucible && player.distanceToSqr(crucible.getBlockPos().getX() + .5, crucible.getBlockPos().getY() + .5, crucible.getBlockPos().getZ() + .5) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < 2) { if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); return original;
    }
}
