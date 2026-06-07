# JTERRALITE Agent Notes

This file is the root orientation document for future agents working in this
repo. Keep it current as modules, conventions, and content pipelines grow.

## Project Shape

JTERRALITE is a multi-module Gradle Java project.

- `core`: shared foundations such as registries, resource ids, and logging.
- `engine`: runtime game systems, world state, chunks, terrain, physics, input,
  camera, entities, simulation, time, weather, and saving.
- `game`: game-facing domain types such as blocks, items, biomes, tags, creative
  categories, JSON definitions, and Terralite registry keys.
- `content`: early content pipeline space for assets, packs, scripting,
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
- `terralite:biomes`
- `terralite:tags`
- `terralite:worldsgen_spawn_areas`
- `terralite:creative_categories`

## Content Patterns

Blocks, items, biomes, and tags are immutable records with builder APIs.

- Blocks: `game.block.Block`, `BlockProperties`, and `block.json.BlockJsonLoader`.
- Items: `game.item.Item`, `ItemProperties`, and `item.json.ItemJsonLoader`.
- Biomes: `game.biome.Biome`, `BiomeProperties`, and `biome.json.BiomeJsonLoader`.
- Tags: `game.tag.Tag`, and `tag.json.TagJsonLoader`.
- Worldsgen spawn areas: `game.worldsgen.WorldsgenSpawnArea` and
  `worldsgen.json.WorldsgenSpawnAreaJsonLoader`.
- Creative categories: `game.category.CreativeCategory`,
  `CreativeCategories`, and `category.json.CreativeCategoryJsonLoader`.

JSON loaders should:

- Accept an `InputStream`.
- Use Jackson with unknown properties disabled.
- Convert JSON DTO records into domain objects.
- Provide a `register(ResourceId, InputStream, MutableRegistry<T>)` helper.
- Keep defaults in the definition conversion layer, not in tests.

Key properties on domain types:

- `Block`: `displayName`, `hardness`, `resistance`, `solid`, `transparent`,
  `requiresTool`, `material`, `soundType`, `categories`.
- `Item`: `displayName`, `weight`, `stackSize`, `placesBlock`, `categories`.
- `Biome`: `name`, `priority`, `rarity`, temperature/humidity ranges,
  `baseHeight`, `heightVariation`, `surfaceTop`, `surfaceMiddle`,
  `surfaceMiddleDepth`, `surfaceBase`.
- `Tag`: `description`, `members` (list of block or item `ResourceId`s).

Category membership is stored on block and item properties as
`List<ResourceId> categories`. Category definitions can also define ordered
`entries` for UI-like creative tab ordering.

Content packs live as directories with a `pack.json` manifest at the root.
JSON discovery scans:

- `data/<type>/<path>.json`
- `assets/<type>/<path>.json`

For those paths, the pack manifest namespace plus `<path>` becomes the
resource id and `<type>` becomes the content file type. For example, in a pack
whose manifest id namespace is `terralite`,
`data/blocks/natural/stone.json` maps to type `blocks` and id
`terralite:natural/stone`. The pack and namespace are treated as the same
boundary; do not add an extra namespace directory under `data` or `assets`.

Core pack/content classes:

- `content.manifest.PackManifest` and `PackManifestLoader`
- `content.pack.ContentPack` and `ContentPackLoader`
- `content.loading.ContentPackDiscovery`
- `content.json.JsonContentScanner`

Game-specific pack application lives in `game.content.GameContentPackApplier`.
It scans content packs and routes data files by type:

- `blocks` -> `BlockJsonLoader` -> `terralite:blocks`
- `items` -> `ItemJsonLoader` -> `terralite:items`
- `biomes` -> `BiomeJsonLoader` -> `terralite:biomes`
- `tags` -> `TagJsonLoader` -> `terralite:tags`
- `worldsgen` -> `WorldsgenSpawnAreaJsonLoader` ->
  `terralite:worldsgen_spawn_areas`
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
  content is loaded and frozen:
  - block/item category references exist in `creative_categories`
  - creative category icons and entries exist in blocks or items
  - item `placesBlock` references an existing block (`item.places_block.missing`)
  - biome surface block references exist in blocks (`biome.surface.missing`)
  - tag members exist in blocks or items (`tag.member.missing`)
- Validation returns `ContentValidationResult`; call `requireValid()` when
  startup should fail on validation issues.

Use `game.content.GameContentLoader` as the startup-facing facade when possible.
It discovers or accepts packs, resolves dependency order, creates the core game
registries, applies JSON content, runs startup scripts, freezes `GameData`, and
runs game validation.

