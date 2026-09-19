# Dimension Tech —— 代码 Wiki

> 本文档面向需要理解、修改或扩展本模组源代码的开发者。它描述 `src/main/java/com/suntide_20210418/dimensiontech` 的整体架构、模块职责、关键类、依赖关系与运行方式。所有描述均以当前源码为准（README 中的"代码地图"是更短的快速版）。

- 技术栈：Minecraft `1.20.1` + Forge `47.4.10`，Java `17`，Parchment 映射，ForgeGradle 6
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

---

## 1. 总体架构

模组围绕"**结构的价值**"构建自动化：把世界中的结构转化为可分析的结构标记，再由多级采掘器按结构战利品表的**期望产出**持续生产资源。

核心循环（玩家侧）：

```
放置结构数据操作仪 → 浏览/分析结构目录 → 写入结构标记（写入维度/位置/价值/期望数据）
→ 放入对应 Tier 的采掘器标记槽 → 搭好多方块结构 → 接 FE 能源 + 流体
→ 采掘器每周期结算资源 → 输出战力品
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
| `block/` | 方块：`BaseMinerBlock` + `Tier1..6StructureMinerBlock`、`StructureReactorBlock`、`StructureDataOperatorBlock`、多方块 `StructureMinerMultiblock`、`StructureMinerStructureBlock`、升级块，注册表 `ModBlocks` |
| `block/entity/` | 方块实体：`BaseMinerBlockEntity` + Tier1..6、`StructureReactorBlockEntity`、`StructureDataOperatorBlockEntity`、各类 `Miner*Controller`，注册表 `ModBlockEntities` |
| `structureminer/processing` | 加工数值：`ProcessingMath`、外部 tick 加速 `ExternalTickAcceleration` |
| `structureminer/output` | 产出路由 `StructureMinerOutputRouter`、期望奖励 `ExpectationRewardGenerator`、拆解 `EquipmentDismantler` |
| `structurereactor/` | 反应堆配方/公式/字节点/循环及其遥测快照 |
| `structure/analysis` | 结构价值计算、虚拟采样、战利品分析服务 |
| `loot/expectation` | **期望引擎**：确定性战利品表期望分析 |
| `loot/fingerprint` | 分析指纹 `LootAnalysisFingerprint`（缓存/存档稳定性） |
| `loot/` | `DimensionCoreChestLoot` 自定义战利品表 |
| `energy/` | `EnergyContainer`、`SimpleEnergyContainer`（Forge 能量） |
| `fluid/` | `ModFluids`、`EssenceFluidType` |
| `item/` | `ModItems`、`ModCreativeModeTabs`、`StructMarkerItem`、`EnchantmentMarkItem` |
| `network/` | `ModNetwork`：Forge 简单信道包注册 |
| `config/` | `ModConfigs`：ForgeConfigSpec 常见配置 + 各 Tier 参数 |
| `client/gui/screen` | 屏幕 & 页面（`StructureMinerScreen` 等） |
| `client/gui/menu` | 容器 Menu 与布局 |
| `client/` | `ClientModEvents`、`StructMarkerClient`、`StructureMinerProjectionClient`、`ModMenu` |
| `integration/` | 可选模组集成（JEI/Jade/KubeJS/AE2）及 `MinerIntegrationHooks` |
| `datagen/` | 各 DataGen Provider + `DatagenExitWatchdog` |
| `gametest/` | Forge GameTest 场景（服务端集成测试） |
| `utils/` | 通用工具：任务缓存、沙盘脚本配置、Loot 助手等 |
| `src/test/java` | 纯 JUnit 单元测试（特别是 `loot/expectation` 的一批 `*1201Test`） |

---

## 3. 入口与装配

### 3.1 模组入口：`DimensionTechMod`
被 `@Mod("dimension_tech")` 标记的主类。构造函数 `DimensionTechMod(FMLJavaModLoadingContext)` 负责在最早时机组装：

```java
ModConfigs.register(context);               // 最先注册配置
ModBlocks.register(bus); ModFluids.register(bus);
ModItems.register(bus); ModBlockEntities.register(bus);
ModCreativeModeTabs.register(bus);
ModMenu.MENU_TYPES.register(bus);            // 菜单类型注册表
MinecraftForge.EVENT_BUS.register(this);
modEventBus.addListener(…::commonSetup);
modEventBus.addListener(…::registerGameTests);
```

- `commonSetup`：在 `enqueueWork` 内执行 `StructureReactorRecipes.resetDefaults()`（建立默认配方集）与 `ModNetwork.register()`（注册信道）。
- `registerGameTests`：把 14 个 `*GameTests` 类注册到 Forge GameTest 事件，作为服务端集成测试。

### 3.2 各注册表
- `ModBlocks` / `ModBlockEntities` / `ModItems` / `ModFluids`：标准的 `DeferredRegister` 装配点，集中了所有方块/方块实体/物品/流体的注册。
- `ModCreativeModeTabs`：创意标签页。
- `ModMenu.MENU_TYPES`：`DeferredRegister<MenuType<?>>`，与 GUI 的 `ModMenu` 配合。

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

默认值（构造参数顺序：name, parallel, luck, efficiency, capacity, transfer, maxParallel）：

| Tier | baseParallel | luck | eff | capacity FE | 备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | 0.0 | 1 | 100_000 | 无需流体 |
| 2 | 3 | 1.0 | 2 | 400_000 | 神话精华 |
| 3 | 5 | 2.0 | 3 | 1_600_000 | 涌动神话精华 |
| 4 | 7 | 4.0 | 4 | 6_400_000 | 递归精华 |
| 5 | 9 | 8.0 | 6 | 25_600_000 | 涌动递归精华 |
| 6 | 11 | 16.0 | 9 | 102_400_000 | 分形精华 |

另有 `UPGRADE_TIERS` 与 `AGGREGATE_UPGRADE_TIERS`（升级块/聚合升级块逐档数值）。所有数值可被服务器配置或 KubeJS 覆盖。

### 4.3 加工数值：`structureminer/processing`
- `ProcessingMath`：纯函数化的期望/累加计算——`quantityFactorHundredths`（数量因子）、`averageParallel`、`expectedDraws`、`expectedItemCount`，以及 `accumulateHundredths`（把百分位累加为整数 + 有界余数，配合矿机的分数累加规则）。全部用 `BigDecimal`/长整型避免浮点漂移。
- `ExternalTickAcceleration`：处理外部实体 tick 加速（如时间之瓶）带来的加倍/节流逻辑；一个加工周期被固定为至少 400 自然 tick，以保证受加速时资源结算依然稳定可预期。

### 4.4 产出系统：`structureminer/output`
- `StructureMinerOutputRouter`：把生成的 `ItemStack` 按输出面 mask 与目标能力，分发到相邻 item handler 或 AE2 ME 网络（`MinerOutputController` 记录 `ITEM_HANDLER` / `ME_NETWORK` 两种输出状态）。
- `ExpectationRewardGenerator`：基于已分析物品的期望构造奖励权重。
- `EquipmentDismantler`：设备拆解（把已装备的 marker 等从输出中拆回物品）。
- 相关测试：`StructureMinerOutputRouterGameTests`、`StructureMinerLootMergeGameTests`。

### 4.5 能量与流体
- `energy/EnergyContainer`（接口）/`SimpleEnergyContainer`（Forge `EnergyStorage` 实现），接入采掘器 FE 结算。
- `fluid/ModFluids` + `EssenceFluidType`（Forge FluidType），Tier2-6 各需要一种精华流体，在一个周期开始时扣除。

---

## 5. Loot Expectation 引擎（核心）

这是模组最有价值的部分：**离线、确定性地算出某张战利品表（loot table）的期望产出概率分布**，而不是像真实抽取那样逐次随机抽样。价值在于：矿机要"把结构变成稳定产出"，需要先精确知道每种物品的期望数量。

### 5.1 核心思想

1. **随机数状态不可变、可枚举**：引擎重写 1.20.1 的两个随机源——`XoroshiroState1201`（Xoroshiro128++，现代 `RandomSource`）与 `LegacyState1201`（48-bit LCG，`LegacyRandomSource`）。每个随机调用返回 `Draw<T>(value, newState, drawCount)`：因为状态迁移是确定的，给定当前 seed，`nextInt(n)` 等调用会落在一个**有限、可精确计算**的取值集合上，并记录"消耗了几次底层 draw"。
2. **把"随机抽取"升级为"有限概率分布"**：一次 `roll` 不再是一次 `nextDouble()`，而是一张"取值 → 精确概率"的映射（`FiniteDistribution`，要求 mass 精确等于 1）。
3. **用线性期望约化**：`DistributionalLootPool1201` 的注释明确写着通过线性期望（linearity of expectation）避免逐样本随机模拟——期望 roll 次数、单格选择期望、最终期望产出都由分布推导。
4. **保持与原版的逐位语义一致**：随机语义类（Exact/Distributional/Stateful 前缀的 `*1201`）刻意复刻原版 `nextInt`/weighted index/附魔语法的边界行为（幂次快速路径、有符号拒绝条件等），以此保证分析结果与原版抽奖等价。

### 5.2 关键类族（`loot/expectation`）

| 类族 | 代表类 | 职责 |
| --- | --- | --- |
| 随机状态 | `XoroshiroState1201`、`LegacyState1201`、`StatefulRandomSource1201`、`StatefulLegacyRandomSource1201` | 不可变随机数状态；`Draw` 携带新状态与 drawCount |
| Reference 语义 | `ReferenceSemantics1201` | 缺失/递归引用（TABLE/PREDICATE/FUNCTION）在 1.20.1 的精确行为：缺失表→空输出、缺失谓词→false、缺失函数→identity，并产出一条带 JSON 指针和调用栈的 `Diagnostic` |
| 精确语义 | `ExactRandomSemantics1201`、`ExactEnchantmentSemantics1201`、`ExactProbability` | 把原版随机方法定义为有限精确语义（可枚举/可复现），用于附魔等场景 |
| Distributional 求值 | `DistributionalLootTableExecutor1201`、`DistributionalLootPool1201`、`DistributionalLootTableExecutor`、`DistributionalFunction1201`、`DistributionalCondition1201`、`DistributionalNumberProvider1201` | 把一张表/一个池解析成期望与分布；入口 `evaluate` / `expectation` |
| Stateful 执行 | `StatefulLootTableExecutor1201`、`StatefulLootSequenceExecutor1201`、`StatefulLootPool1201`、`StatefulCondition1201`、`StatefulFunction1201`、`StatefulNumberProvider1201` | 有状态地推进"直到产出实际 item stack"的执行模拟 |
| 随机状态分布式 | `RandomStateExecutor1201`、`RandomStateDistribution`、`RandomTraceDistribution`、`PersistentRandomSequenceSnapshot1201` | 随机状态在分布空间里的执行/追踪 |
| 概率/数学 | `FiniteDistribution`、`ExactProbability`、`ExpectationMath`、`IdealRandomProbabilitySpace1201` | 归一化 PMF、精确概率、长程期望计算 |
| 数据结构与产物 | `StackState`、`StackMeasure`、`TerminalStackMeasure`、`StackObservationMeasure`、`StackObservation`、`TerminalStackKey`、`FrozenJson`、`LootExpectationResult`、`LootAnalysisContext`、`Diagnostic`、`MarkerAnalysis` | 分析的输入/输出/中间表示；`LootExpectationResult` 是统一输出 |
| 状态/产物枚举 | `AnalysisStatus`（`EXACT / APPROXIMATE / UNSUPPORTED / LEGACY`）、`EvaluationFailureKind` | 结果状态标记 |
| 可达性 | `Reachability` | 惰性可达性分析，避免展开完整输出列表的乘积爆炸 |
| 外部支持 | `RuntimeLootAstSource`、`SavedDataTransaction1201` | 从运行时/存档读取战利品表数据 |

### 5.3 输出结果：`LootExpectationResult`

record 字段：`AnalysisStatus status`、`StackMeasure measure`、`TerminalStackMeasure terminalMeasure`、`boolean fullStackMeasureAvailable`、`List<Diagnostic> diagnostics`。关键不变式：**非 `EXACT` 状态会清空完整 stack measure**（置空、`fullStackMeasureAvailable=false`），即只有当分析被判定为精确时才暴露完整分布。`exactTerminal(...)` 表示终端聚合但未实体化完整 `StackMeasure`。

### 5.4 指纹：`loot/fingerprint/LootAnalysisFingerprint`

分析指纹由**算法版本、marker slot、luck bits、analysis 配置**构成，用于缓存/复用 marker 战利品分析结果，并保证适配层在存档里可复现、不因无关数据（物品数量、非分析派生字段）变化而失效——这支撑了"后台异步分析"与"结果在下次使用时重算"的机制（任何过期/被替换/已清空/方块实体已被移除的异步结果都不能提交）。

---

## 6. 结构分析与价值评估

`structure/analysis/` 把期望引擎接到"结构价值"上：

- `StructureAnalysisService`：分析服务门面，负责把 loot-expectation 缓存下来（`capture`）。
- `StructureLootAnalyzer`：解析一个结构的战利品表来源。
- `StructureValueCalculator`：价值计算入口，**异步**评估 frozen loot source，并按配置在两条路径间切换：
  - 精确期望路径：对每个 root table 调 `DistributionalLootTableExecutor1201.evaluate`，汇总 terminal/full `StackMeasure` 并冻结为 `LootExpectationSnapshot`。
  - 采样近似路径（`ItemExpectationMethod.SAMPLING` 或非精确场景）：通过 `LootTableLottery.draw` 做 Monte Carlo 采样，把计数/频率折算为期望概率。
- `VirtualStructureSampler`：在虚拟（不落盘）结构中采样结构内容/战利品。
- `StructureValueSnapshot`：一次分析结果的不可变快照。
- 相关 GameTest：`StructureValueCalculatorGameTests`、`VirtualStructureSamplerGameTests`。
- `utils/LootTableLottery`：实际随机抽取工具（对有 `LootParams`、seed 的 `LootTable.getRandomItems`），供采样近似路径使用。

回报价值规则（稀有度倍率、维度价值、物品规则、白/黑名单）由 `ModConfigs.StructureValueConfig` 或 KubeJS 配置提供；配置变化会使相关分析缓存失效。

---

## 7. 结构反应堆 / 结构数据操作仪

### 7.1 结构反应堆（`structurereactor` + `StructureReactorBlockEntity`）
- `StructureReactorRecipe` / `StructureReactorRecipes`：配方模型与注册表（`resetDefaults` 在 commonSetup 建立默认配方）。
- `StructureReactorCycle` / `ReactorFormula` / `OperationMatcher`：反应堆的有限状态循环、公式与配方/输入匹配。
- `StateId` / `StateStep`：反应堆状态机节点建模。
- `ReactorSequenceTelemetry` / `ReactorTooltipSnapshot`：遥测与 tooltip 快照（供 GUI 展示）。
- `StructureReactorBlockEntity`：主循环按 cycle 状态执行 `tick / resolve / tryStart / tryCommit`；启动与提交时校验输入、碎片、配方匹配与输出容量，产出 `outputAmountMb()`。
- 相关 GameTest：`StructureReactorGameTests`、`TransactionGameTests`、`FluidContainerGameTests`、`TankControlGameTests`。

### 7.2 结构数据操作仪（`StructureDataOperatorBlock*`）
- `StructureDataOperatorBlockEntity`：管理结构标记的读写/分析/批量复制。
- 层面：把数据整合器放入槽可复制标记数据；放入结构解释器 + 数据整合器可浏览/分析结构目录并写入目标标记；标记保存目标维度、位置或目录结构、边界、价值与物品期望。

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
- `StructureMinerScreenContext`：向各页暴露的只读状态上下文。
- 其他屏幕：`StructureReactorScreen`、`StructureDataOperatorScreen`、`StructMarkerScreen`、`OutputFaceConfigScreen`。

### 8.2 Menu 层：`client/gui/menu`
`StructureMinerMenu` / `StructureReactorMenu` / `StructureDataOperatorMenu` 继承 `AbstractContainerMenu`，读取 block entity、注册 container slots，并通过 `ContainerData` 同步流体、输出面模式、自动提取等状态，为屏幕提供遥测数据源（`StructureMinerTelemetrySnapshot`）。这是 menu↔screen 数据同步的主路径。

### 8.3 其他客户端
- `ClientModEvents`：客户端事件监听（注册客户端渲染器、键位、覆盖页面类型等）。
- `StructureMinerProjectionClient`：在搭建多方块时投影结构线框。
- `StructMarkerClient`：用 `StructMarkerItem` 时的客户端标记渲染/交互。
- `ModMenu`：`MENU_TYPES` 注册表容器。

---

## 9. 模组集成层（可选依赖）

所有集成都是可选的，仅在对应模组存在时才加载/生效。

| 集成 | 包/主类 | 作用 |
| --- | --- | --- |
| JEI | `integration/jei/StructureReactorJeiPlugin`（+ `Category`/`Recipe(s)`/`Text`） | 展示结构反应堆配方；把反应堆屏幕的配方展示区注册为 JEI 点击区域 |
| Jade | `integration/jade/StructureMinerJadePlugin` + `StructureMinerJadeProvider` | 服务端向 Jade HUD 提供矿机状态、slot marker、进度、并行、输出、能量等显示 |
| KubeJS | `integration/kubejs/DimensionTechKubeJSPlugin`、`MinerEventsJS`、`MinerBlockEntityJS`、`DimensionTechJS` | 向服务器脚本暴露矿机参数/结构价值/工作事件覆写 |
| AE2 | `integration/ae2/Ae2Integration` | ME 网络输出与流体交互 |
| 通用钩子 | `MinerIntegrationHooks` | 统一封装跨集成的矿机钩子点 |

选装依赖的构建处理值得注意（见 `build.gradle`）：数据生成与"原版战利品运行"时以 `compileOnly` 隔离可选模组（`-PvanillaLootRuntime`），避免它们污染生成的资源，或如 KubeJS 持有非 daemon 线程导致 datagen JVM 不退出的问题；客户端/gametest 运行时才以 `implementation` 加入。KubeJS 的接入还通过 `src/main/resources/kubejs.plugins.txt`（`META-INF/services` 风格）被 KubeJS 探测加载。

---

## 10. DataGen / 网络 / 配置 / 资源

### 10.1 DataGen（`datagen/`）
`ModDataGenerator#gatherData` 触发时注册：
- 服务端：`ModRecipesProvider`（配方）、`LootTableProvider` + `ModBlockLootTablesProvider`（方块战利品表）。
- 客户端：`ModItemModelsProvider`、`ModBlockStateProvider`、`ModEnusLangProvider`、`ModZhcnLangProvider`（中英文语言文件）。
- `DatagenExitWatchdog`：因为可选模组（尤其 KubeJS）可能持有非 daemon 线程，DataGen 主线程结束后 JVM 可能不退出；watchdog 检测 lingering 线程并在必要时 `System.exit(0)`。这也覆盖了 IDE 直接跑 `Application` 型 runData 配置的场景。
- 生成产物写入 `src/generated/resources`。

