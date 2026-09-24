package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 宝箱分析器：按绑定按键（默认 V）分析准星指向的宝箱；若目标是带 LootTable 引用的容器，直接以该 宝箱的 LootTable
 * 作为战利品来源做期望分析并标记它。右键（use）只打开分析界面查看已标记内容。
 *
 * <p>它写入的 {@code StructureMarkerData} 与 {@link StructMarkerItem} 同构（Dimension + Position +
 * Structure{Id, Bounds}），只是 Structure.Id 固定为虚拟 id {@link #CHEST_MARKER_ID}，并把宝箱的 LootTable 存进
 * {@code ChestData} 子标签。因为下游只认 NBT 结构不认物品类型，结构数据分析仪的 复制模式和结构采掘器的生产链路都能直接消费宝箱标记；唯一的分支点是分析：宝箱没有注册结构、
 * 没有模板，必须用 {@code discoverFixedForValue} 按记录在 NBT 里的 LootTable 构造 profile，而 不能走按结构 id 的 {@code
 * StructureAnalysisService.discover}。
 */
public class ChestMarkerItem extends Item {
    /** 虚拟结构 id：不进注册表，只作为宝箱标记在 StructureMarkerData 里的占位标识。 */
    public static final ResourceLocation CHEST_MARKER_ID =
            ResourceLocationHelper.item("chest_marker");

    public ChestMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level instanceof ServerLevel serverLevel) {
            refreshAnalysisIfNeeded(serverLevel, itemStack);
            openScreen((net.minecraft.server.level.ServerPlayer) player, itemStack, usedHand);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /**
     * 读取指定位置方块的 LootTable 引用，非容器时返回 null。
     *
     * <p>分析动作由绑定按键（默认 V）触发：客户端把准星指向的方块位置发给服务端，服务端在这里 验证并标记。不用右键方块交互，因为右键一个宝箱会优先打开它而不是分析它。
     */
    public static ResourceLocation lootTableAt(ServerLevel level, BlockPos position) {
        return containerLootTable(level.getBlockEntity(position), level.registryAccess());
    }

    private static void openScreen(
            net.minecraft.server.level.ServerPlayer player,
            ItemStack itemStack,
            InteractionHand hand) {
        com.suntide_20210418.dimensiontech.network.ModNetwork.openRefreshedMarker(
                player, itemStack, hand);
    }

    /** 读取标记的宝箱信息；未标记或数据损坏时为空。 */
    public static Optional<ChestInfo> getChestInfo(ItemStack itemStack) {
        return StructMarkerItem.getMarkerData(itemStack)
                .flatMap(
                        data ->
                                data.chestData()
                                        .map(
                                                chest ->
                                                        new ChestInfo(
                                                                data.position(),
                                                                chest.lootTable(),
                                                                chest.lootTableSeed())));
    }

    /** 直接标记指定位置的宝箱并立即分析。 */
    public static boolean markChest(
            ServerLevel level, ItemStack itemStack, BlockPos position, ResourceLocation lootTable) {
        Optional<StructureMarkerData> markerData = createMarkerData(level, position, lootTable);
        markerData.ifPresent(data -> itemStack.set(ModDataComponents.STRUCTURE_MARKER, data));
        return markerData.isPresent();
    }

    public static void clearMarker(ItemStack itemStack) {
        itemStack.remove(ModDataComponents.STRUCTURE_MARKER);
    }

    /** 用当前世界数据和配置重算已标记宝箱的分析结果。 */
    public static void refreshAnalysis(ServerLevel level, ItemStack itemStack) {
        getChestInfo(itemStack)
                .ifPresent(
                        chest ->
                                StructMarkerItem.getMarkerData(itemStack)
                                        .ifPresent(
                                                existing ->
                                                        itemStack.set(
                                                                ModDataComponents.STRUCTURE_MARKER,
                                                                analysisOf(
                                                                        level, existing, chest))));
    }

    private static void refreshAnalysisIfNeeded(ServerLevel level, ItemStack itemStack) {
        getChestInfo(itemStack)
                .ifPresent(
                        chest ->
                                StructMarkerItem.getMarkerData(itemStack)
                                        .ifPresent(
                                                existing -> {
                                                    String fingerprint =
                                                            analysisFingerprint(level, chest);
                                                    if (!fingerprint.equals(
                                                            existing.analysisFingerprint())) {
                                                        itemStack.set(
                                                                ModDataComponents.STRUCTURE_MARKER,
                                                                analysisOf(level, existing, chest));
                                                    }
                                                }));
    }

    /** Re-runs the chest analysis and merges it into the existing payload. */
    private static StructureMarkerData analysisOf(
            ServerLevel level, StructureMarkerData existing, ChestInfo chest) {
        StructureValue value =
                StructureValueCalculator.calculate(
                        level, markerInfo(level, chest.position()), 0.0F, discover(level, chest));
        return StructMarkerItem.withAnalysis(existing, value, analysisFingerprint(level, chest));
    }

    /** 宝箱的分析 profile：只用 NBT 里记录的 LootTable，不查结构模板、不依赖世界加载。 */
    private static StructureLootAnalyzer.DiscoveryResult discover(
            ServerLevel level, ChestInfo chest) {
        return StructureLootAnalyzer.discoverFixedForValue(
                level, ChestMarkerItem.CHEST_MARKER_ID, List.of(chest.lootTable()), List.of());
    }

    private static String analysisFingerprint(ServerLevel level, ChestInfo chest) {
        StringBuilder value =
                new StringBuilder(ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
        value.append('|').append(level.dimension().location()).append('|').append(chest.position());
        value.append('|').append(CHEST_MARKER_ID).append('|').append(chest.lootTable());
        return Integer.toHexString(value.toString().hashCode());
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(itemStack, context, tooltip, flag);

        Optional<StructureMarkerData> markerData = StructMarkerItem.getMarkerData(itemStack);
        if (markerData.isEmpty()) {
            tooltip.add(
                    TranslateHelper.translate(TranslateHelper.tooltip("chest_marker.empty"))
                            .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        StructureMarkerData data = markerData.get();

        Component dimension =
                TranslateHelper.dimensionName(data.dimension()).withStyle(ChatFormatting.AQUA);
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.dimension"), dimension)
                        .withStyle(ChatFormatting.GRAY));
        if (Double.isFinite(data.dimensionValue())) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.dimension_value"),
                                    String.format(
                                            java.util.Locale.ROOT, "%.2f", data.dimensionValue()))
                            .withStyle(ChatFormatting.GRAY));
        }
        AnalysisStatus analysisStatus = data.status();
        if ((analysisStatus == AnalysisStatus.EXACT || analysisStatus == AnalysisStatus.APPROXIMATE)
                && data.structureValue().isPresent()) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("chest_marker.chest_value"),
                                    String.format(
                                            java.util.Locale.ROOT,
                                            "%.2f",
                                            data.structureValue().get()))
                            .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.analysis_status"),
                                Component.literal(analysisStatus.name()))
                        .withStyle(ChatFormatting.DARK_GRAY));

        getChestInfo(itemStack)
                .ifPresentOrElse(
                        chest -> {
                            BlockPos position = chest.position();
                            tooltip.add(
                                    TranslateHelper.translate(
                                                    TranslateHelper.tooltip("chest_marker.chest"),
                                                    Component.literal(
                                                            position.getX()
                                                                    + ", "
                                                                    + position.getY()
                                                                    + ", "
                                                                    + position.getZ()))
                                            .withStyle(ChatFormatting.GRAY));
                            tooltip.add(
                                    Component.literal(chest.lootTable().toString())
                                            .withStyle(ChatFormatting.DARK_GRAY));
                        },
                        () ->
                                tooltip.add(
                                        TranslateHelper.translate(
                                                        TranslateHelper.tooltip(
                                                                "chest_marker.no_chest"))
                                                .withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static Optional<StructureMarkerData> createMarkerData(
            ServerLevel level, BlockPos position, ResourceLocation lootTable) {
        ChestInfo chest = new ChestInfo(position, lootTable, 0L);
        StructureMarkerData base =
                StructMarkerItem.baseMarkerData(
                        markerInfo(level, position),
                        false,
                        Optional.of(new StructureMarkerData.ChestData(lootTable, 0L)));
        StructureValue value =
                StructureValueCalculator.calculate(
                        level, markerInfo(level, position), 0.0F, discover(level, chest));
        return Optional.of(
                StructMarkerItem.withAnalysis(base, value, analysisFingerprint(level, chest)));
    }

    private static MarkerInfo markerInfo(ServerLevel level, BlockPos position) {
        return new MarkerInfo(
                level.dimension().location(),
                position,
                new MarkedStructure(
                        CHEST_MARKER_ID,
                        new BoundingBox(
                                position.getX(),
                                position.getY(),
                                position.getZ(),
                                position.getX(),
                                position.getY(),
                                position.getZ())));
    }

    /** 通用容器探测：任何 BlockEntity，只要保存数据里带 LootTable 引用即可标记。 */
    private static ResourceLocation containerLootTable(
            BlockEntity blockEntity, HolderLookup.Provider registries) {
        if (blockEntity == null) return null;
        // 1.21 requires the registry lookup for block entity serialization.
        CompoundTag data = blockEntity.saveWithoutMetadata(registries);
        if (!data.contains("LootTable", Tag.TAG_STRING)) return null;
        return ResourceLocation.tryParse(data.getString("LootTable"));
    }

    /** 已标记宝箱的持久信息。 */
    public record ChestInfo(BlockPos position, ResourceLocation lootTable, long seed) {}
}
