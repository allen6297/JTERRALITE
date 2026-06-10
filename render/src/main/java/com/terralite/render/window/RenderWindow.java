package com.terralite.render.window;

import com.terralite.render.Viewport;
import com.terralite.render.vulkan.VulkanSurfaceFactory;

public interface RenderWindow {
    WindowConfig config();

    WindowState state();

    void create();

    void show();

    default void makeContextCurrent() {
        throw new UnsupportedOperationException("Window does not expose a graphics context");
    }

    default void swapBuffers() {
        throw new UnsupportedOperationException("Window does not expose swap buffers");
    }

    void pollEvents();

    boolean shouldClose();

    Viewport viewport();

    void destroy();

    /**
     * Returns a {@link VulkanSurfaceFactory} that can create a Vulkan surface for this window.
     * Implementations should override this to provide platform-specific surface creation.
     */
    default VulkanSurfaceFactory vulkanSurfaceFactory() {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not implement vulkanSurfaceFactory()");
    }
}
