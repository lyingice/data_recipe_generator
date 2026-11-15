# Contributing

Thanks for taking the time to improve Data Recipe Generator.

## Prerequisites

- Git
- A JDK for the Gradle launcher. Java 21 is recommended.
- Network access on the first build so the loader toolchains and Minecraft
  artifacts can be downloaded.

The project uses Gradle toolchains, so you do **not** need to install every JDK
manually:

| Module | Java |
|---|---|
| `common` | 17 |
| `forge-1.20.1` | 17 |
| `neoforge-1.21.1` | 21 |
| `neoforge-26.1.2` | 25 (auto-downloaded by foojay if missing) |

## Build and test

```bash
# All modules and the shared unit tests
./gradlew build

# Only the three platform jars
./gradlew buildAll

# Shared tests only
./gradlew :common:test
```

First-time builds can take a while because Forge/NeoForge decompile Minecraft.
Subsequent builds use the Gradle cache.

## Project layout

- `common/` — Minecraft-free shared code: recipe model, JSON generation, naming
  rules, path validation and datapack writing.
- `forge-1.20.1/` — Forge platform code.
- `neoforge-1.21.1/` — NeoForge 1.21.1 platform code.
- `neoforge-26.1.2/` — NeoForge 26.1.2 platform code (new `GuiGraphicsExtractor`
  rendering API).

Keep `common` free of Minecraft imports. If a behavior differs between game
versions, put the switch in `common/.../PlatformEdition.java` and add a test.

## Version rules

Recipe JSON is version-specific. When touching the writer, verify the relevant
serializer codecs:

- 1.20.1: legacy `{"item": ...}` ingredients, string cooking results, string +
  count stonecutting results, no smithing-trim `pattern`.
- 1.21.1: legacy ingredient objects, but `{"id": ...}` results, single-item
  cooking result without count, stonecutting `group`, no smithing-trim `pattern`.
- 26.1.2: holder-set ingredient strings, `{"id": ...}` results, cooking counts,
  no stonecutting `group`, smithing-trim `pattern`.

## Style

- UTF-8, LF line endings (`.gitattributes` enforces this).
- Four spaces for Java/Gradle, two spaces for JSON/YAML.
- Keep comments short and explanatory, matching the surrounding code.
- Do not commit `build/`, `run/`, IDE files, generated datapacks or logs.

## Pull requests

1. Open an issue first for larger changes.
2. Keep the change focused and add or update tests where possible.
3. Run `./gradlew build` before pushing.
4. Fill in the pull request template, including which loader versions you tested.
5. Do not include personal paths, tokens, server addresses or RCON passwords in
   logs or screenshots.

By contributing, you agree that your contribution is licensed under the MIT
License (see [LICENSE](LICENSE)).
