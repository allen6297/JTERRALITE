api.info('Terralite base startup script loaded');

StartupEvents.registry('item', function(event) {
  event.create('terralite:tools/stone_probe')
    .displayName('Stone Probe')
    .weight(1.0)
    .stackSize(1)
    .category('terralite:scripted_tools')
    .tag('terralite:scripted_items');
});

StartupEvents.registry('creative_category', function(event) {
  event.create('terralite:scripted_tools')
    .title('Scripted Tools')
    .icon('terralite:tools/stone_probe')
    .entry('terralite:tools/stone_probe');
});

Registry.modifyBlock('terralite:natural/stone', function(block) {
  block.resistance = 6.5;
});
