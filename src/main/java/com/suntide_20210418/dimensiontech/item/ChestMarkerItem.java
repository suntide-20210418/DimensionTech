package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureLootAnalyzer;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * 宝箱分析器：按绑定按键（默认 V）分析准星指向的宝箱；若目标是带 LootTable 引用的容器，直接以该
 * 宝箱的 LootTable 作为战利品来源做期望分析并标记它。右键（use）只打开分析界面查看已标记内容。
 *
 * <p>它写入的 {@code StructureMarkerData} 与 {@link StructMarkerItem} 同构（Dimension + Position +
 * Structure{Id, Bounds}），只是 Structure.Id 固定为虚拟 id {@link #CHEST_MARKER_ID}，并把宝箱的
 * LootTable 存进 {@code ChestData} 子标签。因为下游只认 NBT 结构不认物品类型，结构数据分析仪的
 * 复制模式和结构采掘器的生产链路都能直接消费宝箱标记；唯一的分支点是分析：宝箱没有注册结构、
 * 没有模板，必须用 {@code discoverFixedForValue} 按记录在 NBT 里的 LootTable 构造 profile，而
 * 不能走按结构 id 的 {@code StructureAnalysisService.discover}。
 */
public class ChestMarkerItem extends Item {
    /** 虚拟结构 id：不进注册表，只作为宝箱标记在 StructureMarkerData 里的占位标识。 */
    public static final ResourceLocation CHEST_MARKER_ID =
            ResourceLocationHelper.item("chest_marker");

    private static final String MARKER_DATA_TAG = "StructureMarkerData";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String POSITION_TAG = "Position";
    private static final String STRUCTURE_TAG = "Structure";
    private static final String CHEST_DATA_TAG = "ChestData";
    private static final String LOOT_TABLE_TAG = "LootTable";
    private static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
    private static final String ANALYSIS_FINGERPRINT_TAG = "AnalysisFingerprint";

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
     * <p>分析动作由绑定按键（默认 V）触发：客户端把准星指向的方块位置发给服务端，服务端在这里
     * 验证并标记。不用右键方块交互，因为右键一个宝箱会优先打开它而不是分析它。
     */
    public static ResourceLocation lootTableAt(ServerLevel level, BlockPos position) {
        return containerLootTable(level.getBlockEntity(position));
    }

    private static void openScreen(
            net.minecraft.server.level.ServerPlayer player, ItemStack itemStack, InteractionHand hand) {
        com.suntide_20210418.dimensiontech.network.ModNetwork.openRefreshedMarker(
                player, itemStack, hand);
    }

    /** 读取标记的宝箱信息；未标记或数据损坏时为空。 */
    public static Optional<ChestInfo> getChestInfo(ItemStack itemStack) {
        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        if (markerData == null || !markerData.contains(CHEST_DATA_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag chestData = markerData.getCompound(CHEST_DATA_TAG);
        ResourceLocation lootTable = ResourceLocation.tryParse(chestData.getString(LOOT_TABLE_TAG));
        if (lootTable == null || !markerData.contains(POSITION_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag positionData = markerData.getCompound(POSITION_TAG);
        return Optional.of(
                new ChestInfo(
                        new BlockPos(
                                positionData.getInt("X"),
                                positionData.getInt("Y"),
                                positionData.getInt("Z")),
                        lootTable,
                        chestData.getLong(LOOT_TABLE_SEED_TAG)));
    }

    /** 直接标记指定位置的宝箱并立即分析。 */
    public static boolean markChest(
            ServerLevel level, ItemStack itemStack, BlockPos position, ResourceLocation lootTable) {
        Optional<CompoundTag> markerData = createMarkerData(level, position, lootTable);
        markerData.ifPresent(data -> itemStack.getOrCreateTag().put(MARKER_DATA_TAG, data));
        return markerData.isPresent();
    }

    public static void clearMarker(ItemStack itemStack) {
        itemStack.removeTagKey(MARKER_DATA_TAG);
    }

    /** 用当前世界数据和配置重算已标记宝箱的分析结果。 */
    public static void refreshAnalysis(ServerLevel level, ItemStack itemStack) {
        getChestInfo(itemStack)
                .ifPresent(
                        chest -> {
                            CompoundTag markerData =
                                    itemStack.getOrCreateTagElement(MARKER_DATA_TAG);
                            writeAnalysis(level, markerData, chest);
                        });
    }

    private static void refreshAnalysisIfNeeded(ServerLevel level, ItemStack itemStack) {
        getChestInfo(itemStack)
                .ifPresent(
                        chest -> {
                            CompoundTag markerData =
                                    itemStack.getOrCreateTagElement(MARKER_DATA_TAG);
                            String fingerprint = analysisFingerprint(level, chest);
                            if (!fingerprint.equals(markerData.getString(ANALYSIS_FINGERPRINT_TAG))) {
                                writeAnalysis(level, markerData, chest);
                            }
                        });
    }

    private static void writeAnalysis(ServerLevel level, CompoundTag markerData, ChestInfo chest) {
        StructureValue value =
                StructureValueCalculator.calculate(
                        level, markerInfo(level, chest.position()), 0.0F, discover(level, chest));
        StructMarkerItem.writeAnalysisResult(markerData, value);
        markerData.putString(ANALYSIS_FINGERPRINT_TAG, analysisFingerprint(level, chest));
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
            ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, tooltip, flag);

        CompoundTag markerData = itemStack.getTagElement(MARKER_DATA_TAG);
        if (markerData == null) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("chest_marker.empty"))
                            .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        ResourceLocation dimensionId =
                ResourceLocation.tryParse(markerData.getString(DIMENSION_TAG));
        Component dimension =
                (dimensionId == null
                                ? Component.literal(markerData.getString(DIMENSION_TAG))
                                : TranslateHelper.dimensionName(dimensionId))
                        .withStyle(ChatFormatting.AQUA);
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("struct_marker.dimension"), dimension)
                        .withStyle(ChatFormatting.GRAY));
        if (hasFiniteNumber(markerData, "DimensionValue")) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("struct_marker.dimension_value"),
                                    String.format(
                                            java.util.Locale.ROOT,
                                            "%.2f",
                                            markerData.getDouble("DimensionValue")))
                            .withStyle(ChatFormatting.GRAY));
        }
        AnalysisStatus analysisStatus = StructMarkerItem.analysisStatus(markerData);
        if ((analysisStatus == AnalysisStatus.EXACT
                        || analysisStatus == AnalysisStatus.APPROXIMATE)
                && markerData.contains("StructureValue", Tag.TAG_ANY_NUMERIC)) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("chest_marker.chest_value"),
                                    String.format(
                                            java.util.Locale.ROOT,
                                            "%.2f",
                                            markerData.getDouble("StructureValue")))
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

    private static Optional<CompoundTag> createMarkerData(
            ServerLevel level, BlockPos position, ResourceLocation lootTable) {
        CompoundTag markerData = new CompoundTag();
        markerData.putString(DIMENSION_TAG, level.dimension().location().toString());

        CompoundTag positionData = new CompoundTag();
        positionData.putInt("X", position.getX());
        positionData.putInt("Y", position.getY());
        positionData.putInt("Z", position.getZ());
        markerData.put(POSITION_TAG, positionData);

        markerData.put(STRUCTURE_TAG, createStructureData(position));

        CompoundTag chestData = new CompoundTag();
        chestData.putString(LOOT_TABLE_TAG, lootTable.toString());
        chestData.putLong(LOOT_TABLE_SEED_TAG, 0L);
        markerData.put(CHEST_DATA_TAG, chestData);

        writeAnalysis(level, markerData, new ChestInfo(position, lootTable, 0L));
        return Optional.of(markerData);
    }

    private static CompoundTag createStructureData(BlockPos position) {
        CompoundTag structureData = new CompoundTag();
        structureData.putString("Id", CHEST_MARKER_ID.toString());
        CompoundTag boundsData = new CompoundTag();
        boundsData.putInt("MinX", position.getX());
        boundsData.putInt("MinY", position.getY());
        boundsData.putInt("MinZ", position.getZ());
        boundsData.putInt("MaxX", position.getX());
        boundsData.putInt("MaxY", position.getY());
        boundsData.putInt("MaxZ", position.getZ());
        structureData.put("Bounds", boundsData);
        return structureData;
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
    private static ResourceLocation containerLootTable(BlockEntity blockEntity) {
        if (blockEntity == null) return null;
        CompoundTag data = blockEntity.saveWithoutMetadata();
        if (!data.contains("LootTable", Tag.TAG_STRING)) return null;
        return ResourceLocation.tryParse(data.getString("LootTable"));
    }

    private static boolean hasFiniteNumber(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) && Double.isFinite(tag.getDouble(key));
    }

    /** 已标记宝箱的持久信息。 */
    public record ChestInfo(BlockPos position, ResourceLocation lootTable, long seed) {}
}
