# Dimension Tech：Forge 1.20.1 → NeoForge 1.21.1 移植计划

- 源工程：`D:\mycode\ModDevelopment\DimensionTech-1.20.1`（Forge 47.4.10 / MC 1.20.1 / Java 17 / ForgeGradle）
- 目标工程：`D:\mycode\ModDevelopment\DimensionTech-1.21.1`（NeoForge 21.1.251 / MC 1.21.1 / Java 21 / ModDevGradle 2.0.147）
- 本计划中的所有 API 结论均以本地 `neoforge-21.1.251-sources.jar` 反编译源码为准（已解压核对），不依赖记忆。

---

## 1. 目标与范围

### 1.1 目标

把 Dimension Tech 的全部玩法功能在 NeoForge 1.21.1 上等价重建，并保持：

1. **注册名不变**：`dimension_tech` 命名空间下所有 block / item / block entity / menu / fluid / creative tab 的注册 ID 与 1.20.1 完全一致。
2. **NBT / 组件数据可迁移**：结构标记（structure marker）上持久化的分析结果，字段语义保持可读；版本升级导致的语义失效由算法版本号显式声明（见 3.9）。
3. **精确语义重建而非近似**：`loot/expectation` 的精确期望引擎按 1.21.1 原版实现重写（用户已确认），不做数值降级。
4. **完整覆盖，含全部集成**：JEI / Jade / KubeJS / AE2 四个集成一并移植（用户已确认）。

### 1.2 范围外

- 不新增玩法、不改数值平衡、不重构 1.20.1 已定型的设计（如 `ProcessingMath` 的并行/效率公式）。
- 不为 1.20.1 存档做跨版本兼容层。1.21.1 是新的存档基线。
- 不改 `.workbuddy/`、不动 `docs/` 下的既有设计文档。

### 1.3 已确认的两项关键决策

| 决策点 | 选择 | 影响 |
| --- | --- | --- |
| loot 精确语义引擎 | 按 1.21.1 重建精确语义 | `*1201` 系列重命名为 `*1211`；`ALGORITHM_VERSION` 5 → 6；指纹与既有标记全部失效重算（Phase 7） |
| 移植范围 | 完整移植含全部集成 | Phase 9 覆盖 JEI/Jade/KubeJS/AE2，逐个独立验证、独立回退 |

---

## 2. 现状分析

### 2.1 源工程规模与结构

`src/main/java` 共 **205 个 Java 文件 / 40,199 行**，按模块：

| 模块 | 行数 | 文件数 | 版本敏感度 |
| --- | --- | --- | --- |
| `loot`（含 `expectation` 精确引擎） | 10,673 | 46 | **极高**（原版内部实现耦合） |
| `client`（GUI / 渲染 / 键位） | 10,557 | 45 | 中（`GuiGraphics` 基本兼容） |
| `block` + `block/entity` | 5,334 | 32 | 高（capability、NBT） |
| `structure`（分析 / 虚拟采样） | 2,910 | 7 | 高（worldgen + registry） |
| `integration` | 2,380 | 22 | 高（四个外部模组 API） |
| `datagen` | 2,103 | 7 | 高（Provider 基类签名变更） |
| `item` | 1,323 | 7 | 高（ItemStack NBT → 组件） |
| `utils` | 1,151 | 13 | 中 |
| `structurereactor` | 1,137 | 10 | 低（纯逻辑） |
| `network` | 899 | 1 | **极高**（整层重写） |
| `structureminer` | 889 | 7 | 低 |
| `config` | 500 | 1 | 中（ConfigSpec 换名 + 注册方式） |
| `energy` / `fluid` | 118 / 120 | 2 / 2 | 高（capability / FluidType 注册表） |
| `recipe` / `ModDataGenerator` / `DimensionTechMod` | 19 / 40 / 46 | 1 / 1 / 1 | 高 |

资源与数据：

- `src/main/resources`：`models` 45、`textures` 68、`guis` 6（PNG）、`data/dimension_tech` 18（`loot_tables` 16、`item_modifiers` 1、`predicates` 1）、`pack.mcmeta`、`kubejs.plugins.txt`
- `src/generated/resources`：`assets` 130、`data` 153（`advancements` 52、`loot_tables` 47、`recipes` 52）

### 2.2 目标工程现状

目标目录是一个**刚初始化的 MDK 骨架**，尚未开始移植：