Suggested pack layout:

```text
packs/example/
  pack.json
  data/blocks/
  data/items/
  data/biomes/
  data/tags/
  data/recipes/
  data/worldsgen/
  assets/textures/
  assets/models/
  assets/sounds/
  assets/shaders/
  assets/lang/
  scripts/startup/
  scripts/server/
  scripts/client/
```

### Block Model Assets

Block models live under `assets/models/` and are referenced from block JSON with
resource ids such as `terralite:block/cube_all`. The current render mesh loader
supports these Terralite JSON model types:

```json
{ "type": "cube_all" }
```

Uses the block's `textures.all` entry for every face. If a block only defines
face-specific textures, the runtime falls back to the same texture choice used
for the top face.

```json
{ "type": "cube_column" }
```

Uses `textures.top` for the up face, `textures.bottom` for the down face, and
`textures.side` for horizontal faces, with the normal block texture fallbacks.

```json
{ "type": "cross", "width": 1.0, "height": 1.0 }
```

Renders two double-sided crossed quads using `textures.all`. `width` and
`height` are optional and default to `1.0`, which is useful for plant growth
states such as wheat.

```json
{
  "type": "elements",
  "elements": [
    {
      "from": [0.0, 0.0, 0.0],
      "to": [2.0, 1.0, 1.0],
      "faces": {
        "north": { "texture": "#all" },
        "south": { "texture": "#all" },
        "east": { "texture": "#all" },
        "west": { "texture": "#all" },
        "up": { "texture": "#all" },
        "down": { "texture": "#all" }
      }
    }
  ]
}
```

Renders box elements with `from` and `to` coordinates in block-local units.
Values can extend outside `0.0..1.0` for multiblock-sized models; mesh bounds
are tracked so neighboring chunks can consider geometry that crosses a chunk
edge.

Blocks can choose state-specific render variants:

```json
"state": {
  "properties": {
    "age": ["0", "1", "2", "3", "4", "5", "6", "7"]
  },
  "default": {
    "age": "0"
  }
},
"states": [
  {
    "when": { "age": "7" },
    "model": "terralite:block/wheat_stage7"
  }
]
```

Every property used by a render variant must be declared in `state.properties`,
and every declared property must have a default value. Pack validation reports
undeclared properties as `block.state.property.unknown`, invalid values as
`block.state.value.invalid`, and missing defaults as
`block.state.default.missing`.

Multiblock-sized blocks can declare occupied cells in block JSON:

```json
"occupancy": [
  [0, 0, 0],
  [1, 0, 0]
]
```

For state-driven orientation, use the object form:

```json
"state": {
  "properties": {
    "facing": ["north", "east", "south", "west"]
  },
  "default": {
    "facing": "north"
  }
},
"occupancy": {
  "rotates_with": "facing",
  "offsets": [
    [0, 0, 0],
    [1, 0, 0]
  ]
}
```

The origin offset `[0, 0, 0]` is required. Runtime worlds created from
`GameData` use multiblock-aware block storage: placement fails if any occupied
cell is already claimed, child cells resolve back to the origin block state,
removing any occupied cell removes the whole multiblock, and render iteration
only visits origin cells. `rotates_with` currently rotates occupancy around the
origin from the north-facing offset layout; model mesh rotation is a separate
rendering step.

World snapshots capture chunks, entity ids, and origin blocks. Block snapshots
include the block id, state properties, and an optional compact state id when
the backing storage exposes one. `WorldSnapshotJsonCodec` can read and write
these snapshots as JSON files. Restoring into multiblock-aware storage replays
only origin blocks, so occupied child cells are rebuilt from block occupancy
metadata instead of being saved redundantly.

Unknown or missing Terralite JSON model types fail during model mesh loading
with a message that includes the model id and file path.

Pack startup also validates block model and texture references against the
loaded asset index when a block defines textures. A block that references a
missing model emits `block.model.missing`; missing texture assets emit
`block.texture.missing`.

## Render Smoke Checks

Two manual render checks are available from the repo root:

```bash
./gradlew :tools:runVulkanSmoke
```

This opens a GLFW/Vulkan window with a debug 3x3 chunk marker grid. A good
result is a visible window with colored markers and no Vulkan setup exception.

```bash
./gradlew :tools:runContentRenderSmoke
```

This loads `packs/terralite`, builds runtime chunks, loads texture/model assets,
and renders the real content through the Vulkan backend for about five seconds.
A good result shows a small terrain surface using the base pack textures and
models, including column-style grass blocks and cross-style wheat when visible.

