package com.suntide_20210418.dimensiontech.loot;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.integration.lootr.LootrCompat;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Adds the dimension core roll and player-scoped 20-chest pity to structure loot. */
@EventBusSubscriber(modid = DimensionTechMod.MOD_ID)
public final class DimensionCoreChestLoot {
    private static final String MISSES_TAG =
            DimensionTechMod.MOD_ID + ":dimension_core_chest_misses";

    // 「这个箱子已经掷过一次」的显式标记，记在方块实体自己的 NeoForge 持久数据里。
    //
    // 这个标记不能省。原版过去是靠 RandomizableContainer#unpackLootTable 内部那句
    // setLootTable(null) 副作用，配合下面的 lootTable == null 门禁，间接实现「一个箱子只掷一次」。
    // Lootr 把 unpackLootTable 覆写成了空实现，于是它自己的箱子上战利品表永不清空 —— 幂等保证随那个
    // 副作用一起消失，同一个箱子可以无限重复掷骰、无限推进保底。与其继续依赖别人的实现细节，不如自己
    // 把这条不变式写下来。BlockEntity#getPersistentData 会以 "NeoForgeData" 键往返存档，而且 Lootr
    // 换方块实体时会显式搬运它（PlatformAPIImpl#copySpecificData / #restoreSpecificData）。
    // 包内可见：临时诊断 ChestLootDiagnostics 要把它打进日志里，诊断删掉后可以收回 private。
    static final String ROLLED_TAG = DimensionTechMod.MOD_ID + ":dimension_core_rolled";

    /** Consecutive failed chests before the next one is guaranteed to hold a core. */
    public static final int PITY_CHEST = 20;

    /** Base per-chest core chance. */
    public static final float DROP_CHANCE = 0.05F;

    private DimensionCoreChestLoot() {}

    @SubscribeEvent
    public static void onOpenContainer(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getLevel().isClientSide()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        ChestLootDiagnostics.probeEntry(event, blockEntity);
        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
            ChestLootDiagnostics.skip("not-a-randomizable-container", level, pos, blockEntity);
            return;
        }
        if (!container.canOpen(player)) {
            ChestLootDiagnostics.skip("canOpen=false", level, pos, blockEntity);
            return;
        }

        // 1.21 的战利品表指针不再是根 tag 里的 "LootTable" 字符串：RandomizableContainerBlockEntity
        // 把它存在普通字段 lootTable 里，序列化时才写成 "LootTable" 键；方块物品那条路径改用
        // DataComponents.CONTAINER_LOOT（见 applyImplicitComponents / collectImplicitComponents）。
        // 所以这里读 getLootTable()，它同时承担「这是不是一个还没展开的战利品箱」这一判断。
        ResourceKey<LootTable> lootTable = container.getLootTable();
        if (lootTable == null) {
            ChestLootDiagnostics.skip("lootTable=null", level, pos, blockEntity);
            return;
        }
        if (!lootTable.location().getPath().startsWith("chests/")) {
            ChestLootDiagnostics.skip(
                    "lootTable-path=" + lootTable.location(), level, pos, blockEntity);
            return;
        }

