package com.terralite.render.vulkan;

import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanUtils {
    private VulkanUtils() {
    }

    public static void check(int result, String message) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(message + " (VkResult=" + result + ")");
        }
    }

    public static int findMemoryType(VkPhysicalDeviceMemoryProperties memProps, int typeFilter, int requiredFlags) {
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (memProps.memoryTypes(i).propertyFlags() & requiredFlags) == requiredFlags) {
                return i;
            }
        }
        throw new IllegalStateException("Failed to find suitable Vulkan memory type (filter=" + typeFilter + ", flags=" + requiredFlags + ")");
    }
}