- [build.gradle](file:///d:/mycode/ModDevelopment/DimensionTech-1.21.1/build.gradle)：`net.neoforged.moddev` 2.0.147，Java 21 toolchain，parchment `2024.11.17`，已预置 13 个 CurseMaven 依赖（1.21.1 版本），含 `generateModMetadata` 任务。
- [gradle.properties](file:///d:/mycode/ModDevelopment/DimensionTech-1.21.1/gradle.properties)：`neo_version=21.1.251`、`mod_id=dimension_tech`、`mod_version=1.0.0`。
- `src/main/java`：仅 3 个示例文件 —— [DimensionTech.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.21.1/src/main/java/com/suntide_20210418/dimensiontech/DimensionTech.java)（示例入口，含正确的新构造签名）、[Config.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.21.1/src/main/java/com/suntide_20210418/dimensiontech/Config.java)、`DimensionTechClient.java`。
- `src/main/templates/META-INF/neoforge.mods.toml`：新格式模板（`type="required"`）。
- 缺失：`spotless`、`vanillaLootRuntime` 开关、`syncGameTestStructures` 任务、`src/test` 配置、`pack.mcmeta`、`kubejs.plugins.txt`。

这 3 个示例文件在 Phase 2 全部删除（入口类名冲突）。

### 2.3 依赖矩阵（1.20.1 → 1.21.1）

| 依赖 | 1.20.1 | 目标 build.gradle 已填 | 需要处置 |
| --- | --- | --- | --- |
| KubeJS | `kubejs-238086:8020595` | `8843626` | 需确认 1.21.1 API 对应版本 |
| Jade | `jade-324717:6855440` | `8591319` | 1.21 API 有 `IElementHelper` / `BoxStyle` 变更 |
| GuideME | `guideme-1173950:7127447` | `8897145` | 1.20.1 build.gradle 声明但源码未引用；决定是否保留 |
| AE2 | `applied-energistics-2-223794:7148487` | `7027323` | 1.21 API 变更 |
| Mekanism | `mekanism-268560:6552911` | `7904058` | 1.20.1 源码未 import；确认是否仍需运行时依赖 |
| Architectury API | `architectury-api-419699:5137938` | `8492726` | 1.20.1 源码未 import；同上 |
| Rhino | `rhino-416294:6186971` | `8218748` | KubeJS 运行时依赖 |
| Mouse Tweaks | `60089:5338457`（compileOnly） | `5637846` | 保持 client-only，不要放进 datagen 类路径 |
| AllTheModium | `364466:7142820` | `8946075` | 仅配置默认值中出现 `allthemodium:the_other` |
| GeckoLib | `388172:8285794` | `8893490` | 1.20.1 源码未 import；确认 |
| ATO / Time in a Bottle | `405593:5348605` / `895919:4744787` | 有 | 1.20.1 源码未 import；确认 |
| JEI | `jei-238222:7391695`（+ EMI compileOnly） | `8759217` | **必需**，集成层依赖 |
| JUnit | `org.junit.jupiter:junit-jupiter:5.10.2` | 无 | 目标工程需补 |

> 已填版本号尚未逐个核验与 MC 1.21.1 的匹配性。Phase 1 首个动作是逐条确认，缺失的用 CurseMaven/Modrinth 实际文件 ID 替换，并在 `gradle.properties` 里集中管理版本号。

---

## 3. 关键 API 差异与处置

以下每一项都标注了已验证的 NeoForge 21.1.251 目标 API。

### 3.1 构建系统

| 项 | 1.20.1 | 1.21.1 NeoForge |
| --- | --- | --- |
| Gradle 插件 | `net.minecraftforge.gradle` + `librarian.forgegradle` | `net.neoforged.moddev`（目标已具备） |
| Java | 17 | **21**（toolchain 已设） |
| 脱混淆 | `fg.deobf(...)` | 直接写 CurseMaven 坐标，无 `deobf` |
| 元数据 | `src/main/resources/META-INF/mods.toml` | `src/main/templates/META-INF/neoforge.mods.toml`（`processResources` 展开） |
| 依赖声明字段 | `mandatory=true` | `type="required"` / `"optional"` |
| 依赖 modId | `forge` | `neoforge` |
| AT | `accessTransformer = file(...)`（源工程引用了一个不存在的 cfg） | `[[accessTransformers]]`；本模组不需要 |
| mixin | 无 | 无 |

`pack.mcmeta` 的 `pack_format: 15` 对 1.21.1 已过时。处置：把资源包格式改为 `34`、数据包格式改为 `48`（`{"pack":{"pack_format":34,"supported_formats":[34,48],...}}`），或直接删除 —— 目标 MDK 默认不含 `pack.mcmeta`，NeoForge 会自行处理模组资源。**推荐直接删除**，并同步删掉源 `build.gradle` 里对 `pack.mcmeta` 的 `filesMatching` 展开。

### 3.2 注册机制

- `net.minecraftforge.registries.ForgeRegistries` → **`net.minecraft.core.registries.Registries`**（内置）或 **`net.neoforged.neoforge.registries.NeoForgeRegistries` / `NeoForgeRegistries.Keys`**（NeoForge 扩展，如 `FLUID_TYPES`）。共 14 处 `ForgeRegistries` 引用。
- `net.minecraftforge.registries.RegistryObject` → **`net.neoforged.neoforge.registries.DeferredHolder<R, T>`**（`RegistryObject` 在 21.x 已删除）。共 9 处 import，实际用法遍布 `ModBlocks` / `ModItems` / `ModBlockEntities` / `ModMenu` / `ModCreativeModeTabs` / `ModFluids` / datagen。
- `DeferredRegister.create(...)` 保留，但签名接受 `ResourceKey<? extends Registry<T>>`（如 `Registries.BLOCK`）。
- `DeferredRegister` 也提供便利工厂：`DeferredRegister.createBlocks(modid)` / `createItems(modid)`，返回 `DeferredRegister.Blocks` / `DeferredRegister.Items`，注册时返回 `DeferredBlock<T>` / `DeferredItem<T>`。**建议不用**，以保持与源工程 1:1 的代码结构，降低 review 成本。
- `ForgeRegistries.ITEMS.tags()`（`RuntimeLootAstSource:250`）→ `BuiltInRegistries.ITEM.getTagNames()` / `getTag(...)`。
- `RegistryObject` 的 `.get()` 语义不变，`.getEntries()` 在 `DeferredRegister` 上仍可用（datagen 用到）。

### 3.3 模组入口与事件总线

- `FMLJavaModLoadingContext` **已删除**。入口构造签名改为 `public DimensionTechMod(IEventBus modEventBus, ModContainer modContainer)`（目标工程的示例文件即为此形态）。
- `MinecraftForge.EVENT_BUS` → **`NeoForge.EVENT_BUS`**（`net.neoforged.neoforge.common.NeoForge`）。
- `@Mod.EventBusSubscriber` → **`net.neoforged.fml.common.EventBusSubscriber`**（`net.neoforged.bus.api.SubscribeEvent`）。
- 事件类路径整体迁移：

| 1.20.1 | 1.21.1 |
| --- | --- |
| `net.minecraftforge.event.TickEvent.ServerTickEvent`（带 `phase`） | `net.neoforged.neoforge.event.tick.ServerTickEvent.Pre` / `.Post`（**无 `phase` 字段**，拆成两个事件类） |
| `TickEvent.ClientTickEvent` | `net.neoforged.neoforge.client.event.ClientTickEvent.Pre` / `.Post` |
| `PlayerInteractEvent.RightClickBlock` | `net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock`（保留）；同时存在新的 `UseItemOnBlockEvent` |
| `ServerStoppingEvent` | `net.neoforged.neoforge.event.server.ServerStoppingEvent` |
| `RenderLevelStageEvent` | `net.neoforged.neoforge.client.event.RenderLevelStageEvent` |
| `RegisterKeyMappingsEvent` | `net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent` |
| `RegisterColorHandlersEvent.Item` | `net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item` |
| `GatherDataEvent` | `net.neoforged.neoforge.data.event.GatherDataEvent` |
| `net.minecraftforge.gametest.GameTestHolder` / `PrefixGameTestTemplate` | `net.neoforged.neoforge.gametest.GameTestHolder` / `PrefixGameTestTemplate`（7 个文件用到） |

`TickEvent.Phase.END` 判断被事件类型取代，`StructureAnalysisService.onServerTick` 与 `ChestMarkerKeyHandler.onClientTick` 的逻辑要按 `Post` 事件重写。
`DistExecutor` **已删除**：`ModNetwork` 里 6 处 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` 必须改为把客户端处理体抽到独立的 client-only 类，用 `FMLEnvironment.dist.isClient()` 守卫 + `IPayloadContext.enqueueWork(...)`。

### 3.4 配置

- `net.minecraftforge.common.ForgeConfigSpec` → **`net.neoforged.neoforge.common.ModConfigSpec`**（API 形状一致：`Builder` / `defineInRange` / `EnumValue` / `ConfigValue` / `defineListAllowEmpty` 全部保留，`ModConfigs` 的 500 行只有类型名需要替换）。
- 注册方式：`FMLJavaModLoadingContext.registerConfig(...)` → **`modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC)`**，由入口注入的 `ModContainer` 调用。
- `net.neoforged.fml.config.ModConfig`（`Type` 枚举）路径保留。
- 配置指纹（`calculationFingerprint` / `discoveryFingerprint`）逻辑本身不依赖版本，但 Phase 7 会因语义版本变化而需要 bump（见 3.9）。

### 3.5 Capability

这是 `block/entity` 层最重的改动。1.20.1 的 `ICapabilityProvider` + `LazyOptional` 模式整体废弃：

| 1.20.1 | 1.21.1 |
| --- | --- |
| `ForgeCapabilities.ENERGY` | `Capabilities.EnergyStorage.BLOCK`（`BlockCapability<IEnergyStorage, Direction>`）、`.ITEM` |
| `ForgeCapabilities.ITEM_HANDLER` | `Capabilities.ItemHandler.BLOCK` / `.ENTITY` / `.ITEM` |
| `ForgeCapabilities.FLUID_HANDLER` | `Capabilities.FluidHandler.BLOCK` |
| `ForgeCapabilities.FLUID_HANDLER_ITEM` | `Capabilities.FluidHandler.ITEM` |
| `ICapabilityProvider.getCapability(...)` + `LazyOptional<T>` | 删除 provider；`level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)` 直接返回 `@Nullable T` |
| `CapabilityManager.get(new CapabilityToken<>(){})` | 不再需要 |
| 无 | 新增 `RegisterCapabilitiesEvent`，在 mod 事件总线上注册 `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TYPE, (be, side) -> ...)` |

具体受影响文件：

- [BaseMinerBlockEntity.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/block/entity/BaseMinerBlockEntity.java)：4 个 `LazyOptional` 字段 + `getCapability` 覆写（约 1124-1160 行）+ `invalidateCaps` 区域，全部移除，改为集中注册。这是全工程最大的单点改造。
- [StructureReactorBlockEntity.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/block/entity/StructureReactorBlockEntity.java)：3 个 `LazyOptional` 字段 + 按朝向的 `faceFluidCapabilities` Map + `getCapability`（947 行）。**注意**：按朝向返回不同 handler 的逻辑必须改为 `RegisterCapabilitiesEvent` 里的 `(blockEntity, side) -> ...` 分支，语义要逐个朝向核对，不能丢面。
- 相邻方块取能力：`MinerOutputController:67`、`StructureReactorBlockEntity:421/455`、`StructureMinerOutputRouter:64` → `level.getCapability(...)`，返回值从 `LazyOptional` 变成可空引用，所有 `.ifPresent` / `.orElse` / `.resolve()` 调用点重写。
- 手持物品取能力：`BaseMinerBlock:189`、`StructureReactorBlock:179` → `stack.getCapability(Capabilities.FluidHandler.ITEM)`，去掉 `.resolve()`。
- `net.minecraftforge.energy.IEnergyStorage` → **`net.neoforged.neoforge.energy.IEnergyStorage`**（方法签名一致：`receiveEnergy` / `extractEnergy` / `getEnergyStored` / `getMaxEnergyStored` / `canExtract` / `canReceive`）。[EnergyContainer.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/energy/EnergyContainer.java) 只需换 import；`SimpleEnergyContainer` 纯逻辑不动。
- `net.minecraftforge.items.IItemHandler` / `ItemStackHandler` / `SlotItemHandler` / `ItemHandlerHelper` → `net.neoforged.neoforge.items.*`。
- `net.minecraftforge.common.util.Lazy` → `net.neoforged.neoforge.common.util.Lazy`（`ModFluids` 用到）。

### 3.6 网络层（整层重写）

`SimpleChannel` + `NetworkRegistry.newSimpleChannel` + 基于 `FriendlyByteBuf` 的 Raw 编解码 **已删除**。共 15 个 packet，全部需要重写为新模型：

- 新模型：每个 packet 实现 `CustomPacketPayload`，暴露 `Type<T> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "name"))` 和 `StreamCodec<? super RegistryFriendlyByteBuf, T> STREAM_CODEC`。
- 注册：mod 事件总线监听 `RegisterPayloadHandlersEvent`，用 `event.registrar("1").playToServer(TYPE, CODEC, handler)` / `.playToClient(...)` / `.playBidirectional(...)`。原 `VERSION = "10"` 的语义由 `registrar("...")` 的参数承担。
- 编解码迁移：`FriendlyByteBuf.writeEnum/readEnum/writeBlockPos/writeResourceLocation/writeVarInt/writeItem/writeDouble/writeBoolean/writeNullable` 在 `RegistryFriendlyByteBuf` 上仍然可用，逐字段平移即可。`writeItem/readItem` 需要 `RegistryFriendlyByteBuf`（新模型天然满足），这正是换新模型的一个附带收益。
- 处理体：`Supplier<NetworkEvent.Context>` → `(T payload, IPayloadContext context)`；`context.getSender()` → `context.player()`（返回 `Player`，需 instanceof 收窄到 `ServerPlayer`）；`context.setPacketHandled(true)` 不再需要，由框架 `enqueueWork` 保证主线程执行。
- **不要**再手写 `nextId++` 序号，`Type` 的 ResourceLocation 就是身份。
- 发送侧：`CHANNEL.sendToServer(p)` 保留语义但换 API；`CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), p)` → `PacketDistributor.sendToPlayer(player, p)`（`PacketDistributor` 仍在 `net.neoforged.neoforge.network`）。
- 15 个 packet 清单（全部在 [ModNetwork.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/network/ModNetwork.java) 内以私有 record 形式定义）：`StructMarkerActionPacket`、`RefreshedMarkerPacket`、`StructureChoicesPacket`、`StructureMinerAnalysisRequestPacket`、`StructureMinerAnalysisPacket`、`StructureMinerExpectedItemTogglePacket`、`StructureMinerSlotTogglePacket`、`StructureReactorTooltipPacket`、`OperatorCatalogueRequestPacket`、`OperatorCataloguePacket`、`OperatorAnalysisRequestPacket`、`OperatorAnalysisPacket`、`OperatorActionPacket`、`ChestAnalysisRequestPacket`（14 个）+ `MarkerAction` 作为枚举内嵌在 `StructMarkerActionPacket` 中。
- 结构建议：把 15 个 record 拆到 `network/payload/` 下的独立文件，每个文件自带 `TYPE` + `STREAM_CODEC` + `handle`；`ModNetwork` 只保留发送侧静态方法和注册入口。理由是 899 行单文件里混杂了 15 套编解码 + 业务逻辑，1.21 的注册模型要求每个 payload 有独立稳定身份，继续堆在一个文件里会让注册表与实现脱节。
- `readCandidates` 里对越界计数的显式校验（`IllegalArgumentException`）要保留 —— 这是对恶意包的防护，不是冗余代码。

### 3.7 流体与物品容器

- `net.minecraftforge.fluids.ForgeFlowingFluid` → **`net.neoforged.neoforge.fluids.BaseFlowingFluid`**（`Source` / `Flowing` 内部类同名）。
- `BaseFlowingFluid.Properties(Supplier<FluidType>, Supplier<Fluid> still, Supplier<Fluid> flowing)` 构造签名与 1.20.1 一致；`.slopeFindDistance(3)` / `.levelDecreasePerBlock(2)` / `.bucket(Supplier<Item>)` 全部保留。
- `FluidType` 注册表：`ForgeRegistries.Keys.FLUID_TYPES` → **`NeoForgeRegistries.Keys.FLUID_TYPES`**（`ResourceKey<Registry<FluidType>>`）。`DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MOD_ID)` 即可。
- `net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions` → **`net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions`**；`FluidType.initializeClient(Consumer<...>)` 机制保留，`EssenceFluidType` 的贴图与 tint 覆盖可直接平移。
- `net.neoforged.neoforge.fluids.FluidStack`、`capability.IFluidHandler`（`FluidAction` 枚举）、`capability.IFluidHandlerItem`、`capability.templates.FluidTank` 路径变更。
- **`FluidTank` 的 NBT 序列化签名变了**：`writeToNBT(CompoundTag)` → `writeToNBT(HolderLookup.Provider, CompoundTag)`；`readFromNBT(CompoundTag)` → `readFromNBT(HolderLookup.Provider, CompoundTag)`。调用点 `BaseMinerBlockEntity:1024/1066` 需要把 provider 一路传进来 —— 1.21 的 `BlockEntity.loadAdditional(CompoundTag, HolderLookup.Provider)` 正好提供了它，字段要接着传而不是从 `level` 反查（`level` 在 load 期可能为 null）。
- `ItemStackHandler`：`serializeNBT()` → `serializeNBT(HolderLookup.Provider)`，`deserializeNBT(CompoundTag)` → `deserializeNBT(HolderLookup.Provider, CompoundTag)`。同样把 provider 从 `loadAdditional` / `saveAdditional(HolderLookup.Provider, CompoundTag)` 传入。
- 流体桶物品的 `BucketItem` 与 `DynamicFluidContainerModel` 仍在 `net.neoforged.neoforge.client.model`；`ModItemModelsProvider:65` 引用的 `new ResourceLocation("forge", "item/bucket")` 需要改为 NeoForge 的命名空间（核对 1.21.1 的 `neoforge:item/bucket`）。

### 3.8 ItemStack 数据模型：NBT → DataComponents（本计划最大的数据面改动）

`ItemStack.getTag()` / `getOrCreateTag()` / `setTag()` / `hasTag()` / `getTagElement()` / `getOrCreateTagElement()` / `removeTagKey()` **在 1.21 已全部删除**，物品数据只剩 DataComponents。

源工程对标记物品的整条数据链都建立在 `StructureMarkerData` 这个 CompoundTag 上，散落在 14+ 个调用点：

- 写入：[StructMarkerItem.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/item/StructMarkerItem.java)（`markAt`/`refreshAnalysis`/`clearMarker`）、[ChestMarkerItem.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/item/ChestMarkerItem.java)
- 读取：[StructureDataOperatorBlockEntity.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/block/entity/StructureDataOperatorBlockEntity.java)（163-363 行，复制/清除/写入槽位）
- 展示：`StructureDataOperatorOperationPage:282` 用 `Objects.hashCode(marker.getTag())` 做变更检测
- 清理：[FullDurabilityLoot.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/utils/FullDurabilityLoot.java)（`result.getTag()` / `result.setTag(null)`）
- GameTest：`LootAnalysisFingerprintGameTests`、`StructMarkerItemGameTests`、`StructureMinerLootMergeGameTests`（多个用于断言顺序的临时 tag）

**处置方案**：新增一个自定义 DataComponent，而不是继续塞 CompoundTag。

1. 新建 `item/StructureMarkerData.java`（record + `Codec` + `StreamCodec`），字段与现有 tag 常量一一对应：`dimension`、`position`、`structure`（含 bounds）、`dimensionValue`、`structureValue`、`expectedItemCounts`（`Map<ResourceLocation, ExactProbability>` 的分数表示）、`analysisStatus`、`diagnostics`、`algorithmVersion`、`randomProbabilitySpace`、`analysisFingerprint`。
2. 新建 `item/ModDataComponents.java`：`DeferredRegister<DataComponentType<?>>` 注册到 `Registries.DATA_COMPONENT_TYPE`，用 `DataComponentType.<StructureMarkerData>builder().persistent(Codec).networkSynchronized(StreamCodec).build()`。`persistent` 必须有（存档），`networkSynchronized` 必须有（标记会随 `RefreshedMarkerPacket` / `OperatorAnalysisPacket` 走网络）。
3. `markAt` / `clearMarker` / `getMarkerInfo` 等方法改为 `stack.set(ModDataComponents.STRUCTURE_MARKER, data)` / `stack.remove(...)` / `stack.get(...)`。
4. **Codec 的字段名必须沿用现有 tag 名称**（`StructureMarkerData` / `Dimension` / `Position` / `Structure` / `DimensionValue` / `StructureValue` / `ExpectedItemCounts` / `AnalysisStatus` 等），这样 Phase 7 的存档迁移器可以把 1.20.1 的 tag 反序列化成同一个 Codec，不需要第二套字段定义。
5. 存档迁移：1.21 的 `ItemStack.CODEC` 在读到旧的 `tag.StructureMarkerData` 时不会有任何提示。是否提供一次性迁移（在 `ItemStack` 解析入口挂 `DataFixer`，或提供 `/dimensiontech migrate` 命令遍历在线玩家背包）属于新增功能；**本计划默认不提供**，理由：模组尚未发布 1.21.1 版本、无历史存档需要承接（见 1.2 范围外）。若确有 1.20.1 存档要继承，需单独立项。
6. `FullDurabilityLoot` 中"清除物品 NBT 以恢复满耐久"的意图，在 1.21 应改为清除对应组件：`stack.remove(DataComponents.DAMAGE)`（并核对 `DataComponents.REPAIR_COST`、`ENCHANTMENTS` 是否也要清）。
7. `StructureDataOperatorOperationPage:282` 的哈希变更检测改为对 `StructureMarkerData` record 求 `hashCode()`（record 的 `hashCode` 由组件决定，语义比 `CompoundTag.hashCode` 更稳定，正好修正了原来依赖 NBT 无序哈希的隐患）。

### 3.9 Loot 精确语义引擎（Phase 7 主体）

用户已确认按 1.21.1 原版重写。以下是已核实的破点，需要在 Phase 7 逐条落地方案。

**(a) 战利品表访问层被替换**

[RuntimeLootAstSource.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/loot/expectation/RuntimeLootAstSource.java) 依赖三件在 1.21 已不存在的东西：

- `server.getLootData()` 返回的 `LootDataManager` —— 已删除。
- `LootDataManager.getElementOptional(LootDataType.TABLE, id)` —— 已删除。
- `Deserializers.createLootTableSerializer()/createConditionSerializer()/createFunctionSerializer()` 这套 Gson 序列化器 —— 已删除。1.21 的战利品表通过 `Codec` + `RegistryOps` 序列化，且**附魔等字段是 holder，Gson 无法还原**。

替换方案：`server.registryAccess().lookupOrThrow(Registries.LOOT_TABLE)` 取注册表；序列化用 `LootTable.DIRECT_CODEC` / `LootItemCondition.DIRECT_CODEC` / `LootItemFunctions.ROOT_CODEC`，配 `RegistryOps.create(JsonOps.INSTANCE, registryAccess)`。这要求 `RuntimeLootAstSource` 全程持有 `HolderLookup.Provider`，`runtimeSemanticsFingerprint()` 也要把 registry access 纳入（附魔注册表内容现在参与语义）。

`Registries.LOOT_TABLE` 的注册表键在 1.21 已由 `loot_tables` 改名为 **`loot_table`**（同理 `predicate` / `item_modifier`），这直接影响第 3.11 节的资源目录改名。

**(b) 附魔语义彻底改变**

- `EnchantmentHelper.enchantItem(RandomSource, ItemStack, int, boolean)` 与 `getAvailableEnchantmentResults(int, ItemStack, boolean)` **在 1.21.1 已不存在**。现有调用点：`ExactEnchantmentSemantics1201:378/633/752/982`、`StatefulFunction1201:288`。
- 1.21 的附魔是数据驱动注册表（`Registries.ENCHANTMENT`），`Enchantment` 变成 record，`EnchantmentInstance` 持 `Holder<Enchantment>`，附魔存储落在 `DataComponents.ENCHANTMENTS`（`ItemEnchantments`）。
- `BuiltInRegistries.ENCHANTMENT` **在 1.21 是空的** —— 附魔不在内置注册表里。`ExactEnchantmentSemantics1201:919`、`StatefulFunction1201:488`、`DistributionalFunction1201:919` 里对 `BuiltInRegistries.ENCHANTMENT.stream()` 的迭代会静默返回空集，必须改为 `registryAccess().lookupOrThrow(Registries.ENCHANTMENT)`。
- `stack.hasTag() && stack.getTag().contains("Enchantments")`（`ExactEnchantmentSemantics1201:688/691`）→ `stack.has(DataComponents.ENCHANTMENTS)`。
- 结论：`ExactEnchantmentSemantics1201`（1071 行）不是改 import，而是**对照 1.21.1 的附魔实现重写**。附件 `ExactEnchantmentSemantics1211` 需要重新设计"可用附魔候选集"的来源：1.21 的附魔可用性来自 `Enchantment` 的 definition（`supported_items` / `primary_items`、`weight`、`max_level`、`exclusive_set`），且相关数据在 `Enchantment` record 里而不是注册表旁边。

**(c) RandomSource 与 SavedData**

- `LegacyState1201` / `XoroshiroState1201` / `StatefulLegacyRandomSource1201` 是自实现的状态机，**不调用原版**，只要 1.21.1 的 `LegacyRandomSource` / `XoroshiroRandomSource` 数值语义未变（这两者跨版本稳定），改名的成本只是类型名不再需要 `1201` 后缀。核对点：`BitRandomSource.nextInt(int)` 的拒绝采样实现、`nextDouble` 的 26+27 bit 组合 —— 两个版本一致，保留原实现即可。
- [SavedDataTransaction1201.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/loot/expectation/SavedDataTransaction1201.java) 用**反射**读 `DimensionDataStorage` 的私有 cache 字段来快照/回滚地图 SavedData。已核实 1.21.1 的 `DimensionDataStorage` 仍然存在，私有字段名仍为 `cache`（`private final Map<String, SavedData> cache`），`MapIndex` 也仍存在 —— 反射目标不变。但这是运行时才暴露的脆弱点，**必须有 GameTest 覆盖**（见 6.3），失败时抛出的 `IllegalStateException("Cannot find the Minecraft 1.20.1 DimensionDataStorage cache field")` 文案要改成 1.21.1。

**(d) ItemStack 解析与产物构造**

- `ItemStack.of(CompoundTag)` **已删除**。调用点：`MinerOutputController:214`（读回合并后的产出）、`StructureReactorBlockEntity:919`（读回预留碎片）、`ExactEnchantmentSemantics1201:1095`。
  → `ItemStack.parseOptional(HolderLookup.Provider, CompoundTag)`；或改用 `ItemStack.CODEC` + `RegistryOps` 走 JSON/NBT 双向路径。`StructureReactorBlockEntity` 的序列化侧同时要把 `stack.save(CompoundTag)` 换成 `ItemStack.CODEC`/`parseOptional` 配对。
- `StatefulFunction1201:365` 写 `instrument` NBT → 1.21 应写 `DataComponents.INSTRUMENT`。
- `ExactEnchantmentSemantics1201` 中为求精确而对 `ItemStack` 做 NBT copy 后 `ItemStack.of(serialized)` 往返的做法，在 1.21 应改为直接 `stack.copy()`（组件已不可变，浅拷贝足够）—— 但**必须逐个调用点核对**：原写法依赖"NBT 往返丢弃了某些运行时状态"这一副作用，直接 `copy()` 可能把这些状态一起带过来，属于语义变化。

**(e) 指纹与算法版本**

- `StructMarkerItem.ALGORITHM_VERSION = 5` → **`6`**。这是显式声明语义变化，让旧标记被判定为需要重算。
- `LootAnalysisFingerprint`、`RuntimeLootAstSource.runtimeSemanticsFingerprint()`、`ModConfigs.calculationFingerprint()` 的输入集合都要重新定义：现在必须包含附魔注册表内容、`HolderLookup.Provider` 标识、registry access。
- `StructMarkerItem.refreshAnalysisIfNeeded` 已经是"指纹不匹配就重算"的逻辑，bump 版本号后既有标记会自动重算 —— 不需要额外迁移代码。
- 文件重命名 `*1201` → `*1211` 是**编译期可验证**的动作，同时用名字标明"这套语义只对 1.21.1 精确"。

### 3.10 客户端 GUI / 渲染

好消息：`GuiGraphics` 在 1.21.1 保留了源工程用到的全部重载。

- `blit(ResourceLocation, x, y, blitOffset, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight)` **保留**（`ChestMarkerScreen`、`StructureMinerScreen:848` 等 16 处调用无需改）。
- `blit(int x, int y, int blitOffset, int w, int h, TextureAtlasSprite)` **保留**（`StructureMinerScreen:832`，Note: 该重载在 1.21 内部委托 `blitSprite`）。
- `blit(ResourceLocation, int, int, int, int, int, int)`、`drawString`、`drawCenteredString`、`renderTooltip(Font, ItemStack, int, int)`、`enableScissor` / `disableScissor` / `setColor` / `fill` / `pose()` 全部保留。
- **渲染类型**：`ItemBlockRenderTypes.setRenderLayer(Block, RenderType)` 在 1.21.1 仍存在但已 `@Deprecated(since="1.19")`，官方指引是写在方块模型的 JSON 里。处置：删掉 [ClientModEvents.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/client/ClientModEvents.java) 中 3 处 `setRenderLayer`，改为在对应 blockstate/model JSON 里加 `"render_type": "cutout"`（反应堆、数据操作仪）和 `"translucent"`（采掘器玻璃）。因为这几处模型是手写在 `src/main/resources/assets/dimension_tech/` 下的（`ModBlockStateProvider` 用 `getExistingFile` 引用），改动只在 JSON，不涉及 datagen。
- `MenuScreens.register(...)`、`RegisterKeyMappingsEvent`、`KeyMapping` 本身兼容。
- `DynamicFluidContainerModel.Colors` + `RegisterColorHandlersEvent.Item` 兼容（路径换到 `net.neoforged.neoforge.client`）。
- `Dist.CLIENT` 注解值 → `net.neoforged.api.distmarker.Dist`（注意不是 `net.minecraftforge.api.distmarker`）。
- `IForgeMenuType.create(factory)` → **`IMenuTypeExtension.create(factory)`**（`net.neoforged.neoforge.common.extensions`），工厂参数类型由 `FriendlyByteBuf` 变为 `RegistryFriendlyByteBuf`，3 个 `Menu` 构造函数签名要跟着改。
- `NetworkHooks.openScreen(player, blockEntity, pos)` → **`player.openMenu(menuProvider, pos)`**（`IPlayerExtension` 提供，内部即 `openMenu(menuProvider, buf -> buf.writeBlockPos(pos))`）。3 处：`BaseMinerBlock:202`、`StructureDataOperatorBlock:92`、`StructureReactorBlock:187`。

### 3.11 资源与数据包格式

1.21 把若干注册表的目录名从复数改成单数，目录名 == 注册表键路径：

| 目录 | 1.20.1 | 1.21.1 | 影响 |
| --- | --- | --- | --- |
| 战利品表 | `loot_tables/` | `loot_table/` | `src/main/resources/data/dimension_tech/loot_tables/`（16 个）+ `src/generated/.../loot_tables/`（47 个） |
| 谓词 | `predicates/` | `predicate/` | `src/main/resources/data/dimension_tech/predicates/`（1 个） |
| 物品修饰器 | `item_modifiers/` | `item_modifier/` | `src/main/resources/data/dimension_tech/item_modifiers/`（1 个） |
| 配方 | `recipes/` | `recipe/` | `src/generated/.../recipes/`（52 个） |
| 进度 | `advancements/` | `advancement/` | `src/generated/.../advancements/`（52 个） |

处置：`generated` 下的 151 个文件全部**删掉重新 datagen**（Phase 8），不要手工改目录名 —— 手工改会和 `RecipeOutput` 的写入路径不一致，下次 datagen 又会出现两套。`main/resources` 下的 18 个文件是手写资产，需要 `git mv` 改名。

其他资源点：

- `ResourceLocationHelper.lootTable(path)` 返回的路径字符串含 `loot_tables/`，要改成 `loot_table/`（虽然它只是拼字符串，但语义上必须对齐）。`recipe(name)` 同理。
- `pack.mcmeta` 见 3.1。
- `kubejs.plugins.txt` 需随 KubeJS 1.21 的插件发现机制一起核对。

### 3.12 DataGen

- `GatherDataEvent` 路径变更（见 3.3）；`event.getGenerator()` / `getPackOutput()` / `includeServer()` / `includeClient()` / `getExistingFileHelper()` 保留。
- `ExistingFileHelper` → `net.neoforged.neoforge.common.data.ExistingFileHelper`。
- `LanguageProvider` → `net.neoforged.neoforge.common.data.LanguageProvider`（`ModEnusLangProvider` / `ModZhcnLangProvider`，构造签名 `(PackOutput, modid, locale)` 不变）。
- `BlockStateProvider` / `ItemModelProvider` / `ModelFile` / `VariantBlockStateBuilder` → `net.neoforged.neoforge.client.model.generators.*`（`BlockStateProvider(PackOutput, String modid, ExistingFileHelper)` 不变）。
- **`RecipeProvider` 签名变了**：构造函数现在要求 `(PackOutput, CompletableFuture<HolderLookup.Provider>)`；`buildRecipes` 由 `Consumer<FinishedRecipe>` 变为 **`RecipeOutput`**。[ModRecipesProvider.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/datagen/ModRecipesProvider.java) 里约 40 处 `writer`/`Consumer<FinishedRecipe>` 参数与 `ShapedRecipeBuilder.save(writer)` 调用要全量改类型。`ShapedRecipeBuilder` 在 1.21 引入 `ShapedRecipePattern` 但 builder API 表面不变。
- **`BlockLootSubProvider` 构造函数变了**：还要传 `HolderLookup.Provider`。[ModBlockLootTablesProvider.java](file:///d:/mycode/ModDevelopment/DimensionTech-1.20.1/src/main/java/com/suntide_20210418/dimensiontech/datagen/ModBlockLootTablesProvider.java) 的 `super(Set.of(), FeatureFlags.REGISTRY.allFlags())` 要补 provider 参数；`generate()` / `getKnownBlocks()` 逻辑不变。
- `IConditionBuilder` → `net.neoforged.neoforge.common.conditions.IConditionBuilder`。
- `DatagenExitWatchdog` 是纯 JDK 逻辑（线程 + 反射），**无需改动**。它解决的是 KubeJS 非守护线程导致 datagen JVM 不退出，1.21.1 的 KubeJS 大概率仍有同样问题，保留。

### 3.13 集成层

| 集成 | 文件 | 关键变更 |
| --- | --- | --- |
| JEI | 15 个 / ~1230 行 | 1.21.1 的 JEI API 与 1.20.1 的 15.x 差异较小：`ITooltipBuilder`、`IRecipeExtrasBuilder`、`AbstractRecipeCategory` 已在用（`mods.toml` 已声明 `[15.20.0,)`）。主要核对 `IRecipeSlotBuilder`、`IGuiHandlerRegistration` 签名，以及 `Ingredient.of(ItemLike)` 在 1.21 的可用性 |
| Jade | 2 个 / 347 行 | `IElementHelper` / `BoxStyle` / `IProgressStyle` / `IThemeHelper` 的构造与 `ITooltip` 写入方式在 Jade 1.21 有调整；`@WailaPlugin` 与 `IWailaCommonRegistration` / `IWailaClientRegistration` 注册方法名需逐个核对 |
| AE2 | 1 个 / 95 行 | `InterfaceBlockEntity` 的包路径（`appeng.blockentity.misc`）与 `AEItemKey` / `AEFluidKey` / `Actionable` / `IActionSource` 在 AE2 1.21 有重排；`Ae2Integration` 是最小的一个，风险可控 |
| KubeJS | 4 个 / 626 行 | 改动最大：`KubeJSPlugin` 的生命周期回调、`EventGroup` 注册、`AttachedData`、`BindingsEvent`、`ScriptType`、`IngredientJS`、`ClassFilter` 在 KubeJS 1.21 均有变动。`kubejs.plugins.txt` 的插件发现机制也要核对 |

KubeJS 集成的对外契约记录在 `docs/kubejs.md`（源工程），移植后要保证脚本 API 表面不退化。

---

## 4. 分阶段执行步骤

原则：**每阶段结束时工程必须能 `./gradlew compileJava` 通过**（Phase 6/8/9 例外，允许局部不编译但必须显式记录）。先把编译面打通，再逐个恢复语义。

### Phase 0 — 基线与回退点

1. 在目标工程建 git 仓库并提交当前 MDK 骨架，作为 Phase 1 的回退点。
2. 记录源工程当前 commit：`git -C D:\mycode\ModDevelopment\DimensionTech-1.20.1 rev-parse HEAD`。移植期间**源工程冻结**，任何修复先在 1.20.1 落地再同步过来，避免两边漂移。
3. 把源工程 `docs/code-wiki.md` 复制到目标工程的 `docs/`（作为移植期的架构参照手册）。

产出：目标工程有可回退基线；源工程 HEAD 已记录。

### Phase 1 — 构建与元数据落地

1. 逐条核验 [build.gradle](file:///d:/mycode/ModDevelopment/DimensionTech-1.21.1/build.gradle) 里 13 个 CurseMaven 依赖是否真的对应 MC 1.21.1，把版本号移到 `gradle.properties` 集中管理（例如 `jei_version=...`）。
2. 从源 `build.gradle` 移植两段丢失的逻辑：
   - `vanillaLootRuntime` Gradle 属性 + `optionalModConfiguration`（`datagen` / `vanillaLootRuntime` 运行时把可选模组降级为 `compileOnly`）。这是 loot 语料门禁的前提，不能丢。
   - `syncGameTestStructures` 任务 + `prepareRunGameTestServer` 依赖。
3. 补 `testImplementation`（JUnit）与 `tasks.named('test', Test) { usePlatform() }`（如果保留 JUnit 测试；源工程 `src/test` 实际为空，可先不加）。
4. 移植或舍弃 `spotless`。源工程的 `googleJavaFormat().aosp().reflowLongStrings()` 是统一格式的来源，**建议保留**，否则 Phase 2-9 的大量机械改动会产生不可读的 diff。
5. 配置 `neoForge.runs`：目标已有 client/server/gameTestServer/data。补上源工程的差异化设置：`gameTestServer` 需要 `dimension_tech.vanilla_loot_runtime` 系统属性、独立的 `run/vanilla-loot-gametest` 与 `run/gametest` 工作目录；data run 的 `--existing` / `--output` 参数对齐。
6. `neoforge.mods.toml`：补 `ae2` / `jade` / `jei` 三个 `type="optional"` 依赖项（含 `versionRange` 与 `ordering="AFTER"`），把 `mod_version` 改为 `1.0.0-1.21.1`，`description` / `authors` 对齐源工程。
7. 删除 `pack.mcmeta`（见 3.1），同步删掉源 `processResources` 里对它的展开。
8. 决定 GuideME / Mekanism / Architectury / GeckoLib / ATO / Time in a Bottle / AllTheModium 的去留：源码里只有 AllTheModium 出现在配置默认值注释中（`allthemodium:the_other`），其余 6 个无 import。**建议删掉无 import 的 6 个**，只留 `kubejs / rhino / jade / ae2 / jei / allthemodium / mouse-tweaks`，减少移植期的类路径污染。

验证：`./gradlew build` 通过（此时只有 3 个示例文件）；`./gradlew runData` 能启动并正常退出。

### Phase 2 — 入口、注册骨架、配置

1. 删除示例文件 `DimensionTech.java` / `Config.java` / `DimensionTechClient.java`。
2. 迁移 `com.suntide_20210418.dimensiontech` 整个包树（205 个文件）到目标 `src/main/java`。
3. 重写 `DimensionTechMod`：新构造签名 `(IEventBus, ModContainer)`，`NeoForge.EVENT_BUS`，把 `ModConfigs.register(context)` 改为 `modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigs.COMMON_SPEC)`，`commonSetup` 里的 `event.enqueueWork(...)` 保留（`FMLCommonSetupEvent` 语义未变）。
4. 机械替换注册层（约 8 个文件）：
   - `ModBlocks`：`ForgeRegistries.BLOCKS` → `Registries.BLOCK`；`RegistryObject<Block>` → `DeferredHolder<Block, Block>`。注意 `UPGRADE_*_TIERS` 是 `RegistryObject<Block>[]` 数组，泛型要全改。
   - `ModItems`：同上（`Registries.ITEM`），含 `blockItem` / `fluidBucket` / `tieredItems` / `upgradeItems` 的辅助方法签名。
   - `ModBlockEntities`：`Registries.BLOCK_ENTITY_TYPE`。**注意** `BlockEntityType.Builder.of(...).build(null)`：1.21 的 `build()` 参数类型已变，核对是否需要 `build(null)` 还是省略参数。
   - `ModCreativeModeTabs`：`Registries.CREATIVE_MODE_TAB`；`CreativeModeTab.builder()` 无参重载在 1.21.1 **仍存在**（已核实），可保留；也可选 `builder(Row, int)`。建议保留 `builder()` 以减少改动，但要用 `withTabsBefore(...)` 显式定位（源工程未指定位置，1.21 下会排到末尾）。
   - `ModMenu`：见 3.10。
   - `ModFluids`：`NeoForgeRegistries.Keys.FLUID_TYPES` + `Registries.FLUID` + `BaseFlowingFluid`。
   - `ModRecipes`：`Registries.RECIPE_SERIALIZER`。
   - `ModDataComponents`（新建）：见 3.8。
5. `ModConfigs`：`ForgeConfigSpec` → `ModConfigSpec`（全局替换类型名即可，500 行 API 形状一致）。`ProcessResources` 里 `ForgeConfigSpec.ConfigValue<List<? extends String>>` 等泛型签名逐个核对。
6. `ResourceLocationHelper`：把 `new ResourceLocation(ns, path)` 改为 `ResourceLocation.fromNamespaceAndPath(ns, path)`（构造器在 1.21 已私有化）。**先改这个文件**，它收口了 2 处调用，剩下的 26 处零散调用在后续阶段随文件触碰时一并改。
7. 全量替换 import 三层：`net.minecraftforge.eventbus.api.IEventBus` → `net.neoforged.bus.api.IEventBus`；`net.minecraftforge.eventbus.api.SubscribeEvent` → `net.neoforged.bus.api.SubscribeEvent`；`net.minecraftforge.fml.common.Mod` → `net.neoforged.fml.common.Mod`。

验证：`./gradlew compileJava` 通过（`block/entity`、`network`、`client` 可能仍报错，属预期，记录清单进入下一阶段）。

### Phase 3 — ItemStack 数据模型

1. 新建 `item/StructureMarkerData.java`（record + Codec + StreamCodec），字段名沿用现有 tag 常量。
2. 新建 `item/ModDataComponents.java` 并注册。
3. 改造 `StructMarkerItem`（约 210 行起）与 `ChestMarkerItem`：所有 `getTagElement` / `getOrCreateTagElement` / `getOrCreateTag().put(...)` / `removeTagKey` 改为 `get` / `set` / `remove`。
4. 改造 `StructureDataOperatorBlockEntity` 的 163-363 行区域（复制/清除/写入槽位），它是标记数据在两个槽位之间搬运的唯一通道，字段级逐条核对。
5. 改造 `FullDurabilityLoot`（清 `DataComponents.DAMAGE`）。
6. 改造 `StructureDataOperatorOperationPage:282` 的哈希检测。
7. `ExactEnchantmentSemantics1201`（转 1211）与 `StatefulFunction1201` 里的 `stack` NBT 操作先在 Phase 7 处理，本阶段只保证编译。

验证：`./gradlew compileJava` 通过本模块；手动在客户端放一个标记，保存退出重进，确认标记数据仍在（组件已持久化）。

### Phase 4 — Capability 与流体

1. 新建 `block/entity/ModCapabilities.java`（或放在 `DimensionTechMod` 里），监听 `RegisterCapabilitiesEvent`，把 `BaseMinerBlockEntity` 与 `StructureReactorBlockEntity` 的 7 个 capability 一次性注册。
2. 删除两个 BlockEntity 里的 `LazyOptional` 字段、`getCapability` 覆写、`ICapabilityProvider` 实现、`invalidateCaps` 相关代码。**按朝向返回不同 fluid handler 的逻辑必须逐面平移**（`StructureReactorBlockEntity` 的 `FaceFluidAccess`）。
3. 改写相邻方块能力访问：`MinerOutputController:67`、`StructureReactorBlockEntity:421/455`、`StructureMinerOutputRouter:64` → `level.getCapability(...)` + 空引用判断。
4. 改写手持物品能力访问：`BaseMinerBlock:188`、`StructureReactorBlock:178`。
5. 流体：`FluidTank` / `FluidStack` / `IFluidHandler` 换包；`writeToNBT` / `readFromNBT` 补 `HolderLookup.Provider`；`ItemStackHandler` 的 `serializeNBT` / `deserializeNBT` 同样补 provider。
6. `EnergyContainer` 换 import 到 `net.neoforged.neoforge.energy`。
7. `Lazy` 换包（`ModFluids`）。

验证：`./gradlew compileJava` 通过；启动游戏，给采掘器接一个箱子与一个流体桶，确认能输出物品、能进出流体（capability 的端到端验证）。

### Phase 5 — 网络层重写

1. 新建 `network/payload/` 包，为 15 个 packet 各建一个文件，实现 `CustomPacketPayload`（`TYPE` + `STREAM_CODEC` + `handle(IPayloadContext)`）。
2. 新建 `network/NetworkHandler.java`：监听 `RegisterPayloadHandlersEvent`，`event.registrar("1")`，按方向 `playToServer` / `playToClient` 注册 15 个 payload。
3. 重写 `ModNetwork`：只保留发送侧静态方法与 `OperatorAction` / `MarkerAction` 枚举。发送侧 `CHANNEL.sendToServer(p)` → `PacketDistributor.sendToServer(p)`；`CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), p)` → `PacketDistributor.sendToPlayer(player, p)`。
4. 客户端处理体：把 6 处 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` 的内容抽到 `client/ClientPayloadHandlers.java`，用 `FMLEnvironment.dist.isClient()` 守卫。**守卫必须在客户端处理体内**，不能用 `Dist` 注解 —— 否则服务端加载 `ModNetwork` 时会尝试解析客户端类。
5. 保留 `readCandidates` 的越界校验与 `MAX_ITEMS` / `MAX_ROWS` 上限。

