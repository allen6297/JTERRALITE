package com.terralite.render.backend;

import com.terralite.render.ChunkDraw;
import com.terralite.render.ChunkRenderCache;
import com.terralite.render.ChunkRenderEntry;
import com.terralite.render.DeferredReleaseQueue;
import com.terralite.render.MeshSignature;
import com.terralite.render.PreparedRenderFrame;
import com.terralite.render.RenderBackend;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;
import com.terralite.render.Viewport;
import com.terralite.render.math.CameraMatrices;
import com.terralite.render.math.FrustumCuller;
import com.terralite.render.mesh.ChunkDebugMeshFactory;
import com.terralite.render.texture.TextureAtlas;
import com.terralite.render.vulkan.VulkanCommands;
import com.terralite.render.vulkan.VulkanContext;
import com.terralite.render.vulkan.VulkanPipeline;
import com.terralite.render.vulkan.VulkanSwapchain;
import com.terralite.render.vulkan.VulkanTextureAtlas;
import com.terralite.render.vulkan.VulkanUtils;
import com.terralite.render.window.RenderWindow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.vkCmdSetScissor;
import static org.lwjgl.vulkan.VK10.vkCmdSetViewport;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkResetCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;

/**
 * Vulkan-backed {@link RenderBackend} with phased rendering:
 * <ol>
 *   <li>{@code prepareFrame}: camera, frustum, visibility</li>
 *   <li>{@code syncResources}: create/upload/retire GPU resources</li>
 *   <li>{@code recordAndSubmit}: pure GPU command recording and frame submission</li>
 * </ol>
 */
public final class VulkanRenderBackend implements RenderBackend {
    private static final int CHUNK_SIZE = 16;
    private static final int EVICTION_THRESHOLD = 120;

    private final RenderWindow window;

