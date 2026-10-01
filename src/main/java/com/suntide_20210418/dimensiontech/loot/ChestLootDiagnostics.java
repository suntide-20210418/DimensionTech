package com.suntide_20210418.dimensiontech.loot;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// TEMPORARY（临时排查工具，不是产品逻辑）。
//
// 排查「装了 Lootr 之后战利品箱不再产出维度解构核心」用的旁路探针，结论确认后整个文件连同
// DimensionCoreChestLoot 里的调用一起删除。它要回答四个问题：
//
//   1. PlayerInteractEvent.RightClickBlock 到底有没有在 Lootr 箱子上触发；
//   2. DimensionCoreChestLoot 的每一个 early-return 分支有没有被命中；
//   3. 核心被写进了哪个容器，写完之后从「方块实体自己的容器」读回来是什么；
//   4. 玩家真正打开的那个容器是什么实现，里面到底有没有核心。
//
// Lootr 相关判断全部走反射。原因不是「优雅」，而是不给项目引入 Lootr 的编译期依赖：Lootr 目前只是
// dev runtime 的可选模组，核心机制不应该因为一次排查就和它绑死。
//
// 注释故意只用 `//`：google-java-format 无法对没有空格的中文做折行，写成 Javadoc 会被它压成一行
// 超长文本，评审时完全没法看。
@EventBusSubscriber(modid = DimensionTechMod.MOD_ID)
public final class ChestLootDiagnostics {

    // 排查结束后把本文件删掉即可，不需要在别处留开关。
    public static final boolean ENABLED = true;

    private static final String TAG = "[DT-LOOT-DIAG]";

    private static final String LOOTR_API = "noobanidus.mods.lootr.common.api.LootrAPI";
    private static final String LOOTR_INFO_PROVIDER =
            "noobanidus.mods.lootr.common.api.data.ILootrInfoProvider";
    private static final String LOOTR_SAVED_DATA =
            "noobanidus.mods.lootr.common.api.data.ILootrSavedData";

    private static Boolean lootrInstalled;

    private ChestLootDiagnostics() {}

    // ------------------------------------------------------------------
    // 日志入口
    // ------------------------------------------------------------------

    public static void log(String stage, String message) {
        if (!ENABLED) {
            return;
        }
        DimensionTechMod.LOGGER.info("{} {} | {}", TAG, stage, message);
    }

    // 这个方块实体值不值得打日志。只对容器类方块（含 Lootr 自己的方块）为真，否则玩家每一次右键门、
    // 拉杆、耕地都会刷屏。判断只走类名，不做反射，避免把开销压到每一次右键上。
    public static boolean isInteresting(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        String name = blockEntity.getClass().getName();
        return blockEntity instanceof RandomizableContainerBlockEntity
                || name.startsWith("noobanidus.mods.lootr")
                || name.contains(".lootr");
    }

    // 每次右击「看起来是容器」的方块都打一行，用来证明事件确实到达了处理器。
    public static void probeEntry(
            PlayerInteractEvent.RightClickBlock event, BlockEntity blockEntity) {
        if (!ENABLED || !isInteresting(blockEntity)) {
            return;
        }
        log(
                "event-entry",
                describeLocation(event.getLevel(), event.getPos())
                        + " "
                        + describeBlockEntity(blockEntity));
    }

    // 交互事件进来了，但被某个 early-return 拦下。reason 就是分支名字。
    public static void skip(
            String reason, PlayerInteractEvent.RightClickBlock event, BlockEntity blockEntity) {
        if (!ENABLED || !isInteresting(blockEntity)) {
            return;
        }
        log(
                "gate-skip",
                "reason="
                        + reason
                        + " "
                        + describeLocation(event.getLevel(), event.getPos())
                        + " be="
                        + blockEntity.getClass().getName());
    }

