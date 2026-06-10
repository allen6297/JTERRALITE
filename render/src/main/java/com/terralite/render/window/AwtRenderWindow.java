package com.terralite.render.window;

import com.terralite.render.Viewport;
import com.terralite.render.vulkan.VulkanSurfaceFactory;
import com.terralite.render.vulkan.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.*;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;

import org.lwjgl.system.windows.WinBase;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.system.jawt.JAWTFunctions.*;
import static org.lwjgl.vulkan.KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRWin32Surface.*;

/**
 * A {@link RenderWindow} backed by an AWT {@link Canvas}, designed to run inside a
 * Compose {@code SwingPanel}.
 *
 * <p>Vulkan surface creation uses JAWT to extract the Win32 HWND of the canvas's
 * native peer. The HWND lookup is performed on the AWT Event Dispatch Thread
 * after the canvas is fully shown and sized.
 *
 * <p><b>Platform:</b> Windows only (uses {@code vkCreateWin32SurfaceKHR}).
 */
public final class AwtRenderWindow implements RenderWindow {

    private final Canvas        canvas;
    private final WindowConfig  config;
    private final AtomicBoolean shouldClose;

    private volatile WindowState state = WindowState.CREATED;
    /** Cached HWND resolved by {@link #create()}; used by the surface factory. */
    private volatile long        resolvedHwnd = 0L;

    /**
     * @param canvas      AWT canvas that will receive the Vulkan surface
     * @param config      window configuration (used as size fallback before layout)
     * @param shouldClose external flag — set to {@code true} to stop the render loop
     */
    public AwtRenderWindow(Canvas canvas, WindowConfig config, AtomicBoolean shouldClose) {
        this.canvas      = canvas;
        this.config      = config;
        this.shouldClose = shouldClose;
    }

    @Override public WindowConfig config() { return config; }
    @Override public WindowState  state()  { return state; }

    /**
     * Blocks the calling thread until the canvas is fully shown and has a positive
     * size, then extracts the native HWND via JAWT (on the AWT EDT).
     *
     * <p>Must be called from the game/render thread before Vulkan surface creation.
     */
    @Override
    public void create() {
        if (state == WindowState.OPEN)   return;
        if (state == WindowState.CLOSED) throw new IllegalStateException("Cannot reopen a destroyed AwtRenderWindow");

        // 1. Wait until the canvas is showing AND has been laid out with a real size.
        //    isDisplayable() → peer exists; isShowing() → visible on screen; size > 0 → laid out.
        long deadline = System.currentTimeMillis() + 15_000;
        while (true) {
            if (canvas.isShowing() && canvas.getWidth() > 0 && canvas.getHeight() > 0) break;
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(
                        "AWT canvas did not become visible within 15 s " +
                        "(showing=" + canvas.isShowing() +
                        ", w=" + canvas.getWidth() + ", h=" + canvas.getHeight() + ")");
            }
            try { Thread.sleep(20); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for canvas");
            }
        }

        // 2. Resolve HWND on the EDT — AWT peer access is safest from the EDT.
        resolvedHwnd = resolveHwndOnEdt(canvas);
        if (resolvedHwnd == 0L) {
            throw new IllegalStateException(
                    "JAWT returned a null HWND for the AWT canvas. " +
                    "Ensure the canvas is a heavyweight component in a displayable Swing hierarchy.");
        }

