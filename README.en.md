# Data Recipe Generator

**[English](README.en.md) | [简体中文](README.md)**

[![Build](https://github.com/lyingice/data_recipe_generator/actions/workflows/build.yml/badge.svg)](https://github.com/lyingice/data_recipe_generator/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Forge 1.20.1](https://img.shields.io/badge/Forge-1.20.1-orange)
![NeoForge 1.21.1](https://img.shields.io/badge/NeoForge-1.21.1-blue)
![NeoForge 26.1.2](https://img.shields.io/badge/NeoForge-26.1.2-blue)

Generate loadable datapack recipe JSON from **native-looking container GUIs**.  
Supported loaders: **Forge 1.20.1**, **NeoForge 1.21.1** and **NeoForge 26.1.2 (Minecraft 26.1.2)**.

[Download the latest release](https://github.com/lyingice/data_recipe_generator/releases) ·
[Report an issue](https://github.com/lyingice/data_recipe_generator/issues) ·
[Contributing](CONTRIBUTING.md)

---

## 1. Features

- Open the generator with a command: `/datarecipe` or `/drg`; direct shortcuts such as `/drg furnace` and `/drg smithing_trim` are also available.
- Switch containers with tabs at the top. The left side uses the vanilla container texture and real slot coordinates; the right side is the options panel.
- The server reads the menu slots when generating, so the client only sends options and cannot forge item data.
- Four filename modes are preserved: custom, auto, auto+suffix and input→output.
- Output is a ready-to-use datapack folder containing `pack.mcmeta` and `data/<namespace>/recipe(s)/`.
- Write paths are restricted to `data_recipe_generator_output`; traversal (`..`) and absolute paths are rejected.

## 2. Supported containers and recipe types

| Container | Forge 1.20.1 | NeoForge 1.21.1 | NeoForge 26.1.2 |
|---|---|---|---|
| Crafting table | `crafting_shaped`, `crafting_shapeless` | `crafting_shaped`, `crafting_shapeless` | same, plus `crafting_transmute`, `crafting_imbue` |
| Furnace | `smelting` | `smelting` | `smelting` |
| Blast furnace | `blasting` | `blasting` | `blasting` |
| Smoker | `smoking` | `smoking` | `smoking` |
| Campfire | `campfire_cooking` | `campfire_cooking` | `campfire_cooking` |
| Stonecutter | `stonecutting` | `stonecutting` | `stonecutting` |
| Smithing table | `smithing_transform`, `smithing_trim` | `smithing_transform`, `smithing_trim` | `smithing_transform`, `smithing_trim` |

Containers that are **not** listed (brewing stand, anvil, grindstone, enchanting table, loom, cartography table, beacon, etc.) have no data-driven recipe JSON in vanilla, so there is nothing to generate.

Version differences:

- **1.20.1**: `recipes` folder; ingredients use `{"item": ...}` / `{"tag": ...}` objects; crafting/smithing results use `{"item": "...", "count": n}`; **cooking results must be a string and always produce one item**; stonecutting uses a string `result` plus `count`; smithing trim derives its pattern from the template item and has no `pattern` field.
- **1.21.1**: `recipe` folder; ingredients still use `{"item": ...}` / `{"tag": ...}` objects; results use `{"id": "...", "count": n}`; cooking results use `{"id": "..."}` without a count (always one item); stonecutting keeps its `group` field; smithing transform requires template/base/addition; smithing trim derives its pattern from the template item and has no `pattern` field.
- **26.1.2**: `recipe` folder; ingredients are holder-set strings (`"minecraft:stone"` / `"#tag"`); results use `{"id": "...", "count": n}`; stonecutting results are objects and no longer have `group`; cooking preserves the count; smithing trim requires a `pattern` field (default `minecraft:sentry`).

## 3. Usage

1. Run `/drg` (crafting table by default) or `/drg <container>`.
2. Use the tab row to switch containers and the first panel button to switch recipe variants.
3. Place ingredients in the slots like a normal container and put the desired **result item** in the output slot.
4. Set the namespace, filename mode, filename and output subfolder, then press **Generate recipe**.
5. The status line and chat message show the generated path.

### Commands

| Command | Container / recipe type |
|---|---|
| `/drg`, `/drg crafting` | Crafting table (shaped) |
| `/drg crafting_shaped` | Crafting table (shaped) |
| `/drg crafting_shapeless` | Crafting table (shapeless) |
| `/drg transmute` | Crafting table (transmute, 26.1.2 only) |
| `/drg imbue` | Crafting table (imbue, 26.1.2 only) |
| `/drg furnace`, `/drg smelting` | Furnace |
| `/drg blast_furnace`, `/drg blasting` | Blast furnace |
| `/drg smoker`, `/drg smoking` | Smoker |
| `/drg campfire`, `/drg campfire_cooking` | Campfire |
| `/drg stonecutter`, `/drg stonecutting` | Stonecutter |
| `/drg smithing`, `/drg smithing_transform` | Smithing table (transform) |
| `/drg smithing_trim` | Smithing table (trim) |

Permission: gamemaster level 2. The owner of a single-player world can use it even when cheats are disabled.

### Slot roles

- **Shaped / shapeless**: 3×3 crafting slots plus the output slot. Shaped patterns are automatically cropped to their used rows and columns.
- **Transmute (26.1.2)**: the **center slot** is the input item; every other filled slot is the same material.
- **Imbue (26.1.2)**: the **center slot** is the source (for example a potion); all eight other slots must be filled with the same material.
- **Smelting / blasting / smoking / campfire**: input slot plus output slot; the fuel slot is ignored.
- **Stonecutting**: input slot plus output slot.
- **Smithing transform**: template, base, addition and result.
- **Smithing trim**: template, base and addition; 26.1.2 additionally needs a trim pattern ID, while 1.20.1/1.21.1 derive it from the template item.

### Filename modes

| Mode | Default filename |
|---|---|
| custom | the user-supplied name; falls back to the item ID |
| auto | result item ID (e.g. `diamond_sword`) |
| auto+suffix | result item ID + `_` + user suffix |
| input→output | input ID + `_to_` + result ID (e.g. `iron_ingot_to_iron_block`) |

Smithing trim uses `base_trim[_pattern]`. All filenames are lowercased and unsafe characters are replaced with `_`.

### Output layout

By default the generator writes to the server directory:

```text
<server directory>/data_recipe_generator_output/
├── pack.mcmeta
└── data/<namespace>/
    ├── recipe/<file>.json      # 1.21.1 / 26.1.2
    └── recipes/<file>.json     # 1.20.1
```

The output folder field is a relative subfolder under `data_recipe_generator_output` (for example `my_pack`), useful for separating packs. Leave it empty to write to the root.

Copy the output folder (or a subfolder) into the world's `datapacks/` folder and run `/reload`. `pack.mcmeta` is generated with the correct format for the target version.

## 4. Building and running

Requirements:

- Gradle Wrapper 9.1.0 (included).
- Forge 1.20.1 uses Java 17.
- NeoForge 1.21.1 uses Java 21.
- NeoForge 26.1.2 uses Java 25; the foojay toolchain resolver downloads it automatically when missing.
- The first build needs network access for the Forge/NeoForge userdev, Minecraft and mappings.

```bash
# Build all modules and run the common tests
./gradlew build

# Build only the three platform jars
./gradlew buildAll

# Clients
./gradlew :forge-1.20.1:runClient
./gradlew :neoforge-1.21.1:runClient
./gradlew :neoforge-26.1.2:runClient

# Servers
./gradlew :forge-1.20.1:runServer
./gradlew :neoforge-1.21.1:runServer
./gradlew :neoforge-26.1.2:runServer
```

Artifacts:

```text
forge-1.20.1/build/libs/data_recipe_generator-forge-1.20.1-1.0.0.jar
neoforge-1.21.1/build/libs/data_recipe_generator-neoforge-1.21.1-1.0.0.jar
neoforge-26.1.2/build/libs/data_recipe_generator-neoforge-26.1.2-1.0.0.jar
```

Install only the jar for your loader. `common` is a compile-time shared module; its classes are folded into the three platform jars and it must not be installed separately.

Prebuilt jars are available on the [releases page](https://github.com/lyingice/data_recipe_generator/releases).

## 5. Project layout

```text
data_recipe_generator/
├── common/                 # Minecraft-free shared layer
│   ├── recipe model, JSON generation, naming modes, path checks, datapack writer
│   └── unit tests
├── forge-1.20.1/           # Forge 1.20.1
│   ├── commands, menu, SimpleChannel networking, GuiGraphics screen
│   └── META-INF/mods.toml
├── neoforge-1.21.1/        # NeoForge 1.21.1
│   ├── commands, menu, payload networking, GuiGraphics screen
│   └── META-INF/neoforge.mods.toml
├── neoforge-26.1.2/        # NeoForge 26.1.2
│   ├── commands, menu, payload networking, GuiGraphicsExtractor screen
│   └── META-INF/neoforge.mods.toml
├── build.gradle            # root conventions and buildAll
└── settings.gradle         # four Gradle subprojects
```

Design notes:

- `common` contains no Minecraft classes; it only handles string IDs, JSON, naming and file paths. Its classes are folded into the platform jars.
- The platform modules only convert menu slots to `SlotStack`, register menus/networking/commands and draw the screen.
- Generation reads the server-side menu slots. Closing the screen or switching tabs returns temporary slot items to the player.
- 26.1.2 uses the 1.21.9+ `GuiGraphicsExtractor` API; 1.20.1 and 1.21.1 use `GuiGraphics`.

## 6. Limitations

- Cooking recipes in 1.20.1 and 1.21.1 always produce one item (vanilla format limitation); 26.1.2 can preserve a count.
- Generated results store only the **item ID + count**; NBT/data components are not written. Recipes that need enchantments, potion contents or other custom components must be edited afterwards.
- Code-driven vanilla special recipes (fireworks, banners, map cloning/extending, repair, dyeing, book cloning, etc.) have no datapack JSON.
- The current GUI reads slot items directly; `ingredientOverrides` (`#tag` ingredients) exists in the shared writer but is not exposed in the GUI yet.
- Smithing trim in 1.20.1/1.21.1 has no `pattern` field; 26.1.2 requires a valid `trim_pattern` ID.
- The generated datapack is not installed automatically; copy it into `datapacks/` and run `/reload`.

## 7. Verification

- `./gradlew build` / `./gradlew buildAll` passes; all 21 common unit tests pass.
- **NeoForge 26.1.2 dedicated server**: mod loads; `/datarecipe`, `/drg` and every subcommand register; eight generated recipes (shaped, shapeless, smelting, stonecutting, smithing transform, smithing trim, transmute, imbue) load, increasing the recipe count from 1515 to 1523; `/reload` works.
- **NeoForge 1.21.1 dedicated server**: mod loads; command tree registers (no transmute/imbue subcommands); six generated recipes load, increasing the recipe count from 1290 to 1296; `/reload` works.
- **Forge 1.20.1 dedicated server**: a clean server installed with the official Forge installer loads the mod and registers the command tree. The six 1.20.1 recipe JSON files were parsed by an unmodified 1.20.1 server with no recipe errors.

## 8. License and community

- Released under the [MIT License](LICENSE), Copyright (c) 2026 lying_ice.
- Read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before contributing.
- Report security issues through [GitHub private vulnerability reporting](https://github.com/lyingice/data_recipe_generator/security/advisories/new), not a public issue.
- See [CHANGELOG.md](CHANGELOG.md) for changes and [SECURITY.md](SECURITY.md) for the support policy.
