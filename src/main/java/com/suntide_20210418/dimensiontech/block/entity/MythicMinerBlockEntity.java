package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.custom.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.LootTableLottery;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public abstract class MythicMinerBlockEntity extends BlockEntity implements MenuProvider {
    private static final String INVENTORY_TAG = "Inventory";

    private final ItemStackHandler itemHandler;
    private LazyOptional<IItemHandler> itemHandlerCapability;

    protected MythicMinerBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
        this.itemHandler = createItemHandler();
        this.itemHandlerCapability = LazyOptional.of(() -> itemHandler);
    }

    protected abstract int getSlotCount();

    protected abstract String getTranslationName();

    protected abstract float getDrawLuck();

    protected abstract int getDrawParallel();

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public void drawMarkerLoot(MinecraftServer server) {
        if (!(level instanceof ServerLevel outputLevel)) {
            return;
        }

        List<IItemHandler> outputHandlers = findAdjacentItemHandlers(outputLevel);
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack marker = itemHandler.getStackInSlot(slot);
            if (!marker.is(ModItems.STRUCT_MARKER.get())) {
                continue;
            }

            StructMarkerItem.getMarkerInfo(marker).ifPresent(markerInfo -> {
                ResourceKey<Level> dimensionKey =
                        ResourceKey.create(Registries.DIMENSION, markerInfo.dimension());
                ServerLevel lootLevel = server.getLevel(dimensionKey);
                if (lootLevel == null) {
                    return;
                }

                Vec3 origin = Vec3.atCenterOf(markerInfo.position());
                for (StructureLootAnalyzer.StructureLoot structureLoot :
                        StructureLootAnalyzer.analyze(server, markerInfo)) {
                    List<ItemStack> loot =
                            LootTableLottery.draw(
                                    lootLevel,
                                    origin,
                                    structureLoot.lootTables(),
                                    null,
                                    getDrawLuck(),
                                    getDrawParallel());
                    loot.forEach(stack -> outputLoot(outputLevel, outputHandlers, stack));
                }
            });
        }
    }

    private List<IItemHandler> findAdjacentItemHandlers(ServerLevel outputLevel) {
        List<IItemHandler> handlers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity adjacent = outputLevel.getBlockEntity(worldPosition.relative(direction));
            if (adjacent == null) {
                continue;
            }
            adjacent.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .resolve()
                    .ifPresent(handlers::add);
        }
        return handlers;
    }

    private void outputLoot(
            ServerLevel outputLevel, List<IItemHandler> outputHandlers, ItemStack stack) {
        ItemStack remainder = stack;
        for (IItemHandler outputHandler : outputHandlers) {
            remainder = ItemHandlerHelper.insertItemStacked(outputHandler, remainder, false);
            if (remainder.isEmpty()) {
                return;
            }
        }
        Block.popResource(outputLevel, worldPosition, remainder);
    }

    private ItemStackHandler createItemHandler() {
        int slotCount = getSlotCount();
        if (slotCount <= 0) {
            throw new IllegalStateException("Mythic miner slot count must be positive");
        }

        return new ItemStackHandler(slotCount) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.is(ModItems.STRUCT_MARKER.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound(INVENTORY_TAG);
            inventoryTag.putInt("Size", itemHandler.getSlots());
            itemHandler.deserializeNBT(inventoryTag);
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandlerCapability = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public Component getDisplayName() {
        return TranslateHelper.translate(TranslateHelper.container(getTranslationName()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MythicMinerMenu(containerId, playerInventory, this);
    }
}