The repo now includes a real base content pack at `packs/terralite`.
It is discoverable through `GameContentLoader.load(Path.of("packs"))` and
currently provides JSON-backed blocks, items, creative categories, a biome,
and tags under `data/...`. It also includes a startup script that
registers an item through `StartupEvents` and modifies a JSON-loaded block
through `Registry`.

Script scope rules from the design doc:

- `scripts/startup`: runs during content loading before registries freeze.
- `scripts/server`: runs on the authoritative simulation side.
- `scripts/client`: runs locally for presentation only and is never gameplay
  authoritative.

## Startup Script API

Startup scripts run through `content.scripting.StartupScriptRunner` as part of
`GameContentLoader`, after JSON content is applied and before registries freeze.
Two globals are exposed: `StartupEvents` and `Registry`.

### StartupEvents.registry

The primary way to register content in a startup script. Supports types
`block`, `item`, `biome`, `tag`, and `creative_category`.

```js
StartupEvents.registry('block', function(event) {
  event.create('base:granite')
    .displayName('Granite')
    .solid(true)
    .material('rock')
    .hardness(1.5)
    .resistance(6.0)
    .category('base:building_blocks')
    .tag('base:natural_blocks');
});

StartupEvents.registry('item', function(event) {
  event.create('base:wheat_seeds')
    .displayName('Wheat Seeds')
    .stackSize(99)
    .placesBlock('base:wheat')
    .category('base:natural_items')
    .tag('base:seeds');
});

StartupEvents.registry('biome', function(event) {
  event.create('base:temperate_forest')
    .name('Temperate Forest')
    .priority(10)
    .rarity(1.0)
    .temperature(0.30, 0.70)
    .humidity(0.40, 0.80)
    .terrain(48, 14)
    .surfaceTop('base:grass')
    .surfaceMiddle('base:dirt')
    .surfaceMiddleDepth(3)
    .surfaceBase('base:stone');
});

StartupEvents.registry('tag', function(event) {
  event.create('base:crops')
    .description('Crop blocks')
    .member('base:wheat')
    .member('base:carrot');
});

StartupEvents.registry('creative_category', function(event) {
  event.create('base:natural_items')
    .title('Natural Items')
    .icon('base:wheat_seeds')
    .entry('base:wheat_seeds');
});
```

### Registry

Read and patch existing entries from JSON or earlier scripts.

```js
var stone = Registry.getBlock('base:stone');
var seeds = Registry.getItem('base:wheat_seeds');
var plains = Registry.getBiome('base:plains');

Registry.modifyBlock('base:copper_ore', function(block) {
  block.displayName = 'Dense Copper Ore';
  block.hardness = 4.0;
});
```

Script diagnostics are recorded in `GameContentLoadReport.startupScripts()`.

See `examples/api-implementation` for copyable startup script examples that
register blocks, items, biomes, tags, and modify a block before registries
freeze.

`GameContentLoader.load(Path)` and `GameContentLoader.load(List<ContentPack>)`
wire these game startup globals by default. Use the overload that accepts a
`ScriptGlobalsFactory` only when a test or tool needs custom globals.

Generate TypeScript editor declarations for pack scripts with:

```powershell
.\gradlew.bat :tools:generateTypeScriptApi
```

The generated file is written to
`types/terralite-scripting.d.ts`.
The `:tools:checkTypeScriptApi` task verifies the checked-in declaration file
matches the generator output and is wired into `:tools:check`.

`packs/terralite/jsconfig.json` points at that declaration file so editors
can provide autocomplete and inline checking for pack scripts.

The startup script API implementation lives in `game.scripting`:
- `StartupEventsScriptApi` — `registry(type, fn)`
- `BlockScriptBuilder`, `ItemScriptBuilder`, `BiomeScriptBuilder`,
  `TagScriptBuilder`, `CreativeCategoryScriptBuilder` — chainable builders
- `RegistryScriptApi` — `getBlock`, `getItem`, `getBiome`, `modifyBlock`
- `GameStartupScriptGlobals` — wires the above into `StartupScriptRunner`

The `api` module exposes `api.scripting.GameStartupScriptGlobals` as a thin
public facade that delegates to the game implementation. Keep game-specific
script behavior in `game.scripting` so there is only one implementation path.

The `content` module exposes `StartupScriptGlobal` as a bridge so `game`-specific
globals can be injected into the script runtime without creating a dependency from
`content` to `game`.

## Server Script API

Server script execution runs through `content.scripting.ServerScriptHost`.
`TerraliteServer` loads configured content pack server scripts when the server
starts. The current server script API:

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

