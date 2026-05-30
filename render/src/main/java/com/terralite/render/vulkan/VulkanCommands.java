package com.terralite.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Manages the command pool and per-frame command buffers, semaphores, and fences.
 */
public final class VulkanCommands {
    public static final int FRAMES_IN_FLIGHT = 2;

    public final long commandPool;
    public final VkCommandBuffer[] commandBuffers;
    public final long[] imageAvailableSemaphores;
    public final long[] renderFinishedSemaphores;
    public final long[] inFlightFences;

    private VulkanCommands(long commandPool, VkCommandBuffer[] commandBuffers,
                            long[] imageAvailableSemaphores, long[] renderFinishedSemaphores,
                            long[] inFlightFences) {
        this.commandPool = commandPool;
        this.commandBuffers = commandBuffers;
        this.imageAvailableSemaphores = imageAvailableSemaphores;
        this.renderFinishedSemaphores = renderFinishedSemaphores;
        this.inFlightFences = inFlightFences;
    }

    public static VulkanCommands create(VulkanContext ctx) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long commandPool = createCommandPool(ctx, stack);
            VkCommandBuffer[] commandBuffers = allocateCommandBuffers(ctx, commandPool, stack);
            long[] imageAvailable = new long[FRAMES_IN_FLIGHT];
            long[] renderFinished = new long[FRAMES_IN_FLIGHT];
            long[] fences = new long[FRAMES_IN_FLIGHT];

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer handlePtr = stack.mallocLong(1);
            for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
                VulkanUtils.check(vkCreateSemaphore(ctx.device, semaphoreInfo, null, handlePtr),
                        "Failed to create imageAvailable semaphore");
                imageAvailable[i] = handlePtr.get(0);

                VulkanUtils.check(vkCreateSemaphore(ctx.device, semaphoreInfo, null, handlePtr),
                        "Failed to create renderFinished semaphore");
                renderFinished[i] = handlePtr.get(0);

                VulkanUtils.check(vkCreateFence(ctx.device, fenceInfo, null, handlePtr),
                        "Failed to create in-flight fence");
                fences[i] = handlePtr.get(0);
            }

            return new VulkanCommands(commandPool, commandBuffers, imageAvailable, renderFinished, fences);
        }
    }

    public void destroy(VulkanContext ctx) {
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(ctx.device, imageAvailableSemaphores[i], null);
            vkDestroySemaphore(ctx.device, renderFinishedSemaphores[i], null);
            vkDestroyFence(ctx.device, inFlightFences[i], null);
        }
        vkDestroyCommandPool(ctx.device, commandPool, null);
    }

    private static long createCommandPool(VulkanContext ctx, MemoryStack stack) {
        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(ctx.graphicsFamily);

        LongBuffer poolPtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreateCommandPool(ctx.device, poolInfo, null, poolPtr), "Failed to create command pool");
        return poolPtr.get(0);
    }

    private static VkCommandBuffer[] allocateCommandBuffers(VulkanContext ctx, long pool, MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(pool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(FRAMES_IN_FLIGHT);

        org.lwjgl.PointerBuffer buffers = stack.mallocPointer(FRAMES_IN_FLIGHT);
        VulkanUtils.check(vkAllocateCommandBuffers(ctx.device, allocInfo, buffers),
                "Failed to allocate command buffers");

        VkCommandBuffer[] result = new VkCommandBuffer[FRAMES_IN_FLIGHT];
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            result[i] = new VkCommandBuffer(buffers.get(i), ctx.device);
        }
        return result;
    }
}
