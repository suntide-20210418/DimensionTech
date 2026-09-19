<a id="english"></a>

# Dimension Tech KubeJS Integration

[English](#english) | [中文](#chinese)

This integration targets Dimension Tech 1.20.1 with KubeJS for Forge. KubeJS is optional; without it, Dimension Tech uses its normal machine values.

## Configuration

Put this in `kubejs/server_scripts/dimension_tech.js`:

```js
DimensionTech.miner('dimension_tech:tier_1_miner', {
  processingTime: 400,
  energyConsumption: 50,
  energyCapacity: 10000,
  baseParallel: 2,
  efficiency: 1.5,
  luck: 0.2,
  requiresFluid: false
})

// The fluent form is also supported.
DimensionTech.miner('dimension_tech:tier_2_miner')
  .processingTime(600)
  .energyConsumption(80)
  .requiresFluid(true)
```

Configuration is server-scoped and is rebuilt after every KubeJS server-script reload. Values are keyed by block ID, so custom blocks can use the same API if they use Dimension Tech's miner block entity.

`processingTime` is a minimum of 400 ticks. This is the same natural-tick window used to
account for external tick acceleration, so shorter configured cycles are rejected.

## Events

```js
DimensionTechEvents.minerWork(event => {
  // This is a server-tick event. Keep handlers lightweight.
  if (event.miner.blockId() === 'dimension_tech:tier_1_miner' &&
      event.miner.energyStored() < event.miner.energyConsumption()) {
    event.cancel()
  }
})

DimensionTechEvents.minerCycle(event => {
  console.log(`miner cycle: slot=${event.slot}, parallel=${event.parallel}`)
})

DimensionTechEvents.minerOutput(event => {
  event.outputs = event.outputs.filter(stack => stack.id !== 'minecraft:cobblestone')
  // event.cancel() discards this output entirely.
})
```

`minerWork` fires before the current server tick consumes energy or advances slot progress. `minerCycle` fires when a slot completes a cycle; cancelling it prevents that cycle from generating loot. `minerOutput` fires once for newly generated loot, before insertion into adjacent inventories or an ME network. Retrying an existing pending output does not fire the event again.

Each event exposes `miner`, `level`, `position`, `slot`, `marker`, `outputs`, `parallel`, `progress`, and `processingTime`. `slot` and `marker` are `-1`/`null` for events that apply to the whole machine.

## Structure analysis and value configuration

These overrides affect structure discovery, expected loot calculation, value calculation, and the cached analysis fingerprint:

```js
DimensionTech.structureConfig()
  .dimensionValue('minecraft:the_nether', 25)
  .itemMultiplier('minecraft:diamond', 100)
  .rarityMultiplier('rare', 20)
  .itemWhitelist(['minecraft:.*', 'my_mod:valuable_.*'])
  .itemBlacklist(['minecraft:cobblestone'])
  .structureBlacklist(['minecraft:stronghold'])
  .dimensionWhitelist(['minecraft:overworld', 'minecraft:the_nether'])
  .itemExpectationMethod('EXACT_THEN_SAMPLING')
  .samplingCount(5000)
  .virtualStructureSamples(16)
  .virtualStructureStepsPerTick(4)
```

Whitelist entries are regular expressions matched against complete resource IDs. A blacklist always denies a match; a non-empty whitelist requires a match. Script values temporarily override the Forge common config for the current server and are cleared/rebuilt on KubeJS server reload.

## Miner API

The `miner` wrapper exposes machine telemetry (`energyStored`, `energyCapacity`, `effectiveEnergyCapacity`, `energyConsumption`, `efficiency`, `luck`, `baseParallel`, `progress`, `slotProgress`, `fluidAmount`, `structureComplete`, and more) and validated controls:

```js
event.miner.setSlotEnabled(0, false)
event.miner.setRedstoneMode('no_signal')
event.miner.setOutputMode('item_handler')
event.miner.setOutputFace('north', true)
event.miner.setAutoExtractFluid(true)
event.miner.setEquipmentDismantling(false)
event.miner.setExpectedItemDisabled('minecraft:diamond', true)
```

All controls run on the server thread and mark the block entity dirty for saving. Invalid modes, directions, IDs, or configuration values raise a KubeJS script error instead of silently changing state.

## Reward semantics

Miner rewards are generated from analysed item expectations. This is expectation-preserving reward generation; it does not reproduce Vanilla LootTable's original joint distribution, pool selection, function chain, or random sequence.

<a id="chinese"></a>

# Dimension Tech KubeJS 集成

[English](#english) | [中文](#chinese)

本集成面向 Forge 版 KubeJS 与 Dimension Tech 1.20.1。KubeJS 是可选依赖；未安装时，Dimension Tech 使用矿机的正常默认值。

## 配置

将以下内容放入 `kubejs/server_scripts/dimension_tech.js`：

```js
DimensionTech.miner('dimension_tech:tier_1_miner', {
  processingTime: 400,
  energyConsumption: 50,
  energyCapacity: 10000,
  baseParallel: 2,
  efficiency: 1.5,
  luck: 0.2,
  requiresFluid: false
})

// 同时支持链式写法。
DimensionTech.miner('dimension_tech:tier_2_miner')
  .processingTime(600)
  .energyConsumption(80)
  .requiresFluid(true)
```

配置以服务器为作用域，并会在每次 KubeJS 服务端脚本重载后重新构建。配置按方块 ID 取值，因此使用 Dimension Tech 采掘器方块实体的自定义方块也可使用相同 API。

`processingTime` 的最小值为 400 tick。这一数值与外部 tick 加速的自然 tick 观测窗口相同，因此更短的加工周期会被明确拒绝。

## 事件

```js
DimensionTechEvents.minerWork(event => {
  // 这是服务端 tick 事件；处理函数应保持轻量。
  if (event.miner.blockId() === 'dimension_tech:tier_1_miner' &&
      event.miner.energyStored() < event.miner.energyConsumption()) {
    event.cancel()
  }
})

DimensionTechEvents.minerCycle(event => {
  console.log(`miner cycle: slot=${event.slot}, parallel=${event.parallel}`)
})

DimensionTechEvents.minerOutput(event => {
  event.outputs = event.outputs.filter(stack => stack.id !== 'minecraft:cobblestone')
  // event.cancel() 会直接丢弃这批输出。
})
```

`minerWork` 在当前服务端 tick 扣除能量或推进槽位进度前触发。`minerCycle` 在槽位完成一个加工周期时触发；取消该事件会阻止该周期生成战利品。`minerOutput` 对新生成的战利品触发一次，发生在插入相邻物品栏或 ME 网络之前。重试已有的待输出物品不会再次触发该事件。

每个事件均提供 `miner`、`level`、`position`、`slot`、`marker`、`outputs`、`parallel`、`progress` 和 `processingTime`。适用于整台机器的事件中，`slot` 为 `-1`，`marker` 为 `null`。

## 结构分析与价值配置

以下覆盖项会影响结构发现、战利品期望计算、价值计算和缓存中的分析 fingerprint：

```js
DimensionTech.structureConfig()
  .dimensionValue('minecraft:the_nether', 25)
  .itemMultiplier('minecraft:diamond', 100)
  .rarityMultiplier('rare', 20)
  .itemWhitelist(['minecraft:.*', 'my_mod:valuable_.*'])
  .itemBlacklist(['minecraft:cobblestone'])
  .structureBlacklist(['minecraft:stronghold'])
  .dimensionWhitelist(['minecraft:overworld', 'minecraft:the_nether'])
  .itemExpectationMethod('EXACT_THEN_SAMPLING')
  .samplingCount(5000)
  .virtualStructureSamples(16)
  .virtualStructureStepsPerTick(4)
```

白名单条目是与完整资源 ID 匹配的正则表达式。黑名单始终优先拒绝匹配；当白名单非空时，目标必须命中白名单。脚本值会暂时覆盖当前服务器的 Forge 通用配置，并在 KubeJS 服务端脚本重载时清除和重建。

## 采掘器 API

`miner` 包装器提供机器遥测数据，例如 `energyStored`、`energyCapacity`、`effectiveEnergyCapacity`、`energyConsumption`、`efficiency`、`luck`、`baseParallel`、`progress`、`slotProgress`、`fluidAmount`、`structureComplete` 等，也提供经过验证的控制方法：

```js
event.miner.setSlotEnabled(0, false)
event.miner.setRedstoneMode('no_signal')
event.miner.setOutputMode('item_handler')
event.miner.setOutputFace('north', true)
event.miner.setAutoExtractFluid(true)
event.miner.setEquipmentDismantling(false)
event.miner.setExpectedItemDisabled('minecraft:diamond', true)
```

所有控制方法都在服务端线程执行，并会将方块实体标记为待保存。无效的模式、方向、资源 ID 或配置值会抛出 KubeJS 脚本错误，而不会静默改变状态。

## 奖励语义

采掘器奖励按已分析的物品期望生成。这属于保持期望值的奖励生成，不复现原版 LootTable 的联合分布、奖池选择、函数链或随机序列。
