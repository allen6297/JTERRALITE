dependencies {
    implementation(project(":core"))
    implementation(project(":content"))
    implementation(project(":render"))

    implementation(libs.imgui.java.app)
}

tasks.register<JavaExec>("runOpenGlSmoke") {
    group = "verification"
    description = "Opens a GLFW/OpenGL window and clears it with a changing color for manual render verification."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.terralite.tools.render.OpenGlSmoke")
}