    private VulkanContext ctx;
    private VulkanSwapchain swapchain;
    private VulkanPipeline pipeline;
    private VulkanCommands commands;
    private TextureAtlas currentAtlas;
    private VulkanTextureAtlas textureAtlas;
    private ChunkRenderCache chunkCache;
    private DeferredReleaseQueue deferredRelease;

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
        chunkCache = new ChunkRenderCache(EVICTION_THRESHOLD);
        deferredRelease = new DeferredReleaseQueue();
    }

    @Override
    public void start() {
        window.show();
    }

    public boolean shouldClose() {
        return window.shouldClose();
    }

    @Override
    public RenderStats render(RenderFrame frame) {
        Objects.requireNonNull(frame, "frame");
        ensureTextureAtlas(frame.textureAtlas());

        window.pollEvents();

        Viewport currentVp = window.viewport();
        if (currentVp.width() <= 1 && currentVp.height() <= 1) {
            return new RenderStats(frameIndex, currentVp);
        }

        if (needsSwapchainRecreation) {
            recreateSwapchain();
            needsSwapchainRecreation = false;
        }

        PreparedRenderFrame prepared = prepareFrame(frame);
        syncResources(frame);
        prepared = prepareFrame(frame);

        return recordAndSubmit(prepared);
    }

    @Override
    public void stop() {
        vkDeviceWaitIdle(ctx.device);
        chunkCache.destroyAll(ctx);
        deferredRelease.destroyAll(ctx);
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

    private PreparedRenderFrame prepareFrame(RenderFrame frame) {
        Viewport vp = new Viewport(swapchain.width, swapchain.height);
        float[] mvp = CameraMatrices.viewProjection(frame.scene().camera(), vp);

        FrustumCuller culler = new FrustumCuller();
        culler.update(mvp);

        List<ChunkDraw> visibleDraws = new ArrayList<>();
        chunkCache.beginFrame(frameIndex);

        List<RenderChunkMesh> chunkMeshes = frame.scene().chunkMeshes();
        if (chunkMeshes.isEmpty()) {
            for (RenderChunk chunk : frame.scene().chunks()) {
                ChunkRenderEntry entry = chunkCache.get(chunk);
                if (entry == null) {
                    continue;
                }
                if (isChunkVisible(culler, chunk)) {
                    entry.markSeen(frameIndex);
                    entry.markVisible();
                    visibleDraws.add(new ChunkDraw(entry.buffer().buffer, 0L, entry.vertexCount()));
                }
            }
        } else {
            for (RenderChunkMesh chunkMesh : chunkMeshes) {
                RenderChunk chunk = chunkMesh.chunk();
                ChunkRenderEntry entry = chunkCache.get(chunk);
                MeshSignature sig = MeshSignature.from(chunkMesh);

                if (entry == null || !entry.signature().equals(sig)) {
                    continue;
                }

                if (isChunkVisible(culler, chunk)) {
                    entry.markSeen(frameIndex);
                    entry.markVisible();
                    visibleDraws.add(new ChunkDraw(entry.buffer().buffer, 0L, entry.vertexCount()));
                }
            }
        }

        return new PreparedRenderFrame(vp, mvp, culler, visibleDraws, textureAtlas, frame.clearColor());
    }

    private boolean isChunkVisible(FrustumCuller culler, RenderChunk chunk) {
        float minX = chunk.x() * CHUNK_SIZE;
        float minY = chunk.y() * CHUNK_SIZE;
        float minZ = chunk.z() * CHUNK_SIZE;
        float maxX = minX + CHUNK_SIZE;
        float maxY = minY + CHUNK_SIZE;
        float maxZ = minZ + CHUNK_SIZE;
        return culler.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void syncResources(RenderFrame frame) {
        List<RenderChunkMesh> chunkMeshes = frame.scene().chunkMeshes();

        if (chunkMeshes.isEmpty()) {
            for (RenderChunk chunk : frame.scene().chunks()) {
                ChunkRenderEntry entry = chunkCache.get(chunk);
                if (entry == null) {
                    chunkCache.create(ctx, chunk, MeshSignature.untracked(), ChunkDebugMeshFactory.create(chunk));
                }
            }
        } else {
            for (RenderChunkMesh chunkMesh : chunkMeshes) {
                RenderChunk chunk = chunkMesh.chunk();
                ChunkRenderEntry entry = chunkCache.get(chunk);
                MeshSignature sig = MeshSignature.from(chunkMesh);

                if (entry == null || !entry.signature().equals(sig)) {
                    if (entry != null) {
                        deferredRelease.enqueue(entry, frameIndex);
                    }
                    chunkCache.create(ctx, chunk, sig, chunkMesh.mesh());
                }
            }
        }

        long completedFrame = Math.max(0, frameIndex - VulkanCommands.FRAMES_IN_FLIGHT);
        deferredRelease.flush(ctx, completedFrame);
        chunkCache.evictStale(ctx, frameIndex);
    }

    private RenderStats recordAndSubmit(PreparedRenderFrame prepared) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int frameIdx = currentFrame;
            long inFlightFence = commands.inFlightFences[frameIdx];
            long imageAvailable = commands.imageAvailableSemaphores[frameIdx];
            long renderFinished = commands.renderFinishedSemaphores[frameIdx];
            VkCommandBuffer cmd = commands.commandBuffers[frameIdx];

            vkWaitForFences(ctx.device, inFlightFence, true, Long.MAX_VALUE);

            IntBuffer imageIndexBuf = stack.mallocInt(1);
            int acquireResult = vkAcquireNextImageKHR(
                    ctx.device,
                    swapchain.swapchain,
                    Long.MAX_VALUE,
                    imageAvailable,
                    VK_NULL_HANDLE,
                    imageIndexBuf
            );

            if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR) {
                needsSwapchainRecreation = true;
                return new RenderStats(++frameIndex, window.viewport());
            }
            if (acquireResult != VK_SUCCESS && acquireResult != VK_SUBOPTIMAL_KHR) {
                throw new IllegalStateException("Failed to acquire swapchain image: " + acquireResult);
            }

            int imageIndex = imageIndexBuf.get(0);

            vkResetFences(ctx.device, inFlightFence);
            vkResetCommandBuffer(cmd, 0);

            recordCommandBuffer(cmd, imageIndex, prepared, stack);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(imageAvailable))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(cmd))
                    .pSignalSemaphores(stack.longs(renderFinished));

            VulkanUtils.check(vkQueueSubmit(ctx.graphicsQueue, submitInfo, inFlightFence), "Failed to submit command buffer");

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
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

    private void recordCommandBuffer(
            VkCommandBuffer cmd,
            int imageIndex,
            PreparedRenderFrame prepared,
            MemoryStack stack
    ) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        VulkanUtils.check(org.lwjgl.vulkan.VK10.vkBeginCommandBuffer(cmd, beginInfo), "Failed to begin command buffer");

        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        clearValues.get(0).color()
                .float32(0, prepared.clearColor().red())
                .float32(1, prepared.clearColor().green())
                .float32(2, prepared.clearColor().blue())
                .float32(3, prepared.clearColor().alpha());
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
                stack.longs(prepared.textureAtlas().descriptorSet),
                null
        );

        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0)
                .y(0)
                .width(swapchain.width)
                .height(swapchain.height)
                .minDepth(0.0f)
                .maxDepth(1.0f);
        vkCmdSetViewport(cmd, 0, viewport);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.offset().set(0, 0);
        scissor.extent().set(swapchain.width, swapchain.height);
        vkCmdSetScissor(cmd, 0, scissor);

        ByteBuffer mvpBuf = stack.malloc(VulkanPipeline.MVP_PUSH_CONSTANT_SIZE);
        for (float f : prepared.mvp()) {
            mvpBuf.putFloat(f);
        }
        mvpBuf.flip();
        vkCmdPushConstants(cmd, pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, mvpBuf);

        for (ChunkDraw draw : prepared.visibleChunkDraws()) {
            vkCmdBindVertexBuffers(cmd, 0, stack.longs(draw.buffer()), stack.longs(draw.offset()));
            vkCmdDraw(cmd, draw.vertexCount(), 1, 0, 0);
        }

        vkCmdEndRenderPass(cmd);
        VulkanUtils.check(vkEndCommandBuffer(cmd), "Failed to end command buffer");
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
        return new TextureAtlas(1, 1, new int[]{0xffffffff}, Map.of());
    }

    private long windowHandle() {
        if (window instanceof com.terralite.render.glfw.GlfwWindow glfwWindow) {
            return glfwWindow.handle();
        }
        throw new IllegalStateException("VulkanRenderBackend requires a GlfwWindow");
    }
}