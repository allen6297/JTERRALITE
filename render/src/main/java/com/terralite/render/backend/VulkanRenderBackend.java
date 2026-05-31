package com.terralite.render.backend;

import com.terralite.render.RenderBackend;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;
import com.terralite.render.Viewport;
import com.terralite.render.math.CameraMatrices;
import com.terralite.render.mesh.ChunkDebugMeshFactory;
import com.terralite.render.vulkan.VulkanCommands;
import com.terralite.render.vulkan.VulkanContext;
import com.terralite.render.vulkan.VulkanMeshBuffer;
import com.terralite.render.vulkan.VulkanPipeline;
import com.terralite.render.vulkan.VulkanSwapchain;
import com.terralite.render.vulkan.VulkanTextureAtlas;
import com.terralite.render.vulkan.VulkanUtils;
import com.terralite.render.texture.TextureAtlas;
import com.terralite.render.window.RenderWindow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Vulkan-backed {@link RenderBackend}. Renders chunk debug markers using the
 * camera MVP push constant.
 */
public final class VulkanRenderBackend implements RenderBackend {
    private final RenderWindow window;

    private VulkanContext ctx;
    private VulkanSwapchain swapchain;
    private VulkanPipeline pipeline;
    private VulkanCommands commands;
    private TextureAtlas currentAtlas;
    private VulkanTextureAtlas textureAtlas;
    private final Map<RenderChunk, VulkanMeshBuffer> chunkBuffers = new HashMap<>();

    private int currentFrame = 0;
    private long frameIndex = 0;
    private boolean needsSwapchainRecreation = false;

    public VulkanRenderBackend(RenderWindow window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    @Override
    public void initialize() {
        window.create();
        long handle = windowHandle();
        ctx = VulkanContext.create(handle);
        Viewport vp = window.viewport();
        swapchain = VulkanSwapchain.create(ctx, vp.width(), vp.height());
        pipeline = VulkanPipeline.create(ctx, swapchain);
        commands = VulkanCommands.create(ctx);
        currentAtlas = fallbackAtlas();
        textureAtlas = VulkanTextureAtlas.create(ctx, commands, pipeline.descriptorSetLayout, currentAtlas);
    }

    @Override
    public void start() {
        window.show();
    }

    /** Returns true if the underlying window has been asked to close. */
    public boolean shouldClose() {
        return window.shouldClose();
    }

    @Override
    public RenderStats render(RenderFrame frame) {
        Objects.requireNonNull(frame, "frame");
        ensureTextureAtlas(frame.textureAtlas());

        // Poll events first so shouldClose() stays up to date
        window.pollEvents();

        // Skip rendering while minimized (framebuffer is 0×0 on some platforms)
        Viewport currentVp = window.viewport();
        if (currentVp.width() <= 1 && currentVp.height() <= 1) {
            return new RenderStats(frameIndex, currentVp);
        }

        if (needsSwapchainRecreation) {
            recreateSwapchain();
            needsSwapchainRecreation = false;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int frameIdx = currentFrame;
            long inFlightFence = commands.inFlightFences[frameIdx];
            long imageAvailable = commands.imageAvailableSemaphores[frameIdx];
            long renderFinished = commands.renderFinishedSemaphores[frameIdx];
            VkCommandBuffer cmd = commands.commandBuffers[frameIdx];

            // Wait for this frame slot to be free
            vkWaitForFences(ctx.device, inFlightFence, true, Long.MAX_VALUE);

            // Acquire next swapchain image
            IntBuffer imageIndexBuf = stack.mallocInt(1);
            int acquireResult = vkAcquireNextImageKHR(ctx.device, swapchain.swapchain,
                    Long.MAX_VALUE, imageAvailable, VK_NULL_HANDLE, imageIndexBuf);
            if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                needsSwapchainRecreation = true;
                return new RenderStats(++frameIndex, window.viewport());
            }
            int imageIndex = imageIndexBuf.get(0);

            vkResetFences(ctx.device, inFlightFence);
            vkResetCommandBuffer(cmd, 0);

            // Record commands
            recordCommandBuffer(cmd, imageIndex, frame, stack);

            // Submit
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(imageAvailable))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(cmd))
                    .pSignalSemaphores(stack.longs(renderFinished));

            VulkanUtils.check(vkQueueSubmit(ctx.graphicsQueue, submitInfo, inFlightFence),
                    "Failed to submit command buffer");

