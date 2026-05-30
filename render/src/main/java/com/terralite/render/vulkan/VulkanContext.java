package com.terralite.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Holds the Vulkan instance, surface, physical device, logical device, and queues.
 * Each private helper manages its own MemoryStack frame to avoid exhausting the
 * thread-local stack through accumulated allocations.
 */
public final class VulkanContext {
    static final boolean VALIDATION = true;
    private static final String VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";

    private final VkInstance instance;
    private final VkDebugUtilsMessengerCallbackEXT debugCallbackHandle;
    private final long debugMessenger;
    public final long surface;
    public final VkPhysicalDevice physicalDevice;
    public final VkDevice device;
    public final VkQueue graphicsQueue;
    public final VkQueue presentQueue;
    public final int graphicsFamily;
    public final int presentFamily;

    private VulkanContext(
            VkInstance instance,
            VkDebugUtilsMessengerCallbackEXT debugCallbackHandle,
            long debugMessenger,
            long surface,
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            VkQueue graphicsQueue,
            VkQueue presentQueue,
            int graphicsFamily,
            int presentFamily
    ) {
        this.instance = instance;
        this.debugCallbackHandle = debugCallbackHandle;
        this.debugMessenger = debugMessenger;
        this.surface = surface;
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.graphicsQueue = graphicsQueue;
        this.presentQueue = presentQueue;
        this.graphicsFamily = graphicsFamily;
        this.presentFamily = presentFamily;
    }

    public static VulkanContext create(long windowHandle) {
        boolean validationActive = VALIDATION && isLayerAvailable(VALIDATION_LAYER);
        if (VALIDATION && !validationActive) {
            System.out.println("[Vulkan] Validation layer not available — running without validation");
        }

        VkInstance instance = createInstance(validationActive);

        VkDebugUtilsMessengerCallbackEXT callbackHandle = null;
        long debugMessenger = VK_NULL_HANDLE;
        if (validationActive) {
            callbackHandle = VkDebugUtilsMessengerCallbackEXT.create(
                    (severity, types, pData, user) -> {
                        VkDebugUtilsMessengerCallbackDataEXT data =
                                VkDebugUtilsMessengerCallbackDataEXT.create(pData);
                        System.err.println("[Vulkan] " + data.pMessageString());
                        return VK_FALSE;
                    });
            debugMessenger = createDebugMessenger(instance, callbackHandle);
        }

        long surface = createSurface(instance, windowHandle);
        VkPhysicalDevice physicalDevice = pickPhysicalDevice(instance, surface);
        int graphicsFamily = findQueueFamily(physicalDevice, VK_QUEUE_GRAPHICS_BIT, -1, surface, false);
        int presentFamily = findQueueFamily(physicalDevice, 0, graphicsFamily, surface, true);
        VkDevice device = createDevice(physicalDevice, graphicsFamily, presentFamily, validationActive);
        VkQueue graphicsQueue = getQueue(device, graphicsFamily);
        VkQueue presentQueue = getQueue(device, presentFamily);

        return new VulkanContext(instance, callbackHandle, debugMessenger, surface,
                physicalDevice, device, graphicsQueue, presentQueue, graphicsFamily, presentFamily);
    }

