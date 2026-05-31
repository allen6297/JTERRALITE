dependencies {
    implementation(project(":core"))
    implementation(project(":content"))
    implementation(project(":game"))
    implementation(project(":runtime"))
    implementation(project(":render"))

    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.imgui.java.app)
}

tasks.register<JavaExec>("runVulkanSmoke") {
    group = "verification"
    description = "Opens a GLFW/Vulkan window with a 3x3 chunk marker grid for manual render verification."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.render.VulkanSmoke")
}

tasks.register<JavaExec>("runContentRenderSmoke") {
    group = "verification"
    description = "Loads repo content, creates runtime chunks, and renders them through the Vulkan debug backend."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.render.ContentRenderSmoke")
    args(rootProject.layout.projectDirectory.file("packs").asFile.absolutePath)
}

tasks.register<JavaExec>("generateTypeScriptApi") {
    group = "generation"
    description = "Generates TypeScript declarations for Terralite content scripts."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.scripting.TypeScriptApiGenerator")
    systemProperty("terralite.codegen.root", rootProject.layout.projectDirectory.asFile.absolutePath)
    args(rootProject.layout.projectDirectory.file("types/terralite-scripting.d.ts").asFile.absolutePath)
}

val checkTypeScriptApi by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks that checked-in TypeScript declarations match the generator output."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.scripting.TypeScriptApiGenerator")
    systemProperty("terralite.codegen.root", rootProject.layout.projectDirectory.asFile.absolutePath)
    args(
        "--check",
        rootProject.layout.projectDirectory.file("types/terralite-scripting.d.ts").asFile.absolutePath
    )
}

tasks.named("check") {
    dependsOn(checkTypeScriptApi)
}
