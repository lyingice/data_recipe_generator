# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-15

### Added

- Multi-loader Gradle build: `common`, `forge-1.20.1`, `neoforge-1.21.1` and `neoforge-26.1.2`.
- Command-driven native container GUIs (`/datarecipe`, `/drg`) for every vanilla container with data-driven recipes.
- Crafting table (shaped/shapeless; 26.1.2 transmute/imbue), furnace, blast furnace, smoker, campfire, stonecutter and smithing table (transform/trim).
- Four file-naming modes: custom, auto, auto+suffix and input→output.
- Version-aware recipe JSON and ready-to-use datapack output with `pack.mcmeta`.
- Server-authoritative slot reads, path-traversal protection and configuration validation.
- 21 common unit tests covering recipe JSON, naming modes, validation and datapack layout.

### Changed

- Replaced the MCreator-generated single-module 1.21.1 project with a clean multi-version build.
- The GUI is now opened by command instead of shift-right-clicking a block.
- The interface uses the vanilla container texture and slot coordinates.
- The shared layer is Minecraft-free and is folded into each platform jar.

### Removed

- Legacy MCreator workspace metadata and tracked local `run/` test data.

[Unreleased]: https://github.com/lyingice/data_recipe_generator/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/lyingice/data_recipe_generator/releases/tag/v1.0.0