### 10.2 网络（`network/ModNetwork`）
- 使用 Forge `NetworkRegistry.newSimpleChannel` 建立 `main` 信道，版本 `"6"`。
- 注册的包覆盖：marker 交互、矿机分析请求/响应、expected-item/slot toggle、反应堆 tooltip、数据操作仪的操作与目录等消息。

### 10.3 配置（`config/ModConfigs`）
- `ForgeConfigSpec` 构建一个 common 配置（`ModConfig.Type.COMMON`），注册为最早一步。
- 内容：`structureMiner`（各 Tier）、`structureMinerUpgrades`、`structureMinerAggregateUpgrades`、`structureValue`（`StructureValueConfig`：稀有度倍率、`ItemExpectationMethod`、采样数、虚拟结构采样、维度/物品/结构白黑名单、维度价值与物品倍率列表）。
- 配置文件由 Forge 生成在实例 `config` 目录，可被 KubeJS 覆盖；配置变化会使相关分析缓存失效。

### 10.4 资源（`src/main/resources`）
- `META-INF/mods.toml`：声明 forge/minecraft 必选依赖 + ae2/jade/jei 可选依赖（`mandatory=false`，`ordering=AFTER`）。
- `assets/dimension_tech/`：guis 精灵图（`guis/*.png`）、方块/物品纹理与模型、`pack.mcmeta`。
- `data/dimension_tech/`：面向 GameTest 的战利品表/物品修饰器/谓词测试数据（如 `loot_tables/gametest/*`、`predicates/gametest/recursive_predicate.json` 等，专用于回归期望引擎的引用语义）。
- `kubejs.plugins.txt`：KubeJS 插件枚举。

