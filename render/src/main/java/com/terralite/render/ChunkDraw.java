package com.terralite.render;

/**
 * A single chunk draw call ready for the GPU.
 * The buffer and offset come from a suballocated arena or dedicated buffer.
 */
public record ChunkDraw(long buffer, long offset, int vertexCount) {
}