            // Present
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(renderFinished))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.swapchain))
                    .pImageIndices(imageIndexBuf);

            int presentResult = vkQueuePresentKHR(ctx.presentQueue, presentInfo);
            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR) {
                needsSwapchainRecreation = true;
            }

            currentFrame = (currentFrame + 1) % VulkanCommands.FRAMES_IN_FLIGHT;
            return new RenderStats(++frameIndex, new Viewport(swapchain.width, swapchain.height));
        }
    }

    @Override
    public void stop() {
        vkDeviceWaitIdle(ctx.device);
        destroyChunkBuffers();
        if (textureAtlas != null) {
            textureAtlas.destroy(ctx);
            textureAtlas = null;
        }
        commands.destroy(ctx);
        pipeline.destroy(ctx);
        swapchain.destroy(ctx);
        ctx.destroy();
        window.destroy();
    }

    // ---- private helpers ----

    private void recordCommandBuffer(VkCommandBuffer cmd, int imageIndex, RenderFrame frame, MemoryStack stack) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        VulkanUtils.check(vkBeginCommandBuffer(cmd, beginInfo), "Failed to begin command buffer");

        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        clearValues.get(0).color()
                .float32(0, frame.clearColor().red())
                .float32(1, frame.clearColor().green())
                .float32(2, frame.clearColor().blue())
                .float32(3, frame.clearColor().alpha());
        clearValues.get(1).depthStencil().depth(1.0f).stencil(0);

        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(swapchain.renderPass)
                .framebuffer(swapchain.framebuffers.get(imageIndex))
                .pClearValues(clearValues);
        renderPassInfo.renderArea().offset().set(0, 0);
        renderPassInfo.renderArea().extent().set(swapchain.width, swapchain.height);

        vkCmdBeginRenderPass(cmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);
        vkCmdBindDescriptorSets(
                cmd,
                VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipeline.pipelineLayout,
                0,
                stack.longs(textureAtlas.descriptorSet),
                null
        );

        // Dynamic viewport + scissor
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0).y(0)
                .width(swapchain.width).height(swapchain.height)
                .minDepth(0.0f).maxDepth(1.0f);
        vkCmdSetViewport(cmd, 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.offset().set(0, 0);
        scissor.extent().set(swapchain.width, swapchain.height);
        vkCmdSetScissor(cmd, 0, scissor);

        // Compute MVP
        float[] mvp = CameraMatrices.viewProjection(frame.scene().camera(),
                new Viewport(swapchain.width, swapchain.height));
        java.nio.ByteBuffer mvpBuf = stack.malloc(VulkanPipeline.MVP_PUSH_CONSTANT_SIZE);
        for (float f : mvp) mvpBuf.putFloat(f);
        mvpBuf.flip();
        vkCmdPushConstants(cmd, pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, mvpBuf);

        // Draw real chunk meshes when present; fall back to flat markers for empty/debug scenes.
        if (frame.scene().chunkMeshes().isEmpty()) {
            syncChunkBuffers(frame.scene().chunks());
        } else {
            syncChunkMeshBuffers(frame.scene().chunkMeshes());
        }
        for (VulkanMeshBuffer buf : chunkBuffers.values()) {
            vkCmdBindVertexBuffers(cmd, 0, stack.longs(buf.buffer), stack.longs(0L));
            vkCmdDraw(cmd, buf.vertexCount, 1, 0, 0);
        }

        vkCmdEndRenderPass(cmd);
        VulkanUtils.check(vkEndCommandBuffer(cmd), "Failed to end command buffer");
    }

    private void syncChunkBuffers(List<RenderChunk> chunks) {
        Set<RenderChunk> submitted = new HashSet<>(chunks);

        // Destroy removed chunks
        chunkBuffers.entrySet().removeIf(entry -> {
            if (!submitted.contains(entry.getKey())) {
                entry.getValue().destroy(ctx);
                return true;
            }
            return false;
        });

        // Create new chunks
        for (RenderChunk chunk : chunks) {
            chunkBuffers.computeIfAbsent(chunk, c ->
                    VulkanMeshBuffer.create(ctx, ChunkDebugMeshFactory.create(c)));
        }
    }

    private void syncChunkMeshBuffers(List<RenderChunkMesh> chunkMeshes) {
        Set<RenderChunk> submitted = new HashSet<>();
        for (RenderChunkMesh chunkMesh : chunkMeshes) {
            submitted.add(chunkMesh.chunk());
        }

        chunkBuffers.entrySet().removeIf(entry -> {
            if (!submitted.contains(entry.getKey())) {
                entry.getValue().destroy(ctx);
                return true;
            }
            return false;
        });

        for (RenderChunkMesh chunkMesh : chunkMeshes) {
            chunkBuffers.computeIfAbsent(chunkMesh.chunk(), ignored ->
                    VulkanMeshBuffer.create(ctx, chunkMesh.mesh()));
        }
    }

    private void destroyChunkBuffers() {
        for (VulkanMeshBuffer buf : chunkBuffers.values()) {
            buf.destroy(ctx);
        }
        chunkBuffers.clear();
    }

    private void recreateSwapchain() {
        vkDeviceWaitIdle(ctx.device);
        swapchain.destroy(ctx);
        pipeline.destroy(ctx);
        Viewport vp = window.viewport();
        swapchain = VulkanSwapchain.create(ctx, vp.width(), vp.height());
        pipeline = VulkanPipeline.create(ctx, swapchain);
        if (textureAtlas != null) {
            textureAtlas.destroy(ctx);
        }
        textureAtlas = VulkanTextureAtlas.create(ctx, commands, pipeline.descriptorSetLayout, currentAtlas);
    }

    private void ensureTextureAtlas(TextureAtlas requestedAtlas) {
        TextureAtlas atlas = requestedAtlas == null ? fallbackAtlas() : requestedAtlas;
        if (atlas == currentAtlas) {
            return;
        }
        vkDeviceWaitIdle(ctx.device);
        textureAtlas.destroy(ctx);
        currentAtlas = atlas;
        textureAtlas = VulkanTextureAtlas.create(ctx, commands, pipeline.descriptorSetLayout, currentAtlas);
    }

    private static TextureAtlas fallbackAtlas() {
        return new TextureAtlas(1, 1, new int[] {0xffffffff}, Map.of());
    }

    private long windowHandle() {
        // GlfwWindow exposes the native handle via handle() — accessed reflectively-safe via cast
        if (window instanceof com.terralite.render.glfw.GlfwWindow glfwWindow) {
            return glfwWindow.handle();
        }
        throw new IllegalStateException("VulkanRenderBackend requires a GlfwWindow");
    }
}
