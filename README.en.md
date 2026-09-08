# Dimension Tech

[中文文档](README.md) | [KubeJS documentation](docs/kubejs.md) | [License](LICENSE.txt)

Dimension Tech is a technology mod for Minecraft Forge 1.20.1. Its automation loop is built around the value of generated structures: analyse a structure into a marker, then let a Mythic Miner produce resources from that structure's loot expectations.

Current version: `1.0.0-1.20.1`
Runtime: Minecraft `1.20.1`, Forge `47.4.10` or a compatible version
License: GPL-3.0

## Player Guide

### Core Loop

1. Use the **Structure Data Operator** to browse available structures and analyse or write structure markers.
2. Place a **Mythic Miner** of the desired tier and right-click it to open its controls.
3. Right-click the miner while holding a stick to toggle its multiblock projection, then build the shown structure.
4. Put written **Structure Markers** into the miner, then provide FE power and any required fluid.
5. Configure redstone, item output, fluid faces, and automatic fluid extraction. The miner processes the markers and routes the generated loot.

Structure analysis reads a structure's loot tables and calculates expected item counts and structure value. Analysis can complete asynchronously; a marker is not advanced into a processing job until its result is ready.

### Mythic Miners

Mythic Miners are available from Tier 1 through Tier 6. Each tier can process multiple structure markers independently. Higher tiers provide greater base parallelism, luck, efficiency, and energy specifications. The server's common configuration controls the actual values.

The base multiblock requires:

- 32 Mythic Miner Casings
- 1 Mythic Miner Structure block
- 8 Dimension Foci matching the miner tier
- 12 upgrade blocks

Tier 1 requires no fluid by default. Tiers 2 through 6 respectively require:

| Tier | Required fluid |
| --- | --- |
| 2 | Mythic Essence |
| 3 | Surging Mythic Essence |
| 4 | Recursive Essence |
| 5 | Surging Recursive Essence |
| 6 | Fractal Essence |

Fluid is charged when a processing cycle starts; energy is charged once per natural game tick. Every processing cycle is at least 400 natural ticks long. This preserves predictable resource accounting when an external system accelerates block-entity ticks.

### Structure Data Operator and Markers

The Structure Data Operator manages structure markers:

- With a Data Integrator installed, it can copy the target marker's data to multiple markers.
- With both a Data Integrator and a Structure Interpreter installed, it can browse and analyse the structure catalogue, then write an entry into a target marker.
- A marker stores its target dimension, position or catalogue structure, bounds, value, and expected item counts.

Server configuration or KubeJS can change the value rules for structures, dimensions, and items. A rule change invalidates relevant analysis caches and causes recalculation on the next use.

### Configuration and Compatibility

The Forge common configuration contains base values for all six miner tiers as well as structure values, rarity multipliers, dimension values, item rules, and analysis strategy. Forge creates the configuration file in the instance's `config` directory.

These integrations are optional:

- **KubeJS**: server-side miner overrides, structure-value rules, and work events.
- **Jade**: miner status display.
- **Applied Energistics 2**: ME-network-related output and fluid interaction.

See [docs/kubejs.md](docs/kubejs.md) for the complete KubeJS API, events, and validation rules. `processingTime` must be at least 400; smaller values are rejected explicitly.

## Developer Guide

### Requirements

- JDK 17
- A Minecraft Forge 1.20.1 development environment
- Use `gradlew.bat` on Windows or `./gradlew` on macOS/Linux

Use the Gradle wrapper to download dependencies on first import. The project uses ForgeGradle 6, Parchment mappings, and a Java 17 toolchain.

### Common Commands

```powershell
# Compile, run unit tests, and build the jar
.\gradlew.bat build

# Start a development client or headless server
.\gradlew.bat runClient
.\gradlew.bat runServer

# Run JUnit and Forge GameTests
.\gradlew.bat test
.\gradlew.bat runGameTestServer

# Generate data resources
.\gradlew.bat runData

# Check formatting
.\gradlew.bat spotlessCheck
```

Artifacts are written to `build/libs`. `runData` writes generated assets to `src/generated/resources`.

### Code Map

| Location | Responsibility |
| --- | --- |
| `block/` | Mythic Miners, Structure Data Operator, multiblock projection, and upgrade blocks |
| `block/entity/` | Miner ticks, resource accounting, output, asynchronous marker analysis, and tier implementations |
| `item/` | Structure markers, enchantment marks, fragments, and mining tokens |
| `loot/expectation/` | Loot-table expectation calculation, probability models, and runtime AST snapshots |
| `config/` | Forge common configuration and tier settings |
| `integration/` | Optional KubeJS, Jade, and AE2 integrations |
| `gametest/` and `src/test/` | Forge GameTest and JUnit regression coverage |

### Miner Behavior Contracts

`BaseMinerBlockEntity#serverTick` coordinates the server-side miner lifecycle. Its phases are fixed: refresh structure and upgrades, redstone gate, automatic fluid extraction, pending-output retry, marker and analysis cache, processing plans, work hook, energy and cycle-fluid accounting, slot advancement, cycle hook, loot generation, output hook, and output routing.

Asynchronous marker analysis uses a fingerprint of its inputs. The fingerprint includes algorithm version, active markers, dimension, position, structure and bounds, luck, and analysis configuration. It ignores stack count and non-analysis derived payload. Results from a stale, replaced, cleared, or removed block entity must never be committed.

Add JUnit or GameTest coverage for behavior changes. Tick ordering, resource charging, external tick acceleration, and asynchronous completion should be asserted through observable outcomes rather than private implementation details.

### KubeJS Extension

KubeJS is optional. It can configure miner processing time, energy use, capacity, parallelism, efficiency, luck, and fluid requirements, and exposes `minerWork`, `minerCycle`, and `minerOutput` events. Examples and the complete contract are in [docs/kubejs.md](docs/kubejs.md).

## Contributing

Before submitting changes, run tests appropriate to the affected scope. Changes to miners, loot analysis, or configuration should run:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

Preserve the existing Chinese comments and naming context. Do not add cache indexes or generic abstraction layers without a clear, measured need.

## License

This project is released under the [GNU General Public License v3.0](LICENSE.txt).
