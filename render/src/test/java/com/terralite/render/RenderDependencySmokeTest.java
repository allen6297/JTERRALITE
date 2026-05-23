package com.terralite.render;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDependencySmokeTest {
    @Test
    void exposesLwjglVulkanGlfwAndImguiDockingApis() {
        assertEquals(1, VK10.VK_TRUE);
        assertTrue(GLFW.GLFW_CLIENT_API > 0);
        assertTrue(ImGuiConfigFlags.DockingEnable != 0);
        assertNotNull(ImGui.class);
    }
}
