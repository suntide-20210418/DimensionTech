package com.suntide_20210418.dimensiontech.integration.lootr;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

// Lootr 兼容层（可选依赖）。整层只通过反射访问 Lootr，文件里没有任何 noobanidus.mods.lootr 的 import，
// 所以 Lootr 缺席时这一层整体退化为「不存在」，核心机制不编译期依赖它。
//
// 为什么需要这一层：Lootr 用 setBlock 把原版箱子换成自己的方块实体，而玩家菜单挂的是
// LootrSavedData 里的 per-player LootrInventory，不是方块实体自己的 items 列表。往后者写等于丢弃 ——
// 见 DimensionCoreChestLoot 的注释。
//
// 反射句柄只在首次使用时解析一次并缓存；任何一步失败都会置位 failed 并停止重试，
// 避免每个右键都在异常里穿行。
public final class LootrCompat {

    private static final String LOOTR_API = "noobanidus.mods.lootr.common.api.LootrAPI";
    private static final String LOOTR_INFO_PROVIDER =
            "noobanidus.mods.lootr.common.api.data.ILootrInfoProvider";
    private static final String LOOTR_SAVED_DATA =
            "noobanidus.mods.lootr.common.api.data.ILootrSavedData";
    private static final String LOOTR_LOOT_FILLER =
            "noobanidus.mods.lootr.common.api.data.LootFiller";
    private static final String LOOTR_MENU_BUILDER = "noobanidus.mods.lootr.common.api.MenuBuilder";

    private static Boolean present;
    private static boolean resolved;
    private static boolean failed;

    private static Method resolveBlockEntity;
    private static Method providerOf;
    private static Method getData;
    private static Method getInventoryOfPlayer;
    private static Method getOrCreateInventory;
    private static Method getDefaultFiller;

    private LootrCompat() {}

    // Lootr 是否在运行时classpath 上。结果缓存，只查一次类。
    public static boolean isPresent() {
        if (present == null) {
            present = Boolean.valueOf(loadClass(LOOTR_API) != null);
        }
        return present;
    }

    // 这个方块实体是不是已经被 Lootr 接管（LootrAPI.resolveBlockEntity 非 null 即为是）。
    //
    // 解析失败时**返回 true**：兼容层坏了但又确认 Lootr 在场的情况下，宁可判定为 Lootr 容器让调用方跳过，
    // 也不要退回往方块实体那个死列表里写 —— 后者是静默失效，前者至少会留下一条警告。
    public static boolean isLootrContainer(BlockEntity blockEntity) {
        if (!isPresent()) {
            return false;
        }
        if (!resolveHandles()) {
            return true;
        }
        try {
            return resolveBlockEntity.invoke(null, blockEntity) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("resolveBlockEntity", exception);
            return true;
        }
    }

    // 只读探测：这个玩家在这个箱子上是否已经有自己的战利品背包。
    //
    // 返回 null 表示「还没有」——它同时是两件事的判据：
    //   1. 兼容层面前这是首开，还没有任何东西被写进去；
    //   2. 因此这是唯一可以安全掷骰的时机。
    // 必须用只读的 ILootrSavedData#getInventory，不能用 getOrCreateInventory：后者会顺手把背包建出来，
    // 建完之后就再也读不到 null 了。
    public static Container peekInventory(Level level, BlockPos pos, ServerPlayer player) {
        if (!isPresent()) {
            return null;
        }
        Object data = savedData(level, pos);
        if (data == null) {
            return null;
        }
        try {
            Object inventory = getInventoryOfPlayer.invoke(data, player);
            return inventory instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("getInventory(player)", exception);
            return null;
        }
    }

    // get-or-create：取到（必要时创建并填充）这个玩家的战利品背包。
    //
    // LootrAPI.getInventory -> DataStorage.getInventory -> LootrSavedData.getOrCreateInventory，是
    // get-or-create 语义，所以这里提前建好之后马上轮到 DefaultLootrAPIImpl.handleProviderOpen 走
    // 同一条路径，它会拿到同一个实例并正常开菜单，战利品不会被填两遍。
    public static Container openLootInventory(Level level, BlockPos pos, ServerPlayer player) {
        if (!isPresent()) {
            return null;
        }
        Object provider = provider(level, pos);
        if (provider == null) {
            return null;
        }
        try {
            Object filler = getDefaultFiller.invoke(provider);
            if (filler == null) {
                warnOnce("getDefaultFiller 返回 null", null);
                return null;
            }
            Object inventory = getOrCreateInventory.invoke(null, provider, player, filler, null);
            return inventory instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("getInventory(provider, player, filler, builder)", exception);
            return null;
        }
    }

    private static Object savedData(Level level, BlockPos pos) {
        Object provider = provider(level, pos);
        if (provider == null) {
            return null;
        }
        try {
            return getData.invoke(null, provider);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("getData", exception);
            return null;
        }
    }

    private static Object provider(Level level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        if (!resolveHandles()) {
            return null;
        }
        try {
            return providerOf.invoke(null, pos, level);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("ILootrInfoProvider.of", exception);
            return null;
        }
    }

    private static synchronized boolean resolveHandles() {
        if (resolved) {
            return !failed;
        }
        resolved = true;
        try {
            Class<?> api = Class.forName(LOOTR_API);
            Class<?> providerType = Class.forName(LOOTR_INFO_PROVIDER);
            Class<?> savedDataType = Class.forName(LOOTR_SAVED_DATA);
            Class<?> fillerType = Class.forName(LOOTR_LOOT_FILLER);
            Class<?> menuBuilderType = Class.forName(LOOTR_MENU_BUILDER);

            resolveBlockEntity = api.getMethod("resolveBlockEntity", BlockEntity.class);
            providerOf = providerType.getMethod("of", BlockPos.class, Level.class);
            getData = api.getMethod("getData", providerType);
            // ILootrInfo#getDefaultFiller 是接口默认方法，ILootrInfoProvider 继承它，且不会返回 null
            // （内部回落到 DefaultLootFiller.getInstance()），所以这里不需要再单独取那个单例。
            getDefaultFiller = providerType.getMethod("getDefaultFiller");
            getInventoryOfPlayer = savedDataType.getMethod("getInventory", ServerPlayer.class);
            getOrCreateInventory =
                    api.getMethod(
                            "getInventory",
                            providerType,
                            ServerPlayer.class,
                            fillerType,
                            menuBuilderType);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException exception) {
            failed = true;
            DimensionTechMod.LOGGER.warn(
                    "Dimension Tech: Lootr 兼容层反射解析失败，Lootr 箱子将不再产出维度解构核心。"
                            + "这通常意味着 Lootr 的 API 发生了变化。",
                    exception);
            return false;
        }
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static void warnOnce(String step, Exception exception) {
        if (failed) {
            return;
        }
        failed = true;
        DimensionTechMod.LOGGER.warn(
                "Dimension Tech: Lootr 兼容层在 {} 上失败，Lootr 箱子将不再产出维度解构核心。", step, exception);
    }
}
