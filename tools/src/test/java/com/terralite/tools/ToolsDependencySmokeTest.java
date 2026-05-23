package com.terralite.tools;

import imgui.app.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolsDependencySmokeTest {
    @Test
    void exposesImguiApplicationLayer() {
        assertNotNull(Application.class);
    }
}