        state = WindowState.OPEN;
    }

    /** No-op — Compose/Swing manages visibility. */
    @Override public void show() {}

    /** No-op — AWT/Compose dispatch thread handles events. */
    @Override public void pollEvents() {}

    @Override public boolean shouldClose() { return shouldClose.get(); }

    @Override
    public Viewport viewport() {
        int w = Math.max(1, canvas.getWidth());
        int h = Math.max(1, canvas.getHeight());
        return new Viewport(w, h);
    }

    /** Marks the window as closed; does not destroy the underlying canvas. */
    @Override
    public void destroy() { state = WindowState.CLOSED; }

    @Override
    public VulkanSurfaceFactory vulkanSurfaceFactory() {
        long hwnd = resolvedHwnd;
        return new HwndVulkanSurfaceFactory(hwnd);
    }

    // ─── HWND resolution ─────────────────────────────────────────────────────

    /**
     * Runs the JAWT HWND lookup on the AWT EDT and returns the result.
     * Returns 0 if the lookup fails for any reason.
     */
    private static long resolveHwndOnEdt(Component component) {
        AtomicLong hwndRef = new AtomicLong(0L);
        Runnable task = () -> {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) {
                // Platform not yet supported — caller will throw a useful message
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                JAWT awt = JAWT.calloc(stack);
                awt.version(JAWT_VERSION_1_4);
                if (!JAWT_GetAWT(awt)) {
                    System.err.println("[AwtRenderWindow] JAWT_GetAWT returned false");
                    return;
                }

                JAWTDrawingSurface ds = JAWT_GetDrawingSurface(component, awt.GetDrawingSurface());
                if (ds == null) {
                    System.err.println("[AwtRenderWindow] JAWT_GetDrawingSurface returned null");
                    return;
                }

                int lockFlags = JAWT_DrawingSurface_Lock(ds, ds.Lock());
                if ((lockFlags & JAWT_LOCK_ERROR) != 0) {
                    System.err.printf("[AwtRenderWindow] JAWT_DrawingSurface_Lock failed: 0x%x%n", lockFlags);
                    JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
                    return;
                }

                try {
                    JAWTDrawingSurfaceInfo dsi =
                            JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
                    if (dsi == null) {
                        System.err.println("[AwtRenderWindow] JAWT_DrawingSurface_GetDrawingSurfaceInfo returned null");
                        return;
                    }

                    long platformInfo = dsi.platformInfo();
                    if (platformInfo == 0) {
                        System.err.println("[AwtRenderWindow] platformInfo pointer is null");
                        JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                        return;
                    }

                    JAWTWin32DrawingSurfaceInfo win32 = JAWTWin32DrawingSurfaceInfo.create(platformInfo);
                    long hwnd = win32.hwnd();
                    System.out.printf("[AwtRenderWindow] Resolved HWND = 0x%x%n", hwnd);
                    hwndRef.set(hwnd);

                    JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                } finally {
                    JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
                    JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            try {
                EventQueue.invokeAndWait(task);
            } catch (InvocationTargetException | InterruptedException e) {
                System.err.println("[AwtRenderWindow] HWND resolution failed: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        return hwndRef.get();
    }

    // ─── Surface factory ─────────────────────────────────────────────────────

    /** Win32 surface factory that uses a pre-resolved HWND. */
    private static final class HwndVulkanSurfaceFactory implements VulkanSurfaceFactory {

        private final long hwnd;

        HwndVulkanSurfaceFactory(long hwnd) {
            this.hwnd = hwnd;
        }

        @Override
        public PointerBuffer requiredExtensions(MemoryStack stack) {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                return stack.pointers(
                        stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME),
                        stack.UTF8(VK_KHR_WIN32_SURFACE_EXTENSION_NAME)
                );
            }
            throw new UnsupportedOperationException(
                    "AwtRenderWindow: unsupported OS '" + System.getProperty("os.name") + "'. " +
                    "Only Windows is currently implemented.");
        }

        @Override
        public long createSurface(VkInstance instance, MemoryStack stack) {
            if (hwnd == 0L) {
                throw new IllegalStateException("HWND is 0 — create() was not called or JAWT failed");
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) {
                throw new UnsupportedOperationException(
                        "AwtRenderWindow: unsupported OS '" + System.getProperty("os.name") + "'.");
            }

            // hinstance: use the current process module handle (null name = current process)
            long hinstance = WinBase.GetModuleHandle(null, (CharSequence) null);

            VkWin32SurfaceCreateInfoKHR createInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                    .sType$Default()
                    .hinstance(hinstance)
                    .hwnd(hwnd);

            LongBuffer pSurface = stack.mallocLong(1);
            VulkanUtils.check(
                    vkCreateWin32SurfaceKHR(instance, createInfo, null, pSurface),
                    "Failed to create Win32 Vulkan surface"
            );
            return pSurface.get(0);
        }
    }
}
