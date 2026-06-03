package com.terralite.render.vulkan;

import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * A device-local Vulkan vertex buffer backed by a single {@link DebugMesh}.
 * Upload uses a temporary host-visible staging buffer.
 */
public final class VulkanMeshBuffer {
    public final long buffer;
    public final long memory;
    public final int vertexCount;

    private VulkanMeshBuffer(long buffer, long memory, int vertexCount) {
        this.buffer = buffer;
        this.memory = memory;
        this.vertexCount = vertexCount;
    }

    public static VulkanMeshBuffer create(VulkanContext ctx, DebugMesh mesh) {
        float[] data = toFloatArray(mesh);
        long size = (long) data.length * Float.BYTES;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            BufferAllocation staging = createBuffer(
                    ctx,
                    size,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    stack
            );

            PointerBuffer dataPtr = stack.mallocPointer(1);
            vkMapMemory(ctx.device, staging.memory, 0, size, 0, dataPtr);
            FloatBuffer mapped = MemoryUtil.memFloatBuffer(dataPtr.get(0), data.length);
            mapped.put(data).flip();
            vkUnmapMemory(ctx.device, staging.memory);

            BufferAllocation vertex = createBuffer(
                    ctx,
                    size,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    stack
            );

            copyBuffer(ctx, staging.buffer, vertex.buffer, size);

            vkDestroyBuffer(ctx.device, staging.buffer, null);
            vkFreeMemory(ctx.device, staging.memory, null);

            return new VulkanMeshBuffer(vertex.buffer, vertex.memory, mesh.vertices().size());
        }
    }

    public void destroy(VulkanContext ctx) {
        vkDestroyBuffer(ctx.device, buffer, null);
        vkFreeMemory(ctx.device, memory, null);
    }

    private static BufferAllocation createBuffer(
            VulkanContext ctx,
            long size,
            int usage,
            int properties,
            MemoryStack stack
    ) {
        VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer bufPtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreateBuffer(ctx.device, bufInfo, null, bufPtr), "Failed to create buffer");
        long buffer = bufPtr.get(0);

        VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
        vkGetBufferMemoryRequirements(ctx.device, buffer, memReq);

        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(ctx.physicalDevice, memProps);
        int memTypeIndex = VulkanUtils.findMemoryType(memProps, memReq.memoryTypeBits(), properties);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(memTypeIndex);

        LongBuffer memPtr = stack.mallocLong(1);
        VulkanUtils.check(vkAllocateMemory(ctx.device, allocInfo, null, memPtr), "Failed to allocate buffer memory");
        long memory = memPtr.get(0);

        vkBindBufferMemory(ctx.device, buffer, memory, 0);
        return new BufferAllocation(buffer, memory);
    }

    private static void copyBuffer(VulkanContext ctx, long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(ctx.transferCommandPool())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer pCmd = stack.mallocPointer(1);
            VulkanUtils.check(
                    vkAllocateCommandBuffers(ctx.device, allocInfo, pCmd),
                    "Failed to allocate transfer command buffer"
            );
            VkCommandBuffer cmd = new VkCommandBuffer(pCmd.get(0), ctx.device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanUtils.check(vkBeginCommandBuffer(cmd, beginInfo), "Failed to begin transfer command buffer");

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(size);
            vkCmdCopyBuffer(cmd, srcBuffer, dstBuffer, copyRegion);

            VulkanUtils.check(vkEndCommandBuffer(cmd), "Failed to end transfer command buffer");

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(cmd));
            VulkanUtils.check(
                    vkQueueSubmit(ctx.graphicsQueue, submitInfo, VK_NULL_HANDLE),
                    "Failed to submit transfer command buffer"
            );

            vkQueueWaitIdle(ctx.graphicsQueue);
            vkFreeCommandBuffers(ctx.device, ctx.transferCommandPool(), cmd);
        }
    }

    private static float[] toFloatArray(DebugMesh mesh) {
        float[] data = new float[mesh.vertices().size() * 9];
        int i = 0;
        for (DebugVertex v : mesh.vertices()) {
            data[i++] = v.x();
            data[i++] = v.y();
            data[i++] = v.z();
            data[i++] = v.red();
            data[i++] = v.green();
            data[i++] = v.blue();
            data[i++] = v.alpha();
            data[i++] = v.u();
            data[i++] = v.v();
        }
        return data;
    }

    private record BufferAllocation(long buffer, long memory) {
    }
}