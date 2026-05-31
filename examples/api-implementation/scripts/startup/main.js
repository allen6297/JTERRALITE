StartupEvents.registry('block', function(event) {
  event.create('example:limestone')
    .displayName('Limestone')
    .solid(true)
    .material('rock')
    .hardness(1.25)
    .resistance(5.0)
    .requiresTool(false)
    .soundType('stone')
    .category('example:natural_blocks')
    .tag('example:natural_blocks');

  event.create('example:limestone_grass')
    .displayName('Limestone Grass')
    .solid(true)
    .material('dirt')
    .hardness(0.8)
    .resistance(2.0)
    .soundType('grass');

  event.create('example:limestone_soil')
    .displayName('Limestone Soil')
    .solid(true)
    .material('dirt')
    .hardness(0.6)
    .resistance(1.5)
    .soundType('gravel');
});

StartupEvents.registry('item', function(event) {
  event.create('example:limestone_shard')
    .displayName('Limestone Shard')
    .stackSize(64)
    .weight(0.2)
    .category('example:natural_items')
    .tag('example:materials');

  event.create('example:limestone_grass_seed')
    .displayName('Limestone Grass Seed')
    .stackSize(64)
    .placesBlock('example:limestone_grass');
});

StartupEvents.registry('creative_category', function(event) {
  event.create('example:natural_items')
    .title('Natural Items')
    .icon('example:limestone_shard')
    .entry('example:limestone_shard')
    .entry('example:limestone_grass_seed');
});

StartupEvents.registry('biome', function(event) {
  event.create('example:limestone_fields')
    .name('Limestone Fields')
    .priority(10)
    .rarity(0.7)
    .temperature(0.35, 0.75)
    .humidity(0.25, 0.65)
    .terrain(48, 12)
    .surfaceTop('example:limestone_grass')
    .surfaceMiddle('example:limestone_soil')
    .surfaceMiddleDepth(3)
    .surfaceBase('example:limestone');
});

StartupEvents.registry('tag', function(event) {
  event.create('example:natural_blocks')
    .description('Natural blocks from the API implementation example')
    .member('example:limestone')
    .member('example:limestone_grass')
    .member('example:limestone_soil');
});

Registry.modifyBlock('example:limestone', function(block) {
  block.displayName = 'Polished Limestone';
  block.hardness = 1.5;
});
