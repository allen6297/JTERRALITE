package com.terralite.render.backend;

import com.terralite.render.RenderBackend;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;

import java.util.ArrayList;
import java.util.List;

public final class RecordingRenderBackend implements RenderBackend {
    private final List<String> lifecycleEvents = new ArrayList<>();
    private final List<RenderFrame> frames = new ArrayList<>();

    @Override
    public void initialize() {
        lifecycleEvents.add("initialize");
    }

    @Override
    public void start() {
        lifecycleEvents.add("start");
    }

    @Override
    public RenderStats render(RenderFrame frame) {
        frames.add(frame);
        return new RenderStats(frames.size(), frame.viewport());
    }

    @Override
    public void stop() {
        lifecycleEvents.add("stop");
    }

    public List<String> lifecycleEvents() {
        return List.copyOf(lifecycleEvents);
    }

    public List<RenderFrame> frames() {
        return List.copyOf(frames);
    }
}
