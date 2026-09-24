# Dimension Tech —— 代码 Wiki

> 本文档面向需要理解、修改或扩展本模组源代码的开发者。它描述 `src/main/java/com/suntide_20210418/dimensiontech` 的整体架构、模块职责、关键类、依赖关系与运行方式。所有描述均以当前源码为准（README 中的"代码地图"是更短的快速版）。

- 技术栈：Minecraft `1.21.1` + NeoForge `21.1.251`，Java `21`，Parchment 映射，ModDevGradle 2
- Mod 标识：`dimension_tech`，包根：`com.suntide_20210418.dimensiontech`
- 许可证：GPL-3.0

## 目录

1. [总体架构](#1-总体架构)
2. [模块划分总览](#2-模块划分总览)
3. [入口与装配（Entry & Registration）](#3-入口与装配)
4. [结构采掘器（Structure Miner）机器系统](#4-结构采掘器机器系统)
5. [Loot Expectation 引擎（核心）](#5-loot-expectation-引擎核心)
6. [结构分析与价值评估](#6-结构分析与价值评估)
7. [结构反应堆 / 结构数据操作仪](#7-结构反应堆--结构数据操作仪)
8. [客户端 GUI 层](#8-客户端-gui-层)
9. [模组集成层（可选依赖）](#9-模组集成层可选依赖)
10. [DataGen / 网络 / 配置 / 资源](#10-datagen--网络--配置--资源)
11. [依赖关系总览](#11-依赖关系总览)
12. [运行方式与常用命令](#12-运行方式与常用命令)
13. [已知不一致与待处理](#13-已知不一致与待处理)

---

## 1. 总体架构

模组围绕"**结构的价值**"构建自动化：把世界中的结构转化为可分析的结构标记，再由多级采掘器按结构战利品表的**期望产出**持续生产资源。

核心循环（玩家侧）：

```
在结构中用结构标记器右键捕获当前位置命中的结构（散落容器则用宝箱标记器按 V）
→ 得到已写入维度/位置/边界/价值/期望的标记
→ 放入对应 Tier 的采掘器标记槽 → 用扳手切换投影、按投影搭好多方块 → 接 FE 能源 + 流体
→ 采掘器每周期结算资源并输出战利品（附带 min(10, 并行) 个本 Tier 维度碎片与采掘代币）
→ 用 Tier 2 / Tier 5 的碎片与代币造出数据整合器 / 结构阐释器
→ 装入结构数据操作仪，浏览结构目录并把分析结果批量写入标记（这一步在循环末端，不是起点）
```

两条关键设计主线：**采掘器机器逻辑**（`block` / `block/entity` / `structureminer`）和**战利品期望引擎**（`loot/expectation`）。前者是玩法循环，后者是价值信息的生产者（离线、确定性计算期望，而不是跑随机抽样）。

架构分层大致为：

```
集成层     integration/{jei,jade,kubejs,ae2}
客户端 UI  client/gui/{screen,menu}  ←→  client/{ClientModEvents, ...}
机器/玩法  block/entity + structureminer + structurereactor
状态/分析  structure/analysis
价值引擎   loot/expectation + loot/fingerprint
基础设施    config / network / datagen / energy / fluid / utils / item
```

---

## 2. 模块划分总览

| 包 | 职责 |
| --- | --- |
| `DimensionTechMod` | 模组入口，装配所有注册与事件 |
| `ModDataGenerator` | DataGen 装配（详见 §10） |
| `block/` | 方块：`BaseMinerBlock` + `Tier1..6StructureMinerBlock`、`StructureReactorBlock`、`StructureDataOperatorBlock`、多方块布局 `StructureMinerMultiblock`、结构块 `StructureMinerStructureBlock`、机壳/玻璃/升级块（在 `ModBlocks` 内直接注册为普通 `Block`，玻璃带 `noOcclusion` 保持通透），注册表 `ModBlocks` |
| `block/entity/` | 方块实体：`BaseMinerBlockEntity` + Tier1..6、`StructureReactorBlockEntity`、`StructureDataOperatorBlockEntity`、各类 `Miner*Controller`，注册表 `ModBlockEntities` |
| `structureminer/processing` | 加工数值：`ProcessingMath`、外部 tick 加速 `ExternalTickAcceleration` |
| `structureminer/output` | 产出路由 `StructureMinerOutputRouter`、期望奖励 `ExpectationRewardGenerator`、拆解 `EquipmentDismantler` |
| `structurereactor/` | 反应堆配方/公式/字节点/循环及其遥测快照 |
| `structure/analysis` | 结构价值计算、虚拟采样、战利品分析服务 |
| `loot/expectation` | **期望引擎**：确定性战利品表期望分析 |
| `loot/fingerprint` | 分析指纹 `LootAnalysisFingerprint`（缓存/存档稳定性） |
| `loot/` | `DimensionCoreChestLoot` 自定义战利品表 |
| `energy/` | `EnergyContainer`、`SimpleEnergyContainer`（NeoForge 能量） |
| `fluid/` | `ModFluids`、`EssenceFluidType` |
| `item/` | `ModItems`、`ModCreativeModeTabs`、`StructMarkerItem`（结构标记器）、`ChestMarkerItem`（宝箱标记器）、`WrenchItem`（扳手）、`DimensionDeconstructionCoreItem`（维度拆解核心）、数据整合器与结构阐释器 |
| `network/` | `ModNetwork`（发送侧）、`NetworkHandler`（注册）、`network/payload/`（各 payload） |
| `recipe/` | `ModRecipes`：RecipeSerializer 的 `DeferredRegister` 注册表 |
| `config/` | `ModConfigs`：ModConfigSpec 常见配置 + 各 Tier 参数 |
| `client/gui/screen` | 屏幕 & 页面（`StructureMinerScreen` 等） |
| `client/gui/menu` | 容器 Menu 与布局 |
| `client/` | `ClientModEvents`、`StructMarkerClient`、`ChestMarkerClient`、`ChestMarkerKeyHandler`、`KeyBindings`、`StructureMinerProjectionClient`、`ModMenu` |
| `integration/` | 可选模组集成（JEI/Jade/KubeJS/AE2）及 `MinerIntegrationHooks` |
| `datagen/` | 各 DataGen Provider + `DatagenExitWatchdog` |
| `utils/` | 通用工具：任务缓存、沙盘脚本配置、Loot 助手等 |

---

## 3. 入口与装配

### 3.1 模组入口：`DimensionTechMod`
被 `@Mod("dimension_tech")` 标记的主类。构造函数 `DimensionTechMod(FMLJavaModLoadingContext)` 负责在最早时机组装：

```java
ModConfigs.register(context);               // 最先注册配置
ModBlocks.register(bus); ModFluids.register(bus);
ModItems.register(bus); ModBlockEntities.register(bus);
ModCreativeModeTabs.register(bus);
ModRecipes.register(bus);                    // RecipeSerializer 注册表
ModMenu.MENU_TYPES.register(bus);            // 菜单类型注册表
MinecraftForge.EVENT_BUS.register(this);
modEventBus.addListener(…::commonSetup);
```

- `commonSetup`：在 `enqueueWork` 内执行 `StructureReactorRecipes.resetDefaults()`（建立默认配方集）与 `ModNetwork.register()`（注册信道）。
- 入口不再注册 GameTest（原来的 `registerGameTests` 方法已随测试体系一并移除，见 §13）。

### 3.2 各注册表
- `ModBlocks` / `ModBlockEntities` / `ModItems` / `ModFluids`：标准的 `DeferredRegister` 装配点，集中了所有方块/方块实体/物品/流体的注册。
- `ModRecipes`：RecipeSerializer 的 `DeferredRegister` 装配点。
- `ModCreativeModeTabs`：创意标签页。
- `ModMenu.MENU_TYPES`：`DeferredRegister<MenuType<?>>`，与 GUI 的 `ModMenu` 配合。

### 3.3 已注册内容清单（速查）
- **方块**：`tier_1..6_structure_miner`、`structure_reactor`、`structure_data_operator`、`structure_miner_casing`、`structure_miner_glass`、`structure_miner_structure`、`structure_miner_upgrade_{parallel,luck,energy,efficiency,aggregate}`（各 6 档，档 1 无后缀，档 2..6 带 `_tier_N`）。
- **物品**：`structure_marker`、`chest_marker`、`wrench`、`dimension_deconstruction_core`、`data_integrator`、`structure_interpreter`、`dimension_fragment_tier_1..6`、`mining_token_tier_1..6`，以及 5 种精华桶（`structure_essence_bucket`、`surging_structure_essence_bucket`、`recursive_essence_bucket`、`surging_recursive_essence_bucket`、`fractal_essence_bucket`）。
- **流体**：`structure_essence`、`surging_structure_essence`、`recursive_essence`、`surging_recursive_essence`、`fractal_essence`。
- 物品中文名（`ModZhcnLangProvider`）：`structure_marker` = 结构标记器、`chest_marker` = 宝箱标记器、`data_integrator` = 数据整合器、`structure_interpreter` = **结构阐释器**、`wrench` = 扳手。三者同源：注册 id `chest_marker`、中文名「宝箱标记器」、英文名 "Chest Marker"。
- 结构标记器与宝箱标记器共用同一 `StructureMarkerData` NBT（宝箱另在其下写 `ChestData` 子标签存 LootTable 与 seed），`ModItems.isMarker(ItemStack)` 是"能否被矿机/操作仪消费"的统一判据。

---

## 4. 结构采掘器机器系统

### 4.1 方块实体层级

- `BaseMinerBlockEntity`：所有采掘器的**服务端主控基类**。构造时初始化 `itemHandler`、`SimpleEnergyContainer`、`FluidTank`，以及四个控制器：
  - `MinerAnalysisController`（分析流转）
  - `MinerUpgradeController`（升级参数）
  - `MinerAccelerationController`（外部加速，如时间之瓶）
  - `MinerOutputController`（输出管理）
- 主循环在 `BaseMinerBlockEntity#serverTick()`，按固定阶段推进：
  `更新结构/升级状态 → 红石判断 → 自动抽液 → 待输出重试 → 标记与记录分析缓存 → 加工计划 → 工作 hook → 能源与周期流体结算 → 槽位推进 → 周期 hook → 战利品生成 → 输出 hook → 输出路由`
- 可运行条件（`#canRun` 一类）要求：多方块结构完整、红石模式允许、输出未阻塞、存在有效 marker；同时刷新 loot/processing plans。
- 产出点：按 slot 推进，读取 `MarkerAnalysis`，生成 completed markers，最终调用 `drawMarkerLoot` 产出战利品（产出基于已分析物品的期望构造权重）。

- `Tier1StructureMinerBlockEntity` … `Tier6StructureMinerBlockEntity`：几乎只做参数差异化——通过 `ModConfigs.TIERS[0..5]` 配置 slot、并行、幸运、能量/消耗、效率等。真正的行为都在基类（与"避免重复状态/职责重叠"的代码卫生一致）。

### 4.2 Tier 参数（`ModConfigs.TIERS`）

`StructureMinerTierConfig` 的构造参数顺序为 `tier, defaultParallel, defaultLuck, defaultSlotCount, defaultEnergyCapacity, defaultEnergyConsumption, defaultEfficiency`；每档另有 `quantityReference`（默认 `16.0`，产出缩放的期望数量基准）。默认值：

| Tier | baseParallel | luck | 标记槽 | capacity FE | 能耗 FE/t | efficiency | 周期流体 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 | 0.0 | 1 | 100_000 | 1_024 | 1.0 | 水 |
| 2 | 3 | 1.0 | 2 | 400_000 | 4_096 | 2.0 | 神话精华 |
| 3 | 5 | 2.0 | 3 | 1_600_000 | 16_384 | 3.0 | 涌动神话精华 |
| 4 | 7 | 4.0 | 4 | 6_400_000 | 65_536 | 4.0 | 递归精华 |
| 5 | 9 | 8.0 | 6 | 25_600_000 | 262_144 | 5.0 | 涌动递归精华 |
| 6 | 11 | 16.0 | 9 | 102_400_000 | 1_048_576 | 6.0 | 分形精华 |

> 周期流体不在 `ModConfigs` 里：`ModFluids.forMinerTier(tier)` 是唯一映射（`case 1 -> Fluids.WATER`，`case 2..6` 对应各档精华），需要流体的开关是 `BaseMinerBlockEntity#requiresFluidInput()`，默认判据 `getMinerTier() >= 1`，即**所有 Tier 默认都要进流体**，Tier 1 用**水**；KubeJS 的 `requiresFluid(false)` 可单独关掉。JEI 也是按 `forMinerTier` 为 1..6 每档展示流体的。

另有 `UPGRADE_TIERS` 与 `AGGREGATE_UPGRADE_TIERS`（升级块/聚合升级块逐档数值），字段为 `efficiencyIncreasePercent`、`energyCapacityIncreasePercent`、`energyConsumptionReductionPercent`、`parallelIncreasePercent`、`luckIncreasePercent`。所有数值可被服务器配置或 KubeJS 覆盖。

### 4.3 多方块几何：`StructureMinerMultiblock`

四个坐标 `Set` 是形状、材料清单与投影叠加层**唯一的事实来源**（不再有手抄数字）：

| 集合 | 数量 | 含义 |
| --- | --- | --- |
| `CASING` | 40 | 上下两块法兰端板（`y=-1` / `y=-5`），各 5×5 外环 16 格 + 4 根通向腰柱的辐条 |
| `GLASS` | 12 | 腰部（`y=-2..-4`）四个角柱，取代旧版的 8 个维度聚焦方块 |
| `STRUCTURE` | 2 | 机头正下方 `(0,-1,0)` 与钻点 `(0,-5,0)` |
| `UPGRADE` | 12 | 腰部四个面心柱 ×3 层；槽位接受升级方块**或结构方块**（`acceptsUpgradeSlot`） |

合计 66 格，布局与 Tier 无关（几何不再随等级变化，所有相关 API 均已去掉 `tier` 参数）。`projectionCounts()` 直接读四个集合的 `size()`，材料提示因此不可能与几何漂移。

硬契约（改坐标前必须保持）：机头 `(0,0,0)` 本体、四侧与上方留空；机头所在层除本体外全空；结构块必须在机头正下方。原工程的 `docs/tools/multiblock_geometry_check.py` 能自动校验这些不变量，但该脚本**未随移植迁入**本工程（见 §13），改几何后请手工核对四个集合的 `size()` 与机头契约。

### 4.4 加工数值：`structureminer/processing`
- `ProcessingMath`：纯函数化的期望/累加计算——`quantityFactorHundredths`（数量因子）、`averageParallel`、`expectedDraws`、`expectedItemCount`，以及 `accumulateHundredths`（把百分位累加为整数 + 有界余数，配合矿机的分数累加规则）。全部用 `BigDecimal`/长整型避免浮点漂移。
- `ExternalTickAcceleration`：处理外部实体 tick 加速（如时间之瓶）带来的加倍/节流逻辑；`MINIMUM_NATURAL_TICKS = 400`，即一个加工周期至少 400 自然 tick，保证受加速时资源结算依然稳定可预期。

### 4.5 产出系统：`structureminer/output`
- `StructureMinerOutputRouter`：把生成的 `ItemStack` 按输出面 mask 与目标能力，分发到相邻 item handler 或 AE2 ME 网络（`MinerOutputController` 记录 `ITEM_HANDLER` / `ME_NETWORK` 两种输出状态）。
- `ExpectationRewardGenerator`：基于已分析物品的期望构造奖励权重（`draw(...)` 按权重抽；`equipmentDismantling` 打开时每个产物先经 `EquipmentDismantler` 拆解，再按 `disabledItems` 过滤）。另有两笔**非战利品**的固定产出：`addTieredRewards` 每周期附送 `min(10, parallel)` 个本 Tier 的 `dimension_fragment_tier_N` 与等量 `mining_token_tier_N`；`rollDimensionCores` 按 `min(1.0, 0.05 × tier)` 掷 `parallel` 次拆解核心，单周期上限 `MAX_DECONSTRUCTION_CORES = 10`，只受"预期物品开关"（`disabledItems`）约束。碎片与代币是 `data_integrator` / `structure_interpreter` 的配方原料，**这是操作仪必须排在采掘器之后的原因**。
- `EquipmentDismantler`：拆解核心的物品拆解路径。

### 4.6 能量与流体
- `energy/EnergyContainer`（接口）/`SimpleEnergyContainer`（Forge `EnergyStorage` 实现），接入采掘器 FE 结算。
- `fluid/ModFluids` + `EssenceFluidType`（Forge FluidType），Tier2-6 各需要一种精华流体，在一个周期开始时扣除。

---

## 5. Loot Expectation 引擎（核心）

这是模组最有价值的部分：**离线、确定性地算出某张战利品表（loot table）的期望产出概率分布**，而不是像真实抽取那样逐次随机抽样。价值在于：矿机要"把结构变成稳定产出"，需要先精确知道每种物品的期望数量。

### 5.1 核心思想

1. **随机数状态不可变、可枚举**：引擎重写 1.20.1 的两个随机源——`XoroshiroState1211`（Xoroshiro128++，现代 `RandomSource`）与 `LegacyState1211`（48-bit LCG，`LegacyRandomSource`）。每个随机调用返回 `Draw<T>(value, newState, drawCount)`：因为状态迁移是确定的，给定当前 seed，`nextInt(n)` 等调用会落在一个**有限、可精确计算**的取值集合上，并记录"消耗了几次底层 draw"。
2. **把"随机抽取"升级为"有限概率分布"**：一次 `roll` 不再是一次 `nextDouble()`，而是一张"取值 → 精确概率"的映射（`FiniteDistribution`，要求 mass 精确等于 1）。
3. **用线性期望约化**：`DistributionalLootPool1211` 的注释明确写着通过线性期望（linearity of expectation）避免逐样本随机模拟——期望 roll 次数、单格选择期望、最终期望产出都由分布推导。
4. **保持与原版的逐位语义一致**：随机语义类（Exact/Distributional/Stateful 前缀的 `*1211`）刻意复刻原版 `nextInt`/weighted index/附魔语法的边界行为（幂次快速路径、有符号拒绝条件等），以此保证分析结果与原版抽奖等价。后缀 `1211` 标明这套语义只对 1.21.1 精确。

### 5.2 关键类族（`loot/expectation`）

| 类族 | 代表类 | 职责 |
| --- | --- | --- |
| 随机状态 | `XoroshiroState1211`、`LegacyState1211`、`StatefulRandomSource1211`、`StatefulLegacyRandomSource1211` | 不可变随机数状态；`Draw` 携带新状态与 drawCount |
| Reference 语义 | `ReferenceSemantics1211` | 缺失/递归引用（TABLE/PREDICATE/FUNCTION）在 1.20.1 的精确行为：缺失表→空输出、缺失谓词→false、缺失函数→identity，并产出一条带 JSON 指针和调用栈的 `Diagnostic` |
| 精确语义 | `ExactRandomSemantics1211`、`ExactEnchantmentSemantics1211`、`ExactProbability` | 把原版随机方法定义为有限精确语义（可枚举/可复现），用于附魔等场景 |
| Distributional 求值 | `DistributionalLootTableExecutor1211`、`DistributionalLootPool1211`、`DistributionalFunction1211`、`DistributionalCondition1211`、`DistributionalNumberProvider1211` | 把一张表/一个池解析成期望与分布；入口 `evaluate` / `expectation` |
| Stateful 执行 | `StatefulLootTableExecutor1211`、`StatefulLootSequenceExecutor1211`、`StatefulLootPool1211`、`StatefulCondition1211`、`StatefulFunction1211`、`StatefulNumberProvider1211` | 有状态地推进"直到产出实际 item stack"的执行模拟 |
| 随机状态分布式 | `RandomStateExecutor1211`、`RandomStateDistribution`、`RandomTraceDistribution`、`PersistentRandomSequenceSnapshot1211` | 随机状态在分布空间里的执行/追踪 |
| 概率/数学 | `FiniteDistribution`、`ExactProbability`、`ExpectationMath`、`IdealRandomProbabilitySpace1211` | 归一化 PMF、精确概率、长程期望计算 |
| 附魔边际 | `EnchantmentKey`、`EnchantmentMarginal` | 附魔选择/等级的概率边际量 |
| 数据结构与产物 | `StackState`、`StackMeasure`、`TerminalStackMeasure`、`StackObservationMeasure`、`StackObservation`、`TerminalStackKey`、`FrozenJson`、`LootExpectationResult`、`LootAnalysisContext`、`Diagnostic`、`MarkerAnalysis` | 分析的输入/输出/中间表示；`LootExpectationResult` 是统一输出 |
| 状态/产物枚举 | `AnalysisStatus`（`EXACT / APPROXIMATE / UNSUPPORTED / LEGACY`）、`EvaluationFailureKind` | 结果状态标记 |
| 可达性 | `Reachability` | 惰性可达性分析，避免展开完整输出列表的乘积爆炸 |
| 1.21 JSON 形状 | `NestedLootTableEntry1211` | `minecraft:loot_table` 条目在 1.21 的字段是 **`value`**（1.20.1 是 `name`），值可为表 id 或内联表对象；集中一处，避免字段名再被抄成旧版本 |
| 外部支持 | `RuntimeLootAstSource`、`SavedDataTransaction1211` | 从运行时/存档读取战利品表数据 |

### 5.3 输出结果：`LootExpectationResult`

record 字段：`AnalysisStatus status`、`StackMeasure measure`、`TerminalStackMeasure terminalMeasure`、`boolean fullStackMeasureAvailable`、`List<Diagnostic> diagnostics`。关键不变式：**非 `EXACT` 状态会清空完整 stack measure**（置空、`fullStackMeasureAvailable=false`），即只有当分析被判定为精确时才暴露完整分布。`exactTerminal(...)` 表示终端聚合但未实体化完整 `StackMeasure`。

### 5.4 指纹：`loot/fingerprint/LootAnalysisFingerprint`

record 字段：`int algorithmVersion`（当前 `ALGORITHM_VERSION = 1`）、`List<String> markerSlots`、`int luckBits`、`String analysisConfig`。用于缓存/复用 marker 战利品分析结果，并保证适配层在存档里可复现、不因无关数据（物品数量、非分析派生字段）变化而失效——这支撑了"后台异步分析"与"结果在下次使用时重算"的机制（任何过期/被替换/已清空/方块实体已被移除的异步结果都不能提交）。

### 5.5 判定层与降级阶梯

「精确」是**引擎层**的性质，不等于对外状态。两层必须分开读：

| 层 | 数据来源 | 引擎判 `UNSUPPORTED` 时的行为 |
| --- | --- | --- |
| `DistributionalLootTableExecutor1211`（引擎） | 实时注册表，或 `RuntimeLootAstSource` 冻结快照 | 直接返回 `UNSUPPORTED` + `UNSUPPORTED_TYPE` 诊断，**不做任何近似** |
| `StructureValueCalculator`（计算器） | 实时 `ServerLevel` | **退化为采样兜底**：状态 `APPROXIMATE` + 诊断 `SAMPLING_APPROXIMATION`（`:887-897`） |

所以"精确分析、不做近似 fallback"只对引擎成立：任何含未建模机制的战利品表，玩家看到的是**采样估计**而不是"不支持"。要拿引擎的原始判定，必须直接调 `DistributionalLootTableExecutor1211.evaluate(...)`。

两条数据来源必须**同判**——它们共用同一套语义，只有输入来源不同。`LootFixtureSemanticsGameTests#requireEnginesAgree` 把这条钉成了常驻不变量。

生产路径走的是**冻结**一侧（`calculateAsync` → `StructureAnalysisService` 缓存 → worker 线程）。这带来一个必须记住的失效方式：`RuntimeLootAstSource.snapshotTables(server, roots)` 递归抓取根表所引用的表/predicate/modifier，**抓不到就静默跳过**。若抓取环节与引擎的解析口径不一致（例如字段名不同），冻结引擎只会把引用当成"缺失表"并返回空产出，期望值**静默偏低**、且不产生任何可区分的错误。跨接口的字段口径必须在同一处定义（见 §5.2 的 `NestedLootTableEntry1211`）。

冻结路径还有一个必然差异：`LootAnalysisContext.snapshot(...)` 按设计没有 level，因此 `minecraft:score` 数量提供器取不到计分板（`DistributionalNumberProvider1211:255` 直接返回 null）→ 判 `UNSUPPORTED`；实时路径能读出真实分数。

---

## 6. 结构分析与价值评估

`structure/analysis/` 把期望引擎接到"结构价值"上：

- `StructureAnalysisService`：分析服务门面，负责把 loot-expectation 缓存下来（`capture`）。
- `StructureLootAnalyzer`：解析一个结构的战利品表来源。1.21.1 的 34 个原版结构里只有 9 个能从 `worldgen/structure/<id>.json` 的 `start_pool` 走到模板图，其余 25 个由代码放置、模板扫描从数据结构上就够不到，它们的表来自三处：模板 NBT（`ruined_portal`，13/13 模板带 `LootTable`）、`StructurePiece#handleDataMarker`（`shipwreck` / `igloo` / `ocean_ruin` / `woodland_mansion` / `end_city`）、`StructurePiece#createChest`（`desert_pyramid` / `jungle_temple` / `stronghold` / `mineshaft` / `nether_fortress` / `buried_treasure`）。
  模板扫描覆盖三种字段形态：容器与发射器的 `LootTable`、vault 的 `config.loot_table`、试炼刷怪笼的 `loot_tables_to_eject`（`normal_config` 在各原版模板里都省略该字段，因此按 `TrialSpawnerConfig.DEFAULT` 取值而不是复制一份常量；`ominous_config` 缺省时按 `TrialSpawnerBlockEntity#loadAdditional` 的 merge 语义继承 `normal`）。`items_to_drop_when_ominous` **有意不计入** —— 它只在玩家带不祥之兆时额外掉落，不属于刷怪笼的常规产出。
- `utils/VanillaStructureLootResolver`：上述代码放置结构的权威映射，覆盖全部 34 个原版结构，每条都记录原版证据（类名:行号）。`Resolution.hasContainers() == false` 表示该结构**确认不放置任何战利品容器**（`ocean_monument` / `swamp_hut` / `nether_fossil` / `trail_ruins`），此时 discovery 直接返回 `EXACT` + 空表并带诊断 `VANILLA_NO_CONTAINER`，而不是 `UNSUPPORTED` —— 对玩家表现为「精确的 0 产出」，而不是「这台机器读不了这个结构」。只有连固定映射都没有的命名空间（模组结构）才会继续走虚拟采样。
- `StructureValueCalculator`：价值计算入口，**异步**评估 frozen loot source，并按配置在两条路径间切换：
  - 精确期望路径：对每个 root table 调 `DistributionalLootTableExecutor1211.evaluate`，汇总 terminal/full `StackMeasure` 并冻结为 `LootExpectationSnapshot`。
  - 采样近似路径（`ItemExpectationMethod.SAMPLING` 或非精确场景）：通过 `LootTableLottery.draw` 做 Monte Carlo 采样，把计数/频率折算为期望概率。
- `VirtualStructureSampler`：在虚拟（不落盘）结构中采样结构内容/战利品。
- `StructureValueSnapshot`：一次分析结果的不可变快照。
- `utils/LootTableLottery`：实际随机抽取工具（对有 `LootParams`、seed 的 `LootTable.getRandomItems`），供采样近似路径使用。

回报价值规则（稀有度倍率、维度价值、物品规则、白/黑名单）由 `ModConfigs.StructureValueConfig` 或 KubeJS 配置提供；配置变化会使相关分析缓存失效。

---

## 7. 结构反应堆 / 结构数据操作仪

### 7.1 结构反应堆（`structurereactor` + `StructureReactorBlockEntity`）
- 单方块机器：两个 `16_000 mB` 精华槽（`FLUID_TANK_CAPACITY_MB`）、一个碎片槽（`FRAGMENT_SLOT = 0`）、一个即时操作槽（`OPERATION_SLOT = 1`，单物品）。
- `StructureReactorRecipe` / `StructureReactorRecipes`：配方模型与内存注册表（`resetDefaults` 在 commonSetup 建立默认配方，datapack/KubeJS 重载时先重置再跑脚本）。
- `StructureReactorCycle` / `ReactorFormula` / `OperationMatcher`：反应堆的有限状态循环、公式与配方/输入匹配。
- `StateId` / `StateStep`：状态机节点建模。`StateId` 只有四个操作——`BRANCH(1)` / `RECURSE(2)` / `CONVERGE(3)` / `STABILIZE(4)`，其 `signalLevel()` 被显式写死（而非取 `ordinal()`），以免日后插入新节时悄悄改变玩家已经搭好的红石接口。
- 状态 DSL：以空格 / `→` / `->` 分隔的状态名序列，例如 `branch recurse recurse converge stabilize`；每个状态对应一个"仪式操作物"（branch=石英、recurse=紫水晶碎片、converge=荧石粉、stabilize=红石）。
- 默认配方（输入流体 → 输出流体，碎片消耗 1，`baseFluidCost`/`targetOutput` 均为 1000）：

| 配方 id | 输入流体 | 输出流体 | 碎片 | A 支线 DSL | B 支线 DSL |
| --- | --- | --- | --- | --- | --- |
| `initial_manifestation` | 水 | 神话精华 | `dimension_fragment_tier_1` | `branch converge stabilize` | — |
| `structure_surge` | 神话精华 | 涌动神话精华 | `dimension_fragment_tier_2` | `branch recurse converge stabilize` | — |
| `recursive_foundation` | 涌动神话精华 | 递归精华 | `dimension_fragment_tier_3` | `branch recurse recurse converge stabilize` | — |
| `recursive_surge` | 递归精华 | 涌动递归精华 | `dimension_fragment_tier_4` | `branch recurse recurse recurse converge stabilize` | — |
| `fractal_closure` | 涌动递归精华 | 分形精华 | `dimension_fragment_tier_5` | `branch recurse recurse converge stabilize` | `branch recurse branch recurse converge stabilize` |

- `ReactorSequenceTelemetry` / `ReactorTooltipSnapshot`：遥测与 tooltip 快照（供 GUI 展示）。
- `ReactorAnalogSignal`：把 cycle 快照映射为比较器输出等级（0–15），按"反应堆在等什么"分区（空闲 / 进行中按 `StateId` / 奖励窗口 / 精炼中 / 启动被阻 / 提交被阻），让红石电路能比较、相减与锁存。
- `StructureReactorBlockEntity`：主循环按 cycle 状态执行 `tick / resolve / tryStart / tryCommit`；启动与提交时校验输入、碎片、配方匹配与输出容量，产出 `outputAmountMb()`。

### 7.2 结构数据操作仪（`StructureDataOperatorBlock*`）
- `StructureDataOperatorBlockEntity`：管理结构标记的读写/分析/批量复制。
- **获取门槛决定了它在循环里的位置**（`ModRecipesProvider`）：`data_integrator` = `FQF / ACA / TRT`（`F` = `dimension_fragment_tier_2`、`T` = `mining_token_tier_2`、`C` = 机壳、`Q` 石英、`A` 紫水晶碎片、`R` 红石）；`structure_interpreter` = `FNF / SCS / TNT`（`F` = `dimension_fragment_tier_5`、`T` = `mining_token_tier_5`、`S` 下界之星、`N` 下界合金锭、`C` = **数据整合器**）。碎片与代币都来自采掘器每周期附带产出 → **操作仪是循环末端的一步，不是第一步**。
- 槽位（`StructureDataOperatorBlockEntity`）：`TARGET = 0` 放目标标记器、`OPERAND_START = 1` 起连续的 `OPERAND_COUNT = 36` 个写入槽（界面 9×4）、`INTEGRATOR = 37`（只收 `data_integrator`）、`INTERPRETER = 38`（只收 `structure_interpreter`）。
- **前置关系**（写文档时最容易漏的一环）：`interpreterAvailable() = hasIntegrator() && hasInterpreter()`，只有两者齐备界面才出现结构目录；`INTERPRETER` 槽在整合器为空时**不接受插入**，`INTEGRATOR` 槽在阐释器在位时 `extractItem` 直接返回空（**抽不出来**）。
- **复制不需要整合器**：`canCopy` 只校验目标槽有已写入数据的标记器 + 36 个写入槽非空；`copyData()` 也只是把目标槽的 `StructureMarkerData` 拷进全部写入槽。整合器唯一的作用是解锁阐释器槽。
- 目录流程：`refreshCatalogue(player, includeAllStructures)` 生成目录 → `analyseCatalogueEntry(dimension, id)` 分析（按 value 配置指纹缓存）→ `writeCatalogueEntry(dimension, id)` 写入目标槽的标记器。
- 宝箱侧：`ChestMarkerItem.markChest` 直接标记并立即分析；`refreshAnalysis` 用当前世界数据与配置重算；profile 只用 NBT 里的 LootTable（`ChestInfo(position, lootTable, seed)`），不查结构模板、不依赖世界加载。

---

## 8. 客户端 GUI 层

### 8.1 屏幕页面系统：`StructureMinerScreen`
`StructureMinerScreen` 是主屏幕容器，定义徽章页结构 `Page.WORK / INFO / ATTRIBUTES`，并实现 `StructureMinerScreenContext` 向各页面暴露状态。页面由 `StructureMinerPageRenderer` 按当前 `Page` 分发渲染/点击/滚动：

- `StructureMinerWorkPage`（工作页：marker lane + 双列 meter 网格）
- `StructureMinerInfoPage`（信息页：单列滚动视口展示 marker 信息）
- `StructureMinerAttributesPage`（属性页：单列滚动属性列表）

基础设施类：
- `StructureMinerUiState`：客户端 UI 状态枢纽——当前页、选中 marker、hover、滚动位置、展开状态、analysis snapshot。
- `StructureMinerTheme` / `GuiChrome` / `GuiPalette` / `StructureMinerSpriteRenderer`：视觉外壳/调色板/精灵绘制。
- `StructureMinerPageRenderer` 与 `StructureMinerInfoLayout`（+ `client/gui/menu/StructureMinerLayout`）：页本地坐标 → 屏幕 scissor 空间的投影，配合滚动视口裁剪。
- `StructureMinerProgressStrip` / `StructureMinerTooltips` / `ReadingFormat`（读数格式化唯一出处）。
- `StructureMinerScreenContext`：向各页暴露的只读状态上下文。
- 其他屏幕：`StructureReactorScreen`、`StructureDataOperatorScreen`、`StructMarkerScreen`、`ChestMarkerScreen`、`OutputFaceConfigScreen` 与 `OutputFaceConfigMenu`。

### 8.2 Menu 层：`client/gui/menu`
`StructureMinerMenu` / `StructureReactorMenu` / `StructureDataOperatorMenu` 继承 `AbstractContainerMenu`，读取 block entity、注册 container slots，并通过 `ContainerData` 同步流体、输出面模式、自动提取等状态，为屏幕提供遥测数据源（`StructureMinerTelemetrySnapshot`）。这是 menu↔screen 数据同步的主路径。

### 8.3 其他客户端
- `ClientModEvents`：客户端事件监听（注册客户端渲染器、键位、覆盖页面类型、流体桶模型等）。
- `StructureMinerProjectionClient`：在搭建多方块时投影结构线框（`toggle` 已无 `tier` 参数）。
- `StructMarkerClient`：用 `StructMarkerItem` 时的客户端标记渲染/交互。
- `ChestMarkerClient` / `ChestMarkerKeyHandler` / `KeyBindings`：用 `ChestMarkerItem` 时的客户端交互——绑定按键（`GLFW_KEY_V`，默认 `V`）分析准星指向的宝箱并标记。
- `ModMenu`：`MENU_TYPES` 注册表容器。

---

## 9. 模组集成层（可选依赖）

所有集成都是可选的，仅在对应模组存在时才加载/生效。

| 集成 | 包/主类 | 作用 |
| --- | --- | --- |
| JEI | `integration/jei/`：`StructureReactorJeiPlugin`、`StructureMinerJeiPlugin`、`DeconstructionCoreJeiCategory`（+ `Recipe(s)`/`Text`）、`chestminerjeitext` | 展示结构反应堆、采掘器、拆解核心的配方；把对应屏幕展示区注册为 JEI 点击区域 |
| Jade | `integration/jade/StructureMinerJadePlugin` + `StructureMinerJadeProvider` | 服务端向 Jade HUD 提供矿机状态、slot marker、进度、并行、输出、能量等显示 |
| KubeJS | `integration/kubejs/DimensionTechKubeJSPlugin`、`MinerEventsJS`、`MinerBlockEntityJS`、`DimensionTechJS` | 向服务器脚本暴露矿机参数、反应堆配方、结构价值与工作事件覆写（详见 `docs/kubejs.md`） |
| AE2 | `integration/ae2/Ae2Integration` | ME 网络输出与流体交互 |
| 通用钩子 | `MinerIntegrationHooks` | 统一封装跨集成的矿机钩子点 |

选装依赖的构建处理值得注意（见 `build.gradle`）：数据生成时以 `compileOnly` 隔离可选模组（`-PvanillaLootRuntime` 则是原版战利品语料门禁的旧开关），避免它们污染生成的资源，或如 KubeJS 持有非 daemon 线程导致 datagen JVM 不退出的问题；客户端/开发运行时才以 `implementation` 加入。KubeJS 的接入还通过 `src/main/resources/kubejs.plugins.txt`（`META-INF/services` 风格）被 KubeJS 探测加载。

---

## 10. DataGen / 网络 / 配置 / 资源

### 10.1 DataGen（`datagen/`）
`ModDataGenerator#gatherData` 触发时注册：
- 服务端：`ModRecipesProvider`（配方）、`LootTableProvider` + `ModBlockLootTablesProvider`（方块战利品表）。
- 客户端：`ModItemModelsProvider`、`ModBlockStateProvider`、`ModEnusLangProvider`、`ModZhcnLangProvider`（中英文语言文件）。
- `DatagenExitWatchdog`：因为可选模组（尤其 KubeJS）可能持有非 daemon 线程，DataGen 主线程结束后 JVM 可能不退出；watchdog 检测 lingering 线程并在必要时 `System.exit(0)`。这也覆盖了 IDE 直接跑 `Application` 型 runData 配置的场景。
- 生成产物写入 `src/generated/resources`。

### 10.2 网络（`network/ModNetwork`）
- 使用 NeoForge `RegisterPayloadHandlersEvent` + `CustomPacketPayload`/`StreamCodec` 注册 14 个 payload（`network/payload/` 一包一文件，`TYPE` 的 ResourceLocation 即包身份），`PacketDistributor.sendToServer/sendToPlayer` 发送。1.20.1 的 `SimpleChannel` + 手写序号已整体废弃。
- 共注册 14 个包：`StructMarkerActionPacket`、`RefreshedMarkerPacket`、`StructureChoicesPacket`、`StructureMinerAnalysisRequestPacket`、`StructureMinerAnalysisPacket`、`StructureMinerExpectedItemTogglePacket`、`StructureMinerSlotTogglePacket`、`StructureReactorTooltipPacket`、`OperatorCatalogueRequestPacket`、`OperatorCataloguePacket`、`OperatorAnalysisRequestPacket`、`OperatorAnalysisPacket`、`OperatorActionPacket`、`ChestAnalysisRequestPacket`。
- 覆盖：marker 交互、矿机分析请求/响应、expected-item/slot toggle、反应堆 tooltip、数据操作仪的操作与目录、宝箱分析请求。
- **重叠结构选择的位置权威在服务端**：`StructMarkerActionPacket` 只携带 `hand` + `action` + `selectionIndex`，不携带位置。候选枚举的原点由服务端在收到 `REQUEST_SELECTION` 时写入玩家私有数据（`StructMarkerItem.rememberSelectionOrigin`），选中时一次性消费。两条原因：标记器终端 `isPauseScreen()` 为 `false`，玩家浏览列表时会走动，用实时位置校验会把合法选择丢掉；而客户端回传的位置本就不可信，用服务端记录的点才能保住「只能标记亲自站进去过的结构」这条约束。改这个包时不要重新引入客户端位置字段。

### 10.3 配置（`config/ModConfigs`）
- `ModConfigSpec` 构建一个 common 配置（`ModConfig.Type.COMMON`），由入口注入的 `ModContainer` 注册，为最早一步。
- 内容：`structureMiner`（各 Tier）、`structureMinerUpgrades`、`structureMinerAggregateUpgrades`、`structureValue`（`StructureValueConfig`：稀有度倍率、`ItemExpectationMethod`、采样数 `samplingCount`、`glmSupplementSamples`、虚拟结构采样、维度/物品/结构白黑名单、维度价值与物品倍率列表）。
- 配置文件由 NeoForge 生成在实例 `config` 目录，可被 KubeJS 覆盖；配置变化会使相关分析缓存失效。

### 10.4 资源（`src/main/resources`）
- `src/main/templates/META-INF/neoforge.mods.toml`：模板，经 `processResources` 展开；声明 neoforge/minecraft 必选依赖 + ae2/jade/jei 可选依赖（`type="optional"`，`ordering=AFTER`）。
- `assets/dimension_tech/`：guis 精灵图（`guis/*.png`）、方块/物品纹理与模型（`render_type` 直接写在手写模型 JSON 里，取代 1.20.1 的 `ItemBlockRenderTypes.setRenderLayer`）。
- `data/dimension_tech/`：`loot_table/`、`item_modifier/`、`predicate/`（1.21 把注册表目录名改为单数，目录名等于注册表键路径）。其中 `gametest/` 与 `loot_table/test/` 下的 18 个 fixture 服务于期望引擎回归测试。
- `kubejs.plugins.txt`：KubeJS 插件枚举。
- 无 `pack.mcmeta`（NeoForge 自行处理模组资源）与 accesstransformer。

---

## 11. 依赖关系总览

### 11.1 必选依赖（`mods.toml` + `build.gradle`）
- `neoforge`/`minecraft`：开发期通过 ModDevGradle 拉取 `net.neoforged:neoforge:21.1.251`；运行时 `minecraft` 与 `neoforge` 为必选且 `ordering=NONE`。

### 11.2 可选模组依赖（`mods.toml` 声明 + 运行时只在存在时生效）
`ae2`（≥19.2.17）、`jade`（≥15.10.6）、`jei`（≥19.25.0.322，因为用到了 `AbstractRecipeCategory` 与 recipe-extras 文本组件）。KubeJS 经 `kubejs.plugins.txt` 发现，故刻意不在 `neoforge.mods.toml` 声明。

另在 `build.gradle` 中 `add(optionalModConfiguration, …)` 的可选依赖：KubeJS、Rhino、Jade、Applied Energistics 2、**GuideME**（AE2 的 `REQUIRED` 依赖，虽无源码 import 但运行时必需）、AllTheModium、**GeckoLib**（Allthemodium 未声明却实际加载其类，故运行时也必需）、JEI；`compileOnly` 的有 Mouse Tweaks（纯客户端便利模组，被刻意排除出 dev 运行时以避免 runData/server 构建它）。其中 `optionalModConfiguration` 依据是否为数据生成运行而切换为 `compileOnly` 或 `implementation`。

### 11.3 代码内部依赖方向（核心）
```
DimensionTechMod ──> 注册表(block/entity/item/fluid/menu/recipe) & ModConfigs & ModNetwork
BaseMinerBlockEntity ──> Miner*Controller ──> structureminer/output ──> structure/analysis
structure/analysis ──> loot/expectation(Distributional/Stateful*) ──> FiniteDistribution / ExactProbability / Draw 语义
config/ModConfigs ──> loot/expectation(FrozenJson, TerminalStackKey) & utils/StructureScriptConfigService
client/gui ──> block/entity(数据源) & network(请求)
integration/* ──> block/entity + structurereactor(配方展示)
```
依赖方向清晰：基础设施 + 期望引擎在最底层，机器与玩法在中间，客户端 UI 与可选集成都依赖它们。

---

## 12. 运行方式与常用命令

### 12.1 环境要求
- JDK 21；Minecraft NeoForge 1.21.1 开发环境。
- Windows 用 `.\gradlew.bat`，macOS/Linux 用 `./gradlew`。
- 首次导入用 Gradle 包装器下载依赖；使用 ModDevGradle 2、Parchment 映射、Java 21 工具链。

### 12.2 常用命令（在项目根 `d:\mycode\ModDevelopment\DimensionTech-1.21.1`）

```powershell
.\gradlew.bat build                  # 编译 + spotless 检查 + 打包 → build/libs
.\gradlew.bat runClient              # 启动开发客户端
.\gradlew.bat runServer              # 启动无 GUI 服务端
.\gradlew.bat runData                # 数据生成，写入 src/generated/resources
.\gradlew.bat runGameTestServer      # 游戏测试（依赖 syncGameTestStructures 先就位）
.\gradlew.bat spotlessCheck          # 格式检查
.\gradlew.bat compileJava --rerun    # 只重编译 Java（改签名后最常用）
```

- `runData` 会重写 `src/generated/resources/...` 下的语言文件等产物；若工作区里存在未提交的手改语言条目，跑之前先确认。
- `runGameTestServer` 依赖 `prepareGameTestServerRun` → `syncGameTestStructures`，后者把 `src/test/resources/gameteststructures/empty.snbt` 复制进游戏目录。**原版不提供 `minecraft:empty` 模板**，缺这个文件所有 GameTest 会以 `Missing test structure: minecraft:empty` 直接崩掉。（ModDevGradle 的任务名是 `prepare<RunName>Run`，与 ForgeGradle 的 `prepareRun<RunName>` 不同，接线时容易接错。）
- `-PvanillaLootRuntime=true` 会把可选模组降级为 `compileOnly`，用于纯原版语料门禁。

### 12.3 修改约定
- 采掘器行为遵守 `BaseMinerBlockEntity#serverTick` 的固定阶段顺序。
- 异步标记分析以 fingerprint 识别输入；任何过期/被替换/已清空/方块实体已移除的异步结果都不能提交。
- 改多方块几何必须手工复核计数、重叠、包围盒覆盖、D4 对称、面连通与机头契约：读 `StructureMinerMultiblock` 的四个集合与 `projectionCounts()`，不要手数格子（几何校验脚本未随移植迁入，见 §13）。
- 避免在无性能数据时引入缓存索引或通用抽象层；保留中文注释与命名语境。
- Tier 判定**以代码为准**：`getMinerTier() >= 1` 对任何合法 Tier 恒为真，因此**每个 Tier 都需要流体输入**（Tier 1 为水），JEI 与 README 都照此写。`StructureMinerTierGameTests#allTiersMapToTheirConfiguredMachineValues` 的断言已改为 `!requiresFluidInput()` 的否定式；若将来要让 Tier 1 免流体，需同时改判据、JEI 展示与 README 三处，该断言会随之失败。

---

## 13. 已知不一致与待处理

以下均为**在当前源码里核实过**的事实：第 1–2 条是测试体系说明，第 7 条是**已修项**的登记（防复发），其余是**尚未处理**项，改文档时不要把它们写成正常状态。

1. **`*GameTests` 在 NeoForge 下会自动被注解发现并运行**（`@GameTestHolder` + `@GameTest`，无需在 `DimensionTechMod` 里显式注册）。1.21.1 上实测 **31 个全部通过**。注意：源 1.20.1 工程曾把这些测试从入口摘除，因此**这套测试此前并未在跑**，其断言未必与当前代码一致——移植期间已发现并修正一处（Tier 1 流体断言，见 §12.3）。
   - 两个配置下都要能跑通：`./gradlew runGameTestServer`（全量，含可选模组）与 `./gradlew runGameTestServer -PvanillaLootRuntime=true`（把可选模组降为 `compileOnly`）。后者的第一次实际运行暴露了 `StructureMinerOutputRouterGameTests#routesLootIntoAnOnlineAe2Interface` 在 AE2 缺席时以 `NoClassDefFoundError: appeng.core.definitions.AEBlocks` 失败——**任何依赖可选模组的 GameTest 都必须自带存在性守卫**（该方法现用 `ModList.get().isLoaded("ae2")`，与 `StructureMinerOutputRouter` 自身同一惯例），否则失败信息会把"环境缺失"伪装成"集成回归"。
2. **`src/test/java` 为空目录**，`build.gradle` 也没有 `testImplementation` / `test` 任务。当前实际在用的测试只有 `src/main/java` 下的 GameTest 用例（现 31 个），加上 `src/test/resources/gameteststructures/empty.snbt`（由 `syncGameTestStructures` 复制进游戏目录；**原版不提供 `minecraft:empty`，缺它所有 GameTest 直接崩**）。
   - fixture 的**实际消费关系**（逐文件核对过，不要按目录想当然）：`data/dimension_tech/loot_table/test/layered_equivalence.json` 由 `StructureValueCalculatorGameTests` 消费；`data/dimension_tech/{loot_table,item_modifier,predicate}/gametest/*` 那 17 个文件在源工程与目标工程**都零 Java 引用**，现由 `LootFixtureSemanticsGameTests` 的 7 个用例统一覆盖（缺失/递归引用降级、不支持函数的两层降级、函数有序执行与数量夹取、计分板数量提供器、附魔开关契约、冻结源递归抓取）。
3. **`ExactEnchantmentSemantics1211.ENABLED = false`（`:49`）关掉了整条附魔枚举**：`enchant_with_levels` 因此恒为 no-op（`DistributionalFunction1211.java:265`、`DistributionalLootTableExecutor1211.java:637`），任何含该函数的战利品表，期望值都**系统性偏低**（不含附魔产出）。这不是移植回归——源工程 `ExactEnchantmentSemantics1201.java:34` 同样是 `false`，属**既有临时开关**，移植时忠实保留；但它意味着玩家看到的期望值不含附魔产出。
   - 连带影响：1.21 删除战利品表 `treasure` 布尔量，省略 `options` 即取**整个附魔注册表**（含宝藏专属附魔），候选集因此**变宽**。该差异在 `ENABLED=false` 期间不生效；一旦翻回 `true`，`nested_terminal_child.json` / `mixed_terminal_output.json` 的期望值必须重新复核。
   - `LootFixtureSemanticsGameTests#disabledEnchantmentKeepsTerminalOutputExact` 把当前口径钉成了断言：翻转开关会让它失败，用来强制复核。
4. **几何校验脚本未随移植迁入**：源工程的 `docs/tools/multiblock_geometry_check.py` 与 `docs/design/schemes/*.java` 在目标工程**不存在**（本仓库零 `.py`）。改多方块几何现在只能手工核对 `StructureMinerMultiblock` 的四个集合与 `projectionCounts()`；README 与本文档已按此改写。
5. **`structureminer/output/EquipmentDismantler` 对狼铠的产出量未定稿**：1.21 新增 `ArmorItem.Type.BODY`（狼铠），1.20.1 无对应物，现按同为耐久系数 16 的胸甲口径取材料数（`:143`）。这是等价映射而非忠实移植，属于平衡决策。
6. **移植期的临时探针日志仍在**：`RuntimeLootAstSource.java:143`、`StructureValueCalculator.java:264/295/406/421`、`MainThreadTaskCache.java:73`、`StructureDataOperatorBlockEntity.java:280` 共 6 处以 `[TEMP PROBE]` 前缀打印耗时。移植计划 §4 第 5 条本要求清理移植期临时物，尚未执行。
7. **1.21 数据包格式断层（已修，登记以免复发）**：`minecraft:loot_table` 条目在 1.21 把字段从 `name` 改成 **`value`** —— 原版 1.21.1 默认包 1178 张表里 `value` 出现 34 次（31 处表 id + 3 处内联表），`name` 出现 **0** 次。移植后的引擎与 `RuntimeLootAstSource` 的递归抓取最初都按 1.20.1 的 `name` 解析：条目派发处把嵌套引用误判成 `UNSUPPORTED`，快照抓取处更糟——漏抓被引用表后冻结引擎把引用当作缺失表，**静默少算期望值**（见 §5.5）。现由 `NestedLootTableEntry1211` 统一承载，并由 `LootFixtureSemanticsGameTests#requireEnginesAgree` 与 `#frozenSourceCapturesNestedReferences` 钉住。
   - 同一批 fixture 自己也是 1.20.1 语法（`name` 字段，以及 `set_lore` 缺 1.21 必填的 `mode`），在 1.21.1 上**整张表加载失败**（`Couldn't parse element ... No key value in MapLike[...]`）。因为零 Java 消费，此前无人察觉；其中 `missing_table` 的旧断言曾**假通过**——根表没加载时同样报 `MISSING_REFERENCE`，与"根表存在、被引用表缺失"的语义撞车。**凡新增 fixture，务必先在 `runGameTestServer` 日志里确认它真的加载成功**，否则断言测的是"表不存在"。
8. **内联嵌套表未建模**：`value` 也可以是一整个表对象（`NestedLootTable.content` 是 `Either`），原版 1.21.1 数据里有 3 处（都在 `equipment/trial_chamber`）。引擎按 `UNSUPPORTED` + `INLINE_TABLE_MESSAGE` 处理——不会静默当成空表，但也不是精确值。要支持需给引擎加"直接对一段内联 JSON 求值"的入口。
9. **「精确分析、不做近似 fallback」只对引擎层成立**：`StructureValueCalculator` 在引擎判 `UNSUPPORTED` 且发现状态为 `EXACT` 时会退化为采样兜底（`APPROXIMATE` + `SAMPLING_APPROXIMATION`，`:887-897`），实时与异步两条路径都是这个阶梯。详见 §5.5；`LootFixtureSemanticsGameTests#unsupportedFunctionsPropagateThroughNesting` 按实测把两层的差异分别固化，不再用"UNSUPPORTED"一句话掩盖两种行为。
10. **冻结源路径读不到计分板**：`LootAnalysisContext.snapshot(...)` 无 level，`minecraft:score` 数量提供器返回 null → `UNSUPPORTED`（`DistributionalNumberProvider1211:255`），而实时路径能读出真实分数。生产走冻结路径，故含计分板数量提供器的表在生产中判不支持。
11. **`StructureAnalysisService.discover` 会覆盖 discovery 失败的真实原因**：`MainThreadTaskCache.submit` 在 admission 失败时**直接返回 failed future**（`:38-41`，预算为 `virtualStructureStepsPerTick`，默认每 tick 1 次），`discoverStatic` 自身抛错也是同一条路；`failDiscovery` 随后把 state 标成 FAILED 且 `result()` 为 null，而 `discover` 在 `future == null && state.result() == null` 分支统一抛出 `RejectedExecutionException("Discovery unavailable")`（`:319`）——**原始异常被丢弃**。
    - 实测表现：同一 tick 内提交超过预算的 discovery 请求时，玩家与日志看到的是「Discovery unavailable」，而不是「本 tick 提交预算已用尽」。编写并发触发的 GameTest 时会在这一步踩到（本仓库的 `VanillaStructureLootGameTests` 因此只用同步静态入口断言，对 service 只发一次请求）。
    - 修法：让 `State` 保留失败原因（它已经存在 `detail` 字段），在该分支把它透出，而不是换成一个泛化异常。