验证：`./gradlew compileJava` + `./gradlew runClient`；在客户端打开结构标记、数据操作仪目录、反应堆 tooltip、采掘器分析，逐个确认请求/响应往返正常（这是 capability 之后第二个端到端断点）。

### Phase 6 — 客户端 GUI、渲染类型、Menu

1. 全量替换 `net.minecraftforge.api.distmarker.Dist` → `net.neoforged.api.distmarker.Dist`、`@Mod.EventBusSubscriber` → `@EventBusSubscriber`。
2. `ClientModEvents`：删 3 处 `setRenderLayer`，改到 block model JSON 加 `render_type`；`FMLClientSetupEvent` + `enqueueWork` 保留；`RegisterKeyMappingsEvent` / `RegisterColorHandlersEvent.Item` 换包。
3. `ModMenu` → `IMenuTypeExtension.create(...)`；3 个 `Menu` 构造函数参数类型改 `RegistryFriendlyByteBuf`。
4. `NetworkHooks.openScreen` → `player.openMenu(provider, pos)`（3 处）。
5. 核对 16 处 `blit` 调用（结论是保留，但因为有 4 种不同重载，逐个确认参数个数与顺序匹配 1.21.1 的重载集合）。
6. `RenderLevelStageEvent` 换包（`StructureMinerProjectionClient:52`）。
7. `TickEvent.ClientTickEvent` → `ClientTickEvent.Post`（`ChestMarkerKeyHandler`，去掉 `phase` 判断）。

