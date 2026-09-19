# 维度科技 / Dimension Tech

[English](README.en.md) | [KubeJS 文档](docs/kubejs.md) | [许可证](LICENSE.txt)

Dimension Tech 是一个面向 Minecraft Forge 1.20.1 的技术模组。它围绕“结构的价值”构建自动化：将世界中的结构转化为可分析的结构标记，再由神话采掘器按结构的战利品期望持续产出资源。

当前版本：`1.0.0-1.20.1`
运行环境：Minecraft `1.20.1`、Forge `47.4.10` 或兼容版本
许可证：GPL-3.0

## 玩家指南

### 核心循环

1. 使用**结构数据操作仪**浏览可用结构，并分析或写入结构标记。
2. 放置对应 Tier 的**神话采掘器**，右键打开其控制界面。
3. 手持木棍右键采掘器以切换多方块投影，按投影完成结构。
4. 将已写入的**结构标记**放入采掘器槽位，接入 FE 能源与需要的流体。
5. 配置红石、物品输出、流体面和自动抽液，等待采掘器完成加工并输出战利品。

结构分析会读取结构的战利品表，并计算每种物品的期望数量与结构价值。计算可能在后台完成；在结果准备好前，采掘器不会把该标记推进为加工任务。

采掘器产出按已分析的物品期望构造奖励权重。它保留长期期望值，但不复现原版 LootTable 的联合随机分布、奖池选择、函数链或随机序列。

### 神话采掘器

采掘器提供 Tier 1 到 Tier 6。所有 Tier 都可以放入多个结构标记并独立推进；更高 Tier 具有更高的基础并行、幸运、效率和能源规格。实际数值由服务器通用配置决定。

多方块的基础材料为：

- 32 个神话采掘器机壳
- 1 个神话采掘器结构块
- 8 个对应 Tier 的维度焦点
- 12 个升级块

Tier 1 默认不需要流体。Tier 2 至 Tier 6 默认分别需要下列流体：

| Tier | 所需流体 |
| --- | --- |
| 2 | 神话精华 |
| 3 | 涌动神话精华 |
| 4 | 递归精华 |
| 5 | 涌动递归精华 |
| 6 | 分形精华 |

流体在一个加工周期开始时扣除，能量按自然游戏刻结算。每个加工周期至少为 400 自然 tick；这是为了让受到外部 tick 加速时仍保持稳定、可预期的资源结算。

### 结构数据操作仪与结构标记

结构数据操作仪用于管理结构标记：

- 将数据整合器放入对应槽位后，可将目标标记的数据复制给多个标记。
- 将结构解释器与数据整合器一同放入后，可浏览并分析结构目录，再把目录项写入目标标记。
- 结构标记会保存目标维度、位置或目录结构、边界、价值和物品期望等分析数据。

结构、维度和物品的价值规则可由服务器配置或 KubeJS 配置修改。配置变化会使相关分析缓存失效，并在下次使用时重新计算。

### 配置与兼容

Forge 通用配置包含六个 Tier 的基础参数，以及结构价值、稀有度倍率、维度价值、物品规则和分析策略。配置文件由 Forge 创建并位于实例的 `config` 目录。

下列集成为可选项：

- **KubeJS**：按服务器脚本覆盖采掘器参数、结构价值规则和工作事件。
- **Jade**：显示采掘器的工作状态。
- **Applied Energistics 2**：支持 ME 网络相关的输出与流体交互。

完整的 KubeJS 配置、事件和验证规则见 [docs/kubejs.md](docs/kubejs.md)。其中 `processingTime` 必须不小于 400；小于该值会被明确拒绝。

## 开发者指南

### 工程要求

- JDK 17
- Minecraft Forge 1.20.1 开发环境
- Windows 可使用 `gradlew.bat`；macOS/Linux 使用 `./gradlew`

首次导入时使用 Gradle 包装器下载依赖。项目使用 ForgeGradle 6、Parchment 映射和 Java 17 工具链。

### 常用命令

```powershell
# 编译、单元测试和打包
.\gradlew.bat build

# 启动开发客户端或无 GUI 服务端
.\gradlew.bat runClient
.\gradlew.bat runServer

# 运行 JUnit 与 Forge GameTest
.\gradlew.bat test
.\gradlew.bat runGameTestServer

# 生成数据资源
.\gradlew.bat runData

# 检查格式
.\gradlew.bat spotlessCheck
```

构建产物位于 `build/libs`。`runData` 会将生成资源写入 `src/generated/resources`。

### 代码地图

| 位置 | 职责 |
| --- | --- |
| `block/` | 神话采掘器、结构数据操作仪、多方块投影与升级方块 |
| `block/entity/` | 采掘器 tick、资源结算、输出、异步标记分析与 Tier 实现 |
| `item/` | 结构标记、附魔标记、碎片与采掘代币 |
| `loot/expectation/` | 战利品表期望计算、概率模型与运行时 AST 快照 |
| `config/` | Forge 通用配置和 Tier 参数 |
| `integration/` | KubeJS、Jade、AE2 等可选集成 |
| `gametest/` 与 `src/test/` | Forge GameTest 和 JUnit 回归测试 |

### 采掘器行为约定

`BaseMinerBlockEntity#serverTick` 是采掘器的服务端协调入口。它遵循固定阶段：更新结构与升级状态、红石判断、自动抽液、待输出重试、标记与分析缓存、加工计划、工作 hook、能源与周期流体结算、槽位推进、周期 hook、战利品生成、输出 hook 和输出路由。

异步标记分析以 fingerprint 识别输入。fingerprint 包括算法版本、有效标记、维度、位置、结构与边界、幸运和分析配置；它忽略物品数量及非分析派生数据。任何过期、被替换、被清空或已移除方块实体的异步结果都不能提交。

请为行为变更补充 JUnit 或 GameTest。尤其是 tick 顺序、资源扣除、外部 tick 加速和异步回调必须通过可观察结果测试，而不是依赖私有实现细节。

### KubeJS 扩展

KubeJS 是可选依赖。它可配置各采掘器的处理时间、能耗、容量、并行、效率、幸运与流体要求，并提供 `minerWork`、`minerCycle` 和 `minerOutput` 事件。接口示例与完整约束见 [docs/kubejs.md](docs/kubejs.md)。

## 贡献

提交前请至少运行与改动范围匹配的测试；涉及采掘器、战利品分析或配置的改动应运行：

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

保留现有的中文注释与命名语境，并避免在无明确性能数据时引入缓存索引或通用抽象层。

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE.txt) 发布。
