package com.terralite.render.vulkan;

import com.terralite.render.texture.TextureAtlas;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanTextureAtlas {
    private final long image;
    private final long memory;
    private final long imageView;
    private final long sampler;
    private final long descriptorPool;
    public final long descriptorSet;

    private VulkanTextureAtlas(long image, long memory, long imageView, long sampler, long descriptorPool, long descriptorSet) {
        this.image = image;
        this.memory = memory;
        this.imageView = imageView;
        this.sampler = sampler;
        this.descriptorPool = descriptorPool;
        this.descriptorSet = descriptorSet;
    }

    public static VulkanTextureAtlas create(VulkanContext ctx, VulkanCommands commands, long descriptorSetLayout, TextureAtlas atlas) {
        long[] staging = createBuffer(ctx, (long) atlas.width() * atlas.height() * 4,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        long stagingBuffer = staging[0];
        long stagingMemory = staging[1];
        copyPixelsToMemory(ctx, stagingMemory, atlas);

        long[] imageResources = createImage(ctx, atlas.width(), atlas.height());
        long image = imageResources[0];
        long memory = imageResources[1];

        transitionImageLayout(ctx, commands, image, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
        copyBufferToImage(ctx, commands, stagingBuffer, image, atlas.width(), atlas.height());
        transitionImageLayout(ctx, commands, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        vkDestroyBuffer(ctx.device, stagingBuffer, null);
        vkFreeMemory(ctx.device, stagingMemory, null);

        long imageView = createImageView(ctx, image);
        long sampler = createSampler(ctx);
        long descriptorPool = createDescriptorPool(ctx);
        long descriptorSet = createDescriptorSet(ctx, descriptorPool, descriptorSetLayout, imageView, sampler);
        return new VulkanTextureAtlas(image, memory, imageView, sampler, descriptorPool, descriptorSet);
    }

    public void destroy(VulkanContext ctx) {
        vkDestroyDescriptorPool(ctx.device, descriptorPool, null);
        vkDestroySampler(ctx.device, sampler, null);
        vkDestroyImageView(ctx.device, imageView, null);
        vkDestroyImage(ctx.device, image, null);
        vkFreeMemory(ctx.device, memory, null);
    }

    private static void copyPixelsToMemory(VulkanContext ctx, long memory, TextureAtlas atlas) {
        long size = (long) atlas.width() * atlas.height() * 4;
        PointerBuffer dataPtr = MemoryUtil.memAllocPointer(1);
        try {
            VulkanUtils.check(vkMapMemory(ctx.device, memory, 0, size, 0, dataPtr), "Failed to map texture staging memory");
            ByteBuffer buffer = MemoryUtil.memByteBuffer(dataPtr.get(0), (int) size);
            for (int argb : atlas.argbPixels()) {
                buffer.put((byte) ((argb >> 16) & 0xff));
                buffer.put((byte) ((argb >> 8) & 0xff));
                buffer.put((byte) (argb & 0xff));
                buffer.put((byte) ((argb >> 24) & 0xff));
            }
            buffer.flip();
            vkUnmapMemory(ctx.device, memory);
        } finally {
            MemoryUtil.memFree(dataPtr);
        }
    }

    private static long[] createImage(VulkanContext ctx, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(VK_FORMAT_R8G8B8A8_UNORM)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().set(width, height, 1);

            LongBuffer imagePtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateImage(ctx.device, imageInfo, null, imagePtr), "Failed to create texture image");
            long image = imagePtr.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(ctx.device, image, memReq);

            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(ctx.physicalDevice, memProps);
            int memType = VulkanUtils.findMemoryType(memProps, memReq.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReq.size())
                    .memoryTypeIndex(memType);

            LongBuffer memoryPtr = stack.mallocLong(1);
            VulkanUtils.check(vkAllocateMemory(ctx.device, allocInfo, null, memoryPtr), "Failed to allocate texture memory");
            long memory = memoryPtr.get(0);
            vkBindImageMemory(ctx.device, image, memory, 0);
            return new long[] {image, memory};
        }
    }

    private static long createImageView(VulkanContext ctx, long image) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(VK_FORMAT_R8G8B8A8_UNORM);
            viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);

            LongBuffer viewPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateImageView(ctx.device, viewInfo, null, viewPtr), "Failed to create texture image view");
            return viewPtr.get(0);
        }
    }

    private static long createSampler(VulkanContext ctx) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_NEAREST)
                    .minFilter(VK_FILTER_NEAREST)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .anisotropyEnable(false)
                    .maxAnisotropy(1.0f)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);

            LongBuffer samplerPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateSampler(ctx.device, samplerInfo, null, samplerPtr), "Failed to create texture sampler");
            return samplerPtr.get(0);
        }
    }

    private static long createDescriptorPool(VulkanContext ctx) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1);
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSize)
                    .maxSets(1);
            LongBuffer poolPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateDescriptorPool(ctx.device, poolInfo, null, poolPtr), "Failed to create texture descriptor pool");
            return poolPtr.get(0);
        }
    }

    private static long createDescriptorSet(VulkanContext ctx, long descriptorPool, long descriptorSetLayout, long imageView, long sampler) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(stack.longs(descriptorSetLayout));
            LongBuffer setPtr = stack.mallocLong(1);
            VulkanUtils.check(vkAllocateDescriptorSets(ctx.device, allocInfo, setPtr), "Failed to allocate texture descriptor set");
            long descriptorSet = setPtr.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(imageView)
                    .sampler(sampler);
            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);
            vkUpdateDescriptorSets(ctx.device, descriptorWrite, null);
            return descriptorSet;
        }
    }

    private static long[] createBuffer(VulkanContext ctx, long size, int usage, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer bufferPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateBuffer(ctx.device, bufferInfo, null, bufferPtr), "Failed to create texture staging buffer");
            long buffer = bufferPtr.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(ctx.device, buffer, memReq);
            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(ctx.physicalDevice, memProps);
            int memType = VulkanUtils.findMemoryType(memProps, memReq.memoryTypeBits(), properties);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReq.size())
                    .memoryTypeIndex(memType);
            LongBuffer memoryPtr = stack.mallocLong(1);
            VulkanUtils.check(vkAllocateMemory(ctx.device, allocInfo, null, memoryPtr), "Failed to allocate texture staging memory");
            long memory = memoryPtr.get(0);
            vkBindBufferMemory(ctx.device, buffer, memory, 0);
            return new long[] {buffer, memory};
        }
    }

    private static void transitionImageLayout(VulkanContext ctx, VulkanCommands commands, long image, int oldLayout, int newLayout) {
        VkCommandBuffer commandBuffer = beginSingleTimeCommands(ctx, commands);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image);
            barrier.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);

            int srcStage;
            int dstStage;
            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }

            vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, barrier);
        }
        endSingleTimeCommands(ctx, commands, commandBuffer);
    }

    private static void copyBufferToImage(VulkanContext ctx, VulkanCommands commands, long buffer, long image, int width, int height) {
        VkCommandBuffer commandBuffer = beginSingleTimeCommands(ctx, commands);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0);
            region.imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(width, height, 1);
            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
        }
        endSingleTimeCommands(ctx, commands, commandBuffer);
    }

    private static VkCommandBuffer beginSingleTimeCommands(VulkanContext ctx, VulkanCommands commands) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commands.commandPool)
                    .commandBufferCount(1);
            PointerBuffer bufferPtr = stack.mallocPointer(1);
            VulkanUtils.check(vkAllocateCommandBuffers(ctx.device, allocInfo, bufferPtr), "Failed to allocate texture command buffer");
            VkCommandBuffer commandBuffer = new VkCommandBuffer(bufferPtr.get(0), ctx.device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VulkanUtils.check(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin texture command buffer");
            return commandBuffer;
        }
    }

    private static void endSingleTimeCommands(VulkanContext ctx, VulkanCommands commands, VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanUtils.check(vkEndCommandBuffer(commandBuffer), "Failed to end texture command buffer");
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));
            VulkanUtils.check(vkQueueSubmit(ctx.graphicsQueue, submitInfo, VK_NULL_HANDLE), "Failed to submit texture command buffer");
            vkQueueWaitIdle(ctx.graphicsQueue);
            vkFreeCommandBuffers(ctx.device, commands.commandPool, commandBuffer);
        }
    }
}