    // 通过了所有门禁并且已经掷过骰子。
    public static void roll(
            RandomizableContainerBlockEntity container,
            int missesBefore,
            boolean guaranteed,
            float roll,
            boolean generated) {
        if (!ENABLED) {
            return;
        }
        log(
                "roll",
                describeBlockEntity(container)
                        + " missesBefore="
                        + missesBefore
                        + " guaranteed="
                        + guaranteed
                        + " roll="
                        + (guaranteed ? "n/a(short-circuited)" : Float.toString(roll))
                        + " chance="
                        + DimensionCoreChestLoot.DROP_CHANCE
                        + " generated="
                        + generated);
    }

    // 核心写进 target 之后调用。这里的 readBack 是关键证据：它反映的是「方块实体自己的容器」，
    // 而不是玩家打开菜单时看到的那个容器。两者不一致，就是本次故障的根因。
    public static void coreWritten(
            RandomizableContainerBlockEntity target,
            int slot,
            boolean displaced,
            ServerPlayer player) {
        if (!ENABLED) {
            return;
        }
        log(
                "core-written",
                "target="
                        + target.getClass().getName()
                        + " slot="
                        + slot
                        + " displaced="
                        + displaced
                        + " readBack="
                        + describeStack(target.getItem(slot))
                        + " snapshot="
                        + describeContainer(target)
                        + " | "
                        + describeLootrInventory(target.getLevel(), target.getBlockPos(), player));
    }

    // 没掷中，只推进保底计数。
    public static void pityAdvanced(
            RandomizableContainerBlockEntity container, int missesAfter, ServerPlayer player) {
        if (!ENABLED) {
            return;
        }
        log(
                "pity-advanced",
                "missesAfter="
                        + missesAfter
                        + "/"
                        + DimensionCoreChestLoot.PITY_CHEST
                        + " nextOpenGuaranteed="
                        + (missesAfter >= DimensionCoreChestLoot.PITY_CHEST - 1)
                        + " snapshot="
                        + describeContainer(container)
                        + " | "
                        + describeLootrInventory(
                                container.getLevel(), container.getBlockPos(), player));
    }

    // ------------------------------------------------------------------
    // 决定性探针：玩家实际打开的那个容器
    // ------------------------------------------------------------------

