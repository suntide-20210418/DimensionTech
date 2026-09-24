# 移植基线记录

本文件记录 Forge 1.20.1 → NeoForge 1.21.1 移植的基线锚点。

## 源工程锚点

- 路径：`D:\mycode\ModDevelopment\DimensionTech-1.20.1`
- 当前锚点提交：`30eb0e2`（2026-09-23 21:13，工作树干净）
- 基线记录时的提交：`e878c79ec2cc62aafb08c3593c6a4c84ad267983`
- 版本：Forge 47.4.10 / MC 1.20.1 / Java 17 / ForgeGradle
- 规模：205 个 Java 文件 / 40,199 行

## 快照过时事故（2026-09-24 补记）

**原计划要求源工程冻结，但实际上没有冻结**，这在移植中途造成了一次真实的资产损坏与一次
功能修复遗漏。记录在此，避免重犯：

- 我复制 `assets` 的时间是 20:47:44，而源工程在 **21:13** 收到了 `7b68fa5` / `1be6908`
  （同一修复的两个分支副本 + 合并 `30eb0e2`）。21:11–21:13 之间源工程工作树还处于过渡态。
- 后果 1：`models/block/structure_miner_strcture.json` 的贴图引用被我取到了当时的占位名
  `shell`（会被解析成不存在的 `minecraft:shell`），**采掘器结构方块渲染为紫黑块**。
  该占位名在源工程 git 历史中从未被提交过，纯属瞬时工作树状态。已于 `0817971` 修复。
- 后果 2：`1be6908` 的 Java 修复（`StructureMinerMultiblock.isFilled` 统一"槽位是否已填"判定、
  `isComplete` 改走投影 + `LevelReader`、投影失效判定加入结构完整性、距离阈值 96→16、新增
  `message.dimension_tech.structure_miner.structure_complete`）**完全没进移植版**。该缺陷的
  用户可见表现是：升级槽位被升级方块占用时，12 个槽位被判为"位置被阻挡"，一键搭建直接退出。
  已于 `7b66700` 同步。

**已核验的同步状态**：`src/main/resources/assets` 与源工程逐字节一致，仅剩 3 处有意的
`render_type` 新增（`strcture_reactor` / `structre_miner_glass` / `structure_data_operator`）；
`1be6908` 删除的 11 个 generated 占位模型（`dimension_focus_tier_1..6`、`tier_2..6_structure_miner`）
在目标工程本就不存在，状态一致。

**教训**：跨版本移植必须把源工程锚定到一个**具体提交**并在其上打 tag，而不是依赖"工作树
当前状态"；复制资产时若源工程有未提交改动，会静默取到过渡态。

## 目标工程锚点

- 路径：`D:\mycode\ModDevelopment\DimensionTech-1.21.1`
- 分支：`suntide/1.21.1`
- 版本：NeoForge 21.1.251 / MC 1.21.1 / Java 21 / ModDevGradle 2.0.147

## 移植计划

见 [.trae/documents/dimensiontech-forge1201-to-neoforge1211-port-plan.md](../.trae/documents/dimensiontech-forge1201-to-neoforge1211-port-plan.md)。