验证：`./gradlew runClient`，逐个打开 6 个 GUI 界面，确认贴图、滚动、tooltip、输入框、多方块投影渲染正常；反应堆与数据操作仪方块外观为 cutout（不出现黑底）；采掘器玻璃为半透明。

### Phase 7 — Loot 精确语义引擎重建（**工作量最大，独立分支**）

这是唯一需要"重新推导"而非"翻译"的阶段，建议单独开分支，按以下顺序推进：

1. **重命名与骨架**：`loot/expectation/*1201*` → `*1211*`（约 20 个文件），`IdealRandomProbabilitySpace1201`、`LegacyState1201`、`XoroshiroState1201`、`*Stateful*1201`、`*Distributional*1201`、`ExactEnchantmentSemantics1201`、`ExactRandomSemantics1201`、`ReferenceSemantics1201`、`SavedDataTransaction1201`、`PersistentRandomSequenceSnapshot1201` 全部改。同步改所有引用点与测试类名。
2. **接入层重建**：`RuntimeLootAstSource` 换掉 `LootDataManager` / `Deserializers`，改用 `registryAccess().lookupOrThrow(Registries.LOOT_TABLE)` + `RegistryOps.create(JsonOps, registryAccess)` + 各 `DIRECT_CODEC`。`runtimeSemanticsFingerprint()` 纳入附魔注册表与 registry access 标识。
3. **附魔语义重建**：对照 1.21.1 的 `Enchantment` / `EnchantmentHelper` / `ItemEnchantments` 重写 `ExactEnchantmentSemantics1211`。关键输入从"遍历内置注册表"改为"从 `registryAccess` 取附魔注册表 + 读 `Enchantment` 的 definition（`supported_items` / `weight` / `max_level` / `exclusive_set`）"。三个 `BuiltInRegistries.ENCHANTMENT` 迭代点必须全部改掉。
4. **ItemStack 往返**：`ItemStack.of` → `parseOptional(provider, tag)`；`instrument` NBT → `DataComponents.INSTRUMENT`；`Enchantments` NBT → `DataComponents.ENCHANTMENTS`。
5. **SavedData 反射校验**：核对 1.21.1 `DimensionDataStorage` 的 cache 字段名与 `MapIndex` 结构（已初步核实为 `cache`），更新错误文案里的版本号，靠 GameTest 兜底。
6. **RandomSource 语义核对**：`LegacyState1211` / `XoroshiroState1211` 是自实现，逐算符比对 1.21.1 的 `LegacyRandomSource` / `XoroshiroRandomSource`，确认无需改数值（预期不改）。
7. **版本号与指纹**：`ALGORITHM_VERSION` 5 → 6；重新定义 `LootAnalysisFingerprint` 输入；`ModConfigs.calculationFingerprint()` / `expectationFingerprint()` / `discoveryFingerprint()` / `generationFingerprint()` 逐个核对。
8. **纯 JVM 单元测试先行**：`FiniteDistribution`、`ExactProbability`、`ExpectationMath`、`LootExpectationSnapshot` 这些不依赖 MC 的类，应该在碰 MC 接入层之前先跑通，作为算法正确性的基线。

