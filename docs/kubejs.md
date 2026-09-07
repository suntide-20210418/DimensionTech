# Dimension Tech KubeJS Integration

This integration targets Dimension Tech 1.20.1 with KubeJS for Forge. KubeJS is optional; without it, Dimension Tech uses its normal machine values.

## Configuration

Put this in `kubejs/server_scripts/dimension_tech.js`:

```js
DimensionTech.miner('dimension_tech:tier_1_miner', {
  processingTime: 200,
  energyConsumption: 50,
  energyCapacity: 10000,
  baseParallel: 2,
  efficiency: 1.5,
  luck: 0.2,
  requiresFluid: false
})

// The fluent form is also supported.
DimensionTech.miner('dimension_tech:tier_2_miner')
  .processingTime(300)
  .energyConsumption(80)
  .requiresFluid(true)
```

Configuration is server-scoped and is rebuilt after every KubeJS server-script reload. Values are keyed by block ID, so custom blocks can use the same API if they use Dimension Tech's miner block entity.

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