See `examples/api-implementation/scripts/server/main.js` for a server script
that loads chunks, logs world counts, and unloads a chunk later.

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

`render` defines the backend-agnostic renderer boundary. `Renderer` owns
lifecycle state and delegates frames to a `RenderBackend`. `RenderFrame`
carries the viewport, clear color, and a `RenderScene` (camera + chunks +
objects). `RenderStats` reports the frame index and actual rendered viewport.

### Vulkan backend

`VulkanRenderBackend` is the primary backend. It requires a `GlfwWindow`
created via `WindowConfig.vulkan(...)` (GLFW with `GLFW_NO_API`).

Current scope:

- full render loop: acquire → record → submit → present (2 frames in flight)
- color attachment + depth buffer (`VK_FORMAT_D32_SFLOAT` or best available)
- GLSL vertex/fragment shaders compiled to SPIRV at runtime via shaderc
- `mat4 mvp` as a Vulkan push constant — computed from `RenderCamera` each frame
- chunk debug markers: flat XY squares at world-space positions
  (`ChunkDebugMeshFactory`), one `VulkanMeshBuffer` per chunk
- swapchain recreation on `VK_ERROR_OUT_OF_DATE_KHR` / `VK_SUBOPTIMAL_KHR`
  (window resize) and on minimize (frames skipped while framebuffer ≤ 1×1)
- `VK_LAYER_KHRONOS_validation` enabled when present; skipped gracefully
  when the Vulkan SDK is not installed

Camera orientation:

`RenderCamera` carries `yaw` (horizontal rotation, degrees) and `pitch`
(vertical tilt, ±89°, degrees) in addition to position and projection params.
`CameraMatrices.viewProjection()` derives the forward vector from yaw + pitch:

```
forwardX = -sin(yaw) * cos(pitch)
forwardY =  sin(pitch)
forwardZ = -cos(yaw) * cos(pitch)
```

The 6-arg `RenderCamera` constructor defaults both to 0 (looking along -Z).

Use `RecordingRenderBackend` in tests when validating renderer-facing code
without opening a native window or GPU backend.

Vulkan smoke test (5 s by default; pass duration in seconds as argument):

```powershell
.\gradlew.bat :tools:runVulkanSmoke
.\gradlew.bat :tools:runVulkanSmoke --args="10"
```

The smoke test opens a 1280×720 window, submits a 3×3 chunk grid, and slowly
rotates camera yaw so the MVP transform is visible.

Content/render smoke test:

```powershell
.\gradlew.bat :tools:runContentRenderSmoke
.\gradlew.bat :tools:runContentRenderSmoke --args="packs 10"
```

This loads `packs/terralite`, creates a small runtime `World` with loaded
engine chunks from `data/worldsgen/spawn_area.json`, extracts a render scene
through `RenderPipeline`, and submits it to the Vulkan debug backend.

### Runtime render integration

`runtime.render.RenderSceneExtractor` adapts engine state into render-owned
scene data:

- `Camera` (position, yaw, pitch, FOV, near/far) -> `RenderCamera`
- loaded world chunks -> `RenderChunk`
- entities with `PhysicsComponents.TRANSFORM` -> placeholder `RenderObject`

`runtime.render.RenderPipeline` composes the extractor with a `Renderer` so
runtime code can render one frame from the current `World`, `Camera`,
`Viewport`, and `ClearColor`.

`runtime.world.RuntimeWorldFactory` creates the initial engine `World` from
loaded `GameData`. It currently reads `terralite:spawn_area` from the
`terralite:worldsgen_spawn_areas` registry and loads the configured starting
chunks, falling back to the default 3x3 spawn area when the entry is missing.

### Engine camera

`engine.camera.Camera` now carries `yaw` and `pitch` alongside its `Transform`.
Use `setOrientation(yaw, pitch)`, `setYaw(yaw)`, or `setPitch(pitch)` to
control the look direction. `RenderSceneExtractor` passes these through to
`RenderCamera` automatically.

## Testing

Run focused tests while working:

```powershell
.\gradlew.bat :server:test
.\gradlew.bat :render:test
.\gradlew.bat :runtime:test
.\gradlew.bat :api:test
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

- 3D chunk cubes replacing the flat debug squares (requires depth buffer ✓).
- Data-driven recipes referencing blocks, items, and tags.
- Localization keys for block/item/biome display names.
- Safe block-facing and entity-facing server script APIs.
- Runtime inventory or creative menu views that consume category registries.
- Registry remapping and save migration system.