    public void destroy() {
        vkDeviceWaitIdle(device);
        vkDestroyDevice(device, null);
        vkDestroySurfaceKHR(instance, surface, null);
        if (debugMessenger != VK_NULL_HANDLE) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            if (debugCallbackHandle != null) {
                debugCallbackHandle.free();
            }
        }
        vkDestroyInstance(instance, null);
    }

    // ---- private setup helpers — each manages its own MemoryStack frame ----

    private static boolean isLayerAvailable(String layerName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer countBuf = stack.mallocInt(1);
            vkEnumerateInstanceLayerProperties(countBuf, null);
            VkLayerProperties.Buffer layers = VkLayerProperties.malloc(countBuf.get(0), stack);
            vkEnumerateInstanceLayerProperties(countBuf, layers);
            for (VkLayerProperties layer : layers) {
                if (layerName.equals(layer.layerNameString())) return true;
            }
            return false;
        }
    }

    private static VkInstance createInstance(boolean validationActive) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (requiredExtensions == null) {
                throw new IllegalStateException("Vulkan is not supported on this platform");
            }

            PointerBuffer extensions;
            if (validationActive) {
                extensions = stack.mallocPointer(requiredExtensions.remaining() + 1);
                extensions.put(requiredExtensions).put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)).flip();
            } else {
                extensions = stack.mallocPointer(requiredExtensions.remaining());
                extensions.put(requiredExtensions).flip();
            }

            PointerBuffer layers = null;
            if (validationActive) {
                layers = stack.mallocPointer(1);
                layers.put(stack.UTF8(VALIDATION_LAYER)).flip();
            }

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8("TERRALITE"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8("TERRALITE Engine"))
                    .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK10.VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(extensions)
                    .ppEnabledLayerNames(layers);

            PointerBuffer instancePtr = stack.mallocPointer(1);
            VulkanUtils.check(vkCreateInstance(createInfo, null, instancePtr), "Failed to create Vulkan instance");
            return new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private static long createDebugMessenger(VkInstance instance, VkDebugUtilsMessengerCallbackEXT cb) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT info = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                    .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                    .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                    .pfnUserCallback(cb);

            LongBuffer messengerPtr = stack.mallocLong(1);
            VulkanUtils.check(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, info, null, messengerPtr),
                    "Failed to create debug messenger");
            return messengerPtr.get(0);
        }
    }

    private static long createSurface(VkInstance instance, long windowHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer surfacePtr = stack.mallocLong(1);
            VulkanUtils.check(GLFWVulkan.glfwCreateWindowSurface(instance, windowHandle, null, surfacePtr),
                    "Failed to create window surface");
            return surfacePtr.get(0);
        }
    }

    private static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer countBuf = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, countBuf, null);
            int count = countBuf.get(0);
            if (count == 0) throw new IllegalStateException("No Vulkan-capable GPU found");

            PointerBuffer devices = stack.mallocPointer(count);
            vkEnumeratePhysicalDevices(instance, countBuf, devices);

            for (int i = 0; i < count; i++) {
                VkPhysicalDevice candidate = new VkPhysicalDevice(devices.get(i), instance);
                if (isDeviceSuitable(candidate, surface)) {
                    try (MemoryStack inner = MemoryStack.stackPush()) {
                        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(inner);
                        vkGetPhysicalDeviceProperties(candidate, props);
                        System.out.println("[Vulkan] Selected GPU: " + props.deviceNameString());
                    }
                    return candidate;
                }
            }
            throw new IllegalStateException("No suitable Vulkan GPU found");
        }
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface) {
        try {
            findQueueFamily(device, VK_QUEUE_GRAPHICS_BIT, -1, surface, false);
            findQueueFamily(device, 0, -1, surface, true);
        } catch (IllegalStateException e) {
            return false;
        }

        // Use heap allocation — extension count can be large (200+) and would exhaust the stack
        IntBuffer extCount = MemoryUtil.memAllocInt(1);
        try {
            vkEnumerateDeviceExtensionProperties(device, (String) null, extCount, null);
            VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extCount.get(0));
            try {
                vkEnumerateDeviceExtensionProperties(device, (String) null, extCount, extensions);
                for (VkExtensionProperties ext : extensions) {
                    if (KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(ext.extensionNameString())) {
                        return true;
                    }
                }
                return false;
            } finally {
                extensions.free();
            }
        } finally {
            MemoryUtil.memFree(extCount);
        }
    }

    private static int findQueueFamily(
            VkPhysicalDevice device, int requiredFlags, int excludeFamily,
            long surface, boolean requirePresent
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer countBuf = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(device, countBuf, null);
            VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(countBuf.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, countBuf, families);

            IntBuffer presentSupport = stack.mallocInt(1);
            for (int i = 0; i < families.capacity(); i++) {
                if (i == excludeFamily) continue;
                boolean flagsOk = requiredFlags == 0 || (families.get(i).queueFlags() & requiredFlags) != 0;
                if (!flagsOk) continue;

                if (requirePresent) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
                    if (presentSupport.get(0) != VK_TRUE) continue;
                }
                return i;
            }
            throw new IllegalStateException("Required queue family not found");
        }
    }

    private static VkDevice createDevice(VkPhysicalDevice physicalDevice, int graphicsFamily,
                                          int presentFamily, boolean validationActive) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean sameFamily = graphicsFamily == presentFamily;
            int queueCount = sameFamily ? 1 : 2;

            VkDeviceQueueCreateInfo.Buffer queueInfos = VkDeviceQueueCreateInfo.calloc(queueCount, stack);
            queueInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsFamily)
                    .pQueuePriorities(stack.floats(1.0f));
            if (!sameFamily) {
                queueInfos.get(1)
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(presentFamily)
                        .pQueuePriorities(stack.floats(1.0f));
            }

            PointerBuffer extensions = stack.mallocPointer(1);
            extensions.put(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)).flip();

            PointerBuffer layers = null;
            if (validationActive) {
                layers = stack.mallocPointer(1);
                layers.put(stack.UTF8(VALIDATION_LAYER)).flip();
            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueInfos)
                    .ppEnabledExtensionNames(extensions)
                    .ppEnabledLayerNames(layers);

            PointerBuffer devicePtr = stack.mallocPointer(1);
            VulkanUtils.check(vkCreateDevice(physicalDevice, createInfo, null, devicePtr), "Failed to create logical device");
            return new VkDevice(devicePtr.get(0), physicalDevice, createInfo);
        }
    }

    private static VkQueue getQueue(VkDevice device, int family) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer queuePtr = stack.mallocPointer(1);
            vkGetDeviceQueue(device, family, 0, queuePtr);
            return new VkQueue(queuePtr.get(0), device);
        }
    }
}
