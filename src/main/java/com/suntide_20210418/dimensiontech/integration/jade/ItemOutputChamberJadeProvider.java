package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.ItemOutputChamberBlockEntity;
import com.suntide_20210418.dimensiontech.integration.MachineGridNode;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * Hover readout for the item output chamber: whether its ME device is live, and how much finished
 * loot is still stacked up waiting to leave.
 *
 * <p>The two rows are the two ways this chamber can be doing nothing useful. An offline device with
 * an empty queue is fine; an offline device with a non-empty queue is a stopped miner, and the
 * count says by how much. The queue itself stays on the controller (it has to survive with the
 * machine and drop when it breaks), so the count is read through the chamber's binding rather than
 * owned here.
 */
public enum ItemOutputChamberJadeProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocationHelper.modLoc("item_output_chamber");
    private static final String BOUND = "Bound";
    private static final String DEVICE_PRESENT = "DevicePresent";
    private static final String DEVICE_ONLINE = "DeviceOnline";
    private static final String PENDING_ITEMS = "PendingItems";
    private static final String OUTPUT_BLOCKED = "OutputBlocked";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ItemOutputChamberBlockEntity chamber)) {
            return;
        }
        MachineGridNode node = chamber.gridNode();
        data.putBoolean(DEVICE_PRESENT, node != null);
        data.putBoolean(DEVICE_ONLINE, node != null && node.isOnline());
        BaseMinerBlockEntity miner = chamber.miner();
        data.putBoolean(BOUND, miner != null);
        data.putInt(PENDING_ITEMS, miner == null ? 0 : miner.getPendingOutputCount());
        data.putBoolean(OUTPUT_BLOCKED, miner != null && miner.isOutputBlocked());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BOUND, Tag.TAG_BYTE)) {
            return;
        }
        IThemeHelper theme = IThemeHelper.get();
        if (!data.getBoolean(BOUND)) {
            tooltip.add(Component.translatable("jade.dimension_tech.unbound"));
        }
        tooltip.add(JadeText.line("device", deviceValue(theme, data)));
        tooltip.add(JadeText.line("backlog", backlogValue(theme, data)));
    }

    /** "AE2 not installed" is a different fact from "offline": only the latter is a fault. */
    private static Component deviceValue(IThemeHelper theme, CompoundTag data) {
        if (!data.getBoolean(DEVICE_PRESENT)) {
            return theme.info(Component.translatable("jade.dimension_tech.device_absent"));
        }
        return data.getBoolean(DEVICE_ONLINE)
                ? theme.success(Component.translatable("jade.dimension_tech.device_online"))
                : theme.danger(Component.translatable("jade.dimension_tech.device_offline"));
    }

    /**
     * A non-empty queue that has stopped the machine is the state worth flagging; a non-empty queue
     * that is still draining normally is not, so the two read differently.
     */
    private static Component backlogValue(IThemeHelper theme, CompoundTag data) {
        int pending = data.getInt(PENDING_ITEMS);
        if (pending <= 0) {
            return theme.info(Component.translatable("jade.dimension_tech.backlog_clear"));
        }
        return data.getBoolean(OUTPUT_BLOCKED)
                ? theme.danger(
                        Component.translatable("jade.dimension_tech.backlog_blocked", pending))
                : Component.translatable("jade.dimension_tech.backlog_waiting", pending);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
