# Dimension Tech

[中文文档](README.md) | [Code wiki](docs/code-wiki.md) | [KubeJS documentation](docs/kubejs.md) | [License](LICENSE.txt)

Dimension Tech is a technology mod. Its automation loop is built around the value of generated structures: analyse a structure into a marker, then let a Mythic Miner produce resources from that structure's loot expectations.

Current version: `1.0.0-1.21.1`
Runtime: Minecraft `1.21.1`, NeoForge `21.1.251` or a compatible version
License: GPL-3.0

## Player Guide

### Core Loop

1. Stand **inside a structure** and right-click with a **Structure Marker**: the screen lists every structure your position hits, and picking one writes its dimension, position, and bounds into the marker and computes the loot expectation. For loose containers, use the **Chest Marker** instead: press `V` to analyse and mark the container under the crosshair.
2. Place a **Mythic Miner** of the desired tier and right-click it to open its controls.
3. Right-click the miner while holding a **wrench** to toggle its multiblock projection; **shift+right-click** to build the multiblock from your inventory in one go. Then build the shown structure.
4. Put written **Structure Markers** into the miner, then provide FE power and any required fluid.
5. Configure redstone, item output, fluid faces, and automatic fluid extraction. The miner processes the markers and routes the generated loot. On top of the structure loot, every cycle also yields the miner tier's own **dimension fragments** and **mining tokens** (`min(10, parallel)` of each).
6. Once you have tier 2 fragments and tokens, craft the **Data Integrator**; tier 5 fragments and tokens give you the **Structure Interpreter** (which also needs a Data Integrator, a nether star, and netherite). Only with both in the **Structure Data Operator** do you unlock the structure catalogue and bulk writing — which is why that machine sits after the miner rather than at the start of the loop.

Structure analysis reads a structure's loot tables and calculates expected item counts and structure value. Analysis can complete asynchronously; a marker is not advanced into a processing job until its result is ready.

Miner output uses expectation-preserving reward generation from analysed item expectations. It preserves long-run expectations, but does not replay Vanilla LootTable joint distributions, pool selection, function chains, or random sequences.

### Mythic Miners

Mythic Miners are available from Tier 1 through Tier 6. Each tier differs in marker slot count, base parallelism, luck, efficiency, and energy specifications (Tier 1 has a single marker slot, Tier 6 has nine), and each slot advances independently. The server's common configuration controls the actual values.

The base multiblock requires:

- 40 Mythic Miner Casings
- 12 Mythic Miner Glass
- 2 Mythic Miner Structure blocks
- 12 upgrade slots (each holds an upgrade block or a structure block)

The whole machine hangs below the miner itself (the miner owns the top layer): clear a 5x5x5 space underneath it first. The projection marks every position. The glass replaces the focus blocks the old layout needed, running through the waist so the casing sides stay see-through.

Every tier requires a fluid input by default (the gate is `BaseMinerBlockEntity#requiresFluidInput`, which defaults to `tier >= 1`; KubeJS can turn it off through `requiresFluid(false)`). The required fluids are:

| Tier | Required fluid |
| --- | --- |
| 1 | Water |
| 2 | Mythic Essence |
| 3 | Surging Mythic Essence |
| 4 | Recursive Essence |
| 5 | Surging Recursive Essence |
| 6 | Fractal Essence |

Fluid is charged when a processing cycle starts; energy is charged once per natural game tick. Every processing cycle is at least 400 natural ticks long. This preserves predictable resource accounting when an external system accelerates block-entity ticks.

On top of the structure loot, every processing cycle yields three extra things: `min(10, parallel)` **dimension fragments** and the same number of **mining tokens**, both of the miner's own tier, plus **dimension deconstruction cores** rolled `parallel` times at `min(1, 5% x tier)` each and capped at 10 per cycle (the expected-item toggle can turn them off). Fragments and tokens are the ingredients of the Data Integrator and the Structure Interpreter, so those two parts can only come after the miner.

### Structure Reactor

