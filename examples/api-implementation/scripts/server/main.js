api.info('API implementation example server script loaded');

api.onTick(function(tick) {
  if (tick.index === 0) {
    api.world().loadChunk(0, 0, 0);
    api.world().loadChunk(1, 0, 0);
    api.info('Loaded starter chunks');
  }

  if (tick.index % 20 === 0) {
    api.info('tick=' + tick.index
      + ' entities=' + api.world().entityCount()
      + ' chunks=' + api.world().chunkCount());
  }

  if (tick.index === 100 && api.world().hasChunk(1, 0, 0)) {
    api.world().unloadChunk(1, 0, 0);
    api.info('Unloaded example chunk');
  }
});
