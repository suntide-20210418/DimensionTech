package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity.OutputState;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;

public enum MythicMinerJadeProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocationHelper.modLoc("mythic_miner_status");
    private static final String STATUS = "Status";
    private static final String SLOTS = "Slots";
    private static final String SLOT_INDEX = "Index";
    private static final String SLOT_STRUCTURE = "Structure";
    private static final String SLOT_PROGRESS = "Progress";
    private static final String SLOT_PROCESSING = "Processing";
    private static final String SLOT_PARALLEL = "Parallel";
    private static final String SLOT_EXTERNAL_PARALLEL = "ExternalParallelHundredths";
    private static final String SLOT_EXTERNAL_EQUIVALENT = "ExternalEquivalentAccelerationTicks";
    private static final String SLOT_ACTUAL_TICKS = "ActualTicks";
    private static final String SLOT_PREVIOUS_TICKS = "PreviousTicks";
    private static final String SLOT_PREVIOUS_PARALLEL = "PreviousParallelHundredths";
    private static final String SLOT_WAITING = "WaitingForNaturalWindow";
    private static final String SLOT_NATURAL_TICKS = "NaturalTicks";
    private static final String OUTPUT = "Output";
    private static final String PENDING_ITEMS = "PendingItems";
    private static final String ENERGY_CONSUMPTION = "EnergyConsumption";
    private static final String BASE_EFFICIENCY = "BaseEfficiency";
    private static final String BASE_CAPACITY = "BaseCapacity";
    private static final String BASE_CONSUMPTION = "BaseConsumption";
    private static final String BASE_PARALLEL = "BaseParallel";
    private static final String BASE_LUCK = "BaseLuck";
    private static final String SLOT_COUNT = "SlotCount";
    private static final String WORKING_COUNT = "WorkingCount";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BaseMinerBlockEntity miner)) {
            return;
        }

        boolean running =
                miner.isStructureComplete()
                        && miner.getEnergyStorage().getEnergyStored()
                                >= miner.getEffectiveEnergyConsumption();
        data.putString(STATUS, miner.isOutputBlocked() ? "blocked" : running ? "running" : "idle");
        ListTag slotTags = new ListTag();
        int workingCount = 0;
        for (int slot = 0; slot < miner.getItemHandler().getSlots(); slot++) {
            var markerInfo =
                    StructMarkerItem.getMarkerInfo(miner.getItemHandler().getStackInSlot(slot));
            if (markerInfo.isEmpty()) {
                continue;
            }
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt(SLOT_INDEX, slot + 1);
            slotTag.putString(SLOT_STRUCTURE, markerInfo.get().structure().id().toString());
            // Jade shows logical server ticks, not accelerated execution calls.
            slotTag.putLong(SLOT_PROGRESS, miner.getSlotLogicalProgress(slot));
            slotTag.putInt(SLOT_PROCESSING, miner.getSlotProcessingTime(slot));
            slotTag.putInt(SLOT_PARALLEL, miner.getSlotDrawParallel(slot));
            slotTag.putBoolean(SLOT_WAITING, miner.isSlotWaitingForNaturalWindow(slot));
            slotTag.putLong(SLOT_NATURAL_TICKS, miner.getSlotCurrentNaturalTicks(slot));
            slotTag.putInt(
                    SLOT_EXTERNAL_PARALLEL,
                    miner.getSlotExternalAccelerationParallelHundredths(slot));
            slotTag.putLong(
                    SLOT_EXTERNAL_EQUIVALENT,
                    miner.getSlotExternalEquivalentAccelerationTicks(slot));
            slotTag.putLong(
                    SLOT_ACTUAL_TICKS, miner.getSlotCurrentExternalAccelerationMachineTicks(slot));
            slotTag.putLong(
                    SLOT_PREVIOUS_TICKS,
                    miner.getSlotPreviousExternalAccelerationMachineTicks(slot));
            slotTag.putInt(
                    SLOT_PREVIOUS_PARALLEL,
                    miner.getSlotPreviousExternalAccelerationParallelHundredths(slot));
            slotTags.add(slotTag);
            if (miner.getSlotProcessingTime(slot) > 0) {
                workingCount++;
            }
        }
        data.put(SLOTS, slotTags);
        data.putInt(SLOT_COUNT, miner.getItemHandler().getSlots());
        data.putInt(WORKING_COUNT, workingCount);
        data.putString(OUTPUT, miner.getOutputState().name().toLowerCase(Locale.ROOT));
        data.putInt(PENDING_ITEMS, miner.getPendingOutputCount());
        data.putInt(ENERGY_CONSUMPTION, miner.getEffectiveEnergyConsumption());
        data.putDouble(BASE_EFFICIENCY, miner.getBaseMachineEfficiency());
        data.putInt(BASE_CAPACITY, miner.getBaseEnergyCapacity());
        data.putInt(BASE_CONSUMPTION, miner.getEnergyConsumption());
        data.putInt(BASE_PARALLEL, miner.getBaseParallelCount());
        data.putFloat(BASE_LUCK, miner.getBaseMachineLuck());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(STATUS, Tag.TAG_STRING)) {
            return;
        }

        String status = data.getString(STATUS);
        IThemeHelper theme = IThemeHelper.get();
        tooltip.add(line("status", themedStatus(theme, status)));
        tooltip.add(Component.empty());
        tooltip.add(
                line(
                        "base_parameters",
                        theme.info(Component.translatable("jade.dimension_tech.parameters"))));
        tooltip.add(
                line(
                        "efficiency",
                        Component.literal(formatDecimal(data.getDouble(BASE_EFFICIENCY)))));
        tooltip.add(line("capacity", Component.literal(data.getInt(BASE_CAPACITY) + " FE")));
        tooltip.add(
                line(
                        "base_consumption",
                        Component.literal(data.getInt(BASE_CONSUMPTION) + " FE/t")));
        tooltip.add(
                line(
                        "base_parallel",
                        Component.literal(Integer.toString(data.getInt(BASE_PARALLEL)))));
        tooltip.add(line("luck", Component.literal(formatDecimal(data.getFloat(BASE_LUCK)))));
        tooltip.add(
                Component.translatable(
                        "jade.dimension_tech.slot_usage",
                        data.getInt(WORKING_COUNT),
                        data.getInt(SLOT_COUNT)));
        ListTag slots = data.getList(SLOTS, Tag.TAG_COMPOUND);
        for (Tag tag : slots) {
            CompoundTag slot = (CompoundTag) tag;
            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable(
                            "jade.dimension_tech.slot",
                            slot.getInt(SLOT_INDEX),
                            slotStructureName(slot)));
            int processing = Math.max(0, slot.getInt(SLOT_PROCESSING));
            boolean waiting = slot.getBoolean(SLOT_WAITING);
            if (waiting) processing = BaseMinerBlockEntity.DEFAULT_PROCESSING_TIME;
            int progress =
                    (int) Math.max(0L, Math.min((long) processing, slot.getLong(SLOT_PROGRESS)));
            addProgressBar(tooltip, progress, processing, theme);
            tooltip.add(
                    line(
                            "slot_progress",
                            theme.info(
                                    Component.translatable(
                                            "jade.dimension_tech.slot_progress_value",
                                            progress,
                                            processing))));
            if (waiting) {
                tooltip.add(
                        line(
                                "waiting_for_natural_window",
                                theme.info(
                                        Component.translatable(
                                                "jade.dimension_tech.waiting_for_natural_window"))));
            }
            tooltip.add(
                    line(
                            "slot_parallel",
                            theme.info(
                                    Component.literal(
                                            Integer.toString(slot.getInt(SLOT_PARALLEL))))));
            boolean externalActive =
                    slot.getLong(SLOT_ACTUAL_TICKS) != slot.getLong(SLOT_NATURAL_TICKS);
            if (externalActive) {
                tooltip.add(
                        line(
                                "external_parallel",
                                theme.info(
                                        Component.literal(
                                                formatDecimal(
                                                        slot.getInt(SLOT_EXTERNAL_PARALLEL)
                                                                / 100.0D)))));
                tooltip.add(
                        line(
                                "external_equivalent_acceleration",
                                theme.info(
                                        Component.literal(
                                                formatDecimal(
                                                        currentCycleEquivalentAcceleration(
                                                                slot))))));
                tooltip.add(
                        line(
                                "actual_ticks",
                                theme.info(
                                        Component.literal(
                                                Long.toString(slot.getLong(SLOT_ACTUAL_TICKS))))));
                tooltip.add(
                        line(
                                "previous_ticks",
                                theme.info(
                                        Component.literal(
                                                Long.toString(
                                                        slot.getLong(SLOT_PREVIOUS_TICKS))))));
            }
            tooltip.add(
                    line(
                            "previous_parallel",
                            theme.info(
                                    Component.literal(
                                            formatDecimal(
                                                    slot.getInt(SLOT_PREVIOUS_PARALLEL)
                                                            / 100.0D)))));
        }
        if ("blocked".equals(status)) {
            tooltip.add(Component.empty());
            tooltip.add(
                    line(
                            "pending",
                            theme.danger(
                                    Component.translatable(
                                            "jade.dimension_tech.items",
                                            data.getInt(PENDING_ITEMS)))));
        }

        tooltip.add(line("output", outputName(data.getString(OUTPUT))));
        tooltip.add(
                line(
                        "consumption",
                        Component.translatable(
                                "jade.dimension_tech.energy_consumption_value",
                                data.getInt(ENERGY_CONSUMPTION))));
        if ("blocked".equals(status)) {
            tooltip.add(line("reason", theme.danger(blockedReason(data.getString(OUTPUT)))));
        }
    }

    private static double currentCycleEquivalentAcceleration(CompoundTag slot) {
        long naturalTicks = slot.getLong(SLOT_NATURAL_TICKS);
        return naturalTicks <= 0L ? 0.0D : slot.getLong(SLOT_ACTUAL_TICKS) / (double) naturalTicks;
    }

    private static Component themedStatus(IThemeHelper theme, String status) {
        Component value = Component.translatable("jade.dimension_tech.status." + status);
        return switch (status) {
            case "running" -> theme.success(value);
            case "blocked" -> theme.danger(value);
            default -> theme.info(value);
        };
    }

    private static void addProgressBar(
            ITooltip tooltip, int progressTicks, int processingTicks, IThemeHelper theme) {
        float progress =
                processingTicks <= 0
                        ? 0.0F
                        : Math.max(0.0F, Math.min(1.0F, (float) progressTicks / processingTicks));
        IElementHelper elements = tooltip.getElementHelper();
        IProgressStyle style = elements.progressStyle().color(0xFF22D3EE, 0xFF164E63);
        tooltip.add(
                elements.progress(
                                progress,
                                Component.translatable(
                                        "jade.dimension_tech.slot_progress_percent",
                                        Math.round(progress * 100.0F)),
                                style,
                                BoxStyle.DEFAULT,
                                false)
                        .tag(UID));
    }

    private static Component slotStructureName(CompoundTag slot) {
        return structureNames(singleStructure(slot.getString(SLOT_STRUCTURE)));
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static ListTag singleStructure(String structure) {
        ListTag result = new ListTag();
        result.add(StringTag.valueOf(structure));
        return result;
    }

    private static Component line(String key, Component value) {
        return Component.translatable("jade.dimension_tech." + key, value);
    }

    private static Component structureNames(ListTag structures) {
        if (structures.isEmpty()) {
            return Component.translatable("jade.dimension_tech.structure.none");
        }
        List<Component> names = new ArrayList<>();
        for (Tag tag : structures) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id == null) {
                names.add(Component.literal(tag.getAsString()));
                continue;
            }
            names.add(TranslateHelper.structureName(id));
        }
        Component result = Component.empty();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                result = result.copy().append(", ");
            }
            result = result.copy().append(names.get(index));
        }
        return result;
    }

    private static Component outputName(String output) {
        return Component.translatable("jade.dimension_tech.output." + output);
    }

    private static Component blockedReason(String output) {
        String reason =
                OutputState.ME_NETWORK.name().toLowerCase(Locale.ROOT).equals(output)
                        ? "me_full"
                        : OutputState.ITEM_HANDLER.name().toLowerCase(Locale.ROOT).equals(output)
                                ? "inventory_full"
                                : "no_target";
        return Component.translatable("jade.dimension_tech.reason." + reason);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