        // 到这里为止两条路径做的事完全一样。往下只有两个变量：幂等判据，以及核心写进哪个容器。
        if (LootrCompat.isLootrContainer(blockEntity)) {
            openLootrChest(blockEntity, level, pos, player);
        } else {
            openVanillaChest(container, level, pos, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalData = event.getOriginal().getPersistentData();
        if (originalData.contains(MISSES_TAG, CompoundTag.TAG_INT)) {
            event.getEntity()
                    .getPersistentData()
                    .putInt(MISSES_TAG, originalData.getInt(MISSES_TAG));
        }
    }

    // 原版箱子：战利品全局只有一份（第一个开的人展开它，之后箱子就空了），所以幂等判据必须是箱子级的。
    private static void openVanillaChest(
            RandomizableContainerBlockEntity container,
            Level level,
            BlockPos pos,
            ServerPlayer player) {
        CompoundTag marker = container.getPersistentData();
        if (marker.getBoolean(ROLLED_TAG)) {
            ChestLootDiagnostics.skip("already-rolled", level, pos, container);
            return;
        }

        // Expanding here makes this event the single point that counts and modifies the chest.
        container.unpackLootTable(player);
        // 标记必须在掷骰之前落下：掷骰有「命中」和「没命中」两条出口，任何一条都要把这个箱子关掉，
        // 否则落空那条路径会被下一次右键重复计数。
        marker.putBoolean(ROLLED_TAG, true);
        rollInto(container, container, player);
    }

    // Lootr 箱子：每个玩家有自己的一份战利品实例，所以幂等判据也必须是每玩家的 ——
    // 判据就是「这个玩家在这个箱子上有没有自己的 LootrInventory」。
    //
    // 关键是写入目标不能是方块实体。Lootr 把原版箱子换成了自己的 LootrChestBlockEntity，它虽然仍然
    // 继承 RandomizableContainerBlockEntity（所以 instanceof / canOpen / getLootTable 三道门禁全部
    // 照常通过），但玩家菜单挂的是 LootrSavedData 里的 per-player LootrInventory，方块实体自己的
    // items 列表从头到尾没人读写 —— 往那里 setItem 就是静默丢弃。
    private static void openLootrChest(
            BlockEntity blockEntity, Level level, BlockPos pos, ServerPlayer player) {
        // 这一步必须用只读探测。get-or-create 会顺手把背包建出来，建完之后就再也读不到 null 了，
        // 「是否首开」这个判据会当场失效。
        if (LootrCompat.peekInventory(level, pos, player) != null) {
            ChestLootDiagnostics.skip("lootr-already-looted-by-player", level, pos, blockEntity);
            return;
        }
        // 取到（必要时创建并填充）这个玩家的背包。Lootr 自己的 handleProviderOpen 紧跟着会走同一条
        // get-or-create 路径拿到同一个实例再开菜单，所以这里提前建好不会把战利品填两遍。
        Container target = LootrCompat.openLootInventory(level, pos, player);
        if (target == null) {
            ChestLootDiagnostics.skip("lootr-inventory-unavailable", level, pos, blockEntity);
            return;
        }
        rollInto(blockEntity, target, player);
    }

    // 掷骰并写入。两条路径共用 —— 它们的区别只有「写进哪个容器」和上面的幂等判据。
    private static void rollInto(BlockEntity blockEntity, Container target, ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        int misses = Math.max(0, persistentData.getInt(MISSES_TAG));
        boolean guaranteed = misses >= PITY_CHEST - 1;
        // TEMP-DIAG：为了把真正的骰值写进日志才拆出 roll。三元表达式只在 !guaranteed 时求值，
        // 和原来 `guaranteed || nextFloat() < DROP_CHANCE` 的短路行为完全一致 —— 不能改成先无条件
        // 取值再比较，那会平移该玩家 RandomSource 的随机流。
        float roll = guaranteed ? Float.NaN : player.getRandom().nextFloat();
        boolean generated = guaranteed || roll < DROP_CHANCE;
        ChestLootDiagnostics.roll(blockEntity, target, misses, guaranteed, roll, generated, player);
        if (generated) {
            insertCore(target, player, blockEntity);
            persistentData.putInt(MISSES_TAG, 0);
        } else {
            persistentData.putInt(MISSES_TAG, misses + 1);
            ChestLootDiagnostics.pityAdvanced(target, misses + 1);
        }
        // 原版箱子上这就是标记容器与标记方块实体；Lootr 背包上它会转成 LootrSavedData#setDirty。
        target.setChanged();
    }

    // 优先塞空槽；全满时随机顶掉一格，并把被顶掉的物品丢在箱子前面。
    private static void insertCore(Container target, ServerPlayer player, BlockEntity blockEntity) {
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            if (target.getItem(slot).isEmpty()) {
                target.setItem(slot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()));
                ChestLootDiagnostics.coreWritten(blockEntity, target, slot, false, player);
                return;
            }
        }

        int slot = player.getRandom().nextInt(target.getContainerSize());
        ItemStack displaced = target.getItem(slot).copy();
        target.setItem(slot, new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()));
        ChestLootDiagnostics.coreWritten(blockEntity, target, slot, true, player);
        BlockPos pos = blockEntity.getBlockPos();
        Containers.dropItemStack(
                player.serverLevel(),
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                displaced);
    }
}