---

## 11. 依赖关系总览

### 11.1 必选依赖（`mods.toml` + `build.gradle`）
- `forgemod`/`minecraft`：开发期通过 `net.minecraftforge:forge:1.20.1-47.4.10`；运行时 `minecraft` 与 `forge` 为必选且 `ordering=NONE`。
- 测试：`org.junit.jupiter:junit-jupiter:5.10.2`（JUnit，`testImplementation`）。

### 11.2 可选模组依赖（`mods.toml` 声明 + 运行时只在存在时生效）
`ae2`（≥15.4.10）、`jade`（≥11.0.0）、`jei`（≥15.20.0，因为用到了 `AbstractRecipeCategory` 与 recipe-extras 文本组件）。

另在 `build.gradle` 中 `add(optionalModConfiguration, …)` 的可选依赖：KubeJS、Jade、GuideME、Applied Energistics 2、Mekanism、Architectury API、Rhino、AllTheModium、GeckoLib、ATO、Time in a Bottle、JEI、EMI（`compileOnly`）、Mouse Tweaks（`compileOnly`，纯客户端便利模组，被刻意排除出 dev 运行时以避免 runData/server 构建它）。其中 `optionalModConfiguration` 依据是否为数据生成/原版战利品运行而切换为 `compileOnly` 或 `implementation`。