The **Structure Reactor** is a single-block machine and the main line for structure-related crafting (such as producing dimension fragments and other dimension products). It has two 16,000 mB essence tanks, a fragment slot, and an immediate operation slot, advancing recipes through a finite-state cycle. Starting and committing a cycle validates input fluid, fragments, recipe matching, and output capacity, and it can emit a redstone analog signal.

The default recipes escalate the essence chain one step at a time: water to Mythic Essence to Surging Mythic Essence to Recursive Essence to Surging Recursive Essence to Fractal Essence, each step consuming the dimension fragment of the matching tier. KubeJS can override the recipes; see the in-game GUI, JEI, and the [KubeJS documentation](docs/kubejs.md) for concrete values and telemetry.

### Structure Data Operator and Markers

The machine needs two prerequisite parts, and both recipes consume miner output: the **Data Integrator** takes tier 2 dimension fragments and mining tokens (plus quartz, amethyst shards, a casing, and redstone), while the **Structure Interpreter** takes tier 5 fragments and tokens plus a nether star and netherite, and one **Data Integrator** in its own recipe. That is why the operator sits after the miner.

The operator **cannot browse structures on its own**: the catalogue needs a **Data Integrator** in the integrator slot first, and then a **Structure Interpreter** in the interpreter slot — that slot only accepts the interpreter while the integrator is in place, and the integrator cannot be pulled out while the interpreter sits there. With both installed the catalogue appears, so you can browse and analyse structures and write an entry into the Structure Marker held in the target slot.

- **Copying needs no prerequisite part**: put a marker that already carries data in the target slot and the markers to fill in the write slots, and the operator copies the target's data into all of them.
- A **Structure Marker** stores its target dimension, position or catalogue structure, bounds, value, and expected item counts.

The **Chest Marker** handles loose containers: hold it and press the analyse key (default `V`) to run an expectation analysis on the container under the crosshair — if that container references a loot table, that table becomes the source and the container gets marked. The action is key-bound rather than right-click, because right-clicking a chest opens it first; right-clicking the Chest Marker only opens the analysis screen for the marked contents (chest value and calculation method).

The Chest Marker and the Structure Marker share the same marker NBT (`StructureMarkerData`; a chest adds a nested `ChestData` tag holding the loot table and seed). Downstream only reads the NBT structure and never the item type, so the operator's copy mode and the miner's production chain consume both. The only difference is the analysis profile: a chest registers no structure, so its profile uses the recorded loot table alone and never touches structure templates or level loading.

Expected counts come first from an **exact expectation of the loot table**: when the whole table can be modelled as a finite probability space, the number shown is exact (status "exact"). The few mechanisms that cannot be modelled exactly today (enchantment enumeration being off, unsupported loot functions, and so on) fall back to a **sampled estimate** and are labelled "approximate" rather than being passed off as exact; the "unsupported" label is reserved for the cases where not even an estimate is possible. **Enchantment output is currently excluded from the expectation**: enchantment enumeration is switched off, so `enchant_with_levels` is treated as yielding an un-enchanted item, which makes any table that enchants read slightly lower than what it can actually produce (reasoning and impact: [docs/code-wiki.md](docs/code-wiki.md) §13).

Server configuration or KubeJS can change the value rules for structures, dimensions, and items. A rule change invalidates relevant analysis caches and causes recalculation on the next use.

### Configuration and Compatibility

The NeoForge common configuration contains base values for all six miner tiers as well as structure values, rarity multipliers, dimension values, item rules, and analysis strategy. NeoForge creates the configuration file in the instance's `config` directory.

These integrations are optional:

- **KubeJS**: server-side miner overrides, reactor recipes, structure-value rules, and work events.
- **Jade**: miner status display.
- **Applied Energistics 2**: ME-network-related output and fluid interaction.
- **JEI**: recipes for the reactor, the miner, and the deconstruction core.

See [docs/kubejs.md](docs/kubejs.md) for the complete KubeJS API, events, and validation rules. `processingTime` must be at least 400; smaller values are rejected explicitly.

## Developer Guide

### Requirements

