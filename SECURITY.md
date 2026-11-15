# Security Policy

## Supported Versions

| Version | Minecraft | Supported |
|---|---|---|
| 1.0.x | Forge 1.20.1 / NeoForge 1.21.1 / NeoForge 26.1.2 | Yes |

## Reporting a Vulnerability

Please **do not open a public issue** for security problems.

Use GitHub's private vulnerability reporting:

<https://github.com/lyingice/data_recipe_generator/security/advisories/new>

Include:

- the loader and Minecraft version;
- the mod version;
- a short description of the impact;
- reproduction steps or a proof of concept;
- relevant logs with personal paths and secrets removed.

You can expect an initial response within a few days. Once a fix is available,
the advisory will be published with credit unless you prefer to stay anonymous.

## Scope

Data Recipe Generator writes files on the server. The following protections are
part of the security model:

- output paths are restricted to `data_recipe_generator_output` and traversal
  segments (`..`, absolute paths, drive prefixes) are rejected;
- recipe generation reads the server-side menu slots, so a client cannot forge
  the item data used to build a recipe;
- commands require gamemaster permission (or the single-player owner);
- generated files are plain JSON and are never executed.
