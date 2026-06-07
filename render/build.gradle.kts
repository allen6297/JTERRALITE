val lwjglNatives = when {
    System.getProperty("os.name").startsWith("Windows") -> "natives-windows"
    System.getProperty("os.name").startsWith("Linux") -> "natives-linux"
    System.getProperty("os.name").startsWith("Mac") && System.getProperty("os.arch") == "aarch64" -> "natives-macos-arm64"
    System.getProperty("os.name").startsWith("Mac") -> "natives-macos"
    else -> throw GradleException("Unsupported LWJGL platform: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
}

val imguiNatives = when {
    System.getProperty("os.name").startsWith("Windows") -> libs.imgui.java.natives.windows
    System.getProperty("os.name").startsWith("Linux") -> libs.imgui.java.natives.linux
    System.getProperty("os.name").startsWith("Mac") -> libs.imgui.java.natives.macos
    else -> throw GradleException("Unsupported ImGui platform: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
}

val lwjglVersion = libs.versions.lwjgl.get()

dependencies {
    api(project(":core"))
    api(project(":platform"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl)
    implementation(libs.lwjgl.assimp)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.openal)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.shaderc)
    implementation(libs.lwjgl.stb)
    implementation(libs.lwjgl.vma)
    implementation(libs.lwjgl.vulkan)

    implementation(libs.imgui.java.binding)
    implementation(libs.imgui.java.lwjgl3)

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:$lwjglNatives")
    // lwjgl-vulkan has no natives JAR on Windows — Vulkan is loaded from the system vulkan-1.dll

    runtimeOnly(imguiNatives)
}
