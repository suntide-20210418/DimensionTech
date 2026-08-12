package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity.OutputState;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
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
    private static final String STRUCTURES = "Structures";
    private static final String PROGRESS = "Progress";
    private static final String REMAINING_TICKS = "RemainingTicks";
    private static final String PARALLEL = "Parallel";
    private static final String OUTPUT = "Output";
    private static final String PENDING_ITEMS = "PendingItems";
    private static final String ENERGY_CONSUMPTION = "EnergyConsumption";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BaseMinerBlockEntity miner)) {
            return;
        }

        List<ResourceLocation> structures = miner.getMarkedStructures();
        boolean running =
                !structures.isEmpty()
                        && miner.getEnergyStorage().getEnergyStored()
                                >= miner.getEnergyConsumption();
        data.putString(
                STATUS, miner.isOutputBlocked() ? "blocked" : running ? "running" : "idle");
        ListTag structureTags = new ListTag();
        structures.forEach(structure -> structureTags.add(StringTag.valueOf(structure.toString())));
        data.put(STRUCTURES, structureTags);
        data.putInt(PROGRESS, miner.getProgressPercent());
        data.putInt(
                REMAINING_TICKS,
                Math.max(0, miner.getProcessingTime() - miner.getProgress()));
        data.putInt(PARALLEL, miner.getDrawParallel());
        data.putString(OUTPUT, miner.getOutputState().name().toLowerCase(Locale.ROOT));
        data.putInt(PENDING_ITEMS, miner.getPendingOutputCount());
        data.putInt(ENERGY_CONSUMPTION, miner.getEnergyConsumption());
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
        tooltip.add(line("structure", structureNames(data.getList(STRUCTURES, Tag.TAG_STRING))));

        if ("running".equals(status)) {
            tooltip.add(Component.empty());
            addProgressBar(tooltip, data.getInt(PROGRESS), theme);
            tooltip.add(
                    line(
                            "remaining",
                            theme.info(
                                    Component.translatable(
                                            "jade.dimension_tech.seconds",
                                            String.format(
                                                    Locale.ROOT,
                                                    "%.1f",
                                                    data.getInt(REMAINING_TICKS) / 20.0D)))));
        } else if ("blocked".equals(status)) {
            tooltip.add(Component.empty());
            tooltip.add(
                    line(
                            "pending",
                            theme.danger(
                                    Component.translatable(
                                            "jade.dimension_tech.items",
                                            data.getInt(PENDING_ITEMS)))));
        }

        tooltip.add(Component.empty());
        tooltip.add(line("parallel", Component.literal(Integer.toString(data.getInt(PARALLEL)))));
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

    private static Component themedStatus(IThemeHelper theme, String status) {
        Component value = Component.translatable("jade.dimension_tech.status." + status);
        return switch (status) {
            case "running" -> theme.success(value);
            case "blocked" -> theme.danger(value);
            default -> theme.info(value);
        };
    }

    private static void addProgressBar(ITooltip tooltip, int percent, IThemeHelper theme) {
        float progress = Math.max(0.0F, Math.min(1.0F, percent / 100.0F));
        IElementHelper elements = tooltip.getElementHelper();
        IProgressStyle style =
                elements
                        .progressStyle()
                        .color(theme.theme().successColor, theme.theme().infoColor);
        tooltip.add(
                elements
                        .progress(
                                progress,
                                Component.translatable(
                                        "jade.dimension_tech.progress_value", percent),
                                style,
                                BoxStyle.DEFAULT,
                                false)
                        .tag(UID));
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
            String translationKey =
                    "jade.dimension_tech.structure." + id.getNamespace() + "." + id.getPath();
            names.add(Component.translatableWithFallback(translationKey, readablePath(id.getPath())));
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

    private static String readablePath(String path) {
        String[] words = path.replace('/', '_').split("_");
        return java.util.Arrays.stream(words)
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
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