- JDK 21 (NeoForge 1.21.1 requires a Java 21 toolchain)
- A Minecraft NeoForge 1.21.1 development environment
- Use `gradlew.bat` on Windows or `./gradlew` on macOS/Linux

Use the Gradle wrapper to download dependencies on first import. The project uses ModDevGradle 2, Parchment mappings, and a Java 21 toolchain.

### Common Commands

```powershell
# Compile, check formatting, and build the jar
.\gradlew.bat build

# Recompile Java only (the usual command after a signature change)
.\gradlew.bat compileJava --rerun

# Start a development client or headless server
.\gradlew.bat runClient
.\gradlew.bat runServer

# Generate data resources
.\gradlew.bat runData

# Check formatting
.\gradlew.bat spotlessCheck
```

Artifacts are written to `build/libs`. `runData` writes generated assets to `src/generated/resources`.

Do not run `spotlessApply`: the repository's aosp 100-column configuration rewrites every file and then fails its own check. Use `spotlessCheck` only.

### Code Map

| Location | Responsibility |
| --- | --- |
| `block/` | Mythic Miners, Structure Reactor, Structure Data Operator, multiblock layout and projection, casing/glass/upgrade blocks |
| `block/entity/` | Miner ticks, resource accounting, output, asynchronous marker analysis, reactor cycle, and tier implementations |
| `structureminer/` | Miner processing math (`ProcessingMath`, external tick acceleration) and output routing |
| `structurereactor/` | Reactor recipes, formulas, state steps, and telemetry |
| `structure/analysis/` | Structure value calculation, virtual sampling, and loot analysis service |
| `item/` | Structure Marker/Chest Marker, wrench, dimension-deconstruction core, data integrator, structure interpreter, fragments, and mining tokens |
| `loot/expectation/` | Loot-table expectation calculation, probability models, and runtime AST snapshots |
| `loot/fingerprint/` | Analysis fingerprint (cache/save stability) |
| `recipe/` | RecipeSerializer registration |
| `config/` | Forge common configuration and tier settings |
| `integration/` | Optional KubeJS, Jade, AE2, and JEI integrations |
| `datagen/` | Data generation for recipes, block loot tables, models, and both language files |

See [docs/code-wiki.md](docs/code-wiki.md) for the full module responsibilities, key classes, and dependency directions.

### Miner Behavior Contracts

`BaseMinerBlockEntity#serverTick` coordinates the server-side miner lifecycle. Its phases are fixed: refresh structure and upgrades, redstone gate, automatic fluid extraction, pending-output retry, marker and analysis cache, processing plans, work hook, energy and cycle-fluid accounting, slot advancement, cycle hook, loot generation, output hook, and output routing.

Asynchronous marker analysis uses a fingerprint of its inputs. The fingerprint includes algorithm version, active markers, dimension, position, structure and bounds, luck, and analysis configuration. It ignores stack count and non-analysis derived payload. Results from a stale, replaced, cleared, or removed block entity must never be committed.

Touching the multiblock coordinates means re-verifying counts, overlap, envelope coverage, D4 symmetry, face connectivity, and the controller contract; the four collections in `StructureMinerMultiblock` and `projectionCounts()` are the only source of truth, so do not count cells by hand. The original project verified these invariants with `docs/tools/multiblock_geometry_check.py`, but that script was **not carried over**, so verification is manual for now (see [docs/code-wiki.md](docs/code-wiki.md) §13).

### KubeJS Extension

KubeJS is optional. It can configure miner processing time, energy use, capacity, parallelism, efficiency, luck, and fluid requirements, override structure reactor recipes, and exposes `minerWork`, `minerCycle`, and `minerOutput` events. Examples and the complete contract are in [docs/kubejs.md](docs/kubejs.md).

## Contributing

Before submitting changes, make sure `.\gradlew.bat build` passes (compile, format check, and packaging). For changes to miners, loot analysis, or configuration, run the affected loop once in a development client or server.

Preserve the existing Chinese comments and naming context. Do not add cache indexes or generic abstraction layers without a clear, measured need.

## License

This project is released under the [GNU General Public License v3.0](LICENSE.txt).
