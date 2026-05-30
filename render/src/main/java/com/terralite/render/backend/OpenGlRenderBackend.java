package com.terralite.render.backend;

import com.terralite.render.RenderBackend;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;
import com.terralite.render.Viewport;
import com.terralite.render.math.CameraMatrices;
import com.terralite.render.mesh.ChunkDebugMeshFactory;
import com.terralite.render.opengl.LwjglOpenGlCommands;
import com.terralite.render.opengl.OpenGlCommands;
import com.terralite.render.window.RenderWindow;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OpenGlRenderBackend implements RenderBackend {
    private final RenderWindow window;
    private final OpenGlCommands commands;
    private final Map<RenderChunk, Integer> chunkMarkerMeshes = new LinkedHashMap<>();
    private long frameIndex;

    public OpenGlRenderBackend(RenderWindow window) {
        this(window, new LwjglOpenGlCommands());
    }

    public OpenGlRenderBackend(RenderWindow window, OpenGlCommands commands) {
        this.window = Objects.requireNonNull(window, "window");
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    @Override
    public void initialize() {
        window.create();
        window.makeContextCurrent();
        commands.createCapabilities();
    }

    @Override
    public void start() {
        window.show();
    }

    @Override
    public RenderStats render(RenderFrame frame) {
        Objects.requireNonNull(frame, "frame");
        window.pollEvents();
        Viewport viewport = window.viewport();
        commands.viewport(viewport);
        commands.clear(frame.clearColor());

        float[] mvp = CameraMatrices.viewProjection(frame.scene().camera(), viewport);
        drawChunkMarkers(frame, mvp);

        window.swapBuffers();
        return new RenderStats(++frameIndex, viewport);
    }

    @Override
    public void stop() {
        destroyChunkMarkers();
        window.destroy();
    }

    private void drawChunkMarkers(RenderFrame frame, float[] mvp) {
        Set<RenderChunk> submittedChunks = new HashSet<>(frame.scene().chunks());
        destroyRemovedChunkMarkers(submittedChunks);

        for (RenderChunk chunk : frame.scene().chunks()) {
            int meshHandle = chunkMarkerMeshes.computeIfAbsent(
                    chunk,
                    missingChunk -> commands.createMesh(ChunkDebugMeshFactory.create(missingChunk))
            );
            commands.drawMesh(meshHandle, mvp);
        }
    }

    private void destroyRemovedChunkMarkers(Set<RenderChunk> submittedChunks) {
        List<RenderChunk> removed = chunkMarkerMeshes.keySet().stream()
                .filter(chunk -> !submittedChunks.contains(chunk))
                .toList();

        for (RenderChunk chunk : removed) {
            commands.destroyMesh(chunkMarkerMeshes.remove(chunk));
        }
    }

    private void destroyChunkMarkers() {
        for (int meshHandle : chunkMarkerMeshes.values()) {
            commands.destroyMesh(meshHandle);
        }
        chunkMarkerMeshes.clear();
    }
}
