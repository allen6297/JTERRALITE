package com.terralite.render.vulkan;

import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * A host-visible Vulkan vertex buffer backed by a single {@link DebugMesh}.
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
            VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer bufPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateBuffer(ctx.device, bufInfo, null, bufPtr), "Failed to create vertex buffer");
            long buffer = bufPtr.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(ctx.device, buffer, memReq);

            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(ctx.physicalDevice, memProps);
            int memTypeIndex = VulkanUtils.findMemoryType(memProps, memReq.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReq.size())
                    .memoryTypeIndex(memTypeIndex);

            LongBuffer memPtr = stack.mallocLong(1);
            VulkanUtils.check(vkAllocateMemory(ctx.device, allocInfo, null, memPtr), "Failed to allocate vertex buffer memory");
            long memory = memPtr.get(0);
            vkBindBufferMemory(ctx.device, buffer, memory, 0);

            // Map, write, unmap
            org.lwjgl.PointerBuffer dataPtr = stack.mallocPointer(1);
            vkMapMemory(ctx.device, memory, 0, size, 0, dataPtr);
            FloatBuffer mapped = MemoryUtil.memFloatBuffer(dataPtr.get(0), data.length);
            mapped.put(data).flip();
            vkUnmapMemory(ctx.device, memory);

            return new VulkanMeshBuffer(buffer, memory, mesh.vertices().size());
        }
    }

    public void destroy(VulkanContext ctx) {
        vkDestroyBuffer(ctx.device, buffer, null);
        vkFreeMemory(ctx.device, memory, null);
    }

    private static float[] toFloatArray(DebugMesh mesh) {
        float[] data = new float[mesh.vertices().size() * 6];
        int i = 0;
        for (DebugVertex v : mesh.vertices()) {
            data[i++] = v.x();
            data[i++] = v.y();
            data[i++] = v.z();
            data[i++] = v.red();
            data[i++] = v.green();
            data[i++] = v.blue();
        }
        return data;
    }
}
