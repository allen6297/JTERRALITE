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
- `server`: authoritative server lifecycle boundary that owns simulation
  ticking through the engine.
- `render`: renderer lifecycle and backend abstraction; Vulkan/LWJGL
  dependencies are present, but the real backend is still future work.
- `runtime`: application composition utilities, including extraction of engine
  world/camera state into render scene submissions.
- `platform`, `launcher`, `tools`, `api`: support modules that are
  currently lighter-weight or smoke-tested.

The design direction is a platform ecosystem rather than a monolithic game:
engine code provides stable capability, packs define gameplay, scripts automate
through exposed APIs, and JVM extensions add trusted engine capabilities.

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

Validation is split the same way:

- `content.validation.PackDependencyValidator` checks duplicate pack ids and
  missing required pack dependencies.
- `content.loading.PackLoadOrderResolver` orders packs so dependencies load
  before dependents and rejects dependency cycles.
- `game.content.GameContentValidator` checks game registry references after
  content is loaded and frozen, including block/item creative categories,
  creative category icons, and creative category entries.
- Validation returns `ContentValidationResult`; call `requireValid()` when
  startup should fail on validation issues.

Use `game.content.GameContentLoader` as the startup-facing facade when possible.
It discovers or accepts packs, resolves dependency order, creates the core game
registries, applies JSON content, freezes `GameData`, and runs game validation.

Suggested pack layout:

```text
packs/example/
  pack.json
  data/example/blocks/
  data/example/items/
  data/example/recipes/
  data/example/tags/
  data/example/worldgen/
  assets/example/textures/
  assets/example/models/
  assets/example/sounds/
  assets/example/shaders/
  assets/example/lang/
  scripts/startup/
  scripts/server/
  scripts/client/
```

Script scope rules from the design doc:

- `scripts/startup`: runs during content loading before registries freeze.
- `scripts/server`: runs on the authoritative simulation side.
- `scripts/client`: runs locally for presentation only and is never gameplay
  authoritative.

`content.scripting.ScriptContentScanner` currently discovers `.js` files in
those three scope directories. Execution and API exposure are separate future
steps.

Startup script execution now runs through `content.scripting.StartupScriptRunner`
as part of `GameContentLoader`, after JSON content is applied and before
registries freeze. The current script API is intentionally minimal:

```js
api.info("message");
```

This records script diagnostics in `GameContentLoadReport.startupScripts()`.
Future work should expose safe registry builders for startup scripts instead of
letting scripts touch internal registry state directly.

Server script execution runs through `content.scripting.ServerScriptHost`.
`TerraliteServer` loads configured content pack server scripts when the server
starts. The current server script API is intentionally minimal:

```js
api.info("message");
api.onTick(function(tick) {
  api.info("tick " + tick.index);
  api.info("entities " + api.world().entityCount());
  api.world().loadChunk(0, 0, 0);
});
```

This records diagnostics in `TerraliteServer.serverScripts()`. Tick handlers
receive `index`, `deltaMillis`, and `totalMillis`. `api.world()` is a read-only
view for entities and a controlled chunk API with `entityCount()`,
`chunkCount()`, `hasChunk(x, y, z)`, `loadChunk(x, y, z)`, and
`unloadChunk(x, y, z)`.

## Server Runtime

`server` is the first boundary for the server-authoritative model from the
design doc. `TerraliteServer` owns a configured `GameEngine`, runs configured
server scripts on start, invokes server script tick handlers during simulation,
exposes start/advance/stop lifecycle methods, and treats the engine world as
authoritative state.

Current scope:

- no networking yet
- no dedicated/integrated server split yet
- no server script block or entity mutation APIs yet

Use this module for simulation-side orchestration before adding multiplayer or
client presentation concerns.

## Rendering

`render` currently defines the backend-agnostic renderer boundary. `Renderer`
owns lifecycle state and delegates frames to a `RenderBackend`. `RenderFrame`
describes the viewport, clear color, and submitted `RenderScene` for a frame,
while `RenderStats` reports frame output from the backend.

Current scope:

- lifecycle and frame boundary only
- scene submission types for camera, chunks, and placeholder render objects
- no window/swapchain ownership yet
- no Vulkan renderer implementation yet
- no mesh, material, or chunk rendering yet

Use `RecordingRenderBackend` in tests when validating renderer-facing code
without opening a native window or GPU backend.

`runtime.render.RenderSceneExtractor` adapts engine state into render-owned
scene data:

- `Camera` -> `RenderCamera`
- loaded world chunks -> `RenderChunk`
- entities with `PhysicsComponents.TRANSFORM` -> placeholder `RenderObject`

## Testing

Run focused tests while working:

```powershell
.\gradlew.bat :server:test
.\gradlew.bat :render:test
.\gradlew.bat :runtime:test
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
- Safe block-facing server script APIs.
- Runtime render loop composition using `RenderSceneExtractor`.
- Real render backend bootstrap behind the `RenderBackend` interface.
- Validation that category entries reference existing blocks/items.
- Tag support for grouping content beyond creative UI categories.
- Data-driven recipes and localization keys.
- Runtime inventory or creative menu views that consume category registries.