验证：见 6.3 的三条基线（精确语料回归、指纹稳定性、vanilla 语料门禁）。

### Phase 8 — DataGen 与资源格式

1. `ModDataGenerator`：`GatherDataEvent` 换包（`net.neoforged.neoforge.data.event.GatherDataEvent`），`@EventBusSubscriber` 换包。`DatagenExitWatchdog.install()` 保留。
2. `ModRecipesProvider`：构造函数补 `CompletableFuture<HolderLookup.Provider>`；`buildRecipes(Consumer<FinishedRecipe>)` → `buildRecipes(RecipeOutput)`；全量替换参数类型与 `.save(writer)` 调用（约 40 处）。
3. `ModBlockLootTablesProvider`：`super(...)` 补 `HolderLookup.Provider`；`getKnownBlocks()` 里 `RegistryObject::get` → `DeferredHolder::get`。
4. `ModBlockStateProvider` / `ModItemModelsProvider`：换包到 `net.neoforged.neoforge.client.model.generators.*`；`new ResourceLocation("forge", "item/bucket")` 改为 1.21.1 的 NeoForge 命名空间。
5. `ModEnusLangProvider` / `ModZhcnLangProvider`：`LanguageProvider` 换包。
6. **删除 `src/generated/resources` 下的 151 个文件**，用 `./gradlew runData` 全量重生成。不要手工改目录名。
7. 手工改 `src/main/resources/data/dimension_tech/` 下的 18 个文件目录：`loot_tables` → `loot_table`、`predicates` → `predicate`、`item_modifiers` → `item_modifier`；同步改文件内的 `dimension_tech:loot_tables/...` 引用字符串。
   - **目录改名只是表面，条目级字段也会改名**（收尾时实测踩到，见 `docs/code-wiki.md` §13 第 7 条）：1.21 把 `minecraft:loot_table` 条目的字段从 `name` 改成 **`value`**（`NestedLootTable.CODEC` = `Codec.either(ResourceKey.codec(Registries.LOOT_TABLE), LootTable.DIRECT_CODEC).fieldOf("value")`；原版 1.21.1 默认包 1178 张表里 `value` 34 次、`name` **0** 次）；`minecraft:set_lore` 新增必填的 `mode`（`ListOperation.codec(256)`，值是 `append` / `replace_all` / `replace_section` / `insert`）。**写错字段的后果是整张表加载失败且只在日志里以 `Couldn't parse element ... No key value in MapLike[...]` 出现**，游戏内表现为"这张表不存在"——所以每次改完手写 datapack JSON，都要在 `runGameTestServer` 日志里确认它真的加载成功。注意 `minecraft:item` / `minecraft:tag` / `minecraft:reference`（谓词与物品修饰器）的字段仍然是 `name`，不要一起改。
