package com.terralite.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Manages swapchain images, image views, the render pass, and framebuffers.
 * Must be recreated on window resize.
 */
public final class VulkanSwapchain {
    public final long swapchain;
    public final List<Long> imageViews;
    public final long renderPass;
    public final List<Long> framebuffers;
    public final int format;
    public final int width;
    public final int height;
    public final int imageCount;

    private VulkanSwapchain(long swapchain, List<Long> imageViews, long renderPass,
                             List<Long> framebuffers, int format, int width, int height) {
        this.swapchain = swapchain;
        this.imageViews = imageViews;
        this.renderPass = renderPass;
        this.framebuffers = framebuffers;
        this.format = format;
        this.width = width;
        this.height = height;
        this.imageCount = imageViews.size();
    }

    public static VulkanSwapchain create(VulkanContext ctx, int windowWidth, int windowHeight) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Query surface capabilities
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(ctx.physicalDevice, ctx.surface, caps);

            int format = chooseFormat(ctx, stack);
            int presentMode = choosePresentMode(ctx, stack);
            int width = clamp(windowWidth, caps.minImageExtent().width(), caps.maxImageExtent().width());
            int height = clamp(windowHeight, caps.minImageExtent().height(), caps.maxImageExtent().height());

            int imageCount = caps.minImageCount() + 1;
            if (caps.maxImageCount() > 0) imageCount = Math.min(imageCount, caps.maxImageCount());

            VkSwapchainCreateInfoKHR swapInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(ctx.surface)
                    .minImageCount(imageCount)
                    .imageFormat(format)
                    .imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .preTransform(caps.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);

            swapInfo.imageExtent().set(width, height);

            if (ctx.graphicsFamily != ctx.presentFamily) {
                swapInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(ctx.graphicsFamily, ctx.presentFamily));
            } else {
                swapInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            LongBuffer swapchainPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateSwapchainKHR(ctx.device, swapInfo, null, swapchainPtr),
                    "Failed to create swapchain");
            long swapchain = swapchainPtr.get(0);

            // Get swapchain images
            IntBuffer countBuf = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(ctx.device, swapchain, countBuf, null);
            LongBuffer images = stack.mallocLong(countBuf.get(0));
            vkGetSwapchainImagesKHR(ctx.device, swapchain, countBuf, images);

            List<Long> imageViews = createImageViews(ctx.device, images, format, stack);
            long renderPass = createRenderPass(ctx.device, format, stack);
            List<Long> framebuffers = createFramebuffers(ctx.device, imageViews, renderPass, width, height, stack);

            return new VulkanSwapchain(swapchain, imageViews, renderPass, framebuffers, format, width, height);
        }
    }

    public void destroy(VulkanContext ctx) {
        for (long fb : framebuffers) vkDestroyFramebuffer(ctx.device, fb, null);
        vkDestroyRenderPass(ctx.device, renderPass, null);
        for (long iv : imageViews) vkDestroyImageView(ctx.device, iv, null);
        vkDestroySwapchainKHR(ctx.device, swapchain, null);
    }

    // ---- private helpers ----

    private static int chooseFormat(VulkanContext ctx, MemoryStack stack) {
        IntBuffer countBuf = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(ctx.physicalDevice, ctx.surface, countBuf, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(countBuf.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(ctx.physicalDevice, ctx.surface, countBuf, formats);

        for (VkSurfaceFormatKHR f : formats) {
            if (f.format() == VK_FORMAT_B8G8R8A8_SRGB &&
                    f.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return f.format();
            }
        }
        return formats.get(0).format();
    }

    private static int choosePresentMode(VulkanContext ctx, MemoryStack stack) {
        IntBuffer countBuf = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(ctx.physicalDevice, ctx.surface, countBuf, null);
        IntBuffer modes = stack.mallocInt(countBuf.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(ctx.physicalDevice, ctx.surface, countBuf, modes);

        for (int i = 0; i < modes.limit(); i++) {
            if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) return VK_PRESENT_MODE_MAILBOX_KHR;
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static List<Long> createImageViews(VkDevice device, LongBuffer images, int format, MemoryStack stack) {
        List<Long> views = new ArrayList<>();
        for (int i = 0; i < images.limit(); i++) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(images.get(i))
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format);
            viewInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);

            LongBuffer viewPtr = stack.mallocLong(1);
            VulkanUtils.check(vkCreateImageView(device, viewInfo, null, viewPtr), "Failed to create image view");
            views.add(viewPtr.get(0));
        }
        return views;
    }

    private static long createRenderPass(VkDevice device, int format, MemoryStack stack) {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack)
                .format(format)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorRef);

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass)
                .pDependencies(dependency);

        LongBuffer renderPassPtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreateRenderPass(device, renderPassInfo, null, renderPassPtr), "Failed to create render pass");
        return renderPassPtr.get(0);
    }

    private static List<Long> createFramebuffers(VkDevice device, List<Long> imageViews,
                                                   long renderPass, int width, int height, MemoryStack stack) {
        List<Long> framebuffers = new ArrayList<>();
        LongBuffer attachments = stack.mallocLong(1);
        LongBuffer fbPtr = stack.mallocLong(1);

        for (long imageView : imageViews) {
            attachments.put(0, imageView);
            VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .pAttachments(attachments)
                    .width(width)
                    .height(height)
                    .layers(1);

            VulkanUtils.check(vkCreateFramebuffer(device, fbInfo, null, fbPtr), "Failed to create framebuffer");
            framebuffers.add(fbPtr.get(0));
        }
        return framebuffers;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
