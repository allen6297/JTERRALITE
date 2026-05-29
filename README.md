# JTERRALITE Agent Notes

This file is the root orientation document for future agents working in this
repo. Keep it current as modules, conventions, and content pipelines grow.

## Project Shape

JTERRALITE is a multi-module Gradle Java project.

- `core`: shared foundations such as registries, resource ids, and logging.
- `engine`: runtime game systems, world state, chunks, terrain, physics, input,
  camera, entities, simulation, time, weather, and saving.
- `game`: game-facing domain types such as blocks, items, creative categories,
  JSON definitions, and Terralite registry keys.
- `content`: early content pipeline space for assets, packs, tags, recipes,
  localization, dependency handling, and validation.
- `render`, `platform`, `launcher`, `runtime`, `tools`, `api`: support modules
  that are currently lighter-weight or smoke-tested.

## Registry Model

The project uses `core` registries instead of global mutable singletons.

- Registry ids use `ResourceId`, normally in `namespace:path` form.
- Registry keys live in `game/src/main/java/com/terralite/game/registry/TerraliteRegistries.java`.
- Mutable registries are created through `RegistryManager`, filled during setup,
  then frozen into `GameData`.
- Prefer registering typed domain objects rather than passing raw strings through
  gameplay systems.

Current game registries:

- `terralite:blocks`
- `terralite:items`
- `terralite:creative_categories`

## Content Patterns

Blocks and items are immutable records with builder APIs.

- Blocks: `game.block.Block`, `BlockProperties`, and `block.json.BlockJsonLoader`.
- Items: `game.item.Item`, `ItemProperties`, and `item.json.ItemJsonLoader`.
- Creative categories: `game.category.CreativeCategory`,
  `CreativeCategories`, and `category.json.CreativeCategoryJsonLoader`.

JSON loaders should:

- Accept an `InputStream`.
- Use Jackson with unknown properties disabled.
- Convert JSON DTO records into domain objects.
- Provide a `register(ResourceId, InputStream, MutableRegistry<T>)` helper.
- Keep defaults in the definition conversion layer, not in tests.

Category membership is stored on block and item properties as
`List<ResourceId> categories`. Category definitions can also define ordered
`entries` for UI-like creative tab ordering.

Content packs live as directories with a `pack.json` manifest at the root.
Minecraft-like JSON discovery currently scans:

- `data/<namespace>/<type>/<path>.json`
- `assets/<namespace>/<type>/<path>.json`

For those paths, `<namespace>:<path>` becomes the resource id and `<type>`
becomes the content file type. For example,
`data/terralite/blocks/natural/stone.json` maps to type `blocks` and id
`terralite:natural/stone`.

Core pack/content classes:

- `content.manifest.PackManifest` and `PackManifestLoader`
- `content.pack.ContentPack` and `ContentPackLoader`
- `content.loading.ContentPackDiscovery`
- `content.json.JsonContentScanner`

Game-specific pack application lives in `game.content.GameContentPackApplier`.
It scans content packs and routes data files by type:

- `blocks` -> `BlockJsonLoader` -> `terralite:blocks`
- `items` -> `ItemJsonLoader` -> `terralite:items`
- `creative_categories` -> `CreativeCategoryJsonLoader` ->
  `terralite:creative_categories`

The generic `content` module should stay independent from `game`; routing that
knows about concrete game registries belongs in the `game` module or a higher
startup/runtime layer.

## Testing

Run focused tests while working:

```powershell
.\gradlew.bat :game:test
.\gradlew.bat :content:test
```

Run the full suite before finishing broad changes:

```powershell
.\gradlew.bat test
```

Gradle may print native-access warnings from its wrapper dependencies on newer
JDKs. Those warnings are not test failures.

## Editing Guidelines

- Follow existing module boundaries. Put shared primitives in `core`, runtime
  simulation in `engine`, and game content/domain definitions in `game`.
- Prefer small immutable records plus builders for user-facing game objects.
- Keep JSON DTOs separate from runtime/domain records.
- Add tests near the module that owns the behavior.
- Do not rewrite unrelated files or undo uncommitted user work.
- Use `ResourceId`/`ResourceKey` instead of ad hoc string ids once data crosses
  a registry or gameplay boundary.

## Current Extension Points

Likely next additions:

- Content-pack discovery in `content`.
- Validation that category entries reference existing blocks/items.
- Tag support for grouping content beyond creative UI categories.
- Data-driven recipes and localization keys.
- Runtime inventory or creative menu views that consume category registries.
