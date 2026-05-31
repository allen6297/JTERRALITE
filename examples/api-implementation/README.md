# API Implementation Examples

This example pack shows the current script API shape for JTERRALITE content.
It is meant as copyable reference for pack authors and for future engine/API
work.

## Files

- `pack.json`: minimal content pack manifest.
- `scripts/startup/main.js`: registers blocks, items, biomes, and tags during
  content loading before registries freeze.
- `scripts/server/main.js`: runs on the authoritative server side and uses the
  tick/world API.

## Startup API

Startup scripts can use:

```js
StartupEvents.registry('block', function(event) {});
StartupEvents.registry('item', function(event) {});
StartupEvents.registry('biome', function(event) {});
StartupEvents.registry('tag', function(event) {});
StartupEvents.registry('creative_category', function(event) {});
Registry.modifyBlock('example:block_id', function(block) {});
```

Block and item builders can assign creative categories and tags inline:

```js
event.create('example:limestone')
  .category('example:natural_blocks')
  .tag('example:natural_blocks');
```

## Server API

Server scripts can use:

```js
api.info('message');
api.onTick(function(tick) {});
api.world().entityCount();
api.world().chunkCount();
api.world().hasChunk(x, y, z);
api.world().loadChunk(x, y, z);
api.world().unloadChunk(x, y, z);
```

The startup API is for content registration and mutation. The server API is for
authoritative simulation-side automation.

Generate editor typings with:

```powershell
.\gradlew.bat :tools:generateTypeScriptApi
```

The checked-in declaration file lives at `types/terralite-scripting.d.ts`.
This example pack includes `jsconfig.json` so editors can pick up those types
while working on `scripts/**/*.js`.