    // PlayerContainerEvent.Open 在 ServerPlayer.openMenu 里、containerMenu 赋值之后触发
    // （见 ServerPlayer.java 中的 NeoForge patch），所以此刻能拿到玩家面前那个真实的 Container：
    // 是方块实体本身，还是 Lootr 的 LootrInventory。
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!ENABLED || !(event.getContainer() instanceof ChestMenu menu)) {
            return;
        }
        Container container = menu.getContainer();
        log(
                "menu-open",
                "menu="
                        + event.getContainer().getClass().getSimpleName()
                        + "(type="
                        + event.getContainer().getType()
                        + ") container="
                        + container.getClass().getName()
                        + " lootrOwned="
                        + container.getClass().getName().startsWith("noobanidus.mods.lootr")
                        + " "
                        + describeContainer(container));
    }

    // ------------------------------------------------------------------
    // 描述工具
    // ------------------------------------------------------------------

    public static String describeLocation(Level level, BlockPos pos) {
        return "dim=" + level.dimension().location() + " pos=" + pos.toShortString();
    }

    // 方块实体的实际类型、战利品表指针，以及它有没有被 Lootr 接管。
    // 注意 RandomizableContainerBlockEntity.getLootTable() 读的是普通字段，不是数据组件。
    public static String describeBlockEntity(BlockEntity blockEntity) {
        StringBuilder builder = new StringBuilder("be=").append(blockEntity.getClass().getName());
        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            ResourceKey<LootTable> table = container.getLootTable();
            builder.append(" randomizable=true");
            builder.append(" lootTable=").append(table == null ? "null" : table.location());
            builder.append(" lootSeed=").append(container.getLootTableSeed());
            builder.append(" size=").append(container.getContainerSize());
        } else {
            builder.append(" randomizable=false");
        }
        builder.append(" lootrManaged=").append(isLootrManaged(blockEntity));
        return builder.toString();
    }

    // 容器快照：尺寸、非空槽位、以及里面有没有解构核心。
    public static String describeContainer(Container container) {
        try {
            int size = container.getContainerSize();
            int nonEmpty = 0;
            int cores = 0;
            StringBuilder sample = new StringBuilder();
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                nonEmpty++;
                if (stack.is(ModItems.DIMENSION_DECONSTRUCTION_CORE.get())) {
                    cores++;
                }
                if (sample.length() < 96) {
                    if (sample.length() > 0) {
                        sample.append(',');
                    }
                    sample.append(slot).append('=').append(describeStack(stack));
                }
            }
            return "size="
                    + size
                    + " nonEmpty="
                    + nonEmpty
                    + " cores="
                    + cores
                    + " sample=["
                    + sample
                    + "]";
        } catch (RuntimeException exception) {
            return "snapshotFailed(" + exception + ")";
        }
    }

    public static String describeStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static boolean isLootrInstalled() {
        if (lootrInstalled == null) {
            try {
                Class.forName(LOOTR_API);
                lootrInstalled = Boolean.TRUE;
            } catch (ClassNotFoundException exception) {
                lootrInstalled = Boolean.FALSE;
            }
        }
        return lootrInstalled;
    }

    // LootrAPI.resolveBlockEntity 非 null 就说明这个方块实体已经被 Lootr 接管。Lootr 是把自己的方块
    // setBlock 上去替换原版箱子，所以被接管之后方块实体的实际类型是 Lootr 的子类 —— 但它仍然继承
    // RandomizableContainerBlockEntity，这正是「instanceof 门禁照样通过」却依然失效的原因。
    public static boolean isLootrManaged(BlockEntity blockEntity) {
        if (!isLootrInstalled()) {
            return false;
        }
        try {
            Class<?> api = Class.forName(LOOTR_API);
            Object resolved =
                    api.getMethod("resolveBlockEntity", BlockEntity.class)
                            .invoke(null, blockEntity);
            return resolved != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            log("lootr-probe-failed", "resolveBlockEntity: " + exception);
            return false;
        }
    }

    // 读 Lootr 为这个玩家保存的真实背包（LootrInventory）。Lootr 箱子自己的 items 列表是死的，
    // 玩家打开菜单时看到的是这里的容器。
    //
    // inventory=null(尚未创建) 是符合预期的：Lootr 在打开菜单时才惰性建它，而 RightClickBlock
    // 发生在打开菜单之前 —— 这本身就是根因链的一环。
    public static String describeLootrInventory(Level level, BlockPos pos, Player player) {
        if (!isLootrInstalled()) {
            return "lootrInventory=not-installed";
        }
        if (level == null || !(player instanceof ServerPlayer serverPlayer)) {
            return "lootrInventory=(no-player-context)";
        }
        try {
            Class<?> providerType = Class.forName(LOOTR_INFO_PROVIDER);
            Object provider =
                    providerType
                            .getMethod("of", BlockPos.class, Level.class)
                            .invoke(null, pos, level);
            if (provider == null) {
                return "lootrInventory=(not-a-lootr-container)";
            }
            Class<?> api = Class.forName(LOOTR_API);
            Object data = api.getMethod("getData", providerType).invoke(null, provider);
            if (data == null) {
                return "lootrInventory=(no-saved-data)";
            }
            Class<?> savedDataType = Class.forName(LOOTR_SAVED_DATA);
            Object inventory =
                    savedDataType
                            .getMethod("getInventory", ServerPlayer.class)
                            .invoke(data, serverPlayer);
            if (inventory == null) {
                return "lootrInventory=null(尚未创建)";
            }
            if (inventory instanceof Container container) {
                return "lootrInventory="
                        + inventory.getClass().getSimpleName()
                        + " "
                        + describeContainer(container);
            }
            return "lootrInventory=" + inventory.getClass().getName() + "(not-a-container)";
        } catch (ReflectiveOperationException | RuntimeException exception) {
            log("lootr-probe-failed", "inventory: " + exception);
            return "lootrInventory=(probe-failed: " + exception.getClass().getSimpleName() + ")";
        }
    }
}