### 11.3 代码内部依赖方向（核心）
```
DimensionTechMod ──> 注册表(block/entity/item/fluid/menu) & ModConfigs & ModNetwork & gametest
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
- JDK 17；Minecraft Forge 1.20.1 开发环境。
- Windows 用 `.\gradlew.bat`，macOS/Linux 用 `./gradlew`。
- 首次导入用 Gradle 包装器下载依赖；使用 ForgeGradle 6、Parchment 映射、Java 17 工具链。（仓库同时配置了 Fast-build 的阿里云/BMCLAPI 镜像，方便国内环境。）

### 12.2 常用命令（在项目根 `d:\mycode\ModDevelopment\DimensionTech-1.20.1`）

```powershell
.\gradlew.bat build                  # 编译 + JUnit 单测 + spotless 检查 + 打包 → build/libs
.\gradlew.bat runClient              # 启动开发客户端
.\gradlew.bat runServer              # 启动无 GUI 服务端
.\gradlew.bat test                   # 运行 JUnit 单元测试
.\gradlew.bat runGameTestServer      # 运行 Forge GameTest（含期望引擎/矿机集成测试）
.\gradlew.bat runData                # 数据生成，写入 src/generated/resources
.\gradlew.bat spotlessCheck          # 格式检查
```

- 数据生成专用开关 `-PvanillaLootRuntime=true`：以不携带可选 dev 模组的运行时跑原版战利品语料门禁（GameTest），避免可选模组污染生成物或拖住 JVM。
- `syncGameTestStructures` task 会随 `prepareRunGameTestServer` 把 `src/test/resources/gameteststructures` 同步到对应运行目录。GameTest 结构文件位于 `src/test/resources/gameteststructures`，测试类位于 `gametest/` 与 `src/test/java`。

### 12.3 修改约定（来自 README 与代码卫生）
- 采掘器行为遵守 `BaseMinerBlockEntity#serverTick` 的固定阶段顺序；tick 顺序、资源扣除、外部 tick 加速与异步回调必须用**可观察结果**测试（JUnit / GameTest），不要依赖私有实现细节。
- 异步标记分析以 fingerprint 识别输入；任何过期/被替换/已清空/方块实体已移除的异步结果都不能提交。
- 避免在无性能数据时引入缓存索引或通用抽象层；保留中文注释与命名语境。