8. 在 3 个 block model JSON 里加 `render_type`（Phase 6 的对应项）。
9. 迁移 `kubejs.plugins.txt`（内容不变，核对 KubeJS 1.21 的读取路径）。

验证：`./gradlew runData` 正常结束；重生成的目录结构正确；`git status` 干净（说明生成结果稳定、可复现）。**这是 Phase 7 之外最重要的回归信号 —— datagen 不幂等通常意味着 Codec / registry 接入还有问题。**

### Phase 9 — 集成层

按风险从低到高逐个移植，每个集成独立提交、独立验证、独立可回退：

1. **AE2**（95 行）：换 `appeng.*` 包路径与 1.21 API。
2. **Jade**（347 行）：`IElementHelper` / `BoxStyle` / `IProgressStyle` / `ITooltip` 按 Jade 1.21 重写。
3. **JEI**（1230 行）：核对 `IRecipeLayoutBuilder` / `IRecipeSlotBuilder` / `IGuiHandlerRegistration` / `AbstractRecipeCategory` 签名；`Ingredient.of(ItemLike)` 在 1.21 的可用性。
4. **KubeJS**（626 行）：`KubeJSPlugin` 生命周期、`EventGroup`、`AttachedData`、`BindingsEvent`、`ScriptType`、`IngredientJS`、`ClassFilter` 全部按 KubeJS 1.21 重写；`kubejs.plugins.txt` 发现机制核对；`docs/kubejs.md` 记录的脚本 API 表面必须保持一致。

