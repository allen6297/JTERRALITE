package com.terralite.render.glfw;

import com.terralite.render.Viewport;
import com.terralite.render.vulkan.VulkanSurfaceFactory;
import com.terralite.render.window.RenderWindow;
import com.terralite.render.window.WindowConfig;
import com.terralite.render.window.WindowState;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import java.nio.IntBuffer;
import java.util.Objects;

public final class GlfwWindow implements RenderWindow {
    private final WindowConfig config;
    private WindowState state = WindowState.CREATED;
    private long handle;

    public GlfwWindow(WindowConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public WindowConfig config() {
        return config;
    }

    @Override
    public WindowState state() {
        return state;
    }

    public long handle() {
        return handle;
    }

    @Override
    public void create() {
        if (state == WindowState.OPEN) {
            return;
        }
        if (state == WindowState.CLOSED) {
            throw new IllegalStateException("Window cannot be recreated after destroy");
        }
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        if (config.openGlContext()) {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        }

        handle = GLFW.glfwCreateWindow(config.width(), config.height(), config.title(), 0L, 0L);
        if (handle == 0L) {
            GLFW.glfwTerminate();
            throw new IllegalStateException("Failed to create GLFW window");
        }

        state = WindowState.OPEN;
        if (config.visible()) {
            show();
        }
    }

    @Override
    public void show() {
        ensureOpen();
        GLFW.glfwShowWindow(handle);
    }

    @Override
    public void makeContextCurrent() {
        ensureOpen();
        if (!config.openGlContext()) {
            throw new IllegalStateException("Window was not configured with an OpenGL context");
        }
        GLFW.glfwMakeContextCurrent(handle);
    }

    @Override
    public void swapBuffers() {
        ensureOpen();
        if (!config.openGlContext()) {
            throw new IllegalStateException("Window was not configured with an OpenGL context");
        }
        GLFW.glfwSwapBuffers(handle);
    }

    @Override
    public void pollEvents() {
        ensureOpen();
        GLFW.glfwPollEvents();
    }

    @Override
    public boolean shouldClose() {
        ensureOpen();
        return GLFW.glfwWindowShouldClose(handle);
    }

    @Override
    public Viewport viewport() {
        ensureOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, width, height);
            return new Viewport(Math.max(1, width.get(0)), Math.max(1, height.get(0)));
        }
    }

    @Override
    public void destroy() {
        if (state != WindowState.OPEN) {
            return;
        }

        GLFW.glfwDestroyWindow(handle);
        handle = 0L;
        state = WindowState.CLOSED;
        GLFW.glfwTerminate();
    }

    @Override
    public VulkanSurfaceFactory vulkanSurfaceFactory() {
        final long h = handle;
        return new VulkanSurfaceFactory() {
            @Override
            public org.lwjgl.PointerBuffer requiredExtensions(MemoryStack stack) {
                org.lwjgl.PointerBuffer ext = GLFWVulkan.glfwGetRequiredInstanceExtensions();
                if (ext == null) throw new IllegalStateException("Vulkan not supported on this platform (GLFW)");
                return ext;
            }

            @Override
            public long createSurface(VkInstance instance, MemoryStack stack) {
                LongBuffer surfacePtr = stack.mallocLong(1);
                com.terralite.render.vulkan.VulkanUtils.check(
                        GLFWVulkan.glfwCreateWindowSurface(instance, h, null, surfacePtr),
                        "Failed to create GLFW window surface"
                );
                return surfacePtr.get(0);
            }
        };
    }

    private void ensureOpen() {
        if (state != WindowState.OPEN || handle == 0L) {
            throw new IllegalStateException("Window is not open");
        }
    }
}
