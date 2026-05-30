dependencies {
    implementation(project(":core"))
    implementation(project(":content"))
    implementation(project(":render"))

    implementation(libs.imgui.java.app)
}

tasks.register<JavaExec>("runVulkanSmoke") {
    group = "verification"
    description = "Opens a GLFW/Vulkan window with a 3x3 chunk marker grid for manual render verification."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.render.VulkanSmoke")
}