验证：每个集成单独启动一次（`runClient` + 对应模组在场），确认无 `NoClassDefFoundError` / `NoSuchMethodError`；KubeJS 需要跑一个脚本样例验证 API 表面。

### Phase 10 — 游戏测试与收尾

1. 迁移 7 个 GameTest 类：换 `GameTestHolder` / `PrefixGameTestTemplate` 的包；`ItemStackHandler` 换包；标记相关的断言改用组件。
2. `syncGameTestStructures` 任务的源目录 `src/test/resources/gameteststructures` 在源工程**不存在** —— 目标工程已补入 `empty.snbt`（**必需**：原版不提供 `minecraft:empty` 模板，缺它所有 GameTest 直接崩），任务与 `prepareGameTestServerRun` 的接线保持。
3. 跑完整回归（见第 6 节）。
4. 更新 `README.md` / `README.en.md` 的版本与运行环境段落（`1.20.1` → `1.21.1`，`Forge 47.4.10` → `NeoForge 21.1.251`）；更新 `docs/code-wiki.md` 中已失效的模块说明（网络层、注册层、loot 引擎命名）。
5. 删除移植期的临时分支与 TODO 注释。

---

## 5. 风险与注意事项

### 5.1 高风险

| 风险 | 说明 | 缓解 |
| --- | --- | --- |
| **loot 精确语义重建的隐蔽偏差** | Phase 7 是重新推导 1.21.1 语义，任何一处偏差都不会编译报错，只会让期望值静默算错。`*1201` 的文件名本身就是原作者的"语义版本即契约"设计 | 三条独立基线（6.3）+ 纯 JVM 单元测试先行 + `ALGORITHM_VERSION` bump 让偏差可被显式观测 |
| **`BuiltInRegistries.ENCHANTMENT` 在 1.21 为空** | 附魔已迁出内置注册表。3 处 `BuiltInRegistries.ENCHANTMENT.stream()` 会**静默返回空集**，不报错、不崩溃，只是期望值少算一堆附魔 | 逐点审计这三个调用；在引擎里加"附魔注册表非空"断言 |
| **Capability 按朝向分发** | `StructureReactorBlockEntity` 的 7 个 capability 里有按面不同的 fluid handler，改成 `RegisterCapabilitiesEvent` 后很容易漏面 | 保留 `FaceFluidAccess` 结构，只在注册回调里分派；用 GameTest 覆盖每个面的进出 |
| **ItemStack NBT → 组件的字段漂移** | 14+ 个调用点手工改造，字段名拼错不会编译报错（Codec 的字段名是字符串） | Codec 字段名沿用原 tag 常量做常量引用；`StructureMarkerData` 的 round-trip 单元测试 |
| **`RecipeProvider.buildRecipes` 签名变化波及 40 处** | 机械但量大，容易漏改导致 datagen 静默少生成配方 | Phase 8 重生成后用 `git status` 校验文件数（配方应为 52 个） |
| **datagen 非幂等** | 若 Codec / registry 接入有错，重生成会产出与上次不同的结果 | 连续跑两次 `runData`，`git status` 必须干净 |
| **KubeJS 1.21 API 变动面** | 626 行的集成，7 类 API 均变动；且 KubeJS 是外部脚本契约的承载者 | 单独排期、最后做；`docs/kubejs.md` 作为 API 表面验收清单 |

### 5.2 中风险

- **`DistExecutor` 删除后的客户端类隔离**：守卫必须写成方法体内的 `FMLEnvironment.dist.isClient()` 判断，或把客户端 handler 放在独立类里由 `ClientPayloadHandlers` 内引用。写成 `Dist` 注解式守卫会让专用服务器启动时类加载失败。
- **`FluidTank` / `ItemStackHandler` 的 `HolderLookup.Provider` 传递**：`BlockEntity.loadAdditional(CompoundTag, HolderLookup.Provider)` 提供了 provider，但如果按旧习惯从 `level.registryAccess()` 取，会在 load 期拿到 null（`level` 尚未绑定）。必须用形参。
- **`TickEvent.Phase` 消失**：`StructureAnalysisService.onServerTick` 是分析调度的驱动源，从 `Phase.END` 改到 `ServerTickEvent.Post` 时，若有依赖"pre/post 之间状态不变"的隐含假设需要复核。
- **`ItemStack.copy()` 不能直接替代 NBT 往返**：`ExactEnchantmentSemantics1201:1095` 依赖 NBT 往返"丢弃运行时状态"的副作用，改成 `copy()` 会把状态带过来，属于语义变化，必须逐个调用点确认。
- **`SavedDataTransaction1201` 的反射**：已核实 1.21.1 的 `DimensionDataStorage.cache` 字段名未变，但这是运行时才暴露的依赖，必须有 GameTest 覆盖。
- **`ResourceLocation` 构造器私有化**：28 处调用点，2 处在 `ResourceLocationHelper` 收口，其余散落。建议在 Phase 2 先用编辑器全局替换 `new ResourceLocation(` 的两种形态。
- **`CreativeModeTab` 排序**：源工程未指定 `withTabsBefore`，1.21 下会排到创造模式物品栏末尾。是否接受这个位置变化需要确认（不属于 API 问题，属于体验问题）。
- **可选依赖裁剪**：源 build.gradle 声明了 6 个源码里完全没 import 的模组（Mekanism / Architectury / GeckoLib / ATO / Time in a Bottle / GuideME）。保留会拖慢构建并污染 datagen 类路径，删除前需与用户确认这些不是"为将来预留"。

### 5.3 全局注意事项

- **源工程冻结**：Phase 0 之后所有修复先在 1.20.1 落地再同步，避免双向漂移。
- **不要顺手改设计**：`ProcessingMath` 的并行/效率公式、`StructureMinerMultiblock` 的几何校验、`MinerAnalysisController` 的缓存策略、异步回调顺序，都是 1.20.1 已定型的设计。移植只换 API，不改算法。
- **不要为兼容加层**：`*1201` → `*1211` 是直接改名，不要保留 `1201` 别名做兼容；标记迁移不做 DataFixer（1.2 已排除）。
- **每阶段一个提交**：Phase 2-10 共 9 个提交，每个提交的 message 说明该阶段换掉了哪些 API 面，便于 review 与二分定位。

---

## 6. 验证与测试

### 6.1 每阶段编译门禁

Phase 2-10 每阶段结束执行 `./gradlew compileJava`（含 datagen 时用 `./gradlew build`），必须零 error。已知的跨阶段残留错误要在阶段说明里显式列出，不允许"整体还没编译过"推进超过两个阶段。

### 6.2 客户端端到端检查清单

| 检查项 | 覆盖的移植面 |
| --- | --- |
| 放一个结构标记 → 右键 → 选择结构 → 数据写入 | DataComponents 持久化 + 网络往返 |
| 退出重进，标记数据仍在 | 组件的 `persistent` 序列化 |
| 宝箱标记器按 `V` 标记容器 | `ClientTickEvent.Post` + 网络 |
| 6 个 GUI 界面全部打开、滚动、tooltip、输入框正常 | 客户端层 |
| 反应堆 / 数据操作仪方块为 cutout，采掘器玻璃半透明 | `render_type` |
| 采掘器接箱子 → 能输出物品 | `Capabilities.ItemHandler.BLOCK` |
| 采掘器接流体桶 → 能进出流体 | `Capabilities.FluidHandler.BLOCK` / `.ITEM` + `FluidTank` provider |
| 反应堆每个面单独测流体进出 | 按朝向 capability 分发 |
| 多方块投影渲染 + 一键构建 | `RenderLevelStageEvent` + `IPlayerExtension.openMenu` |
| 数据操作仪目录加载 + 写入读槽 | 网络 + 注册表访问 |
| 创造模式物品栏里全部条目可见、顺序合理 | 注册层 + `CreativeModeTab` |
| JEI 里 6 个 tier 页 + 反应堆页 + 核心页正常 | Phase 9 |
| Jade 在采掘器上显示进度 | Phase 9 |
| KubeJS 脚本样例能跑通 | Phase 9 |

### 6.3 三条 loot 语义基线（Phase 7 的验收核心）

1. **精确语料回归**：`LootAnalysisFingerprintGameTests` 与 `StructureValueCalculatorGameTests`、`VirtualStructureSamplerGameTests`、`StructureMinerLootMergeGameTests`、`StructureMinerOutputRouterGameTests`、`StructureMinerTierGameTests`、`LootFixtureSemanticsGameTests` 全部通过（8 个 GameTest 类 / 26 个用例）。这是唯一能捕捉"静默算错"的机制。
   - 收尾时补上的一条实测教训：**只要 fixture 零 Java 消费，它坏掉是没人知道的**。`loot_table/gametest/*` 那批 fixture 在源工程与目标工程都没有消费者，于是 5 张表一直带着 1.20.1 语法（`name` 字段、缺 `mode`）在 1.21.1 上加载失败；接上测试后第一轮就全红。补测试时还必须检查"这条断言是不是在测一个不存在的表"——`missing_table` 的旧断言就因为"根表没加载"和"根表存在但被引用表缺失"都会报 `MISSING_REFERENCE` 而**假通过**。
