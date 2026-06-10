package com.terralite.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

/**
 * Abstracts platform-specific Vulkan surface and instance extension provisioning.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@code GlfwVulkanSurfaceFactory} — uses GLFW to provide extensions and surface</li>
 *   <li>{@code AwtVulkanSurfaceFactory} — uses JAWT (Win32) to provide extensions and surface</li>
 * </ul>
 */
public interface VulkanSurfaceFactory {

    /**
     * Returns the Vulkan instance extensions required by this surface type
     * (e.g. {@code VK_KHR_surface} + {@code VK_KHR_win32_surface}).
     *
     * <p>The returned buffer must remain valid for the duration of the {@code VkInstanceCreateInfo}
     * call — allocate from the provided {@code MemoryStack} or return a statically valid buffer.
     */
    PointerBuffer requiredExtensions(MemoryStack stack);

    /**
     * Creates a Vulkan surface for the underlying native window/canvas.
     *
     * @param instance the already-created {@link VkInstance}
     * @param stack    a memory stack for temporary allocations
     * @return the raw {@code VkSurfaceKHR} handle
     */
    long createSurface(VkInstance instance, MemoryStack stack);
}