2. **指纹稳定性**：同一个世界、同一份配置，连续两次分析同一个结构，`AnalysisFingerprint` 必须一致；重启服务器后再次分析仍一致。这验证 `RuntimeLootAstSource` 的输入集合完备（特别是新增的附魔注册表维度）。
3. **vanilla 语料门禁**：用 `-PvanillaLootRuntime=true` 跑 `gameTestServer`（可选模组降到 `compileOnly`）。这条门禁的意义是：精确引擎算出的期望值必须与**纯原版 + 本模组**的战利品表一致；如果只有装了 KubeJS / AE2 等可选模组才通过，说明引擎还在依赖模组注入的副作用，而不是原版语义。
   - 收尾时这条门禁第一次真正跑起来，立刻暴露 `StructureMinerOutputRouterGameTests#routesLootIntoAnOnlineAe2Interface` 在 AE2 缺席时以 `NoClassDefFoundError: appeng.core.definitions.AEBlocks` 失败。**凡是引用可选模组类型的 GameTest，都必须自带运行时存在性守卫**（`ModList.get().isLoaded(...)`，与 `StructureMinerOutputRouter` 同一惯例），否则这条门禁永远红，而失败信息会把"环境缺失"伪装成"集成回归"。反过来不要用 `@GameTest(required = false)` 一了百了——那会让真正的 AE2 集成回归在全量配置下也不再阻断。

### 6.4 回归对比方法

Phase 7 期间保留一个 1.20.1 侧的离线对照：对同一组战利品表 JSON，用 `*1201` 与 `*1211` 两套引擎各算一遍期望值，差异必须能逐条解释（附魔语义变化、战利品表格式变化）。**无法解释的差异一律当作 bug**，不允许"先放过去"。

---

## 7. 假设与决策

### 7.1 已决策

| 编号 | 决策 | 依据 |
| --- | --- | --- |
| D1 | loot 精确语义按 1.21.1 重建，`*1201` → `*1211`，`ALGORITHM_VERSION` 5 → 6 | 用户确认 |
| D2 | 完整移植，含 JEI / Jade / KubeJS / AE2 全部集成 | 用户确认 |
| D3 | 不做 1.20.1 存档兼容层，1.21.1 是新基线 | 模组尚未发布 1.21.1 版本，无历史存档需承接；避免引入兼容层污染 |
| D4 | 标记数据用自定义 DataComponent 承载，而非继续塞 CompoundTag | 1.21 的物品数据模型只有组件；Codec 化后字段语义比 NBT 更稳定 |
| D5 | Codec 字段名沿用现有 tag 常量名 | 保留可读性，且与 `*1211` 引擎的字段读取保持一致 |
| D6 | 网络层拆成 `network/payload/` 每 payload 一文件 | 1.21 的注册模型要求每个 payload 有独立稳定身份，继续堆单文件会让注册表与实现脱节 |
| D7 | `src/generated` 全量删除重生成，不手工改目录名 | 避免与 `RecipeOutput` 写入路径不一致导致双份数据 |
| D8 | 保留 `DatagenExitWatchdog` | 纯 JDK 逻辑，解决 KubeJS 非守护线程导致 datagen JVM 不退出 |
| D9 | 保留 `spotless` | 移植期大量机械改动，格式化能显著降低 review 成本 |
| D10 | 保留 `vanillaLootRuntime` 门禁与 `optionalModConfiguration` 切换 | 是精确语料验证的前提，不能丢 |
| D11 | 用 `DeferredRegister.create(Registries.X, modid)`，不用 `createBlocks` / `createItems` 便利工厂 | 保持与源工程 1:1 的代码结构 |

### 7.2 待确认（不阻塞 Phase 0-6，但影响 Phase 1 与 Phase 9）

| 编号 | 待确认项 | 建议 |
| --- | --- | --- |
| Q1 | 6 个源码未 import 的可选依赖（Mekanism / Architectury / GeckoLib / ATO / Time in a Bottle / GuideME）是否删除 | 建议删除，减少类路径污染 |
| Q2 | 允许木 Tweaks 是否继续保持 client-only（`compileOnly`，不进 datagen 类路径） | 建议保持 |
| Q3 | 创造模式物品栏标签的位置是否需要在 1.21 显式指定（`withTabsBefore`） | 建议指定，否则排到末尾 |
| Q4 | ~~`src/test/resources/gameteststructures` 目录缺失~~ **已核实**：目标工程该目录下 `empty.snbt` 存在且**必需**（原版不提供 `minecraft:empty`），`syncGameTestStructures` 不是残留 | 保留任务与接线，不要删 |
| Q5 | 是否需要为 1.20.1 存档提供一次性标记迁移（命令 / DataFixer） | 建议不做（与 D3 一致），除非确有存档要继承 |

---

## 附录 A：已验证的 NeoForge 21.1.251 API 索引

以下路径均已从本地 `neoforge-21.1.251-sources.jar` 核实：

```
net.neoforged.bus.api.IEventBus / SubscribeEvent
net.neoforged.fml.common.Mod / EventBusSubscriber
net.neoforged.fml.ModContainer                                  (registerConfig)
net.neoforged.fml.config.ModConfig
net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent / FMLClientSetupEvent
net.neoforged.fml.loading.FMLEnvironment                        (DistExecutor 的替代)
net.neoforged.neoforge.common.NeoForge                          (EVENT_BUS)
net.neoforged.neoforge.common.ModConfigSpec
net.neoforged.neoforge.common.util.Lazy
net.neoforged.neoforge.common.data.ExistingFileHelper / LanguageProvider
net.neoforged.neoforge.common.conditions.IConditionBuilder
net.neoforged.neoforge.common.extensions.IMenuTypeExtension / IPlayerExtension
net.neoforged.neoforge.registries.DeferredRegister / DeferredHolder / NeoForgeRegistries(.Keys)
net.neoforged.neoforge.capabilities.Capabilities(.EnergyStorage/.ItemHandler/.FluidHandler)
net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
net.neoforged.neoforge.energy.IEnergyStorage / EnergyStorage
net.neoforged.neoforge.items.IItemHandler / ItemStackHandler / SlotItemHandler / ItemHandlerHelper
net.neoforged.neoforge.fluids.FluidStack / FluidType / BaseFlowingFluid
net.neoforged.neoforge.fluids.capability.IFluidHandler / IFluidHandlerItem
net.neoforged.neoforge.fluids.capability.templates.FluidTank
net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
net.neoforged.neoforge.client.model.DynamicFluidContainerModel
net.neoforged.neoforge.client.model.generators.BlockStateProvider / ItemModelProvider
net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent / RegisterColorHandlersEvent / RenderLevelStageEvent / ClientTickEvent
net.neoforged.neoforge.event.tick.ServerTickEvent(.Pre/.Post)
net.neoforged.neoforge.event.entity.player.PlayerInteractEvent / UseItemOnBlockEvent
net.neoforged.neoforge.event.server.ServerStoppingEvent
net.neoforged.neoforge.gametest.GameTestHolder / PrefixGameTestTemplate
net.neoforged.neoforge.data.event.GatherDataEvent
net.neoforged.neoforge.network.PacketDistributor
net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
net.neoforged.neoforge.network.registration.PayloadRegistrar   (playToServer/playToClient/playBidirectional)
net.neoforged.neoforge.network.handling.IPayloadContext / IPayloadHandler
net.neoforged.api.distmarker.Dist
```

已确认**不存在**（必须替换）：`RegistryObject`、`ForgeRegistries`、`DeferredRegister`(Forge 版)、`MinecraftForge`、`FMLJavaModLoadingContext`、`DistExecutor`、`LazyOptional`、`ICapabilityProvider`(Forge 版)、`CapabilityManager`、`SimpleChannel`、`NetworkRegistry.newSimpleChannel`、`NetworkEvent`、`ForgeConfigSpec`、`ForgeFlowingFluid`、`ForgeCapabilities`、`ForgeHooks`、`IForgeMenuType`、`NetworkHooks`、`Deserializers`、`LootDataManager`、`EnchantmentHelper.enchantItem`、`EnchantmentHelper.getAvailableEnchantmentResults`、`ItemStack.getTag/getOrCreateTag/setTag/hasTag/of`。

## 附录 B：阶段-产出对照

| 阶段 | 主要产出 | 编译门禁 |
| --- | --- | --- |
| 0 | 基线提交、源工程 HEAD 记录 | — |
| 1 | 构建脚本、mods.toml、依赖裁剪、run 配置 | `build` + `runData` |
| 2 | 入口、8 个注册类、ModConfigs、ResourceLocationHelper | `compileJava` |
| 3 | StructureMarkerData(Codec)、ModDataComponents、标记链路 | `compileJava` |
| 4 | RegisterCapabilitiesEvent、能力访问改写、流体 provider 传递 | `compileJava` |
| 5 | 15 个 payload、NetworkHandler、ModNetwork 发送侧 | `compileJava` + `runClient` |
| 6 | 客户端包替换、render_type、IMenuTypeExtension、openMenu | `runClient` 全 GUI 走查 |
| 7 | `*1211` 全量、RuntimeLootAstSource 重建、附魔语义 | 三条 loot 基线 |
| 8 | datagen providers、generated 重生成、数据目录改名 | `runData` 幂等 |
| 9 | AE2 → Jade → JEI → KubeJS | 每集成单独启动 |
| 10 | GameTest 迁移、文档更新 | 完整回归 |
